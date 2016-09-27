package code.sma.main;

import java.util.Properties;

import code.sma.datastructure.DenseVector;

/**
 * 
 * @author Chao.Chen
 * @version $Id: Configures.java, v 0.1 2016年9月27日 下午2:21:47 Chao.Chen Exp $
 */
public final class Configures extends Properties {
    /**  serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * add computational parameters
     * 
     * @param key   the key name of the parameters
     * @param val   the values of the parameters
     */
    public void setVector(String key, DenseVector val) {
        put(key, val);
    }

    /**
     * get the computational parameters
     * 
     * @param key   the key name
     * @return
     */
    public DenseVector getVector(String key) {
        return (DenseVector) get(key);
    }

}
