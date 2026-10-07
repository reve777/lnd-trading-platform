package com.lightning.trading.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 全域例外狀況處理器 (GlobalExceptionHandler)
 * 統一攔截並處理 Controller 層拋出的各類異常，回傳標準化的錯誤 JSON 格式，避免洩漏底層敏感堆疊。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理 Spring 內建的 ResponseStatusException（例如頻率限制 429 Too Many Requests）
     *
     * @param ex ResponseStatusException 例外實體
     * @return 包含 HTTP 狀態碼與錯誤訊息之回應實體
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {}", ex.getReason());
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "status", ex.getStatusCode().value(),
                "error", ex.getStatusCode().toString(),
                "message", ex.getReason() != null ? ex.getReason() : "Request error"
        );
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    /**
     * 處理 @Valid 參數驗證失敗例外 (MethodArgumentNotValidException)
     *
     * 【功能邏輯】：萃取所有欄位驗證錯誤（如 @NotBlank, @Min 等），整理為 { "欄位名": "錯誤提示" } 格式回傳給前端。
     *
     * @param ex 驗證失敗例外物件
     * @return HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Validation Failed",
                "message", "Input validation failed: " + errors,
                "validationErrors", errors
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 處理非法參數例外 (IllegalArgumentException)
     *
     * 【功能邏輯】：當業務邏輯傳入不存在的使用者、重覆的帳號等無效參數時觸發，回傳 400 錯誤。
     *
     * @param ex 非法參數例外
     * @return HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", ex.getMessage()
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 處理非法狀態例外 (IllegalStateException)
     *
     * 【功能邏輯】：當違反業務狀態規則時觸發（例如餘額不足、已審核過之申請單重複審核等），回傳 409 Conflict。
     *
     * @param ex 狀態衝突例外
     * @return HTTP 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.CONFLICT.value(),
                "error", "Conflict / Business Rule Violation",
                "message", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * 處理全系統未捕捉的伺服器內部異常 (Exception)
     *
     * 【功能邏輯】：攔截未預期的底層錯誤，記錄伺服器日誌堆疊並回傳 500 Internal Server Error，保護系統細節不外洩。
     *
     * @param ex 通用未捕捉例外
     * @return HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled internal exception: ", ex);
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Internal Server Error",
                "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
