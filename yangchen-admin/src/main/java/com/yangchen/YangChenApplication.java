package com.yangchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 *
 * @author yangchen
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class YangChenApplication {
    public static void main(String[] args) {
        SpringApplication.run(YangChenApplication.class, args);
    }
}
