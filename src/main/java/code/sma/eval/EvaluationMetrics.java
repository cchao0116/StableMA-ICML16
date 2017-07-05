package code.sma.eval;

import code.sma.core.AbstractIterator;
import code.sma.model.Model;

/**
 * This is a unified class providing evaluation metrics,
 * including comparison of predicted ratings and rank-based metrics, etc.
 * 
 * @author Chao.Chen
 * @version $Id: EvaluationMetrics.java, v 0.1 2016年9月29日 下午2:09:04 Chao.Chen Exp $
 */
public abstract class EvaluationMetrics {
    /** Top-N recommendations*/
    protected int    N = -1;
    /** Mean Absoulte Error (MAE) */
    protected double mae;
    /** Mean Squared Error (MSE) */
    protected double mse;

    /** Rank-based Normalized Discounted Cumulative Gain (NDCG) */
    protected double ndcg;
    protected double recall;
    /** Average Precision */
    protected double avgPrecision;

    /**
     * evaluate the model in terms of RMSE and MAE
     * 
     * @param model     model to be evaluated
     * @param idata     iterator of testing data
     */
    public abstract void evalRating(Model model, AbstractIterator idata);

    public double getRecall() {
        return recall;
    }

    /**
     * Getter method for property <tt>mae</tt>.
     * 
     * @return property value of mae
     */
    public double getMAE() {
        return mae;
    }

    /**
     * Getter method for property <tt>mse</tt>.
     * 
     * @return property value of mse
     */
    public double getRMSE() {
        return Math.sqrt(mse);
    }

    /**
     * Getter method for property <tt>ndcg</tt>.
     * 
     * @return property value of ndcg
     */
    public double getNDCG() {
        return ndcg;
    }

    /**
     * Getter method for property <tt>avgPrecision</tt>.
     * 
     * @return property value of avgPrecision
     */
    public double getAvgPrecision() {
        return avgPrecision;
    }

    /**
     * Print all loss values in one line.
     * 
     * @return The one-line string to be printed.
     */
    public String printOneLine() {
        return String.format("MAE(%.6f) RMSE(%.6f) NDCG@%d(%.6f) AP(%.6f) Recall(%.6f)",
            this.getMAE(), this.getRMSE(), N, this.getNDCG(), this.getAvgPrecision(),
            this.getRecall());
    }
}
