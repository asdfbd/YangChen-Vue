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
     * 校验并消费一次用户确认（一次性）。
     *
     * <p>确认令牌只能被消费一次：本方法校验通过后立即删除 Redis 中的令牌与 used 标记，
     * 任何后续请求即使带上同一个 approvalId 也无法再次通过确认，必须重新弹出确认卡片。
     * 这样保证“每一次写操作都由一次真实的卡片确认驱动”，避免令牌跨轮复用导致
     * 未弹确认卡却被直接执行。</p>
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
        // 一次性确认：激活后立即清理所有令牌，杜绝同一个 approvalId 在 1 分钟窗口内
        // 被后续请求复用放行（否则会出现“本次没弹确认卡却被直接执行”）。
        redisCache.deleteObject(key(approvalId));
        redisCache.deleteObject(key(approvalId) + USED_KEY_SUFFIX);
        return true;
    }

    /**
     * 兼容旧调用：确认已消费后 active 标记不存在，恒返回 false。
     * 现在每次写操作都必须重新确认，不再支持“一次确认多次放行”。
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