package com.ricky.cloudpan.controller;

import com.ricky.cloudpan.annotation.GlobalInterceptor;
import com.ricky.cloudpan.annotation.VerifyParam;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.dto.UploadResultDto;
import com.ricky.cloudpan.entity.enums.FileCategoryEnums;
import com.ricky.cloudpan.entity.enums.FileDelFlagEnums;
import com.ricky.cloudpan.entity.enums.FileFolderTypeEnums;
import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.entity.po.FileInfo2;
import com.ricky.cloudpan.entity.vo.FileInfoVO;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.query.FileInfoQuery;
import com.ricky.cloudpan.service.FileInfoService;
import com.ricky.cloudpan.utils.CopyTools;
import com.ricky.cloudpan.utils.Result;
import com.ricky.cloudpan.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController("fileInfoController")
@RequestMapping("file")
public class FileInfoController extends CommonFileConroller{


    @RequestMapping("/loadDataList")
    @GlobalInterceptor(checkParams = true)
    public Result loadDataList(HttpSession session, FileInfoQuery query, String category){
        FileCategoryEnums fileCategoryEnums = FileCategoryEnums.getByCode(category);
        if( null != fileCategoryEnums){
            query.setFileCategory(fileCategoryEnums.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlags());
        query.setPageNo(1);
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return Result.of_success(convert2PaginationVO2(result, FileInfoVO.class));
    }

    /**
     *上传文件
     * @param session
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid
     * @param fileMd5
     * @param chunkIndex 分片的索引
     * @param chunks 共有几个分片
     * @return
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    public Result uploadFile(HttpSession session,
                             String fileId,
                             MultipartFile file,
                             @VerifyParam(required = true) String fileName,
                             @VerifyParam(required = true) String filePid,
                             @VerifyParam(required = true) String fileMd5,
                             @VerifyParam(required = true) Integer chunkIndex,
                             @VerifyParam(required = true) Integer chunks){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return Result.of_success(resultDto);
    }

    //获取缩略图
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder")String imageFolder,
                         @PathVariable("imageName") String imageName){
        super.getImage(response, imageFolder, imageName);
    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response,HttpSession session,@PathVariable("fileId")@VerifyParam(required = true) String fileId){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        super.getFile(response,fileId,sessionWebUserDto.getUserId());
    }

    @RequestMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session,
                        @PathVariable("fileId")@VerifyParam(required = true) String fileId){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    //创建目录
    @RequestMapping("/newFoloder")
    @GlobalInterceptor(checkParams = true)
    public Result newFOloder(HttpSession session,
                           @VerifyParam(required = true)String filePid,
                           @VerifyParam(required = true)String fileName){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        FileInfo fileINfo = fileInfoService.newFolder(filePid,sessionWebUserDto.getUserId(),fileName);

        return Result.of_success(convert2PaginationVO3(fileINfo));
    }


    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public Result getFolderInfo(HttpSession session, @VerifyParam(required = true) String path) {
        return super.getFolderInfo(path, getUserInfoFromSession(session).getUserId());
    }

    @RequestMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    public Result rename(HttpSession session,
                         @VerifyParam(required = true) String fileId,
                         @VerifyParam(required = true) String fileName){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId,webUserDto.getUserId(),fileName);
        return Result.of_success(CopyTools.copy_FileInfo_to_FileInfoVo(fileInfo,FileInfoVO.class));
    }


    //移动文件的时候，要先获取所有目录
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    public Result loadAllFolder(HttpSession session,
                                @VerifyParam(required = true) String filePid,
                                String currentFileIds){
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setFilePid(filePid);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if(!StringTools.isEmpty(currentFileIds)){
            query.setExcludeFileIdArray(currentFileIds.split(","));
        }
        query.setDelFlag(FileDelFlagEnums.USING.getFlags());
        query.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(query);
        return Result.of_success(CopyTools.copyList_FileInfo_to_FileInfoVo(fileInfoList,FileInfoVO.class));
    }

    //移动文件夹
    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public Result changeFileFolder(HttpSession session,
                                   @VerifyParam(required = true) String fileIds,
                                   @VerifyParam(required = true) String filePid){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds,filePid,webUserDto.getUserId());
        return Result.of_success(null);
    }

    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public Result createDownloadUrl(HttpSession session,
                                    @PathVariable("fileId")@VerifyParam(required = true) String fileId
                                    ){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        return super.createDownloadUrl(fileId,webUserDto.getUserId());
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response, @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }


    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public Result delFile(HttpSession session, @VerifyParam(required = true) String fileIds){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(sessionWebUserDto.getUserId(),fileIds);
        return Result.of_success(null);
    }
}
