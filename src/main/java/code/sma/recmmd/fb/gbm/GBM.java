package code.sma.recmmd.fb.gbm;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.eval.EvaluationMetrics;
import code.sma.eval.FeatureBasedMetrics;
import code.sma.main.Configures;
import code.sma.model.AbstractModel;
import code.sma.model.BoostedModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.RuntimeEnv;
import code.sma.recmmd.fb.FeatureBasedRecmmder;
import code.sma.util.LoggerUtil;
import code.sma.util.SerializeUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

/**
 * 
 * @author Chao.Chen
 * @version $Id: GBM.java, Jul 3, 2017 4:23:15 PM$
 */
public class GBM extends FeatureBasedRecmmder {
    /** model*/
    protected BoostedModel gbm;
    /** gradients*/
    private FloatArrayList grad;
    /** hessian*/
    private FloatArrayList hess;

    public GBM(Configures conf, Map<String, Plugin> plugins) {
        runtimes = new RuntimeEnv(conf);
        runtimes.plugins = plugins;
    }

    /** 
     * @see code.sma.recmmd.Recommender#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
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
        grad = new FloatArrayList(new float[train.num_row]);
        hess = new FloatArrayList(new float[train.num_row]);

        if (gbm == null) {
            gbm = new BoostedModel(runtimes.conf);
        }

    }

    /** 
     * @see code.sma.recmmd.Recommender#finish_round()
     */
    @Override
    protected void finish_round() {
        {
            runtimes.round++;
            EvaluationMetrics em = new FeatureBasedMetrics();
            em.evalRating(gbm, runtimes.itrain);
            runtimes.prevErr = runtimes.currErr;
            runtimes.currErr = em.getRMSE();
        }

        if (runtimes.showProgress && (runtimes.round % 5 == 0) && runtimes.itest != null) {
            EvaluationMetrics em = new FeatureBasedMetrics();
            em.evalRating(gbm, runtimes.itest);
            LoggerUtil.info(runningLogger, String.format("%d\t%.6f [%s]", runtimes.round,
                runtimes.currErr, em.printOneLine()));
            gbm.testErr.add(em.getRMSE());
        } else {
            LoggerUtil.info(runningLogger,
                String.format("%d\t%.6f", runtimes.round, runtimes.currErr));
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#buildModel(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    public void buildModel(AbstractMatrix train, AbstractMatrix test) {
        LoggerUtil.info(runningLogger, this);

        // prepare runtime environment
        prepare_runtimes(train, test);

        AbstractIterator iDataElem = runtimes.itrain;
        while (runtimes.prevErr - runtimes.currErr > 0.0001 && runtimes.round < runtimes.maxIter) {

            //compute gradients and hessians
            int rowId = 0;

            iDataElem.refresh();
            while (iDataElem.hasNext()) {
                DataElem e = iDataElem.next();
                double label = e.getLabel();
                double pred = gbm.predict(e)[0];

                double grad = runtimes.lossFunction.calcGrad(label, pred);
                double hess = runtimes.lossFunction.calcHession(label, pred);

                this.grad.set(rowId, (float) grad);
                this.hess.set(rowId, (float) hess);
                rowId++;
            }

            Configures conf = new Configures(runtimes.conf);
            Booster booster = new CARTBooster(conf, runtimes.plugins);
            gbm.add(booster.doBoost(train, test, grad, hess));

            finish_round();
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#loadModel(java.lang.String)
     */
    @Override
    public void loadModel(String fi) {
        assert Files.exists((new File(fi)).toPath()) : "The path does not exist.";
        gbm = (BoostedModel) SerializeUtil.readObject(fi);
    }

    /** 
     * @see code.sma.recmmd.Recommender#getModel()
     */
    @Override
    public AbstractModel getModel() {
        return gbm;
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("GBM%s", runtimes.briefDesc());
    }

}
