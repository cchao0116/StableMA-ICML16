package code.sma.main;

import java.util.Properties;

import code.sma.core.impl.DenseVector;

/**
 * 
 * @author Chao.Chen
 * @version $Id: Configures.java, v 0.1 2016年9月27日 下午2:21:47 Chao.Chen Exp $
 */
public final class Configures extends Properties {
    /**  serialVersionUID */
    private static final long serialVersionUID = 1L;

    public Configures() {
    }

    public Configures(Configures conf) {
        for (Object k : conf.keySet()) {
            String key = (String) k;
            put(key, conf.get(key));
        }
    }

    public void setDouble(String key, Double val) {
        put(key.trim(), val);
    }

    public void setVector(String key, DenseVector val) {
        put(key.trim(), val);
    }

    public DenseVector getVector(String key) {
        return (DenseVector) get(key.trim());
    }

    public double getDouble(String key) {
        return ((double) get(key.trim()));
    }

    public float getFloat(String key) {
        return ((Double) get(key.trim())).floatValue();
    }

    public int getInteger(String key) {
        return ((Double) get(key.trim())).intValue();
    }

    public short getShort(String key) {
        return ((Double) get(key.trim())).shortValue();
    }

    public boolean getBoolean(String key) {
        return (boolean) get(key.trim());
    }

    public double[] getDoubleArr(String key) {
        String[] elems = getProperty(key).split(",+");
        int length = elems.length;

        double[] d = new double[length];
        for (int i = 0; i < length; i++) {
            d[i] = Double.parseDouble(elems[i].trim());
        }
        return d;
    }

    public float[] getFloatArr(String key) {
        String[] elems = getProperty(key).split(",+");
        int length = elems.length;

        float[] d = new float[length];
        for (int i = 0; i < length; i++) {
            d[i] = Float.parseFloat(elems[i].trim());
        }
        return d;
    }

    public int[] getIntArr(String key) {
        String[] elems = getProperty(key).split(",+");
        int length = elems.length;

        int[] d = new int[length];
        for (int i = 0; i < length; i++) {
            d[i] = Integer.parseInt(elems[i].trim());
        }
        return d;
    }

    public short[] getShortArr(String key) {
        String[] elems = getProperty(key).split(",+");
        int length = elems.length;

        short[] d = new short[length];
        for (int i = 0; i < length; i++) {
            d[i] = Short.parseShort(elems[i].trim());
        }
        return d;
    }

}
