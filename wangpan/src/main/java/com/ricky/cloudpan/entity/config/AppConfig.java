package com.ricky.cloudpan.entity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appConfig")
public class AppConfig {
    //邮件发送人
    @Value("${spring.mail.username}")
    private String sendUserName;

    @Value("${admin.emails}")
    private String aminEmails;

    @Value("${project.folder}")
    private String projectFolder;

    @Value("${dev:false}")
    private Boolean dev;
    public String getSendUserName() {
        return sendUserName;
    }

    public String getAminEmails() {
        return aminEmails;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    public Boolean getDev() {
        return dev;
    }
}
