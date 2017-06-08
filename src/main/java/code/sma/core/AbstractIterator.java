package code.sma.core;

import java.util.Iterator;

import code.sma.core.impl.CRefVector;

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
        e.setIndex_global(new CRefVector((int[]) null, 0, 0));
        e.setValue_global(new CRefVector((float[]) null, 0, 0));

        e.setIndex_user(new CRefVector((int[]) null, 0, 0));
        e.setValue_ufactor(new CRefVector((float[]) null, 0, 0));

        e.setIndex_item(new CRefVector((int[]) null, 0, 0));
        e.setValue_ifactor(new CRefVector((float[]) null, 0, 0));
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

}
