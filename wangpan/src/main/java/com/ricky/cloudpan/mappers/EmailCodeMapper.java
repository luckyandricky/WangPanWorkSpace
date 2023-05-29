package com.ricky.cloudpan.mappers;

import com.ricky.cloudpan.entity.po.EmailCode;
import org.apache.ibatis.annotations.Param;

public interface EmailCodeMapper{
    EmailCode selectByEmailAndCode(String email,@Param("emailCode") String emailCode);

    Integer updateByEmailAndCode(String email, @Param("code") String code);

    Integer insert(EmailCode emailCode);

    void disableEmailCode(@Param("email") String email);


}
