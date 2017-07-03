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
    public int num_global;
    /** number of nonzero user feature */
    public int num_ufactor;
    /** number of nonzero item feature*/
    public int num_ifactor;

    /**  number of rows in the sparse matrix */
    public int num_row;
    /**  number of nonzero entries in sparse matrix */
    public int num_val;

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
     * Return a reference of a given row.
     * Make sure to use this method only for read-only purpose.
     * 
     * @param index The row index to retrieve.
     * @return A reference to the designated row.
     */
    public abstract DataElem rowRef(int i);

    /**
     * Load one-row data. For example, <br/>
     * [LABEL] #GLOBAL_FEAT #USER_FEAT #ITEM_FEAT {gIndex:gVal} {uIndex:uVal} {iIndex:iVal}
     * @param line One line containing one-row data
     */
    public abstract void loadNext(String line);

    /**
     * Returns an iterator over a set of elements with a join subset of user/item features
     * 
     * @param acc_ufeature  indicator whether user feature is accessible
     * @param acc_ifeature  indicator whether item feature is accessible
     * @return  an Iterator.
     */
    public Iterator<DataElem> iteratorJion(boolean[]... acc_feature) {
        assert acc_feature.length == 2 : "2D Tensor requires row and column accessible indicator.";
        return new Default2DMatrixIter(acc_feature[0], acc_feature[1], false);
    }

    /**
     * Returns an iterator over a set of elements with a uion subset of user/item features
     * 
     * @param acc_ufeature  indicator whether user feature is accessible
     * @param acc_ifeature  indicator whether item feature is accessible
     * @return  an Iterator.
     */
    public Iterator<DataElem> iteratorUion(boolean[]... acc_feature) {
        assert acc_feature.length == 2 : "2D Tensor requires row and column accessible indicator.";
        return new Default2DMatrixIter(acc_feature[0], acc_feature[1], true);
    }

    private final class Default2DMatrixIter extends AbstractIterator {
        private int              _num_row;
        private int              _num_gfactors;     // ABUSE: used to record #Entry of both available users and items
        private int              _num_ufactors;     // ABUSE: used to record #Entry of only available users 
        private int              _num_ifactors;     // ABUSE: used to record #Entry of only available users
        private short[][]        _prj_map;
        private AbstractIterator iter;
        private boolean[]        indicator_ufeature;
        private boolean          needUion;

        Default2DMatrixIter(boolean[] acc_ufeature, boolean[] acc_ifeature, boolean needUion) {
            this.cursor = 0;
            this._num_gfactors = 0;
            this._num_ufactors = 0;
            this._num_ifactors = 0;
            this.needUion = needUion;
            this.indicator_ufeature = new boolean[acc_ufeature.length];
            build(acc_ufeature, acc_ifeature);

        }

        public Default2DMatrixIter(int _num_row, int _num_gfactors, int _num_ufactors,
                                   int _num_ifactors, short[][] _prj_map, AbstractIterator iter,
                                   boolean[] indicator_ufeature, boolean needUion) {
            super();
            this._num_row = _num_row;
            this._num_gfactors = _num_gfactors;
            this._num_ufactors = _num_ufactors;
            this._num_ifactors = _num_ifactors;
            this._prj_map = _prj_map;
            this.iter = iter;
            this.indicator_ufeature = indicator_ufeature;
            this.needUion = needUion;
        }

        private void build(boolean[] acc_ufeature, boolean[] acc_ifeature) {
            _num_row = 0;
            _prj_map = new short[num_row][0];

            iter = (AbstractIterator) iterator();
            while (iter.hasNext()) {
                DataElem e = iter.next();
                int u = e.getIndex_user(0);
                if (!needUion && !acc_ufeature[u]) {
                    continue;
                }

                ShortArrayList shorts = new ShortArrayList();
                short _num_ifactor = e.getNum_ifacotr();
                for (short f = 0; f < _num_ifactor; f++) {
                    int i = e.getIndex_item(f);

                    if (acc_ufeature[u] && acc_ifeature[i]) {
                        _num_gfactors++;
                        _num_ufactors++;
                        _num_ifactors++;
                        shorts.add(f);
                    } else if (needUion && acc_ufeature[u]) {
                        _num_ufactors++;
                        shorts.add(f);
                    } else if (needUion && acc_ifeature[i]) {
                        _num_ifactors++;
                        shorts.add(f);
                    }
                }

                if (shorts.size() != 0) {
                    indicator_ufeature[u] = true;
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
                if (!indicator_ufeature[u]) {
                    continue;
                } else {
                    short num_ifactor = (short) _prj_map[cursor].length;
                    e.setNum_ifacotr(num_ifactor);

                    CPrjRefVector value_ifactor = (CPrjRefVector) e.getValue_ifactor();
                    value_ifactor.setPrj_mpg(_prj_map[cursor]);

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
            return _num_gfactors;
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_ufactor()
         */
        @Override
        public int get_num_ufactor() {
            return _num_ufactors;
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_ifactor()
         */
        @Override
        public int get_num_ifactor() {
            return _num_ifactors;
        }

        /** 
         * @see code.sma.core.AbstractIterator#clone()
         */
        @Override
        public AbstractIterator clone() {
            return new Default2DMatrixIter(_num_row, _num_gfactors, _num_ufactors, _num_ifactors,
                _prj_map, (AbstractIterator) iterator(), indicator_ufeature, needUion);
        }

        /** 
         * @see code.sma.core.AbstractIterator#get_num_row()
         */
        @Override
        public int get_num_row() {
            return _num_row;
        }

    }
}
