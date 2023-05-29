package com.ricky.cloudpan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {
    //校验参数
    boolean checkParams() default false;
    boolean checkAdmin() default false;

    boolean checkLogin() default false;
}
