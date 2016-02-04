/**
 * Tongji Edu.
 * Copyright (c) 2004-2014 All Rights Reserved.
 */
package code.sma.parser;

import code.sma.util.ExceptionUtil;
import code.sma.util.StringUtil;

/**
 * RatingVO解析模板，向上兼容Rating模板
 * 
 * @author Hanke Chen
 * @version $Id: NetflixRatingVOTemplateParser.java, v 0.1 23 Apr 2014 13:21:00
 *          chench Exp $
 */
public class NetflixParser implements Parser {

    /** 分隔符正则表达式 */
    private final static String SAPERATOR_EXPRESSION = "\\,";

    /** 
     * @see edu.tongji.parser.Parser#parse(java.lang.String)
     */
    @Override
    public Object parse(String template) {
        if (StringUtil.isEmpty(template) | (template.indexOf(":") != -1)) {
            return null;
        }

        try {
            String[] elements = template.split(SAPERATOR_EXPRESSION);
            int movieId = Integer.valueOf(elements[0]).intValue();
            int userId = Integer.valueOf(elements[1]).intValue();
            Float ratingReal = Float.valueOf(elements[2]);
            Float ratingCmp = (elements.length == 5 && StringUtil.isNotBlank(elements[4]))
                ? Float.valueOf(elements[4]) : ratingReal;
            return new RatingVO(userId, movieId, ratingCmp, ratingReal);
        } catch (Exception e) {
            ExceptionUtil.caught(e, "解析ParserTemplate错误，内容: " + template);
        }

        return null;
    }

}
