package code.sma.recmmd.ensemble;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.impl.SparseMatrix;
import code.sma.main.Configures;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.standalone.MFRecommender;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.thread.WeakLearner;
import code.sma.util.EvaluationMetrics;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerUtil;

/**
 * Ensemble-based Matrix Approximation method
 * 
 * @author Chao.Chen
 * @version $Id: EnsembleMFRecommender.java, v 0.1 2016年9月26日 下午4:22:14 Chao.Chen Exp $
 */
public abstract class EnsembleMFRecommender extends MFRecommender implements TaskMsgDispatcher {
    /** SerialVersionNum */
    protected static final long serialVersionUID = 1L;
    /** cumulative prediction */
    protected SparseMatrix      cumPrediction    = null;
    /** cumulative weights */
    protected SparseMatrix      cumWeight        = null;

    /** mutex using in map procedure*/
    protected static Object     MAP_MUTEX        = new Object();
    /** mutex using in reduce procedure*/
    protected static Object     REDUCE_MUTEX     = new Object();

    /*========================================
     * Constructors
     *========================================*/
    public EnsembleMFRecommender(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);
        cumPrediction = new SparseMatrix(runtimes.userCount, runtimes.itemCount);
        cumWeight = new SparseMatrix(runtimes.userCount, runtimes.itemCount);
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#buildModel(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    public void buildModel(AbstractMatrix train, AbstractMatrix test) {
        // run learning threads
        try {
            ExecutorService exec = Executors.newCachedThreadPool();
            for (int t = 0; t < runtimes.threadNum; t++) {
                exec.execute(new WeakLearner(this, train, test));
            }
            exec.shutdown();
            exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            ExceptionUtil.caught(e, "Ensemble Recmmd Thead!");
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#reduce(java.lang.Object, code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    public void reduce(Object recmmd, AbstractMatrix train, AbstractMatrix test) {
        // update approximated model
        synchronized (REDUCE_MUTEX) {

            AbstractIterator iDataElem = (AbstractIterator) test.iterator();
            iDataElem.refresh();
            while (iDataElem.hasNext()) {
                DataElem e = iDataElem.next();
                short num_ifactor = e.getNum_ifacotr();

                int u = e.getIndex_user(0);
                for (int f = 0; f < num_ifactor; f++) {
                    int i = e.getIndex_item(f);

                    // update global approximation model
                    if (((MFRecommender) recmmd).userDenseFeatures.getRowRef(u) == null
                        || ((MFRecommender) recmmd).itemDenseFeatures.getRowRef(i) == null) {
                        continue;
                    }

                    double prediction = ((Recommender) recmmd).predict(u, i);
                    double weight = ensnblWeight(u, i, prediction);

                    double newCumPrediction = prediction * weight + cumPrediction.getValue(u, i);
                    double newCumWeight = weight + cumWeight.getValue(u, i);

                    cumPrediction.setValue(u, i, newCumPrediction);
                    cumWeight.setValue(u, i, newCumWeight);
                }
            }
        }

        // evaluate approximated model
        // WARNING: this part is not thread safe in order to quick produce the evaluation
        EvaluationMetrics em = new EvaluationMetrics(this);

        LoggerUtil.info(resultLogger,
            String.format("ThreadId: %d\tRMSE: %.6f - %.6f",
                ((MFRecommender) recmmd).runtimes.threadId, em.getRMSE(),
                ((MFRecommender) recmmd).runtimes.bestTestErr()));
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        double maxValue = runtimes.maxValue;
        double minValue = runtimes.minValue;

        double prediction = (cumWeight.getValue(u, i) == 0.0) ? ((maxValue + minValue) / 2)
            : (cumPrediction.getValue(u, i) / cumWeight.getValue(u, i));
        return Math.max(minValue, Math.min(prediction, maxValue));
    }

    /**
     * return the weight of which the prediction
     * 
     * @param u the given user index
     * @param i the given item index
     * @param prediction the predicted rating
     * @return
     */
    public double ensnblWeight(int u, int i, double prediction) {
        return 1.0d;
    }

}
