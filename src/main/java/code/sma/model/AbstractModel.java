package code.sma.model;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.google.common.primitives.Doubles;

import code.sma.core.DataElem;
import code.sma.plugin.Plugin;
import code.sma.util.LoggerDefineConstant;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractModel.java, v 0.1 2017年6月19日 下午2:47:37 Chao.Chen Exp $
 */
public abstract class AbstractModel implements Serializable, Plugin {
    private static final long serialVersionUID = 1L;

    public DoubleArrayList    trainErr;
    public DoubleArrayList    testErr;

    public AbstractModel() {
        this.trainErr = new DoubleArrayList();
        this.testErr = new DoubleArrayList();
    }

    /** logger */
    protected final static transient Logger runningLogger = Logger
        .getLogger(LoggerDefineConstant.SERVICE_CORE);

    /**
     * predict the rating/preference for u-th users on i-th item
     * 
     * @param u     user index
     * @param i     item index
     * @return      the predicted rating
     */
    public abstract double predict(int u, int i);

    /**
     * predict the missing label based on given features
     * 
     * @param e     user/item features
     * @return      the predicted label
     */
    public abstract double predict(DataElem e);

    public double bestTrainErr() {
        return trainErr.isEmpty() ? -1.0d : Doubles.min(trainErr.toDoubleArray());
    }

    public double bestTestErr() {
        return testErr.isEmpty() ? -1.0d : Doubles.min(testErr.toDoubleArray());
    }
}
