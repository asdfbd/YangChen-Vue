package com.yangchen.ai.context;

import com.yangchen.common.core.redis.RedisCache;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Redis 中的一分钟工具确认令牌。 */
@Component
public class ToolApprovalRegistry {

    // 升级键前缀，避免读取此前 record 序列化写入的无 @class 历史值。
    private static final String KEY_PREFIX = "ai:tool:approval:v2:";
    private static final String USED_KEY_SUFFIX = ":used";
    private static final int TTL_MINUTES = 1;

    private final RedisCache redisCache;

    public ToolApprovalRegistry(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public PendingApproval create(String conversationId, List<AssistantMessage.ToolCall> calls) {
        PendingApproval approval = new PendingApproval(
                UUID.randomUUID().toString(), conversationId, fingerprints(calls));
        redisCache.setCacheObject(key(approval.getApprovalId()), approval, TTL_MINUTES, TimeUnit.MINUTES);
        return approval;
    }

    /**
     * 原子抢占一次性消费标记，再校验 Redis 中的调用指纹。
     * 令牌被抢占后无论是否匹配都不可复用，防止参数被篡改后反复尝试。
     */
    public boolean consumeIfMatches(String approvalId, String conversationId,
                                    List<AssistantMessage.ToolCall> calls) {
        if (approvalId == null || approvalId.isBlank()) {
            return false;
        }
        PendingApproval approval = redisCache.getCacheObject(key(approvalId));
        if (approval == null) {
            return false;
        }
        Boolean firstConsumer = redisCache.redisTemplate.opsForValue()
                .setIfAbsent(key(approvalId) + USED_KEY_SUFFIX, "1", TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(firstConsumer)) {
            return false;
        }
        redisCache.deleteObject(key(approvalId));
        return Objects.equals(approval.getConversationId(), conversationId)
                && approval.getCallFingerprints().equals(fingerprints(calls));
    }

    private String key(String approvalId) {
        return KEY_PREFIX + approvalId;
    }

    private List<String> fingerprints(List<AssistantMessage.ToolCall> calls) {
        return calls.stream()
                .map(call -> sha256(call.name() + "\n" + Objects.toString(call.arguments(), "")))
                .toList();
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    /**
     * 不能使用 record：项目的 GenericJackson2JsonRedisSerializer 对 final 类型不会
     * 写入 @class，但读取目标是 Object 时又需要该类型字段。普通 POJO 可正常序列化。
     */
    public static class PendingApproval {
        private String approvalId;
        private String conversationId;
        private List<String> callFingerprints;

        public PendingApproval() {
        }

        public PendingApproval(String approvalId, String conversationId,
                               List<String> callFingerprints) {
            this.approvalId = approvalId;
            this.conversationId = conversationId;
            this.callFingerprints = callFingerprints;
        }

        public String getApprovalId() {
            return approvalId;
        }

        public void setApprovalId(String approvalId) {
            this.approvalId = approvalId;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public List<String> getCallFingerprints() {
            return callFingerprints;
        }

        public void setCallFingerprints(List<String> callFingerprints) {
            this.callFingerprints = callFingerprints;
        }
    }
}
