/**
 * Tongji Edu.
 * Copyright (c) 2004-2015 All Rights Reserved.
 */
package code.sma.parser;

/**
 * 
 * @author Hanke Chen
 * @version $Id: Movie1MParser.java, v 0.1 2015-1-25 下午9:35:36 chench Exp $
 */
public class Ml1mParser implements Parser {

    /** 分隔符正则表达式 */
    private static String SAPERATOR_EXPRESSION = "\t";

    /** 
     * @see edu.tongji.parser.Parser#parse(java.lang.String)
     */
    @Override
    public Object parse(String template) {
        RatingVO rating = new RatingVO();
        String[] elements = template.split(SAPERATOR_EXPRESSION);
        rating.setUsrId(Integer.valueOf(elements[0]));
        rating.setMovieId(Integer.valueOf(elements[1]));
        rating.setRatingReal(Float.valueOf(elements[2]));
        return rating;
    }
}
