package code.sma.recmmd.standalone;

import java.util.ArrayList;
import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.AbstractVector;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.plugin.Discretizer;
import code.sma.plugin.NetflixMovieLensDiscretizer;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Loss;
import code.sma.recmmd.Regularizer;
import code.sma.recmmd.stats.Accumulator;
import code.sma.recmmd.stats.StatsOperator;
import code.sma.util.EvaluationMetrics;
import code.sma.util.LoggerUtil;

/**
 * GLOMA: Embedding Global Information in Local Matrix Approximation Models for Collaborative Filtering 
 * Chao Chen, Dongsheng Li, Qin Lv, Junchi Yan, Li Shang, Stephen M. Chu 
 * In AAAI Conference on Artificial Intelligence (AAAI), 2017
 *
 * @author Chao.Chen
 * @version $Id: GLOMA.java, v 0.1 2017年2月28日 下午1:46:27 Chao.Chen Exp $
 */
public class GLOMA extends MFRecommender {
    /** SerialVersionNum */
    private static final long       serialVersionUID = 1L;
    /** Contribution of each component, i.e., LuLi, LuGi, GuLi */
    private double[]                lambda;
    /** Previously-trained model*/
    private transient MFRecommender auxRec;

    /*========================================
     * Constructors
     *========================================*/
    public GLOMA(Configures conf, boolean[] acc_ufi, boolean[] acc_ifi, Map<String, Plugin> plugins,
                 MFRecommender auxRec) {
        super(conf, acc_ufi, acc_ifi, plugins);
        this.auxRec = auxRec;

        runtimes.plugins.put("DISCRETIZER", new NetflixMovieLensDiscretizer(conf));
        lambda = conf.getDoubleArr("LAMBDA");
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
        super.prepare_runtimes(train, test);

        int nnz = runtimes.nnz;
        int userCount = runtimes.userCount;
        int itemCount = runtimes.itemCount;
        int featureCount = runtimes.featureCount;

        runtimes.acumltors = new ArrayList<Accumulator>();
        runtimes.acumltors.add(new Accumulator(1, nnz)); // accumulator to record difference between real and predicted value
        runtimes.acumltors.add(new Accumulator(1, train.num_ifactor));
        runtimes.acumltors.add(new Accumulator(1, train.num_ifactor)); // accumulator to record difference between real and predicted value
        runtimes.acumltors.add(new Accumulator(userCount, featureCount)); // accumulator to record value of user latent factors
        runtimes.acumltors.add(new Accumulator(itemCount, featureCount)); // accumulator to record value of item latent factors

        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
        runtimes.tnWs = dctzr.cmpTrainWs(runtimes.itrain);
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#update_inner(code.sma.core.AbstractIterator)
     */
    @Override
    protected void update_inner(AbstractIterator iDataElem) {

        // collaboratively train
        collab_update(iDataElem);

        // update runtime environment
        update_runtimes();

        // biased fit in final turn
        if (runtimes.round == runtimes.maxIter) {
            biased_fit(iDataElem);

            // update runtime environment
            update_runtimes();
        }
    }

    protected void collab_update(AbstractIterator iDataElem) {
        boolean[] acc_ufi = runtimes.acc_uf_indicator;
        boolean[] acc_ifi = runtimes.acc_if_indicator;

        int featureCount = runtimes.featureCount;
        double learningRate = runtimes.learningRate;
        double regularizer = runtimes.regularizer;

        Accumulator acum_diff_LuLi = runtimes.acumltors.get(0);
        Accumulator acum_diff_LuGi = runtimes.acumltors.get(1);
        Accumulator acum_diff_GuLi = runtimes.acumltors.get(2);
        Accumulator acum_ufactor = runtimes.acumltors.get(3);
        Accumulator acum_ifactor = runtimes.acumltors.get(4);

        Regularizer regType = runtimes.regType;
        Loss lossFunction = runtimes.lossFunction;
        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");

        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();
            int u = e.getIndex_user(0);

            DenseVector lref_ufactor = StatsOperator.getVectorRef(userDenseFeatures, u,
                acum_ufactor);
            DenseVector gref_ufactor = auxRec.userDenseFeatures.getRowRef(u);
            for (int f = 0; f < num_ifactor; f++) {

                int i = e.getIndex_item(f);
                DenseVector lref_ifactor = StatsOperator.getVectorRef(itemDenseFeatures, i,
                    acum_ifactor);
                DenseVector gref_ifactor = auxRec.itemDenseFeatures.getRowRef(i);

                double AuiReal = e.getValue_ifactor(f);

                double LuLi = 0.0d;
                if (acc_ufi[u] && acc_ifi[i])
                    LuLi = StatsOperator.innerProduct(lref_ufactor, lref_ifactor, lossFunction,
                        AuiReal, acum_diff_LuLi);

                double LuGi = 0.0d;
                if (acc_ufi[u])
                    LuGi = StatsOperator.innerProduct(lref_ufactor, gref_ifactor, lossFunction,
                        AuiReal, acum_diff_LuGi);

                double GuLi = 0.0d;
                if (acc_ifi[i])
                    GuLi = StatsOperator.innerProduct(gref_ufactor, lref_ifactor, lossFunction,
                        AuiReal, acum_diff_GuLi);

                double RMELuLi = acum_diff_LuLi.rm();
                double RMELuGi = acum_diff_LuGi.rm();
                double RMEGuli = acum_diff_GuLi.rm();

                double deriWRTpLuLi = lossFunction.dervWRTPrdctn(AuiReal, LuLi) / RMELuLi;
                double deriWRTpLuGi = lossFunction.dervWRTPrdctn(AuiReal, LuGi) / RMELuGi;
                double deriWRTpGuLi = lossFunction.dervWRTPrdctn(AuiReal, GuLi) / RMEGuli;

                double tnW = 1 + 0.4 * runtimes.tnWs[dctzr.convert(AuiReal)];
                for (int s = 0; s < featureCount; s++) {
                    double Fus = lref_ufactor.floatValue(s);
                    double fus = gref_ufactor.floatValue(s);
                    double Gis = lref_ifactor.floatValue(s);
                    double gis = gref_ifactor.floatValue(s);

                    double update_uf = acc_ifi[i]
                        ? (-deriWRTpLuLi * Gis * lambda[0] * tnW
                           - deriWRTpLuGi * gis * lambda[1] * tnW
                           - regularizer * regType.reg(acum_ufactor, u, Fus))
                        : (-deriWRTpLuGi * gis * 3.0
                           - regularizer * Regularizer.L2.reg(acum_ufactor, u, Fus));
                    StatsOperator.updateVector(lref_ufactor, s, learningRate * update_uf,
                        acum_ufactor, u, s);

                    double update_if = acc_ufi[u]
                        ? (-deriWRTpLuLi * Fus * lambda[0] * tnW
                           - deriWRTpGuLi * fus * lambda[2] * tnW
                           - regularizer * regType.reg(acum_ifactor, i, Gis))
                        : (-deriWRTpGuLi * fus * 0.8
                           - regularizer * Regularizer.L2.reg(acum_ifactor, i, Gis));
                    StatsOperator.updateVector(lref_ifactor, s, learningRate * update_if,
                        acum_ifactor, i, s);
                }
            }
        }
    }

    protected void biased_fit(AbstractIterator iDataElem) {
        boolean[] acc_ufi = runtimes.acc_uf_indicator;
        boolean[] acc_ifi = runtimes.acc_if_indicator;

        int featureCount = runtimes.featureCount;
        double learningRate = runtimes.learningRate;

        Accumulator acum_diff = runtimes.acumltors.get(0);
        Accumulator acum_ufactor = runtimes.acumltors.get(3);
        Accumulator acum_ifactor = runtimes.acumltors.get(4);

        Loss lossFunction = runtimes.lossFunction;
        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");

        int id_diff = -1;
        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();
            int u = e.getIndex_user(0);

            DenseVector lref_ufactor = userDenseFeatures.getRowRef(u);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);
                id_diff++;

                if (!acc_ufi[u] || !acc_ifi[i]) {
                    continue;
                }

                DenseVector lref_ifactor = itemDenseFeatures.getRowRef(i);

                double AuiReal = e.getValue_ifactor(f);
                double LuLi = (acc_ufi[u] && acc_ifi[i]) ? StatsOperator.innerProduct(lref_ufactor,
                    lref_ifactor, lossFunction, AuiReal, acum_diff, 0, id_diff) : 0.0d;

                double RMELuLi = acum_diff.rm(0);
                double deriWRTpLuLi = lossFunction.dervWRTPrdctn(AuiReal, LuLi) / RMELuLi;
                double tnW = 1 + 0.4 * runtimes.tnWs[dctzr.convert(AuiReal)];
                for (int s = 0; s < featureCount; s++) {
                    double Fus = lref_ufactor.floatValue(s);
                    double Gis = lref_ifactor.floatValue(s);

                    double update_uf = (-deriWRTpLuLi * Gis * tnW
                                        - 0.01 * Regularizer.L2.reg(acum_ufactor, u, Fus));
                    StatsOperator.updateVector(lref_ufactor, s, learningRate * update_uf,
                        acum_ufactor, u, s);

                    double update_if = (-deriWRTpLuLi * Fus * tnW
                                        - 0.01 * Regularizer.L2.reg(acum_ifactor, i, Gis));
                    StatsOperator.updateVector(lref_ifactor, s, learningRate * update_if,
                        acum_ifactor, i, s);
                }
            }
        }
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#update_runtimes()
     */
    @Override
    protected void update_runtimes() {
        runtimes.prevErr = runtimes.currErr;
        runtimes.currErr = runtimes.acumltors.get(0).rm();
        runtimes.trainErr.add(runtimes.currErr);

        runtimes.round++;
        runtimes.sumErr = 0.0;

        if (runtimes.showProgress && (runtimes.round % 5 == 0) && runtimes.itest != null) {
            EvaluationMetrics metric = new EvaluationMetrics(this);
            LoggerUtil.info(runningLogger, String.format("%d\t%.6f [%s]", runtimes.round,
                runtimes.currErr, metric.printOneLine()));
            runtimes.testErr.add(metric.getRMSE());
        } else {
            LoggerUtil.info(runningLogger,
                String.format("%d\t%.6f,%.6f,%.6f", runtimes.round, runtimes.acumltors.get(0).rm(),
                    runtimes.acumltors.get(1).rm(), runtimes.acumltors.get(2).rm()));
        }
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        assert (userDenseFeatures != null
                && itemDenseFeatures != null) : "Feature matrix cannot be null";

        double maxValue = runtimes.maxValue;
        double minValue = runtimes.minValue;
        double prediction = 0.0;

        AbstractVector ufactors = userDenseFeatures.getRowRef(u);
        AbstractVector ifactors = itemDenseFeatures.getRowRef(i);
        if (ufactors == null || ifactors == null) {
            prediction += (maxValue + minValue) / 2;
            LoggerUtil.debug(runningLogger,
                String.format("null latent factors for (%d,%d)-entry", u, i));
        } else {
            double LuLi = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, false);
            double LuGi = userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures, false);
            double GuLi = auxRec.userDenseFeatures.innerProduct(u, i, itemDenseFeatures, false);
            double GuGi = auxRec.userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures,
                false);
            prediction += (LuLi + LuGi + GuLi + GuGi) / 4.0d;
        }

        return Math.max(minValue, Math.min(prediction, maxValue));
    }
}
