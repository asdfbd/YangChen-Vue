package com.yangchen.framework.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.TimeZone;

/**
 * 程序注解配置
 *
 * @author yangchen
 */
@Configuration
// 表示通过aop框架暴露该代理对象,AopContext能够访问
@EnableAspectJAutoProxy(exposeProxy = true)
// 指定要扫描的Mapper类的包的路径
@MapperScan("com.yangchen.**.mapper")
public class ApplicationConfig {
    /**
     * 时区配置 + 全局 Long 序列化为字符串
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
            // 全局将 Long 序列化为字符串，避免雪花 ID 在前端丢失精度（primitive long 不受影响，分页 total 仍是数字）
            jacksonObjectMapperBuilder.serializerByType(Long.class, new ToStringSerializer(Long.class));
        };
    }
}
