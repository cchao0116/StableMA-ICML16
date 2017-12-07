package code.sma.recmmd.cf.ma.stats;

import java.util.Random;

import code.sma.core.impl.DenseVector;

/**
 * Randomly initialize vectors
 * 
 * @author Chao Chen
 * @version $Id: RandomVector.java, v 1.0 Dec 7, 2017 10:27:25 AM$
 */
public enum RandomVector {
                          ZERO, //zero vector
                          UNIFORM, //uniformal at random
                          GAUSSIAN; // in guassian

    private static Random ran = new Random();

    /**
     * create a vector at random
     * 
     * @param N         the length of the vector
     * @param params    extension parameters
     * @return          a not null vector
     */
    public DenseVector create(int N, double... params) {
        DenseVector vec = new DenseVector(N);

        switch (this) {
            case ZERO:
                return vec;
            case UNIFORM:
                for (int n = 0; n < N; n++) {
                    vec.setValue(n, ran.nextDouble() * params[0]);
                }
                return vec;
            case GAUSSIAN:
                for (int n = 0; n < N; n++) {
                    vec.setValue(n, ran.nextGaussian() * params[0]);
                }
                return vec;
            default:
                return null;
        }
    }
}
