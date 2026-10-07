package com.lightning.trading.entity;

import com.lightning.trading.util.AccountStatus;
import com.lightning.trading.util.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 使用者與錢包帳戶實體 (UserAccount)
 * <p>
 * 儲存系統使用者之身分驗證資料、系統權限角色、帳戶餘額、閃電網路公鑰與帳戶狀態。
 * </p>
 */
@Entity
@Table(name = "user_accounts", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_role", columnList = "role")
})
public class UserAccount {

    /** 資料庫內部自增主鍵 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 使用者登入帳號（唯一，不可重覆） */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 電子信箱（唯一，不可重覆） */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt 雜湊加密密碼 */
    @Column(nullable = false, length = 255)
    private String password;

    /** 使用者真實全名或暱稱 */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /** 系統身分權限角色：ROLE_SUPER_ADMIN（最高管理者）、ROLE_ADMIN（管理者）、ROLE_USER（一般交易員） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    /** 帳戶當前資產餘額（預設開戶給予 100,000.00） */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal balance = BigDecimal.valueOf(100000.00);

    /** 比特幣閃電網路節點 Compressed Public Key (33-byte hex) */
    @Column(name = "lightning_pubkey", length = 255)
    private String lightningPubkey;

    /** 帳戶啟用狀態：ACTIVE（啟用）、SUSPENDED（凍結）、PENDING_APPROVAL（等待核准） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    /** 帳號建立時間戳記 (UTC) */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 帳號最後更新時間戳記 (UTC) */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public UserAccount() {
    }

    public UserAccount(Long id, String username, String email, String password, String fullName, UserRole role,
                       BigDecimal balance, String lightningPubkey, AccountStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.balance = balance != null ? balance : BigDecimal.valueOf(100000.00);
        this.lightningPubkey = lightningPubkey;
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private String password;
        private String fullName;
        private UserRole role;
        private BigDecimal balance = BigDecimal.valueOf(100000.00);
        private String lightningPubkey;
        private AccountStatus status = AccountStatus.ACTIVE;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder balance(BigDecimal balance) { if (balance != null) this.balance = balance; return this; }
        public Builder lightningPubkey(String lightningPubkey) { this.lightningPubkey = lightningPubkey; return this; }
        public Builder status(AccountStatus status) { if (status != null) this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserAccount build() {
            return new UserAccount(id, username, email, password, fullName, role, balance, lightningPubkey, status, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getLightningPubkey() { return lightningPubkey; }
    public void setLightningPubkey(String lightningPubkey) { this.lightningPubkey = lightningPubkey; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
