package code.sma.clustering;

import code.sma.core.impl.SparseVector;

/**
 * measure distance between two data point
 * 
 * @author Chao.Chen
 * @version $Id: Distance.java, v 0.1 2016年9月28日 下午12:32:04 Chao.Chen Exp $
 */
public enum Distance {
                      IW, //I_DIVERGENCE  
                      CR, //PEARSON_CORRELATION_DISTANCE,
                      EW, //Euclidean_Distance 
                      SI, //SINE_DISTANCE
                      KL; //KL_DISTANCE_CONVEX

    /**
     * calculate the distance between two vectors
     *  
     * @param a     given vector
     * @param b     given vector
     * @return
     */
    public double distance(final SparseVector a, final SparseVector centroid) {
        //check vector with all zeros
        if (a.norm() == 0 || centroid.norm() == 0) {
            return 0.0;
        }

        switch (this) {
            case SI:
                double cosine = a.innerProduct(centroid) / (a.norm() * centroid.norm());// a*b / (|a|*|b|)
                return Math.sqrt(1 - cosine * cosine);
            case EW:
                SparseVector c = a.minus(centroid);
                return c.innerProduct(c); // |a-b|
            case CR:
                a.sub(a.average());
                centroid.sub(centroid.average());
                return a.innerProduct(centroid) / (a.norm() * centroid.norm());
            case KL:
                double DklCon = 0.0d;
                for (int indx : centroid.indexList()) {
                    DklCon += a.floatValue(indx)
                              * Math.log(a.floatValue(indx) / centroid.floatValue(indx));
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
     * @return
     */
    public double divergence(double z1, double z2) {
        switch (this) {
            case IW:
                return z1 * Math.log(z1 / z2) - (z1 - z2);
            case EW:
                return Math.pow(z1 - z2, 2.0);
            default:
                return 0.0;
        }
    }
}
