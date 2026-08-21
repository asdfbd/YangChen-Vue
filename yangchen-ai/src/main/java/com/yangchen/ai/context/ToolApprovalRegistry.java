package com.yangchen.ai.context;

import com.yangchen.common.core.redis.RedisCache;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 中的一分钟工具确认令牌。
 */
@Component
public class ToolApprovalRegistry {

    // 只存 String，避免 DevTools 热重启后 Redis 反序列化出旧 ClassLoader 的对象。
    private static final String KEY_PREFIX = "ai:tool:approval:v4:";
    private static final String USED_KEY_SUFFIX = ":used";
    private static final String ACTIVE_KEY_SUFFIX = ":active";
    private static final int TTL_MINUTES = 1;

    private final RedisCache redisCache;

    public ToolApprovalRegistry(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public PendingApproval create(String conversationId) {
        PendingApproval approval = new PendingApproval(UUID.randomUUID().toString(), conversationId);
        redisCache.setCacheObject(key(approval.getApprovalId()), conversationId, TTL_MINUTES, TimeUnit.MINUTES);
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
        String approvedConversationId = redisCache.getCacheObject(key(approvalId));
        if (approvedConversationId == null) {
            return false;
        }
        Boolean firstConsumer = redisCache.redisTemplate.opsForValue()
                .setIfAbsent(key(approvalId) + USED_KEY_SUFFIX, "1", TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(firstConsumer)) {
            return false;
        }
        boolean matches = Objects.equals(approvedConversationId, conversationId);
        if (!matches) {
            return false;
        }
        redisCache.setCacheObject(activeKey(approvalId), conversationId, TTL_MINUTES, TimeUnit.MINUTES);
        redisCache.deleteObject(key(approvalId));
        return true;
    }

    /**
     * 当前对话流已经通过首个工具的确认，允许其后续工具继续执行。
     */
    public boolean isActive(String approvalId, String conversationId) {
        if (approvalId == null || approvalId.isBlank()) {
            return false;
        }
        String approvedConversationId = redisCache.getCacheObject(activeKey(approvalId));
        return Objects.equals(approvedConversationId, conversationId);
    }

    /**
     * 聊天流结束或被取消时调用，令牌不能用于下一次独立请求。
     */
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
     * 仅用于把 approvalId 返回给调用方，不写入 Redis。
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
