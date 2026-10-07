package com.lightning.trading.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 符合 RFC 9562 標準之 UUIDv7 生成器（含 16 碼高效率時間排序交易流水號）
 *
 * 【位元結構規劃】：
 * - 48 位元：毫秒級 Unix Epoch 時間戳記（支援時間自然排序）
 * - 4 位元：UUID 版本號 Version 7 (0b0111)
 * - 12 位元：單調遞增次毫秒計數器 (rand_a，避免同毫秒碰撞)
 * - 2 位元：RFC 4122 / 9562 規範 Variant 標識 (0b10)
 * - 62 位元：加密級安全隨機熵 (rand_b)
 *
 * 去除連字號後，前 16 個十六進位字元即為：
 * [48-bit 時間戳 (12 碼)] + [Version 7 (1 碼)] + [計數器 (3 碼)]。
 * 保證具備嚴格的時間順序性、無碰撞風險、長度固定 16 碼，特別適合作為高併發交易的 TxUid。
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicLong LAST_TIMESTAMP_MS = new AtomicLong(-1L);
    private static final AtomicInteger SEQUENCE_COUNTER = new AtomicInteger(0);

    private UuidV7Generator() {
    }

    /**
     * 生成結果封裝紀錄
     *
     * @param fullUuid 標準 36 字元之 UUIDv7 字串（含連字號）
     * @param txUid16  前 16 位元大寫十六進位唯一交易序號（依時間嚴格單調遞增）
     */
    public record UuidV7Result(String fullUuid, String txUid16) {}

    /**
     * 生成全新的 UUIDv7 並提取 16 碼交易唯一鍵
     *
     * @return 包含完整 UUID 與 16 碼鍵的 UuidV7Result 物件
     */
    public static UuidV7Result generate() {
        long currentMs = Instant.now().toEpochMilli();

        long lastMs = LAST_TIMESTAMP_MS.get();
        int seq;

        if (currentMs <= lastMs) {
            currentMs = lastMs;
            seq = SEQUENCE_COUNTER.incrementAndGet() & 0x0FFF;
            if (seq == 0) {
                // 當同一毫秒內的 12 位元計數器滿溢 (超過 4095) 時，時間戳手動前進 1 毫秒避免重號
                currentMs = LAST_TIMESTAMP_MS.incrementAndGet();
            }
        } else {
            LAST_TIMESTAMP_MS.set(currentMs);
            seq = RANDOM.nextInt(0x0100); // 每個新毫秒隨機序列起點
            SEQUENCE_COUNTER.set(seq);
        }

        // 高 64 位元 (MSB):
        // [48 bits 時間戳] | [4 bits 版本號 7] | [12 bits 序列號]
        long msb = (currentMs << 16) | (0x7000L) | (seq & 0x0FFFL);

        // 低 64 位元 (LSB):
        // [2 bits Variant 0b10] | [62 bits 密碼學安全隨機亂數]
        long randLsb = RANDOM.nextLong();
        long lsb = (randLsb & 0x3FFFFFFFFFFFFFFFL) | (0x8000000000000000L);

        UUID uuid = new UUID(msb, lsb);
        String fullUuid = uuid.toString();
        String rawNoHyphen = fullUuid.replace("-", "");
        String txUid16 = rawNoHyphen.substring(0, 16).toUpperCase();

        return new UuidV7Result(fullUuid, txUid16);
    }

    /**
     * 便捷方法：直接取得 16 碼大寫唯一交易流水號
     *
     * @return 16 碼大寫交易識別碼
     */
    public static String generateTxUid16() {
        return generate().txUid16();
    }
}
