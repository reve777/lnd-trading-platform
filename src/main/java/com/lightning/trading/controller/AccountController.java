package com.lightning.trading.controller;

import com.lightning.trading.aop.AuditAction;
import com.lightning.trading.aop.RateLimit;
import com.lightning.trading.dto.CreateUserRequest;
import com.lightning.trading.entity.UserAccount;
import com.lightning.trading.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 帳戶管理控制器 (AccountController)
 * 提供使用者帳戶之查詢、新用戶開戶註冊、以及資金餘額調整等 REST API 接口。
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final UserService userService;

    /**
     * 建構子注入 UserService
     *
     * @param userService 使用者帳戶業務處理服務
     */
    public AccountController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查詢全系統所有使用者帳戶清單
     *
     * 【端點路徑】：GET /api/accounts
     * 【功能邏輯】：從資料庫讀取所有註冊用戶的詳細資料（包含帳號、身分角色、現有餘額、閃電網路公鑰與帳號狀態），依建立時間倒序排序。
     *
     * @return 包含所有 UserAccount 物件之清單
     */
    @GetMapping
    public ResponseEntity<List<UserAccount>> getAllAccounts() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * 依據使用者名稱查詢特定帳戶詳情
     *
     * 【端點路徑】：GET /api/accounts/{username}
     * 【功能邏輯】：依據路徑參數指定的帳號名稱進行精確查詢；若帳號不存在，Service 層將拋出 IllegalArgumentException。
     *
     * @param username 使用者帳號 (如 super_admin, user_a)
     * @return 指定的使用者帳戶實體
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserAccount> getAccount(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }

    /**
     * 註冊建立新使用者帳戶
     *
     * 【端點路徑】：POST /api/accounts
     * 【功能邏輯】：
     * 1. 透過 @RateLimit 限制單一 IP 於 60 秒內最多發起 30 次開戶請求，防止惡意批量註冊。
     * 2. 透過 @AuditAction 自動在切面寫入 ACCOUNT_CREATE 審計紀錄。
     * 3. 檢查帳號與 Email 唯一性，對密碼進行 BCrypt 加密雜湊，並給予初始設定餘額。
     *
     * @param request 包含帳號、Email、密碼、全名、角色與初始額度之請求體
     * @return 註冊成功後的 UserAccount 實體
     */
    @PostMapping
    @AuditAction("ACCOUNT_CREATE")
    @RateLimit(maxRequests = 30, windowSeconds = 60)
    public ResponseEntity<UserAccount> createAccount(@Valid @RequestBody CreateUserRequest request) {
        UserAccount created = userService.createUser(request);
        return ResponseEntity.ok(created);
    }

    /**
     * 調整指定帳戶之餘額（加款或扣款）
     *
     * 【端點路徑】：POST /api/accounts/{username}/adjust-balance
     * 【功能邏輯】：
     * 1. 接收 delta 金額（正數代表儲值加款，負數代表扣款提現）。
     * 2. 驗證帳戶扣款後餘額不可小於 0（防超額透支）。
     * 3. 透過 @AuditAction 記錄 ACCOUNT_BALANCE_ADJUST 審計日誌以利後續稽核追蹤。
     *
     * @param username 欲調整餘額的使用者帳號
     * @param payload  JSON Map 結構，包含 "delta" 變動金額 (BigDecimal)
     * @return 餘額調整後的最新 UserAccount 物件
     */
    @PostMapping("/{username}/adjust-balance")
    @AuditAction("ACCOUNT_BALANCE_ADJUST")
    public ResponseEntity<UserAccount> adjustBalance(
            @PathVariable String username,
            @RequestBody Map<String, BigDecimal> payload) {
        BigDecimal delta = payload.getOrDefault("delta", BigDecimal.ZERO);
        UserAccount updated = userService.adjustBalance(username, delta);
        return ResponseEntity.ok(updated);
    }
}
