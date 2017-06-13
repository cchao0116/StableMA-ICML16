package code.sma.recmmd;

/**
 * This is a class implementing kernel smoothing functions
 * 
 * @author Chao.Chen
 * @version $Id: KernelSmoothing.java, v 0.1 2017年4月10日 下午1:07:54 Chao.Chen Exp $
 */
public enum KernelSmoothing {
                             TRIANGULAR_KERNEL, // Triangular kernel
                             UNIFORM_KERNEL, EPANECHNIKOV_KERNEL, GAUSSIAN_KERNEL;

    public double kernelize(double sim, double width) {
        double dist = 1.0 - sim;

        switch (this) {
            case TRIANGULAR_KERNEL:
                return Math.max(1 - dist / width, 0);
            case UNIFORM_KERNEL:
                return dist < width ? 1 : 0;
            case EPANECHNIKOV_KERNEL:
                return Math.max(3.0 / 4.0 * (1 - Math.pow(dist / width, 2)), 0);
            case GAUSSIAN_KERNEL:
                return 1 / Math.sqrt(2 * Math.PI) * Math.exp(-0.5 * Math.pow(dist / width, 2));
            default:
                return Math.max(1 - dist / width, 0);
        }
    }
}
