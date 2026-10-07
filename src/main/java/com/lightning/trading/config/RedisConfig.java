package com.lightning.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * 自訂 RedisTemplate Bean，方便在專案中操作 Redis
     * 預設的 RedisTemplate 採用 JdkSerializationRedisSerializer，容易出現二進位亂碼且跨語言不友善
     * 此處統一調整為：Key 使用字串序列化，Value 使用 JSON 格式序列化
     *
     * @param connectionFactory Redis 連線工廠（由 Spring Boot 自動注入，通常底層是 Lettuce 或 Jedis）
     * @return 配置完成的 RedisTemplate 實例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 建立 RedisTemplate 實例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 設定底層連線工廠
        template.setConnectionFactory(connectionFactory);

        // 設定常規 Key 的序列化器：使用純字串格式（避免在 Redis Client 看到 \xac\xed 等亂碼前綴）
        template.setKeySerializer(new StringRedisSerializer());

        // 設定常規 Value 的序列化器：使用 JSON 格式（GenericJackson2JsonRedisSerializer 會在 JSON 中夾帶 @class 屬性，反序列化時能自動轉回原物件類型）
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 設定 Hash 結構中 Key（即 field）的序列化器：使用純字串格式
        template.setHashKeySerializer(new StringRedisSerializer());

        // 設定 Hash 結構中 Value 的序列化器：使用 JSON 格式
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 初始化內部屬性與檢查設定（確保所有必要的序列化器與連線工廠已正確配置）
        template.afterPropertiesSet();

        return template;
    }
}