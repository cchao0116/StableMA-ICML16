package code.sma.recmmd.cf.ma.standalone;

import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.model.SVDPPModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Regularizer.RegEnum;
import code.sma.recmmd.cf.ma.stats.StatsOperator;

/**
 * 
 * @author Chao Chen
 * @version $Id: SVDPP.java, v 1.0 Dec 7, 2017 1:13:05 PM$
 */
public class SVDPP extends FactorRecmmder {

    /*========================================
     * Constructors
     *========================================*/
    public SVDPP(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);
    }

    public SVDPP(Configures conf, boolean[] acc_ufi, boolean[] acc_ifi,
                 Map<String, Plugin> plugins) {
        super(conf, acc_ufi, acc_ifi, plugins);
    }

    /*========================================
     * Model Builder
     *========================================*/
    /** 
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
        assert train != null : "Training data cannot be null.";

        boolean[] acc_ufi = runtimes.acc_uf_indicator;
        boolean[] acc_ifi = runtimes.acc_if_indicator;

        runtimes.itrain = (acc_ufi == null && acc_ifi == null) ? (AbstractIterator) train.iterator()
            : (AbstractIterator) train.iteratorJion(acc_ufi, acc_ifi);
        runtimes.itest = (test == null) ? null
            : ((acc_ufi == null && acc_ifi == null) ? (AbstractIterator) test.iterator()
                : (AbstractIterator) test.iteratorJion(acc_ufi, acc_ifi));
        runtimes.nnz = runtimes.itrain.get_num_ifactor();

        if (model == null) {
            model = new SVDPPModel(runtimes.conf);
        }
    }

    /** 
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#update_each(code.sma.core.DataElem)
     */
    @Override
    protected void update_each(DataElem e) {
        SVDPPModel factModel = (SVDPPModel) model;
        double lr = runtimes.learningRate;
        short num_ifactor = e.getNum_ifacotr();

        double scale_yfactors = 1.0d / Math.sqrt(num_ifactor);
        DenseVector[] ref_yfactors = new DenseVector[num_ifactor];
        for (int f = 0; f < num_ifactor; f++) {
            int i = e.getIndex_item(f);
            ref_yfactors[f] = StatsOperator.getVectorRef(factModel.yfactors, i);
        }
        DenseVector ref_yfactor = factModel.calcImplicFeature(ref_yfactors);
        double[] deriv_yus = new double[runtimes.featureCount];

        int u = e.getIndex_user(0);
        for (int f = 0; f < num_ifactor; f++) {
            int i = e.getIndex_item(f);

            DenseVector ref_ufactor = StatsOperator.getVectorRef(factModel.ufactors, u);
            DenseVector ref_ifactor = StatsOperator.getVectorRef(factModel.ifactors, i);

            double bu = factModel.ubias.floatValue(u);
            double bi = factModel.ibias.floatValue(i);

            double AuiReal = e.getValue_ifactor(f);
            double AuiEst = factModel.base + bu + bi + ref_ufactor.innerProduct(ref_ifactor);
            runtimes.sumErr += runtimes.lossFunction.calcLoss(AuiReal, AuiEst);

            double deriWRTp = runtimes.lossFunction.calcGrad(AuiReal, AuiEst);

            // update latent factors
            for (int s = 0; s < runtimes.featureCount; s++) {
                double Fus = ref_ufactor.floatValue(s);
                double Yus = ref_yfactor.floatValue(s);
                double Gis = ref_ifactor.floatValue(s);

                // update explicit factors
                {
                    double newFus = Fus + lr * (-deriWRTp * Gis - runtimes.regType.calcReg(Fus));
                    double newGis = Gis
                                    + lr
                                      * (-deriWRTp * (Fus + Yus) - runtimes.regType.calcReg(Gis));
                    ref_ufactor.setValue(s, runtimes.regType.afterReg(newFus));
                    ref_ifactor.setValue(s, runtimes.regType.afterReg(newGis));
                }

                // update implicit factors
                deriv_yus[s] = deriWRTp * Gis * scale_yfactors;
                for (int y = 0; y < num_ifactor; y++) {
                    double yus = ref_yfactors[y].floatValue(s);
                    double newyus = yus + lr * (-deriv_yus[s] - runtimes.regType.calcReg(yus));
                    ref_yfactors[y].setValue(s, runtimes.regType.afterReg(newyus));
                }
            }

            // update biases
            double newbu = bu + lr * (-deriWRTp - runtimes.regType.calcReg(bu));
            double newbi = bi + lr * (-deriWRTp - runtimes.regType.calcReg(bi));
            factModel.ubias.setValue(u, runtimes.regType.afterReg(newbu));
            factModel.ibias.setValue(i, runtimes.regType.afterReg(newbi));

            // tricks for implicit factors
            if (runtimes.regType.regEnum == RegEnum.L2) {
                for (int s = 0; s < runtimes.featureCount; s++) {
                    double yus = ref_yfactor.floatValue(s);
                    double newyus = yus + lr * (-deriv_yus[s] / scale_yfactors - runtimes.regType.calcReg(yus));
                    ref_yfactor.setValue(s, newyus);
                }
            } else {
                ref_yfactor = factModel.calcImplicFeature(ref_yfactors);
            }
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("SVDPP%s", runtimes.briefDesc());
    }

}
