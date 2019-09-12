package code.sma.recmmd;

import org.apache.log4j.Logger;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.model.AbstractModel;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.SerializeUtil;

/**
 * Abstract Class for Recommender Model
 * 
 * @author Chao Chen
 * @version $Id: Recommender.java, v 0.1 2015-6-8 下午6:59:37 Exp $
 */
public abstract class Recommender {
    /** Runtime environment*/
    public RuntimeEnv                       runtimes;

    /** logger */
    protected final static transient Logger runningLogger = Logger
        .getLogger(LoggerDefineConstant.SERVICE_CORE);
    protected final static transient Logger resultLogger  = Logger
        .getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /*========================================
     * Model Builder
     *========================================*/
    /**
     * Build a model with given training set.
     * 
     * @param train     training data
     * @param test      test data
     */
    public void buildModel(AbstractMatrix train, AbstractMatrix test) {
        LoggerUtil.info(runningLogger, this);

        // prepare runtime environment
        prepare_runtimes(train, test);

        // update model
        AbstractIterator iDataElem = runtimes.itrain;
        while (runtimes.round < runtimes.maxIter) {
            update_inner(iDataElem);
        }
    }

    /**
     * initial trainer and prepare essentials for the model trainer
     * 
     * @param train     training data
     * @param test      test data
     */
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
    };

    /**
     * major component to update the model
     * 
     * @param iDataElem the data iterator
     */
    protected void update_inner(AbstractIterator iDataElem) {
        // prepare runtime environment
        start_round();

        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            update_each(e);
        }

        // update runtime environment
        finish_round();
    };

    /**
     * prepare runtime environment
     */
    protected void start_round() {

    }

    /**
     * update based on one-user's 
     * 
     * @param e user-grouped data, i.e., one-user's data
     */
    protected void update_each(DataElem e) {
    }

    /**
     * update runtime environment 
     */
    protected void finish_round() {
    }

    /**
     * save model
     * 
     * @param fo    the output file
     */
    public void saveModel(String fo) {
        SerializeUtil.writeObject(getModel(), fo);
    }

    /**
     * load model 
     * 
     * @param fi    the input file
     */
    public abstract void loadModel(String fi);

    /**
     * get resulting model
     * 
     * @return  the resulting model
     */
    public abstract AbstractModel getModel();

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

}
