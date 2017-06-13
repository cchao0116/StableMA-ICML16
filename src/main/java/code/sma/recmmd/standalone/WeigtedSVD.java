package code.sma.recmmd.standalone;

import java.util.Map;

import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.plugin.Discretizer;
import code.sma.plugin.Plugin;
import code.sma.recmmd.stats.StatsOperator;

/**
 * This is a class implementing WSVD (Weighted Matrix Approximation).
 * Technical detail of the algorithm can be found in
 * Chao Chen, WEMAREC: Accurate and Scalable Recommendation through Weighted and Ensemble Matrix Approximation,
 * Proceedings of SIGIR, 2015.
 * 
 * @author Chao Chen
 * @version $Id: WeigtedRSVD.java, v 0.1 2014-10-19 上午11:20:27 chench Exp $
 */
public class WeigtedSVD extends MFRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /*========================================
     * Constructors
     *========================================*/
    public WeigtedSVD(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);

        runtimes.doubles.add(conf.getDouble("BETA0_VALUE"));
    }

    /*========================================
     * Model Builder
     *========================================*/
    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
        super.prepare_runtimes(train, test);

        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
        runtimes.tnWs = dctzr.cmpTrainWs(runtimes.itrain);
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#update_each(code.sma.core.DataElem)
     */
    @Override
    protected void update_each(DataElem e) {
        double learningRate = runtimes.learningRate;
        double regularizer = runtimes.regularizer;
        short num_ifactor = e.getNum_ifacotr();

        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
        double beta0 = runtimes.doubles.getDouble(0);

        int u = e.getIndex_user(0);
        for (int f = 0; f < num_ifactor; f++) {
            int i = e.getIndex_item(f);

            DenseVector ref_ufactor = StatsOperator.getVectorRef(userDenseFeatures, u);
            DenseVector ref_ifactor = StatsOperator.getVectorRef(itemDenseFeatures, i);

            double AuiReal = e.getValue_ifactor(f);
            double AuiEst = ref_ufactor.innerProduct(ref_ifactor);
            runtimes.sumErr += runtimes.lossFunction.diff(AuiReal, AuiEst);

            double deriWRTp = runtimes.lossFunction.dervWRTPrdctn(AuiReal, AuiEst);
            double tnW = 1 + beta0 * runtimes.tnWs[dctzr.convert(AuiReal)];
            for (int s = 0; s < runtimes.featureCount; s++) {
                double Fus = ref_ufactor.floatValue(s);
                double Gis = ref_ifactor.floatValue(s);

                //global model updates
                ref_ufactor.setValue(s,
                    Fus + learningRate * (-deriWRTp * Gis * tnW - regularizer * Fus));
                ref_ifactor.setValue(s,
                    Gis + learningRate * (-deriWRTp * Fus * tnW - regularizer * Gis));
            }
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("WSMA%s_Tn[%d]", runtimes.briefDesc(),
            (int) (runtimes.doubles.getDouble(0) * 100));
    }

}
