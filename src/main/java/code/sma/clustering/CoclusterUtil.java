/**
 * Tongji Edu.
 * Copyright (c) 2004-2014 All Rights Reserved.
 */
package code.sma.clustering;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.log4j.Logger;

import code.sma.core.impl.SparseMatrix;
import code.sma.core.impl.SparseVector;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;

/**
 * Co-cluster method 
 * A Generalized Maximum Entropy Approach to Bregman Co-clustering and Matrix Approximation
 * Banerjee Arindam, Dhillon Inderjit
 * The Journal of Machine Learning Research
 * 
 * @author Hanke Chen
 * @version $Id: CoclusterUtil.java, v 0.1 2014-10-27 下午4:11:21 chench Exp $
 */
public final class CoclusterUtil {

    //===========================================
    //      Constraints
    //===========================================
    /** Constraint 1: preserve E[Z|U*], E[Z|V*] */
    public final static int     C_1    = 1;

    /** Constraint 2: preserve E[Z|U*,V*] */
    public final static int     C_2    = 2;

    /** Constraint 3: preserve E[Z|U*,V*], E[Z|U] */
    public final static int     C_3    = 3;

    /** Constraint 4: preserve E[Z|U*,V*], E[Z|V] */
    public final static int     C_4    = 4;

    /** Constraint 5: preserve E[Z|U*,V*], E[Z|U*,V], E[Z|U,V*] */
    public final static int     C_5    = 5;

    /** Constraint 6: preserve E[Z|U,V*], E[Z|U*,V] */
    public final static int     C_6    = 6;

    /** logger */
    private final static Logger logger = Logger.getLogger(LoggerDefineConstant.SERVICE_CORE);

    /**
     * forbide construction
     */
    private CoclusterUtil() {
        //forbide construction
    }

    /**
     * cocolustering the matrix, while we assume the missing value comply
     * with the distribution of observations.
     * 
     * @param points        the matrix contains the data
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param maxIteration  the maximum iteration to divide
     * @param constraint    the co-clustering base
     * @param divergence    the bragman divergence involved
     * @return the cocolusters, index 0 contains the row clusters, whereas
     * 1 contains the column clusters.
     */
    public static Cluster[][] divideWithConjugateAssumption(final SparseMatrix points, final int K,
                                                            final int L, final int maxIteration,
                                                            final int constraint,
                                                            final Distance dtncConst) {
        //check primary parameter
        int rowCount = points.shape()[0];
        int colCount = points.shape()[1];
        if (rowCount < K || colCount < L) {
            throw new RuntimeException("Number of samples is less than the number of classes.");
        }

        //make a initial division
        Cluster[] rowCluster = new Cluster[K];
        int[] rowAssigmnt = new int[rowCount];
        initialCluster(rowCluster, rowAssigmnt);

        Cluster[] colCluster = new Cluster[L];
        int[] colAssigmnt = new int[colCount];
        initialCluster(colCluster, colAssigmnt);

        //conditional expectation
        double[][] E_Uc_Vc = new double[K][L];
        double[][] E_U_Vc = new double[rowCount][L];
        double[][] E_Uc_V = new double[K][colCount];
        double[] E_U = new double[rowCount];
        double[] E_Uc = new double[K];
        double[] E_V = new double[colCount];
        double[] E_Vc = new double[L];
        double E = points.average();
        comConExpectationAsConjugateDist(points, K, L, rowCluster, colCluster, E_Uc_Vc, E_U, E_Uc,
            E_V, E_Vc, E_U_Vc, E_Uc_V);

        int round = 0;
        while (round < maxIteration) {
            boolean isChanged = false;
            double err = 0.0;
            round++;

            //update row cluster
            err = updateRowCluster(points, K, L, rowCluster, colCluster, rowAssigmnt, colAssigmnt,
                constraint, dtncConst, isChanged, E_Uc_Vc, E_U, E_Uc, E_V, E_Vc, E_U_Vc, E_Uc_V, E);
            LoggerUtil.info(logger, round + "A:\t" + err);

            //update column cluster
            err = updateColCluster(points, K, L, rowCluster, colCluster, rowAssigmnt, colAssigmnt,
                constraint, dtncConst, isChanged, E_Uc_Vc, E_U, E_Uc, E_V, E_Vc, E_U_Vc, E_Uc_V, E);
            LoggerUtil.info(logger, round + "B:\t" + err);

            //update Lagrange multipliers
            if (round < maxIteration) {
                comConExpectationAsConjugateDist(points, K, L, rowCluster, colCluster, E_Uc_Vc, E_U,
                    E_Uc, E_V, E_Vc, E_U_Vc, E_Uc_V);
            }

        }

        Cluster[][] result = new Cluster[2][1];
        result[0] = rowCluster;
        result[1] = colCluster;
        return result;
    }

    /**
     * Choose an initialization for the cluster
     * 
     * @param cluster       the cluster to initialize
     * @param assigmnt      the cluster assigment
     */
    protected static void initialCluster(Cluster[] cluster, int[] assigmnt) {
        int dimension = cluster.length;
        int sampleCount = assigmnt.length;
        for (int i = 0; i < dimension; i++) {
            cluster[i] = new Cluster();
        }

        boolean isEmpty = true;
        while (isEmpty) {
            // clear old assigment
            for (Cluster local : cluster) {
                local.clear();
            }

            // distribute umiformly
            UniformIntegerDistribution ran = new UniformIntegerDistribution(0, dimension - 1);
            for (int i = 0; i < sampleCount; i++) {
                int local = ran.sample();

                cluster[local].add(i);
                assigmnt[i] = local;
            }

            // check whether a cluster is empty
            isEmpty = false;
            for (Cluster local : cluster) {
                if (local.isEmpty()) {
                    isEmpty = true;
                    break;
                }
            }

        }
    }

    /**
     * update row clusters w.r.t bragmen-divergence
     * 
     * @param points        the matrix contains the data
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param rowCluster    the row cluster
     * @param colCluster    the column cluster
     * @param rowAssigmnt   the assigment of row clusters
     * @param colAssigmnt   the assigment of column clusters
     * @param constraint    the constraints to retain
     * @param divergence    the bragman divergence to compute
     * @param isChanged     the pivot to indicate whether the cocluster changes
     */
    protected static double updateRowCluster(final SparseMatrix points, final int K, final int L,
                                             final Cluster[] rowCluster, final Cluster[] colCluster,
                                             final int[] rowAssigmnt, final int[] colAssigmnt,
                                             final int constraint, final Distance dtncConst,
                                             boolean isChanged, double[][] E_Uc_Vc, double[] E_U,
                                             double[] E_Uc, double[] E_V, double[] E_Vc,
                                             double[][] E_U_Vc, double[][] E_Uc_V, double E) {
        switch (dtncConst) {
            case IW:
                return updateRowClusterForIDivergence(points, K, L, rowCluster, colCluster,
                    rowAssigmnt, colAssigmnt, constraint, isChanged, E_Uc_Vc, E_U, E_Uc, E_V, E_Vc,
                    E_U_Vc, E_Uc_V, E);
            case EW:
                return updateRowClusterForEuclidean(points, K, L, rowCluster, colCluster,
                    rowAssigmnt, colAssigmnt, constraint, isChanged, E_Uc_Vc, E_U, E_Uc, E_V, E_Vc,
                    E_U_Vc, E_Uc_V, E);
            default:
                throw new RuntimeException("No Divergence is choosed! ");
        }
    }

    /**
     * update row clusters, when the I-divergence is involved
     * 
     * @param points        the matrix contains the data
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param rowCluster    the row cluster
     * @param colCluster    the column cluster
     * @param rowAssigmnt   the assigment of row clusters
     * @param colAssigmnt   the assigment of column clusters
     * @param constraint    the constraints to retain
     * @param isChanged     the pivot to indicate whether the cocluster changes
     */
    protected static double updateRowClusterForIDivergence(final SparseMatrix points, final int K,
                                                           final int L, final Cluster[] rowCluster,
                                                           final Cluster[] colCluster,
                                                           final int[] rowAssigmnt,
                                                           final int[] colAssigmnt,
                                                           final int constraint, boolean isChanged,
                                                           double[][] E_Uc_Vc, double[] E_U,
                                                           double[] E_Uc, double[] E_V,
                                                           double[] E_Vc, double[][] E_U_Vc,
                                                           double[][] E_Uc_V, double E) {
        int rowCount = points.shape()[0];

        //clear current clusters
        for (Cluster local : rowCluster) {
            local.clear();
        }

        //update row clusters
        double error = 0.0d;
        for (int u = 0; u < rowCount; u++) {
            SparseVector ZuReal = points.getRowRef(u);
            int[] itemList = ZuReal.indexList();
            if (itemList == null) {
                continue;
            }

            // find minimium position
            int pivot = -1;
            double min = Double.MAX_VALUE;
            for (int Uc = 0; Uc < K; Uc++) {
                double iDivergence = 0.0d;
                for (int v : itemList) {
                    int Vc = colAssigmnt[v];

                    double ZuvReal = ZuReal.floatValue(v);
                    // compute Zuv w.r.t constraints
                    double ZuvEstim = 0.0d;
                    switch (constraint) {
                        case C_1:
                            ZuvEstim = E_Uc[Uc] * E_Vc[Vc] / E;
                            break;
                        case C_2:
                            ZuvEstim = E_Uc_Vc[Uc][Vc];
                            break;
                        case C_3:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] * E_U[u] / E_Uc[Uc];
                            break;
                        case C_4:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] * E_U[v] / E_Uc[Vc];
                            break;
                        case C_5:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] * E_U[u] * E_V[v] / (E_Uc[Uc] * E_Vc[Vc]);
                            break;
                        case C_6:
                            ZuvEstim = E_U_Vc[u][Vc] * E_Uc_V[Uc][v] / (E_Uc_Vc[Uc][Vc]);
                            break;
                        default:
                            throw new RuntimeException("No contraints is choosed! ");
                    }
                    iDivergence += Distance.IW.divergence(ZuvReal, ZuvEstim);
                }

                if (min > iDivergence) {
                    min = iDivergence;
                    pivot = Uc;
                }
            }

            // check whether changed
            if (rowAssigmnt[u] != pivot) {
                isChanged = true;
            }

            // move to the position
            rowCluster[pivot].add(u);
            rowAssigmnt[u] = pivot;

            // update errors
            error += min;
        }
        return error;
    }

    /**
     * update row clusters, when the euclidean divergence is involved
     * 
     * @param points        the matrix contains the data
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param rowCluster    the row cluster
     * @param colCluster    the column cluster
     * @param rowAssigmnt   the assigment of row clusters
     * @param colAssigmnt   the assigment of column clusters
     * @param constraint    the constraints to retain
     * @param isChanged     the pivot to indicate whether the cocluster changes
     */
    protected static double updateRowClusterForEuclidean(final SparseMatrix points, final int K,
                                                         final int L, final Cluster[] rowCluster,
                                                         final Cluster[] colCluster,
                                                         final int[] rowAssigmnt,
                                                         final int[] colAssigmnt,
                                                         final int constraint, boolean isChanged,
                                                         double[][] E_Uc_Vc, double[] E_U,
                                                         double[] E_Uc, double[] E_V, double[] E_Vc,
                                                         double[][] E_U_Vc, double[][] E_Uc_V,
                                                         double E) {
        int rowCount = points.shape()[0];

        //clear current clusters
        for (Cluster local : rowCluster) {
            local.clear();
        }

        //update row clusters
        double error = 0.0d;
        for (int u = 0; u < rowCount; u++) {
            SparseVector ZuReal = points.getRowRef(u);
            int[] itemList = ZuReal.indexList();
            if (itemList == null) {
                continue;
            }

            // find minimium position
            int pivot = -1;
            double min = Double.MAX_VALUE;
            for (int Uc = 0; Uc < K; Uc++) {
                double euDivergence = 0.0d;
                for (int v : itemList) {
                    int Vc = colAssigmnt[v];

                    double ZuvReal = ZuReal.floatValue(v);
                    // compute Zuv w.r.t constraints
                    double ZuvEstim = 0.0d;
                    switch (constraint) {
                        case C_1:
                            ZuvEstim = E_Uc[Uc] + E_Vc[Vc] - E;
                            break;
                        case C_2:
                            ZuvEstim = E_Uc_Vc[Uc][Vc];
                            break;
                        case C_3:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] + E_U[u] - E_Uc[Uc];
                            break;
                        case C_4:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] + E_U[v] - E_Uc[Vc];
                            break;
                        case C_5:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] + E_U[u] + E_V[v] - E_Uc[Uc] - E_Vc[Vc];
                            break;
                        case C_6:
                            ZuvEstim = E_U_Vc[u][Vc] + E_Uc_V[Uc][v] - E_Uc_Vc[Uc][Vc];
                            break;
                        default:
                            throw new RuntimeException("No contraints is choosed! ");
                    }
                    euDivergence += Distance.EW.divergence(ZuvReal, ZuvEstim);
                }

                if (min > euDivergence) {
                    min = euDivergence;
                    pivot = Uc;
                }
            }

            // check whether changed
            if (rowAssigmnt[u] != pivot) {
                isChanged = true;
            }

            // move to the position
            rowCluster[pivot].add(u);
            rowAssigmnt[u] = pivot;

            // update errors
            error += min;
        }

        return error;
    }

    protected static double updateColCluster(final SparseMatrix points, final int K, final int L,
                                             final Cluster[] rowCluster, final Cluster[] colCluster,
                                             final int[] rowAssigmnt, final int[] colAssigmnt,
                                             final int constraint, final Distance dtncConst,
                                             boolean isChanged, double[][] E_Uc_Vc, double[] E_U,
                                             double[] E_Uc, double[] E_V, double[] E_Vc,
                                             double[][] E_U_Vc, double[][] E_Uc_V, double E) {
        switch (dtncConst) {
            case IW:
                return updateColClusterForIDivergence(points, K, L, rowCluster, colCluster,
                    rowAssigmnt, colAssigmnt, constraint, isChanged, E_Uc_Vc, E_U, E_Uc, E_V, E_Vc,
                    E_U_Vc, E_Uc_V, E);
            case EW:
                return updateColClusterForEuclidean(points, K, L, rowCluster, colCluster,
                    rowAssigmnt, colAssigmnt, constraint, isChanged, E_Uc_Vc, E_U, E_Uc, E_V, E_Vc,
                    E_U_Vc, E_Uc_V, E);
            default:
                throw new RuntimeException("No Divergence is choosed! ");
        }
    }

    /**
     * update column clusters, when the I-divergence is involved
     * 
     * @param points        the matrix contains the data
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param rowCluster    the row cluster
     * @param colCluster    the column cluster
     * @param rowAssigmnt   the assigment of row clusters
     * @param colAssigmnt   the assigment of column clusters
     * @param constraint    the constraints to retain
     * @param isChanged     the pivot to indicate whether the cocluster changes
     */
    protected static double updateColClusterForIDivergence(final SparseMatrix points, final int K,
                                                           final int L, final Cluster[] rowCluster,
                                                           final Cluster[] colCluster,
                                                           final int[] rowAssigmnt,
                                                           final int[] colAssigmnt,
                                                           final int constraint, boolean isChanged,
                                                           double[][] E_Uc_Vc, double[] E_U,
                                                           double[] E_Uc, double[] E_V,
                                                           double[] E_Vc, double[][] E_U_Vc,
                                                           double[][] E_Uc_V, double E) {
        int colCount = points.shape()[1];
        //clear current clusters
        boolean[] emptyIndicator = new boolean[L];
        for (int i = 0; i < L; i++) {
            if (colCluster[i].isEmpty()) {
                emptyIndicator[i] = true;
            } else {
                colCluster[i].clear();
                emptyIndicator[i] = false;
            }
        }

        //update column clusters
        double error = 0.0d;
        for (int v = 0; v < colCount; v++) {
            SparseVector ZvReal = points.getColRef(v);
            int[] itemList = ZvReal.indexList();
            if (itemList == null) {
                continue;
            }

            // find minimium position
            int pivot = -1;
            double min = Double.MAX_VALUE;
            for (int Vc = 0; Vc < L; Vc++) {
                if (emptyIndicator[Vc]) {
                    continue;
                }

                double iDivergence = 0.0d;
                for (int u : itemList) {
                    int Uc = rowAssigmnt[u];

                    // compute Zuv w.r.t constraints
                    double ZuvReal = ZvReal.floatValue(u);
                    double ZuvEstim = 0.0d;
                    switch (constraint) {
                        case C_1:
                            ZuvEstim = E_Uc[Uc] * E_Vc[Vc] / E;
                            break;
                        case C_2:
                            ZuvEstim = E_Uc_Vc[Uc][Vc];
                            break;
                        case C_3:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] * E_U[u] / E_Uc[Uc];
                            break;
                        case C_4:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] * E_U[v] / E_Uc[Vc];
                            break;
                        case C_5:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] * E_U[u] * E_V[v] / (E_Uc[Uc] * E_Vc[Vc]);
                            break;
                        case C_6:
                            ZuvEstim = E_U_Vc[u][Vc] * E_Uc_V[Uc][v] / (E_Uc_Vc[Uc][Vc]);
                            break;

                        default:
                            throw new RuntimeException("No contraints is choosed! ");
                    }
                    iDivergence += Distance.IW.divergence(ZuvReal, ZuvEstim);
                }

                if (min > iDivergence) {
                    min = iDivergence;
                    pivot = Vc;
                }
            }

            //check whether changed
            if (colAssigmnt[v] != pivot) {
                isChanged = true;
            }

            // move to position
            colCluster[pivot].add(v);
            colAssigmnt[v] = pivot;

            //record errors
            error += min;
        }

        return error;
    }

    /**
     * update column clusters, when the I-divergence is involved
     * 
     * @param points        the matrix contains the data
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param rowCluster    the row cluster
     * @param colCluster    the column cluster
     * @param rowAssigmnt   the assigment of row clusters
     * @param colAssigmnt   the assigment of column clusters
     * @param constraint    the constraints to retain
     * @param isChanged     the pivot to indicate whether the cocluster changes
     */
    protected static double updateColClusterForEuclidean(final SparseMatrix points, final int K,
                                                         final int L, final Cluster[] rowCluster,
                                                         final Cluster[] colCluster,
                                                         final int[] rowAssigmnt,
                                                         final int[] colAssigmnt,
                                                         final int constraint, boolean isChanged,
                                                         double[][] E_Uc_Vc, double[] E_U,
                                                         double[] E_Uc, double[] E_V, double[] E_Vc,
                                                         double[][] E_U_Vc, double[][] E_Uc_V,
                                                         double E) {
        int colCount = points.shape()[1];
        //clear current clusters
        for (Cluster local : colCluster) {
            local.clear();
        }

        //update column clusters
        double error = 0.0d;
        for (int v = 0; v < colCount; v++) {
            SparseVector ZvReal = points.getColRef(v);
            int[] itemList = ZvReal.indexList();
            if (itemList == null) {
                continue;
            }

            // find minimium position
            int pivot = -1;
            double min = Double.MAX_VALUE;
            for (int Vc = 0; Vc < L; Vc++) {
                double euDivergence = 0.0d;
                for (int u : itemList) {
                    int Uc = rowAssigmnt[u];

                    // compute Zuv w.r.t constraints
                    double ZuvReal = ZvReal.floatValue(u);
                    double ZuvEstim = 0.0d;
                    switch (constraint) {
                        case C_1:
                            ZuvEstim = E_Uc[Uc] + E_Vc[Vc] - E;
                            break;
                        case C_2:
                            ZuvEstim = E_Uc_Vc[Uc][Vc];
                            break;
                        case C_3:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] + E_U[u] - E_Uc[Uc];
                            break;
                        case C_4:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] + E_U[v] - E_Uc[Vc];
                            break;
                        case C_5:
                            ZuvEstim = E_Uc_Vc[Uc][Vc] + E_U[u] + E_V[v] - E_Uc[Uc] - E_Vc[Vc];
                            break;
                        case C_6:
                            ZuvEstim = E_U_Vc[u][Vc] + E_Uc_V[Uc][v] - E_Uc_Vc[Uc][Vc];
                            break;
                        default:
                            throw new RuntimeException("No contraints is choosed! ");
                    }
                    euDivergence += Distance.EW.divergence(ZuvReal, ZuvEstim);
                }

                if (min > euDivergence) {
                    min = euDivergence;
                    pivot = Vc;
                }
            }

            //check whether changed
            if (colAssigmnt[v] != pivot) {
                isChanged = true;
            }

            // move to position
            colCluster[pivot].add(v);
            colAssigmnt[v] = pivot;

            //update errors
            error += min;
        }

        return error;
    }

    /**
     * estimate the conditional expectation
     * 
     * @param points        the matrix contains the data
     * @param rowCluster    the row cluster
     * @param colCluster    the column cluster
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param E_Uc_Vc       the mean of cocluster
     * @param E_U           the mean of specified row
     * @param E_Uc          the mean of specified row cluster
     * @param E_V           the mean of specified column
     * @param E_Vc          the mean of specified column cluster
     */
    protected static void comConExpectation(final SparseMatrix points, final int K, final int L,
                                            final Cluster[] rowCluster, final Cluster[] colCluster,
                                            double[][] E_Uc_Vc, double[] E_U, double[] E_Uc,
                                            double[] E_V, double[] E_Vc) {
        int rowCount = points.shape()[0];
        int colCount = points.shape()[1];

        //1. cmp E_Uc_Vc
        for (int k = 0; k < K; k++) {
            for (int l = 0; l < L; l++) {
                Cluster rowLocal = rowCluster[k];
                Cluster colLocal = colCluster[l];

                for (int i : rowLocal) {
                    for (int j : colLocal) {
                        E_Uc_Vc[k][l] += points.getValue(i, j);
                    }
                }
                E_Uc_Vc[k][l] /= rowLocal.getList().size() * colLocal.getList().size();
            }
        }

        //2. cmp E_Uc
        for (int k = 0; k < K; k++) {
            Cluster rowLocal = rowCluster[k];
            for (int u : rowLocal) {
                E_Uc[k] += points.getRowRef(u).sum();

            }
            E_Uc[k] /= rowLocal.getList().size() * colCount;
        }

        //3. cmp E_U
        for (int u = 0; u < rowCount; u++) {
            E_U[u] = points.getRowRef(u).sum() / colCount;
        }

        //4. cmp E_Vc
        for (int l = 0; l < L; l++) {
            Cluster colLocal = colCluster[l];
            for (int v : colLocal) {
                E_Vc[l] += points.getColRef(v).sum();
            }
            E_Vc[l] /= colLocal.getList().size() * rowCount;
        }

        //5. cmp E_V
        for (int v = 0; v < colCount; v++) {
            E_V[v] = points.getColRef(v).sum() / rowCount;
        }
    }

    /**
     * estimate the conditional expectation
     * 
     * @param points        the matrix contains the data
     * @param rowCluster    the row cluster
     * @param colCluster    the column cluster
     * @param K             the number of row clusters
     * @param L             the number of column clusters
     * @param E_Uc_Vc       the mean of cocluster
     * @param E_U           the mean of specified row
     * @param E_Uc          the mean of specified row cluster
     * @param E_V           the mean of specified column
     * @param E_Vc          the mean of specified column cluster
     * @param E_U_Vc        the mean of specified row in cocluster
     * @param E_Uc_V        the mean of specified column in cocluster
     * @param E             the mean of the entire matrix
     */
    protected static void comConExpectationAsConjugateDist(final SparseMatrix points, final int K,
                                                           final int L, final Cluster[] rowCluster,
                                                           final Cluster[] colCluster,
                                                           double[][] E_Uc_Vc, double[] E_U,
                                                           double[] E_Uc, double[] E_V,
                                                           double[] E_Vc, double[][] E_U_Vc,
                                                           double[][] E_Uc_V) {
        int rowCount = points.shape()[0];
        int colCount = points.shape()[1];

        //1. cmp E_Uc_Vc
        for (int k = 0; k < K; k++) {
            for (int l = 0; l < L; l++) {
                int itemCount = 0;
                Cluster rowLocal = rowCluster[k];
                Cluster colLocal = colCluster[l];
                for (int i : rowLocal) {
                    for (int j : colLocal) {
                        double val = points.getValue(i, j);
                        if (val == 0.0d) {
                            continue;
                        }

                        E_Uc_Vc[k][l] += val;
                        itemCount++;
                    }
                }
                E_Uc_Vc[k][l] = (itemCount == 0) ? 0.0 : (E_Uc_Vc[k][l] / itemCount);
            }
        }

        //2. cmp E_Uc
        for (int k = 0; k < K; k++) {
            Cluster rowLocal = rowCluster[k];
            int itemCount = 0;
            for (int u : rowLocal) {
                E_Uc[k] += points.getRowRef(u).sum();
                itemCount += points.getRowRef(u).itemCount();

            }
            E_Uc[k] = (itemCount == 0) ? 0.0 : (E_Uc[k] / itemCount);
        }

        //3. cmp E_U
        for (int u = 0; u < rowCount; u++) {
            E_U[u] = points.getRowRef(u).average();
        }

        //4. cmp E_Vc
        for (int l = 0; l < L; l++) {
            Cluster colLocal = colCluster[l];
            int itemCount = 0;
            for (int v : colLocal) {
                E_Vc[l] += points.getColRef(v).sum();
                itemCount += points.getColRef(v).itemCount();
            }
            E_Vc[l] = (itemCount == 0) ? 0.0 : (E_Vc[l] / itemCount);
        }

        //5. cmp E_V
        for (int v = 0; v < colCount; v++) {
            E_V[v] = points.getColRef(v).average();
        }

        //6. cmp E_U_Vc
        for (int u = 0; u < rowCount; u++) {
            SparseVector Ru = points.getRowRef(u);
            for (int l = 0; l < L; l++) {
                Cluster colLocal = colCluster[l];

                int itemCount = 0;
                double sum = 0.0d;
                for (int v : colLocal) {
                    double val = Ru.floatValue(v);
                    if (val != 0.0d) {
                        sum += val;
                        itemCount++;
                    }
                }
                E_U_Vc[u][l] = sum / itemCount;
            }
        }

        //7. cmp E_Uc_V
        for (int v = 0; v < colCount; v++) {
            SparseVector Rv = points.getColRef(v);
            for (int k = 0; k < K; k++) {
                Cluster rowLocal = rowCluster[k];

                int itemCount = 0;
                double sum = 0.0d;
                for (int u : rowLocal) {
                    double val = Rv.floatValue(u);
                    if (val != 0.0d) {
                        sum += val;
                        itemCount++;
                    }
                }
                E_Uc_V[k][v] = sum / itemCount;
            }
        }
    }

    /** 
     * @see java.lang.Object#toString()
     */
    public static String info() {
        return "Cocluster";
    }
}
