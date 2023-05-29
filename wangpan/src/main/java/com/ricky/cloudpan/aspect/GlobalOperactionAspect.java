package com.ricky.cloudpan.aspect;

import com.ricky.cloudpan.annotation.GlobalInterceptor;
import com.ricky.cloudpan.annotation.VerifyParam;
import com.ricky.cloudpan.entity.config.AppConfig;
import com.ricky.cloudpan.entity.constants.Constants;
import com.ricky.cloudpan.entity.dto.SessionWebUserDto;
import com.ricky.cloudpan.entity.enums.ResponseCodeEnum;
import com.ricky.cloudpan.entity.po.UserInfo;
import com.ricky.cloudpan.exception.BusinessException;
import com.ricky.cloudpan.query.UserInfoQuery;
import com.ricky.cloudpan.service.UserInfoService;
import com.ricky.cloudpan.utils.StringTools;
import com.ricky.cloudpan.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Aspect
@Component("globalOperactionAspect")
public class GlobalOperactionAspect {
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";
    @Resource
    private UserInfoService userInfoService;
    @Resource
    private AppConfig appConfig;
    @Pointcut("@annotation(com.ricky.cloudpan.annotation.GlobalInterceptor)")
    private void requestInterceptor(){

    }

    @Before("requestInterceptor()")
    private void interceptorDo(JoinPoint point) throws BusinessException {
        try {
            Object target = point.getTarget();
            System.out.println("进入拦截器========");
            Object[] arguments = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if(null == interceptor){
                return;
            }
            //校验登录
            if(interceptor.checkParams() || interceptor.checkAdmin()){
                checkLogin(interceptor.checkAdmin());
            }

            //校验参数
            if(interceptor.checkParams()){
                validateParams(method,arguments);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    private void checkLogin(boolean checkAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);

        if (userDto == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                userDto = new SessionWebUserDto();
                userDto.setUserId(userInfo.getUser_id());
                userDto.setNickName(userInfo.getNick_name());
                userDto.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, userDto);
            }
        }

        if (null == userDto) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        if (checkAdmin && !userDto.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

    private void validateParams(Method m, Object[] arguments) {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if(verifyParam == null){
                continue;
            }
            //基本数据类型
            if(TYPE_STRING.equals(
                    parameter.getParameterizedType().getTypeName())
                    || TYPE_LONG.equals(parameter.getParameterizedType().getTypeName())
                    || TYPE_INTEGER.equals(parameter.getParameterizedType().getTypeName()))
            {
                checkValue(value,verifyParam);
            }else {
                checkObjValue();
            }
        }
    }

    private void checkObjValue() {
        //检查对象
    }

    //检查值
    private void checkValue(Object value,VerifyParam verifyParam){
        //判断value是否为空
        Boolean isEmpty = value == null ||
                StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();
        //校验空
        if(isEmpty && verifyParam.required()){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        //校验长度
        if(!isEmpty &&
                (verifyParam.max() != -1
                        && verifyParam.max() < length ||
                        verifyParam.min() != -1 && verifyParam.min() > length)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //校验正则
        if(!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex())
                && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

}
