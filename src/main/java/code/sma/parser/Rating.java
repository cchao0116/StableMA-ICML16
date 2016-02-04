/**
 * Tongji Edu.
 * Copyright (c) 2004-2013 All Rights Reserved.
 */
package code.sma.parser;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 
 * @author Hanke Chen
 * @version $Id: Rating.java, v 0.1 2013-9-6 下午3:42:30 chench Exp $
 */
public class Rating implements Serializable {

    /**  serialVersionUID*/
    private static final long serialVersionUID = -7837456678460417103L;

    /** 元素发个符号 */
    public static final char ELEMENT_SEPERATOR = ',';

    /** 主键 */
    private int id;

    /** 用户id **/
    private int usrId;

    /** 电影id **/
    private int movieId;

    /** 评分指 **/
    private int rating;

    /** 评分时间 **/
    private Timestamp time;

    /**
     * 
     */
    public Rating() {
    }

    /**
     * @param usrId
     * @param movieId
     * @param rating
     * @param time
     */
    public Rating(int usrId, int movieId, int rating, Timestamp time) {
        this.usrId = usrId;
        this.movieId = movieId;
        this.rating = rating;
        this.time = time;
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
     * Getter method for property <tt>rating</tt>.
     * 
     * @return property value of rating
     */
    public int getRating() {
        return rating;
    }

    /**
     * Setter method for property <tt>rating</tt>.
     * 
     * @param rating value to be assigned to property rating
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Getter method for property <tt>time</tt>.
     * 
     * @return property value of time
     */
    public Timestamp getTime() {
        return time;
    }

    /**
     * Setter method for property <tt>time</tt>.
     * 
     * @param time value to be assigned to property time
     */
    public void setTime(Timestamp time) {
        this.time = time;
    }

    /**
     * Getter method for property <tt>id</tt>.
     * 
     * @return property value of id
     */
    public int getId() {
        return id;
    }

    /**
     * Setter method for property <tt>id</tt>.
     * 
     * @param id value to be assigned to property id
     */
    public void setId(int id) {
        this.id = id;
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Rating) {
            return this.usrId == ((Rating) obj).usrId;
        }

        return super.equals(obj);
    }

    /** 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Integer.valueOf(this.usrId).hashCode();

    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = (new StringBuilder()).append(this.usrId)
            .append(Rating.ELEMENT_SEPERATOR).append(this.movieId).append(Rating.ELEMENT_SEPERATOR)
            .append(this.rating).append(Rating.ELEMENT_SEPERATOR);
        if (this.time == null) {
            stringBuilder.append(Rating.ELEMENT_SEPERATOR);
        } else {
            stringBuilder.append(this.time.toString());
        }

        return stringBuilder.toString();
    }

}
