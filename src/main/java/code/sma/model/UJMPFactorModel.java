package code.sma.model;

import code.sma.core.DataElem;
import code.sma.core.impl.UJMPDenseMatrix;
import code.sma.main.Configures;

/**
 * 
 * @author Chao.Chen
 * @version $Id: UJMPFactorModel.java, v 0.1 2017年6月19日 下午5:21:31 Chao.Chen Exp $
 */
public class UJMPFactorModel extends AbstractModel {
    private static final long serialVersionUID = 1L;

    /** User profile in low-rank matrix form. */
    public UJMPDenseMatrix    ufactors;
    /** Item profile in low-rank matrix form. */
    public UJMPDenseMatrix    ifactors;

    public UJMPFactorModel(Configures conf) {
        super(conf);

        int userCount = conf.getInteger("USER_COUNT_VALUE");
        int itemCount = conf.getInteger("ITEM_COUNT_VALUE");
        int featureCount = conf.getInteger("FEATURE_COUNT_VALUE");

        this.ufactors = new UJMPDenseMatrix(userCount, featureCount);
        this.ifactors = new UJMPDenseMatrix(featureCount, itemCount);
    }

    /** 
     * @see code.sma.model.Model#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        double prediction = ufactors.getRow(u).innerProduct(ifactors.getCol(i));
        return Math.max(minValue, Math.min(prediction, maxValue));
    }

    /** 
     * @see code.sma.model.Model#predict(code.sma.core.DataElem)
     */
    @Override
    public double predict(DataElem e) {
        throw new RuntimeException("This method has not been implemented in UJMPFactorModel!");
    }

}
