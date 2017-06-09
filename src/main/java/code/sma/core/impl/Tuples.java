package code.sma.core.impl;

import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

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
    /** points to the beginning and ends of each row in the data space, size(row_ptr)=num_row+1,
     * because we need to points the beginning and ends of user feature*/
    protected int[]   row_ptr;

    /** array of user id*/
    protected int[]   rowIndx;
    /** array of item id*/
    protected int[]   colIndx;
    /** array of labels*/
    protected float[] vals;

    public Tuples(int nnz) {
        this.row_ptr = new int[nnz + 1];
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
        assert StringUtil.isNotBlank(line) : "Line must not be blank";

        Scanner scanner = new Scanner(line);
        scanner.skip("^(\\d+\\s+){1}");
        this.num_global += scanner.nextInt();
        this.num_ufactor += scanner.nextInt();
        this.num_ifactor += scanner.nextInt();

        scanner.useDelimiter(":+|\\s+");

        int uId = scanner.nextInt();
        scanner.nextFloat();

        while (scanner.hasNextInt()) {
            rowIndx[num_val] = uId;
            colIndx[num_val] = scanner.nextInt();
            vals[num_val] = scanner.nextFloat();
            num_val++;
        }
        row_ptr[num_row + 1] = num_val;

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
        Iter() {
            super();
        }

        /** 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            assert num_row != 0 : "The data should be loaded by using loadNext()!";

            return cursor != num_row;
        }

        /** 
         * @see java.util.Iterator#next()
         */
        @Override
        public DataElem next() {
            assert row_ptr[cursor
                           + 1] > row_ptr[cursor] : "The number of item features in each row should be more than one";

            int num_factor = row_ptr[cursor + 1] - row_ptr[cursor];

            e.getIndex_user().setIntPtr(rowIndx);
            e.getIndex_user().setPtr_offset(row_ptr[cursor]);
            e.getIndex_user().setNum_factors(num_factor);

            e.getIndex_item().setIntPtr(colIndx);
            e.getIndex_item().setPtr_offset(row_ptr[cursor]);
            e.getIndex_item().setNum_factors(num_factor);

            e.getValue_ifactor().setFloatPtr(vals);
            e.getValue_ifactor().setPtr_offset(row_ptr[cursor]);
            e.getValue_ifactor().setNum_factors(num_factor);
            e.setNum_ifacotr((short) num_factor);

            cursor++;
            return e;
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_global()
         */
        @Override
        public int get_num_global() {
            return 0;
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_ufactor()
         */
        @Override
        public int get_num_ufactor() {
            return num_ufactor;
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_ifactor()
         */
        @Override
        public int get_num_ifactor() {
            return num_ifactor;
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

}
