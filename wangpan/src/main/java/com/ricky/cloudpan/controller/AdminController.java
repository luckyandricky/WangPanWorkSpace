package com.ricky.cloudpan.controller;

import com.ricky.cloudpan.annotation.GlobalInterceptor;
import com.ricky.cloudpan.annotation.VerifyParam;
import com.ricky.cloudpan.component.RedisComponent;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.CreateImageCode;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.dto.SysSetingDto;
import com.ricky.cloudpan.entity.dto.UserSpaceDto;
import com.ricky.cloudpan.entity.enums.VerifyRegexEnum;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.entity.vo.FileInfoVO;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.entity.vo.UserInfoVO;
import com.ricky.cloudpan.query.FileInfoQuery;
import com.ricky.cloudpan.query.UserInfoQuery;
import com.ricky.cloudpan.service.EmailCodeService;
import com.ricky.cloudpan.service.FileInfoService;
import com.ricky.cloudpan.service.UserInfoService;
import com.ricky.cloudpan.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@RestController("adminController")
@RequestMapping("/admin")
//RestController作用相当于ResponseBody共同作用，采用RestController一般采用Restful风格
public class AdminController extends CommonFileConroller{

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/getSysSettings")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result getSysSetting(){
        return Result.of_success(redisComponent.getSysSettingDto());
    }

    @RequestMapping("/saveSysSettings")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result saveSysSettings(
            @VerifyParam(required = true) String registerEmailTitle,
            @VerifyParam(required = true) String registerEmailContent,
            @VerifyParam(required = true) Integer userInitUseSpace) {
        SysSetingDto sysSettingsDto = new SysSetingDto();
        sysSettingsDto.setRegisterEmailTitle(registerEmailTitle);
        sysSettingsDto.setRegisterEmailContent(registerEmailContent);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        redisComponent.saveSysSettingsDto(sysSettingsDto);
        return Result.of_success(null);
    }

    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public Result loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return Result.of_success(convert2PaginationVO4(resultVO, UserInfoVO.class));
    }

    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result updateUserStatus(@VerifyParam(required = true)String userId,
                                   @VerifyParam(required = true) Integer status){
        userInfoService.updateUserStatus(userId, status);
        return Result.of_success(null);
    }


    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true)
    public Result loadDataList(FileInfoQuery query){
        query.setOrderBy("last_update_time desc");
        query.setQueryNickName(true);
        PaginationResultVO result = fileInfoService.findListByPage(query);
        //return Result.of_success(resultVO);
        return Result.of_success(convert2PaginationVO2(result, FileInfoVO.class));
    }

    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public void getFile(HttpServletResponse response,
                        @PathVariable("userId") @VerifyParam(required = true) String userId,
                        @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        super.getFile(response, fileId, userId);
    }

    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("userId") @VerifyParam(required = true) String userId,
                             @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        super.getFile(response, fileId, userId);
    }

    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result createDownloadUrl(@PathVariable("userId") @VerifyParam(required = true) String userId,
                                        @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        return super.createDownloadUrl(fileId, userId);
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public Result delFile(@VerifyParam(required = true) String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0], itemArray[1], true);
        }
        return Result.of_success(null);
    }
}
