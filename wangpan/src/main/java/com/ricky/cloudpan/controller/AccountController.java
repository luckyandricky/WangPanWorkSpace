package com.ricky.cloudpan.controller;

import com.ricky.cloudpan.annotation.GlobalInterceptor;
import com.ricky.cloudpan.annotation.VerifyParam;
import com.ricky.cloudpan.component.RedisComponent;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.CreateImageCode;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.dto.UserSpaceDto;
import com.ricky.cloudpan.entity.enums.VerifyRegexEnum;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.service.EmailCodeService;
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

@RestController("UserInfoController")
//RestController作用相当于ResponseBody共同作用，采用RestController一般采用Restful风格
public class AccountController extends ABaseController{

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    @Resource
    private EmailCodeService emailCodeServic;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;
    /**
     * 验证码
     * @param response
     * @param session
     * @param type
     * @throws IOException
     */
    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws
            IOException{
        CreateImageCode vCode = new CreateImageCode(130,38,5,10);
        response.setHeader("Pragma","no-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if(type == null || type == 0){
            session.setAttribute(Constants.CHECK_CODE_KEY,code);

        }else {
            session.setAttribute(Constants.CHECK_CODE_EMAIL,code);
        }
        vCode.write(response.getOutputStream());
    }

    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public Result sendEmailCode(HttpSession session,
                                        @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                        @VerifyParam(required = true) String checkCode,
                                        @VerifyParam(required = true) Integer type){
        try {
            if(!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_EMAIL))){
                //throw new BusinessException("图片验证码不正确");
                //System.out.println("验证码不正确");
                return Result.of_error(null);
            }
            emailCodeServic.sendEmailCode(email,type);
            return Result.of_success("null");
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_EMAIL);
        }
    }


    /**
     * 注册
     * @param session
     * @param email
     * @param nickName
     * @param password
     * @param checkCode
     * @param emailCode
     * @return
     */
    @RequestMapping("/register")
    public Result<String> register(HttpSession session,
                                   @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                   @VerifyParam(required = true, max = 20) String nickName,
                                   @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                                   @VerifyParam(required = true) String checkCode,
                                   @VerifyParam(required = true) String emailCode){
        try {
            if(!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))){
                return Result.of_error(null);
                //throw new BusinessException("图片验证码不正确");
            }
            //注册
            Integer result = userInfoService.register(email,nickName,password,emailCode);
            if(result == -1){
                //账号已经存在
                return Result.of_error_603(null);
            }else if(result == -2){
                //昵称已经存在
                return Result.of_error_601(null);
            }else {
                return Result.of_success(null);
            }

        }finally {
            //移除session中的attribute  CHECK_CODE_KEY
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/login")
    public Result login(HttpSession session, HttpServletRequest request,
                        @VerifyParam(required = true) String email,
                        @VerifyParam(required = true) String password,
                        @VerifyParam(required = true) String checkCode){
        try {
            //仅仅测试账号
            if(email.equals("sdkjdxzxr@163.com")){
                SessionWebUserDto sessionWebUserDto = userInfoService.login(email,password);
                sessionWebUserDto.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY,sessionWebUserDto);
                return Result.of_success(sessionWebUserDto);
            }
            if(!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))){
                //验证码不正确
                return Result.of_error(null);
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email,password);
            session.setAttribute(Constants.SESSION_KEY,sessionWebUserDto);
            return Result.of_success(sessionWebUserDto);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public Result resetPwd(HttpSession session,
                           @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                           @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                           @VerifyParam(required = true) String checkCode,
                           @VerifyParam(required = true) String emailCode){
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                return Result.of_error(null);
                //throw new BusinessException("图片验证码不正确");
            }
            userInfoService.resetPwd(email,password,emailCode);
            return Result.of_success(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }

    }
    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void getAvatar(HttpServletResponse response, HttpSession session, @VerifyParam(required = true) @PathVariable("userId")String userId){
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder()+avatarFolderName);
        if(!folder.exists()){
            folder.mkdirs();
        }
        String avatarPath = appConfig.getProjectFolder()+avatarFolderName+userId+Constants.AVATAR_SUBFIX;
        File file = new File(avatarPath);
        if(!file.exists()){
            if(!new File(appConfig.getProjectFolder()+avatarFolderName+Constants.AVATAR_DEFAULT).exists())
            {
                printNoDefaultImage(response);
                return ;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFAULT;
        }
        response.setContentType("image/jpeg");
        readFile(response,avatarPath);
    }

    private void printNoDefaultImage(HttpServletResponse response){
        response.setHeader(CONTENT_TYPE,CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println("请在头像目录下放置默认头像default_avatat.jpg");
            writer.close();
        }catch (Exception e){
            logger.error("输出默认图失败",e);
        }finally {
            writer.close();
        }

    }

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkParams = true)
    public Result getUserInfo(HttpSession session){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return Result.of_success(sessionWebUserDto);
    }

    @RequestMapping("/getUseSpace")
    @GlobalInterceptor(checkParams = true)
    public Result getUseSpace(HttpSession session){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserSpaceDto userSpaceDto = redisComponent.getUserSpace(sessionWebUserDto.getUserId());
        return Result.of_success(userSpaceDto);
    }

    @RequestMapping("/logout")
    @GlobalInterceptor(checkParams = true)
    public Result logout(HttpSession session){
        session.invalidate();
        return Result.of_success(null);
    }

    @RequestMapping("/updateUserAvatar")
    public Result updateUserAvatar(HttpSession session, MultipartFile avatar){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if(!targetFileFolder.exists()){
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUBFIX);
        try {
            avatar.transferTo(targetFile);
        }catch (Exception e){
            logger.error("上传头像失败",e);
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setQq_avatar("");
        userInfoService.updateUserInfoByUserId(userInfo,webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY,webUserDto);
        return Result.of_success(null);

    }


}
