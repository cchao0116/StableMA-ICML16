package code.sma.recmmd.standalone;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import org.apache.log4j.Logger;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.main.Configures;
import code.sma.model.FactorModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.RuntimeEnv;
import code.sma.util.EvaluationMetrics;
import code.sma.util.LoggerDefineConstant;
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
public abstract class MFRecommender extends Recommender {

    /** logger */
    protected final static transient Logger runningLogger = Logger
        .getLogger(LoggerDefineConstant.SERVICE_CORE);
    protected final static transient Logger resultLogger  = Logger
        .getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /*========================================
     * Constructors
     *========================================*/
    protected MFRecommender() {
    }

    public MFRecommender(Configures conf, Map<String, Plugin> plugins) {
        model = new FactorModel(conf);
        runtimes = new RuntimeEnv(conf);
        runtimes.plugins = plugins;
    }

    public MFRecommender(Configures conf, boolean[] acc_ufi, boolean[] acc_ifi,
                         Map<String, Plugin> plugins) {
        model = new FactorModel(conf);

        runtimes = new RuntimeEnv(conf);
        runtimes.plugins = plugins;

        runtimes.acc_uf_indicator = acc_ufi;
        runtimes.acc_if_indicator = acc_ifi;
    }

    /*========================================
     * Model Builder
     *========================================*/
    /**
     * @see code.sma.recmmd.Recommender#buildModel(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    public void buildModel(AbstractMatrix train, AbstractMatrix test) {
        LoggerUtil.info(runningLogger, this);

        // prepare runtime environment
        prepare_runtimes(train, test);

        // update model
        AbstractIterator iDataElem = runtimes.itrain;
        while (runtimes.prevErr - runtimes.currErr > 0.0001 && runtimes.round < runtimes.maxIter) {
            update_inner(iDataElem);
        }
    }

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
    }

    protected void update_inner(AbstractIterator iDataElem) {
        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            update_each(e);
        }

        // update runtime environment
        update_runtimes();
    }

    /**
     * update based on one-user's 
     * 
     * @param e user-grouped data, i.e., one-user's data
     */
    protected void update_each(DataElem e) {
    }

    protected void update_runtimes() {
        runtimes.prevErr = runtimes.currErr;
        runtimes.currErr = Math.sqrt(runtimes.sumErr / runtimes.nnz);
        runtimes.round++;
        runtimes.sumErr = 0.0;

        model.trainErr.add(runtimes.currErr);

        if (runtimes.showProgress && (runtimes.round % 5 == 0) && runtimes.itest != null) {
            EvaluationMetrics em = new EvaluationMetrics();
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
     * @see code.sma.recmmd.Recommender#saveModel(java.lang.String)
     */
    @Override
    public void saveModel(String fo) {
        SerializeUtil.writeObject(model, fo);
    }

    /** 
     * @see code.sma.recmmd.Recommender#loadModel(java.lang.String)
     */
    @Override
    public void loadModel(String fi) {
        assert Files.exists((new File(fi)).toPath()) : "The path does not exist.";

        model = (FactorModel) SerializeUtil.readObject(fi);
    }

}
