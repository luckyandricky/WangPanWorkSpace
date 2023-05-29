package com.ricky.cloudpan.utils;

import com.ricky.cloudpan.entity.enums.ResponseCodeEnum;

public class Result<T> {
    protected static final String STARUC_SUCCESS = "success";
    protected static final String STATUC_ERROR = "error";
    private String status;
    private Integer code;
    private String info;
    private T data;

    public Result(Integer code, String status, String info, T data) {
        this.code = code;
        this.status = status;
        this.info = info;
        this.data = data;

    }

    public Result() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> Result<T> of_success(T data){
        return new Result<>(ResponseCodeEnum.CODE_200.getCode(),STARUC_SUCCESS,ResponseCodeEnum.CODE_200.getMsg(),data);
    }

    public static <T> Result<T> of_error(T data){
        return new Result<>(ResponseCodeEnum.CODE_602.getCode(),STATUC_ERROR,ResponseCodeEnum.CODE_602.getMsg(),data);
    }

    public static <T> Result<T> of_error_603(T data){
        return new Result<>(ResponseCodeEnum.CODE_603.getCode(),STATUC_ERROR,ResponseCodeEnum.CODE_603.getMsg(),data);
    }

    public static <T> Result<T> of_error_604(T data){
        return new Result<>(ResponseCodeEnum.CODE_604.getCode(),STATUC_ERROR,ResponseCodeEnum.CODE_604.getMsg(),data);
    }
    public static <T> Result<T> of_error_601(T data){
        return new Result<>(ResponseCodeEnum.CODE_601.getCode(),STATUC_ERROR,ResponseCodeEnum.CODE_601.getMsg(),data);
    }
}
