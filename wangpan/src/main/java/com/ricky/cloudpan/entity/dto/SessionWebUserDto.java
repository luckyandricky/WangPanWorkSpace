package com.ricky.cloudpan.entity.dto;

import lombok.Data;

import java.util.Date;

@Data
public class SessionWebUserDto {
    private String nickName;
    private String userId;
    private Boolean admin;
    private Long useSpace;
    private Long totalSpace;
    private String avatar;
}
