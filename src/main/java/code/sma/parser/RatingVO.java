/**
 * Tongji Edu.
 * Copyright (c) 2004-2013 All Rights Reserved.
 */
package code.sma.parser;

/**
 * Rating的扩展值类
 * 
 * @author Hanke Chen
 * @version $Id: RatingVO.java, v 0.1 2013-10-30 下午8:27:30 chench Exp $
 */
public class RatingVO {

    /** 用户id **/
    private int usrId;

    /** 电影id **/
    private int movieId;

    /** 计算评分_RP算法中，包含噪声 **/
    private float ratingCmp;

    /** 真实评分*/
    private float ratingReal;

    /**
     * 
     */
    public RatingVO() {
    }

    /**
     * @param usrId
     * @param movieId
     * @param ratingCmp
     * @param ratingReal
     */
    public RatingVO(int usrId, int movieId, Float ratingCmp, float ratingReal) {
        this.usrId = usrId;
        this.movieId = movieId;
        this.ratingCmp = ratingCmp;
        this.ratingReal = ratingReal;
    }

    /**
     * Getter method for property <tt>usrId</tt>.
     * 
     * @return property value of usrId
     */
    public int getUsrId() {
        return usrId;
    }

    /**
     * Setter method for property <tt>usrId</tt>.
     * 
     * @param usrId value to be assigned to property usrId
     */
    public void setUsrId(int usrId) {
        this.usrId = usrId;
    }

    /**
     * Getter method for property <tt>movieId</tt>.
     * 
     * @return property value of movieId
     */
    public int getMovieId() {
        return movieId;
    }

    /**
     * Setter method for property <tt>movieId</tt>.
     * 
     * @param movieId value to be assigned to property movieId
     */
    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    /**
     * Getter method for property <tt>ratingCmp</tt>.
     * 
     * @return property value of ratingCmp
     */
    public Float getRatingCmp() {
        return ratingCmp;
    }

    /**
     * Setter method for property <tt>ratingCmp</tt>.
     * 
     * @param ratingCmp value to be assigned to property ratingCmp
     */
    public void setRatingCmp(Float ratingCmp) {
        this.ratingCmp = ratingCmp;
    }

    /**
     * Getter method for property <tt>ratingReal</tt>.
     * 
     * @return property value of ratingReal
     */
    public float getRatingReal() {
        return ratingReal;
    }

    /**
     * Setter method for property <tt>ratingReal</tt>.
     * 
     * @param ratingReal value to be assigned to property ratingReal
     */
    public void setRatingReal(float ratingReal) {
        this.ratingReal = ratingReal;
    }

    /** 
     * [movieId],[userId],[ratingReal],,[ratingCmp]
     * 
     * @see edu.tongji.model.Rating#toString()
     */
    @Override
    public String toString() {
        return (new StringBuilder()).append(this.movieId).append(Rating.ELEMENT_SEPERATOR)
            .append(this.usrId).append(Rating.ELEMENT_SEPERATOR).append(this.ratingReal)
            .append(Rating.ELEMENT_SEPERATOR).append(Rating.ELEMENT_SEPERATOR)
            .append(this.ratingCmp).toString();
    }
}
