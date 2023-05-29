package com.ricky.cloudpan.mappers;

import com.ricky.cloudpan.entity.po.FileShare;
import com.ricky.cloudpan.query.FileShareQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FileShareMapper {
    public List<FileShare> selectList(@Param("query") FileShareQuery param);

    Integer selectCount(@Param("query") FileShareQuery param);

    Integer insert(@Param("bean")FileShare share);

    FileShare selectByShareId(@Param("shareId")String shareId);

    void updateShareShowCount(@Param("shareId")String shareId);
}
