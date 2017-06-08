package code.sma.plugin;

import code.sma.core.AbstractIterator;

/**
 * convert continuous values into discrete values   
 * 
 * @author Chao.Chen
 * @version $Id: Discretizer.java, v 0.1 2016年9月26日 下午3:33:09 Chao.Chen Exp $
 */
public abstract class Discretizer implements Plugin {

    /**
     * convert continuous values into discrete values
     * 
     * @param val continous values
     * @return discrete values
     */
    public abstract int convert(double val);

    /**
     * compute the training weights using in WEMAREC
     * 
     * @param iter            the iterator of data
     * @return
     */
    public abstract double[] cmpTrainWs(AbstractIterator iter);

    /**
     * compute the ensemble weights using in WEMAREC
     * 
     * @param iter           the iterator of data  
     * @return
     */
    public abstract double[][][] cmpEnsmblWs(AbstractIterator iter);
}
