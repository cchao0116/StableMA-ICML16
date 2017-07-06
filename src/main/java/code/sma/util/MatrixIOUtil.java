package code.sma.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import code.sma.core.AbstractMatrix;
import code.sma.core.impl.CSRMatrix;
import code.sma.core.impl.DCSRMatrix;
import code.sma.core.impl.SparseMatrix;
import code.sma.core.impl.Tuples;
import code.sma.main.Configures;
import it.unimi.dsi.fastutil.chars.Char2FloatMap;
import it.unimi.dsi.fastutil.chars.Char2FloatOpenHashMap;
import it.unimi.dsi.fastutil.floats.Float2CharMap;
import it.unimi.dsi.fastutil.floats.Float2CharOpenHashMap;

/**
 * Matrix write utilities
 * 
 * @author Chao Chen
 * @version $Id: MatrixFileUtil.java, v 0.1 2014-10-16 下午2:32:09 chench Exp $
 */
public final class MatrixIOUtil {

    /**
     * forbid construction method
     */
    private MatrixIOUtil() {
        //forbid construction method
    }

    //=============================================
    //      Read methods
    //=============================================
    public static DCSRMatrix loadDCSRMatrix(String filePath, int num_row, int num_val,
                                            Configures conf) {
        assert conf.containsKey(
            "DISCRETE_INPUT_SET") : "DSCRMatrix require DISCRETE_INPUT_SET in configure";

        float[] discrete_value = conf.getFloatArr("DISCRETE_INPUT_SET");

        // make char2float map
        char c = 0;
        Char2FloatMap char2num = new Char2FloatOpenHashMap();
        Float2CharMap num2char = new Float2CharOpenHashMap();
        for (float d : discrete_value) {
            char2num.put(c, d);
            num2char.put(d, c);
            c++;
        }

        DCSRMatrix dcsrm = new DCSRMatrix(num_row, num_val, char2num, num2char);
        loadData(filePath, dcsrm);
        return dcsrm;
    }

    public static CSRMatrix loadCSRMatrix(String filePath, int num_row, int num_val) {
        CSRMatrix csm = new CSRMatrix(num_row, num_val);
        loadData(filePath, csm);
        return csm;
    }

    public static Tuples loadTuples(String filePath, int num_val) {
        Tuples tupl = new Tuples(num_val);
        loadData(filePath, tupl);
        return tupl;
    }

    public static SparseMatrix loadSparseMatrix(String filePath, int num_row, int num_colum) {
        SparseMatrix sm = new SparseMatrix(num_row, num_colum);
        loadData(filePath, sm);
        return sm;
    }

    public static void loadData(String filePath, AbstractMatrix dm) {
        BufferedReader reader = null;
        try {
            File file = new File(filePath);
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                dm.loadNext(line);
            }

        } catch (FileNotFoundException e) {
            ExceptionUtil.caught(e, "File Path: " + filePath);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "Error In Reading");
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

}
