package code.sma.recommender;

/**
 * Loss function
 * 
 * @author Chao.Chen
 * @version $Id: Loss.java, v 0.1 2016年9月29日 下午12:47:44 Chao.Chen Exp $
 */
public enum Loss {
                  LOSS_RMSE, //
                  LOSS_LOG, //
                  LOSS_EXP, //
                  LOSS_HINGE;//

    /**
     * compute the difference between two values
     * 
     * @param realVal    real value
     * @param predVal    predicted value
     * @return      the difference
     */
    public double diff(double realVal, double predVal) {
        switch (this) {
            case LOSS_RMSE:
                return Math.pow(realVal - predVal, 2.0d);
            case LOSS_LOG:
                return Math.log(1 + Math.exp(-1 * realVal * predVal));
            case LOSS_EXP:
                return Math.exp(-1 * realVal * predVal);
            case LOSS_HINGE:
                return realVal - predVal < 0 ? 1 - realVal * predVal : 0.0d;
            default:
                return 0.0d;
        }
    }

    /**
     * compute the derivative of the difference with respect to the predicted value
     * 
     * @param realVal    real value
     * @param predVal    predicted value
     * @return
     */
    public double dervWRTPrdctn(double realVal, double predVal) {
        switch (this) {
            case LOSS_RMSE:
                return realVal - predVal;
            case LOSS_LOG:
                return -realVal / (1 + Math.exp(-1 * realVal * predVal));
            case LOSS_EXP:
                return -realVal * Math.exp(-1 * realVal * predVal);
            case LOSS_HINGE:
                return realVal - predVal < 0 ? -realVal : 0.0d;
            default:
                return 0.0d;
        }
    }
}
