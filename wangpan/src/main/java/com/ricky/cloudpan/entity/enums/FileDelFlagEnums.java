package com.ricky.cloudpan.entity.enums;

public enum FileDelFlagEnums {
    DEL(0,"删除"),
    RECYCLE(1,"回收站"),
    USING(2,"使用中");

    private Integer flags;
    private String desc;

    FileDelFlagEnums(Integer flags, String desc) {
        this.flags = flags;
        this.desc = desc;
    }

    public Integer getFlags() {
        return flags;
    }

    public String getDesc() {
        return desc;
    }
}
