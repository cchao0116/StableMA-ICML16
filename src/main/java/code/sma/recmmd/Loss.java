package code.sma.recmmd;

/**
 * Loss function
 * 
 * @author Chao.Chen
 * @version $Id: Loss.java, v 0.1 2016年9月29日 下午12:47:44 Chao.Chen Exp $
 */
public enum Loss {
                  LOSS_RMSE, //  ROOT MEAN SQUARE ERROR
                  LOSS_LOG, //   LOGISTIC ERROR
                  LOSS_EXP, //   EXPONENTIAL ERROR
                  LOSS_HINGE;//  HINGE LOSS

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
                return realVal * predVal < 1 ? 1 - realVal * predVal : 0.0d;
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
                return -realVal + predVal;
            case LOSS_LOG:
                return -realVal * Math.exp(-1 * realVal * predVal)
                       / (1 + Math.exp(-1 * realVal * predVal));
            case LOSS_EXP:
                return -realVal * Math.exp(-1 * realVal * predVal);
            case LOSS_HINGE:
                return realVal * predVal < 1 ? -realVal : 0.0d;
            default:
                return 0.0d;
        }
    }
}
