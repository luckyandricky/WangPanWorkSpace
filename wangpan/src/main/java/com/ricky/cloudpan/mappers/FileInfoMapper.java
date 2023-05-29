package com.ricky.cloudpan.mappers;

import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.query.FileInfoQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FileInfoMapper {
    Integer selectCount(@Param("query") FileInfoQuery param);

    List<FileInfo> selectList(@Param("query") FileInfoQuery param);

    Long selectUseSpace(String userId);

    Integer insert(@Param("bean") FileInfo fileInfo);

    FileInfo selectByFileIdAndUserId(String fileId, String userId);

    void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") String userId, @Param("bean") FileInfo updateInfo, @Param("oldStatus")Integer oldStatus);

    void updateFileDelFlagBatch(@Param("bean")FileInfo updateInfo,
                                 @Param("userId")String userId,
                                 @Param("filePidList")List<String> delFilePidList,
                                 @Param("fileIdList") List<String> fileIdList,
                                 @Param("oldDelFlag") Integer oldDelFlag);

    Integer updateByFileIdAndUserId(@Param("bean")FileInfo dbInfo,@Param("fileId") String fileId, @Param("userId") String userId);

    void delFileBatch(@Param("userId")String userId,
                      @Param("filePidList")List<String> delFileSubFolderFileIdList,
                      @Param("fileIdList") List<String> fileIdList,
                      @Param("oldDelFlag") Integer oldDelFlag);

    void deleteFileByUserId(String userId);
}
