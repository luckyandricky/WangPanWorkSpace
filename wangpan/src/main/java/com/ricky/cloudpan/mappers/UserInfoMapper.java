package com.ricky.cloudpan.mappers;


import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.query.UserInfoQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface UserInfoMapper {
    UserInfo selectByEmail(@Param("email")String email);

    UserInfo selectByNickName(@Param("nickName")String nickName);

    void insert(@Param("userInfo")UserInfo userInfo);

    Integer updateByUserId(@Param("bean")UserInfo updateInfo, @Param("userId")String user_id);

    void updateByEmail(@Param("bean") UserInfo updateUser,@Param("email") String email);

    Integer updateUserSpace(@Param("userId") String userId,@Param("useSpace") Long useSize,@Param("totalSpace") Long totalSize);

    Integer selectCount(@Param("query")UserInfoQuery param);

    List<UserInfo> selectList(@Param("query")UserInfoQuery param);

    UserInfo selectByUserId(@Param("userId") String userId);
}
