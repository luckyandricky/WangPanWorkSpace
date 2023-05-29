package com.ricky.cloudpan.controller;


import com.ricky.cloudpan.annotation.GlobalInterceptor;
import com.ricky.cloudpan.annotation.VerifyParam;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.po.FileShare;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.query.FileShareQuery;
import com.ricky.cloudpan.service.FileShareService;
import com.ricky.cloudpan.utils.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/share")
public class ShareController extends ABaseController{

    @Resource
    private FileShareService fileShareService;

    @RequestMapping("/loadShareList")
    @GlobalInterceptor(checkParams = true)
    public Result loadShareList(HttpSession session,
                                FileShareQuery query,
                                @RequestParam("pageNo")String pageNo){
        query.setOrderBy("share_time desc");
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        query.setUserId(userDto.getUserId());
        query.setQueryFileName(true);
        PaginationResultVO resultVO = fileShareService.findListByPage(query);

        return Result.of_success(resultVO);
    }

    @RequestMapping("/shareFile")
    @GlobalInterceptor(checkParams = true)
    public Result shareFile(HttpSession session,
                                @VerifyParam(required = true) String fileId,
                                @VerifyParam(required = true) Integer validType,
                                String code) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setValidType(validType);
        share.setCode(code);
        share.setUserId(webUserDto.getUserId());
        fileShareService.saveShare(share);
        return Result.of_success(share);
    }
}
