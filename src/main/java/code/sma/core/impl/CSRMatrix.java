package code.sma.core.impl;

import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

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

    /** label of each row */
    protected float[] row_label;
    // note: features is stored in order, global, ufactor, ifactor
    /** points to the beginning and ends of each row in the data space, size(row_ptr)=3*num_row+1,
      * because we need to points the beginning and ends of global feature, user feature and item feature*/
    protected int[]   row_ptr;
    /** array of the indices*/
    protected int[]   feat_index;
    /** array of the  features*/
    protected float[] feat_value;

    public CSRMatrix(int num_row, int num_val) {
        row_label = new float[num_row];
        row_ptr = new int[3 * num_row + 1];

        feat_index = new int[num_val];
        feat_value = new float[num_val];
    }

    /** 
     * @see code.sma.core.AbstractMatrix#loadNext(java.lang.String)
     */
    @Override
    public void loadNext(String line) {
        assert StringUtil.isNotBlank(line) : "Line must not be blank";

        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(":+|\\s+");

        row_label[num_row] = scanner.nextFloat();

        int num = scanner.nextInt();
        this.num_global += num;
        row_ptr[3 * num_row + 1] = row_ptr[3 * num_row] + num;

        num = scanner.nextInt();
        this.num_ufactor += num;
        row_ptr[3 * num_row + 2] = row_ptr[3 * num_row + 1] + num;

        num = scanner.nextInt();
        this.num_ifactor += num;
        row_ptr[3 * num_row + 3] = row_ptr[3 * num_row + 2] + num;

        while (scanner.hasNextInt()) {
            feat_index[num_val] = scanner.nextInt();
            feat_value[num_val] = scanner.nextFloat();
            num_val++;
        }

        num_row++;
        IOUtils.closeQuietly(scanner);
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

    /** 
     * @see code.sma.core.AbstractMatrix#rowRef(int)
     */
    @Override
    public DataElem rowRef(int i) {
        assert i >= 0 && i <= num_row : String.format("Parameter i=%d should be in [0, %d)", i,
            num_row);

        int cursor = i;

        DataElem e = new DataElem();
        e.setIndex_global(new CRefVector((int[]) null, 0, 0));
        e.setValue_global(new CRefVector((float[]) null, 0, 0));

        e.setIndex_user(new CRefVector((int[]) null, 0, 0));
        e.setValue_ufactor(new CRefVector((float[]) null, 0, 0));

        e.setIndex_item(new CRefVector((int[]) null, 0, 0));
        e.setValue_ifactor(new CRefVector((float[]) null, 0, 0));
        e.setLabel(row_label[cursor]);

        short num_global = (short) (row_ptr[3 * cursor + 1] - row_ptr[3 * cursor]);
        short num_ufactor = (short) (row_ptr[3 * cursor + 2] - row_ptr[3 * cursor + 1]);
        short num_ifactor = (short) (row_ptr[3 * cursor + 3] - row_ptr[3 * cursor + 2]);
        e.setNum_global(num_global);
        e.setNum_ufactor(num_ufactor);
        e.setNum_ifacotr(num_ifactor);

        CRefVector index_global = e.getIndex_global();
        index_global.setIntPtr(feat_index);
        index_global.setPtr_offset(row_ptr[3 * cursor + 0]);
        index_global.setNum_factors(num_global);

        CRefVector value_global = e.getValue_global();
        value_global.setFloatPtr(feat_value);
        value_global.setPtr_offset(row_ptr[3 * cursor + 0]);
        value_global.setNum_factors(num_global);

        CRefVector index_user = e.getIndex_user();
        index_user.setIntPtr(feat_index);
        index_user.setPtr_offset(row_ptr[3 * cursor + 1]);
        index_user.setNum_factors(num_ufactor);

        CRefVector value_ufactor = e.getValue_ufactor();
        value_ufactor.setFloatPtr(feat_value);
        value_ufactor.setPtr_offset(row_ptr[3 * cursor + 1]);
        value_ufactor.setNum_factors(num_ufactor);

        CRefVector index_item = e.getIndex_item();
        index_item.setIntPtr(feat_index);
        index_item.setPtr_offset(row_ptr[3 * cursor + 2]);
        index_item.setNum_factors(num_ifactor);

        CRefVector value_ifactor = e.getValue_ifactor();
        value_ifactor.setFloatPtr(feat_value);
        value_ifactor.setPtr_offset(row_ptr[3 * cursor + 2]);
        value_ifactor.setNum_factors(num_ifactor);
        return e;
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
            return cursor != num_row;
        }

        /** 
         * @see java.util.Iterator#next()
         */
        @Override
        public DataElem next() {
            e.setLabel(row_label[cursor]);

            short num_global = (short) (row_ptr[3 * cursor + 1] - row_ptr[3 * cursor]);
            short num_ufactor = (short) (row_ptr[3 * cursor + 2] - row_ptr[3 * cursor + 1]);
            short num_ifactor = (short) (row_ptr[3 * cursor + 3] - row_ptr[3 * cursor + 2]);
            e.setNum_global(num_global);
            e.setNum_ufactor(num_ufactor);
            e.setNum_ifacotr(num_ifactor);

            CRefVector index_global = e.getIndex_global();
            index_global.setIntPtr(feat_index);
            index_global.setPtr_offset(row_ptr[3 * cursor + 0]);
            index_global.setNum_factors(num_global);

            CRefVector value_global = e.getValue_global();
            value_global.setFloatPtr(feat_value);
            value_global.setPtr_offset(row_ptr[3 * cursor + 0]);
            value_global.setNum_factors(num_global);

            CRefVector index_user = e.getIndex_user();
            index_user.setIntPtr(feat_index);
            index_user.setPtr_offset(row_ptr[3 * cursor + 1]);
            index_user.setNum_factors(num_ufactor);

            CRefVector value_ufactor = e.getValue_ufactor();
            value_ufactor.setFloatPtr(feat_value);
            value_ufactor.setPtr_offset(row_ptr[3 * cursor + 1]);
            value_ufactor.setNum_factors(num_ufactor);

            CRefVector index_item = e.getIndex_item();
            index_item.setIntPtr(feat_index);
            index_item.setPtr_offset(row_ptr[3 * cursor + 2]);
            index_item.setNum_factors(num_ifactor);

            CRefVector value_ifactor = e.getValue_ifactor();
            value_ifactor.setFloatPtr(feat_value);
            value_ifactor.setPtr_offset(row_ptr[3 * cursor + 2]);
            value_ifactor.setNum_factors(num_ifactor);

            cursor++;
            return e;
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_global()
         */
        @Override
        public int get_num_global() {
            return num_global;
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

        /** 
         * @see code.sma.core.AbstractIterator#clone()
         */
        @Override
        public AbstractIterator clone() {
            return new Iter();
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_row()
         */
        @Override
        public int get_num_row() {
            return num_row;
        }

    }

}
