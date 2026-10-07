package com.lightning.trading.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightning.trading.config.LndConfig;
import com.lightning.trading.dto.LndNodeInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;

/**
 * 實體 LND 閃電網路節點 REST 客戶端實作 (LndRestClientImpl)
 * <p>
 * 負責透過 HTTP/REST 協議與真實的 LND (Lightning Network Daemon) 節點通訊：
 * <ul>
 *   <li>包含 TLS/SSL 憑證豁免處理（支援 LND 本地自我簽署 tls.cert）</li>
 *   <li>透過 Macaroon 權限憑證標頭 (Grpc-Metadata-macaroon) 進行身分驗證</li>
 *   <li>具備自動降級容錯機制：若未設定實體節點模式 (mode != "real") 或遠端呼叫異常時，自動切換至 {@link MockLndClientImpl} 模擬器</li>
 * </ul>
 * </p>
 */
@Component("lndRestClient")
public class LndRestClientImpl implements LndClient {

    private static final Logger log = LoggerFactory.getLogger(LndRestClientImpl.class);

    /** LND 連線設定參數（節點 URL、Macaroon 憑證 Hex、運作模式等） */
    private final LndConfig lndConfig;

    /** 備用之 LND 模擬器客戶端（當配置為 mock 或連線實體節點失敗時自動降級呼叫） */
    private final MockLndClientImpl mockLndClient;

    /** JSON 序列化與反序列化工具 */
    private final ObjectMapper objectMapper;

    /** 專屬 HTTP 傳輸客戶端（已配置自訂 SSLContext 支援自我簽署憑證） */
    private final HttpClient httpClient;

    public LndRestClientImpl(LndConfig lndConfig, MockLndClientImpl mockLndClient, ObjectMapper objectMapper) {
        this.lndConfig = lndConfig;
        this.mockLndClient = mockLndClient;
        this.objectMapper = objectMapper;
        this.httpClient = createHttpClient();
    }

    /**
     * 建立支援 LND 自我簽署憑證的 HttpClient
     * <p>
     * 由於開發與私有節點環境下 LND 通常使用自我簽署的 TLS 憑證 (tls.cert)，
     * 此處建立信任所有憑證的 TrustManager 以避免 SSLHandshakeException。
     * </p>
     *
     * @return 配置好 SSL 與 5 秒逾時之 HttpClient 實體
     */
    private HttpClient createHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        } catch (Exception e) {
            log.warn("建立自訂 SSL HttpClient 失敗，改用預設客戶端", e);
            return HttpClient.newHttpClient();
        }
    }

    /**
     * 檢查當前是否應連線實體 LND 節點
     *
     * @return 若 application.yml 中 lnd.mode 為 "real" 則回傳 true，否則走模擬模式
     */
    private boolean shouldUseRealNode() {
        return "real".equalsIgnoreCase(lndConfig.getMode());
    }

    /**
     * 查詢 LND 節點基本資訊
     * <p>
     * 【REST 端點】：GET /v1/getinfo
     * 【功能邏輯】：向 LND 查詢節點別名、公鑰、通道數量、Peer 連線數、區塊高度與鏈上同步狀態。
     * 若非 real 模式或遠端查詢失敗，則降級回傳模擬器數據。
     * </p>
     *
     * @return 節點資訊 DTO 物件
     */
    @Override
    public LndNodeInfoDto getInfo() {
        if (!shouldUseRealNode()) {
            return mockLndClient.getInfo();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(lndConfig.getBaseUrl() + "/v1/getinfo"))
                    .header("Grpc-Metadata-macaroon", lndConfig.getMacaroonHex())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return new LndNodeInfoDto(
                        root.path("alias").asText("LND-Real-Node"),
                        root.path("identity_pubkey").asText(""),
                        root.path("num_active_channels").asInt(0),
                        root.path("num_peers").asInt(0),
                        root.path("block_height").asLong(0),
                        root.path("synced_to_chain").asBoolean(false),
                        "REAL_LND_NODE",
                        getChannelBalanceSat(),
                        root.path("version").asText("0.18.x")
                );
            }
        } catch (Exception e) {
            log.error("查詢實體 LND 節點 /v1/getinfo 失敗，自動降級至 Mock: {}", e.getMessage());
        }

        return mockLndClient.getInfo();
    }

    /**
     * 查詢節點所有閃電網路通道的本端總可用餘額
     * <p>
     * 【REST 端點】：GET /v1/balance/channels
     * 【功能邏輯】：取得當前節點所有已開啟通道的本地流動性餘額 (local_balance.sat)。
     * </p>
     *
     * @return 本端可用通道餘額（聰 Sat）
     */
    @Override
    public long getChannelBalanceSat() {
        if (!shouldUseRealNode()) {
            return mockLndClient.getChannelBalanceSat();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(lndConfig.getBaseUrl() + "/v1/balance/channels"))
                    .header("Grpc-Metadata-macaroon", lndConfig.getMacaroonHex())
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("local_balance").path("sat").asLong(0);
            }
        } catch (Exception e) {
            log.warn("查詢實體 LND 通道餘額失敗: {}", e.getMessage());
        }

        return mockLndClient.getChannelBalanceSat();
    }

    /**
     * 建立一筆閃電網路收款發票 (BOLT-11 Invoice)
     * <p>
     * 【REST 端點】：POST /v1/invoices
     * 【功能邏輯】：向收款節點請求生成帶有密碼學數位簽章 (secp256k1) 之 BOLT-11 發票，包含支付雜湊值 (r_hash)。
     * </p>
     *
     * @param amountSat 欲收款之金額（聰 Sat）
     * @param memo      發票備註與用途說明
     * @return 發票建立回應（包含 payment_request 發票字串、r_hash 等）
     */
    @Override
    public LndInvoiceResponse createInvoice(long amountSat, String memo) {
        if (!shouldUseRealNode()) {
            return mockLndClient.createInvoice(amountSat, memo);
        }

        try {
            Map<String, Object> bodyMap = Map.of(
                    "value", String.valueOf(amountSat),
                    "memo", memo != null ? memo : "LIGHTNING-Payment"
            );
            String json = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(lndConfig.getBaseUrl() + "/v1/invoices"))
                    .header("Grpc-Metadata-macaroon", lndConfig.getMacaroonHex())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return new LndInvoiceResponse(
                        root.path("r_hash").asText(),
                        root.path("payment_request").asText(),
                        root.path("add_index").asLong()
                );
            } else {
                log.error("實體 LND createInvoice 回傳錯誤狀態 {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("呼叫實體 LND createInvoice 失敗，自動降級至 Mock: {}", e.getMessage());
        }

        return mockLndClient.createInvoice(amountSat, memo);
    }

    /**
     * 透過閃電網路支付指定的發票 (Send Payment)
     * <p>
     * 【REST 端點】：POST /v1/channels/transactions
     * 【功能邏輯】：
     * 1. 發起多跳 (Multi-hop) 洋蔥路由與 HTLC 鎖定。
     * 2. 若中繼節點尋徑與結算成功，回傳最終密碼學憑證 Preimage、手續費 feeSat 與路由節點跳數 hops。
     * 3. 若路由失敗，回傳 failureReason 錯誤訊息。
     * </p>
     *
     * @param paymentRequest BOLT-11 發票字串 (lnbc...)
     * @param amountSat      欲支付金額（聰 Sat）
     * @return 支付結果物件（包含 preimage、手續費、跳數、成功與否）
     */
    @Override
    public LndPaymentResponse sendPayment(String paymentRequest, long amountSat) {
        if (!shouldUseRealNode()) {
            return mockLndClient.sendPayment(paymentRequest, amountSat);
        }

        try {
            Map<String, Object> bodyMap = Map.of(
                    "payment_request", paymentRequest,
                    "timeout_seconds", 30,
                    "fee_limit_sat", Math.max(10, amountSat / 100)
            );
            String json = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(lndConfig.getBaseUrl() + "/v1/channels/transactions"))
                    .header("Grpc-Metadata-macaroon", lndConfig.getMacaroonHex())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(35))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String paymentError = root.path("payment_error").asText("");
                if (paymentError.isEmpty()) {
                    String preimage = root.path("payment_preimage").asText();
                    String hash = root.path("payment_hash").asText();
                    long feeSat = root.path("payment_route").path("total_fees").asLong(0);
                    int hops = root.path("payment_route").path("hops").size();
                    return new LndPaymentResponse(preimage, hash, feeSat, Math.max(1, hops), true, null);
                } else {
                    return new LndPaymentResponse(null, null, 0, 0, false, paymentError);
                }
            } else {
                return new LndPaymentResponse(null, null, 0, 0, false, "LND HTTP error: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("發起實體 LND 支付異常，降級嘗試 Mock: {}", e.getMessage());
            return mockLndClient.sendPayment(paymentRequest, amountSat);
        }
    }

    /**
     * 檢查當前是否處於模擬模式
     *
     * @return 若未啟用實體節點則回傳 true
     */
    @Override
    public boolean isMockMode() {
        return !shouldUseRealNode();
    }
}
