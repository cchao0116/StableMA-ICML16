package code.sma.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.apache.commons.io.IOUtils;

import code.sma.core.AbstractMatrix;
import code.sma.core.impl.SparseMatrix;
import code.sma.core.impl.Tuples;

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

    public static void loadData(String filePath, AbstractMatrix dm) {

    }

    /**
     * Read matrix from file
     * 
     * @param file          file contain matrix data
     * @param rowCount      the number of rows
     * @param colCount      the number of columns
     * @param parser        the parser to parse the data structure
     * @return
     */

    public static Tuples reads(String filePath) {
        LineNumberReader lnr = null;
        BufferedReader reader = null;

        Tuples result = null;
        try {
            File file = new File(filePath);
            lnr = new LineNumberReader(new FileReader(file));
            lnr.skip(Long.MAX_VALUE);
            int nnz = lnr.getLineNumber();

            result = new Tuples(nnz);
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                //                String[] elemnts = line.split("\\::");
                //                result.setValue(Integer.valueOf(elemnts[0].trim()),
                //                    Integer.valueOf(elemnts[1].trim()), Double.valueOf(elemnts[2].trim()));

                result.loadNext(line);
            }

            return result;
        } catch (FileNotFoundException e) {
            ExceptionUtil.caught(e, "File Path: " + filePath);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "Error In Reading");
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(lnr);
        }
        return null;
    }

    /**
     * Read matrix from file
     * 
     * @param file          file contain matrix data
     * @param rowCount      the number of rows
     * @param colCount      the number of columns
     * @param parser        the parser to parse the data structure
     * @return
     */
    public static SparseMatrix read(String filePath, int rowCount, int colCount) {

        SparseMatrix result = new SparseMatrix(rowCount, colCount);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] elemnts = line.split("\\::");
                result.setValue(Integer.valueOf(elemnts[0].trim()),
                    Integer.valueOf(elemnts[1].trim()), Double.valueOf(elemnts[2].trim()));
            }

            return result;
        } catch (FileNotFoundException e) {
            ExceptionUtil.caught(e, "File Path: " + filePath);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "Error In Reading");
        } finally {
            IOUtils.closeQuietly(reader);
        }
        return null;
    }

}
