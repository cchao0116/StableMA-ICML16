package code.sma.recmmd;

import java.io.Serializable;

import code.sma.core.impl.Tuples;
import code.sma.util.EvaluationMetrics;

/**
 * Abstract Class for Recommender Model
 * 
 * @author Chao Chen
 * @version $Id: Recommender.java, v 0.1 2015-6-8 下午6:59:37 Exp $
 */
public abstract class Recommender implements Serializable {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /*========================================
     * Common Variables
     *========================================*/
    /** thread id in multiple thread version */
    public int                threadId;
    /** The number of users. */
    public int                userCount;
    /** The number of items. */
    public int                itemCount;
    /** Maximum value of rating, existing in the dataset. */
    public double             maxValue;
    /** Minimum value of rating, existing in the dataset. */
    public double             minValue;

    /*========================================
     * Model Builder
     *========================================*/

    /**
     * Build a model with given training set.
     * 
     * @param rateMatrix
     * @param tMatrix
     */
    public abstract void buildModel(Tuples rateMatrix,
                                    Tuples tMatrix);

    /**
     * Build a model with given training set.
     * 
     * @param rateMatrix
     * @param tMatrix
     */
    public abstract void buildloclModel(Tuples rateMatrix,
                                        Tuples tMatrix);

    /*========================================
     * Prediction
     *========================================*/
    /**
     * Evaluate the designated algorithm with the given test data.
     * 
     * @param testMatrix The rating matrix with test data.
     * 
     * @return The result of evaluation, such as MAE, RMSE, and rank-score.
     */
    public abstract EvaluationMetrics evaluate(Tuples testMatrix);

    /**
     * return the predicted rating
     * 
     * @param u the given user index
     * @param i the given item index
     * @return the predicted rating
     */
    public abstract double predict(int u, int i);

}
