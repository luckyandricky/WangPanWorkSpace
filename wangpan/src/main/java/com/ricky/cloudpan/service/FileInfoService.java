package com.ricky.cloudpan.service;

import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.dto.UploadResultDto;
import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.query.FileInfoQuery;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileInfoService {

    PaginationResultVO findListByPage(FileInfoQuery query);


    Integer findCountByParam(FileInfoQuery param);

    List<FileInfo> findListByParam(FileInfoQuery param);


    UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

    void removeFile2RecycleBatch(String userId, String fileIds);

    FileInfo newFolder(String filePid, String userId, String folderName);

    FileInfo rename(String fileId, String userId, String fileName);

    void changeFileFolder(String fileIds, String filePid, String userId);

    void recoverFileBatch(String userId, String fileIds);

    void delFileBatch(String userId, String fileIds, Boolean adminOp);

    void deleteFileByUserId(String userId);

    void checkRootFilePid(String fileId, String shareUserId, String filePid);
}
