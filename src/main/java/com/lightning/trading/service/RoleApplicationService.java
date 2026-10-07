package com.lightning.trading.service;

import com.lightning.trading.dto.ReviewRoleApplicationRequest;
import com.lightning.trading.dto.RoleApplicationRequest;
import com.lightning.trading.entity.RoleApplication;
import com.lightning.trading.entity.UserAccount;
import com.lightning.trading.repo.RoleApplicationRepository;
import com.lightning.trading.repo.UserAccountRepository;
import com.lightning.trading.util.ApplicationStatus;
import com.lightning.trading.util.TradeConstants;
import com.lightning.trading.util.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 角色權限申請與審核服務 (RoleApplicationService)
 * 負責處理使用者申請升級權限（如申請成為 Trader / Admin）以及最高管理員審核之業務邏輯。
 * 因無多重實作需求，直接定義為具體服務類別 (@Service)，無需額外的 Interface 與 Impl。
 */
@Service
public class RoleApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RoleApplicationService.class);

    private final RoleApplicationRepository roleApplicationRepository;
    private final UserAccountRepository userAccountRepository;

    public RoleApplicationService(RoleApplicationRepository roleApplicationRepository,
                                  UserAccountRepository userAccountRepository) {
        this.roleApplicationRepository = roleApplicationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * 提交角色權限申請
     *
     * @param request 包含申請人帳號、欲申請角色與申請事由之請求物件
     * @return 儲存成功後的角色申請實體物件
     */
    @Transactional
    public RoleApplication submitApplication(RoleApplicationRequest request) {
        UserAccount applicant = userAccountRepository.findByUsername(request.applicantUsername())
                .orElseThrow(() -> new IllegalArgumentException("User '" + request.applicantUsername() + "' not found."));

        RoleApplication application = RoleApplication.builder()
                .applicant(applicant)
                .requestedRole(request.requestedRole())
                .reason(request.reason().trim())
                .status(ApplicationStatus.PENDING)
                .build();

        RoleApplication saved = roleApplicationRepository.save(application);
        log.info("Role application submitted by {} for role {}", applicant.getUsername(), request.requestedRole());
        return saved;
    }

    /**
     * 審核角色權限申請
     * 僅允許具備 ROLE_SUPER_ADMIN 權限的管理員進行審核；若審核通過 (APPROVED)，會自動更新申請人的系統角色。
     *
     * @param request 包含申請單號、審核者帳號、審核結果 (APPROVED/REJECTED) 與備註
     * @return 審核完成後的角色申請實體物件
     */
    @Transactional
    public RoleApplication reviewApplication(ReviewRoleApplicationRequest request) {
        RoleApplication application = roleApplicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application ID " + request.applicationId() + " not found."));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException(TradeConstants.ERR_APPLICATION_ALREADY_REVIEWED + " (當前狀態: " + application.getStatus() + ")");
        }

        UserAccount reviewer = userAccountRepository.findByUsername(request.reviewerUsername())
                .orElseThrow(() -> new IllegalArgumentException("Reviewer '" + request.reviewerUsername() + "' not found."));

        if (reviewer.getRole() != UserRole.ROLE_SUPER_ADMIN) {
            throw new IllegalStateException(TradeConstants.ERR_SUPER_ADMIN_ONLY_REVIEW);
        }

        application.setStatus(request.decision());
        application.setReviewedBy(reviewer);
        application.setReviewNotes(request.reviewNotes());
        application.setReviewedAt(Instant.now());

        // 若審核通過，同步更新 UserAccount 中的實際角色
        if (request.decision() == ApplicationStatus.APPROVED) {
            UserAccount applicant = application.getApplicant();
            applicant.setRole(application.getRequestedRole());
            userAccountRepository.save(applicant);
            log.info("Role application #{} APPROVED: User {} role updated to {}",
                    application.getId(), applicant.getUsername(), application.getRequestedRole());
        } else {
            log.info("Role application #{} REJECTED for user {}", application.getId(), application.getApplicant().getUsername());
        }

        return roleApplicationRepository.save(application);
    }

    /**
     * 取得全系統所有的角色申請紀錄（依建立時間倒序）
     *
     * @return 角色申請清單
     */
    @Transactional(readOnly = true)
    public List<RoleApplication> getAllApplications() {
        return roleApplicationRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 取得所有等待審核中 (PENDING) 的角色申請紀錄
     *
     * @return 待審核清單
     */
    @Transactional(readOnly = true)
    public List<RoleApplication> getPendingApplications() {
        return roleApplicationRepository.findByStatusOrderByCreatedAtDesc(ApplicationStatus.PENDING);
    }
}
