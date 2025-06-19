/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

/**
 * Interface that implements JNA (Java Native Access) to Hnswlib a fast
 * approximate nearest neighbor search library available on
 * (https://github.com/nmslib/hnswlib). This implementation was created in order
 * to provide a high performance also in Java (not only in Python or C++).
 * <p>
 * This implementation relies also in a dynamic library generated from the
 * sources available in bindings.cpp.
 */
public interface Hnswlib
        extends Library
    {
    String LIBRARY_NAME = "hnswlib-" + Platform.ARCH;

    String JNA_LIBRARY_PATH_PROPERTY = "jna.library.path";

    /**
     * Allocates memory for the index in the native context and stores the
     * address in a JNA Pointer variable.
     *
     * @param spaceName - use: l2, ip or cosine strings only;
     * @param dimension - length of the vectors used for indexation.
     *
     * @return the index reference pointer.
     */
    Pointer createNewIndex(String spaceName, int dimension);

    /**
     * Initialize the index with information needed for the indexation.
     *
     * @param index               - JNA pointer reference of the index;
     * @param maxNumberOfElements - max number of elements in the index;
     * @param m                   - M defines tha maximum number of outgoing
     *                            connections in the graph;
     * @param efConstruction      - ef parameter;
     * @param randomSeed          - a random seed specified by the user.
     * @param allowReplaceDeleted - enables replacing of deleted elements with
     *                            new added ones
     *
     * @return a result code.
     */
    int initNewIndex(Pointer index, int maxNumberOfElements, int m, int efConstruction, int randomSeed, boolean allowReplaceDeleted);

    /**
     * Add an item to the index.
     *
     * @param item       - array containing the input to be inserted into the
     *                   index;
     * @param id         - an identifier to be used for this entry;
     * @param index      - JNA pointer reference of the index.
     *
     * @return a result code.
     */
    int addItemToIndex(Pointer index, float[] item, int id, boolean replaceDeleted);

    /**
     * Retrieve the number of elements already inserted into the index.
     *
     * @param index - JNA pointer reference of the index.
     *
     * @return number of items in the index.
     */
    int getIndexLength(Pointer index);

    /**
     * Retrieve the maximum number of elements that can be inserted into the index.
     *
     * @param index - JNA pointer reference of the index.
     *
     * @return number of items that can be inserted into the index.
     */
    int getMaxIndexLength(Pointer index);

    /**
     * Retrieve the index size in bytes.
     *
     * @param index - JNA pointer reference of the index.
     *
     * @return the index size in bytes.
     */
    int getIndexSize(Pointer index);

    /**
     * Resize the index.
     *
     * @param index    JNA pointer reference of the index
     * @param maxSize  the new maximum size to resize the index to
     */
    void resizeIndex(Pointer index, int maxSize);

    /**
     * Save the content of an index into a file (using native implementation).
     *
     * @param index - JNA pointer reference of the index.
     * @param path  - path where the index will be stored.
     *
     * @return a result code.
     */
    int saveIndexToPath(Pointer index, String path);

    /**
     * Restore the content of an index saved into a file (using native
     * implementation).
     *
     * @param index               - JNA pointer reference of the index;
     * @param maxNumberOfElements - max number of items to be inserted into the
     *                            index;
     * @param path                - path where the index will be stored.
     *
     * @return a result code.
     */
    int loadIndexFromPath(Pointer index, int maxNumberOfElements, String path);

    /**
     * This function invokes the knnFilterQuery available in the hnswlib native
     * library.
     *
     * @param index        JNA pointer reference of the index;
     * @param input        input used for the query;
     * @param k            dimension used for the query;
     * @param filter       query filter to apply
     * @param indices      [output] retrieves the indices returned by the
     *                     query;
     * @param coefficients [output] retrieves the coefficients returned by the
     *                     query.
     *
     * @return a result code.
     */
    int knnFilterQuery(Pointer index, float[] input, int k, QueryFilter filter, int[] indices, float[] coefficients);

    /**
     * This function invokes the knnQuery available in the hnswlib native
     * library.
     *
     * @param index        JNA pointer reference of the index;
     * @param input        input used for the query;
     * @param k            dimension used for the query;
     * @param indices      [output] retrieves the indices returned by the
     *                     query;
     * @param coefficients [output] retrieves the coefficients returned by the
     *                     query.
     *
     * @return a result code.
     */
    int knnQuery(Pointer index, float[] input, int k, int[] indices, float[] coefficients);

    /**
     * Clear the index from the memory.
     *
     * @param index - JNA pointer reference of the index.
     *
     * @return a result code.
     */
    int clearIndex(Pointer index);

    /**
     * Sets the query time accuracy / speed trade-off value.
     *
     * @param index - JNA pointer reference of the index;
     * @param ef    value.
     *
     * @return a result code.
     */
    int setEf(Pointer index, int ef);

    /**
     * Populate vector with data for given id
     *
     * @param index  index
     * @param id     id
     * @param vector vector
     * @param dim    dimension
     *
     * @return result code
     */
    int getData(Pointer index, int id, float[] vector, int dim);

    /**
     * Determine whether the index contains data for given id.
     *
     * @param index index
     * @param id    id
     *
     * @return result_code
     */
    int hasId(Pointer index, int id);

    /**
     * Compute similarity between two vectors
     *
     * @param index   index
     * @param vector1 vector1
     * @param vector2 vector2
     *
     * @return similarity score between vectors
     */
    float computeSimilarity(Pointer index, float[] vector1, float[] vector2);

    /**
     * Retrieves the value of M.
     *
     * @param index reference.
     *
     * @return value of M.
     */
    int getM(Pointer index);

    /**
     * Retrieves the current ef construction value.
     *
     * @param index reference.
     *
     * @return efConstruction value.
     */
    int getEfConstruction(Pointer index);

    /**
     * Retrieves the current ef value.
     *
     * @param index reference.
     *
     * @return EF value.
     */
    int getEf(Pointer index);

    /**
     * Marks an item ID as deleted.
     *
     * @param index reference;
     * @param id    label.
     *
     * @return a result code.
     */
    int markDeleted(Pointer index, int id);

    /**
     * A filter class that can determine whether a specified identifier
     * should be returned in a HNSW search query.
     */
    interface QueryFilter
            extends Callback
        {
        /**
         * Return {@code true} if the specified {@code id}
         * can be returned in the search results.
         *
         * @param id  the {@code id} to test
         *
         * @return {@code true} if the specified {@code id}
         *         can be returned in the search results
         */
        boolean filter(int id);
        }
    }
