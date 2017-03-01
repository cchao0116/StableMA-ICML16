package code.sma.util;

import code.sma.clustering.Cluster;
import code.sma.datastructure.DynIntArr;
import code.sma.datastructure.MatlabFasionSparseMatrix;

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
    public static int[] readInvolvedIndices(MatlabFasionSparseMatrix mfMatrix, boolean[] raf,
                                            boolean[] caf) {
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
    public static int[][] readInvolvedIndices(MatlabFasionSparseMatrix mfMatrix, int[] raf,
                                              int[] caf, int[] clusteringSize) {
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
     * read the mapping between the uId (iId) and uClustering(iClustering)Id 
     * 
     * @param raf           row assignment function. [userId] : [user clustering id]
     * @param caf           column assignment function. [itemId] : [item clustering id]
     * @param clusterDir    root directory of clustering configuration files  
     * @return
     */
    public static int[] readClusteringAssigmntFunction(int[] raf, int[] caf, String clusterDir) {
        // reading clustering size
        int[] clusteringSize = new int[2];
        {
            String[] settngContent = FileUtil.readLines(clusterDir + "SETTING");
            clusteringSize[0] = settngContent[0].split("\\,").length;
            clusteringSize[1] = settngContent[1].split("\\,").length;
        }

        // reading row assignment functions
        {
            String[] rowAssgnContent = FileUtil.readLines(clusterDir + "RM");
            for (String line : rowAssgnContent) {
                String[] elemnts = line.split("\\,");
                raf[Integer.valueOf(elemnts[0].trim())] = Integer.valueOf(elemnts[1].trim());
            }
        }

        // reading column assignment functions
        {
            String[] colAssgnContent = FileUtil.readLines(clusterDir + "CM");
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
     */
    public static void saveClustering(Cluster[][] clustering, String clusterDir) {
        // writing clustering size
        {
            StringBuilder settngContt = new StringBuilder();
            for (Cluster rowCluster : clustering[0]) {
                settngContt.append(rowCluster.size() + ",");
            }
            settngContt.replace(settngContt.length() - 1, settngContt.length(), "\n");

            for (Cluster colCluster : clustering[1]) {
                settngContt.append(colCluster.size() + ",");
            }
            settngContt.deleteCharAt(settngContt.length() - 1);
            FileUtil.writeAsAppendWithDirCheck(clusterDir + "SETTING", settngContt.toString());
        }

        // writing row assignment functions
        {
            int rowClusterId = 0;
            for (Cluster rowCluster : clustering[0]) {
                StringBuilder rafContt = new StringBuilder();
                for (Integer rowId : rowCluster) {
                    rafContt.append(rowId + "," + rowClusterId + '\n');
                }

                rowClusterId++;
                FileUtil.writeAsAppend(clusterDir + "RM", rafContt.toString());
            }
        }

        // writing column assignment functions
        {
            int colClusterId = 0;
            for (Cluster colCluster : clustering[1]) {
                StringBuilder cafContt = new StringBuilder();
                for (Integer colId : colCluster) {
                    cafContt.append(colId + "," + colClusterId + '\n');
                }

                colClusterId++;
                FileUtil.writeAsAppend(clusterDir + "CM", cafContt.toString());
            }
        }
    }
}
