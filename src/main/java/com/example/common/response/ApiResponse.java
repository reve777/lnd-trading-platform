package com.example.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * 系統公用統一回應封裝結構 (ApiResponse)
 *
 * @param code      業務狀態碼 (如 "SUCCESS", "ORDER_NOT_FOUND")
 * @param message   提示訊息
 * @param data      泛型承載資料 (成功時有值，失敗時通常為 null)
 * @param timestamp 伺服器時間戳記 (UTC)
 * @param <T>       承載資料的型別
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    String code,          // 業務狀態碼 (如 "SUCCESS", "ORDER_NOT_FOUND")
    String message,       // 提示訊息
    T data,               // 泛型承載資料 (成功時有值，失敗時通常為 null)
    Instant timestamp     // 伺服器時間戳記 (UTC)
) {
    // 成功回應快速工廠方法
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "Operation successful", data, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, Instant.now());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("SUCCESS", "Operation successful", null, Instant.now());
    }

    // 失敗回應快速工廠方法
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message, T errorDetails) {
        return new ApiResponse<>(code, message, errorDetails, Instant.now());
    }
}
