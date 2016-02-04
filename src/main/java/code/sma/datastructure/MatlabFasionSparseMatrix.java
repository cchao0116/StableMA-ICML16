package code.sma.datastructure;

import java.util.Arrays;

/**
 * 
 * @author Hanke
 * @version $Id: MatlabFasionSparseMatrix.java, v 0.1 2015-5-16 下午2:55:00 Exp $
 */
public class MatlabFasionSparseMatrix {
    /** */
    int[]    rowIndx;
    /** */
    int[]    colIndx;
    /** */
    double[] vals;
    /** */
    int      nnz;

    public MatlabFasionSparseMatrix(int nnz) {
        rowIndx = new int[nnz];
        colIndx = new int[nnz];
        vals = new double[nnz];
        this.nnz = 0;
    }

    public void setValue(int i, int j, double value) {
        rowIndx[nnz] = i;
        colIndx[nnz] = j;
        vals[nnz] = value;
        nnz++;
    }

    public void reduceMem() {
        if (nnz < rowIndx.length) {
            rowIndx = Arrays.copyOf(rowIndx, nnz);
            colIndx = Arrays.copyOf(colIndx, nnz);
            vals = Arrays.copyOf(vals, nnz);
        }
    }

    /**
     * Getter method for property <tt>rowIndx</tt>.
     * 
     * @return property value of rowIndx
     */
    public int[] getRowIndx() {
        return rowIndx;
    }

    /**
     * Getter method for property <tt>colIndx</tt>.
     * 
     * @return property value of colIndx
     */
    public int[] getColIndx() {
        return colIndx;
    }

    /**
     * Getter method for property <tt>vals</tt>.
     * 
     * @return property value of vals
     */
    public double[] getVals() {
        return vals;
    }

    /**
     * Getter method for property <tt>nnz</tt>.
     * 
     * @return property value of nnz
     */
    public int getNnz() {
        return nnz;
    }

    public SparseMatrix toSparseMatrix(int m, int n) {
        SparseMatrix sm = new SparseMatrix(m, n);
        for (int indx = 0; indx < nnz; indx++) {
            int u = rowIndx[indx];
            int i = colIndx[indx];
            double AuiReal = vals[indx];
            sm.setValue(u, i, AuiReal);
        }
        return sm;
    }

}
