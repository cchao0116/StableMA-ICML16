package code.sma.core;

import java.util.Iterator;

import code.sma.core.impl.CPrjRefVector;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractMatrix.java, v 0.1 2017年6月1日 上午10:59:13 Chao.Chen Exp $
 */
public abstract class AbstractMatrix implements Iterable<DataElem> {
    /** number of nonzero global feature */
    public int    num_global;
    /** number of nonzero user feature */
    public int    num_ufactor;
    /** number of nonzero item feature*/
    public int    num_ifactor;

    /**  number of rows in the sparse matrix */
    protected int num_row;
    /**  number of nonzero entries in sparse matrix */
    protected int num_val;

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The row index to retrieve.
     * @param j The column index to retrieve.
     * @return The value stored at the given index.
     */
    public abstract double getValue(int i, int j);

    /**
     * Set a new value at the given index.
     * 
     * @param i The row index to store new value.
     * @param j The column index to store new value.
     * @param value The value to store.
     */
    public abstract void setValue(int i, int j, double value);

    /**
     * Load one-row data. For example, <br/>
     * [LABEL] #GLOBAL_FEAT #USER_FEAT #ITEM_FEAT {gIndex:gVal} {uIndex:uVal} {iIndex:iVal}
     * @param line One line containing one-row data
     */
    public abstract void loadNext(String line);

    /**
     * Returns an iterator over a set of elements with a subset of user/item features
     * 
     * @param acc_ufeature  indicator whether user feature is accessible
     * @param acc_ifeature  indicator whether item feature is accessible
     * @return  an Iterator.
     */
    public Iterator<DataElem> iterator(boolean[]... acc_feature) {
        assert acc_feature.length == 2 : "2D Tensor requires row and column accessible indicator.";
        return new Default2DMatrixIter(acc_feature[0], acc_feature[1]);
    }

    private final class Default2DMatrixIter extends AbstractIterator {
        private int              _num_row;
        private int              _num_ifactors;
        private short[][]        _prj_map;
        private AbstractIterator iter;
        private boolean[]        acc_ufeature;

        Default2DMatrixIter(boolean[] acc_ufeature, boolean[] acc_ifeature) {
            this.cursor = 0;
            build(acc_ufeature, acc_ifeature);
        }

        private void build(boolean[] acc_ufeature, boolean[] acc_ifeature) {
            _num_row = 0;
            _prj_map = new short[num_row][0];
            this.acc_ufeature = acc_ufeature;

            iter = (AbstractIterator) iterator();
            while (iter.hasNext()) {
                DataElem e = iter.next();
                int u = e.getIndex_user(0);
                if (!acc_ufeature[u]) {
                    continue;
                }

                ShortArrayList shorts = new ShortArrayList();
                short _num_ifactor = e.getNum_ifacotr();
                for (short f = 0; f < _num_ifactor; f++) {
                    int i = e.getIndex_item(f);
                    if (!acc_ifeature[i]) {
                        continue;
                    }

                    shorts.add(f);
                    _num_ifactors++;
                }

                if (shorts.size() != 0) {
                    _prj_map[_num_row] = shorts.toShortArray();
                    _num_row++;
                }
            }
        }

        /** 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return cursor != _num_row;
        }

        /** 
         * @see java.util.Iterator#next()
         */
        @Override
        public DataElem next() {

            while (iter.hasNext()) {
                e = iter.next();
                int u = e.getIndex_user(0);
                if (!acc_ufeature[u]) {
                    continue;
                } else {
                    short num_ifactor = (short) _prj_map[cursor].length;
                    e.setNum_ifacotr(num_ifactor);

                    CPrjRefVector value_ufactor = (CPrjRefVector) e.getValue_ufactor();
                    value_ufactor.setPrj_mpg(_prj_map[cursor]);

                    CPrjRefVector index_item = (CPrjRefVector) e.getIndex_item();
                    index_item.setPrj_mpg(_prj_map[cursor]);

                    break;
                }
            }

            cursor++;
            return e;
        }

        /** 
         * @see code.sma.core.AbstractIterator#refresh()
         */
        @Override
        public AbstractIterator refresh() {
            cursor = 0;
            iter.refresh();
            return this;
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
            return _num_row;
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_ifactor()
         */
        @Override
        public int get_num_ifactor() {
            return _num_ifactors;
        }

    }
}
