/**
 * Tongji Edu.
 * Copyright (c) 2004-2013 All Rights Reserved.
 */
package code.sma.util;

import org.apache.log4j.Logger;


/**
 * 捕捉到异常的时候，我们通常会使用<code>logger.error("xxxx",e)</code>方式打印日常堆栈日志<br>
 * 但是这种方式会造成错误日志打印两遍，精益求精，
 * 
 * @author Hanke Chen
 * @version $Id: ExceptionUtil.java, v 0.1 2013-9-9 下午1:35:51 chench Exp $
 */
public class ExceptionUtil {

    /** logger */
    private static final Logger logger = Logger.getLogger(LoggerDefineConstant.COMMON_ERROR);
    
    /**
     * 禁用构造函数
     */
    private ExceptionUtil() {
        // 禁用构造函数
    }

    /**
     * 捕捉错误日志并输出到日志文件：common-error.log
     * 
     * @param e 异常堆栈
     * @param message 错误日志上下文信息描述，尽量带上业务特征
     */
    public static void caught(Throwable e, Object... message) {
        logger.error(LoggerUtil.getLogString(message), e);
    }
    
}
