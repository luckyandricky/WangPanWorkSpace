package com.ricky.cloudpan.service.impl;

import com.ricky.cloudpan.component.RedisComponent;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.dto.SysSetingDto;
import com.ricky.cloudpan.entity.dto.UserSpaceDto;
import com.ricky.cloudpan.entity.enums.PageSize;
import com.ricky.cloudpan.entity.enums.UserStatusEnum;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.mappers.FileInfoMapper;
import com.ricky.cloudpan.mappers.UserInfoMapper;
import com.ricky.cloudpan.query.SimplePage;
import com.ricky.cloudpan.query.UserInfoQuery;
import com.ricky.cloudpan.service.EmailCodeService;
import com.ricky.cloudpan.service.FileInfoService;
import com.ricky.cloudpan.service.UserInfoService;
import com.ricky.cloudpan.utils.StringTools;
import org.apache.catalina.User;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private FileInfoService fileInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer register(String email, String nickName, String password, String emailCode) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if(null != userInfo){
            //throw new BusinessException("该邮箱账号已经存在");
            return -1;
        }
        UserInfo NickNameUser = userInfoMapper.selectByNickName(nickName);
        if(null != NickNameUser){
            //throw new BusinessException("昵称已经存在");
            return -2;
        }
        //校验邮箱验证码
        emailCodeService.checkCode(email,emailCode);
        //随机生成userid
        String userId = StringTools.getRandomNumber(Constants.LENGTH_10);
        userInfo = new UserInfo();
        userInfo.setUser_id(userId);
        userInfo.setNick_name(nickName);
        userInfo.setEmail(email);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setJoin_time(new Date());

        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        SysSetingDto sysSettingDto = redisComponent.getSysSettingDto();
        userInfo.setTotal_space(sysSettingDto.getUserInitUseSpace()* Constants.MB);
        userInfo.setUse_space(0L);
        userInfoMapper.insert(userInfo);
        return 1;
    }

    @Override
    public SessionWebUserDto login(String email, String password) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if(userInfo == null || !userInfo.getPassword().equals(password)){
            throw new BusinessException("账号或者密码错误");
        }
        if(UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())){
            throw new BusinessException("账号已禁用");
        }
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLast_login_time(new Date());
        System.out.println(updateInfo);
        //修改最后登录时间
        userInfoMapper.updateByUserId(updateInfo, userInfo.getUser_id());
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setNickName(userInfo.getNick_name());
        sessionWebUserDto.setUserId(userInfo.getUser_id());
        //如果账号属于超级管理员。
        if(ArrayUtils.contains(appConfig.getAminEmails().split(","),email)){
            sessionWebUserDto.setAdmin(true);
        }else {
            sessionWebUserDto.setAdmin(false);
        }
        //用户空间
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        //查询mysql数据库，用户用了多少空间。
        Long usespace = fileInfoMapper.selectUseSpace(userInfo.getUser_id());
        userSpaceDto.setUseSpace(usespace);
        userSpaceDto.setTotalSpace(userInfo.getTotal_space());
        redisComponent.saveUserSpaceUse(userInfo.getUser_id(),userSpaceDto);
        return sessionWebUserDto;
    }

    /**
     * 修改密码
     * @param email
     * @param password
     * @param emailCode
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(String email, String password, String emailCode) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if(null == userInfo){
            throw new BusinessException("邮箱账号不存在");
        }
        //校验邮箱验证码
        emailCodeService.checkCode(email,emailCode);

        UserInfo updateUser = new UserInfo();
        updateUser.setPassword(StringTools.encodeByMD5(password));
        userInfoMapper.updateByEmail(updateUser,email);
    }

    @Override
    public Integer updateUserInfoByUserId(UserInfo userInfo, String userId) {
        return userInfoMapper.updateByUserId(userInfo,userId);
    }


    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectCount(param);
    }

    @Override
    public void updateUserStatus(String userId, Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        if(UserStatusEnum.DISABLE.getStatus().equals(status)){
            userInfo.setUse_space(0L);
            fileInfoService.deleteFileByUserId(userId);
        }
        userInfoMapper.updateByUserId(userInfo,userId);
    }

    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        return userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserInfo> findListByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectList(param);
    }
    @Override
    public PaginationResultVO findListByPage(UserInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(1, count, pageSize);
        //param.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(param);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

}
