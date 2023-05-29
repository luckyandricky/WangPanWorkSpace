package com.ricky.cloudpan.exception;

import com.ricky.cloudpan.entity.enums.ResponseCodeEnum;

public class BusinessException extends RuntimeException{
    private String message;
    private Integer code;
    private ResponseCodeEnum codeEnum;
    public BusinessException(String message) {
        super(message);
        this.message = message;
    }
    public BusinessException(ResponseCodeEnum responseCodeEnum){
        super(responseCodeEnum.getMsg());
        this.codeEnum = responseCodeEnum;
        this.code = codeEnum.getCode();
        this.message = responseCodeEnum.getMsg();
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Integer getCode() {
        return code;
    }

    public ResponseCodeEnum getCodeEnum() {
        return codeEnum;
    }
}
