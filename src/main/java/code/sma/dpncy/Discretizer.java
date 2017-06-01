package code.sma.dpncy;

import code.sma.core.impl.Tuples;

/**
 * convert continuous values into discrete values   
 * 
 * @author Chao.Chen
 * @version $Id: Discretizer.java, v 0.1 2016年9月26日 下午3:33:09 Chao.Chen Exp $
 */
public abstract class Discretizer {

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
     * @param tnMatrix            the training data
     * @param invlvIndces         involved data indices
     * @return
     */
    public abstract double[] cmpTrainWs(Tuples tnMatrix, int[] invlvIndces);

    /**
     * compute the ensemble weights using in WEMAREC
     * 
     * @param tnMatrix           the training data   
     * @param invlvIndces        involved data indices
     * @return
     */
    public abstract double[][][] cmpEnsmblWs(Tuples tnMatrix, int[] invlvIndces);
}
