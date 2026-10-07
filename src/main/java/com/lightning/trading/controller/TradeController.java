package com.lightning.trading.controller;

import com.lightning.trading.aop.AuditAction;
import com.lightning.trading.aop.RateLimit;
import com.lightning.trading.client.LndClient;
import com.lightning.trading.common.response.ApiResponse;
import com.lightning.trading.dto.*;
import com.lightning.trading.repo.TransactionRecordRepository;
import com.lightning.trading.service.TradeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交易核心控制器 (TradeController)
 * 提供內部標準記帳交易、LND 閃電網路支付、效能基準對比與交易紀錄查詢之 REST API 接口。
 */
@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;
    private final LndClient lndClient;

    public TradeController(TradeService tradeService,
                           @Qualifier("lndRestClient") LndClient lndClient) {
        this.tradeService = tradeService;
        this.lndClient = lndClient;
    }

    /**
     * 執行一般標準交易（本地資料庫內部劃轉）
     *
     * 【端點路徑】：POST /api/trades/standard
     * 【功能說明】：在本地關聯式資料庫中以 ACID 事務執行兩個帳戶之間的餘額扣減與增加，具備極致低延遲。
     *
     * @param request        包含付款方、收款方、金額與備註之請求
     * @param servletRequest HTTP 原始請求物件（用於提取 IP 與 User-Agent）
     * @return 交易完成資訊物件
     */
    @PostMapping("/standard")
    @AuditAction("TRADE_STANDARD_EXECUTE")
    @RateLimit(maxRequests = 100, windowSeconds = 60)
    public ResponseEntity<TradeResponse> executeStandardTrade(
            @Valid @RequestBody StandardTradeRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        TradeResponse response = tradeService.executeStandardTrade(request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * 執行 LND 閃電網路支付交易
     *
     * 【端點路徑】：POST /api/trades/lnd
     * 【功能說明】：向 LND 節點發起比特幣二層閃電網路支付，透過 BOLT-11 發票與 HTLC 進行離鏈結算。
     *
     * @param request        包含付款方、收款方、金額 (聰) 與可選發票之請求
     * @param servletRequest HTTP 原始請求物件
     * @return 交易完成資訊物件（包含 LND Payment Hash、手續費、跳數等）
     */
    @PostMapping("/lnd")
    @AuditAction("TRADE_LND_EXECUTE")
    @RateLimit(maxRequests = 100, windowSeconds = 60)
    public ResponseEntity<TradeResponse> executeLndTrade(
            @Valid @RequestBody LndTradeRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        TradeResponse response = tradeService.executeLndTrade(request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * 執行效能基準對比測試 (Benchmark Comparison)
     *
     * 【端點路徑】：POST /api/trades/benchmark
     * 【功能說明】：同組交易參數分別在「一般本地後端」與「LND 閃電網路」各跑一次，對比兩者耗時與速度倍率。
     *
     * @param request        基準測試交易參數
     * @param servletRequest HTTP 原始請求物件
     * @return 對比結果物件（包含耗時差距與分析文字）
     */
    @PostMapping("/benchmark")
    @AuditAction("TRADE_BENCHMARK_COMPARISON")
    @RateLimit(maxRequests = 30, windowSeconds = 60)
    public ResponseEntity<BenchmarkComparisonResponse> executeBenchmarkComparison(
            @Valid @RequestBody BenchmarkTradeRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        BenchmarkComparisonResponse response = tradeService.executeBenchmarkComparison(request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * 查詢最近的交易紀錄清單
     *
     * 【端點路徑】：GET /api/trades/records
     *
     * @param limit 限制回傳筆數（預設 50 筆）
     * @return 交易紀錄列表
     */
    @GetMapping("/records")
    public ResponseEntity<List<TradeResponse>> getRecentRecords(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(tradeService.getRecentTransactions(limit));
    }

    /**
     * 依據 16 位元 TxUid 查詢特定交易紀錄
     *
     * 【端點路徑】：GET /api/trades/records/{txUid}
     *
     * @param txUid 交易唯一辨識碼
     * @return 該筆交易紀錄
     */
    @GetMapping("/records/{txUid}")
    public ResponseEntity<TradeResponse> getRecordByTxUid(@PathVariable String txUid) {
        return ResponseEntity.ok(tradeService.getTransactionByTxUid(txUid));
    }

    /**
     * 查詢全系統交易類型效能基準統計（平均耗時、成功率）
     *
     * 【端點路徑】：GET /api/trades/benchmark-stats
     *
     * @return 各交易類型的統計分析列表
     */
    @GetMapping("/benchmark-stats")
    public ResponseEntity<List<TransactionRecordRepository.TxBenchmarkStats>> getBenchmarkStats() {
        return ResponseEntity.ok(tradeService.getBenchmarkStats());
    }

    /**
     * 查詢當前 LND 節點連線與同步資訊
     *
     * 【端點路徑】：GET /api/trades/lnd-node
     *
     * @return LND 節點 PubKey、別名、區塊高度與同步狀態
     */
    @GetMapping("/lnd-node")
    public ResponseEntity<LndNodeInfoDto> getLndNodeInfo() {
        return ResponseEntity.ok(lndClient.getInfo());
    }

    /**
     * 從 HTTP 請求中提取客戶端真實 IP 位址
     *
     * @param request HTTP 請求
     * @return 客戶端 IP
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
