package code.sma.recmmd.ensemble;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import code.sma.datastructure.DenseVector;
import code.sma.recmmd.KernelSmoothing;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.standalone.GLOMA;
import code.sma.recmmd.standalone.MatrixFactorizationRecommender;
import code.sma.util.SerializeUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: MultTskREC.java, v 0.1 2017年2月28日 下午12:32:54 Chao.Chen Exp $
 */
public class MultTskREC extends EnsembleMFRecommender {
    /** SerialVersionNum */
    protected static final long serialVersionUID = 1L;

    /*========================================
     * Model specific parameters
     *========================================*/
    /** configure environments*/
    private RecConfigEnv        rce;
    /** the arrays containing random seeds*/
    private Queue<Long>         randSeeds;
    /** the sampling rate of randomized submatrix */
    private double              samplingRate;
    /** the path of the auxiliary model */
    private String              auxRcmmdPath;
    /** Contribution of each component, i.e., LuLi, LuGi, GuLi */
    private double[]            lambda           = { 1.0d, 0.5d, 0.5d };

    /*========================================
     * Constructors
     *========================================*/
    public MultTskREC(RecConfigEnv rce) {
        super(rce);
        this.rce = rce;
        this.samplingRate = (double) rce.get("SAMPLE_RATE_VALUE");
        this.auxRcmmdPath = (String) rce.get("AUXILIARY_RCMMD_MODEL_PATH");

        {
            String lam = (String) rce.get("LAMBDA");
            if (lam != null) {
                String[] lamStr = lam.split(",");
                for (int l = 0; l < 3; l++) {
                    lambda[l] = Double.valueOf(lamStr[l]).doubleValue();
                }
            }
        }

        this.randSeeds = new LinkedList<Long>();
        {
            String[] randSds = ((String) rce.get("RANDOM_SEED_SET")).split(",|\t| ");
            for (String rand : randSds) {
                long seed = Long.valueOf(rand.trim());
                randSeeds.add(seed == -1 ? ((long) (Math.random() * Long.MAX_VALUE)) : seed);
            }
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#map()
     */
    @Override
    public Object map() {
        return kernelSmoothedMap();
    }

    protected Object kernelSmoothedMap() {
        synchronized (MAP_MUTEX) {
            if (randSeeds.isEmpty()) {
                return null;
            } else {
                double width = 0.6d;
                MatrixFactorizationRecommender auxRec = (MatrixFactorizationRecommender) SerializeUtil
                    .readObject(auxRcmmdPath);
                Random ran = new Random(randSeeds.poll().longValue());

                // closely-related users
                boolean[] raf = new boolean[userCount];
                {
                    int anchorUser = ran.nextInt(userCount);
                    DenseVector anchorVec = auxRec.userDenseFeatures.getRowRef(anchorUser);
                    double normAnchorU = anchorVec.norm();
                    for (int u = 0; u < userCount; u++) {
                        DenseVector uVec = auxRec.userDenseFeatures.getRowRef(u);
                        double sim = 1 - 2.0 / Math.PI * Math
                            .acos(anchorVec.innerProduct(uVec) / (normAnchorU * uVec.norm()));
                        if (KernelSmoothing.EPANECHNIKOV_KERNEL.kernelize(sim, width) > 0) {
                            raf[u] = true;
                        }
                    }
                }

                // closely-related items
                boolean[] caf = new boolean[itemCount];
                {
                    int anchorItem = ran.nextInt(itemCount);
                    DenseVector anchrVec = auxRec.itemDenseFeatures.getRowRef(anchorItem);
                    double normAnchorI = anchrVec.norm();
                    for (int i = 0; i < itemCount; i++) {
                        DenseVector iVec = auxRec.itemDenseFeatures.getRowRef(i);
                        double sim = 1 - 2.0 / Math.PI * Math
                            .acos(anchrVec.innerProduct(iVec) / (normAnchorI * iVec.norm()));

                        if (KernelSmoothing.EPANECHNIKOV_KERNEL.kernelize(sim, width) > 0) {
                            caf[i] = true;
                        }
                    }
                }

                GLOMA rcmmd = new GLOMA(rce, lambda, raf, caf, auxRec);
                rcmmd.threadId = tskId++;
                return rcmmd;
            }
        }
    }

    protected Object ranMap() {
        synchronized (MAP_MUTEX) {
            if (randSeeds.isEmpty()) {
                return null;
            } else {
                Random ran = new Random(randSeeds.poll().longValue());
                boolean[] raf = new boolean[userCount];
                for (int u = 0; u < userCount; u++) {
                    if (ran.nextFloat() < samplingRate) {
                        raf[u] = true;
                    }
                }

                boolean[] caf = new boolean[itemCount];
                for (int i = 0; i < itemCount; i++) {
                    if (ran.nextFloat() < samplingRate) {
                        caf[i] = true;
                    }
                }

                MatrixFactorizationRecommender auxRec = (MatrixFactorizationRecommender) SerializeUtil
                    .readObject(auxRcmmdPath);
                GLOMA rcmmd = new GLOMA(rce, lambda, raf, caf, auxRec);
                rcmmd.threadId = tskId++;
                return rcmmd;
            }
        }
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Param[%d]: FC:%d LR:%.7f R:%.7f ALG[MultTskREC][%.2f]%s", maxIter,
            featureCount, learningRate, regularizer, samplingRate, ArrayUtils.toString(lambda));
    }

}
