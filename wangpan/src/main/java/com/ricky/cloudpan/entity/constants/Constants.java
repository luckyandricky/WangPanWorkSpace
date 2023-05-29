package com.ricky.cloudpan.entity.constants;

//常量
public class Constants {
    public static final String CHECK_CODE_KEY = "check_code_key";
    public static final String CHECK_CODE_EMAIL = "check_code_email";

    public static final Integer LENGTH = 5;

    public static final Integer LENGTH_5 = 5;
    public static final Integer LENGTH_15 = 15;
    public static final Integer LENGTH_10 = 10;
    public static final Integer LENGTH_20 = 20;
    public static final Integer LENGTH_50 = 50;
    public static final Integer LENGTH_150 = 150;
    public static final Integer ZERO = 0;

    public static final String ZERO_STR = "0";

    //容量
    public static final Long MB = 1024 * 1024L;

    /**
     * redis key相关
     */
    /**
     * 过期时间 1分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60;
    public static final Integer REDIS_KEY_EXPIRES_FIVE_MIN = REDIS_KEY_EXPIRES_ONE_MIN * 5;
    /**
     * 过期时间 1天
     */
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;
    //过期时间1分钟
    public static final Integer REDIS_KEY_EXPIRES_ONE_HOUR = REDIS_KEY_EXPIRES_ONE_MIN * 60;

    public static final String REDIS_KEY_SETTING = "cloudpan:syssetting";
    public static final String REDIS_KEY_USER_SPACE_USE = "cloudpan:user:spaceuse:";
    public static final String REDIS_KEY_DOWNLOAD = "cloudpan:download:";
    public static final String FILE_FOLDER_FILE = "/file/";

    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";
    public static final String AVATAR_SUBFIX = ".jpg";

    public static final String AVATAR_DEFAULT = "default_avatar.jpg";


    public static final String SESSION_KEY = "session_key";

    public static final String SESSION_SHARE_KEY = "session_share_key_";

    //文件上传
    public static final String FILE_FOLDER_TEMP = "/temp/";
    public static final String IMAGE_PNG_SUFFIX = ".png";
    public static final String TS_NAME = "index.ts";
    public static final String M3U8_NAME = "index.m3u8";
    public static final String REDIS_KEY_USER_FILE_TEMP_SIZE = "cloudpan:user:file:temp:";


}
