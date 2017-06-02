package code.sma.core;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractMatrix.java, v 0.1 2017年6月1日 上午10:59:13 Chao.Chen Exp $
 */
public abstract class AbstractMatrix implements Iterable<DataElem> {

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

}
