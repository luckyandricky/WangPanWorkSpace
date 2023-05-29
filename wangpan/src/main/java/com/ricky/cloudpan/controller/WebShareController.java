package com.ricky.cloudpan.controller;

import com.ricky.cloudpan.annotation.GlobalInterceptor;
import com.ricky.cloudpan.annotation.VerifyParam;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.SessionShareDto;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.enums.FileDelFlagEnums;
import com.ricky.cloudpan.entity.enums.ResponseCodeEnum;
import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.entity.po.FileShare;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.entity.vo.FileInfoVO;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.entity.vo.ShareInfoVO;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.query.FileInfoQuery;
import com.ricky.cloudpan.service.FileInfoService;
import com.ricky.cloudpan.service.FileShareService;
import com.ricky.cloudpan.service.UserInfoService;
import com.ricky.cloudpan.utils.CopyTools;
import com.ricky.cloudpan.utils.Result;
import com.ricky.cloudpan.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.mail.Session;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController("webShareController")
@RequestMapping("/showShare")
public class WebShareController extends CommonFileConroller {
    @Resource
    private FileShareService fileShareService;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserInfoService userInfoService;

    //获取分享登录信息
    @RequestMapping("getShareLoginInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public Result getShareLoginInfo(HttpSession session,
                                    @VerifyParam(required = true)String shareId){
        SessionShareDto sessionShareDto = getSessionShareFromSession(session, shareId);
        if (sessionShareDto == null) {
            return Result.of_success(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        //判断是够是当前用户分享的文件
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if(userDto != null && userDto.getUserId().equals(sessionShareDto.getShareUserId())){
            shareInfoVO.setCurrentUser(true);
        }else {
            shareInfoVO.setCurrentUser(false);
        }
        return Result.of_success(shareInfoVO);
    }
    private ShareInfoVO getShareInfoCommon(String shareId) {
        FileShare share = fileShareService.getFileShareByShareId(shareId);
        //如果没有分享信息，并且过期
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(), share.getUserId());
        if (fileInfo == null || !FileDelFlagEnums.USING.getFlags().equals(fileInfo.getDel_flag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        shareInfoVO.setFileName(fileInfo.getFile_name());
        UserInfo userInfo = userInfoService.getUserInfoByUserId(share.getUserId());
        shareInfoVO.setNickName(userInfo.getNick_name());
        shareInfoVO.setAvatar(userInfo.getQq_avatar());
        shareInfoVO.setUserId(userInfo.getUser_id());
        return shareInfoVO;
    }

    /**
     * 获取分享信息
     *
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public Result getShareInfo(@VerifyParam(required = true) String shareId) {
        return Result.of_success(getShareInfoCommon(shareId));
    }

    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public Result checkShareCode(HttpSession session,
                                     @VerifyParam(required = true) String shareId,
                                     @VerifyParam(required = true) String code) {
        SessionShareDto shareSessionDto = fileShareService.checkShareCode(shareId, code);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, shareSessionDto);
        return Result.of_success(null);
    }

    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public Result loadFileList(HttpSession session,
                                   @VerifyParam(required = true) String shareId, String filePid){
        SessionShareDto shareDto = checkShare(session,shareId);
        FileInfoQuery query = new FileInfoQuery();
        if (!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)) {
            fileInfoService.checkRootFilePid(shareDto.getFileId(), shareDto.getShareUserId(), filePid);
            query.setFilePid(filePid);
        } else {
            query.setFileId(shareDto.getFileId());
        }
        query.setUserId(shareDto.getShareUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlags());
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return Result.of_success(convert2PaginationVO2(resultVO, FileInfoVO.class));
    }

    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto shareSessionDto = getSessionShareFromSession(session, shareId);
        if (shareSessionDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_903);
        }
        if (shareSessionDto.getExpireTime() != null && new Date().after(shareSessionDto.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        return shareSessionDto;
    }
}
