package com.lightning.trading.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightning.trading.client.LndClient;
import com.lightning.trading.client.LndInvoiceResponse;
import com.lightning.trading.client.LndPaymentResponse;
import com.lightning.trading.dto.*;
import com.lightning.trading.entity.TransactionRecord;
import com.lightning.trading.entity.UserAccount;
import com.lightning.trading.repo.TransactionRecordRepository;
import com.lightning.trading.repo.UserAccountRepository;
import com.lightning.trading.util.TradeConstants;
import com.lightning.trading.util.TransactionStatus;
import com.lightning.trading.util.TransactionType;
import com.lightning.trading.util.UuidV7Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 交易核心服務 (TradeService)
 * 負責處理標準後端內部記帳交易、LND 閃電網路交易以及兩者間的延遲基準測試 (Benchmark)。
 * 因無多重實作需求，直接定義為具體服務類別 (@Service)，無需額外建立 Interface 與 Impl。
 */
@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    private final TransactionRecordRepository transactionRecordRepository;
    private final UserAccountRepository userAccountRepository;
    private final LndClient lndClient;
    private final ObjectMapper objectMapper;

    public TradeService(TransactionRecordRepository transactionRecordRepository,
                        UserAccountRepository userAccountRepository,
                        @Qualifier("lndRestClient") LndClient lndClient,
                        ObjectMapper objectMapper) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.userAccountRepository = userAccountRepository;
        this.lndClient = lndClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 執行一般標準後端交易（本地資料庫內部記帳）
     * 透過關聯式資料庫 ACID 事務進行帳戶餘額加減與交易紀錄保存，具備極致低延遲。
     *
     * @param request   標準交易請求參數（付款方、收款方、金額、備註）
     * @param clientIp  客戶端真實 IP
     * @param userAgent 客戶端 User-Agent
     * @return 交易完成回傳物件
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TradeResponse executeStandardTrade(StandardTradeRequest request, String clientIp, String userAgent) {
        long startNanos = System.nanoTime();
        UuidV7Generator.UuidV7Result uuid = UuidV7Generator.generate();
        String requestJson = toJson(request);

        UserAccount sender = null;
        UserAccount receiver = null;
        try {
            sender = userAccountRepository.findByUsername(request.senderUsername())
                    .orElseThrow(() -> new IllegalArgumentException(TradeConstants.ERR_SENDER_NOT_FOUND + ": '" + request.senderUsername() + "'"));
            receiver = userAccountRepository.findByUsername(request.receiverUsername())
                    .orElseThrow(() -> new IllegalArgumentException(TradeConstants.ERR_RECEIVER_NOT_FOUND + ": '" + request.receiverUsername() + "'"));

            if (sender.getId().equals(receiver.getId())) {
                throw new IllegalArgumentException(TradeConstants.ERR_SAME_ACCOUNT_TRADE);
            }

            if (sender.getBalance().compareTo(request.amount()) < 0) {
                throw new IllegalStateException(TradeConstants.ERR_INSUFFICIENT_FUNDS + ". 可用: " + sender.getBalance() + ", 請求: " + request.amount());
            }

            // 執行資料庫帳戶餘額劃轉
            sender.setBalance(sender.getBalance().subtract(request.amount()));
            receiver.setBalance(receiver.getBalance().add(request.amount()));
            userAccountRepository.save(sender);
            userAccountRepository.save(receiver);

            long elapsedMs = Math.max(1, (System.nanoTime() - startNanos) / 1_000_000);

            TransactionRecord record = TransactionRecord.builder()
                    .txUid(uuid.txUid16())
                    .uuidv7Full(uuid.fullUuid())
                    .txType(TransactionType.STANDARD)
                    .sender(sender)
                    .receiver(receiver)
                    .amount(request.amount())
                    .status(TransactionStatus.SUCCESS)
                    .executionTimeMs(elapsedMs)
                    .memo(request.memo() != null ? request.memo() : TradeConstants.MEMO_STANDARD_TRANSFER)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .requestPayload(requestJson)
                    .build();

            TradeResponse response = TradeResponse.fromEntity(record);
            record.setResponsePayload(toJson(response));
            TransactionRecord saved = transactionRecordRepository.save(record);

            log.info("[STANDARD TRADE SUCCESS] TxUid: {}, Amount: {}, Latency: {}ms", saved.getTxUid(), saved.getAmount(), elapsedMs);
            return TradeResponse.fromEntity(saved);

        } catch (Exception ex) {
            long elapsedMs = Math.max(1, (System.nanoTime() - startNanos) / 1_000_000);
            log.error("[STANDARD TRADE FAILED] TxUid: {}, Reason: {}", uuid.txUid16(), ex.getMessage());

            TransactionRecord failedRecord = TransactionRecord.builder()
                    .txUid(uuid.txUid16())
                    .uuidv7Full(uuid.fullUuid())
                    .txType(TransactionType.STANDARD)
                    .sender(sender)
                    .receiver(receiver)
                    .amount(request.amount() != null ? request.amount() : BigDecimal.ZERO)
                    .status(TransactionStatus.FAILED)
                    .executionTimeMs(elapsedMs)
                    .memo(request.memo())
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .requestPayload(requestJson)
                    .errorMessage(ex.getMessage())
                    .build();

            TradeResponse failedResp = TradeResponse.fromEntity(failedRecord);
            failedRecord.setResponsePayload(toJson(failedResp));
            transactionRecordRepository.save(failedRecord);

            throw new RuntimeException("Trade failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 執行 LND 閃電網路支付交易
     * 包含 BOLT-11 加密發票建立、HTLC 節點尋徑、洋蔥加密轉發與離鏈結算。
     *
     * @param request   閃電網路交易請求參數（付款方、收款方、金額聰 Sat、自訂發票）
     * @param clientIp  客戶端真實 IP
     * @param userAgent 客戶端 User-Agent
     * @return 交易完成回傳物件
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TradeResponse executeLndTrade(LndTradeRequest request, String clientIp, String userAgent) {
        long startNanos = System.nanoTime();
        UuidV7Generator.UuidV7Result uuid = UuidV7Generator.generate();
        String requestJson = toJson(request);

        UserAccount sender = null;
        UserAccount receiver = null;
        try {
            sender = userAccountRepository.findByUsername(request.senderUsername())
                    .orElseThrow(() -> new IllegalArgumentException(TradeConstants.ERR_SENDER_NOT_FOUND + ": '" + request.senderUsername() + "'"));
            receiver = userAccountRepository.findByUsername(request.receiverUsername())
                    .orElseThrow(() -> new IllegalArgumentException(TradeConstants.ERR_RECEIVER_NOT_FOUND + ": '" + request.receiverUsername() + "'"));

            if (sender.getId().equals(receiver.getId())) {
                throw new IllegalArgumentException(TradeConstants.ERR_SAME_ACCOUNT_TRADE);
            }

            if (sender.getBalance().compareTo(request.amountSat()) < 0) {
                throw new IllegalStateException(TradeConstants.ERR_INSUFFICIENT_FUNDS + ". 可用: " + sender.getBalance() + ", 請求: " + request.amountSat());
            }

            // 1. 準備 Lightning Invoice (若未傳入則由收款方節點產生)
            String bolt11 = request.paymentRequest();
            String paymentHash;
            if (bolt11 == null || bolt11.isBlank()) {
                LndInvoiceResponse invoiceResp = lndClient.createInvoice(
                        request.amountSat().longValue(),
                        request.memo() != null ? request.memo() : "LIGHTNING-LND-Tx-" + uuid.txUid16()
                );
                bolt11 = invoiceResp.paymentRequest();
                paymentHash = invoiceResp.rHash();
            } else {
                paymentHash = "provided_in_invoice";
            }

            // 2. 透過 LND 發起離鏈支付
            LndPaymentResponse paymentResp = lndClient.sendPayment(bolt11, request.amountSat().longValue());

            long elapsedMs = Math.max(1, (System.nanoTime() - startNanos) / 1_000_000);

            if (!paymentResp.success()) {
                throw new RuntimeException("LND Payment Failed: " + paymentResp.failureReason());
            }

            // 3. 扣減餘額並結算
            BigDecimal totalDeducted = request.amountSat().add(BigDecimal.valueOf(paymentResp.feeSat()));
            sender.setBalance(sender.getBalance().subtract(totalDeducted));
            receiver.setBalance(receiver.getBalance().add(request.amountSat()));
            userAccountRepository.save(sender);
            userAccountRepository.save(receiver);

            TransactionRecord record = TransactionRecord.builder()
                    .txUid(uuid.txUid16())
                    .uuidv7Full(uuid.fullUuid())
                    .txType(TransactionType.LND_LIGHTNING)
                    .sender(sender)
                    .receiver(receiver)
                    .amount(request.amountSat())
                    .status(TransactionStatus.SUCCESS)
                    .executionTimeMs(elapsedMs)
                    .memo(request.memo() != null ? request.memo() : TradeConstants.MEMO_LND_OFFCHAIN_PAYMENT)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .requestPayload(requestJson)
                    .lndPaymentHash(paymentResp.paymentHash() != null ? paymentResp.paymentHash() : paymentHash)
                    .lndPaymentRequest(bolt11)
                    .lndPreimage(paymentResp.paymentPreimage())
                    .lndFeeSat(paymentResp.feeSat())
                    .lndHopCount(paymentResp.hopCount())
                    .build();

            TradeResponse response = TradeResponse.fromEntity(record);
            record.setResponsePayload(toJson(response));
            TransactionRecord saved = transactionRecordRepository.save(record);

            log.info("[LND TRADE SUCCESS] TxUid: {}, Amount: {} sats, Latency: {}ms, Fee: {} sats",
                    saved.getTxUid(), saved.getAmount(), elapsedMs, paymentResp.feeSat());

            return TradeResponse.fromEntity(saved);

        } catch (Exception ex) {
            long elapsedMs = Math.max(1, (System.nanoTime() - startNanos) / 1_000_000);
            log.error("[LND TRADE FAILED] TxUid: {}, Reason: {}", uuid.txUid16(), ex.getMessage());

            TransactionRecord failedRecord = TransactionRecord.builder()
                    .txUid(uuid.txUid16())
                    .uuidv7Full(uuid.fullUuid())
                    .txType(TransactionType.LND_LIGHTNING)
                    .sender(sender)
                    .receiver(receiver)
                    .amount(request.amountSat() != null ? request.amountSat() : BigDecimal.ZERO)
                    .status(TransactionStatus.FAILED)
                    .executionTimeMs(elapsedMs)
                    .memo(request.memo())
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .requestPayload(requestJson)
                    .errorMessage(ex.getMessage())
                    .build();

            TradeResponse failedResp = TradeResponse.fromEntity(failedRecord);
            failedRecord.setResponsePayload(toJson(failedResp));
            transactionRecordRepository.save(failedRecord);

            throw new RuntimeException("LND Trade failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 執行一般交易與閃電網路交易的效能基準對比 (Benchmark)
     *
     * @param request   基準測試參數
     * @param clientIp  客戶端真實 IP
     * @param userAgent 客戶端 User-Agent
     * @return 包含兩者耗時、倍率與分析說明的比較結果
     */
    public BenchmarkComparisonResponse executeBenchmarkComparison(BenchmarkTradeRequest request, String clientIp, String userAgent) {
        // 執行一般交易
        StandardTradeRequest standardReq = new StandardTradeRequest(
                request.senderUsername(),
                request.receiverUsername(),
                request.amount(),
                TradeConstants.MSG_BENCHMARK_PREFIX_STANDARD + (request.memo() != null ? request.memo() : "")
        );
        TradeResponse standardResp = executeStandardTrade(standardReq, clientIp, userAgent);

        // 執行 LND 交易
        LndTradeRequest lndReq = new LndTradeRequest(
                request.senderUsername(),
                request.receiverUsername(),
                request.amount(),
                TradeConstants.MSG_BENCHMARK_PREFIX_LND + (request.memo() != null ? request.memo() : ""),
                null
        );
        TradeResponse lndResp = executeLndTrade(lndReq, clientIp, userAgent);

        long diff = lndResp.executionTimeMs() - standardResp.executionTimeMs();
        double ratio = standardResp.executionTimeMs() > 0 ?
                (double) lndResp.executionTimeMs() / (double) standardResp.executionTimeMs() : 1.0;

        String analysis = String.format(
                TradeConstants.BENCHMARK_ANALYSIS_TEMPLATE,
                standardResp.executionTimeMs(),
                lndResp.executionTimeMs(),
                diff,
                ratio
        );

        return new BenchmarkComparisonResponse(standardResp, lndResp, diff, ratio, analysis);
    }

    /**
     * 查詢最近的交易紀錄清單
     *
     * @param limit 限制回傳筆數
     * @return 交易紀錄清單
     */
    @Transactional(readOnly = true)
    public List<TradeResponse> getRecentTransactions(int limit) {
        return transactionRecordRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .map(TradeResponse::fromEntity)
                .toList();
    }

    /**
     * 依據 16 位元 TxUid 查詢特定交易紀錄
     *
     * @param txUid 交易唯一辨識碼
     * @return 交易紀錄物件
     */
    @Transactional(readOnly = true)
    public TradeResponse getTransactionByTxUid(String txUid) {
        TransactionRecord record = transactionRecordRepository.findByTxUid(txUid)
                .orElseThrow(() -> new IllegalArgumentException("Transaction with UID '" + txUid + "' not found."));
        return TradeResponse.fromEntity(record);
    }

    /**
     * 取得交易類型基準統計數據
     *
     * @return 統計資料清單
     */
    @Transactional(readOnly = true)
    public List<TransactionRecordRepository.TxBenchmarkStats> getBenchmarkStats() {
        return transactionRecordRepository.getBenchmarkStatistics();
    }

    private String toJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Serialization error\"}";
        }
    }
}
