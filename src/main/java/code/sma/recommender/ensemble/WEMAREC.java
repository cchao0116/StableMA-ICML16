package code.sma.recommender.ensemble;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.depndncy.Discretizer;
import code.sma.recommender.RecConfigEnv;
import code.sma.recommender.Recommender;
import code.sma.recommender.standalone.MatrixFactorizationRecommender;
import code.sma.recommender.standalone.WeigtedSVD;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.thread.WeakLearner;
import code.sma.util.ClusterInfoUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;

/**
 * The task dispatcher used in WEMAREC
 * Technical detail of the algorithm can be found in
 * Chao Chen, WEMAREC: Accurate and Scalable Recommendation through Weighted and Ensemble Matrix Approximation,
 * Proceedings of SIGIR, 2015.
 * 
 * @author Chao.Chen
 * @version $Id: WEMARECDispatcherImpl.java, v 0.1 2016年9月26日 下午4:32:41 Chao.Chen Exp $
 */
public class WEMAREC extends EnsembleMFRecommender implements TaskMsgDispatcher {
    /** SerialVersionNum */
    protected static final long      serialVersionUID = 1L;
    /** mutex using in map procedure*/
    protected static Object          MAP_MUTEX        = new Object();
    /** mutex using in reduce procedure*/
    protected static Object          REDUCE_MUTEX     = new Object();

    /** training data*/
    private MatlabFasionSparseMatrix tnMatrix;
    /** testing data*/
    private MatlabFasionSparseMatrix ttMatrix;
    /** dicretizer */
    protected Discretizer            dctzr;

    /** the number of threads in training*/
    protected int                    threadNum;
    /** the arrays containing various clusterings*/
    protected Queue<String>          clusterDirList;
    /** the learning task buffer*/
    protected Queue<Recommender>     recmmdsBuffer;

    /*========================================
     * Model specific parameters
     *========================================*/
    /** parameter used in training*/
    protected double                 beta0            = 0.4f;
    /** parameter used in ensemble (user-related) */
    public double                    beta1            = 0.7f;
    /** parameter used in ensemble (item-related) */
    public double                    beta2            = 0.8f;

    /** the rating distribution w.r.t each user*/
    protected double[][]             ensmblWeightInU;
    /** the rating distribution w.r.t each item*/
    protected double[][]             ensmblWeightInI;

    protected final static Logger    threadLogger     = Logger
        .getLogger(LoggerDefineConstant.SERVICE_THREAD);

    /**
     * Construct a matrix-factorization-based model with the given data.
     * 
     * @param uc The number of users in the dataset.
     * @param ic The number of items in the dataset.
     * @param max The maximum rating value in the dataset.
     * @param min The minimum rating value in the dataset.
     * @param fc The number of features used for describing user and item profiles.
     * @param lr Learning rate for gradient-based or iterative optimization.
     * @param r Controlling factor for the degree of regularization. 
     * @param m Momentum used in gradient-based or iterative optimization.
     * @param iter The maximum number of iterations.
     * @param verbose Indicating whether to show iteration steps and train error.
     * @param rce  The computational hyper-parameters
     * @param dr The dicretizer to convert continuous data
     * @param clusterDirs The clustering configure files used in WEMAREC
     */
    public WEMAREC(int uc, int ic, double max, double min, int fc, double lr, double r, double m,
                   int iter, boolean verbose, RecConfigEnv rce, Discretizer dr,
                   Queue<String> clusterDirs) {
        super(uc, ic, max, min, fc, lr, r, m, iter, verbose);
        beta0 = (Double) rce.get("BETA0");
        beta1 = (Double) rce.get("BETA1");
        beta2 = (Double) rce.get("BETA2");
        threadNum = (Integer) rce.get("THREAD_NUMBER");
        dctzr = dr;
        clusterDirList = clusterDirs;
        recmmdsBuffer = new LinkedList<Recommender>();
    }

    /** 
     * @see code.sma.recommender.standalone.MatrixFactorizationRecommender#buildGloblModel(code.sma.datastructure.MatlabFasionSparseMatrix, code.sma.datastructure.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildModel(MatlabFasionSparseMatrix rateMatrix, MatlabFasionSparseMatrix tMatrix) {
        super.buildModel(rateMatrix, tMatrix);
        tnMatrix = rateMatrix;
        ttMatrix = tMatrix;

        // compute ensemble weights
        double[][][] ensmbleWs = dctzr.cmpEnsmblWs(ttMatrix, null);
        ensmblWeightInU = ensmbleWs[0];
        ensmblWeightInI = ensmbleWs[1];

        // run learning threads
        try {
            ExecutorService exec = Executors.newCachedThreadPool();
            for (int t = 0; t < threadNum; t++) {
                exec.execute(new WeakLearner(this, rateMatrix, tMatrix));
            }
            exec.shutdown();
            exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            ExceptionUtil.caught(e, "WEMAREC Thead!");
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#map()
     */
    @Override
    public Recommender map() {
        synchronized (MAP_MUTEX) {
            if (!recmmdsBuffer.isEmpty()) {
                return recmmdsBuffer.poll();
            } else if (clusterDirList.isEmpty()) {
                return null;
            } else {
                String clusterDir = clusterDirList.poll();
                int[] raf = new int[userCount];
                int[] caf = new int[itemCount];
                int[] clusteringSize = ClusterInfoUtil.readClusteringAssigmntFunction(raf, caf,
                    clusterDir);
                int[][] tnInvlvedIndcs = ClusterInfoUtil.readInvolvedIndices(tnMatrix, raf, caf,
                    clusteringSize);
                int[][] ttInvlvedIndcs = ClusterInfoUtil.readInvolvedIndices(ttMatrix, raf, caf,
                    clusteringSize);

                int clusterNum = clusteringSize[0] * clusteringSize[1];
                for (int c = 0; c < clusterNum; c++) {
                    Recommender wsvd = new WeigtedSVD(userCount, itemCount, maxValue, minValue,
                        featureCount, learningRate, regularizer, momentum, maxIter,
                        tnInvlvedIndcs[c], ttInvlvedIndcs[c], beta0, dctzr);
                    recmmdsBuffer.add(wsvd);
                }
                return recmmdsBuffer.poll();
            }
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#reduce(code.sma.recommender.Recommender)
     */
    @Override
    public void reduce(Recommender recmmd, MatlabFasionSparseMatrix tnMatrix,
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
                double prediction = recmmd.predict(u, i);
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

        LoggerUtil.info(threadLogger, (new StringBuilder("ThreadId: " + recmmd.threadId))
            .append(String.format("\tRMSE: %.6f", rmse)));
    }

    /** 
     * @see code.sma.recommender.ensemble.EnsembleMFRecommender#ensnblWeight(int, int, double)
     */
    @Override
    public double ensnblWeight(int u, int i, double prediction) {
        int indx = dctzr.convert(prediction);
        return 1.0 + beta1 * ensmblWeightInU[u][indx] + beta2 * ensmblWeightInI[i][indx];
    }

}
