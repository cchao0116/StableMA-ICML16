package code.sma.recmmd.cf.ma.standalone;

import java.util.Map;

import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.model.FactorModel;
import code.sma.plugin.Discretizer;
import code.sma.plugin.Plugin;
import code.sma.recmmd.cf.ma.stats.StatsOperator;

/**
 * This is a class implementing WSVD (Weighted Matrix Approximation).
 * Technical detail of the algorithm can be found in
 * Chao Chen, WEMAREC: Accurate and Scalable Recommendation through Weighted and Ensemble Matrix Approximation,
 * Proceedings of SIGIR, 2015.
 * 
 * @author Chao Chen
 * @version $Id: WeigtedRSVD.java, v 0.1 2014-10-19 上午11:20:27 chench Exp $
 */
public class WeigtedSVD extends FactorRecmmder {

    public WeigtedSVD(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);

        runtimes.doubles.add(conf.getDouble("BETA0_VALUE"));
    }

    /*========================================
     * Model Builder
     *========================================*/
    /** 
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
        super.prepare_runtimes(train, test);

        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
        runtimes.tnWs = dctzr.cmpTrainWs(runtimes.itrain);
    }

    /** 
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#update_each(code.sma.core.DataElem)
     */
    @Override
    protected void update_each(DataElem e) {
        FactorModel factModel = (FactorModel) model;
        double lr = runtimes.learningRate;
        short num_ifactor = e.getNum_ifacotr();

        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
        double beta0 = runtimes.doubles.getDouble(0);

        int u = e.getIndex_user(0);
        for (int f = 0; f < num_ifactor; f++) {
            int i = e.getIndex_item(f);

            DenseVector ref_ufactor = StatsOperator.getVectorRef(factModel.ufactors, u);
            DenseVector ref_ifactor = StatsOperator.getVectorRef(factModel.ifactors, i);

            double AuiReal = e.getValue_ifactor(f);
            double AuiEst = ref_ufactor.innerProduct(ref_ifactor);
            runtimes.sumErr += runtimes.lossFunction.calcLoss(AuiReal, AuiEst);

            double deriWRTp = runtimes.lossFunction.calcGrad(AuiReal, AuiEst);
            double tnW = 1 + beta0 * runtimes.tnWs[dctzr.convert(AuiReal)];
            for (int s = 0; s < runtimes.featureCount; s++) {
                double Fus = ref_ufactor.floatValue(s);
                double Gis = ref_ifactor.floatValue(s);

                // perform gradient descent
                double newFus = Fus + lr * (-deriWRTp * Gis * tnW - runtimes.regType.calcReg(Fus));
                double newGis = Gis + lr * (-deriWRTp * Fus * tnW - runtimes.regType.calcReg(Gis));

                // perform regularization
                ref_ufactor.setValue(s, runtimes.regType.afterReg(newFus));
                ref_ifactor.setValue(s, runtimes.regType.afterReg(newGis));
            }
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("WSMA%s_Tn[%d]", runtimes.briefDesc(),
            Math.round(runtimes.doubles.getDouble(0) * 100));
    }

}
