package com.ricky.cloudpan.controller;

import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.SessionShareDto;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.po.FileInfo;
import com.ricky.cloudpan.entity.po.FileInfo2;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.entity.vo.FileInfoVO;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.entity.vo.UserInfoVO;
import com.ricky.cloudpan.utils.CopyTools;
import com.ricky.cloudpan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.io.*;
import java.util.List;

public class ABaseController {
    private static final Logger logger = LoggerFactory.getLogger(ABaseController.class);

    protected <S, T> PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> result, Class<T> classz) {
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList(result.getList(), classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }

    protected <S, T> PaginationResultVO<FileInfoVO> convert2PaginationVO2(PaginationResultVO<FileInfo> result, Class<FileInfoVO> classz) {
        PaginationResultVO<FileInfoVO> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList_FileInfo_to_FileInfoVo(result.getList(), classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }

    protected FileInfo2 convert2PaginationVO3(FileInfo result) {
        FileInfo2 fileInfo2 = new FileInfo2();
        fileInfo2.setFileId(result.getFile_id());
        fileInfo2.setUserId(result.getUser_id());
        fileInfo2.setFolderType(result.getFolder_type());
        fileInfo2.setFilePid(result.getFile_pid());
        fileInfo2.setFileName(result.getFile_name());
        fileInfo2.setCreateTime(result.getCreate_time());
        fileInfo2.setLastUpdateTime(result.getLast_update_time());
        fileInfo2.setStatus(result.getStatus());
        fileInfo2.setDelFlag(result.getDel_flag());

        return fileInfo2;
    }

    protected <S, T> PaginationResultVO<UserInfoVO> convert2PaginationVO4(PaginationResultVO<UserInfo> result, Class<UserInfoVO> classz) {
        PaginationResultVO<UserInfoVO> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList_UserInfo_to_UserInfoVo(result.getList(), classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }

    protected SessionWebUserDto getUserInfoFromSession(HttpSession session){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return sessionWebUserDto;
    }

    protected SessionShareDto getSessionShareFromSession(HttpSession session, String shareId){
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        return sessionShareDto;
    }

    protected void readFile(HttpServletResponse response, String filePath){
        if(!StringTools.pathIsOk(filePath)){
            return;
        }
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if(!file.exists()){
                return;
            }
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData))!=-1){
                out.write(byteData,0,len);
            }
            out.flush();
        }catch (Exception e){
            logger.error("读取文件异常",e);
        }finally {
            if(out != null){
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常",e);
                }
            }
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常",e);
                }
            }
        }
    }
}
