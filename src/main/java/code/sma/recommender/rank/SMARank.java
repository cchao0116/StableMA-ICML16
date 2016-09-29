package code.sma.recommender.rank;

import java.util.HashSet;
import java.util.Set;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recommender.RecConfigEnv;
import code.sma.recommender.standalone.MatrixFactorizationRecommender;
import code.sma.util.LoggerUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: SMARank.java, v 0.1 2016年9月29日 上午11:22:33 Chao.Chen Exp $
 */
public class SMARank extends MatrixFactorizationRecommender {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    /**  negative sampling rate*/
    private double            negSmplnRat;

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
     * @param rce The recommender's specific parameters
     */
    public SMARank(int uc, int ic, double max, double min, int fc, double lr, double r, double m,
                   int iter, boolean verbose, RecConfigEnv rce) {
        super(uc, ic, max, min, fc, lr, r, m, iter, verbose, rce);
        negSmplnRat = (Double) rce.get("NEGATIVE_SAMPLING_RATE_VALUE");
    }

    /** 
     * @see code.sma.recommender.standalone.MatrixFactorizationRecommender#buildModel(code.sma.datastructure.MatlabFasionSparseMatrix, code.sma.datastructure.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildModel(MatlabFasionSparseMatrix rateMatrix, MatlabFasionSparseMatrix tMatrix) {
        super.buildModel(rateMatrix, tMatrix);

        // Collect user observations
        UserObsvtn[] uObsvtn = new UserObsvtn[userCount];
        formUserObservations(rateMatrix, uObsvtn);

        // Gradient Descent:
        int round = 0;
        int rateCount = rateMatrix.getNnz();
        double prevErr = 99999;
        double currErr = 9999;

        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        double[] Auis = rateMatrix.getVals();

        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter) {
            double sum = 0.0;
            int tnCount = 0;

            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int u = uIndx[numSeq];

                // find negative observations for this positive sample
                int uObsvtnSize = uObsvtn[u].size();
                int num = (int) (negSmplnRat * (itemCount - uObsvtnSize) / uObsvtnSize) + 1;
                int[] vInds = new int[num];
                vInds[0] = iIndx[numSeq];
                for (int v = 1; v < num; v++) {
                    do {
                        vInds[v] = (int) (Math.random() * itemCount);
                    } while (!uObsvtn[u].contains(vInds[v]));
                }

                //training process
                for (int v = 0; v < num; v++) {
                    int i = vInds[v];
                    tnCount++;

                    double AuiReal = (v == 0) ? Auis[numSeq] : -1.0d;
                    double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                    sum += lossFunction.diff(AuiReal, AuiEst);

                    double deriWRTp = lossFunction.dervWRTPrdctn(AuiReal, AuiEst);
                    for (int s = 0; s < featureCount; s++) {
                        double Fus = userDenseFeatures.getValue(u, s);
                        double Gis = itemDenseFeatures.getValue(i, s);

                        //global model updates
                        userDenseFeatures.setValue(u, s,
                            Fus + learningRate * (deriWRTp * Gis - regularizer * Fus), true);
                        itemDenseFeatures.setValue(i, s,
                            Gis + learningRate * (deriWRTp * Fus - regularizer * Gis), true);
                    }
                }
            }

            prevErr = currErr;
            currErr = Math.sqrt(sum / tnCount);

            round++;
            LoggerUtil.info(runningLogger, round + "\t" + String.format("%.6f", currErr));
        }

    }

    /**
     * find the whole observations for every user
     * 
     * @param rateMatrix    training data
     * @param uObsvtn       the observations for every user
     */
    protected void formUserObservations(MatlabFasionSparseMatrix rateMatrix, UserObsvtn[] uObsvtn) {
        int rateCount = rateMatrix.getNnz();
        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];

            if (uObsvtn[u] == null) {
                uObsvtn[u] = new UserObsvtn();
            }
            uObsvtn[u].add(i);
        }
    }

    /**
     * Bean class containing all user observations
     * 
     * @author Chao.Chen
     * @version $Id: SMARank.java, v 0.1 2016年9月29日 下午12:31:40 Chao.Chen Exp $
     */
    protected class UserObsvtn {
        private Set<Integer> obsvatn;

        public UserObsvtn() {
            obsvatn = new HashSet<Integer>();
        }

        public void add(Integer obn) {
            obsvatn.add(obn);
        }

        public boolean contains(Integer obn) {
            return obsvatn.contains(obn);
        }

        public int size() {
            return obsvatn.size();
        }
    }
}
