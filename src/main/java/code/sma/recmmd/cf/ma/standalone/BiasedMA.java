package code.sma.recmmd.cf.ma.standalone;

import java.util.Map;

import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.model.FactorModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.cf.ma.stats.StatsOperator;

/**
 * Advanced RegSVD
 * 
 * @author Chao Chen
 * @version $Id: BiasedMA.java, Dec 6, 2017 6:14:46 PM$
 */
public class BiasedMA extends FactorRecmmder {

    /*========================================
     * Constructors
     *========================================*/
    public BiasedMA(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);
    }

    public BiasedMA(Configures conf, boolean[] acc_ufi, boolean[] acc_ifi,
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
            double bu = factModel.ubias.floatValue(u);
            double bi = factModel.ibias.floatValue(i);

            double AuiReal = e.getValue_ifactor(f);
            double AuiEst = factModel.base + factModel.ubias.floatValue(u) + bu + bi
                            + ref_ufactor.innerProduct(ref_ifactor);
            runtimes.sumErr += runtimes.lossFunction.calcLoss(AuiReal, AuiEst);

            double deriWRTp = runtimes.lossFunction.calcGrad(AuiReal, AuiEst);

            // update latent factors
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

            // update biases
            factModel.ubias.setValue(u, bu + lr * (-deriWRTp - runtimes.regType.calcReg(bu)));
            factModel.ibias.setValue(i, bi + lr * (-deriWRTp - runtimes.regType.calcReg(bi)));
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("BiasedMA%s", runtimes.briefDesc());
    }

}
