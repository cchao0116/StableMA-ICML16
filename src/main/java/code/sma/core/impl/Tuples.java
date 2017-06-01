package code.sma.core.impl;

import java.util.Arrays;
import java.util.Iterator;

import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;

/**
 * 
 * @author Chao Chen
 * @version $Id: MatlabFasionSparseMatrix.java, v 0.1 2015-5-16 下午2:55:00 Exp $
 */
public class Tuples extends AbstractMatrix {
    /** array of user id*/
    protected int[]   rowIndx;
    /** array of item id*/
    protected int[]   colIndx;
    /** array of labels*/
    protected float[] vals;
    /** number of non-zero labels*/
    protected int     nnz;

    /** index of next element to return*/

    public Tuples(int nnz) {
        this.rowIndx = new int[nnz];
        this.colIndx = new int[nnz];
        this.vals = new float[nnz];
        this.nnz = 0;
    }

    /**
     * @see code.sma.core.AbstractMatrix#setValue(int, int, double)
     */
    @Override
    public void setValue(int i, int j, double value) {
        rowIndx[nnz] = i;
        colIndx[nnz] = j;
        vals[nnz] = (float) value;
        nnz++;
    }

    /** 
     * @see code.sma.core.AbstractMatrix#getValue(int, int)
     */
    @Override
    public double getValue(int i, int j) {
        throw new RuntimeException("This method has not been implemented in Tuples!");
    }

    /** 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<DataElem> iterator() {
        return new Iter();
    }

    protected class Iter implements Iterator<DataElem> {
        protected int cursor  = 0;
        /** index of last element returned; -1 if no such*/
        protected int lastRet = -1;

        /** 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return cursor != nnz;
        }

        /** 
         * @see java.util.Iterator#next()
         */
        @Override
        public DataElem next() {
            DataElem e = new DataElem(vals[cursor]);

            int[] index_global = new int[2];
            index_global[0] = rowIndx[cursor];
            index_global[1] = colIndx[cursor];
            e.setIndex_global(index_global);

            cursor++;
            return e;
        }

        /** 
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new RuntimeException("This method has not been implemented in Tuples!");
        }

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
    public float[] getVals() {
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
            float AuiReal = vals[indx];
            sm.setValue(u, i, AuiReal);
        }
        return sm;
    }

}
