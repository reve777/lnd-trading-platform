package com.lightning.trading.aop;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 請求頻率限制切面 (RateLimiterAspect)
 * 攔截標註了 @RateLimit 註解的方法，依據「客戶端 IP + API 方法簽章」進行記憶體滑動/固定時間窗口限流。
 */
@Aspect
@Component
public class RateLimiterAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    /**
     * 內部時間窗口計數器
     */
    private static final class WindowCounter {
        long windowStartMs;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }
    }

    /**
     * 以執行緒安全的 ConcurrentHashMap 儲存各 IP 與各端點的呼叫計數
     */
    private final Map<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    /**
     * 環繞通知：執行限流檢查
     *
     * 【功能邏輯】：
     * 1. 從當前 HTTP 請求提取客戶端 IP。
     * 2. 組裝唯一限流鍵：方法名稱 + IP。
     * 3. 判斷當前請求時間是否已跨越設定的時間窗口 (windowSeconds)；若跨越則重置計數，否則原子遞增 (+1)。
     * 4. 若呼叫次數超過上限 (maxRequests)，記錄告警並拋出 429 Too Many Requests 異常。
     *
     * @param joinPoint 切入點
     * @param rateLimit 註解設定的上限次數與窗口秒數
     * @return 原業務方法之回傳結果
     * @throws Throwable 若超限拋出 ResponseStatusException，或原方法執行之例外
     */
    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String clientIp = "127.0.0.1";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            clientIp = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
        }

        String key = joinPoint.getSignature().toShortString() + ":" + clientIp;
        long now = System.currentTimeMillis();
        long windowMs = rateLimit.windowSeconds() * 1000L;

        WindowCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs > windowMs) {
                WindowCounter fresh = new WindowCounter(now);
                fresh.count.set(1);
                return fresh;
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        if (counter.count.get() > rateLimit.maxRequests()) {
            log.warn("[SECURITY ALERT] Rate limit exceeded for IP: {} on {}", clientIp, key);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Rate limit: " + rateLimit.maxRequests() + " requests per " + rateLimit.windowSeconds() + " seconds.");
        }

        return joinPoint.proceed();
    }
}
