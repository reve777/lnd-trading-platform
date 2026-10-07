package com.lightning.trading.controller;

import com.lightning.trading.entity.AuditLog;
import com.lightning.trading.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 審計日誌控制器 (AuditLogController)
 * 提供前端或管理後台查詢系統內部各項操作與交易之審計紀錄的 REST API 接口。
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditService auditService;

    /**
     * 建構子注入 AuditService 服務
     * 遵循 Spring 推薦的建構子注入原則，確保依賴不可變 (final) 並提高模組的可測試性。
     *
     * @param auditService 審計日誌業務邏輯處理服務
     */
    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * 查詢最近的審計日誌清單
     *
     * 【端點路徑】：GET /api/audit/logs
     * 【功能說明】：向後端查詢最近記錄的系統操作審計日誌（包含操作人員、動作名稱、請求端點、客戶端 IP、耗時與成功/失敗狀態等）。
     *
     * @param limit 限制回傳的日誌數量（非必填，預設為 50 筆）
     * @return HTTP 200 OK 包含 AuditLog 實體物件清單
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getRecentAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditService.getRecentAuditLogs(limit));
    }
}
