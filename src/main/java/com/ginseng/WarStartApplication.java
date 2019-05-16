package com.ginseng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

//使用外置的tomcat启动项目
public class WarStartApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SixinsApplication.class);
    }


    public static void main(String[] args) {
        SpringApplication.run(WarStartApplication.class, args);
    }

}
