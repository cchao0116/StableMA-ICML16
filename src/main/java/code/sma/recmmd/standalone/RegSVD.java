package code.sma.recmmd.standalone;

import java.util.Map;

import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.plugin.Plugin;
import code.sma.recmmd.stats.StatsOperator;

/**
 * This is a class implementing Regularized SVD (Singular Value Decomposition).
 * Technical detail of the algorithm can be found in
 * Arkadiusz Paterek, Improving Regularized Singular Value Decomposition Collaborative Filtering,
 * Proceedings of KDD Cup and Workshop, 2007.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public class RegSVD extends MFRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /*========================================
     * Constructors
     *========================================*/
    public RegSVD(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);
    }

    public RegSVD(Configures conf, boolean[] acc_ufi, boolean[] acc_ifi,
                  Map<String, Plugin> plugins) {
        super(conf, acc_ufi, acc_ifi, plugins);
    }

    /*========================================
     * Model Builder
     *========================================*/
    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#update_each(code.sma.core.DataElem)
     */
    @Override
    protected void update_each(DataElem e) {
        double lr = runtimes.learningRate;
        double reg = runtimes.regularizer;
        short num_ifactor = e.getNum_ifacotr();

        int u = e.getIndex_user(0);
        for (int f = 0; f < num_ifactor; f++) {
            int i = e.getIndex_item(f);

            DenseVector ref_ufactor = StatsOperator.getVectorRef(userDenseFeatures, u);
            DenseVector ref_ifactor = StatsOperator.getVectorRef(itemDenseFeatures, i);

            double AuiReal = e.getValue_ifactor(f);
            double AuiEst = ref_ufactor.innerProduct(ref_ifactor);
            runtimes.sumErr += runtimes.lossFunction.diff(AuiReal, AuiEst);

            double deriWRTp = runtimes.lossFunction.dervWRTPrdctn(AuiReal, AuiEst);
            for (int s = 0; s < runtimes.featureCount; s++) {
                double Fus = ref_ufactor.floatValue(s);
                double Gis = ref_ifactor.floatValue(s);

                //global model updates
                ref_ufactor.setValue(s,
                    Fus + lr * (-deriWRTp * Gis - reg * runtimes.regType.reg(null, u, Fus)));
                ref_ifactor.setValue(s,
                    Gis + lr * (-deriWRTp * Fus - reg * runtimes.regType.reg(null, i, Gis)));
            }
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("RSVD%s", runtimes.briefDesc());
    }

}
