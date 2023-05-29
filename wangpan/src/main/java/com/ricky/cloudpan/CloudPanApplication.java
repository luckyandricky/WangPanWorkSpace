package com.ricky.cloudpan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.ricky.cloudpan"})
@MapperScan(basePackages = {"com.ricky.cloudpan.mappers"})
@EnableTransactionManagement //事务实现
@EnableAsync //异步调用
@EnableScheduling //定时任务
public class CloudPanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPanApplication.class, args);
    }

}
