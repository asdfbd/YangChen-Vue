package com.yangchen.ai.context;

import com.yangchen.ai.entity.vo.ChatResp;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具调用进度事件通道。
 *
 * <p>每个会话（conversationId）绑定一个 {@link Sinks.Many}，
 * 工具调用装饰器在真实执行前后发布 {@link ChatResp}(type=event)，
 * Controller 把该 Sink 的流与聊天文本流 {@code Flux.merge} 后合并输出，
 * 从而实现“正在查询 A / A 无记录，继续查询 B / 正在创建 C”的实时进度旁白。
 * 事件仅用于实时展示，不落库、不进记忆、刷新后不重放。</p>
 */
@Component
public class ToolProgressRegistry {

    private final Map<String, Sinks.Many<ChatResp>> sinks = new ConcurrentHashMap<>();

    /**
     * 绑定会话并返回其事件通道；重复绑定返回已有通道。
     */
    public Sinks.Many<ChatResp> bind(String conversationId) {
        return sinks.computeIfAbsent(conversationId,
                key -> Sinks.many().unicast().onBackpressureBuffer());
    }

    /**
     * 解绑并让通道完成，避免下游一直等待。
     */
    public void unbind(String conversationId) {
        Sinks.Many<ChatResp> sink = sinks.remove(conversationId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    public Optional<Sinks.Many<ChatResp>> sinkOf(String conversationId) {
        return Optional.ofNullable(conversationId == null ? null : sinks.get(conversationId));
    }

    /**
     * 向指定会话推送一条进度事件；会话未绑定或通道已关闭时静默忽略。
     */
    public void emit(String conversationId, ChatResp event) {
        if (conversationId == null) {
            return;
        }
        Sinks.Many<ChatResp> sink = sinks.get(conversationId);
        if (sink != null) {
            sink.tryEmitNext(event);
        }
    }

    /** 供控制器合并输出。 */
    public Flux<ChatResp> fluxOf(String conversationId) {
        Sinks.Many<ChatResp> sink = conversationId == null ? null : sinks.get(conversationId);
        return sink == null ? Flux.empty() : sink.asFlux();
    }
}
