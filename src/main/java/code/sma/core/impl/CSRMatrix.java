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
 * storage block of random order input, 
 * in CSR format for sparse matrix, multiple lines are stored together in a block
 * 
 * @author Chao.Chen
 * @version $Id: CSRMatrix.java, v 0.1 2017年6月1日 下午2:48:10 Chao.Chen Exp $
 */
public class CSRMatrix extends AbstractMatrix {
    /**  number of rows in the sparse matrix */
    protected int     num_row;
    /**  number of nonzero entries in sparse matrix */
    protected int     num_val;

    /** label of each row */
    protected float[] row_label;
    // note: features is stored in order, global, ufactor, ifactor
    /** points to the beginning and ends of each row in the data space, size(row_ptr)=3*num_row+1,
      * because we need to points the beginning and ends of global feature, user feature and item feature*/
    protected int[]   row_ptr;
    /** array of the indices, i.e., uIDs*/
    protected int[]   feat_index;
    /** array of the  features*/
    protected float[] feat_value;

    /** 
     * @see code.sma.core.AbstractMatrix#loadNext(java.lang.String)
     */
    @Override
    public void loadNext(String line) {
        Assert.assertTrue("Line must not be blank", StringUtil.isNotBlank(line));
        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(":+|\\s+");

        row_label[num_row] = scanner.nextFloat();
        row_ptr[3 * num_row + 1] = row_ptr[3 * num_row] + scanner.nextInt();
        row_ptr[3 * num_row + 2] = row_ptr[3 * num_row + 1] + scanner.nextInt();
        row_ptr[3 * num_row + 3] = row_ptr[3 * num_row + 2] + scanner.nextInt();

        while (scanner.hasNextInt()) {
            feat_index[num_val] = scanner.nextInt();
            feat_value[num_val] = scanner.nextFloat();
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
        protected int cursor = 0;

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
            DataElem e = new DataElem(row_label[cursor]);

            short num_global = (short) (row_ptr[3 * cursor + 1] - row_ptr[3 * cursor]);
            short num_ufactor = (short) (row_ptr[3 * cursor + 2] - row_ptr[3 * cursor - 1]);
            short num_ifacotr = (short) (row_ptr[3 * cursor + 3] - row_ptr[3 * cursor - 2]);
            e.setNum_global(num_global);
            e.setNum_ufactor(num_ufactor);
            e.setNum_ifacotr(num_ifacotr);

            e.setIndex_global(Arrays.copyOfRange(feat_index, row_ptr[3 * cursor + 0],
                row_ptr[3 * cursor + 0] + num_ifacotr));
            e.setValue_global(Arrays.copyOfRange(feat_value, row_ptr[3 * cursor + 0],
                row_ptr[3 * cursor + 0] + num_ifacotr));

            e.setIndex_user(Arrays.copyOfRange(feat_index, row_ptr[3 * cursor + 1],
                row_ptr[3 * cursor + 1] + num_ifacotr));
            e.setValue_ufactor(Arrays.copyOfRange(feat_value, row_ptr[3 * cursor + 1],
                row_ptr[3 * cursor + 1] + num_ifacotr));

            e.setIndex_item(Arrays.copyOfRange(feat_index, row_ptr[3 * cursor + 2],
                row_ptr[3 * cursor + 2] + num_ifacotr));
            e.setValue_ifactor(Arrays.copyOfRange(feat_value, row_ptr[3 * cursor + 2],
                row_ptr[3 * cursor + 2] + num_ifacotr));

            cursor++;
            return null;
        }

    }

    /** 
     * @see code.sma.core.AbstractMatrix#getValue(int, int)
     */
    @Override
    public double getValue(int i, int j) {
        throw new RuntimeException("This method has not been implemented in CSRMatrix!");
    }

    /** 
     * @see code.sma.core.AbstractMatrix#setValue(int, int, double)
     */
    @Override
    public void setValue(int i, int j, double value) {
        throw new RuntimeException("This method has not been implemented in CSRMatrix!");
    }

}
