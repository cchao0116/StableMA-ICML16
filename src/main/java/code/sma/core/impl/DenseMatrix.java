package code.sma.core.impl;

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
            return vals[i].floatValue(j);
        } else {
            return 0.0d;
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

    public int[] shape() {
        int[] shape = new int[2];
        shape[0] = M;
        shape[1] = N;
        return shape;
    }
}
