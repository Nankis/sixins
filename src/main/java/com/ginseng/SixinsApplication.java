package com.ginseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
// 扫描mybatis mapper包路径 注意该注解用的tk的
@MapperScan(basePackages = "com.ginseng.mapper")
//扫描 所有需要的包,包含一些自用的工具类包 所在的路径
@ComponentScan(basePackages = {"com.ginseng", "org.n3r.idworker"})
public class SixinsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SixinsApplication.class, args);
    }

}
