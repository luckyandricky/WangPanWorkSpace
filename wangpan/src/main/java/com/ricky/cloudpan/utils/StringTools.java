package com.ricky.cloudpan.utils;

import com.ricky.cloudpan.entity.constants.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {

    /**
     * 生成随机数
     * @param count
     * @return
     */
    public static final String getRandomNumber(Integer count){
        return RandomStringUtils.random(count,true,false);
    }

    public static boolean isEmpty(String str){
        if(null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)){
            return true;
        }else if("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String encodeByMD5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }

    public static String rename(String fileName) {
        String fileNameReal = getFileNameNoSuffix(fileName);
        String suffix = getFileSuffix(fileName);
        return fileNameReal + "_" + getRandomString(Constants.LENGTH_5) + suffix;
    }

    public static String getFileNameNoSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        fileName = fileName.substring(0, index);
        return fileName;
    }

    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }

    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }
}
