package code.sma.recmmd.standalone;

import code.sma.datastructure.Accumulator;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recmmd.Loss;
import code.sma.util.ClusterInfoUtil;
import code.sma.util.LoggerUtil;

/**
 * GLOMA: Embedding Global Information in Local Matrix Approximation Models for Collaborative Filtering 
 * Chao Chen, Dongsheng Li, Qin Lv, Junchi Yan, Li Shang, Stephen M. Chu 
 * In AAAI Conference on Artificial Intelligence (AAAI), 2017
 *
 * @author Chao.Chen
 * @version $Id: GLOMA.java, v 0.1 2017年2月28日 下午1:46:27 Chao.Chen Exp $
 */
public class GLOMA extends MatrixFactorizationRecommender {
    /** SerialVersionNum */
    private static final long              serialVersionUID = 1L;
    /**Indicator function to show whether the row is within the class*/
    private boolean[]                      raf;
    /**Indicator function to show whether the column is within the class*/
    private boolean[]                      caf;
    /** Contribution of each component, i.e., LuLi, LuGi, GuLi */
    private double[]                       lambda;
    /** Previously-trained model*/
    private MatrixFactorizationRecommender auxRec;

    /*========================================
     * Constructors
     *========================================*/
    public GLOMA(int uc, int ic, double max, double min, int fc, double lr, double r, double m,
                 int iter, boolean verbose, Loss lossFunction, double[] lambda, boolean[] raf,
                 boolean[] caf, MatrixFactorizationRecommender auxRec) {
        super(uc, ic, max, min, fc, lr, r, m, iter, verbose, lossFunction, null, null);
        this.lambda = lambda;
        this.raf = raf;
        this.caf = caf;
        this.auxRec = auxRec;
    }

    /** 
     * @see code.sma.recmmd.standalone.MatrixFactorizationRecommender#buildloclModel(code.sma.datastructure.MatlabFasionSparseMatrix, code.sma.datastructure.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildloclModel(MatlabFasionSparseMatrix rateMatrix,
                               MatlabFasionSparseMatrix tMatrix) {
        super.buildloclModel(rateMatrix, tMatrix);

        // Gradient Descent:
        int round = 0;
        int rateCount = rateMatrix.getNnz();
        double prevErr = 99999;
        double currErr = 9999;
        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        double[] Auis = rateMatrix.getVals();

        // Compute the involved entries
        trainInvlvIndces = ClusterInfoUtil.readInvolvedIndicesExpanded(rateMatrix, raf, caf);
        testInvlvIndces = ClusterInfoUtil.readInvolvedIndices(tMatrix, raf, caf);

        // statistics of current model
        Accumulator accErr = new Accumulator(3, rateCount);
        Accumulator accFactrUsr = new Accumulator(featureCount, userCount);
        Accumulator accFactrItm = new Accumulator(featureCount, itemCount);
        statistics(rateMatrix, trainInvlvIndces, accErr, accFactrUsr, accFactrItm);

        // SGD
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter) {
            for (int numSeq : trainInvlvIndces) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];
                double AuiReal = Auis[numSeq];

                double LuLi = 0.0d;
                if (raf[u] && caf[i]) {
                    LuLi = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                    accErr.update(0, numSeq, lossFunction.diff(AuiReal, LuLi));
                }

                double LuGi = 0.0d;
                if (raf[u]) {
                    LuGi = userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures, true);
                    accErr.update(1, numSeq, lossFunction.diff(AuiReal, LuGi));
                }

                double GuLi = 0.0d;
                if (caf[i]) {
                    GuLi = auxRec.userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                    accErr.update(2, numSeq, lossFunction.diff(AuiReal, GuLi));
                }

                double RMELuLi = accErr.rm(0);
                double RMELuGi = accErr.rm(1);
                double RMEGuli = accErr.rm(2);

                double deriWRTpLuLi = lossFunction.dervWRTPrdctn(AuiReal, LuLi) / RMELuLi;
                double deriWRTpLuGi = lossFunction.dervWRTPrdctn(AuiReal, LuGi) / RMELuGi;
                double deriWRTpGuLi = lossFunction.dervWRTPrdctn(AuiReal, GuLi) / RMEGuli;

                for (int s = 0; s < featureCount; s++) {
                    double Fus = userDenseFeatures.getValue(u, s);
                    double fus = auxRec.userDenseFeatures.getValue(u, s);
                    double Gis = itemDenseFeatures.getValue(i, s);
                    double gis = auxRec.itemDenseFeatures.getValue(i, s);

                    if (raf[u] && caf[i]) {
                        userDenseFeatures.setValue(u, s,
                            Fus + learningRate * (-deriWRTpLuLi * Gis * lambda[0]
                                                  - deriWRTpLuGi * gis * lambda[1]
                                                  - regularizer * Fus / accFactrUsr.rs(s)),
                            true);
                        itemDenseFeatures.setValue(i, s,
                            Gis + learningRate * (-deriWRTpLuLi * Fus * lambda[0]
                                                  - deriWRTpGuLi * fus * lambda[2]
                                                  - regularizer * Gis / accFactrItm.rs(s)),
                            true);

                        auxRec.itemDenseFeatures.setValue(i, s,
                            gis + learningRate * (-deriWRTpLuGi * Fus - regularizer * gis), true);

                        auxRec.userDenseFeatures.setValue(u, s,
                            fus + learningRate * (-deriWRTpGuLi * Gis - regularizer * fus), true);

                        accFactrUsr.update(s, u, Math.pow(userDenseFeatures.getValue(u, s), 2.0));
                        accFactrItm.update(s, i, Math.pow(itemDenseFeatures.getValue(i, s), 2.0));
                    } else if (raf[u]) {
                        userDenseFeatures
                            .setValue(u, s,
                                Fus + learningRate * (-deriWRTpLuGi * gis
                                                      - regularizer * Fus / accFactrUsr.rs(s)),
                                true);
                        auxRec.itemDenseFeatures.setValue(i, s,
                            gis + learningRate * (-deriWRTpLuGi * Fus - regularizer * gis), true);
                        accFactrUsr.update(s, u, Math.pow(userDenseFeatures.getValue(u, s), 2.0));
                    } else if (caf[i]) {
                        itemDenseFeatures
                            .setValue(i, s,
                                Gis + learningRate * (-deriWRTpGuLi * fus
                                                      - regularizer * Gis / accFactrItm.rs(s)),
                                true);
                        auxRec.userDenseFeatures.setValue(u, s,
                            fus + learningRate * (-deriWRTpGuLi * Gis - regularizer * fus), true);
                        accFactrItm.update(s, i, Math.pow(itemDenseFeatures.getValue(i, s), 2.0));
                    }
                }
            }

            prevErr = currErr;
            currErr = accErr.rm(0);
            round++;

            // Show progress:
            if (runningLogger.isDebugEnabled()) {
                Accumulator accTest = new Accumulator(3, testInvlvIndces.length);
                int testNum = 0;
                for (int numSeq : testInvlvIndces) {
                    int u = tMatrix.getRowIndx()[numSeq];
                    int i = tMatrix.getColIndx()[numSeq];
                    double AuiReal = tMatrix.getVals()[numSeq];

                    if (raf[u] && caf[i]) {
                        double LuLi = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                        double LuGi = userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures,
                            true);
                        double GuLi = auxRec.userDenseFeatures.innerProduct(u, i, itemDenseFeatures,
                            true);
                        accTest.insert(0, testNum, lossFunction.diff(AuiReal, LuLi));
                        accTest.insert(1, testNum, lossFunction.diff(AuiReal, LuGi));
                        accTest.insert(2, testNum, lossFunction.diff(AuiReal, GuLi));
                    } else if (raf[u]) {
                        double LuGi = userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures,
                            true);
                        accTest.insert(1, testNum, lossFunction.diff(AuiReal, LuGi));
                    } else if (caf[i]) {
                        double GuLi = auxRec.userDenseFeatures.innerProduct(u, i, itemDenseFeatures,
                            true);
                        accTest.insert(2, testNum, lossFunction.diff(AuiReal, GuLi));
                    }
                    testNum++;
                }

                LoggerUtil.info(runningLogger,
                    String.format("%d: %.5f,%.5f,%.5f-[%.5f],[%.5f],[%.5f]\t[%.5f, %.5f]", round,
                        accErr.rm(0), accErr.rm(1), accErr.rm(2), accTest.rm(0), accTest.rm(1),
                        accTest.rm(2)),
                    accFactrUsr.rm(), accFactrItm.rm());
            } else if (showProgress) {
                LoggerUtil.info(runningLogger,
                    String.format("%d: %.5f,%.5f,%.5f\t[%.5f, %.5f]", round, accErr.rm(0),
                        accErr.rm(1), accErr.rm(2), accFactrUsr.rm(), accFactrItm.rm()));
            }

        }
    }

    /**
     * calculate the error and count w.r.t individual entry of current model
     * 
     * @param rateMatrix    training model
     * @param invlvIndces   involved indices
     * @param indvdlErr     individual error of three mixture models respectively
     * @param squrError     square error of three mixture models respectively
     * @param invlvCounts   item count of three mixture models respectively
     * @return  the squared error of three mixture models
     */
    protected void statistics(MatlabFasionSparseMatrix rateMatrix, int[] invlvIndces,
                              Accumulator accErr, Accumulator accFactrUsr,
                              Accumulator accFactrItm) {
        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        double[] Auis = rateMatrix.getVals();

        // initialize accumulator about ERROR
        for (int numSeq : invlvIndces) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double AuiReal = Auis[numSeq];

            if (raf[u] && caf[i]) {
                double LuLi = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                accErr.insert(0, numSeq, lossFunction.diff(AuiReal, LuLi));
            }

            if (raf[u]) {
                double LuGi = userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures, true);
                accErr.insert(1, numSeq, lossFunction.diff(AuiReal, LuGi));
            }

            if (caf[i]) {
                // local model with global_V U \tilde_V
                double GuLi = auxRec.userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                accErr.insert(2, numSeq, lossFunction.diff(AuiReal, GuLi));
            }
        }

        // initialize accumulator about FEATURE
        for (int u = 0; u < userCount; u++) {
            if (userDenseFeatures.getRowRef(u) == null) {
                continue;
            }

            for (int f = 0; f < featureCount; f++) {
                accFactrUsr.insert(f, u, Math.pow(userDenseFeatures.getValue(u, f), 2.0d));
            }
        }

        for (int i = 0; i < itemCount; i++) {
            if (itemDenseFeatures.getRowRef(i) == null) {
                continue;
            }

            for (int f = 0; f < featureCount; f++) {
                accFactrItm.insert(f, i, Math.pow(itemDenseFeatures.getValue(i, f), 2.0d));
            }
        }
    }

}
