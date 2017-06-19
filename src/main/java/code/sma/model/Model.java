package code.sma.model;

import code.sma.core.DataElem;

/**
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
     * @return      the predicted rating
     */
    public double predict(int u, int i);

    /**
     * predict the missing label based on given features
     * 
     * @param e     user/item features
     * @return      the predicted label
     */
    public double predict(DataElem e);
}
