package com.ricky.cloudpan.entity.enums;

public enum PageSize {
    SIZE15(15),SIZE20(20),SIZE30(30);
    private Integer size;

    PageSize(Integer size) {
        this.size = size;
    }

    public Integer getSize() {
        return size;
    }
}
