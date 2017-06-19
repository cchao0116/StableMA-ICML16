package code.sma.plugin;

import org.apache.commons.math3.stat.StatUtils;

import code.sma.core.AbstractIterator;
import code.sma.core.DataElem;
import code.sma.main.Configures;

/**
 * Convert the rating data in Netflix and Movielens into continuous discrete
 * data e.g, 0.5, 1.0, ..., 5.0 => 0, 1, ..., 9
 * 
 * @author Chao.Chen
 * @version $Id: NetflixMovieLensDiscretizer.java, v 0.1 2016年9月28日 下午1:34:51
 *          Chao.Chen Exp $
 */
public class NetflixMovieLensDiscretizer extends Discretizer {
    /** The number of users. */
    private int    userCount;
    /** The number of items. */
    private int    itemCount;
    /** minimum value */
    private double minValue;
    /** maximum value */
    private double maxValue;

    public NetflixMovieLensDiscretizer(Configures conf) {
        super();
        this.userCount = conf.getInteger("USER_COUNT_VALUE");
        this.itemCount = conf.getInteger("ITEM_COUNT_VALUE");
        this.maxValue = conf.getDouble("MAX_RATING_VALUE");
        this.minValue = conf.getDouble("MIN_RATING_VALUE");
    }

    /**
     * @see code.sma.plugin.Discretizer#convert(double)
     */
    @Override
    public int convert(double val) {
        return (int) ((val - minValue) / minValue);
    }

    /**
     * @see code.sma.plugin.Discretizer#cmpTrainWs(code.sma.core.impl.Tuples,
     *      int[])
     */
    @Override
    public double[] cmpTrainWs(AbstractIterator iter) {
        iter.refresh();

        int tnS = (int) (maxValue / minValue);
        double[] tnWs = new double[tnS];

        int nnz = 0;
        while (iter.hasNext()) {
            DataElem e = iter.next();
            short num_ifactor = e.getNum_ifacotr();

            for (int f = 0; f < num_ifactor; f++) {
                double AuiReal = e.getValue_ifactor(f);
                tnWs[convert(AuiReal)]++;
                nnz++;
            }
        }

        // every entry plus 1 to avoid zero condition
        for (int t = 0; t < tnS; t++) {
            tnWs[t] = (tnWs[t] + 1) / (nnz + tnS);
        }
        return tnWs;
    }

    /**
     * @see code.sma.plugin.Discretizer#cmpEnsmblWs(code.sma.core.impl.Tuples,
     *      int[])
     */
    @Override
    public double[][][] cmpEnsmblWs(AbstractIterator iter) {
        iter.refresh();

        int tnS = (int) (maxValue / minValue);
        double[][][] emWs = new double[2][0][0];
        emWs[0] = new double[userCount][tnS]; // user-related weights
        emWs[1] = new double[itemCount][tnS]; // item-related weights

        while (iter.hasNext()) {
            DataElem e = iter.next();
            short num_ifactor = e.getNum_ifacotr();

            int u = e.getIndex_user(0);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);
                double AuiReal = e.getValue_ifactor(f);

                int label = convert(AuiReal);
                emWs[0][u][label]++;
                emWs[1][i][label]++;
            }
        }

        // user side
        for (int uId = 0; uId < userCount; uId++) {
            double sum = StatUtils.sum(emWs[0][uId]);
            if (sum == 0.0d) {
                continue;
            }

            for (int t = 0; t < tnS; t++) {
                emWs[0][uId][t] = (emWs[0][uId][t] + 1) / (sum + tnS);
            }
        }

        for (int iId = 0; iId < itemCount; iId++) {
            double sum = StatUtils.sum(emWs[1][iId]);
            if (sum == 0.0d) {
                continue;
            }

            for (int t = 0; t < tnS; t++) {
                emWs[1][iId][t] = (emWs[1][iId][t] + 1) / (sum + tnS);
            }
        }
        return emWs;
    }

}
