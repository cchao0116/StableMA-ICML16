package code.sma.recmmd.tree.gbm;

import code.sma.core.AbstractMatrix;
import code.sma.model.AbstractModel;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

/**
 * Booster
 * 
 * @author Chao.Chen
 * @version $Id: Booster.java, v 0.1 Jun 29, 2017 5:37:22 PM Exp $
 */
public interface Booster {

    /**
     * train the booster
     * 
     * @param train     training data
     * @param test      testing data
     * @param grad      the gradients of each training data
     * @param hess      the hessians of each training data
     * @return      the resulting booster
     */
    public AbstractModel doBoost(AbstractMatrix train, AbstractMatrix test, FloatArrayList grad,
                                 FloatArrayList hess);

}
