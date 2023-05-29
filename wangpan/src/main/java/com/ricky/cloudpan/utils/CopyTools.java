package com.ricky.cloudpan.utils;

import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.entity.po.FolderVO;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.entity.vo.FileInfoVO;
import com.ricky.cloudpan.entity.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
            T t = null;
            try {
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    public static List<FileInfoVO> copyList_FileInfo_to_FileInfoVo(List<FileInfo> sList, Class<FileInfoVO> classz) {
        List<FileInfoVO> list = new ArrayList<FileInfoVO>();
        for (FileInfo s : sList) {
            FileInfoVO t = null;
            try {
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            t.setFileName(s.getFile_name());
            t.setFilePid(s.getFile_pid());
            t.setFileSize(s.getFile_size());
            t.setFileId(s.getFile_id());
            t.setFileCover(s.getFile_cover());
            t.setLastUpdateTime(s.getLast_update_time());
            t.setFolderType(s.getFolder_type());
            t.setFileCategory(s.getFile_category());
            t.setStatus(s.getStatus());
            t.setFileType(s.getFile_type());
            list.add(t);
        }
        return list;
    }

    public static List<UserInfoVO> copyList_UserInfo_to_UserInfoVo(List<UserInfo> sList, Class<UserInfoVO> classz) {
        List<UserInfoVO> list = new ArrayList<UserInfoVO>();
        for (UserInfo s : sList) {
            UserInfoVO t = null;
            try {
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            t.setUserId(s.getUser_id());
            t.setNickName(s.getNick_name());
            t.setEmail(s.getEmail());
            t.setQqAvatar(s.getQq_avatar());
            t.setLastLoginTime(s.getLast_login_time());
            t.setJoinTime(s.getJoin_time());
            t.setStatus(s.getStatus());
            t.setUseSpace(s.getUse_space());
            t.setTotalSpace(s.getTotal_space());
            list.add(t);
        }
        return list;
    }

    public static List<FolderVO> copyList_FileInfo_to_FolderVO(List<FileInfo> sList, Class<FolderVO> classz) {
        List<FolderVO> list = new ArrayList<FolderVO>();
        for (FileInfo s : sList) {
            FolderVO t = null;
            try {
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            t.setFileId(s.getFile_id());
            t.setFileName(s.getFile_name());
            list.add(t);
        }
        return list;
    }

    public static <T, S> T copy(S s, Class<T> classz) {
        T t = null;
        try {
            t = classz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s, t);
        return t;
    }

    public static FileInfoVO copy_FileInfo_to_FileInfoVo(FileInfo fileinfo, Class<FileInfoVO> classz) {
        FileInfoVO fileInfoVO = new FileInfoVO();
        fileInfoVO.setFileName(fileinfo.getFile_name());
        fileInfoVO.setFilePid(fileinfo.getFile_pid());
        fileInfoVO.setFileSize(fileinfo.getFile_size());
        fileInfoVO.setFileId(fileinfo.getFile_id());
        fileInfoVO.setFileCover(fileinfo.getFile_cover());
        fileInfoVO.setLastUpdateTime(fileinfo.getLast_update_time());
        fileInfoVO.setFolderType(fileinfo.getFolder_type());
        fileInfoVO.setFileCategory(fileinfo.getFile_category());
        fileInfoVO.setStatus(fileinfo.getStatus());
        fileInfoVO.setFileType(fileinfo.getFile_type());

        return fileInfoVO;
    }
}
