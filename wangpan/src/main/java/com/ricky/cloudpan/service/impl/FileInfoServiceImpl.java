package com.ricky.cloudpan.service.impl;

import com.ricky.cloudpan.component.RedisComponent;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.dto.UploadResultDto;
import com.ricky.cloudpan.entity.dto.UserSpaceDto;
import com.ricky.cloudpan.entity.enums.*;
import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.mappers.FileInfoMapper;
import com.ricky.cloudpan.mappers.UserInfoMapper;
import com.ricky.cloudpan.query.FileInfoQuery;
import com.ricky.cloudpan.query.SimplePage;
import com.ricky.cloudpan.service.FileInfoService;
import com.ricky.cloudpan.utils.DateUtil;
import com.ricky.cloudpan.utils.ProcessUtils;
import com.ricky.cloudpan.utils.ScaleFilter;
import com.ricky.cloudpan.utils.StringTools;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FileInfoServiceImpl implements FileInfoService {
    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);
    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private AppConfig appConfig;

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    /**
     * 分页查询
     * @param param
     * @return
     */
    @Override
    public PaginationResultVO findListByPage(FileInfoQuery param) {
        int count = findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();
        //SimplePage simplePage = new SimplePage(param.getPageNo(),count,pageSize);
        SimplePage simplePage = new SimplePage(1,count,15);
        //param.setSimplePage(simplePage);
        List<FileInfo> list = findListByParam(param);
        PaginationResultVO<FileInfo> result = new PaginationResultVO<>(count,simplePage.getPageSize(),simplePage.getPageNo(),simplePage.getPageTotal(),list);
        return result;
    }

    public List<FileInfo> findListByParam(FileInfoQuery param) {
        return fileInfoMapper.selectList(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks){
        File tempFileFolder = null;
        Boolean uploadSuccess = true;
        try {
            UploadResultDto resultDto = new UploadResultDto();
            if (StringTools.isEmpty(fileId)) {
                //随机生成fileID
                fileId = StringTools.getRandomString(Constants.LENGTH_10);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();
            UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(webUserDto.getUserId());
            if (chunkIndex == 0) {
                FileInfoQuery infoQuery = new FileInfoQuery();
                infoQuery.setFileMd5(fileMd5);
                infoQuery.setSimplePage(new SimplePage(0, 1));
                infoQuery.setStatus(FileStatusEnums.USING.getStatus());
                List<FileInfo> dbFileList = fileInfoMapper.selectList(infoQuery);
                //秒传
                if (!dbFileList.isEmpty()) {
                    FileInfo dbFile = dbFileList.get(0);
                    //判断文件状态
                    if (dbFile.getFile_size() + userSpaceDto.getUseSpace() > userSpaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    dbFile.setFile_id(fileId);
                    dbFile.setFile_pid(filePid);
                    dbFile.setUser_id(webUserDto.getUserId());
                    dbFile.setFile_md5(null);
                    dbFile.setCreate_time(curDate);
                    dbFile.setLast_update_time(curDate);
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    dbFile.setDel_flag(FileDelFlagEnums.USING.getFlags());
                    dbFile.setFile_md5(fileMd5);
                    fileName = autoRename(filePid,webUserDto.getUserId(),fileName);
                    dbFile.setFile_name(fileName);
                    //fileInfoMapper.insert(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    //更新用户空间使用：在数据库和redis中更新使用空间
                    updateuserSpace(webUserDto,dbFile.getFile_size());

                    return resultDto;
                }
            }
            //暂存在临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            //创建临时目录
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if(!tempFileFolder.exists()){
                tempFileFolder.mkdirs();
            }

            //判断磁盘空间
            Long currentTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(),fileId);
            if (file.getSize() + currentTempSize + userSpaceDto.getUseSpace() > userSpaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }

            //将文件存储到临时目录
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            file.transferTo(newFile);

            //保存临时大小
            redisComponent.saveFileTempSize(webUserDto.getUserId(),fileId, file.getSize());

            //不是最后一个分片，直接返回
            if(chunkIndex < chunks-1){
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return resultDto;
            }

            //最后一个分片上传完成，记录数据库，异步合并分片
            String month = DateUtil.format(curDate,DateTimePatternEnum.YYYYMM.getPattern());
            //文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileName);
            //真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            //自动重命名
            fileName = autoRename(filePid,webUserDto.getUserId(),fileName);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFile_id(fileId);
            fileInfo.setUser_id(webUserDto.getUserId());
            fileInfo.setFile_md5(fileMd5);
            fileInfo.setFile_name(fileName);
            fileInfo.setFile_path(month + "/" + realFileName);
            fileInfo.setFile_pid(filePid);
            fileInfo.setCreate_time(curDate);
            fileInfo.setLast_update_time(curDate);
            fileInfo.setFile_category(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFile_type(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolder_type(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDel_flag(FileDelFlagEnums.USING.getFlags());
            fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(),fileId);
            updateuserSpace(webUserDto,totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            //事务提交后调用异步方法,转码。 等待事务提交后再执行
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit(){
                    fileInfoService.transferFile(fileInfo.getFile_id(),webUserDto);
                }
            });
            return resultDto;
        } catch (BusinessException e){
            uploadSuccess = false;
            logger.error("文件上传失败",e);
            throw new BusinessException("文件上传失败");
        } catch (Exception e){
            uploadSuccess = false;
            logger.error("文件上传失败",e);
        }finally {
            //清除临时目录
            if(tempFileFolder != null && !uploadSuccess){
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                }catch (IOException e){
                    logger.error("删除临时目录失败");
                }
            }
        }
        return null;
    }

    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
        return fileInfoMapper.selectByFileIdAndUserId(fileId,userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.USING.getFlags());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        if (fileInfoList.isEmpty()) {
            return;
        }
        List<String> delFilePidList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(delFilePidList, userId, fileInfo.getFile_id(), FileDelFlagEnums.USING.getFlags());
        }
        //将目录下所有文件更新为已经删除
        if(!delFilePidList.isEmpty()){
            FileInfo updateInfo = new FileInfo();
            updateInfo.setDel_flag(FileDelFlagEnums.DEL.getFlags());
            fileInfoMapper.updateFileDelFlagBatch(updateInfo, userId,delFilePidList,null,FileDelFlagEnums.USING.getFlags());
        }

        //将选中的文件更新为回收站
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecovery_time(new Date());
        fileInfo.setDel_flag(FileDelFlagEnums.RECYCLE.getFlags());
        fileInfoMapper.updateFileDelFlagBatch(fileInfo,userId,null,delFileIdList,FileDelFlagEnums.USING.getFlags());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        //校验文件名
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        Date curDate = new Date();
        FileInfo fileinfo = new FileInfo();
        fileinfo.setFile_id(StringTools.getRandomString(Constants.LENGTH_10));
        fileinfo.setUser_id(userId);
        fileinfo.setFile_pid(filePid);
        fileinfo.setFile_name(folderName);
        fileinfo.setFolder_type(FileFolderTypeEnums.FOLDER.getType());
        fileinfo.setCreate_time(curDate);
        fileinfo.setLast_update_time(curDate);
        fileinfo.setStatus(FileStatusEnums.USING.getStatus());
        fileinfo.setDel_flag(FileDelFlagEnums.USING.getFlags());
        fileInfoMapper.insert(fileinfo);

        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(folderName);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlags());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if(count>1)
        {
            throw new BusinessException("文件夹" + folderName + "已经存在");
        }
        fileinfo.setFile_name(folderName);
        fileinfo.setLast_update_time(curDate);
        return fileinfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId,userId);
        if(fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        String filePid = fileInfo.getFile_pid();
        checkFileName(filePid,userId,fileName,fileInfo.getFolder_type());
        //文件获取后缀
        if(FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolder_type())){
            fileName = fileName + StringTools.getFileNameNoSuffix(fileInfo.getFile_name());
        }
        Date curDate = new Date();
        FileInfo dbInfo = new FileInfo();
        dbInfo.setFile_name(fileName);
        dbInfo.setLast_update_time(curDate);
        fileInfoMapper.updateByFileIdAndUserId(dbInfo,fileId,userId);


        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlags());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已经存在");
        }
        fileInfo.setFile_name(fileName);
        fileInfo.setLast_update_time(curDate);
        return fileInfo;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if(fileIds.equals(fileInfoService)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(!Constants.ZERO_STR.equals(filePid)){
            //查一下如果不是根目录，查一下它的父id是否存在
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(filePid,userId);
            if(fileInfo == null ||
                    !FileDelFlagEnums.USING.getFlags().equals(fileInfo.getDel_flag())){
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            String[] fileIdArray = fileIds.split(",");
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(filePid);
            query.setUserId(userId);
            List<FileInfo> dbFileList = fileInfoService.findListByParam(query);

            Map<String,FileInfo> dbFileNameMap = dbFileList.stream().collect(Collectors.toMap(FileInfo::getFile_name, Function.identity(),(data1,data2)->data1));

            //查询选中的文件
            query = new FileInfoQuery();
            query.setUserId(userId);
            query.setFileIdArray(fileIdArray);
            List<FileInfo> selectFileList = fileInfoService.findListByParam(query);

            //重命名
            for(FileInfo item:selectFileList){
                FileInfo rootFileInfo = dbFileNameMap.get(item.getFile_name());
                //文件名已经存在，重命名被还原的文件名
                FileInfo updateinfo = new FileInfo();
                if (rootFileInfo != null){
                    String fileName = StringTools.rename(item.getFile_name());
                    updateinfo.setFile_name(fileName);
                }
                updateinfo.setFile_pid(filePid);
                fileInfoMapper.updateByFileIdAndUserId(updateinfo,item.getFile_id(),userId);
            }
        }
    }

    //还原文件
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] filedArray = fileIds.split(",");
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileIdArray(filedArray);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlags());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(fileInfoQuery);

        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for(FileInfo fileInfo:fileInfoList){
            if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolder_type())){
                findAllSubFolderFileIdList(delFileSubFolderFileIdList,userId,fileInfo.getFile_id(),FileDelFlagEnums.DEL.getFlags());

            }
        }
        //查询所有根目录的文件
        fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlags());
        fileInfoQuery.setFilePid(Constants.ZERO_STR);
        List<FileInfo> allRootFileList = fileInfoMapper.selectList(fileInfoQuery);
        Map<String, FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFile_name, Function.identity(), (file1, file2) -> file2));
        //查询所有所选文件
        //将目录下的所有删除的文件更新为正常
        if (!delFileSubFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDel_flag(FileDelFlagEnums.USING.getFlags());
            this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, delFileSubFolderFileIdList, null, FileDelFlagEnums.DEL.getFlags());
        }
        //将选中的文件更新为正常,且父级目录到跟目录
        List<String> delFileIdList = Arrays.asList(filedArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDel_flag(FileDelFlagEnums.USING.getFlags());
        fileInfo.setFile_pid(Constants.ZERO_STR);
        fileInfo.setLast_update_time(new Date());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlagEnums.RECYCLE.getFlags());

        //将所选文件重命名
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item.getFile_name());
            //文件名已经存在，重命名被还原的文件名
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFile_name());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFile_name(fileName);
                this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, item.getFile_id(), userId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlags());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);

        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolder_type())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFile_id(), FileDelFlagEnums.DEL.getFlags());
            }
        }

        //删除所选文件，子目录中的文件
        if (!delFileSubFolderFileIdList.isEmpty()) {
            fileInfoMapper.delFileBatch(userId, delFileSubFolderFileIdList, null, adminOp ? null : FileDelFlagEnums.DEL.getFlags());
        }
        //删除所选文件
        fileInfoMapper.delFileBatch(userId, null, Arrays.asList(fileIdArray), adminOp ? null : FileDelFlagEnums.RECYCLE.getFlags());

        Long useSpace = fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUse_space(useSpace);
        userInfoMapper.updateByUserId(userInfo, userId);

        //设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);
    }

    @Override
    public void deleteFileByUserId(String userId) {
        fileInfoMapper.deleteFileByUserId(userId);
    }

    @Override
    public void checkRootFilePid(String rootFilePid, String userId, String fileId) {
        if (StringTools.isEmpty(fileId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(fileId)) {
            return;
        }
        checkFilePid(rootFilePid, fileId, userId);
    }
    private void checkFilePid(String rootFilePid, String fileId, String userId) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STR.equals(fileInfo.getFile_pid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFile_pid().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFile_pid(), userId);
    }

    private void checkFileName(String filePid, String userId, String fileName, Integer folderType){
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFolderType(folderType);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        Integer count = fileInfoMapper.selectCount(fileInfoQuery);
        if(count>0){
            throw new BusinessException("此目录下已经存在同名文件，请修改名称");
        }
    }

    private void findAllSubFolderFileIdList(List<String> fileIdList, String userId, String fileId, Integer delFlag){
        fileIdList.add(fileId);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFilePid(fileId);
        query.setDelFlag(delFlag);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(fileIdList, userId, fileInfo.getFile_id(), delFlag);
        }

    }

    @Async
    public void transferFile(String fileId, SessionWebUserDto webUserDto){
        //转码
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId,webUserDto.getUserId());
        try {
            //如果fileinfo不为空，并且状态为转换中
            if(fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())){
                return;
            }
            //临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() +fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            if(!fileFolder.exists()){
                fileFolder.mkdirs();
            }
            //文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFile_name());
            String month = DateUtil.format(fileInfo.getCreate_time(),DateTimePatternEnum.YYYYMM.getPattern());
            //目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()){
                targetFolder.mkdirs();
            }
            //真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            //真实文件路径
            targetFilePath = targetFolder.getPath() +  "/" + realFileName;
            //合并文件
            union(fileFolder.getPath(),targetFilePath,fileInfo.getFile_name(),true);

            //视频文件切割
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnum) {
                cutFile4Video(fileId, targetFilePath);
                //视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
                //生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        }catch (Exception e){
            logger.error("文件转码失败，文件Id:{},userId:{}",fileId,webUserDto.getUserId(),e);
            transferSuccess = false;
        }finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFile_size(new File(targetFilePath).length());
            updateInfo.setFile_cover(cover);
            updateInfo.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, webUserDto.getUserId(), updateInfo, FileStatusEnums.TRANSFER.getStatus());
        }

    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8 和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }

    private static void union(String dirPath, String toFilePath, String fileName, boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile,"rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile,"r");
                    while((len = readFile.read(b))!= -1){
                        writeFile.write(b,0,len);
                    }
                } catch (Exception e){
                    logger.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                }finally {
                    readFile.close();
                }
            }
        }catch (Exception e){
            logger.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        }finally {
            try {
                if (null != writeFile) {
                    writeFile.close();
                }
            } catch (IOException e) {
                logger.error("关闭流失败", e);
            }
            if (delSource) {
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void updateuserSpace(SessionWebUserDto webUserDto, Long useSize) {
        Integer count = userInfoMapper.updateUserSpace(webUserDto.getUserId(),useSize,null);
        if(count == 0){
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        //redis缓存中更新用户使用空间
        UserSpaceDto userSpaceDto = redisComponent.getUserSpace(webUserDto.getUserId());
        userSpaceDto.setUseSpace(userSpaceDto.getUseSpace()+useSize);
        redisComponent.saveUserSpaceUse(webUserDto.getUserId(),userSpaceDto);
    }

    @Override
    public Integer findCountByParam(FileInfoQuery param) {
        int count = fileInfoMapper.selectCount(param);
        return count;
    }


    //判断是否需要文件重命名
    private String autoRename(String filePid, String userId, String fileName){
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlags());
        fileInfoQuery.setFileName(fileName);
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            return StringTools.rename(fileName);
        }

        return fileName;
    }
}
