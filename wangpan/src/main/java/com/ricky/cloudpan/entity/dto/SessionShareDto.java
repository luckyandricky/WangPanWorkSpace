package com.ricky.cloudpan.entity.dto;

import lombok.Data;

import java.util.Date;

@Data
public class SessionShareDto {
    private String shareId;
    private String shareUserId;
    private Date expireTime;
    private String fileId;
}
