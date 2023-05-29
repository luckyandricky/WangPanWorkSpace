package com.ricky.cloudpan.entity.po;

import lombok.Data;

import java.util.Date;

@Data
public class UserInfo {
    private String user_id;
    private String nick_name;
    private String email;
    private String qq_open_id;
    private String qq_avatar;
    private String password;
    private Date join_time;
    private Date last_login_time;
    private Integer status;
    private Long use_space;
    private Long total_space;

}
