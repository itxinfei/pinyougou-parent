package com.pinyougou.common.aspect;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.pinyougou.common.annotation.Log;

/**
 * 操作日志切面（简化版）
 * 通过AOP拦截带有@Log注解的方法，记录操作日志到日志文件
 *
 * @author Administrator
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger logger = Logger.getLogger(LogAspect.class);

    /**
     * 环绕通知：拦截所有带有@Log注解的方法
     */
    @Around("execution(* com.pinyougou..controller..*(..)) && @annotation(com.pinyougou.common.annotation.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 1. 获取方法信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);

        // 2. 获取类信息
        String className = point.getTarget().getClass().getName();
        String methodName = method.getName();

        // 3. 构建方法描述
        String methodDescription = logAnnotation.value();
        if (methodDescription.isEmpty()) {
            methodDescription = className + "." + methodName + "()";
        }

        // 4. 执行目标方法
        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            // 5. 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;

            // 6. 记录日志
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【操作日志】");
            logMsg.append("操作：").append(methodDescription).append(" | ");
            logMsg.append("类型：").append(logAnnotation.type()).append(" | ");
            logMsg.append("耗时：").append(executionTime).append("ms | ");
            logMsg.append("成功：").append(success ? "是" : "否");

            if (errorMessage != null) {
                logMsg.append(" | 错误：").append(errorMessage);
            }

            if (success) {
                logger.info(logMsg);
            } else {
                logger.error(logMsg);
            }
        }
    }
}
