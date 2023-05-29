package com.ricky.cloudpan.service;

import com.ricky.cloudpan.entity.dto.SessionShareDto;
import com.ricky.cloudpan.entity.po.FileShare;
import com.ricky.cloudpan.entity.vo.PaginationResultVO;
import com.ricky.cloudpan.query.FileShareQuery;

import java.util.List;

public interface FileShareService {
    List<FileShare> findListByParam(FileShareQuery param);
    PaginationResultVO<FileShare> findListByPage(FileShareQuery query);

    public Integer findCountByParam(FileShareQuery param);

    void saveShare(FileShare share);

    FileShare getFileShareByShareId(String shareId);

    SessionShareDto checkShareCode(String shareId, String code);
}
