package code.sma.recmmd.standalone;

import code.sma.core.DynIntArr;
import code.sma.core.impl.SparseMatrix;
import code.sma.core.impl.Tuples;
import code.sma.core.impl.UJMPDenseMatrix;
import code.sma.core.impl.UJMPDenseVector;
import code.sma.recmmd.RecConfigEnv;
import code.sma.util.LoggerUtil;

/**
 * This is a class implementing GroupSparse Matrix Factorization (GSMF)
 * Technical detail of the algorithm can be found in
 * Ting Yuan, Recommendation by Mining Multiple User Behaviors with Group Sparsity
 * Proceedings of AAAI, 2014
 * 
 * @author Chao Chen
 * @version $Id: GSMF.java, v 0.1 Jan 28, 2016 1:05:24 PM Exp $
 */
public class GroupSparsityMF extends MFRecommender {
    /**  SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /** Number of item clusters*/
    private int               L;
    /** User profile in low-rank matrix form. */
    private UJMPDenseMatrix   userUJMPFeatures;
    /** Item profile in low-rank matrix form. */
    private UJMPDenseMatrix   itemUJMPFeatures;
    /** The Indicator matrix indexed by user id*/
    private DynIntArr[]       IijIndxU;
    /** The Indicator matrix indexed by item id*/
    private DynIntArr[]       IijIndxI;

    /** Controlling factor for loss function */
    double                    alpha            = 1;
    /** Controlling factor for the degree of group-sparsity regularization. */
    double                    beta             = 70;
    /** Controlling factor for the degree of regularization. */
    double                    lambda           = 0.05;

    /*========================================
     * Constructors
     *========================================*/
    public GroupSparsityMF(RecConfigEnv rce) {
        super(rce);
        alpha = (double) rce.get("ALPA_VALUE");
        beta = (double) rce.get("BETA_VALUE");
        lambda = (double) rce.get("LAMBDA_VALUE");
        this.L = ((Double) rce.get("ITEM_CLUSTER_NUM_VALUE")).intValue();
    }

    /** 
     * @see MFRecommender.tongji.ml.matrix.MatrixFactorizationRecommender#buildModel(Tuples.tongji.data.MatlabFasionSparseMatrix, Tuples.tongji.data.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildModel(Tuples rateMatrix, Tuples tMatrix) {
        //1. initialize features
        initFeatures();

        //2. 
        int[] itemAssigm = new int[itemCount];
        initParam(itemAssigm, rateMatrix);

        //3. updating model
        int round = 0;
        int rateCount = rateMatrix.getNnz();
        if (rateCount >= 20 * 1000 * 1000) {
            System.gc();
        }

        double prevErr = 99999;
        double currErr = 9999;
        boolean isCollaps = false;
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter && !isCollaps) {

            // a) update U features
            updateU(rateMatrix);

            // b) update V features
            updateI(rateMatrix, itemAssigm);

            // c) training error
            prevErr = currErr;
            currErr = trainError(rateMatrix);
            round++;
            isCollaps = recordLoggerAndDynamicStop(round, tMatrix, currErr);
        }

    }

    protected void initFeatures() {
        LoggerUtil.info(runningLogger, "Param: FC: " + featureCount + "\talpha: " + alpha
                                       + "\tbeta: " + beta + "\tlambda: " + lambda);
        //user features
        userUJMPFeatures = new UJMPDenseMatrix(userCount, featureCount);
        for (int u = 0; u < userCount; u++) {
            for (int f = 0; f < featureCount; f++) {
                double rdm = Math.random() / featureCount;
                userUJMPFeatures.setValue(u, f, rdm);
            }
        }

        //item features
        itemUJMPFeatures = new UJMPDenseMatrix(featureCount, itemCount);
        for (int i = 0; i < itemCount; i++) {
            for (int f = 0; f < featureCount; f++) {
                double rdm = Math.random() / featureCount;
                itemUJMPFeatures.setValue(f, i, rdm);
            }
        }
    }

    protected void initParam(int[] itemAssigm, Tuples rateMatrix) {
        // dividing rating matrix into l pieces
        for (int i = 0; i < itemCount; i++) {
            itemAssigm[i] = (int) (Math.random() * L);
        }

        // initial I matrix
        if (IijIndxU != null && IijIndxI != null) {
            return;
        }

        IijIndxU = new DynIntArr[userCount];
        IijIndxI = new DynIntArr[itemCount];
        SparseMatrix sm = rateMatrix.toSparseMatrix(userCount, itemCount);
        for (int uIndx = 0; uIndx < userCount; uIndx++) {
            IijIndxU[uIndx] = new DynIntArr(sm.getRowRef(uIndx).indexList());
        }

        for (int iIndx = 0; iIndx < itemCount; iIndx++) {
            IijIndxI[iIndx] = new DynIntArr(sm.getColRef(iIndx).indexList());
        }
    }

    protected void updateU(Tuples rateMatrix) {
        int rateCount = rateMatrix.getNnz();
        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        float[] Auis = rateMatrix.getVals();

        // the right vector term for every user
        UJMPDenseMatrix rightSideMtx = new UJMPDenseMatrix(userCount, featureCount);
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double Rui = Auis[numSeq];
            rightSideMtx.plusInRow(u, itemUJMPFeatures.getCol(i).scale(alpha * Rui));
        }
        for (int i = 0; i < userCount; i++) {
            // left matrix term, which will be inversed
            UJMPDenseMatrix leftSideMtx = UJMPDenseMatrix.makeIdentity(featureCount).scale(lambda);
            for (int ratedItemIndx : IijIndxU[i].getArr()) {
                UJMPDenseVector Vj = itemUJMPFeatures.getCol(ratedItemIndx);
                leftSideMtx = leftSideMtx.plus(Vj.outerProduct(Vj).scale(alpha));
            }

            // follow Equation (7)
            UJMPDenseVector Ui = leftSideMtx.inverse().times(rightSideMtx.getRow(i));
            for (int fcIndx = 0; fcIndx < featureCount; fcIndx++) {
                userUJMPFeatures.setValue(i, fcIndx, Ui.getValue(fcIndx));
            }
        }
    }

    protected void updateI(Tuples rateMatrix, int[] itemAssigm) {
        int rateCount = rateMatrix.getNnz();
        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        float[] Auis = rateMatrix.getVals();

        // the right vector term for every item
        UJMPDenseMatrix rightSideMtx = new UJMPDenseMatrix(itemCount, featureCount);
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double Rui = Auis[numSeq];
            rightSideMtx.plusInRow(i, userUJMPFeatures.getRow(u).scale(Rui));
        }

        // compute D^b
        UJMPDenseMatrix[] Ds = new UJMPDenseMatrix[L];
        for (int l = 0; l < L; l++) {
            Ds[l] = new UJMPDenseMatrix(featureCount, featureCount);
        }
        for (int t = 0; t < featureCount; t++) {
            double[] Dtt = new double[L];
            for (int j = 0; j < itemCount; j++) {
                Dtt[itemAssigm[j]] += Math.pow(itemUJMPFeatures.getValue(t, j), 2.0d);
            }

            for (int l = 0; l < L; l++) {
                Ds[l].setValue(t, t, beta / Math.sqrt(Dtt[l]));
            }
        }

        // update V
        for (int j = 0; j < itemCount; j++) {
            // left matrix term, which will be inversed
            UJMPDenseMatrix leftSideMtx = UJMPDenseMatrix.makeIdentity(featureCount)
                .scale(lambda / alpha).plus(Ds[itemAssigm[j]]);

            for (int usrRatedIndx : IijIndxI[j].getArr()) {
                UJMPDenseVector Vj = userUJMPFeatures.getRow(usrRatedIndx);
                leftSideMtx = leftSideMtx.plus(Vj.outerProduct(Vj));
            }

            // follow Equation (5)
            UJMPDenseVector Vj = leftSideMtx.inverse().times(rightSideMtx.getRow(j));
            for (int fcIndx = 0; fcIndx < featureCount; fcIndx++) {
                itemUJMPFeatures.setValue(fcIndx, j, Vj.getValue(fcIndx));
            }
        }
    }

    protected double trainError(Tuples rateMatrix) {
        double sum = 0.0d;

        int rateCount = rateMatrix.getNnz();
        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        float[] Auis = rateMatrix.getVals();
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double Rui = Auis[numSeq];
            double RuiEstm = userUJMPFeatures.getRow(u).innerProduct(itemUJMPFeatures.getCol(i));
            sum += Math.pow(Rui - RuiEstm, 2.0);
        }
        return Math.sqrt(sum / rateCount);
    }

    /** 
     * @see MFRecommender.tongji.ml.matrix.MatrixFactorizationRecommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        double prediction = this.offset
                            + userUJMPFeatures.getRow(u).innerProduct(itemUJMPFeatures.getCol(i));

        if (prediction > maxValue) {
            return maxValue;
        } else if (prediction < minValue) {
            return minValue;
        } else {
            return prediction;
        }
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Param: FC: " + featureCount + " LR: " + learningRate + " R: " + regularizer
               + " ALG[GSMF]";
    }
}
