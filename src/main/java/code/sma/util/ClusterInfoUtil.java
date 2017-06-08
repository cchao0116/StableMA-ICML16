package code.sma.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

import com.google.common.io.Files;

import code.sma.clustering.Cluster;
import code.sma.core.DynIntArr;
import code.sma.core.impl.Tuples;

/**
 * This class is used to read the clustering structure from the setting files
 * 
 * @author Chao.Chen
 * @version $Id: ClusterInfoUtil.java, v 0.1 2016年9月27日 上午10:07:56 Chao.Chen Exp $
 */
public final class ClusterInfoUtil {

    /**
     * forbidden construction
     */
    private ClusterInfoUtil() {
        //forbidden construction
    }

    /**
     * filter the specific cluster-based indices from the sequential data stream
     * 
     * @param mfMatrix          the data matrix
     * @param raf               row assignment function
     * @param caf               column assignment function
     * @param specClusterIndx   the target cluster index
     * @return                  the involved indices
     */
    public static int[] readInvolvedIndices(Tuples mfMatrix, boolean[] raf, boolean[] caf) {
        int[] involvedIndices = new int[0];
        {
            int nnz = mfMatrix.getNnz();
            int[] rowIndx = mfMatrix.getRowIndx();
            int[] colIndx = mfMatrix.getColIndx();
            DynIntArr dynInvlvIndcs = new DynIntArr(nnz / 10);

            for (int mfSeq = 0; mfSeq < nnz; mfSeq++) {
                int rowId = rowIndx[mfSeq];
                int colId = colIndx[mfSeq];

                if (raf[rowId] && caf[colId]) {
                    dynInvlvIndcs.addValue(mfSeq);
                }
            }

            involvedIndices = dynInvlvIndcs.getArr();
        }
        return involvedIndices;
    }

    /**
     * filter the specific-expanded cluster-based indices from the sequential data stream
     * 
     * @param mfMatrix          the data matrix
     * @param raf               row assignment function
     * @param caf               column assignment function
     * @param specClusterIndx   the target cluster index
     * @return                  the involved indices
     */
    public static int[] readInvolvedIndicesExpanded(Tuples mfMatrix, boolean[] raf, boolean[] caf) {
        int[] involvedIndices = new int[0];
        {
            int nnz = mfMatrix.getNnz();
            int[] rowIndx = mfMatrix.getRowIndx();
            int[] colIndx = mfMatrix.getColIndx();
            DynIntArr dynInvlvIndcs = new DynIntArr(nnz / 10);

            for (int mfSeq = 0; mfSeq < nnz; mfSeq++) {
                int rowId = rowIndx[mfSeq];
                int colId = colIndx[mfSeq];

                if (raf[rowId] || caf[colId]) {
                    dynInvlvIndcs.addValue(mfSeq);
                }
            }

            involvedIndices = dynInvlvIndcs.getArr();
        }
        return involvedIndices;
    }

    /**
     * given a clustering result, divide the original data into subgroups
     * 
     * @param mfMatrix          the data matrix
     * @param raf               row assignment function
     * @param caf               column assignment function
     * @param clusteringSize    clustering size
     * @return                  the involved indices
     */
    public static int[][] readInvolvedIndices(Tuples mfMatrix, int[] raf, int[] caf,
                                              int[] clusteringSize) {
        int clusterNum = clusteringSize[0] * clusteringSize[1];
        int[][] involvedIndices = new int[clusterNum][0];

        {
            int nnz = mfMatrix.getNnz();
            int[] rowIndx = mfMatrix.getRowIndx();
            int[] colIndx = mfMatrix.getColIndx();

            DynIntArr[] dynInvlvIndcs = new DynIntArr[clusterNum];
            for (int c = 0; c < clusterNum; c++) {
                dynInvlvIndcs[c] = new DynIntArr(nnz / clusterNum);
            }

            for (int mfSeq = 0; mfSeq < nnz; mfSeq++) {
                int rowId = rowIndx[mfSeq];
                int colId = colIndx[mfSeq];

                int clusterId = raf[rowId] * clusteringSize[1] + caf[colId];
                dynInvlvIndcs[clusterId].addValue(mfSeq);
            }

            for (int c = 0; c < clusterNum; c++) {
                involvedIndices[c] = dynInvlvIndcs[c].getArr();
            }
        }

        return involvedIndices;
    }

    /**
     * read <b>a</b>ccessible <b>f</b>eature <b>i</b>ndicator <b>(AFI)</b> based on clustering
     * 
     * @param num_rows          number of rows
     * @param num_cols          number of columns
     * @param clusterDir        file path of clustering 
     * @return                  [0] user feature accessible indicator, [1] item feature accessible indicator
     * @throws IOException
     */
    public static boolean[][][] readAFI(int num_rows, int num_cols,
                                        String clusterDir) throws IOException {
        boolean[][][] acc_features = new boolean[2][0][0];

        acc_features[0] = readAFIInner(new File(clusterDir + "RM"), num_rows);
        acc_features[1] = readAFIInner(new File(clusterDir + "CM"), num_cols);
        return acc_features;
    }

    protected static boolean[][] readAFIInner(File f, int num) throws IOException {
        List<String> lines = Files.readLines(f, Charset.defaultCharset());

        int num_lines = lines.size();
        boolean[][] acc_feature = new boolean[num_lines][num];
        for (int n = 0; n < num_lines; n++) {
            String line = lines.get(n);
            if (StringUtil.isBlank(line)) {
                continue;
            }

            Scanner scanner = new Scanner(line);
            scanner.useDelimiter(":+|[|]|,");

            int c = scanner.nextInt();

            while (scanner.hasNextInt()) {
                int rowId = scanner.nextInt();
                acc_feature[c][rowId] = true;
            }
            IOUtils.closeQuietly(scanner);
        }

        return acc_feature;
    }

    /**
     * read the mapping between the uId (iId) and uClustering(iClustering)Id 
     * 
     * @param raf           row assignment function. [userId] : [user clustering id]
     * @param caf           column assignment function. [itemId] : [item clustering id]
     * @param clusterDir    root directory of clustering configuration files  
     * @return
     * @throws IOException 
     */
    public static int[] readClusteringAssigmntFunction(int[] raf, int[] caf,
                                                       String clusterDir) throws IOException {
        // reading clustering size
        int[] clusteringSize = new int[2];
        {
            List<String> settngContent = Files.readLines(new File(clusterDir + "SETTING"),
                Charset.defaultCharset());
            clusteringSize[0] = settngContent.get(0).split("\\,").length;
            clusteringSize[1] = settngContent.get(0).split("\\,").length;
        }

        // reading row assignment functions
        {
            List<String> rowAssgnContent = Files.readLines(new File(clusterDir + "RM"),
                Charset.defaultCharset());
            for (String line : rowAssgnContent) {
                String[] elemnts = line.split("\\,");
                raf[Integer.valueOf(elemnts[0].trim())] = Integer.valueOf(elemnts[1].trim());
            }
        }

        // reading column assignment functions
        {
            List<String> colAssgnContent = Files.readLines(new File(clusterDir + "CM"),
                Charset.defaultCharset());
            for (String line : colAssgnContent) {
                String[] elemnts = line.split("\\,");
                caf[Integer.valueOf(elemnts[0].trim())] = Integer.valueOf(elemnts[1].trim());
            }
        }

        return clusteringSize;
    }

    /**
     * write the clustering information to the disk, 
     * including assignment function and clustering size
     * 
     * @param clustering    clustering structure
     * @param clusterDir    the file destination
     * @throws IOException 
     */
    public static void saveClustering(Cluster[][] clustering,
                                      String clusterDir) throws IOException {

        // writing row assignment functions
        {
            StringBuilder str = new StringBuilder();
            int c = 0;
            for (Cluster cluster : clustering[0]) {
                str.append(String.format("%d:%s\n", c, cluster.toString()));
                c++;
            }
            Files.write(str, new File(clusterDir + "RM"), Charset.defaultCharset());
        }

        // writing column assignment functions
        {
            int c = 0;
            StringBuilder str = new StringBuilder();
            for (Cluster cluster : clustering[1]) {
                str.append(String.format("%d:%s\n", c, cluster.toString()));
                c++;
            }
            Files.write(str, new File(clusterDir + "CM"), Charset.defaultCharset());
        }
    }
}
