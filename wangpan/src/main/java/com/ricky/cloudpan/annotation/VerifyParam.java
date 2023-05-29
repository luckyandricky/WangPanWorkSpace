package com.ricky.cloudpan.annotation;

import com.ricky.cloudpan.entity.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyParam {
    int min() default -1;
    int max() default -1;
    boolean required() default false;

    VerifyRegexEnum regex() default VerifyRegexEnum.NO; //默认不校验
}
