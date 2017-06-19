package code.sma.recmmd;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.main.Configures;
import code.sma.plugin.Plugin;
import code.sma.recmmd.stats.Accumulator;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * The runtime environment: <br/>
 * <b>BASICS</b>  <br/>
 * <b>ERROR:</b> round, prevErr, currErr <br/>
 * <b>CLUSTERING:</b> accessible user features (acc_ufeature), accessible item features (acc_ifeature) <br/>
 * <b>DATA:</b> training data, testing data <br/>
 * 
 * @author Chao.Chen
 * @version $Id: RuntimeEnv.java, v 0.1 2017年6月6日 下午3:00:19 Chao.Chen Exp $
 */
public final class RuntimeEnv implements Serializable {
    private static final long            serialVersionUID = 1L;

    // BASICS
    public int                           userCount;
    public int                           itemCount;
    public double                        maxValue;
    public double                        minValue;

    public int                           featureCount;
    public double                        learningRate;
    public double                        regularizer;
    public double                        momentum;
    public int                           maxIter;
    public boolean                       showProgress;
    public Loss                          lossFunction;
    public Regularizer                   regType;

    // THREAD
    public int                           threadNum;
    public int                           threadId         = 0;

    // ERROR
    public int                           round            = 0;
    public double                        prevErr          = 99999;
    public double                        currErr          = 9999;
    public double                        sumErr           = 0.0d;

    //CLUSTERING
    public boolean[]                     acc_uf_indicator;        //user feature accessible indicator
    public boolean[]                     acc_if_indicator;        //item feature accessible indicator
    public short[]                       ua_func;                 //user assign function 
    public short[]                       ia_func;                 //item assign function 

    // DATA
    public int                           nnz;
    public transient AbstractIterator    itrain;
    public transient AbstractIterator    itest;

    // Weight/Importance for each feature, e.g., rating, user, item
    public double[]                      tnWs;
    public double[][]                    ensmblUWs;
    public double[][]                    ensmblIWs;

    // Extension
    public DoubleArrayList               doubles;
    public IntArrayList                  ints;

    // ACCUMULATORS
    public transient List<Accumulator>   acumltors;

    // PLUGINS
    public transient Map<String, Plugin> plugins;

    // CONFIGURE 
    public transient Configures          conf;

    public RuntimeEnv(Configures conf) {
        this.conf = conf;

        // initiate containers
        this.doubles = new DoubleArrayList();
        this.ints = new IntArrayList();

        // load configures
        if (conf.containsKey("FEATURE_COUNT_VALUE"))
            this.featureCount = conf.getInteger("FEATURE_COUNT_VALUE");
        if (conf.containsKey("LEARNING_RATE_VALUE"))
            this.learningRate = conf.getFloat("LEARNING_RATE_VALUE");
        if (conf.containsKey("REGULAIZED_VALUE"))
            this.regularizer = conf.getFloat("REGULAIZED_VALUE");
        if (conf.containsKey("MAX_ITERATION_VALUE"))
            this.maxIter = conf.getInteger("MAX_ITERATION_VALUE");

        this.userCount = conf.getInteger("USER_COUNT_VALUE");
        this.itemCount = conf.getInteger("ITEM_COUNT_VALUE");
        this.maxValue = conf.getFloat("MAX_RATING_VALUE");
        this.minValue = conf.getFloat("MIN_RATING_VALUE");
        this.showProgress = conf.getBoolean("VERBOSE_BOOLEAN");

        this.threadNum = conf.getInteger("THREAD_NUMBER_VALUE");

        this.lossFunction = conf.contains("LOSS_FUNCTION")
            ? Loss.valueOf(conf.getProperty("LOSS_FUNCTION")) : Loss.LOSS_RMSE;
        this.regType = conf.contains("REG_TYPE") ? Regularizer.valueOf(conf.getProperty("REG_TYPE"))
            : Regularizer.L2;
    }

    public String briefDesc() {
        return String.format("[%d]_[%d]_[%d]", featureCount, Math.round(learningRate * 1000),
            Math.round(regularizer * 1000));
    }
}
