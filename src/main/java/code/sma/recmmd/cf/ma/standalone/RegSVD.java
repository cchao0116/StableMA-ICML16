package code.sma.recmmd.cf.ma.standalone;

import java.util.Map;

import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.model.FactorModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.cf.ma.stats.StatsOperator;


/**
 * This is a class implementing Regularized SVD (Singular Value Decomposition).
 * Technical detail of the algorithm can be found in
 * Arkadiusz Paterek, Improving Regularized Singular Value Decomposition Collaborative Filtering,
 * Proceedings of KDD Cup and Workshop, 2007.
 * 
 * @author Chao Chen
 * @version $Id: RegSVD.java, v 0.1 Jan 28, 2016 1:05:24 PM Exp $
 */
public class RegSVD extends FactorRecmmder {

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
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#update_each(code.sma.core.DataElem)
     */
    @Override
    protected void update_each(DataElem e) {
        FactorModel factModel = (FactorModel) model;
        double lr = runtimes.learningRate;
        short num_ifactor = e.getNum_ifacotr();

        int u = e.getIndex_user(0);
        for (int f = 0; f < num_ifactor; f++) {
            int i = e.getIndex_item(f);

            DenseVector ref_ufactor = StatsOperator.getVectorRef(factModel.ufactors, u);
            DenseVector ref_ifactor = StatsOperator.getVectorRef(factModel.ifactors, i);

            double AuiReal = e.getValue_ifactor(f);
            double AuiEst = model.predict(u, i);
            runtimes.sumErr += runtimes.lossFunction.calcLoss(AuiReal, AuiEst);

            double deriWRTp = runtimes.lossFunction.calcGrad(AuiReal, AuiEst);
            for (int s = 0; s < runtimes.featureCount; s++) {
                double Fus = ref_ufactor.floatValue(s);
                double Gis = ref_ifactor.floatValue(s);

                // perform gradient descent
                double newFus = Fus + lr * (-deriWRTp * Gis - runtimes.regType.calcReg(Fus));
                double newGis = Gis + lr * (-deriWRTp * Fus - runtimes.regType.calcReg(Gis));

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
        return String.format("RSVD%s", runtimes.briefDesc());
    }

}
