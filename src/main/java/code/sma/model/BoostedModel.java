package code.sma.model;

import java.util.ArrayList;
import java.util.List;

import code.sma.core.DataElem;
import code.sma.main.Configures;

/**
 * 
 * @author Chao.Chen
 * @version $Id: BoostedModel.java, Jul 3, 2017 4:16:24 PM$
 */
public class BoostedModel extends AbstractModel {
    private static final long   serialVersionUID = 1L;

    /** boosters*/
    private List<AbstractModel> boosters;
    /** weight for each booster*/
    private double              learningRate;

    /**
     * @param conf
     */
    public BoostedModel(Configures conf) {
        super(conf);
        boosters = new ArrayList<AbstractModel>();
        learningRate = conf.getDouble("LEARNING_RATE_VALUE");
    }

    /** 
     * @see code.sma.model.Model#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        double prediction = 0.0d;
        for (AbstractModel bster : boosters) {
            prediction += learningRate * bster.predict(u, i);
        }

        return Math.max(minValue, Math.min(prediction, maxValue));
    }

    /** 
     * @see code.sma.model.Model#predict(code.sma.core.DataElem)
     */
    @Override
    public double predict(DataElem e) {
        double prediction = 0.0d;
        for (AbstractModel bster : boosters) {
            prediction += learningRate * bster.predict(e);
        }

        return Math.max(minValue, Math.min(prediction, maxValue));
    }

    public void add(AbstractModel booster) {
        boosters.add(booster);
    }
}
