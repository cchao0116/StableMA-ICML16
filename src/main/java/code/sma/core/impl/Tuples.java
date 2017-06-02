package code.sma.core.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.util.StringUtil;

/**
 * 
 * @author Chao Chen
 * @version $Id: MatlabFasionSparseMatrix.java, v 0.1 2015-5-16 下午2:55:00 Exp $
 */
public class Tuples extends AbstractMatrix {
    /**  number of rows in the sparse matrix */
    protected int     num_row;
    /**  number of nonzero entries in sparse matrix */
    protected int     num_val;

    /** array of user id*/
    protected int[]   rowIndx;
    /** array of item id*/
    protected int[]   colIndx;
    /** array of labels*/
    protected float[] vals;

    public Tuples(int nnz) {
        this.rowIndx = new int[nnz];
        this.colIndx = new int[nnz];
        this.vals = new float[nnz];
        this.num_val = 0;
    }

    /**
     * @see code.sma.core.AbstractMatrix#setValue(int, int, double)
     */
    @Override
    public void setValue(int i, int j, double value) {
        rowIndx[num_val] = i;
        colIndx[num_val] = j;
        vals[num_val] = (float) value;
        num_val++;
    }

    /** 
     * @see code.sma.core.AbstractMatrix#getValue(int, int)
     */
    @Override
    public double getValue(int i, int j) {
        throw new RuntimeException("This method has not been implemented in Tuples!");
    }

    /** 
     * @see code.sma.core.AbstractMatrix#loadNext(java.lang.String)
     */
    @Override
    public void loadNext(String line) {
        Assert.assertTrue("Line must not be blank", StringUtil.isNotBlank(line));
        Scanner scanner = new Scanner(line);
        scanner.skip("^(\\d+\\s+){4}");
        scanner.useDelimiter(":+|\\s+");

        int uId = scanner.nextInt();
        scanner.nextFloat();

        while (scanner.hasNextInt()) {
            rowIndx[num_val] = uId;
            colIndx[num_val] = scanner.nextInt();
            vals[num_val] = scanner.nextFloat();
            num_val++;
        }

        num_row++;
        IOUtils.closeQuietly(scanner);
    }

    /** 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<DataElem> iterator() {
        return new Iter();
    }

    protected class Iter extends AbstractIterator {

        /** 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return cursor != num_row;
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

    }

    public void reduceMem() {
        if (num_val < rowIndx.length) {
            rowIndx = Arrays.copyOf(rowIndx, num_val);
            colIndx = Arrays.copyOf(colIndx, num_val);
            vals = Arrays.copyOf(vals, num_val);
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
        return num_val;
    }

    public SparseMatrix toSparseMatrix(int m, int n) {
        SparseMatrix sm = new SparseMatrix(m, n);
        for (int indx = 0; indx < num_val; indx++) {
            int u = rowIndx[indx];
            int i = colIndx[indx];
            float AuiReal = vals[indx];
            sm.setValue(u, i, AuiReal);
        }
        return sm;
    }

}
