package com.lightning.trading.service;

import com.lightning.trading.entity.AuditLog;
import com.lightning.trading.repo.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 審計日誌服務 (AuditService)
 * 負責處理系統審計日誌的寫入與查詢業務邏輯。
 * 因無多重實作需求，依據簡潔架構原則直接定義為具體服務類別 (@Service)，無需額外建立 Interface 與 Impl。
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 建構子注入 AuditLogRepository
     * 
     * @param auditLogRepository 審計日誌資料庫儲存庫
     */
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 記錄單筆審計日誌
     * <p>
     * 使用 Propagation.REQUIRES_NEW 開啟獨立事務，確保即使主業務交易異常回滾，
     * 審計操作紀錄依然能被完整保存至資料庫中。
     * </p>
     *
     * @param action          執行的業務操作或方法名稱
     * @param operator        操作人員的使用者名稱
     * @param clientIp        客戶端來源真實 IP
     * @param method          HTTP 請求方法 (GET, POST 等)
     * @param endpoint        請求路徑或方法簽章
     * @param executionTimeMs 方法執行耗時 (毫秒)
     * @param status          執行結果狀態 (SUCCESS / FAILURE)
     * @param details         附加參數或例外錯誤訊息
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(String action, String operator, String clientIp, String method,
                            String endpoint, Long executionTimeMs, String status, String details) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .operatorUsername(operator)
                .clientIp(clientIp)
                .httpMethod(method)
                .endpoint(endpoint)
                .executionTimeMs(executionTimeMs)
                .status(status)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    /**
     * 取得最近的審計日誌列表
     * <p>
     * 依據建立時間 (createdAt) 由新到舊排序，回傳指定筆數的日誌記錄。
     * </p>
     *
     * @param limit 限制回傳的日誌筆數 (預設查前 100 筆並依此參數截取)
     * @return 審計日誌清單
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs(int limit) {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .toList();
    }
}
