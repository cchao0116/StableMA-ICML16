package code.sma.core;

import java.util.Iterator;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractMatrix.java, v 0.1 2017年6月1日 上午10:59:13 Chao.Chen Exp $
 */
public abstract class AbstractMatrix implements Iterable<DataElem> {
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
     * get the number of non-zero entries
     * 
     * @return  the number of non-zero entries
     */
    public abstract int getnnz();

    /**
     * Returns an iterator over a set of elements with a subset of user/item features
     * 
     * @param acc_ufeature  indicator whether user feature is accessible
     * @param acc_ifeature  indicator whether item feature is accessible
     * @return  an Iterator.
     */
    public abstract Iterator<DataElem> iterator(boolean[] acc_ufeature, boolean[] acc_ifeature);

}
