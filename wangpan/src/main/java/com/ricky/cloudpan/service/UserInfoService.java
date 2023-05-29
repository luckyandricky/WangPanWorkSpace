package com.ricky.cloudpan.service;

import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.query.UserInfoQuery;

import java.util.List;

public interface UserInfoService {
    Integer register(String email, String nickName, String password, String emailCode);

    SessionWebUserDto login(String email, String password);

    void resetPwd(String email, String password, String emailCode);

    Integer updateUserInfoByUserId(UserInfo userInfo, String userId);

    PaginationResultVO findListByPage(UserInfoQuery userInfoQuery);
    public List<UserInfo> findListByParam(UserInfoQuery param);
    public Integer findCountByParam(UserInfoQuery param);

    void updateUserStatus(String userId, Integer status);

    UserInfo getUserInfoByUserId(String userId);
}
