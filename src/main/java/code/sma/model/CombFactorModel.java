package code.sma.model;

import code.sma.core.AbstractVector;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseMatrix;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.util.LoggerUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: CombFactorModel.java, v 0.1 2017年6月19日 下午4:19:06 Chao.Chen Exp $
 */
public class CombFactorModel extends FactorModel {
    private static final long serialVersionUID = 1L;
    /** Introduced user profile. */
    public DenseMatrix        g_ufactors;
    /** Introduced item profile */
    public DenseMatrix        g_ifactors;

    public CombFactorModel(Configures conf, FactorModel m) {
        super(conf);
        g_ufactors = m.ufactors;
        g_ifactors = m.ifactors;
    }

    private double predict(int u, int i) {
        assert (ufactors != null && ifactors != null) : "Feature matrix cannot be null";

        //        double maxValue = runtimes.maxValue;
        //        double minValue = runtimes.minValue;
        double prediction = 0.0;

        AbstractVector ufs = ufactors.getRowRef(u);
        AbstractVector ifs = ifactors.getRowRef(i);

        if (ufs == null || ifs == null) {
            LoggerUtil.debug(runningLogger,
                String.format("null latent factors for (%d,%d)-entry", u, i));
            return defaultValue;
        } else {
            DenseVector gref_ufactor = g_ufactors.getRowRef(u);
            DenseVector gref_ifactor = g_ifactors.getRowRef(i);

            double LuLi = ufs.innerProduct(ifs);
            double LuGi = ufs.innerProduct(gref_ifactor);
            double GuLi = gref_ufactor.innerProduct(ifs);
            double GuGi = gref_ufactor.innerProduct(gref_ifactor);
            prediction += (LuLi + LuGi + GuLi + GuGi) / 4.0d;
        }

        return Math.max(minValue, Math.min(prediction, maxValue));
    }

    /** 
     * @see code.sma.model.AbstractModel#predict(code.sma.core.DataElem)
     */
    @Override
    public double[] predict(DataElem e) {
        assert (ufactors != null && ifactors != null) : "Feature matrix cannot be null";

        short num_ifactor = e.getNum_ifacotr();
        double[] preds = new double[num_ifactor];

        int u = e.getIndex_user(0);
        for (int p = 0; p < num_ifactor; p++) {
            int i = e.getIndex_item(p);
            preds[p] = predict(u, i);
        }
        return preds;
    }

}
