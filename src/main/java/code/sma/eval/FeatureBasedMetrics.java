package code.sma.eval;

import code.sma.core.AbstractIterator;
import code.sma.core.DataElem;
import code.sma.model.Model;

/**
 * Metrics for feature-based method
 * 
 * @author Chao.Chen
 * @version $Id: FeatureBasedMetrics.java, v 0.1 Jul 5, 2017 10:28:39 AM$
 */
public class FeatureBasedMetrics extends EvaluationMetrics {

    /** 
     * @see code.sma.eval.EvaluationMetrics#evalRating(code.sma.model.Model, code.sma.core.AbstractIterator)
     */
    @Override
    public void evalRating(Model model, AbstractIterator idata) {
        // Rating Prediction evaluation
        mae = 0.0d;
        mse = 0.0d;

        idata = idata.clone();
        int nnz = 0;
        while (idata.hasNext()) {

            DataElem e = idata.next();

            double realVal = e.getLabel();
            double predVal = model.predict(e);

            mae += Math.abs(realVal - predVal);
            mse += Math.pow(realVal - predVal, 2.0d);
            nnz++;
        }
        mae /= nnz;
        mse /= nnz;
    }

}
