package code.sma.eval;

import code.sma.core.AbstractIterator;
import code.sma.core.DataElem;
import code.sma.model.Model;

/**
 * Metrics for Collaborative filtering method
 * 
 * @author Chao.Chen
 * @version $Id: CollaFiltrMetrics.java, v 0.1 Jul 5, 2017 10:13:43 AM$
 */
public class CollaFiltrMetrics extends EvaluationMetrics {

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
            nnz += num_ifactor;

            double[] preds = model.predict(e);
            for (int f = 0; f < num_ifactor; f++) {
                double realVal = e.getValue_ifactor(f);
                double predVal = preds[f];

                mae += Math.abs(realVal - predVal);
                mse += Math.pow(realVal - predVal, 2.0d);
            }

        }
        mae /= nnz;
        mse /= nnz;
    }

}
