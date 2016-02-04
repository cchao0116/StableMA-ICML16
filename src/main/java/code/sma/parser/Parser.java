/**
 * Tongji Edu.
 * Copyright (c) 2004-2013 All Rights Reserved.
 */
package code.sma.parser;

/**
 * 
 * 数据采集，解析类。将采集到的内容，转化为内部处理类。
 * 
 * @author Hanke Chen
 * @version $Id: Parser.java, v 0.1 2013-9-6 下午4:16:35 chench Exp $
 */
public interface Parser {

    /**
     * Parse the content in dataset
     * 
     * @param line
     * @return
     */
    public Object parse(String template);

}
