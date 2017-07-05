package code.sma.recmmd.cf.ma.standalone;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.eval.CollaFiltrMetrics;
import code.sma.eval.EvaluationMetrics;
import code.sma.main.Configures;
import code.sma.model.AbstractModel;
import code.sma.model.FactorModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.RuntimeEnv;
import code.sma.util.LoggerUtil;
import code.sma.util.SerializeUtil;

/**
 * This is an abstract class implementing four matrix-factorization-based methods
 * including Regularized SVD, NMF, PMF, and Bayesian PMF.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public abstract class FactorRecmmder extends Recommender {
    /** Resulting model */
    protected FactorModel model;

    /*========================================
     * Constructors
     *========================================*/
    protected FactorRecmmder() {
    }

    public FactorRecmmder(Configures conf, Map<String, Plugin> plugins) {
        runtimes = new RuntimeEnv(conf);
        runtimes.plugins = plugins;
    }

    public FactorRecmmder(Configures conf, boolean[] acc_ufi, boolean[] acc_ifi,
                          Map<String, Plugin> plugins) {
        runtimes = new RuntimeEnv(conf);
        runtimes.plugins = plugins;

        runtimes.acc_uf_indicator = acc_ufi;
        runtimes.acc_if_indicator = acc_ifi;
    }

    /*========================================
     * Model Builder
     *========================================*/
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
        runtimes.nnz = runtimes.itrain.get_num_ifactor();

        if (model == null) {
            model = new FactorModel(runtimes.conf);
        }
    }

    /**
     * @see code.sma.recmmd.Recommender#finish_round()
     */
    @Override
    protected void finish_round() {
        runtimes.prevErr = runtimes.currErr;
        runtimes.currErr = Math.sqrt(runtimes.sumErr / runtimes.nnz);
        runtimes.round++;
        runtimes.sumErr = 0.0;

        model.trainErr.add(runtimes.currErr);

        if (runtimes.showProgress && (runtimes.round % 5 == 0) && runtimes.itest != null) {
            EvaluationMetrics em = new CollaFiltrMetrics();
            em.evalRating(model, runtimes.itest);
            LoggerUtil.info(runningLogger, String.format("%d\t%.6f [%s]", runtimes.round,
                runtimes.currErr, em.printOneLine()));
            model.testErr.add(em.getRMSE());
        } else {
            LoggerUtil.info(runningLogger,
                String.format("%d\t%.6f", runtimes.round, runtimes.currErr));
        }
    }

    /** 
     * @see code.sma.recmmd.Recommender#loadModel(java.lang.String)
     */
    @Override
    public void loadModel(String fi) {
        assert Files.exists((new File(fi)).toPath()) : "The path does not exist.";
        model = (FactorModel) SerializeUtil.readObject(fi);
    }

    /** 
     * @see code.sma.recmmd.Recommender#getModel()
     */
    @Override
    public AbstractModel getModel() {
        return model;
    }

}
