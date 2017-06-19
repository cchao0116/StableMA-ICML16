package code.sma.recmmd.standalone;

import java.util.ArrayList;
import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.model.AbstractModel;
import code.sma.model.FactorModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.stats.Accumulator;
import code.sma.recmmd.stats.StatsOperator;

/**
 * This is a class implementing SMA (Stable Matrix Approximation).
 * Technical detail of the algorithm can be found in
 * Dongsheng Li, Stable Matrix Approximation,
 * Proceedings of ICML, 2016.
 * 
 * @author Chao Chen
 * @version $Id: StableSVD.java, v 0.1 Dec 22, 2015 11:43:15 AM Exp $
 */
public class StableMA extends MFRecommender {
    /** the indicator of hard-predictive set, where true means in the set*/
    private boolean[][] hps_indicator;

    /*========================================
     * Constructors
     *========================================*/

    public StableMA(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);

        runtimes.ints.add(conf.getInteger("NUMBER_HARD_PREDICTION_SET_VALUE"));
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
        super.prepare_runtimes(train, test);

        int num_hps = runtimes.ints.getInt(0); // number of hard-predictive subsets
        int nnz = runtimes.nnz;
        hps_indicator = new boolean[num_hps][nnz];

        int[] num_en = new int[num_hps]; // number of chosen entries in each  hard-predictive subsets
        {
            AbstractModel auxRec = (AbstractModel) runtimes.plugins.remove("AUXILIARY_RCMMD_MODEL");
            double bestRMSE = auxRec.bestTrainErr();

            int id_en = 0;

            AbstractIterator iDataElem = runtimes.itrain.refresh();
            while (iDataElem.hasNext()) {
                DataElem e = iDataElem.next();
                short num_ifactor = e.getNum_ifacotr();

                int u = e.getIndex_user(0);
                for (int f = 0; f < num_ifactor; f++) {
                    int i = e.getIndex_item(f);

                    double AuiReal = e.getValue_ifactor(f);
                    double AuiEstm = auxRec.predict(u, i);

                    // choose hard-predictive subsets at random
                    double chosen_prob = Math.abs(AuiReal - AuiEstm) > bestRMSE ? 0.55 : 0.75;
                    for (int h = 0; h < num_hps; h++) {
                        if (Math.random() > chosen_prob) {
                            continue;
                        }

                        hps_indicator[h][id_en] = true;
                        num_en[h]++;
                    }

                    id_en++;
                }
            }
        }

        runtimes.acumltors = new ArrayList<Accumulator>();
        runtimes.acumltors.add(new Accumulator(1, nnz));
        for (int h = 0; h < num_hps; h++) {
            runtimes.acumltors.add(new Accumulator(1, num_en[h]));
        }
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#update_inner(code.sma.core.AbstractIterator)
     */
    @Override
    protected void update_inner(AbstractIterator iDataElem) {
        FactorModel factModel = (FactorModel) model;
        double lr = runtimes.learningRate;
        double reg = runtimes.regularizer;

        int num_hps = runtimes.ints.getInt(0);
        Accumulator[] acumltor = new Accumulator[num_hps + 1];
        acumltor[0] = runtimes.acumltors.get(0);

        int rid = 0;

        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();

            int u = e.getIndex_user(0);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                DenseVector ref_ufactor = StatsOperator.getVectorRef(factModel.ufactors, u);
                DenseVector ref_ifactor = StatsOperator.getVectorRef(factModel.ifactors, i);

                for (int h = 1; h <= num_hps; h++) {
                    acumltor[h] = hps_indicator[h - 1][rid] ? runtimes.acumltors.get(h) : null;
                }

                double AuiReal = e.getValue_ifactor(f);
                double AuiEst = StatsOperator.innerProduct(ref_ufactor, ref_ifactor,
                    runtimes.lossFunction, AuiReal, acumltor);
                runtimes.sumErr += runtimes.lossFunction.diff(AuiReal, AuiEst);

                // compute RMSEs
                double tnW = 1 / acumltor[0].rm();
                for (int h = 0; h <= num_hps; h++) {
                    if (acumltor[h] != null) {
                        tnW += 1 / (2 * num_hps * acumltor[h].rm());
                    }
                }

                // stochastic gradient descend
                double deriWRTp = runtimes.lossFunction.dervWRTPrdctn(AuiReal, AuiEst);
                for (int s = 0; s < runtimes.featureCount; s++) {
                    double Fus = ref_ufactor.floatValue(s);
                    double Gis = ref_ifactor.floatValue(s);

                    //global model updates
                    ref_ufactor.setValue(s,
                        Fus + lr
                              * (-deriWRTp * Gis * tnW - reg * runtimes.regType.reg(null, u, Fus)));
                    ref_ifactor.setValue(s,
                        Gis + lr
                              * (-deriWRTp * Fus * tnW - reg * runtimes.regType.reg(null, i, Gis)));
                }

                rid++;
            }
        }

        // update runtime environment
        update_runtimes();
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("SMA%s_[%d]", runtimes.briefDesc(), runtimes.ints.getInt(0));
    }

}
