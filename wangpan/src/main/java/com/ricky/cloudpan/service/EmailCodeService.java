package com.ricky.cloudpan.service;

import org.apache.ibatis.annotations.Param;

public interface EmailCodeService {
    void sendEmailCode(String email, Integer type);

    void checkCode(String email, String emailCode);
}
