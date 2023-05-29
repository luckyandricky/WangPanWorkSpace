package com.ricky.cloudpan.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class FileInfoVO {
    private String fileId;
    private String filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;

    //0文件 1目录
    private Integer folderType;
    private Integer fileCategory;
    private Integer fileType;
    private Integer status;

}
