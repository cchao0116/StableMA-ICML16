package code.sma.recommender;

import code.sma.datastructure.MatlabFasionSparseMatrix;

/**
 * 
 * @author Hanke
 * @version $Id: Recommender.java, v 0.1 2015-6-8 下午6:59:37 Exp $
 */
public abstract class Recommender {
    /*========================================
     * Common Variables
     *========================================*/
    /** The number of users. */
    public int    userCount;
    /** The number of items. */
    public int    itemCount;
    /** Maximum value of rating, existing in the dataset. */
    public double maxValue;
    /** Minimum value of rating, existing in the dataset. */
    public double minValue;

    /*========================================
     * Model Builder
     *========================================*/

    /**
     * Build a model with given training set.
     * 
     * @param rateMatrix
     * @param tMatrix
     */
    public abstract void buildModel(MatlabFasionSparseMatrix rateMatrix,
                                    MatlabFasionSparseMatrix tMatrix);

    /*========================================
     * Prediction
     *========================================*/

    /**
     * return the predicted rating
     * 
     * @param u the given user index
     * @param i the given item index
     * @return the predicted rating
     */
    public abstract double predict(int u, int i);

}
