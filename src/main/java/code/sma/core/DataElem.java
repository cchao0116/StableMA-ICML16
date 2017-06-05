package code.sma.core;

import code.sma.core.impl.CRefVector;

/**
 * data element, the smallest unit of the feature-based data
 * 
 * @author Chao.Chen
 * @version $Id: DataElem.java, v 0.1 2017年6月1日 上午10:35:45 Chao.Chen Exp $
 */
public class DataElem {
    /** result label, rate or {0,1} for classification*/
    protected float      label;
    /** number of nonzero global feature*/
    protected short      num_global;
    /** number of nonzero user feature*/
    protected short      num_ufactor;
    /** number of nonzero item feature*/
    protected short      num_ifacotr;

    /** array of global feature index*/
    protected CRefVector index_global;
    /** array of user feature index, i.e., uIDs*/
    protected CRefVector index_user;
    /** array of item feature index, i.e., iIDs*/
    protected CRefVector index_item;

    /** array of global feature value */
    protected CRefVector value_global;
    /** array of global feature value */
    protected CRefVector value_ufactor;
    /** array of global feature value */
    protected CRefVector value_ifactor;

    public DataElem() {
    }

    public DataElem(float label) {
        super();
        this.label = label;
    }

    public int getIndex_global(int i) {
        return index_global.intValue(i);
    }

    public int getIndex_user(int i) {
        return index_user.intValue(i);
    }

    public int getIndex_item(int i) {
        return index_item.intValue(i);
    }

    public float getValue_global(int i) {
        return value_global.floatValue(i);
    }

    public float getValue_ufactor(int i) {
        return value_ufactor.floatValue(i);
    }

    public float getValue_ifactor(int i) {
        return value_ifactor.floatValue(i);
    }

    /**
     * Getter method for property <tt>label</tt>.
     * 
     * @return property value of label
     */
    public float getLabel() {
        return label;
    }

    /**
     * Setter method for property <tt>label</tt>.
     * 
     * @param label value to be assigned to property label
     */
    public void setLabel(float label) {
        this.label = label;
    }

    /**
     * Getter method for property <tt>num_global</tt>.
     * 
     * @return property value of num_global
     */
    public short getNum_global() {
        return num_global;
    }

    /**
     * Setter method for property <tt>num_global</tt>.
     * 
     * @param num_global value to be assigned to property num_global
     */
    public void setNum_global(short num_global) {
        this.num_global = num_global;
    }

    /**
     * Getter method for property <tt>num_ufactor</tt>.
     * 
     * @return property value of num_ufactor
     */
    public short getNum_ufactor() {
        return num_ufactor;
    }

    /**
     * Setter method for property <tt>num_ufactor</tt>.
     * 
     * @param num_ufactor value to be assigned to property num_ufactor
     */
    public void setNum_ufactor(short num_ufactor) {
        this.num_ufactor = num_ufactor;
    }

    /**
     * Getter method for property <tt>num_ifacotr</tt>.
     * 
     * @return property value of num_ifacotr
     */
    public short getNum_ifacotr() {
        return num_ifacotr;
    }

    /**
     * Setter method for property <tt>num_ifacotr</tt>.
     * 
     * @param num_ifacotr value to be assigned to property num_ifacotr
     */
    public void setNum_ifacotr(short num_ifacotr) {
        this.num_ifacotr = num_ifacotr;
    }

    /**
     * Getter method for property <tt>index_global</tt>.
     * 
     * @return property value of index_global
     */
    public CRefVector getIndex_global() {
        return index_global;
    }

    /**
     * Setter method for property <tt>index_global</tt>.
     * 
     * @param index_global value to be assigned to property index_global
     */
    public void setIndex_global(CRefVector index_global) {
        this.index_global = index_global;
    }

    /**
     * Getter method for property <tt>index_user</tt>.
     * 
     * @return property value of index_user
     */
    public CRefVector getIndex_user() {
        return index_user;
    }

    /**
     * Setter method for property <tt>index_user</tt>.
     * 
     * @param index_user value to be assigned to property index_user
     */
    public void setIndex_user(CRefVector index_user) {
        this.index_user = index_user;
    }

    /**
     * Getter method for property <tt>index_item</tt>.
     * 
     * @return property value of index_item
     */
    public CRefVector getIndex_item() {
        return index_item;
    }

    /**
     * Setter method for property <tt>index_item</tt>.
     * 
     * @param index_item value to be assigned to property index_item
     */
    public void setIndex_item(CRefVector index_item) {
        this.index_item = index_item;
    }

    /**
     * Getter method for property <tt>value_global</tt>.
     * 
     * @return property value of value_global
     */
    public CRefVector getValue_global() {
        return value_global;
    }

    /**
     * Setter method for property <tt>value_global</tt>.
     * 
     * @param value_global value to be assigned to property value_global
     */
    public void setValue_global(CRefVector value_global) {
        this.value_global = value_global;
    }

    /**
     * Getter method for property <tt>value_ufactor</tt>.
     * 
     * @return property value of value_ufactor
     */
    public CRefVector getValue_ufactor() {
        return value_ufactor;
    }

    /**
     * Setter method for property <tt>value_ufactor</tt>.
     * 
     * @param value_ufactor value to be assigned to property value_ufactor
     */
    public void setValue_ufactor(CRefVector value_ufactor) {
        this.value_ufactor = value_ufactor;
    }

    /**
     * Getter method for property <tt>value_ifactor</tt>.
     * 
     * @return property value of value_ifactor
     */
    public CRefVector getValue_ifactor() {
        return value_ifactor;
    }

    /**
     * Setter method for property <tt>value_ifactor</tt>.
     * 
     * @param value_ifactor value to be assigned to property value_ifactor
     */
    public void setValue_ifactor(CRefVector value_ifactor) {
        this.value_ifactor = value_ifactor;
    }

}