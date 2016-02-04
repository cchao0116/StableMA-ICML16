package code.sma.datastructure;

import java.io.Serializable;

/**
 * 
 * @author Hanke
 * @version $Id: DenseMatrix.java, v 0.1 2015-5-16 下午3:15:50 Exp $
 */
public class DenseMatrix implements Serializable {
    /**  */
    private static final long serialVersionUID = 1L;
    /** */
    boolean[]   nzRows;
    /** */
    double[][]  vals;

    /** The number of rows. */
    private int M;
    /** The number of columns. */
    private int N;

    /**
     * @param m
     * @param n
     */
    public DenseMatrix(int m, int n) {
        M = m;
        N = n;
        vals = new double[M][0];
        nzRows = new boolean[M];
    }

    /*
     * ======================================== Getter/Setter
     * ========================================
     */
    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i
     *            The row index to retrieve.
     * @param j
     *            The column index to retrieve.
     * @return The value stored at the given index.
     */
    public double getValue(int i, int j) {
        if (nzRows[i]) {
            return vals[i][j];
        } else {
            return 0.0d;
        }
    }

    /**
     * Set a new value at the given index.
     * 
     * @param i
     *            The row index to store new value.
     * @param j
     *            The column index to store new value.
     * @param value
     *            The value to store.
     */
    public void setValue(int i, int j, double value) {
        if (!nzRows[i]) {
            nzRows[i] = true;
            vals[i] = new double[N];
        }

        vals[i][j] = value;
    }

    /**
     * nner product of two vectors.
     * 
     * @param u                 the index of rows in this object 
     * @param i                 the index of rows in tDenseFeature
     * @param tDenseMatrix     the transposed DenseMatrix
     * @return
     */
    public double innerProduct(int u, int i, DenseMatrix tDenseMatrix) {
        double sum = 0.0d;
        for (int f = 0; f < N; f++) {
            sum += this.getValue(u, f) * tDenseMatrix.getValue(f, i);
        }
        return sum;
    }

}
