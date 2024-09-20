/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib;

import com.oracle.coherence.hnswlib.exception.IndexAlreadyInitializedException;
import com.oracle.coherence.hnswlib.exception.IndexNotInitializedException;
import com.oracle.coherence.hnswlib.exception.ItemCannotBeInsertedIntoTheVectorSpaceException;
import com.oracle.coherence.hnswlib.exception.OnceIndexIsClearedItCannotBeReusedException;
import com.oracle.coherence.hnswlib.exception.UnableToCreateNewIndexInstanceException;
import com.oracle.coherence.hnswlib.exception.UnexpectedNativeException;
import com.sun.jna.Pointer;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents a small world index in the java context. This class includes some
 * abstraction to make the integration with the native library a bit more java
 * like and relies on the JNA implementation.
 * <p>
 * Each instance of index has a different memory context and should work
 * independently.
 */
public class Index implements Closeable
    {
    protected static final int NO_ID = -1;

    private static final int RESULT_SUCCESSFUL = 0;
    private static final int RESULT_QUERY_NO_RESULTS = 3;
    private static final int RESULT_ITEM_CANNOT_BE_INSERTED_INTO_THE_VECTOR_SPACE = 4;
    private static final int RESULT_ONCE_INDEX_IS_CLEARED_IT_CANNOT_BE_REUSED = 5;
    private static final int RESULT_INDEX_NOT_INITIALIZED = 8;

    private static final Hnswlib hnswlib = HnswlibFactory.getInstance();

    private Pointer reference;
    private volatile boolean initialized;
    private volatile boolean cleared;
    private volatile boolean referenceReused;
    private final SpaceName spaceName;
    private final int dimension;

    public Index(SpaceName spaceName, int dimension)
        {
        this.spaceName = spaceName;
        this.dimension = dimension;
        this.reference = hnswlib.createNewIndex(spaceName.toString(), dimension);
        if (reference == null)
            {
            throw new UnableToCreateNewIndexInstanceException();
            }
        }

    /**
     * Util function that normalizes an array.
     *
     * @param array input.
     */
    public static float[] normalize(float[] array)
        {
        int n = array.length;
        float norm = 0;
        for (float v : array)
            {
            norm += v * v;
            }
        norm = (float) (1.0f / (Math.sqrt(norm) + 1e-30f));
        for (int i = 0; i < n; i++)
            {
            array[i] = array[i] * norm;
            }
        return array;
        }

    /**
     * This method returns a ConcurrentIndex instance which contains the same
     * items present in a Index object. The indexes will share the same native
     * pointer, so there will be no memory duplication.
     * <p>
     * It is important to say that the Index class allows adding items is in
     * parallel, so the building time can be much slower. On the other hand,
     * ConcurrentIndex offers thread-safe methods for adding and querying, which
     * can be interesting for multi-threaded environment with online updates.
     * Via this method, you can get the best of the two worlds.
     *
     * @param index with the items added
     *
     * @return a thread-safe index
     */
    public static Index synchronizedIndex(Index index)
        {
        Index concurrentIndex = new ConcurrentIndex(index.spaceName, index.dimension);
        concurrentIndex.reference = index.reference;
        concurrentIndex.cleared = index.cleared;
        concurrentIndex.initialized = index.initialized;
        index.referenceReused = true;
        return concurrentIndex;
        }

    /**
     * This method initializes the index with the default values for the
     * parameters m, efConstruction, randomSeed and sets the maxNumberOfElements
     * to 1_000_000.
     * <p>
     * Note: not setting the maxNumberOfElements might lead to out of memory
     * issues and unpredictable behaviours in your application. Thus, use this
     * method wisely and combine it with monitoring.
     * <p>
     * For more information, please @see {link #initialize(int, int, int,
     * int)}.
     */
    public void initialize()
        {
        initialize(1_000_000);
        }

    /**
     * For more information, please @see {link #initialize(int, int, int,
     * int)}.
     *
     * @param maxNumberOfElements allowed in the index.
     */
    public void initialize(int maxNumberOfElements)
        {
        initialize(maxNumberOfElements, 16, 200, 100, false);
        }

    /**
     * Initialize the index to be used.
     *
     * @param maxNumberOfElements - max number of elements in the index;
     * @param m                   - M defines tha maximum number of outgoing
     *                            connections in the graph;
     * @param efConstruction      - ef parameter;
     * @param randomSeed          - a random seed specified by the user.
     * @param allowReplaceDeleted - enables replacing of deleted elements with
     *                            new added ones
     *
     * @throws IndexAlreadyInitializedException when a index reference was
     *                                          initialized before.
     * @throws UnexpectedNativeException        when something unexpected
     *                                          happened in the native side.
     */
    public void initialize(int maxNumberOfElements, int m, int efConstruction, int randomSeed, boolean allowReplaceDeleted)
        {
        if (initialized)
            {
            throw new IndexAlreadyInitializedException();
            }
        else
            {
            checkResultCode(hnswlib.initNewIndex(reference, maxNumberOfElements, m, efConstruction, randomSeed, allowReplaceDeleted));
            initialized = true;
            }
        }

    /**
     * Add an item without ID to the index. Internally, an incremental
     * identifier (starting from 1) will be given to this item.
     *
     * @param item - float array with the length expected by the index
     *             (dimension).
     */
    public void addItem(float[] item)
        {
        addItem(item, NO_ID, false);
        }

    /**
     * Add an item with ID to the index. It won't apply any extra normalization
     * unless it is required by the Vector Space (e.g., COSINE).
     *
     * @param item - float array with the length expected by the index
     *             (dimension);
     * @param id   - an identifier used by the native library.
     */
    public void addItem(float[] item, int id)
        {
        addItem(item, id, false);
        }

    /**
     * Add an item with ID to the index. It won't apply any extra normalization
     * unless it is required by the Vector Space (e.g., COSINE).
     *
     * @param item   item
     * @param id     id
     * @param replaceDeleted true to replace an item marked as deleted
     */
    public void addItem(float[] item, int id, boolean replaceDeleted)
        {
        checkResultCode(hnswlib.addItemToIndex(reference, item, id, replaceDeleted));
        }

    /**
     * Return the number of elements already inserted in the index.
     *
     * @return elements count.
     */
    public int getLength()
        {
        return hnswlib.getIndexLength(reference);
        }

    /**
     * Return the maximum number of elements that can be inserted into the index.
     *
     * @return number of items that can be inserted into the index.
     */
    public int getMaxLength()
        {
        return hnswlib.getMaxIndexLength(reference);
        }

    /**
     * Resize the index.
     *
     * @param maxSize  the new maximum size to resize the index to
     */
    void resize(int maxSize)
        {
        hnswlib.resizeIndex(reference, maxSize);
        }

    /**
     * Performs a knn query in the index instance. In case the vector space
     * requires the input to be normalized, it will normalize at the native
     * level.
     *
     * @param input - float array;
     * @param k     - number of results expected.
     *
     * @return a query tuple instance that contain the indices and coefficients.
     */
    public QueryTuple knnQuery(float[] input, int k)
        {
        return knnQuery(input, k, null);
        }

    /**
     * Performs a knn query in the index instance. In case the vector space
     * requires the input to be normalized, it will normalize at the native
     * level.
     *
     * @param input  - float array;
     * @param k      - number of results expected.
     * @param filter - an optional filter to filter the query results
     *
     * @return a query tuple instance that contain the indices and coefficients.
     */
    public QueryTuple knnQuery(float[] input, int k, Hnswlib.QueryFilter filter)
        {
        int length = getLength();
        if (length == 0)
            {
            checkIndexIsInitialized();
            return QueryTuple.EMPTY;
            }

        int        kActual    = Math.min(k, length);
        QueryTuple queryTuple = new QueryTuple(kActual);

        if (filter == null)
            {
            boolean empty = checkResultCode(hnswlib.knnQuery(reference, input, kActual, queryTuple.ids, queryTuple.coefficients));
            if (empty)
                {
                return QueryTuple.EMPTY;
                }
            }
        else
            {
            CapturingFilter capturingFilter = new CapturingFilter(kActual, filter);
            boolean empty = checkResultCode(hnswlib.knnFilterQuery(reference, input, kActual, capturingFilter, queryTuple.ids, queryTuple.coefficients));
            if (empty)
                {
                if (capturingFilter.count == 0)
                    {
                    return QueryTuple.EMPTY;
                    }
                else
                    {
                    int[] ids = capturingFilter.ids;
                    queryTuple = new QueryTuple(ids);
                    queryTuple.count(capturingFilter.count);
                    float[] coefficients = queryTuple.coefficients;
                    for (int i = 0; i < capturingFilter.count; i++)
                        {
                        float[] vector = new float[dimension];
                        hnswlib.getData(reference, ids[i], vector, dimension);
                        coefficients[i] = computeSimilarity(input, vector);
                        }
                    }
                }
            }
        return queryTuple;
        }

    /**
     * Stores the content of the index into a file. This method relies on the
     * native implementation.
     *
     * @param path - destination path.
     */
    public void save(Path path)
        {
        checkResultCode(hnswlib.saveIndexToPath(reference, path.toAbsolutePath().toString()));
        }

    /**
     * This method loads the content stored in a file path onto the index.
     * <p>
     * Note: if the index was previously initialized, the old content will be
     * erased.
     *
     * @param path                - path to the index file;
     * @param maxNumberOfElements - max number of elements in the index.
     */
    public void load(Path path, int maxNumberOfElements)
        {
        checkResultCode(hnswlib.loadIndexFromPath(reference, maxNumberOfElements, path.toAbsolutePath().toString()));
        }

    /**
     * Free the memory allocated for this index in the native context.
     * <p>
     * NOTE: Once the index is cleared, it cannot be initialized or used again.
     */
    public void clear()
        {
        checkResultCode(hnswlib.clearIndex(reference));
        cleared = true;
        }

    /**
     * Cleanup the area allocated by the index in the native side.
     */
    @Override
    public void close()
        {
        if (!cleared && !referenceReused)
            {
            this.clear();
            }
        }

    /**
     * Cleanup the area allocated by the index in the native side.
     *
     * @throws Throwable when anything weird happened. :)
     */
    @SuppressWarnings("removal")
    @Override
    protected void finalize() throws Throwable
        {
        close();
        super.finalize();
        }

    /**
     * This method checks the result code coming from the native execution is
     * correct otherwise throws an exception.
     *
     * @throws UnexpectedNativeException when something went out of control in
     *                                   the native side.
     */
    private boolean checkResultCode(int resultCode)
        {
        switch (resultCode)
            {
            case RESULT_SUCCESSFUL ->
                {
                return false;
                }
            case RESULT_QUERY_NO_RESULTS ->
                {
                return true;
                }
            case RESULT_ITEM_CANNOT_BE_INSERTED_INTO_THE_VECTOR_SPACE ->
                    throw new ItemCannotBeInsertedIntoTheVectorSpaceException();
            case RESULT_ONCE_INDEX_IS_CLEARED_IT_CANNOT_BE_REUSED ->
                    throw new OnceIndexIsClearedItCannotBeReusedException();
            case RESULT_INDEX_NOT_INITIALIZED ->
                    throw new IndexNotInitializedException();
            default -> throw new UnexpectedNativeException();
            }
        }

    /**
     * Checks whether there is an item with the specified identifier in the
     * index.
     *
     * @param id - identifier.
     *
     * @return true or false.
     */
    public boolean hasId(int id)
        {
        return hnswlib.hasId(reference, id) == RESULT_SUCCESSFUL;
        }

    /**
     * Gets the data from a specific identifier in the index.
     *
     * @param id - identifier.
     *
     * @return an optional containing or not the
     */
    public Optional<float[]> getData(int id)
        {
        float[] vector = new float[dimension];
        int success = hnswlib.getData(reference, id, vector, dimension);
        if (success == RESULT_SUCCESSFUL)
            {
            return Optional.of(vector);
            }
        return Optional.empty();
        }

    /**
     * Computer similarity on the native side taking advantage of SSE, AVX, SIMD
     * instructions, when available.
     *
     * @param vector1 array with correct dimension;
     * @param vector2 array with correct dimension.
     *
     * @return the similarity score.
     */
    public float computeSimilarity(float[] vector1, float[] vector2)
        {
        checkIndexIsInitialized();
        return hnswlib.computeSimilarity(reference, vector1, vector2);
        }

    /**
     * Retrieves the current M value.
     *
     * @return the M value.
     */
    public int getM()
        {
        checkIndexIsInitialized();
        return hnswlib.getM(reference);
        }

    /**
     * Retrieves the current Ef value.
     *
     * @return the EF value.
     */
    public int getEf()
        {
        checkIndexIsInitialized();
        return hnswlib.getEf(reference);
        }

    /**
     * Sets the query time accuracy / speed trade-off value.
     *
     * @param ef value.
     */
    public void setEf(int ef)
        {
        checkResultCode(hnswlib.setEf(reference, ef));
        }

    /**
     * Retrieves the current ef construction.
     *
     * @return the ef construction value.
     */
    public int getEfConstruction()
        {
        checkIndexIsInitialized();
        return hnswlib.getEfConstruction(reference);
        }

    /**
     * Marks an ID as deleted.
     *
     * @param id identifier.
     */
    public void markDeleted(int id)
        {
        checkResultCode(hnswlib.markDeleted(reference, id));
        }

    private void checkIndexIsInitialized()
        {
        if (!initialized)
            {
            throw new IndexNotInitializedException();
            }
        }

    protected static class CapturingFilter
            implements Hnswlib.QueryFilter
        {
        private final int max;

        private final int[] ids;

        private final Hnswlib.QueryFilter delegate;

        private int count;

        public CapturingFilter(int k, Hnswlib.QueryFilter delegate)
            {
            this.max = k;
            this.ids = new int[k];
            this.delegate = delegate;
            this.count = 0;
            }

        @Override
        public boolean filter(int id)
            {
            boolean match = delegate.filter(id);
            if (match && count < max)
                {
                ids[count++] = id;
                }
            return match;
            }
        }
    }
