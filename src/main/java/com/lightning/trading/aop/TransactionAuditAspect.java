package com.lightning.trading.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightning.trading.service.AuditService;
import com.lightning.trading.util.TradeConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;
import java.util.Arrays;

/**
 * 交易與操作審計切面 (TransactionAuditAspect)
 * 攔截標註了 @AuditAction 註解的業務方法，自動記錄操作者、客戶端 IP、HTTP 請求路徑、耗時、脫敏後的輸入參數與執行狀態。
 */
@Aspect
@Component
public class TransactionAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(TransactionAuditAspect.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public TransactionAuditAspect(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * 環繞通知：自動採集並記錄審計日誌
     *
     * 【功能邏輯】：
     * 1. 記錄開始時間戳記，提取動作名稱。
     * 2. 從 Spring 上下文中取得當前 HTTP 請求資訊（IP、HTTP Method、URI、操作人身分）。
     * 3. 對輸入參數進行敏感資訊脫敏處理（過濾密碼與 Macaroon 憑證密鑰）。
     * 4. 執行目標方法 (proceed())。
     * 5. 若成功，計算耗時並呼叫 AuditService 記錄 SUCCESS 審計紀錄。
     * 6. 若拋出例外，計算耗時並記錄 FAILURE 狀態與異常訊息，隨後重新向上拋出例外。
     *
     * @param joinPoint   切入點
     * @param auditAction @AuditAction 註解中定義的操作動作名稱
     * @return 原業務方法之回傳結果
     * @throws Throwable 原方法執行過程之例外
     */
    @Around("@annotation(auditAction)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        long startMs = System.currentTimeMillis();
        String actionName = auditAction.value();

        HttpServletRequest request = null;
        String clientIp = TradeConstants.DEFAULT_CLIENT_IP;
        String httpMethod = TradeConstants.DEFAULT_HTTP_METHOD_INTERNAL;
        String endpoint = joinPoint.getSignature().toShortString();
        String operator = TradeConstants.DEFAULT_OPERATOR_ANONYMOUS;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            request = attrs.getRequest();
            clientIp = extractClientIp(request);
            httpMethod = request.getMethod();
            endpoint = request.getRequestURI();
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                operator = userPrincipal.getName();
            }
        }

        // 參數敏感資料脫敏遮蔽
        String maskedArgs = maskArgs(joinPoint.getArgs());

        Object result;
        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startMs;

            auditService.recordAudit(
                    actionName,
                    operator,
                    clientIp,
                    httpMethod,
                    endpoint,
                    duration,
                    TradeConstants.STATUS_SUCCESS,
                    "Args: " + maskedArgs
            );
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startMs;
            auditService.recordAudit(
                    actionName,
                    operator,
                    clientIp,
                    httpMethod,
                    endpoint,
                    duration,
                    TradeConstants.STATUS_FAILURE,
                    "Error: " + ex.getMessage() + " | Args: " + maskedArgs
            );
            throw ex;
        }
    }

    /**
     * 從 HTTP 請求標頭中提取客戶端 IP
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 參數脫敏：過濾密碼 (password) 與閃電網路憑證密鑰 (macaroonHex) 等敏感欄位
     */
    private String maskArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        try {
            String json = objectMapper.writeValueAsString(args);
            return json.replaceAll("\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"***MASKED***\"")
                       .replaceAll("\"macaroonHex\"\\s*:\\s*\"[^\"]+\"", "\"macaroonHex\":\"***MASKED***\"");
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }
}
