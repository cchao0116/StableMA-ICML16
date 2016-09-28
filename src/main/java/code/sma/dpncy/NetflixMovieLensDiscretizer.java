package code.sma.dpncy;

import org.apache.commons.math3.stat.StatUtils;

import code.sma.datastructure.MatlabFasionSparseMatrix;

/**
 * Convert the rating data in Netflix and Movielens into continuous discrete data
 * e.g,
 *      0.5, 1.0, ..., 5.0 => 0, 1, ..., 9
 *      
 * @author Chao.Chen
 * @version $Id: NetflixMovieLensDiscretizer.java, v 0.1 2016年9月28日 下午1:34:51 Chao.Chen Exp $
 */
public class NetflixMovieLensDiscretizer extends Discretizer {
    /** The number of users. */
    private int    userCount;
    /** The number of items. */
    private int    itemCount;
    /** minimum value*/
    private double minValue;
    /** maximum value*/
    private double maxValue;

    public NetflixMovieLensDiscretizer(int userCount, int itemCount, double maxValue,
                                       double minValue) {
        super();
        this.userCount = userCount;
        this.itemCount = itemCount;
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    /** 
     * @see code.sma.dpncy.Discretizer#convert(double)
     */
    @Override
    public int convert(double val) {
        return (int) ((val - minValue) / minValue);
    }

    /** 
     * @see code.sma.dpncy.Discretizer#cmpTrainWs(code.sma.datastructure.MatlabFasionSparseMatrix, int[])
     */
    @Override
    public double[] cmpTrainWs(MatlabFasionSparseMatrix tnMatrix, int[] invlvIndces) {
        int tnS = (int) (maxValue / minValue);
        double[] tnWs = new double[tnS];
        double[] Auis = tnMatrix.getVals();
        for (int numSeq : invlvIndces) {
            tnWs[convert(Auis[numSeq])]++;
        }

        // every entry plus 1 to avoid zero condition
        for (int t = 0; t < tnS; t++) {
            tnWs[t] = (tnWs[t] + 1) / (tnMatrix.getNnz() + tnS);
        }
        return tnWs;
    }

    /** 
     * @see code.sma.dpncy.Discretizer#cmpEnsmblWs(code.sma.datastructure.MatlabFasionSparseMatrix, int[])
     */
    @Override
    public double[][][] cmpEnsmblWs(MatlabFasionSparseMatrix tnMatrix, int[] invlvIndces) {
        int tnS = (int) (maxValue / minValue);
        double[][][] emWs = new double[2][0][0];
        emWs[0] = new double[userCount][tnS]; // user-related weights
        emWs[1] = new double[itemCount][tnS]; // item-related weights

        int nnz = tnMatrix.getNnz();
        int[] uIndx = tnMatrix.getRowIndx();
        int[] iIndx = tnMatrix.getColIndx();
        double[] Auis = tnMatrix.getVals();
        for (int numSeq = 0; numSeq < nnz; numSeq++) {
            int uId = uIndx[numSeq];
            int iId = iIndx[numSeq];

            int label = convert(Auis[numSeq]);
            emWs[0][uId][label]++;
            emWs[1][iId][label]++;
        }

        //user side
        for (int uId = 0; uId < userCount; uId++) {
            double sum = StatUtils.sum(emWs[0][uId]);
            if (sum == 0.0d) {
                continue;
            }

            for (int t = 0; t < tnS; t++) {
                emWs[0][uId][t] = (emWs[0][uId][t] + 1) / (sum + tnS);
            }
        }

        for (int iId = 0; iId < itemCount; iId++) {
            double sum = StatUtils.sum(emWs[1][iId]);
            if (sum == 0.0d) {
                continue;
            }

            for (int t = 0; t < tnS; t++) {
                emWs[1][iId][t] = (emWs[1][iId][t] + 1) / (sum + tnS);
            }
        }
        return emWs;
    }

}
