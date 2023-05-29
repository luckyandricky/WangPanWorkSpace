package com.ricky.cloudpan.service.impl;

import com.ricky.cloudpan.component.RedisComponent;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.SysSetingDto;
import com.ricky.cloudpan.entity.po.EmailCode;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.mappers.EmailCodeMapper;
import com.ricky.cloudpan.mappers.UserInfoMapper;
import com.ricky.cloudpan.service.EmailCodeService;
import com.ricky.cloudpan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Date;


@Service
public class EmailCodeServiceImpl implements EmailCodeService {
    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);
    @Resource
    private EmailCodeMapper emailCodeMapper;
    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private JavaMailSender JavaMailSender;

    @Resource
    private RedisComponent rediComponent;
    @Resource
    private AppConfig appConfig;

    @Override
    public void sendEmailCode(String email, Integer type) {
        if(type == 0){
            UserInfo userInfo = userInfoMapper.selectByEmail(email);
            if(null!=userInfo){
                throw new BusinessException("邮箱已经存在");
            }
        }
        //随机生成5位长度验证码
        String code = StringTools.getRandomNumber(Constants.LENGTH);
        //把之前的验证码置为无效
        emailCodeMapper.disableEmailCode(email);
        //TODO 发送验证码
        sendMailCode(email,code);

        EmailCode emailCode = new EmailCode();
        emailCode.setCode(code);
        emailCode.setEmail(email);
        emailCode.setStatus(Constants.ZERO);
        emailCode.setCreate_time(new Date());
        //存入数据库
        emailCodeMapper.insert(emailCode);

    }



    private void sendMailCode(String toEmail, String code){
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
            logger.error("邮件发送失败");
            throw new BusinessException("邮件发送异常");
        }
    }

    /**
     * 校验邮箱验证码
     * @param email
     * @param code
     */
    @Override
    public void checkCode(String email, String code) {
        EmailCode emailcode = emailCodeMapper.selectByEmailAndCode(email,code);
        if(null == emailcode){
            throw new BusinessException("邮箱验证码不正确");
        }
        if(emailcode.getStatus() == 1 || System.currentTimeMillis() - emailcode.getCreate_time().getTime() > Constants.LENGTH_15 * 1000 * 60){
            throw new BusinessException("验证码已经失效");
        }
        emailCodeMapper.disableEmailCode(email);
    }
}
