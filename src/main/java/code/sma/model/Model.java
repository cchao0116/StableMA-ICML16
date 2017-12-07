package code.sma.model;

import code.sma.core.DataElem;

/**
 * abstract model
 * 
 * @author Chao.Chen
 * @version $Id: Model.java, v 0.1 2017年6月19日 下午5:09:25 Chao.Chen Exp $
 */
public interface Model {
    /**
     * predict the rating/preference for u-th users on i-th item
     * 
     * @param u     user index
     * @param i     item index
     * @param e     extra features
     * @return      the predicted rating
     */
    public double predict(int u, int i, DataElem... e);

    /**
     * predict the missing labels based on given features
     * 
     * @param e     user/item features
     * @return      the predicted labels
     */
    public double[] predict(DataElem e);
}
