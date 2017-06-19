package code.sma.model;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.google.common.primitives.Doubles;

import code.sma.main.Configures;
import code.sma.plugin.Plugin;
import code.sma.util.LoggerDefineConstant;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractModel.java, v 0.1 2017年6月19日 下午2:47:37 Chao.Chen Exp $
 */
public abstract class AbstractModel implements Serializable, Model, Plugin {
    private static final long serialVersionUID = 1L;

    protected double          maxValue;
    protected double          minValue;
    protected double          defaultValue;

    public DoubleArrayList    trainErr;
    public DoubleArrayList    testErr;

    protected AbstractModel(Configures conf) {
        this.maxValue = conf.getFloat("MAX_RATING_VALUE");
        this.minValue = conf.getFloat("MIN_RATING_VALUE");
        this.defaultValue = (maxValue + minValue) / 2.0;

        this.trainErr = new DoubleArrayList();
        this.testErr = new DoubleArrayList();
    }

    /** logger */
    protected final static transient Logger runningLogger = Logger
        .getLogger(LoggerDefineConstant.SERVICE_CORE);

    public double bestTrainErr() {
        return trainErr.isEmpty() ? -1.0d : Doubles.min(trainErr.toDoubleArray());
    }

    public double bestTestErr() {
        return testErr.isEmpty() ? -1.0d : Doubles.min(testErr.toDoubleArray());
    }
}
