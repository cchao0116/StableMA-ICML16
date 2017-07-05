package code.sma.recmmd.fb.gbm;

import java.util.Map;

import org.apache.log4j.Logger;

import code.sma.core.AbstractMatrix;
import code.sma.main.Configures;
import code.sma.model.AbstractModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.RuntimeEnv;
import code.sma.util.LoggerDefineConstant;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

/**
 * Booster
 * 
 * @author Chao.Chen
 * @version $Id: Booster.java, v 0.1 Jun 29, 2017 5:37:22 PM Exp $
 */
public abstract class Booster {
    /** Runtime environment*/
    public RuntimeEnv                       runtimes;

    /** the gradients of each training data*/
    protected FloatArrayList                grad;
    /** the hessians of each training data*/
    protected FloatArrayList                hess;

    /** logger */
    protected final static transient Logger runningLogger = Logger
        .getLogger(LoggerDefineConstant.SERVICE_CORE);
    protected final static transient Logger resultLogger  = Logger
        .getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    public Booster(Configures conf, Map<String, Plugin> plugins) {
        runtimes = new RuntimeEnv(conf);
        runtimes.learningRate = conf.getDouble("BOOSTER_LR_VALUE");
        runtimes.plugins = plugins;
    }

    /**
     * train the booster
     * 
     * @param train     training data
     * @param test      testing data
     * @param grad      the gradients of each training data
     * @param hess      the hessians of each training data
     * @return      the resulting booster
     */
    public abstract AbstractModel doBoost(AbstractMatrix train, AbstractMatrix test,
                                          FloatArrayList grad, FloatArrayList hess);

}
