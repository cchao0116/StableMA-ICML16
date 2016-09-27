package code.sma.depndncy;

import code.sma.datastructure.MatlabFasionSparseMatrix;

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
     * @param tnMatrix            the 
     * @param invlvIndces
     * @return
     */
    public abstract double[] cmpTrainWs(MatlabFasionSparseMatrix tnMatrix, int[] invlvIndces);

    /**
     * compute the ensemble weights using in WEMAREC
     * 
     * @param ttMatrix
     * @param invlvIndces
     * @return
     */
    public abstract double[][][] cmpEnsmblWs(MatlabFasionSparseMatrix ttMatrix, int[] invlvIndces);
}
