package com.ricky.cloudpan.controller;


import com.ricky.cloudpan.component.RedisComponent;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.DownloadFileDto;
import com.ricky.cloudpan.entity.enums.FileCategoryEnums;
import com.ricky.cloudpan.entity.enums.FileFolderTypeEnums;
import com.ricky.cloudpan.entity.enums.ResponseCodeEnum;
import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.entity.po.FolderVO;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.query.FileInfoQuery;
import com.ricky.cloudpan.service.FileInfoService;
import com.ricky.cloudpan.utils.CopyTools;
import com.ricky.cloudpan.utils.Result;
import com.ricky.cloudpan.utils.StringTools;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.StringUtils;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;

public class CommonFileConroller extends ABaseController{
    @Resource
    protected FileInfoService fileInfoService;

    @Resource
    protected AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    public void getImage(HttpServletResponse response, String imageFolder,String imageName){
        if(StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName)){
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, filePath);
    }

    public void getFile(HttpServletResponse response,String fileId,String userId){
        //刚开始视频的请求时m3u8, 后面都是.js 文件。所以要在这里判断一下文件类型----》m0k9Eo8sPz_0000.ts
        String filePath = null;
        if(fileId.endsWith(".ts")){
            String[] tsAarray = fileId.split("_");
            String realFileId = tsAarray[0];
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId,userId);
            if(fileInfo == null) {
                return;
            }
            String fileName = fileInfo.getFile_path();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        }else {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId,userId);
            if(fileInfo == null){
                return;
            }
            //视频文件读取m3u8文件
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFile_category())) {
                //重新设置文件路径
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFile_path());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            }else {
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFile_path();
            }
        }
        File file = new File(filePath);
        if(!file.exists()){
            return;
        }
        readFile(response,filePath);
    }

    public Result getFolderInfo(String path, String userId) {
        String[] pathArray = path.split("/");
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileIdArray(pathArray);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        String orderby = "field(file_id,\"" + StringUtils.join(pathArray,"\",\"") + "\")";
        fileInfoQuery.setOrderBy(orderby);
        List<FileInfo> fileInfos = fileInfoService.findListByParam(fileInfoQuery);
        return Result.of_success(CopyTools.copyList_FileInfo_to_FolderVO(fileInfos, FolderVO.class));
    }

    protected Result createDownloadUrl(String filId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(filId,userId);
        if(fileInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolder_type())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        DownloadFileDto fileDto = new DownloadFileDto();
        fileDto.setFileId(filId);
        fileDto.setFileName(fileInfo.getFile_name());
        fileDto.setFilePath(fileInfo.getFile_path());
        fileDto.setDownloadCode(code);

        redisComponent.saveDownloadCode(code,fileDto);
        return Result.of_success(code);
    }

    protected void download(HttpServletRequest request,HttpServletResponse response,
                            String code) throws Exception{
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if(null == downloadFileDto){
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if(request.getHeader("User-Agent").toLowerCase().indexOf("msie")>0){
            //IE浏览器
            fileName = URLEncoder.encode(fileName,"UTF-8");

        }else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }
}
