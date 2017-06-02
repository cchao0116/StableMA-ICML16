package code.sma.core;

import java.util.Iterator;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractIterator.java, v 0.1 2017年6月2日 上午10:28:23 Chao.Chen Exp $
 */
public abstract class AbstractIterator implements Iterator<DataElem> {
    /** index of next element to return*/
    protected int cursor = 0;

    /**
     * refresh the iterator for re-use
     */
    public void refresh() {
        cursor = 0;
    }

    /** 
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        throw new RuntimeException("This method has not been implemented in AbstractIterator!");
    }
}
