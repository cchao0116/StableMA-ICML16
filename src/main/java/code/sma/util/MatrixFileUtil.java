/**
 * Tongji Edu.
 * Copyright (c) 2004-2014 All Rights Reserved.
 */
package code.sma.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.datastructure.SparseMatrix;
import code.sma.datastructure.SparseVector;
import code.sma.parser.Ml10mParser;
import code.sma.parser.Parser;
import code.sma.parser.RatingVO;

/**
 * Matrix write utilities
 * 
 * @author Hanke Chen
 * @version $Id: MatrixFileUtil.java, v 0.1 2014-10-16 下午2:32:09 chench Exp $
 */
public final class MatrixFileUtil {

    /**
     * forbid construction method
     */
    private MatrixFileUtil() {

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
    public static MatlabFasionSparseMatrix reads(String file, int nnz, Parser parser) {
        if (parser == null) {
            parser = new Ml10mParser();
        }

        MatlabFasionSparseMatrix result = new MatlabFasionSparseMatrix(nnz);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                RatingVO rating = (RatingVO) parser.parse(line);
                result.setValue(rating.getUsrId(), rating.getMovieId(), rating.getRatingReal());
            }
            result.reduceMem();

            return result;
        } catch (FileNotFoundException e) {
            ExceptionUtil.caught(e, "无法找到对应的加载文件: " + file);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "读取文件发生异常，校验文件格式");
        } finally {
            IOUtils.closeQuietly(reader);
        }
        return null;
    }

}
