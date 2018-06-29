package code.sma.recmmd.cf.ma.standalone;

import java.util.ArrayList;
import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseVector;
import code.sma.eval.CollaFiltrMetrics;
import code.sma.eval.EvaluationMetrics;
import code.sma.main.Configures;
import code.sma.model.CombFactorModel;
import code.sma.model.FactorModel;
import code.sma.plugin.Discretizer;
import code.sma.plugin.NetflixMovieLensDiscretizer;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Loss;
import code.sma.recmmd.Regularizer;
import code.sma.recmmd.RuntimeEnv;
import code.sma.recmmd.cf.ma.stats.Accumulator;
import code.sma.recmmd.cf.ma.stats.StatsOperator;
import code.sma.util.LoggerUtil;

/**
 * GLOMA: Embedding Global Information in Local Matrix Approximation Models for Collaborative Filtering 
 * Chao Chen, Dongsheng Li, Qin Lv, Junchi Yan, Li Shang, Stephen M. Chu 
 * In AAAI Conference on Artificial Intelligence (AAAI), 2017
 *
 * @author Chao.Chen
 * @version $Id: GLOMA.java, v 0.1 2017年2月28日 下午1:46:27 Chao.Chen Exp $
 */
public class GLOMA extends FactorRecmmder {
    private CombFactorModel fmRef;

    /** Contribution of each component, i.e., LuLi, LuGi, GuLi */
    private double[]        lambda;

    /** L2 regularizer*/
    private Regularizer     L2;

    /** Statistics for user factor values*/
    private Accumulator     acum_ufactor;
    private Accumulator     acum_ifactor;

    /*========================================
     * Constructors
     *========================================*/
    public GLOMA(Configures conf, boolean[] acc_ufi, boolean[] acc_ifi,
                 Map<String, Plugin> plugins) {
        runtimes = new RuntimeEnv(conf);
        runtimes.plugins = plugins;

        runtimes.acc_uf_indicator = acc_ufi;
        runtimes.acc_if_indicator = acc_ifi;

        runtimes.plugins.put("DISCRETIZER", new NetflixMovieLensDiscretizer(conf));
        lambda = conf.getDoubleArr("LAMBDA");

        L2 = Regularizer.L2(runtimes.regularizer);
    }

    /** 
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
        assert train != null : "Training data cannot be null.";

        boolean[] acc_ufi = runtimes.acc_uf_indicator;
        boolean[] acc_ifi = runtimes.acc_if_indicator;

        runtimes.itrain = (acc_ufi == null && acc_ifi == null) ? (AbstractIterator) train.iterator()
            : (AbstractIterator) train.iteratorUion(acc_ufi, acc_ifi);
        runtimes.itest = (test == null) ? null
            : ((acc_ufi == null && acc_ifi == null) ? (AbstractIterator) test.iterator()
                : (AbstractIterator) test.iteratorJion(acc_ufi, acc_ifi));
        runtimes.nnz = runtimes.itrain.get_num_global();

        runtimes.acumltors = new ArrayList<Accumulator>();
        runtimes.acumltors.add(new Accumulator(runtimes.itrain.get_num_global())); // accumulator to record difference between real and predicted value
        runtimes.acumltors.add(new Accumulator(runtimes.itrain.get_num_ufactor()));
        runtimes.acumltors.add(new Accumulator(runtimes.itrain.get_num_ifactor()));

        acum_ufactor = new Accumulator(runtimes.featureCount);
        acum_ifactor = new Accumulator(runtimes.featureCount);

        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
        runtimes.tnWs = dctzr.cmpTrainWs(runtimes.itrain);

        if (model == null) {
            fmRef = new CombFactorModel(runtimes.conf,
                (FactorModel) runtimes.plugins.get("AUXILIARY_RCMMD_MODEL"));
            model = fmRef;
        } else {
            fmRef = (CombFactorModel) model;
        }
    }

    /** 
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#update_inner(code.sma.core.AbstractIterator)
     */
    @Override
    protected void update_inner(AbstractIterator iDataElem) {

        // collaboratively train
        collab_update(iDataElem);

        // update runtime environment
        finish_round();

        // biased fit in final turn
        if (runtimes.round == runtimes.maxIter) {
            biased_fit(iDataElem);

            // update runtime environment
            finish_round();
        }
    }

    protected void collab_update(AbstractIterator iDataElem) {
        boolean[] acc_ufi = runtimes.acc_uf_indicator;
        boolean[] acc_ifi = runtimes.acc_if_indicator;

        int featureCount = runtimes.featureCount;
        double learningRate = runtimes.learningRate;

        Accumulator acum_diff_LuLi = runtimes.acumltors.get(0);
        assert acum_diff_LuLi.cursor_vId == 0 : "Check it! At every begin, cursor shouble be at 0";
        Accumulator acum_diff_LuGi = runtimes.acumltors.get(1);
        assert acum_diff_LuGi.cursor_vId == 0 : "Check it! At every begin, cursor shouble be at 0";
        Accumulator acum_diff_GuLi = runtimes.acumltors.get(2);
        assert acum_diff_GuLi.cursor_vId == 0 : "Check it! At every begin, cursor shouble be at 0";

        Loss lossFunction = runtimes.lossFunction;
        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");

        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();
            int u = e.getIndex_user(0);

            DenseVector lref_ufactor = acc_ufi[u]
                ? StatsOperator.getVectorRef(fmRef.ufactors, u, acum_ufactor.toZero())
                : null;
            DenseVector gref_ufactor = fmRef.g_ufactors.getRowRef(u);

            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                DenseVector lref_ifactor = acc_ifi[i]
                    ? StatsOperator.getVectorRef(fmRef.ifactors, i, acum_ifactor.toZero())
                    : null;
                DenseVector gref_ifactor = fmRef.g_ifactors.getRowRef(i);

                double AuiReal = e.getValue_ifactor(f);
                double LuLi = 0.0d;
                if (acc_ufi[u] && acc_ifi[i]) {
                    LuLi = StatsOperator.innerProduct(lref_ufactor, lref_ifactor, lossFunction,
                        AuiReal, acum_diff_LuLi);
                }

                double LuGi = 0.0d;
                if (acc_ufi[u]) {
                    LuGi = StatsOperator.innerProduct(lref_ufactor, gref_ifactor, lossFunction,
                        AuiReal, acum_diff_LuGi);
                }

                double GuLi = 0.0d;
                if (acc_ifi[i]) {
                    GuLi = StatsOperator.innerProduct(gref_ufactor, lref_ifactor, lossFunction,
                        AuiReal, acum_diff_GuLi);
                }

                double RMELuLi = acum_diff_LuLi.rm();
                double RMELuGi = acum_diff_LuGi.rm();
                double RMEGuli = acum_diff_GuLi.rm();

                double deriWRTpLuLi = lossFunction.calcGrad(AuiReal, LuLi) / RMELuLi;
                double deriWRTpLuGi = lossFunction.calcGrad(AuiReal, LuGi) / RMELuGi;
                double deriWRTpGuLi = lossFunction.calcGrad(AuiReal, GuLi) / RMEGuli;

                double tnW = 1 + 0.4 * runtimes.tnWs[dctzr.convert(AuiReal)];
                for (int s = 0; s < featureCount; s++) {
                    double Fus = acc_ufi[u] ? lref_ufactor.floatValue(s) : 0.0d;
                    double fus = gref_ufactor.floatValue(s);
                    double Gis = acc_ifi[i] ? lref_ifactor.floatValue(s) : 0.0d;
                    double gis = gref_ifactor.floatValue(s);

                    if (acc_ufi[u]) {
                        double update_uf = acc_ifi[i]
                            ? (-deriWRTpLuLi * Gis * lambda[0] * tnW
                               - deriWRTpLuGi * gis * lambda[1] * tnW
                               - runtimes.regType.calcReg(Fus, acum_ufactor.rs()))
                            : (-deriWRTpLuGi * gis * 3.0 - L2.calcReg(Fus));
                        double newFus = Fus + learningRate * update_uf;

                        StatsOperator.updateVector(lref_ufactor, s,
                            runtimes.regType.afterReg(newFus), acum_ufactor);
                    }

                    if (acc_ifi[i]) {
                        double update_if = acc_ufi[u]
                            ? (-deriWRTpLuLi * Fus * lambda[0] * tnW
                               - deriWRTpGuLi * fus * lambda[2] * tnW
                               - runtimes.regType.calcReg(Gis, acum_ifactor.rs()))
                            : (-deriWRTpGuLi * fus * 0.8 - L2.calcReg(Gis));
                        double newGis = Gis + learningRate * update_if;

                        StatsOperator.updateVector(lref_ifactor, s,
                            runtimes.regType.afterReg(newGis), acum_ifactor);
                    }
                }
            }
        }
    }

    protected void biased_fit(AbstractIterator iDataElem) {
        boolean[] acc_ufi = runtimes.acc_uf_indicator;
        boolean[] acc_ifi = runtimes.acc_if_indicator;

        int featureCount = runtimes.featureCount;
        double lr = runtimes.learningRate;

        Accumulator acum_diff = runtimes.acumltors.get(0);

        Loss lossFunction = runtimes.lossFunction;
        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");

        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();
            int u = e.getIndex_user(0);

            DenseVector lref_ufactor = fmRef.ufactors.getRowRef(u);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                if (!acc_ufi[u] || !acc_ifi[i]) {
                    continue;
                }

                DenseVector lref_ifactor = fmRef.ifactors.getRowRef(i);

                double AuiReal = e.getValue_ifactor(f);
                double LuLi = (acc_ufi[u] && acc_ifi[i])
                    ? StatsOperator.innerProduct(lref_ufactor, lref_ifactor, lossFunction, AuiReal,
                        acum_diff)
                    : 0.0d;

                double RMELuLi = acum_diff.rm(0);
                double deriWRTpLuLi = lossFunction.calcGrad(AuiReal, LuLi) / RMELuLi;
                double tnW = 1 + 0.4 * runtimes.tnWs[dctzr.convert(AuiReal)];
                for (int s = 0; s < featureCount; s++) {
                    double Fus = lref_ufactor.floatValue(s);
                    double Gis = lref_ifactor.floatValue(s);

                    // perform gradient descent
                    double newFus = Fus + lr * (-deriWRTpLuLi * Gis * tnW - 0.01 * Fus);
                    double newGis = Gis + lr * (-deriWRTpLuLi * Fus * tnW - 0.01 * Gis);

                    // DEFAULT: L2 Regularization
                    StatsOperator.updateVector(lref_ufactor, s, newFus);
                    StatsOperator.updateVector(lref_ifactor, s, newGis);
                }
            }
        }
    }

    /** 
     * @see code.sma.recmmd.cf.ma.standalone.FactorRecmmder#finish_round()
     */
    @Override
    protected void finish_round() {
        runtimes.prevErr = runtimes.currErr;
        runtimes.currErr = runtimes.acumltors.get(0).rm();
        runtimes.round++;
        runtimes.sumErr = 0.0;

        model.trainErr.add(runtimes.currErr);

        if (runtimes.showProgress && (runtimes.round % 5 == 0 || runtimes.round >= runtimes.maxIter)
            && runtimes.itest != null) {
            EvaluationMetrics em = new CollaFiltrMetrics();
            em.evalRating(model, runtimes.itest);
            LoggerUtil.info(runningLogger, String.format("%d\t%.6f [%s]", runtimes.round,
                runtimes.currErr, em.printOneLine()));
            model.testErr.add(em.getRMSE());
        } else {
            LoggerUtil.info(runningLogger,
                String.format("%d\t%.6f,%.6f,%.6f", runtimes.round, runtimes.acumltors.get(0).rm(),
                    runtimes.acumltors.get(1).rm(), runtimes.acumltors.get(2).rm()));
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("GLOMA%s_[%d_%d_%d]", runtimes.briefDesc(),
            Math.round(lambda[0] * 100), Math.round(lambda[1] * 100), Math.round(lambda[2] * 100));
    }

}
