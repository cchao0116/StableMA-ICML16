package code.sma.recmmd.ensemble;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Pair;

import code.sma.core.impl.DenseVector;
import code.sma.core.impl.Tuples;
import code.sma.dpncy.Discretizer;
import code.sma.dpncy.NetflixMovieLensDiscretizer;
import code.sma.recmmd.KernelSmoothing;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.standalone.GLOMA;
import code.sma.recmmd.standalone.MFRecommender;
import code.sma.util.SerializeUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: MultTskREC.java, v 0.1 2017年2月28日 下午12:32:54 Chao.Chen Exp $
 */
public class MultTskREC extends EnsembleMFRecommender {
    /** SerialVersionNum */
    protected static final long                      serialVersionUID = 1L;

    /*
     * ======================================== Model specific parameters
     * ========================================
     */
    /** configure environments */
    private RecConfigEnv                             rce;
    /** the arrays containing random seeds */
    private Queue<Long>                              randSeeds;
    /** the anchors */
    private List<Pair<Integer, Integer>>             anchors          = new ArrayList<Pair<Integer, Integer>>();
    /** the sampling rate of randomized submatrix */
    private double                                   samplingRate;
    /** the instance of the auxiliary model */
    private transient MFRecommender auxRec;
    /** Contribution of each component, i.e., LuLi, LuGi, GuLi */
    private double[]                                 lambda           = { 1.0d, 0.5d, 0.5d };

    /** dicretizer */
    protected transient Discretizer                  dctzr;

    /** the rating distribution w.r.t each user */
    protected double[][]                             ensmblWeightInU;
    /** the rating distribution w.r.t each item */
    protected double[][]                             ensmblWeightInI;

    /*
     * ======================================== Constructors
     * ========================================
     */
    public MultTskREC(RecConfigEnv rce) {
        super(rce);
        this.rce = rce;
        this.samplingRate = (double) rce.get("SAMPLE_RATE_VALUE");
        this.dctzr = new NetflixMovieLensDiscretizer(rce);

        String auxRcmmdPath = (String) rce.get("AUXILIARY_RCMMD_MODEL_PATH");
        this.auxRec = (MFRecommender) SerializeUtil.readObject(auxRcmmdPath);

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

    @Override
    public void buildModel(Tuples rateMatrix, Tuples tMatrix) {
        // compute ensemble weights
        double[][][] ensmbleWs = dctzr.cmpEnsmblWs(rateMatrix, null);
        ensmblWeightInU = ensmbleWs[0];
        ensmblWeightInI = ensmbleWs[1];

        super.buildModel(rateMatrix, tMatrix);
    }

    /**
     * @see code.sma.thread.TaskMsgDispatcher#map()
     */
    @Override
    public Object map() {
        long randSeed = -1;
        boolean isOdd = true;

        synchronized (MAP_MUTEX) {
            if (randSeeds.isEmpty()) {
                return null;
            } else {
                isOdd = false;
                randSeed = randSeeds.poll().longValue();
            }
        }

        if (isOdd) {
            return kernelSmoothedMap(randSeed);
        } else {
            return ranMap(randSeed);
        }
    }

    protected Object kernelSmoothedMap(long randSeed) {
        int anchorUser = -1;
        int anchorItem = -1;
        double width = 0.60;

        synchronized (this) {
            Random ran = new Random(randSeed);

            if (anchors.isEmpty()) {
                anchorUser = ran.nextInt(userCount);
                anchorItem = ran.nextInt(itemCount);
            } else {
                // choose anchor of user
                {
                    double[] minD = new double[userCount];
                    double sumD = 0.0d;

                    double[] anchorNorms = new double[anchors.size()];
                    for (int a = 0; a < anchors.size(); a++) {
                        int anLu = anchors.get(a).getKey();
                        anchorNorms[a] = auxRec.userDenseFeatures.getRowRef(anLu).norm();
                    }

                    for (int u = 0; u < userCount; u++) {
                        DenseVector uVec = auxRec.userDenseFeatures.getRowRef(u);
                        double normuVec = uVec.norm();
                        minD[u] = Double.MAX_VALUE;

                        for (int a = 0; a < anchors.size(); a++) {
                            int anLu = anchors.get(a).getKey();

                            DenseVector anchorVec = auxRec.userDenseFeatures.getRowRef(anLu);
                            double distnt = Math
                                .acos(anchorVec.innerProduct(uVec) / (normuVec * anchorNorms[a]));
                            if (minD[u] > distnt) {
                                minD[u] = distnt;
                            }
                        }
                        sumD += minD[u];
                    }

                    while (true) {
                        anchorUser = ran.nextInt(userCount);
                        if (ran.nextFloat() < minD[anchorUser] * userCount / sumD) {
                            break;
                        }
                    }
                }

                // choose anchor or item
                {
                    double[] minD = new double[itemCount];
                    double sumD = 0.0d;

                    double[] anchorNorms = new double[anchors.size()];
                    for (int a = 0; a < anchors.size(); a++) {
                        int anLi = anchors.get(a).getValue();
                        anchorNorms[a] = auxRec.itemDenseFeatures.getRowRef(anLi).norm();
                    }

                    for (int i = 0; i < itemCount; i++) {
                        DenseVector anchrVec = auxRec.itemDenseFeatures.getRowRef(i);
                        double normAnchorI = anchrVec.norm();
                        minD[i] = Double.MAX_VALUE;

                        for (int a = 0; a < anchors.size(); a++) {
                            {
                                int anLi = anchors.get(a).getValue();
                                DenseVector iVec = auxRec.itemDenseFeatures.getRowRef(anLi);

                                double distnt = Math.acos(
                                    anchrVec.innerProduct(iVec) / (normAnchorI * anchorNorms[a]));
                                if (minD[i] > distnt) {
                                    minD[i] = distnt;
                                }
                            }
                            sumD += minD[i];
                        }

                        while (true) {
                            anchorItem = ran.nextInt(itemCount);
                            if (ran.nextFloat() < minD[anchorItem] * itemCount / sumD) {
                                break;
                            }
                        }
                    }
                }
            }

            anchors.add(new Pair<Integer, Integer>(anchorUser, anchorItem));
        }

        // closely-related users
        boolean[] raf = new boolean[userCount];
        {
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
        synchronized (MultTskREC.class) {
            rcmmd.threadId = tskId++;
        }
        return rcmmd;
    }

    protected Object ranMap(long randSeed) {
        Random ran = new Random(randSeed);
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

        GLOMA rcmmd = new GLOMA(rce, lambda, raf, caf, auxRec);
        synchronized (MultTskREC.class) {
            rcmmd.threadId = tskId++;
        }
        return rcmmd;
    }

    /**
     * @see code.sma.recmmd.ensemble.EnsembleMFRecommender#ensnblWeight(int,
     *      int, double)
     */
    @Override
    public double ensnblWeight(int u, int i, double prediction) {
        int indx = dctzr.convert(prediction);
        return 1.0 + 0.6 * ensmblWeightInU[u][indx] + 0.7 * ensmblWeightInI[i][indx];
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
