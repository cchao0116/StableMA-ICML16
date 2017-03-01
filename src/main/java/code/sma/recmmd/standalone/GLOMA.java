package code.sma.recmmd.standalone;

import java.util.Arrays;

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
    /** Previously-trained model*/
    private MatrixFactorizationRecommender auxRec;

    /*========================================
     * Constructors
     *========================================*/
    public GLOMA(int uc, int ic, double max, double min, int fc, double lr, double r, double m,
                 int iter, boolean verbose, Loss lossFunction, boolean[] raf, boolean[] caf,
                 MatrixFactorizationRecommender auxRec) {
        super(uc, ic, max, min, fc, lr, r, m, iter, verbose, lossFunction, null, null);
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
        trainInvlvIndces = ClusterInfoUtil.readInvolvedIndices(rateMatrix, raf, caf);
        testInvlvIndces = ClusterInfoUtil.readInvolvedIndices(tMatrix, raf, caf);

        // statistics of current model
        double[][] indvdlErr = new double[4][0];
        indvdlErr[1] = new double[rateCount];
        indvdlErr[2] = new double[rateCount];
        indvdlErr[3] = new double[rateCount];

        int[] invlvCounts = new int[4];
        double[] squrErr = new double[4];
        statistics(rateMatrix, trainInvlvIndces, indvdlErr, squrErr, invlvCounts);

        // SGD
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter) {
            for (int numSeq : trainInvlvIndces) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];
                double AuiReal = Auis[numSeq];

                double LuLi = 0.0d;
                if (raf[u] && caf[i]) {
                    LuLi = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                    double err = lossFunction.diff(AuiReal, LuLi);

                    squrErr[1] += err - indvdlErr[1][numSeq];
                    indvdlErr[1][numSeq] = err;
                }

                double LuGi = 0.0d;
                if (raf[u]) {
                    LuGi = userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures, true);
                    double err = lossFunction.diff(AuiReal, LuGi);

                    squrErr[2] += err - indvdlErr[2][numSeq];
                    indvdlErr[2][numSeq] = err;
                }

                double GuLi = 0.0d;
                if (caf[i]) {
                    GuLi = auxRec.userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                    double err = lossFunction.diff(AuiReal, GuLi);

                    squrErr[3] += err - indvdlErr[3][numSeq];
                    indvdlErr[3][numSeq] = err;
                }

                double RMELuLi = Math.sqrt(squrErr[1] / invlvCounts[1]);
                double RMELuGi = Math.sqrt(squrErr[2] / invlvCounts[2]);
                double RMEGuli = Math.sqrt(squrErr[3] / invlvCounts[3]);

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
                            Fus + learningRate
                                  * (-deriWRTpLuLi * Gis - deriWRTpLuGi * gis - regularizer * Fus),
                            true);
                        itemDenseFeatures.setValue(i, s,
                            Gis + learningRate
                                  * (-deriWRTpLuLi * Fus - deriWRTpGuLi * fus - regularizer * Gis),
                            true);

                        auxRec.itemDenseFeatures.setValue(i, s,
                            gis + learningRate * (-deriWRTpLuGi * Fus - regularizer * gis), true);

                        auxRec.userDenseFeatures.setValue(u, s,
                            fus + learningRate * (-deriWRTpGuLi * Gis - regularizer * fus), true);
                    } else if (raf[u]) {
                        userDenseFeatures.setValue(u, s,
                            Fus + learningRate * (-deriWRTpLuGi * gis - regularizer * Fus), true);
                        auxRec.itemDenseFeatures.setValue(i, s,
                            gis + learningRate * (-deriWRTpLuGi * Fus - regularizer * gis), true);
                    } else if (caf[i]) {
                        itemDenseFeatures.setValue(i, s,
                            Gis + learningRate * (-deriWRTpGuLi * fus - regularizer * Gis), true);
                        auxRec.userDenseFeatures.setValue(u, s,
                            fus + learningRate * (-deriWRTpGuLi * Gis - regularizer * fus), true);
                    }
                }
            }

            prevErr = currErr;
            currErr = Math.sqrt(squrErr[1] / invlvCounts[1]);
            round++;

            // Show progress:
            {
                if (showProgress) {
                    LoggerUtil.info(runningLogger,
                        String.format("%d: %.5f,%.5f,%.5f", round, currErr,
                            Math.sqrt(squrErr[2] / (invlvCounts[1] + invlvCounts[2])),
                            Math.sqrt(squrErr[3] / (invlvCounts[1] + invlvCounts[3]))

                        ));
                }
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
                              double[][] indvdlErr, double[] squrError, int[] invlvCounts) {
        // refresh statistical parameters
        Arrays.fill(indvdlErr[1], 0.0d);
        Arrays.fill(indvdlErr[2], 0.0d);
        Arrays.fill(indvdlErr[3], 0.0d);

        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        double[] Auis = rateMatrix.getVals();

        for (int numSeq : invlvIndces) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double AuiReal = Auis[numSeq];

            if (raf[u] && caf[i]) {
                double LuLi = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                indvdlErr[1][numSeq] = lossFunction.diff(AuiReal, LuLi);
                squrError[1] += indvdlErr[1][numSeq];
                invlvCounts[1]++;
            }

            if (raf[u]) {
                double LuGi = userDenseFeatures.innerProduct(u, i, auxRec.itemDenseFeatures, true);
                indvdlErr[2][numSeq] = lossFunction.diff(AuiReal, LuGi);
                squrError[2] += indvdlErr[2][numSeq];
                invlvCounts[2]++;
            }

            if (caf[i]) {
                // local model with global_V U \tilde_V
                double GuLi = auxRec.userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                indvdlErr[3][numSeq] = lossFunction.diff(AuiReal, GuLi);
                squrError[3] += indvdlErr[3][numSeq];
                invlvCounts[3]++;
            }
        }
    }

}
