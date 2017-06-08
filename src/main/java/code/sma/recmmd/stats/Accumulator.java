package code.sma.recmmd.stats;

import java.util.Arrays;

/**
 * 
 * @author Chao.Chen
 * @version $Id: Accumulator.java, v 0.1 2017年3月1日 下午1:55:02 Chao.Chen Exp $
 */
public class Accumulator {
    float[][] indvdlVal;
    float[]   accVal;      // accumulated value
    int[]     accNum;      // number of values in each calculator

    int       cursor_vId;
    int       cursor_accId;

    public Accumulator(int num, int dimnsn) {
        accVal = new float[num];
        accNum = new int[num];

        indvdlVal = new float[num][dimnsn];
        for (float[] iv : indvdlVal) {
            Arrays.fill(iv, Float.NaN);
        }

        cursor_vId = 0;
        cursor_accId = 0;
    }

    /**
     * WARN: Please carefully use this method, 
     * which recurrently update the values one-by-one 
     * from 0-th value of different calculator to the last one.
     * 
     * @param value the value to update
     */
    public void traverse(double value) {
        update(cursor_accId, cursor_vId, value);

        // update cursor
        int num_cal = indvdlVal.length;
        int num_val = indvdlVal[0].length;

        cursor_accId++;
        if (cursor_accId == num_cal) {
            cursor_vId++;
        }

        cursor_accId %= num_cal;
        cursor_vId %= num_val;
    }

    /**
     * update the value which exists in the accumulator
     * 
     * @param accId the accumulator's ID
     * @param vId   the index of the updated value
     * @param value the value to update
     */
    public void update(int accId, int vId, double value) {
        if (Float.isNaN(indvdlVal[accId][vId])) {
            accVal[accId] += value;
            accNum[accId]++;
        } else {
            accVal[accId] += value - indvdlVal[accId][vId];
        }

        indvdlVal[accId][vId] = (float) value;
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
