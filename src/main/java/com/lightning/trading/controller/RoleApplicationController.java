package com.lightning.trading.controller;

import com.lightning.trading.aop.AuditAction;
import com.lightning.trading.aop.RateLimit;
import com.lightning.trading.dto.ReviewRoleApplicationRequest;
import com.lightning.trading.dto.RoleApplicationRequest;
import com.lightning.trading.dto.RoleApplicationResponse;
import com.lightning.trading.entity.RoleApplication;
import com.lightning.trading.service.RoleApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色權限申請與審核控制器 (RoleApplicationController)
 * 提供一般用戶申請升級身分角色（如申請升為 TRADER 或 ADMIN），以及超級管理員進行審批（核准/駁回）之 REST API 接口。
 */
@RestController
@RequestMapping("/api/roles")
public class RoleApplicationController {

    private final RoleApplicationService roleApplicationService;

    public RoleApplicationController(RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    /**
     * 提交角色權限升級申請
     *
     * 【端點路徑】：POST /api/roles/apply
     * 【功能邏輯】：
     * 1. 使用 @RateLimit 限制單一 IP 於 60 秒內最多發起 20 次申請，避免濫用。
     * 2. 驗證申請人是否存在，將狀態初始化為 PENDING（待審核）。
     * 3. 透過 @AuditAction 自動記錄 ROLE_APPLICATION_SUBMIT 審計日誌。
     *
     * @param request 包含申請人帳號、目標角色 (requestedRole) 與申請事由 (reason)
     * @return 申請單回應物件 (RoleApplicationResponse)
     */
    @PostMapping("/apply")
    @AuditAction("ROLE_APPLICATION_SUBMIT")
    @RateLimit(maxRequests = 20, windowSeconds = 60)
    public ResponseEntity<RoleApplicationResponse> submitApplication(@Valid @RequestBody RoleApplicationRequest request) {
        RoleApplication result = roleApplicationService.submitApplication(request);
        return ResponseEntity.ok(RoleApplicationResponse.fromEntity(result));
    }

    /**
     * 審核角色權限申請單
     *
     * 【端點路徑】：POST /api/roles/review
     * 【功能邏輯】：
     * 1. 嚴格驗證審核人員的身分角色必須為 ROLE_SUPER_ADMIN。
     * 2. 檢查該申請單是否處於 PENDING 狀態（防止重覆審核）。
     * 3. 若審核決定為 APPROVED，系統會連動更新該申請人帳戶於 UserAccount 表格中的實際身分角色。
     * 4. 透過 @AuditAction 記錄 ROLE_APPLICATION_REVIEW 審計追蹤。
     *
     * @param request 包含申請單 ID、審核人帳號、審查決定 (APPROVED/REJECTED) 與審核備註
     * @return 審核完成後的申請單資料
     */
    @PostMapping("/review")
    @AuditAction("ROLE_APPLICATION_REVIEW")
    public ResponseEntity<RoleApplicationResponse> reviewApplication(@Valid @RequestBody ReviewRoleApplicationRequest request) {
        RoleApplication result = roleApplicationService.reviewApplication(request);
        return ResponseEntity.ok(RoleApplicationResponse.fromEntity(result));
    }

    /**
     * 查詢全系統所有角色申請紀錄清單
     *
     * 【端點路徑】：GET /api/roles/applications
     * 【功能邏輯】：查詢所有已提交之申請單（含待審核、已核准、已駁回），依建立時間倒序排序。
     *
     * @return 所有申請單回應清單
     */
    @GetMapping("/applications")
    public ResponseEntity<List<RoleApplicationResponse>> getAllApplications() {
        List<RoleApplicationResponse> list = roleApplicationService.getAllApplications()
                .stream()
                .map(RoleApplicationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * 查詢所有待審核 (PENDING) 之角色申請單
     *
     * 【端點路徑】：GET /api/roles/applications/pending
     * 【功能邏輯】：過濾出目前仍處於 PENDING 狀態之申請單，供管理後台儀表板或審批人員優先處理。
     *
     * @return 待審核申請單清單
     */
    @GetMapping("/applications/pending")
    public ResponseEntity<List<RoleApplicationResponse>> getPendingApplications() {
        List<RoleApplicationResponse> list = roleApplicationService.getPendingApplications()
                .stream()
                .map(RoleApplicationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }
}
