package code.sma.recommender;

import java.util.HashMap;
import java.util.Map;

import code.sma.main.Configures;

/**
 * A bean containing different hyper-parameters
 * 
 * @author Chao.Chen
 * @version $Id: RecConfigEnv.java, v 0.1 2016年9月27日 下午12:53:28 Chao.Chen Exp $
 */
public final class RecConfigEnv {
    /** parameter repository*/
    Map<String, Object> parameters;

    /**
     * construction
     */
    public RecConfigEnv() {
        parameters = new HashMap<String, Object>();
    }

    /**
     * construction
     */
    public RecConfigEnv(Configures conf) {
        parameters = new HashMap<String, Object>();
        for (Object k : conf.keySet()) {
            String key = (String) k;
            put(key, conf.get(key));
        }
    }

    /**
     * Returns the value to which the specified key is mapped
     * 
     * @param key the key whose associated value is to be returned
     * @return
     */
    public Object get(String key) {
        return parameters.get(key);
    }

    /**
     * Associates the specified value with the specified key in this map
     * 
     * @param key the key whose associated value is to be returned
     * @param val value to be associated with the specified key
     */
    public void put(String key, Object val) {
        parameters.put(key, val);
    }
}
