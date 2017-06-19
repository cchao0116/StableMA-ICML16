package code.sma.model;

import code.sma.core.AbstractVector;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseMatrix;
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
    /** the default predicted value */
    protected double          defaultValue;

    public FactorModel(int num_user, int num_item, int num_feature, double defaultValue) {
        super();
        this.ufactors = new DenseMatrix(num_user, num_feature);
        this.ifactors = new DenseMatrix(num_item, num_feature);
        this.defaultValue = defaultValue;
    }

    /** 
     * @see code.sma.model.AbstractModel#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        AbstractVector ufs = ufactors.getRowRef(u);
        AbstractVector ifs = ifactors.getRowRef(i);

        if (ufactors == null || ifactors == null) {
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
