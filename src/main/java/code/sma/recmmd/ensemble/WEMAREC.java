package code.sma.recmmd.ensemble;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.main.Configures;
import code.sma.plugin.Discretizer;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.standalone.MFRecommender;
import code.sma.recmmd.standalone.WeigtedSVD;
import code.sma.thread.TaskMsgDispatcher;
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

    /** the arrays containing various clusterings*/
    protected transient Queue<String>      clusterDirList;
    /** the learning task buffer*/
    protected transient Queue<Recommender> recmmdsBuffer;

    /*========================================
     * Constructors
     *========================================*/
    public WEMAREC(Configures conf, Map<String, Plugin> plugins, Queue<String> clusterDirs) {
        super(conf, plugins);
        runtimes.doubles.add(conf.getDouble("BETA0_VALUE"));
        runtimes.doubles.add(conf.getDouble("BETA1_VALUE"));
        runtimes.doubles.add(conf.getDouble("BETA2_VALUE"));

        clusterDirList = clusterDirs;
        recmmdsBuffer = new LinkedList<Recommender>();
    }

    /** 
     * @see code.sma.recmmd.ensemble.EnsembleMFRecommender#buildModel(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    public void buildModel(AbstractMatrix train, AbstractMatrix test) {
        // compute ensemble weights
        Discretizer dctzr = (Discretizer) runtimes.plugins.get("DISCRETIZER");
        double[][][] ensmbleWs = dctzr.cmpEnsmblWs((AbstractIterator) train.iterator());
        runtimes.ensmblUWs = ensmbleWs[0];
        runtimes.ensmblIWs = ensmbleWs[1];

        super.buildModel(train, test);
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
                    boolean[][][] acc_features = ClusterInfoUtil.readAFI(runtimes.userCount,
                        runtimes.itemCount, clusterDir);

                    boolean[][] uf_indicator = acc_features[0];
                    boolean[][] if_indicator = acc_features[1];

                    for (int ufi = 0; ufi < uf_indicator.length; ufi++) {
                        for (int ifi = 0; ifi < if_indicator.length; ifi++) {
                            MFRecommender wsvd = new WeigtedSVD(runtimes.conf, runtimes.plugins);
                            wsvd.runtimes.acc_uf_indicator = uf_indicator[ufi];
                            wsvd.runtimes.acc_if_indicator = if_indicator[ifi];
                            wsvd.runtimes.threadId = runtimes.threadId++;
                            recmmdsBuffer.add(wsvd);
                        }
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
        int indx = ((Discretizer) runtimes.plugins.get("DISCRETIZER")).convert(prediction);
        return 1.0 + runtimes.doubles.getDouble(1) * runtimes.ensmblUWs[u][indx]
               + runtimes.doubles.getDouble(2) * runtimes.ensmblIWs[i][indx];
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("WEMAREC%s_Tn[%d]_Ens[%d_%d]", runtimes.briefDesc(),
            (int) (runtimes.doubles.getDouble(0)), (int) (runtimes.doubles.getDouble(1)),
            (int) (runtimes.doubles.getDouble(2)));
    }

}
