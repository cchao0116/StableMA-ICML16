package code.sma.main;

import code.sma.recommender.RecConfigEnv;
import code.sma.recommender.Recommender;
import code.sma.recommender.standalone.GroupSparsityMF;
import code.sma.recommender.standalone.RegularizedSVD;
import code.sma.recommender.standalone.StableMA;
import code.sma.util.StringUtil;

/**
 * stand-alone recommender algorithm Factory class
 * 
 * @author Chao.Chen
 * @version $Id: RecommenderFactory.java, v 0.1 2016年9月27日 下午1:18:06 Chao.Chen Exp $
 */
public final class StandAloneRecommenderFactory {
    private StandAloneRecommenderFactory() {
    };

    public static Recommender instance(String algName, RecConfigEnv rce) {
        int featureCount = ((Double) rce.get("FEATURE_COUNT_VALUE")).intValue();
        double lrate = (double) rce.get("LEARNING_RATE_VALUE");
        double regularized = (double) rce.get("REGULAIZED_VALUE");
        int maxIteration = ((Double) rce.get("MAX_ITERATION_VALUE")).intValue();

        int userCount = ((Double) rce.get("USER_COUNT_VALUE")).intValue();
        int itemCount = ((Double) rce.get("ITEM_COUNT_VALUE")).intValue();
        double maxValue = ((Double) rce.get("MAX_RATING_VALUE")).doubleValue();
        double minValue = ((Double) rce.get("MIN_RATING_VALUE")).doubleValue();
        boolean showProgress = (Boolean) rce.get("VERBOSE_BOOLEAN");

        if (StringUtil.equalsIgnoreCase(algName, "RegSVD")) {
            return new RegularizedSVD(userCount, itemCount, maxValue, minValue, featureCount, lrate,
                regularized, 0, maxIteration, showProgress);
        } else if (StringUtil.equalsIgnoreCase(algName, "SMA")) {
            int numHPSet = ((Double) rce.get("NUMBER_HARD_PREDICTION_SET_VALUE")).intValue();
            return new StableMA(userCount, itemCount, maxValue, minValue, featureCount, lrate,
                regularized, 0, maxIteration, numHPSet, showProgress);
        } else if (StringUtil.equalsIgnoreCase(algName, "GSMF")) {
            double alpha = (double) rce.get("ALPA");
            double beta = (double) rce.get("BETA");
            double lambda = (double) rce.get("LAMBDA");
            return new GroupSparsityMF(userCount, itemCount, maxValue, minValue, featureCount,
                alpha, beta, lambda, maxIteration, 3, showProgress);
        } else {
            return null;
        }
    }
}
