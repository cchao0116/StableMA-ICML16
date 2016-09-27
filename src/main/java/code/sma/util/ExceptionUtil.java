package code.sma.util;

import org.apache.log4j.Logger;

/**
 * a specialized logger-related class to capture the error message
 * 
 * @author Chao Chen
 * @version $Id: ExceptionUtil.java, v 0.1 2013-9-9 下午1:35:51 chench Exp $
 */
public class ExceptionUtil {

    /** logger */
    private static final Logger logger = Logger.getLogger(LoggerDefineConstant.COMMON_ERROR);

    /**
     * forbidden constructions
     */
    private ExceptionUtil() {
        // forbidden constructions
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
