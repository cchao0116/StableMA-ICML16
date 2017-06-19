package code.sma.model;

import code.sma.core.AbstractVector;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseMatrix;
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

    public FactorModel(Configures conf) {
        super(conf);

        int userCount = conf.getInteger("USER_COUNT_VALUE");
        int itemCount = conf.getInteger("ITEM_COUNT_VALUE");
        int featureCount = conf.getInteger("FEATURE_COUNT_VALUE");

        this.ufactors = new DenseMatrix(userCount, featureCount);
        this.ifactors = new DenseMatrix(itemCount, featureCount);
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
            return ufs.innerProduct(ifs);
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
