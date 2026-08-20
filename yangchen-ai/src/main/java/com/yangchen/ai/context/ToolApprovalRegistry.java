package com.yangchen.ai.context;

import com.yangchen.common.core.redis.RedisCache;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Redis 中的一分钟工具确认令牌。 */
@Component
public class ToolApprovalRegistry {

    // 升级键前缀，避免读取此前 record 或旧字段序列化写入的历史值。
    private static final String KEY_PREFIX = "ai:tool:approval:v3:";
    private static final String USED_KEY_SUFFIX = ":used";
    private static final String ACTIVE_KEY_SUFFIX = ":active";
    private static final int TTL_MINUTES = 1;

    private final RedisCache redisCache;

    public ToolApprovalRegistry(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public PendingApproval create(String conversationId) {
        PendingApproval approval = new PendingApproval(UUID.randomUUID().toString(), conversationId);
        redisCache.setCacheObject(key(approval.getApprovalId()), approval, TTL_MINUTES, TimeUnit.MINUTES);
        return approval;
    }

    /**
     * 激活一次确认，并把授权限定在当前 HTTP 对话流内。
     *
     * <p>一次“查询用户列表”通常会连续调用表目录、表字段、数据查询多个工具。若第一个
     * 工具调用后立刻删除令牌，后续工具又会被判定为未确认，从而不停弹确认框。因此首个
     * 调用校验通过后将令牌标记为 active；控制器在本次流结束时立即清除 active 标记。</p>
     */
    public boolean activateForCurrentConversation(String approvalId, String conversationId) {
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
        boolean matches = Objects.equals(approval.getConversationId(), conversationId);
        if (!matches) {
            return false;
        }
        redisCache.setCacheObject(activeKey(approvalId), approval, TTL_MINUTES, TimeUnit.MINUTES);
        redisCache.deleteObject(key(approvalId));
        return true;
    }

    /** 当前对话流已经通过首个工具的确认，允许其后续工具继续执行。 */
    public boolean isActive(String approvalId, String conversationId) {
        if (approvalId == null || approvalId.isBlank()) {
            return false;
        }
        PendingApproval approval = redisCache.getCacheObject(activeKey(approvalId));
        return approval != null && Objects.equals(approval.getConversationId(), conversationId);
    }

    /** 聊天流结束或被取消时调用，令牌不能用于下一次独立请求。 */
    public void clearActive(String approvalId) {
        if (approvalId == null || approvalId.isBlank()) {
            return;
        }
        redisCache.deleteObject(activeKey(approvalId));
        redisCache.deleteObject(key(approvalId) + USED_KEY_SUFFIX);
    }

    private String key(String approvalId) {
        return KEY_PREFIX + approvalId;
    }

    private String activeKey(String approvalId) {
        return key(approvalId) + ACTIVE_KEY_SUFFIX;
    }

    /**
     * 不能使用 record：项目的 GenericJackson2JsonRedisSerializer 对 final 类型不会
     * 写入 @class，但读取目标是 Object 时又需要该类型字段。普通 POJO 可正常序列化。
     */
    public static class PendingApproval {
        private String approvalId;
        private String conversationId;
        public PendingApproval() {
        }

        public PendingApproval(String approvalId, String conversationId) {
            this.approvalId = approvalId;
            this.conversationId = conversationId;
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

    }
}
