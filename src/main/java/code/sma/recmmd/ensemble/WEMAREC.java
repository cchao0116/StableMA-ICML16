package code.sma.recmmd.ensemble;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import code.sma.core.impl.Tuples;
import code.sma.dpncy.Discretizer;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.standalone.WeigtedSVD;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.thread.WeakLearner;
import code.sma.util.ClusterInfoUtil;
import code.sma.util.ExceptionUtil;

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
    protected static final long            serialVersionUID = 1L;

    /** dicretizer */
    protected Discretizer                  dctzr;

    /** the arrays containing various clusterings*/
    protected transient Queue<String>      clusterDirList;
    /** the learning task buffer*/
    protected transient Queue<Recommender> recmmdsBuffer;

    /*========================================
     * Model specific parameters
     *========================================*/
    /** parameter used in training*/
    protected double                       beta0            = 0.4f;
    /** parameter used in ensemble (user-related) */
    public double                          beta1            = 0.7f;
    /** parameter used in ensemble (item-related) */
    public double                          beta2            = 0.8f;

    /** the rating distribution w.r.t each user*/
    protected double[][]                   ensmblWeightInU;
    /** the rating distribution w.r.t each item*/
    protected double[][]                   ensmblWeightInI;

    /*========================================
     * Constructors
     *========================================*/
    public WEMAREC(RecConfigEnv rce, Discretizer dr, Queue<String> clusterDirs) {
        super(rce);
        beta0 = (Double) rce.get("BETA0_VALUE");
        beta1 = (Double) rce.get("BETA1_VALUE");
        beta2 = (Double) rce.get("BETA2_VALUE");

        dctzr = dr;
        clusterDirList = clusterDirs;
        recmmdsBuffer = new LinkedList<Recommender>();
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#buildGloblModel(code.sma.core.impl.Tuples, code.sma.core.impl.Tuples)
     */
    @Override
    public void buildModel(Tuples rateMatrix, Tuples tMatrix) {
        tnMatrix = rateMatrix;
        ttMatrix = tMatrix;

        // compute ensemble weights
        double[][][] ensmbleWs = dctzr.cmpEnsmblWs(tnMatrix, null);
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
            ExceptionUtil.caught(e, "WEMAREC Thread!");
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
                try {
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
                        Recommender wsvd = new WeigtedSVD(rce, tnInvlvedIndcs[c], ttInvlvedIndcs[c],
                            beta0, dctzr);
                        wsvd.threadId = tskId++;
                        recmmdsBuffer.add(wsvd);
                    }
                    return recmmdsBuffer.poll();
                } catch (IOException e) {
                    ExceptionUtil.caught(e, "DIR: " + clusterDir);
                    return null;
                }
            }

        }
    }

    /** 
     * @see code.sma.recmmd.ensemble.EnsembleMFRecommender#ensnblWeight(int, int, double)
     */
    @Override
    public double ensnblWeight(int u, int i, double prediction) {
        int indx = dctzr.convert(prediction);
        return 1.0 + beta1 * ensmblWeightInU[u][indx] + beta2 * ensmblWeightInI[i][indx];
    }

}
