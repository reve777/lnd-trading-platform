package com.lightning.trading.entity;

import com.lightning.trading.util.TransactionStatus;
import com.lightning.trading.util.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 交易歷史紀錄實體 (TransactionRecord)
 * <p>
 * 記錄系統內部標準記帳交易與 LND 閃電網路離鏈支付交易之所有明細。
 * 包含金額、狀態、執行延遲、客戶端 IP、原始請求/回應負載、以及閃電網路專屬之發票、Payment Hash 與 Preimage。
 * </p>
 */
@Entity
@Table(name = "transaction_records", indexes = {
        @Index(name = "idx_tx_uid", columnList = "tx_uid", unique = true),
        @Index(name = "idx_tx_type", columnList = "tx_type"),
        @Index(name = "idx_tx_status", columnList = "status"),
        @Index(name = "idx_tx_created_at", columnList = "created_at")
})
public class TransactionRecord {

    /** 資料庫內部自增流水主鍵 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 16 碼大寫十六進位唯一交易流水號（由 UUIDv7 提取，支援嚴格時間自然排序且防碰撞） */
    @Column(name = "tx_uid", nullable = false, unique = true, length = 16)
    private String txUid;

    /** 完整 36 字元之 RFC 9562 UUIDv7 字串 */
    @Column(name = "uuidv7_full", nullable = false, length = 36)
    private String uuidv7Full;

    /** 交易類型：STANDARD（一般本地內部記帳）或 LND_LIGHTNING（比特幣二層閃電網路交易） */
    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 30)
    private TransactionType txType;

    /** 付款方使用者帳戶（延遲載入以提高查詢效能） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private UserAccount sender;

    /** 收款方使用者帳戶 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private UserAccount receiver;

    /** 交易金額（標準交易為法幣/帳面點數，閃電網路為比特幣聰數 Sats） */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    /** 交易執行狀態（SUCCESS: 成功, FAILED: 失敗, PENDING: 處理中, REVERTED: 已撤銷） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** 交易執行總耗時（毫秒），用於效能基準監控與比對分析 */
    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs;

    /** 交易備註訊息或用途說明 */
    @Column(length = 255)
    private String memo;

    /** 發起該筆交易之客戶端來源真實 IP 位址 */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /** 客戶端 User-Agent 標頭字串（瀏覽器或 API 客戶端辨識） */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** 請求進入時之原始 Payload JSON 字串 */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /** 回應結果之原始 Payload JSON 字串 */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    /** 【閃電網路專屬】支付雜湊值 (Payment Hash / r_hash，SHA-256) */
    @Column(name = "lnd_payment_hash", length = 100)
    private String lndPaymentHash;

    /** 【閃電網路專屬】BOLT-11 編碼之閃電網路支付發票字串 (lnbc...) */
    @Column(name = "lnd_payment_request", columnDefinition = "TEXT")
    private String lndPaymentRequest;

    /** 【閃電網路專屬】支付密鑰原像 (Payment Preimage，最終不可抵賴的密碼學付款憑證) */
    @Column(name = "lnd_preimage", length = 100)
    private String lndPreimage;

    /** 【閃電網路專屬】路由網絡中繼節點扣除的手續費 (單位: 聰 Sats) */
    @Column(name = "lnd_fee_sat")
    private Long lndFeeSat;

    /** 【閃電網路專屬】洋蔥路由路徑經過的節點跳數 (Hop Count) */
    @Column(name = "lnd_hop_count")
    private Integer lndHopCount;

    /** 交易失敗時記錄的例外錯誤訊息 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 交易紀錄寫入時間戳記（UTC，自動填入且不可更新） */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TransactionRecord() {
    }

    public TransactionRecord(Long id, String txUid, String uuidv7Full, TransactionType txType, UserAccount sender,
                             UserAccount receiver, BigDecimal amount, TransactionStatus status, Long executionTimeMs,
                             String memo, String clientIp, String userAgent, String requestPayload, String responsePayload,
                             String lndPaymentHash, String lndPaymentRequest, String lndPreimage, Long lndFeeSat,
                             Integer lndHopCount, String errorMessage, Instant createdAt) {
        this.id = id;
        this.txUid = txUid;
        this.uuidv7Full = uuidv7Full;
        this.txType = txType;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.status = status;
        this.executionTimeMs = executionTimeMs;
        this.memo = memo;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.lndPaymentHash = lndPaymentHash;
        this.lndPaymentRequest = lndPaymentRequest;
        this.lndPreimage = lndPreimage;
        this.lndFeeSat = lndFeeSat;
        this.lndHopCount = lndHopCount;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String txUid;
        private String uuidv7Full;
        private TransactionType txType;
        private UserAccount sender;
        private UserAccount receiver;
        private BigDecimal amount;
        private TransactionStatus status;
        private Long executionTimeMs;
        private String memo;
        private String clientIp;
        private String userAgent;
        private String requestPayload;
        private String responsePayload;
        private String lndPaymentHash;
        private String lndPaymentRequest;
        private String lndPreimage;
        private Long lndFeeSat;
        private Integer lndHopCount;
        private String errorMessage;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder txUid(String txUid) { this.txUid = txUid; return this; }
        public Builder uuidv7Full(String uuidv7Full) { this.uuidv7Full = uuidv7Full; return this; }
        public Builder txType(TransactionType txType) { this.txType = txType; return this; }
        public Builder sender(UserAccount sender) { this.sender = sender; return this; }
        public Builder receiver(UserAccount receiver) { this.receiver = receiver; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder status(TransactionStatus status) { this.status = status; return this; }
        public Builder executionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; return this; }
        public Builder memo(String memo) { this.memo = memo; return this; }
        public Builder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder requestPayload(String requestPayload) { this.requestPayload = requestPayload; return this; }
        public Builder responsePayload(String responsePayload) { this.responsePayload = responsePayload; return this; }
        public Builder lndPaymentHash(String lndPaymentHash) { this.lndPaymentHash = lndPaymentHash; return this; }
        public Builder lndPaymentRequest(String lndPaymentRequest) { this.lndPaymentRequest = lndPaymentRequest; return this; }
        public Builder lndPreimage(String lndPreimage) { this.lndPreimage = lndPreimage; return this; }
        public Builder lndFeeSat(Long lndFeeSat) { this.lndFeeSat = lndFeeSat; return this; }
        public Builder lndHopCount(Integer lndHopCount) { this.lndHopCount = lndHopCount; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public TransactionRecord build() {
            return new TransactionRecord(id, txUid, uuidv7Full, txType, sender, receiver, amount, status,
                    executionTimeMs, memo, clientIp, userAgent, requestPayload, responsePayload,
                    lndPaymentHash, lndPaymentRequest, lndPreimage, lndFeeSat, lndHopCount, errorMessage, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTxUid() { return txUid; }
    public void setTxUid(String txUid) { this.txUid = txUid; }

    public String getUuidv7Full() { return uuidv7Full; }
    public void setUuidv7Full(String uuidv7Full) { this.uuidv7Full = uuidv7Full; }

    public TransactionType getTxType() { return txType; }
    public void setTxType(TransactionType txType) { this.txType = txType; }

    public UserAccount getSender() { return sender; }
    public void setSender(UserAccount sender) { this.sender = sender; }

    public UserAccount getReceiver() { return receiver; }
    public void setReceiver(UserAccount receiver) { this.receiver = receiver; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }

    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }

    public String getLndPaymentHash() { return lndPaymentHash; }
    public void setLndPaymentHash(String lndPaymentHash) { this.lndPaymentHash = lndPaymentHash; }

    public String getLndPaymentRequest() { return lndPaymentRequest; }
    public void setLndPaymentRequest(String lndPaymentRequest) { this.lndPaymentRequest = lndPaymentRequest; }

    public String getLndPreimage() { return lndPreimage; }
    public void setLndPreimage(String lndPreimage) { this.lndPreimage = lndPreimage; }

    public Long getLndFeeSat() { return lndFeeSat; }
    public void setLndFeeSat(Long lndFeeSat) { this.lndFeeSat = lndFeeSat; }

    public Integer getLndHopCount() { return lndHopCount; }
    public void setLndHopCount(Integer lndHopCount) { this.lndHopCount = lndHopCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
