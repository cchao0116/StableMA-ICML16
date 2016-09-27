package code.sma.util;

import code.sma.datastructure.SparseVector;

/**
 * the utility class to compute the distance between data point and its corresponding centroid
 * 
 * @author Hanke
 * @version $Id: DistanceUtil.java, v 0.1 2016年6月28日 上午10:23:10 Exp $
 */
public final class DistanceUtil {
    /** sine distance*/
    public final static int SINE_DISTANCE                = 201;
    /** square error*/
    public final static int SQUARE_EUCLIDEAN_DISTANCE    = 202;
    /** pearson correlation*/
    public final static int PEARSON_CORRELATION_DISTANCE = 203;
    /** KL divergence*/
    public final static int KL_DISTANCE                  = 204;
    /** KL divergence with convergence insurance*/
    public final static int KL_DISTANCE_CONVEX           = 205;
    /** I-Divergence*/
    public final static int I_DIVERGENCE                 = 501;
    /** Euclidean-Divergence*/
    public final static int EUCLIDEAN_DIVERGENCE         = 502;

    /**
     * Forbidden constructor
     */
    private DistanceUtil() {
        // Forbidden constructor
    }

    /**
     * calculate the distance between two vectors
     *  
     * @param a     given vector
     * @param b     given vector
     * @param type  the distance to compute
     * @return
     */
    public static double distance(final SparseVector a, final SparseVector centroid,
                                  final int type) {
        //check vector with all zeros
        if (a.norm() == 0 || centroid.norm() == 0) {
            return 0.0;
        }

        switch (type) {
            case SINE_DISTANCE:
                double cosine = a.innerProduct(centroid) / (a.norm() * centroid.norm());// a*b / (|a|*|b|)
                return Math.sqrt(1 - cosine * cosine);
            case SQUARE_EUCLIDEAN_DISTANCE:
                SparseVector c = a.minus(centroid);
                return c.innerProduct(c); // |a-b|
            case PEARSON_CORRELATION_DISTANCE:
                a.sub(a.average());
                centroid.sub(centroid.average());
                return a.innerProduct(centroid) / (a.norm() * centroid.norm());
            case KL_DISTANCE:
                double Dkl = 0.0d;
                for (int indx : centroid.indexList()) {
                    Dkl += centroid.getValue(indx)
                           * Math.log(centroid.getValue(indx) / a.getValue(indx));
                }
                return Dkl;
            case KL_DISTANCE_CONVEX:
                double DklCon = 0.0d;
                for (int indx : centroid.indexList()) {
                    DklCon += a.getValue(indx)
                              * Math.log(a.getValue(indx) / centroid.getValue(indx));
                }
                return DklCon;
            default:
                throw new RuntimeException("Wrong Distance Type! ");
        }
    }

    /**
     * compute the divergence
     * 
     * @param z1            the parameter to compute
     * @param z2            the parameter to compute
     * @param divergence    the divergence to compute
     * @return
     */
    public static double divergence(double z1, double z2, final int divergence) {
        switch (divergence) {
            case I_DIVERGENCE:
                return z1 * Math.log(z1 / z2) - (z1 - z2);
            case EUCLIDEAN_DIVERGENCE:
                return Math.pow(z1 - z2, 2.0);
            default:
                return 0.0;
        }
    }

}
