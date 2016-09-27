package code.sma.datastructure;

import java.io.Serializable;

/**
 * Dense Matrix used to represent the features
 * 
 * @author Chao Chen
 * @version $Id: DenseMatrix.java, v 0.1 2015-5-16 下午3:15:50 Exp $
 */
public class DenseMatrix implements Serializable {
    /**  */
    private static final long serialVersionUID = 1L;
    /** The matrix of values*/
    private DenseVector[]     vals;
    /** The number of rows. */
    private int               M;
    /** The number of columns. */
    private int               N;

    /**
     * @param m
     * @param n
     */
    public DenseMatrix(int m, int n) {
        M = m;
        N = n;
        vals = new DenseVector[M];
    }

    // ======================================== 
    //          Getter/Setter
    // ========================================

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
        if (vals[i] != null) {
            return vals[i].getValue(j);
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
    public void setValue(int i, int j, double value, boolean needRanInit) {
        if (vals[i] == null) {
            vals[i] = new DenseVector(N, needRanInit);
        } else {
            vals[i].setValue(j, value);
        }
    }

    /**
     * return the reference of the vector at the given row index
     * 
     * @param i The row index
     * @return  The reference
     */
    public DenseVector getRowRef(int i) {
        return vals[i];
    }

    /**
     * set the reference of the vector at the given row index
     * 
     * @param i The row index
     * @param b The reference
     */
    public void setRowRef(int i, DenseVector b) {
        vals[i] = b;
    }

    /**
     * Inner product of two vectors.
     * 
     * @param u                 the index of rows in this object 
     * @param i                 the index of rows in tDenseFeature
     * @param tDenseMatrix     the transposed DenseMatrix
     * @return
     */
    public double innerProduct(int u, int i, DenseMatrix tDenseMatrix, boolean needRanInit) {
        if (needRanInit & vals[u] == null) {
            this.setRowRef(u, new DenseVector(N, true));

        }
        if (needRanInit & tDenseMatrix.getRowRef(i) == null) {
            tDenseMatrix.setRowRef(i, new DenseVector(N, true));
        }

        return vals[u].innerProduct(tDenseMatrix.getRowRef(i));
    }

}
