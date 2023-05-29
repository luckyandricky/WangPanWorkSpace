package com.ricky.cloudpan.controller;

import com.ricky.cloudpan.annotation.GlobalInterceptor;
import com.ricky.cloudpan.annotation.VerifyParam;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.enums.FileDelFlagEnums;
import com.ricky.cloudpan.entity.vo.FileInfoVO;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.query.FileInfoQuery;
import com.ricky.cloudpan.service.FileInfoService;
import com.ricky.cloudpan.service.impl.FileInfoServiceImpl;
import com.ricky.cloudpan.utils.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("recycleController")
@RequestMapping("/recycle")
public class RecycleController extends ABaseController{
    @Resource
    private FileInfoService fileInfoService;

    @RequestMapping("/loadRecycleList")
    public Result loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize){
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setPageNo(pageNo);
        fileInfoQuery.setPageSize(pageSize);
        fileInfoQuery.setUserId(getUserInfoFromSession(session).getUserId());
        fileInfoQuery.setOrderBy("recovery_time desc");
        fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlags());
        PaginationResultVO result = fileInfoService.findListByPage(fileInfoQuery);
        return Result.of_success(convert2PaginationVO2(result, FileInfoVO.class));
    }

    //还原
    @RequestMapping("/recoverFile")
    @GlobalInterceptor(checkParams = true)
    public Result recoverFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.recoverFileBatch(webUserDto.getUserId(), fileIds);
        return Result.of_success(null);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public Result delFile(HttpSession session, @VerifyParam(required = true) String fileIds){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.delFileBatch(webUserDto.getUserId(),fileIds,false);
        return Result.of_success(null);
    }
}
