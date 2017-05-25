package code.sma.recmmd.ensemble;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.datastructure.SparseMatrix;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.standalone.MatrixFactorizationRecommender;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.thread.WeakLearner;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerUtil;

/**
 * Ensemble-based Matrix Approximation method
 * 
 * @author Chao.Chen
 * @version $Id: EnsembleMFRecommender.java, v 0.1 2016年9月26日 下午4:22:14 Chao.Chen Exp $
 */
public abstract class EnsembleMFRecommender extends MatrixFactorizationRecommender
                                            implements TaskMsgDispatcher {
    /** SerialVersionNum */
    protected static final long                  serialVersionUID = 1L;
    /** cumulative prediction */
    protected SparseMatrix                       cumPrediction    = null;
    /** cumulative weights */
    protected SparseMatrix                       cumWeight        = null;
    /** the number of threads in training*/
    protected int                                threadNum;
    /** current assigned thread id*/
    protected int                                tskId            = 0;
    /** algorithm environment*/
    protected transient RecConfigEnv             rce;

    /** mutex using in map procedure*/
    protected static Object                      MAP_MUTEX        = new Object();
    /** mutex using in reduce procedure*/
    protected static Object                      REDUCE_MUTEX     = new Object();
    /** training data*/
    protected transient MatlabFasionSparseMatrix tnMatrix;
    /** testing data*/
    protected transient MatlabFasionSparseMatrix ttMatrix;

    /*========================================
     * Constructors
     *========================================*/
    public EnsembleMFRecommender(RecConfigEnv rce) {
        super(rce);
        this.rce = rce;
        threadNum = ((Double) rce.get("THREAD_NUMBER_VALUE")).intValue();
        cumPrediction = new SparseMatrix(userCount, itemCount);
        cumWeight = new SparseMatrix(userCount, itemCount);
    }

    /** 
     * @see code.sma.recmmd.standalone.MatrixFactorizationRecommender#buildModel(code.sma.datastructure.MatlabFasionSparseMatrix, code.sma.datastructure.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildModel(MatlabFasionSparseMatrix rateMatrix, MatlabFasionSparseMatrix tMatrix) {
        tnMatrix = rateMatrix;
        ttMatrix = tMatrix;

        // run learning threads
        try {
            ExecutorService exec = Executors.newCachedThreadPool();
            for (int t = 0; t < threadNum; t++) {
                exec.execute(new WeakLearner(this, rateMatrix, tMatrix));
            }
            exec.shutdown();
            exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            ExceptionUtil.caught(e, "Ensemble Recmmd Thead!");
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#reduce(code.sma.recmmd.Recommender)
     */
    @Override
    public void reduce(Object recmmd, MatlabFasionSparseMatrix tnMatrix,
                       MatlabFasionSparseMatrix ttMatrix) {
        int[] uIndx = ttMatrix.getRowIndx();
        int[] iIndx = ttMatrix.getColIndx();
        double[] vals = ttMatrix.getVals();

        // update approximated model
        synchronized (REDUCE_MUTEX) {
            int[] testInvlvIndces = ((MatrixFactorizationRecommender) recmmd).testInvlvIndces;
            for (int numSeq : testInvlvIndces) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];

                // update global approximation model
                if (((MatrixFactorizationRecommender) recmmd).userDenseFeatures.getRowRef(u) == null
                    || ((MatrixFactorizationRecommender) recmmd).itemDenseFeatures
                        .getRowRef(i) == null) {
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

        // evaluate approximated model
        // WARNING: this part is not thread safe in order to quick produce the evaluation
        int nnz = ttMatrix.getNnz();
        double rmse = 0.0d;
        for (int numSeq = 0; numSeq < nnz; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double AuiRel = vals[numSeq];
            double AuiEst = (cumWeight.getValue(u, i) == 0.0) ? ((maxValue + minValue) / 2)
                : (cumPrediction.getValue(u, i) / cumWeight.getValue(u, i));
            rmse += Math.pow(AuiEst - AuiRel, 2.0d);
        }
        rmse = Math.sqrt(rmse / nnz);

        LoggerUtil.info(resultLogger,
            String.format("ThreadId: %d\tRMSE: %.6f N[%d][%d]-%.6f",
                ((Recommender) recmmd).threadId, rmse,
                ((MatrixFactorizationRecommender) recmmd).trainInvlvIndces.length,
                ((MatrixFactorizationRecommender) recmmd).testInvlvIndces.length,
                ((MatrixFactorizationRecommender) recmmd).bestRMSE));
    }

    /** 
     * @see code.sma.recmmd.standalone.MatrixFactorizationRecommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        double prediction = (cumWeight.getValue(u, i) == 0.0) ? ((maxValue + minValue) / 2)
            : (cumPrediction.getValue(u, i) / cumWeight.getValue(u, i));

        // normalize the prediction
        if (prediction > maxValue) {
            return maxValue;
        } else if (prediction < minValue) {
            return minValue;
        } else {
            return prediction;
        }
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
