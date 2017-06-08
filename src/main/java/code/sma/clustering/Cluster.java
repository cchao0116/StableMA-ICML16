/**
 * Tongji Edu.
 * Copyright (c) 2004-2014 All Rights Reserved.
 */
package code.sma.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.primitives.Ints;

import code.sma.core.impl.SparseMatrix;
import code.sma.core.impl.SparseVector;

/**
 * The cluster in K-mean with the matrix index
 * 
 * @author Hanke Chen
 * @version $Id: Cluster.java, v 0.1 2014-10-14 上午11:26:37 chench Exp $
 */
public final class Cluster implements Iterable<Integer> {

    /** the list with matrix index */
    private Set<Integer> elements;
    /** the centroid of the cluster */
    private SparseVector centroid;

    /** 
     * Construction
     */
    public Cluster() {
        elements = new HashSet<Integer>();
    }

    /**
     * divide a element to this cluster
     * 
     * @param index the matrix index to add
     */
    public void add(int index) {
        elements.add(index);
    }

    /**
     * Removes the specified element from this set if it is present
     * 
     * @param index
     */
    public void remove(int index) {
        elements.remove(index);
    }

    /**
     * Returns the number of elements in this set
     * 
     * @return the number of elements in this set (its cardinality)
     */
    public int size() {
        return elements.size();
    }

    /**
     * clear all the index in this cluster
     */
    public void clear() {
        elements.clear();
    }

    /**
     * Returns true if this list contains no elements
     * 
     * @return
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * return the matrix index list
     * 
     * @return
     */
    public List<Integer> getList() {
        return new ArrayList<Integer>(elements);
    }

    /**
     * calculate the centroid in the cluster
     * 
     * @param matrix
     * @return
     */
    public SparseVector centroid(SparseMatrix matrix) {
        if (elements.isEmpty()) {
            return null;
        }

        SparseVector result = new SparseVector(matrix.shape()[1]);
        for (Integer memberId : elements) {
            SparseVector a = matrix.getRowRef(memberId);
            result = result.plus(a);
        }
        centroid = result.scale(1.0 / elements.size());
        return centroid;
    }

    /**
     * fetch the centroid in the cluster.
     * Note that if the centroid existed, the centroid will not be updated.
     * 
     * @param matrix
     * @return
     */
    public SparseVector fetchCentroid(SparseMatrix matrix) {
        if (centroid != null) {
            return centroid;
        } else {
            return centroid(matrix);
        }
    }

    /** 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Integer> iterator() {
        return elements.iterator();
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Arrays.toString(Ints.toArray(elements));
    }

}
