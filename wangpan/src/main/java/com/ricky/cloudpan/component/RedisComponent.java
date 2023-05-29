package com.ricky.cloudpan.component;

import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.DownloadFileDto;
import com.ricky.cloudpan.entity.dto.SysSetingDto;
import com.ricky.cloudpan.entity.dto.UserSpaceDto;
import com.ricky.cloudpan.mappers.FileInfoMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private FileInfoMapper fileInfoMapper;

    //获取系统设置
    public SysSetingDto getSysSettingDto(){
        SysSetingDto sysSetingDto = (SysSetingDto) redisUtils.get(Constants.REDIS_KEY_SETTING);
        if(null == sysSetingDto){
            sysSetingDto = new SysSetingDto();
            redisUtils.set(Constants.REDIS_KEY_SETTING,sysSetingDto);
        }
        return sysSetingDto;
    }

    //保存已使用的空间
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto){
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE+userId,userSpaceDto,Constants.REDIS_KEY_EXPIRES_DAY);
    }

    public UserSpaceDto getUserSpace(String userId){
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE+userId);
        if(userSpaceDto == null){
            userSpaceDto = new UserSpaceDto();
            userSpaceDto.setUseSpace(0L);
            userSpaceDto.setTotalSpace(getSysSettingDto().getUserInitUseSpace() * Constants.MB);
            saveUserSpaceUse(userId,userSpaceDto);
        }
        return userSpaceDto;
    }

    public UserSpaceDto getUserSpaceUse(String userId){
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE+userId);
        if(null == spaceDto){
            spaceDto = new UserSpaceDto();
            Long useSpace = fileInfoMapper.selectUseSpace(userId);
            spaceDto.setTotalSpace(useSpace);
            spaceDto.setTotalSpace(getSysSettingDto().getUserInitUseSpace() * Constants.MB);
            redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY );
        }
        return spaceDto;
    }

    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }

    private Long getFileSizeFromRedis(String key){
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null){
            return 0L;
        }
        if(sizeObj instanceof Integer){
            return ((Integer) sizeObj).longValue();
        }else if(sizeObj instanceof Long){
            return (Long) sizeObj;
        }
        return 0L;
    }

    public void saveFileTempSize(String userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        //更新redis，生存周期一小时
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE +userId+fileId,currentSize+fileSize,Constants.REDIS_KEY_EXPIRES_ONE_HOUR);

    }

    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        //设置5分钟时效
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
        //System.out.println("sdfdsfdsfd");
    }

    public DownloadFileDto getDownloadCode(String code){
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD+code);
    }

    //保存设置
    public void saveSysSettingsDto(SysSetingDto sysSettingsDto) {
        redisUtils.set(Constants.REDIS_KEY_SETTING,sysSettingsDto);
    }


}
