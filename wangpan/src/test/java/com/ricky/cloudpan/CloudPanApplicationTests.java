package com.ricky.cloudpan;

import com.ricky.cloudpan.component.RedisComponent;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.CreateImageCode;
import com.ricky.cloudpan.entity.dto.SysSetingDto;
import com.ricky.cloudpan.entity.po.EmailCode;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.mappers.EmailCodeMapper;
import com.ricky.cloudpan.mappers.UserInfoMapper;
import com.ricky.cloudpan.service.UserInfoService;
import com.ricky.cloudpan.utils.StringTools;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

@SpringBootTest
class CloudPanApplicationTests {

    @Resource
    private EmailCodeMapper emailCodeMapper;
    @Resource
    private org.springframework.mail.javamail.JavaMailSender JavaMailSender;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private RedisComponent rediComponent;
    @Resource
    private AppConfig appConfig;

    @Resource
    private UserInfoService userInfoService;
    @Test
    void contextLoads() throws IOException {
        CreateImageCode vCode = new CreateImageCode(130,38,5,10);
        FileOutputStream fos = new FileOutputStream("./1.jpg");
        vCode.write2(fos);
    }

    @Test
    void testDate(){
        String code = StringTools.getRandomNumber(Constants.LENGTH);
        String email = "sdfsszfdsxs.com";
        EmailCode emailCode = new EmailCode();
        emailCode.setCode(code);
        emailCode.setEmail(email);
        emailCode.setStatus(Constants.ZERO);
        emailCode.setCreate_time(new Date());
        System.out.println(new Date());
        System.out.println(emailCode);

        Integer insert = emailCodeMapper.insert(emailCode);
        System.out.println("insert result is ======"+insert );
        //throw new BusinessException("邮箱已经存在");
        //
        String code2 = StringTools.getRandomNumber(Constants.LENGTH);
        System.out.println(code2);
    }

    @Test
    void testSendMail(){
        String toEmail = "sdkjdxzxr@163.com";
        String code = StringTools.getRandomNumber(Constants.LENGTH);
        try {
            MimeMessage message = JavaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true);
            helper.setFrom(appConfig.getSendUserName());
            helper.setTo(toEmail);

            SysSetingDto sysSetingDto = rediComponent.getSysSettingDto();
            helper.setSubject(sysSetingDto.getRegisterEmailTitle());
            helper.setText(String.format(sysSetingDto.getRegisterEmailContent(),code));

            helper.setSentDate(new Date());
            JavaMailSender.send(message);
        }catch (Exception e){
            throw new BusinessException("邮件发送异常");
        }
    }

    @Test
    void testSelect(){
        UserInfo userInfo = userInfoMapper.selectByEmail("dsfsdf.com");
        System.out.println(userInfo);
    }

    @Test
    void TestUserInfo(){
        //注册模块，先检查邮箱验证码，和图片验证码
        String email = "sdfsszfdsxs.com";
        String code = "qvMYc";
        userInfoService.register(email,"ricky","11111",code);
    }
}
