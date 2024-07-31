/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

/**
 * An algorithm that can calculate distance to a given vector.
 *
 * @param <T>  the type of the vector
 */
public interface DistanceAlgorithm<T>
    {
    /**
     * Calculate the distance between the specified vectors.
     *
     * @param v1 the vector to calculate the distance from
     * @param v2 the vector to calculate the distance to
     *
     * @return the distance between the specified vectors
     */
    double distance(Vector<T> v1, Vector<T> v2);
    }
