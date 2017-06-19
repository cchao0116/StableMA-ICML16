package code.sma.util;

import code.sma.core.AbstractIterator;
import code.sma.core.DataElem;
import code.sma.model.Model;

/**
 * This is a unified class providing evaluation metrics,
 * including comparison of predicted ratings and rank-based metrics, etc.
 * 
 * @author Chao.Chen
 * @version $Id: EvaluationMetrics.java, v 0.1 2016年9月29日 下午2:09:04 Chao.Chen Exp $
 */
public class EvaluationMetrics {
    /** Top-N recommendations*/
    private int    N = -1;
    /** Mean Absoulte Error (MAE) */
    private double mae;
    /** Mean Squared Error (MSE) */
    private double mse;
    /** Rank-based Normalized Discounted Cumulative Gain (NDCG) */
    private double ndcg;
    private double recall;
    /** Average Precision */
    private double avgPrecision;

    /**
     * compute all the evaluations
     * 
     * @param ttMatrix  test data
     */
    public void evalRating(Model model, AbstractIterator idata) {
        // Rating Prediction evaluation
        mae = 0.0d;
        mse = 0.0d;

        idata = idata.clone();
        int nnz = 0;
        while (idata.hasNext()) {

            DataElem e = idata.next();
            short num_ifactor = e.getNum_ifacotr();

            int u = e.getIndex_user(0);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                double realVal = e.getValue_ifactor(f);
                double predVal = model.predict(u, i);

                mae += Math.abs(realVal - predVal);
                mse += Math.pow(realVal - predVal, 2.0d);
                nnz++;
            }

        }
        mae /= nnz;
        mse /= nnz;

    }

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
