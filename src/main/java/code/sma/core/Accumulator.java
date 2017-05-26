package code.sma.core;

/**
 * 
 * @author Chao.Chen
 * @version $Id: Accumulator.java, v 0.1 2017年3月1日 下午1:55:02 Chao.Chen Exp $
 */
public class Accumulator {
    double[][] indvdlVal;
    double[]   accVal;
    int[]      accNum;

    public Accumulator(int num) {
        accVal = new double[num];
        accNum = new int[num];
    }

    public Accumulator(int num, int dimnsn) {
        indvdlVal = new double[num][dimnsn];
        accVal = new double[num];
        accNum = new int[num];
    }

    /**
     * insert a new value to the accumulator
     * 
     * @param accId the accumulator's ID
     * @param vId   the index of the new value
     * @param value the value to insert
     */
    public void insert(int accId, int vId, double value) {
        if (indvdlVal != null) {
            indvdlVal[accId][vId] = value;
        }
        accVal[accId] += value;
        accNum[accId]++;
    }

    /**
     * update the value which exists in the accumulator
     * 
     * @param accId the accumulator's ID
     * @param vId   the index of the updated value
     * @param value the value to update
     */
    public void update(int accId, int vId, double value) {
        accVal[accId] += value - indvdlVal[accId][vId];
        indvdlVal[accId][vId] = value;
    }

    /** 
     * get the root sum of the given accumulator's ID
     * 
     * @param accId the accumulator's ID
     * @return
     */
    public double rs(int accId) {
        return Math.sqrt(accVal[accId]);
    }

    /**
     * get the root mean value of the given accumulator's ID
     * 
     * @param accId the accumulator's ID
     * @return      root mean value
     */
    public double rm(int accId) {
        return Math.sqrt(accVal[accId] / accNum[accId]);
    }

    /**
     * get the root mean value of overall data
     * 
     * @return  root mean value
     */
    public double rm() {
        double sse = 0.0d;
        int availNum = 0;
        for (int accId = 0; accId < accVal.length; accId++) {
            if (accNum[accId] != 0) {
                sse += accVal[accId] / accNum[accId];
                availNum++;
            }
        }
        return Math.sqrt(sse / availNum);
    }
}
