package com.lightning.trading.entity;

import com.lightning.trading.util.ApplicationStatus;
import com.lightning.trading.util.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 角色權限申請單實體 (RoleApplication)
 * <p>
 * 記錄使用者發起的身分角色提升申請（如申請由 USER 晉升為 TRADER/ADMIN），以及管理員審核之歷程與審批決策。
 * </p>
 */
@Entity
@Table(name = "role_applications", indexes = {
        @Index(name = "idx_role_app_status", columnList = "status"),
        @Index(name = "idx_role_app_applicant", columnList = "applicant_id")
})
public class RoleApplication {

    /** 申請單自增主鍵 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申請人使用者帳戶 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private UserAccount applicant;

    /** 申請目標角色（如 ROLE_ADMIN） */
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", nullable = false, length = 30)
    private UserRole requestedRole;

    /** 申請事由或原因說明 */
    @Column(nullable = false, length = 500)
    private String reason;

    /** 申請審核狀態（PENDING: 待審核, APPROVED: 已核准, REJECTED: 已駁回） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    /** 審核該申請單之管理員帳戶 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private UserAccount reviewedBy;

    /** 管理員審核備註或駁回原因 */
    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    /** 申請單提出時間戳記 (UTC) */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 審核完成時間戳記 (UTC) */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public RoleApplication() {
    }

    public RoleApplication(Long id, UserAccount applicant, UserRole requestedRole, String reason,
                           ApplicationStatus status, UserAccount reviewedBy, String reviewNotes,
                           Instant createdAt, Instant reviewedAt) {
        this.id = id;
        this.applicant = applicant;
        this.requestedRole = requestedRole;
        this.reason = reason;
        this.status = status != null ? status : ApplicationStatus.PENDING;
        this.reviewedBy = reviewedBy;
        this.reviewNotes = reviewNotes;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private UserAccount applicant;
        private UserRole requestedRole;
        private String reason;
        private ApplicationStatus status = ApplicationStatus.PENDING;
        private UserAccount reviewedBy;
        private String reviewNotes;
        private Instant createdAt;
        private Instant reviewedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder applicant(UserAccount applicant) { this.applicant = applicant; return this; }
        public Builder requestedRole(UserRole requestedRole) { this.requestedRole = requestedRole; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder status(ApplicationStatus status) { if (status != null) this.status = status; return this; }
        public Builder reviewedBy(UserAccount reviewedBy) { this.reviewedBy = reviewedBy; return this; }
        public Builder reviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder reviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; return this; }

        public RoleApplication build() {
            return new RoleApplication(id, applicant, requestedRole, reason, status, reviewedBy, reviewNotes, createdAt, reviewedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserAccount getApplicant() { return applicant; }
    public void setApplicant(UserAccount applicant) { this.applicant = applicant; }

    public UserRole getRequestedRole() { return requestedRole; }
    public void setRequestedRole(UserRole requestedRole) { this.requestedRole = requestedRole; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public UserAccount getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UserAccount reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}
