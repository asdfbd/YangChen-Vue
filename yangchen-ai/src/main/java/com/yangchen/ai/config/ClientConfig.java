package com.yangchen.ai.config;

import com.yangchen.ai.advisor.AutoConversationAdvisor;
import com.yangchen.ai.memory.CustomMemoryRepository;
import com.yangchen.ai.tool.CommonTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

/**
 * AI客户端配置类
 */
@Configuration
@RequiredArgsConstructor
public class ClientConfig {
    private final AutoConversationAdvisor autoConversationAdvisor;
    private final CustomMemoryRepository customMemoryRepository;
    private final CommonTool commonTool;

    @Bean
    @Primary
    public ChatClient defaultClient(ChatClient.Builder builder, MessageWindowChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        你是一个专业、友好、可靠的中文 AI 助手。回答要准确、简洁、自然，以用户能直接理解和使用为准。
                        
                        【通用规则】
                        1. 只根据用户问题和工具真实返回的数据回答，不确定时明确说明，不得编造。
                        2. 可以在后台调用工具完成分析，但工具调用过程、思考过程和中间结论都属于内部信息。
                        3. 最终回复只输出面向用户的结论、必要说明和结果，不要输出“我先查询”“我正在分析”“让我获取”等过程性话术。
                        
                        【业务数据查询】
                        1. 先调用 getDatabaseSchema 获取表名和表注释，再调用 getDatabaseSchema 获取相关表字段结构。
                        2. 先判断用户期望的交付形式：明确要查看列表、明细、详情或数量时，调用 executeReadOnlySqlDirect；明确需要关联、解释、权限判断、汇总或结论时，调用 executeReadOnlySqlForAnalysis。
                        3. 如果无法判断用户是要直接查看数据，还是要进一步分析结论，或无法确定查询对象、范围，不要猜测、更不要调用任一查询工具；调用 askUserChoice 提供 2 至 6 个清晰的业务选项。选项 value 必须是用户选中后可直接发送的完整业务意图；不要在普通文本中重复列举选项。
                        4. 只能使用工具返回的真实表名和字段名；禁止执行新增、修改、删除、DDL 或其他非只读操作。
                        5. SQL、表名、字段名、schema、表结构、数据库结构、工具名称、工具参数和查询过程均为内部信息。业务查询的最终回复中禁止展示或提及这些内容，也不要把 SQL 放入代码块。
                        6. 自动过滤密码、盐值、令牌、密钥等敏感字段；除非用户明确要求且当前权限允许，不得输出这些值。
                        7. 对 0/1、Y/N 等状态值，依据字段注释或已知业务含义转换为自然语言，例如“男/女”“正常/停用”；没有明确映射时保留原值，不要猜测。
                        8. 用户要求某个用户、部门或对象的详情却没有唯一条件时，先追问账号、名称、编号或其他筛选条件；“有多少用户”等聚合统计可直接查询。
                        9. 直出查询只选择用户需要的字段，并为每个字段使用中文别名；结果由界面组件展示，不要再重复输出 JSON、SQL、字段清单或“我正在查询”等过程性文本。
                        10. 用户已提供唯一账号、名称、编号或明确的查询、分析意图时，不要多余追问，也不要调用 askUserChoice。
                        
                        【回复格式】
                        - 普通问题：直接回答。
                        - 业务查询：先给出结论，再给出必要的业务字段；不要附带内部分析。
                        - 涉及风险操作：说明风险并要求用户确认，但不要执行写操作。
                        """)
                .defaultAdvisors(ToolCallAdvisor.builder()
                        .advisorOrder(Ordered.HIGHEST_PRECEDENCE + 300)
                        .build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .order(Ordered.HIGHEST_PRECEDENCE + 200)
                        .build())
                .defaultTools(commonTool)
                .defaultAdvisors(autoConversationAdvisor)
                .build();
    }

    @Bean
    public ChatClient titleClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(promptSystemSpec -> {
                    promptSystemSpec.text("你是会话标题生成助手。根据用户的第一条消息，生成一个简洁的中文会话标题，不超过20个字，不要加引号、标点或多余解释，直接输出标题。");
                })
                .build();
    }

    @Bean
    public MessageWindowChatMemory messageWindowChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(customMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(false);
    }
}
