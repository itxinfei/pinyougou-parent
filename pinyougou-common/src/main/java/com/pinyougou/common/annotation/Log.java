package com.pinyougou.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 *
 * @author Administrator
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 操作描述
     *
     * @return 操作描述
     */
    String value() default "";

    /**
     * 操作类型
     * 可选值：INSERT、UPDATE、DELETE、SELECT、EXPORT、IMPORT、LOGIN、LOGOUT等
     *
     * @return 操作类型
     */
    String type() default "OTHER";

    /**
     * 是否记录请求参数
     *
     * @return true-记录，false-不记录
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果
     *
     * @return true-记录，false-不记录
     */
    boolean recordResult() default false;

    /**
     * 是否异步保存日志（推荐true，避免影响主流程性能）
     *
     * @return true-异步，false-同步
     */
    boolean async() default true;
}
