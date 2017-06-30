package code.sma.core;

import java.util.Iterator;

import code.sma.core.impl.CPrjRefVector;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractIterator.java, v 0.1 2017年6月2日 上午10:28:23 Chao.Chen Exp $
 */
public abstract class AbstractIterator implements Iterator<DataElem> {
    /** index of next element to return*/
    protected int      cursor;
    /** the reusable instance*/
    protected DataElem e;

    protected AbstractIterator() {
        cursor = 0;

        e = new DataElem();
        e.setIndex_global(new CPrjRefVector((int[]) null, 0, 0));
        e.setValue_global(new CPrjRefVector((float[]) null, 0, 0));

        e.setIndex_user(new CPrjRefVector((int[]) null, 0, 0));
        e.setValue_ufactor(new CPrjRefVector((float[]) null, 0, 0));

        e.setIndex_item(new CPrjRefVector((int[]) null, 0, 0));
        e.setValue_ifactor(new CPrjRefVector((float[]) null, 0, 0));
    }

    /**
     * refresh the iterator for re-use
     */
    public AbstractIterator refresh() {
        cursor = 0;
        return this;
    }

    /** 
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        throw new RuntimeException("This method has not been implemented in AbstractIterator!");
    }

    /**
     * @see java.lang.Object#clone()
     */
    public abstract AbstractIterator clone();

    /**
     * Get the number of nonzero global feature 
     * @return  number of nonzero global feature 
     */
    public abstract int get_num_global();

    /**
     * Get the number of nonzero user feature
     * @return  number of nonzero user feature 
     */
    public abstract int get_num_ufactor();

    /**
     * Get the number of nonzero item feature
     * @return  number of nonzero item feature
     */
    public abstract int get_num_ifactor();

    /**
     * Get the number of data rows
     * @return  the number of data rows
     */
    public abstract int get_num_row();
}
