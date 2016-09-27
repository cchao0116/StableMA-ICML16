package code.sma.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.apache.commons.io.IOUtils;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.datastructure.SparseMatrix;
import code.sma.datastructure.SparseVector;

/**
 * Matrix write utilities
 * 
 * @author Chao Chen
 * @version $Id: MatrixFileUtil.java, v 0.1 2014-10-16 下午2:32:09 chench Exp $
 */
public final class MatrixFileUtil {

    /**
     * forbid construction method
     */
    private MatrixFileUtil() {
        //forbid construction method
    }

    /**
     * write matrix to disk
     * 
     * @param file              the file to write
     * @param matrix            the matrix contains the data
     */
    public static void write(String file, SparseMatrix matrix) {
        FileUtil.delete(file);
        FileUtil.existDirAndMakeDir(file);

        int itemCount = 0;
        StringBuilder buffer = new StringBuilder();
        for (int u = 0, rowCount = matrix.length()[0]; u < rowCount; u++) {
            SparseVector Fu = matrix.getRowRef(u);
            int[] itemList = Fu.indexList();
            if (itemList == null) {
                continue;
            }

            for (int i : itemList) {
                double val = matrix.getValue(u, i);
                String elemnt = u + "::" + i + "::" + String.format("%.1f", val);
                buffer.append(elemnt).append('\n');
                itemCount++;
            }

            // if greater than buffer size, then clear the buffer.
            if (itemCount >= 1000000) {
                FileUtil.writeAsAppend(file, buffer.toString());

                //reset buffer
                itemCount = 0;
                buffer = new StringBuilder();
            }
        }

        FileUtil.writeAsAppend(file, buffer.toString());
    }

    //=============================================
    //      Read methods
    //=============================================
    /**
     * Read matrix from file
     * 
     * @param file          file contain matrix data
     * @param rowCount      the number of rows
     * @param colCount      the number of columns
     * @param parser        the parser to parse the data structure
     * @return
     */
    public static MatlabFasionSparseMatrix reads(String filePath) {
        LineNumberReader lnr = null;
        BufferedReader reader = null;

        MatlabFasionSparseMatrix result = null;
        try {
            File file = new File(filePath);
            lnr = new LineNumberReader(new FileReader(file));
            lnr.skip(Long.MAX_VALUE);
            int nnz = lnr.getLineNumber();

            result = new MatlabFasionSparseMatrix(nnz);
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] elemnts = line.split(",");
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
            IOUtils.closeQuietly(lnr);
        }
        return null;
    }

}
