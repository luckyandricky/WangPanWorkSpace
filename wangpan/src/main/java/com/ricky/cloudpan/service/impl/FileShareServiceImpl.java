package com.ricky.cloudpan.service.impl;

import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.SessionShareDto;
import com.ricky.cloudpan.entity.enums.PageSize;
import com.ricky.cloudpan.entity.enums.ResponseCodeEnum;
import com.ricky.cloudpan.entity.enums.ShareValidTypeEnums;
import com.ricky.cloudpan.entity.po.FileShare;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.mappers.FileShareMapper;
import com.ricky.cloudpan.query.FileShareQuery;
import com.ricky.cloudpan.query.SimplePage;
import com.ricky.cloudpan.service.FileShareService;
import com.ricky.cloudpan.utils.DateUtil;
import com.ricky.cloudpan.utils.StringTools;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.Session;
import java.util.Date;
import java.util.List;

@Service("fileShareService")
public class FileShareServiceImpl implements FileShareService {

    @Resource
    private FileShareMapper fileShareMapper;

    @Override
    public List<FileShare> findListByParam(FileShareQuery param) {
        return fileShareMapper.selectList(param);
    }

    public Integer findCountByParam(FileShareQuery param){
        return fileShareMapper.selectCount(param);
    }

    @Override
    public void saveShare(FileShare share) {
        ShareValidTypeEnums typeEnum = ShareValidTypeEnums.getByType(share.getValidType());
        if(null == typeEnum){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (typeEnum != ShareValidTypeEnums.FOREVER) {
            share.setExpireTime(DateUtil.getAfterDate(typeEnum.getDays()));
        }
        Date curDate = new Date();
        share.setShareTime(curDate);
        if(StringTools.isEmpty(share.getCode())){
            share.setCode(StringTools.getRandomString(Constants.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
        fileShareMapper.insert(share);

    }

    @Override
    public FileShare getFileShareByShareId(String shareId) {
        return fileShareMapper.selectByShareId(shareId);

    }

    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        FileShare share = fileShareMapper.selectByShareId(shareId);
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        //如果校验码错误
        if (!share.getCode().equals(code)) {
            throw new BusinessException("提取码错误");
        }
        fileShareMapper.updateShareShowCount(shareId);
        SessionShareDto sessionShareDto = new SessionShareDto();
        sessionShareDto.setShareUserId(shareId);
        sessionShareDto.setShareUserId(share.getUserId());
        sessionShareDto.setFileId(share.getFileId());
        sessionShareDto.setExpireTime(share.getExpireTime());
        return sessionShareDto;
    }

    @Override
    public PaginationResultVO<FileShare> findListByPage(FileShareQuery param) {
        int count = findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(1, count, 15);
        //暂时屏蔽分页信息，查找数据库的时候不limit
        //param.setSimplePage(page);
        List<FileShare> list = this.findListByParam(param);
        PaginationResultVO<FileShare> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }
}
