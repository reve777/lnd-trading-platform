package com.lightning.trading.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 系統操作與交易審計日誌實體 (AuditLog)
 * <p>
 * 記錄所有受切面標註方法的操作者、客戶端 IP、HTTP 請求動詞、URI、執行耗時、脫敏參數與執行成功/失敗狀態。
 * </p>
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_operator", columnList = "operator_username")
})
public class AuditLog {

    /**
     * 自增主鍵 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作行為標籤（如 TRADE_STANDARD_EXECUTE, ACCOUNT_CREATE）
     */
    @Column(nullable = false, length = 100)
    private String action;

    /**
     * 操作人員帳號（若未登入或匿名則為 ANONYMOUS）
     */
    @Column(name = "operator_username", length = 50)
    private String operatorUsername;

    /**
     * 客戶端來源 IP 位址
     */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /**
     * HTTP 請求方法 (GET, POST 等)
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /**
     * 請求端點路徑或方法簽章
     */
    @Column(length = 255)
    private String endpoint;

    /**
     * 執行耗時（毫秒）
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /**
     * 執行狀態 (SUCCESS / FAILURE)
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 脫敏後之輸入參數或異常堆疊訊息
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * 日誌寫入時間戳記 (UTC)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(Long id, String action, String operatorUsername, String clientIp, String httpMethod,
                    String endpoint, Long executionTimeMs, String status, String details, Instant createdAt) {
        this.id = id;
        this.action = action;
        this.operatorUsername = operatorUsername;
        this.clientIp = clientIp;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.executionTimeMs = executionTimeMs;
        this.status = status;
        this.details = details;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String action;
        private String operatorUsername;
        private String clientIp;
        private String httpMethod;
        private String endpoint;
        private Long executionTimeMs;
        private String status;
        private String details;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder operatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; return this; }
        public Builder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public Builder httpMethod(String httpMethod) { this.httpMethod = httpMethod; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public Builder executionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public AuditLog build() {
            return new AuditLog(id, action, operatorUsername, clientIp, httpMethod, endpoint, executionTimeMs, status, details, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
