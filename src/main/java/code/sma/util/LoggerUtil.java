package code.sma.util;

import org.apache.log4j.Logger;

/**
 * 
 * 规范化日志打印工具，注意日志的级别选择：<br>
 * 
 * <p>
 *   <ol>
 *     <li>DEBUG <b>开发环境</b>应用调试，输出详细的应用状态
 *     <li>INFO <b>生产环境</b>运行状态观察，输出应用生命周期的中<b>正常重要事件</b>
 *     <li>WARN <b>生产环境</b>故障诊断，输出应用中的<b>可预期的异常事件</b>
 *     <li>ERROR <b>生产环境</b>境故障诊断，输出应用中的<b>未预期的异常事件</b>
 *   </ol>
 * </p>
 * 
 * <p>注意：ERROR日志记录请使用{@link ExceptionUtil}，避免日志的重复记录</p>
 * @author Hanke Chen
 * @version $Id: LogUtil.java, v 0.1 2013-7-11 下午8:55:44 chench Exp $
 */
public final class LoggerUtil {

    /** 线程编号修饰符 **/
    private final static char THREAD_RIGHT_TAG = '[';

    /** 线程编号修饰符 **/
    private final static char THREAD_LEFT_TAG  = ']';

    /** 换行符 **/
    public final static char  ENTERSTR         = '\n';

    /** 逗号  **/
    public final static char  COMMA            = ',';

    /**
     * 禁用构造函数
     */
    private LoggerUtil() {
        //禁用构造函数
    }

    /**
     * 生成<font color="blue">调试</font>级别日志<br>
     * 可处理任意多个输入参数，并避免在日志级别不够时字符串拼接带来的资源浪费
     * 
     * @param logger
     * @param obj
     */
    public static void debug(Logger logger, Object... obj) {
        if (logger.isDebugEnabled()) {
            logger.debug(getLogString(obj));
        }
    }

    /**
     * 生成<font color="blue">通知</font>级别日志<br>
     * 可处理任意多个输入参数，并避免在日志级别不够时字符串拼接带来的资源浪费
     * 
     * @param logger
     * @param obj
     */
    public static void info(Logger logger, Object... obj) {
        if (logger.isInfoEnabled()) {
            logger.info(getLogString(obj));
        }
    }

    /**
     * 生成<font color="brown">警告</font>级别日志<br>
     * 可处理任意多个输入参数，并避免在日志级别不够时字符串拼接带来的资源浪费
     * 
     * @param logger
     * @param obj
     */
    public static void warn(Logger logger, Object... obj) {
        logger.warn(getLogString(obj));
    }

    /**
     * 生成输出到日志的字符串
     * 
     * @param obj 任意个要输出到日志的参数
     * @return
     */
    public static String getLogString(Object... obj) {
        StringBuilder log = new StringBuilder();
        log.append(THREAD_RIGHT_TAG).append(Thread.currentThread().getId()).append(THREAD_LEFT_TAG);

        for (Object o : obj) {
            log.append(o.toString());
        }

        return log.toString();
    }
}
