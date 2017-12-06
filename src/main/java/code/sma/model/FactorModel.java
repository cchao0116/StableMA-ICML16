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
 * @version $Id: FactorModel.java, v 0.1 2017年6月19日 下午2:47:58 Chao.Chen Exp $
 */
public class FactorModel extends AbstractModel {
    private static final long serialVersionUID = 1L;

    /** User profile in low-rank matrix form. */
    public DenseMatrix        ufactors;
    /** Item profile in low-rank matrix form. */
    public DenseMatrix        ifactors;
    /** User-dependent bias*/
    public DenseVector        ubias;
    /** Item-dependent bias*/
    public DenseVector        ibias;
    /** the base and stationary prediction*/
    public float              base;

    public FactorModel(Configures conf) {
        super(conf);

        int userCount = conf.getInteger("USER_COUNT_VALUE");
        int itemCount = conf.getInteger("ITEM_COUNT_VALUE");
        int featureCount = conf.getInteger("FEATURE_COUNT_VALUE");

        this.ufactors = new DenseMatrix(userCount, featureCount);
        this.ifactors = new DenseMatrix(itemCount, featureCount);
        this.ubias = new DenseVector(userCount);
        this.ibias = new DenseVector(itemCount);
        this.base = conf.containsKey("BASE_PREDICTION_VALUE")
            ? conf.getFloat("BASE_PREDICTION_VALUE")
            : 0.0f;
    }

    /** 
     * @see code.sma.model.AbstractModel#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        assert (ufactors != null && ifactors != null) : "Feature matrix cannot be null";
        AbstractVector ufs = ufactors.getRowRef(u);
        AbstractVector ifs = ifactors.getRowRef(i);

        if (ufs == null || ifs == null) {
            LoggerUtil.debug(runningLogger,
                String.format("null latent factors for (%d,%d)-entry", u, i));
            return defaultValue;
        } else {
            double prediction = base + ubias.floatValue(u) + ibias.floatValue(i)
                                + ufs.innerProduct(ifs);
            return Math.max(minValue, Math.min(prediction, maxValue));
        }

    }

    /** 
     * @see code.sma.model.AbstractModel#predict(code.sma.core.DataElem)
     */
    @Override
    public double predict(DataElem e) {
        throw new RuntimeException("This method has not been implemented in FactorModel!");
    }
}
