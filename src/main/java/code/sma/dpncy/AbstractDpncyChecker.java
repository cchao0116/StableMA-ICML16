package code.sma.dpncy;

import org.apache.log4j.Logger;

import code.sma.main.Configures;
import code.sma.util.LoggerDefineConstant;

/**
 * To check the dependencies for every algorithm
 * 
 * @author Chao.Chen
 * @version $Id: AbstractDpncyChecker.java, v 0.1 2016年9月28日 上午11:06:23 Chao.Chen Exp $
 */
public abstract class AbstractDpncyChecker {
    protected final static Logger  normalLogger = Logger
        .getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /** the chain of responsibility*/
    protected AbstractDpncyChecker successor;

    /**
     * check the dependency
     * 
     * @param conf  algorithm configure
     */
    public abstract void handler(Configures conf);

    /**
     * Getter method for property <tt>successor</tt>.
     * 
     * @return property value of successor
     */
    public AbstractDpncyChecker getSuccessor() {
        return successor;
    }

    /**
     * Setter method for property <tt>successor</tt>.
     * 
     * @param successor value to be assigned to property successor
     */
    public void setSuccessor(AbstractDpncyChecker successor) {
        this.successor = successor;
    }

}
