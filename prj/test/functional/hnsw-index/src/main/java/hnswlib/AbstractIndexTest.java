/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package hnswlib;

import com.oracle.coherence.hnswlib.Hnswlib;
import com.oracle.coherence.hnswlib.Index;
import com.oracle.coherence.hnswlib.QueryTuple;
import com.oracle.coherence.hnswlib.SpaceName;
import com.oracle.coherence.hnswlib.exception.IndexAlreadyInitializedException;
import com.oracle.coherence.hnswlib.exception.IndexNotInitializedException;
import com.oracle.coherence.hnswlib.exception.OnceIndexIsClearedItCannotBeReusedException;
import com.oracle.coherence.hnswlib.exception.UnexpectedNativeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("resource")
public abstract class AbstractIndexTest
    {

    protected abstract Index createIndexInstance(SpaceName spaceName, int dimensions);

    @BeforeAll
    static void setup()
        {
        System.clearProperty(Hnswlib.JNA_LIBRARY_PATH_PROPERTY);
        }

    @Test
    public void testSingleIndexInstantiation() throws UnexpectedNativeException
        {
        Index i1 = createIndexInstance(SpaceName.IP, 30);
        assertNotNull(i1);
        i1.clear();
        }

    @Test
    public void testMultipleIndexInstantiation()
            throws UnexpectedNativeException
        {
        Index i1 = createIndexInstance(SpaceName.IP, 30);
        assertNotNull(i1);
        Index i2 = createIndexInstance(SpaceName.COSINE, 30);
        assertNotNull(i2);
        Index i3 = createIndexInstance(SpaceName.L2, 30);
        assertNotNull(i3);
        i1.clear();
        i2.clear();
        i3.clear();
        }

    @Test
    public void testIndexInitialization() throws UnexpectedNativeException
        {
        Index i1 = createIndexInstance(SpaceName.COSINE, 50);
        i1.initialize(500_000, 16, 200, 100, true);
        assertEquals(0, i1.getLength());
        i1.clear();
        }

    @Test
    public void testIndexInitialization2() throws UnexpectedNativeException
        {
        Index i1 = createIndexInstance(SpaceName.COSINE, 50);
        i1.initialize();
        assertEquals(0, i1.getLength());
        i1.clear();
        }

    @Test
    public void testIndexMultipleInitialization()
            throws UnexpectedNativeException
        {
        assertThrows(IndexAlreadyInitializedException.class, () ->
            {
            Index i1 = createIndexInstance(SpaceName.COSINE, 50);
            i1.initialize(500_000, 16, 200, 100, true);
            i1.initialize();
            });
        }

    @Test
    public void testIndexAddItem() throws UnexpectedNativeException
        {
        Index i1 = createIndexInstance(SpaceName.COSINE, 3);
        i1.initialize(1);
        i1.addItem(new float[] {1.3f, 1.2f, 1.5f}, 3);
        assertEquals(1, i1.getLength());
        i1.clear();
        }

    @Test
    public void testIndexAddItemIndependence() throws UnexpectedNativeException
        {
        testIndexAddItem();
        Index i2 = createIndexInstance(SpaceName.IP, 4);
        i2.initialize(3);
        assertEquals(0, i2.getLength());
        i2.clear();
        }

    @Test
    public void testIndexSaveAndLoad()
            throws UnexpectedNativeException, IOException
        {
        File tempFile = File.createTempFile("index", "sm");
        Path tempFilePath = Paths.get(tempFile.getAbsolutePath());

        Index i1 = createIndexInstance(SpaceName.COSINE, 3);
        i1.initialize(1);
        i1.addItem(new float[] {1.3f, 1.2f, 1.5f}, 3);
        i1.save(tempFilePath);
        i1.clear();

        Index i2 = createIndexInstance(SpaceName.COSINE, 3);
        assertEquals(0, i2.getLength());
        i2.load(tempFilePath, 1);
        assertEquals(1, i2.getLength());
        i2.clear();

        assertTrue(tempFile.delete());
        }

    @Test
    public void testParallelAddItemsInMultipleIndexes()
            throws InterruptedException, UnexpectedNativeException
        {
        int cpus = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(cpus);

        Index i1 = createIndexInstance(SpaceName.L2, 50);
        i1.initialize(1_050);

        Index i2 = createIndexInstance(SpaceName.COSINE, 50);
        i2.initialize(1_050);

        Runnable addItemIndex1 = () ->
            {
            try
                {
                i1.addItem(HnswlibTestUtils.getRandomFloatArray(50));
                }
            catch (UnexpectedNativeException e)
                {
                e.printStackTrace();
                }
            };
        Runnable addItemIndex2 = () ->
            {
            try
                {
                i2.addItem(HnswlibTestUtils.getRandomFloatArray(50));
                }
            catch (UnexpectedNativeException e)
                {
                e.printStackTrace();
                }
            };

        for (int i = 0; i < 1_000; i++)
            {
            executorService.submit(addItemIndex1);
            executorService.submit(addItemIndex2);
            }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertEquals(1_000, i1.getLength());
        assertEquals(1_000, i2.getLength());

        i1.clear();
        i2.clear();
        }

    @Test
    public void testConcurrentInsertQuery()
            throws InterruptedException, UnexpectedNativeException
        {
        ExecutorService executorService = Executors.newFixedThreadPool(50);

        Index i1 = createIndexInstance(SpaceName.L2, 50);
        i1.initialize(1_050);

        float[] randomFloatArray = HnswlibTestUtils.getRandomFloatArray(50);

        Runnable addItemIndex1 = () ->
            {
            try
                {
                i1.addItem(randomFloatArray);
                }
            catch (UnexpectedNativeException e)
                {
                e.printStackTrace();
                }
            };

        Runnable queryItemIndex1 = () ->
            {
            QueryTuple queryTuple;
            try
                {
                queryTuple = i1.knnQuery(randomFloatArray, 1);
                assertEquals(50, queryTuple.getIds().length);
                assertEquals(50, queryTuple.getCoefficients().length);
                }
            catch (UnexpectedNativeException e)
                {
                e.printStackTrace();
                }
            };

        for (int i = 0; i < 1_000; i++)
            {
            executorService.submit(addItemIndex1);
            executorService.submit(queryItemIndex1);
            }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertEquals(1_000, i1.getLength());
        i1.clear();
        }

    @Test
    public void testQueryEmptyException() throws UnexpectedNativeException
        {
        Index idx = createIndexInstance(SpaceName.COSINE, 3);
        idx.initialize(300);
        QueryTuple queryTuple = idx.knnQuery(new float[] {1.3f, 1.4f, 1.5f}, 3);
        assertTrue(queryTuple.empty());
        }

    @Test
    public void testOverwritingAnItemInTheModel()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.COSINE, 4);
        index.initialize(5);

        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f}, 1);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 0.95f}, 2);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 0.9f}, 3);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 0.85f}, 4);

        QueryTuple queryTuple = index.knnQuery(new float[] {1.0f, 1.0f, 1.0f, 1.0f}, 3);
        assertEquals(1, queryTuple.getIds()[0]);
        assertEquals(2, queryTuple.getIds()[1]);
        assertEquals(3, queryTuple.getIds()[2]);

        index.addItem(new float[] {0.0f, 0.0f, 0.0f, 0.0f}, 2);
        queryTuple = index.knnQuery(new float[] {1.0f, 1.0f, 1.0f, 1.0f}, 3);
        assertEquals(1, queryTuple.getIds()[0]);
        assertEquals(3, queryTuple.getIds()[1]);
        assertEquals(4, queryTuple.getIds()[2]);

        index.clear();
        }

    @Test
    public void testIncludingMoreItemsThanPossible()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.L2, 4);
        index.initialize(2);

        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f}, 1);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 0.95f}, 2);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 0.9f}, 3);

        assertEquals(3, index.getLength());
        assertEquals(4, index.getMaxLength());
        }

    @Test
    public void testHostNormalization()
        {
        float[] item1 = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        Index.normalize(item1);
        assertArrayEquals(new float[] {0.3779645f, 0.3779645f, 0.3779645f, 0.3779645f, 0.3779645f, 0.3779645f, 0.3779645f}, item1, 0.000001f);
        }

    @Test
    public void testIndexCosineEqualsToIPWhenNormalized()
            throws UnexpectedNativeException
        {
        float[] i1 = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        Index.normalize(i1);
        float[] i2 = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f};
        Index.normalize(i2);
        float[] i3 = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f};
        Index.normalize(i3);

        Index indexCosine = createIndexInstance(SpaceName.COSINE, 7);
        indexCosine.initialize(3);
        indexCosine.addItem(i1, 1_111_111);
        indexCosine.addItem(i2, 1_222_222);
        indexCosine.addItem(i3, 1_333_333);

        Index indexIP = createIndexInstance(SpaceName.IP, 7);
        indexIP.initialize(3);
        indexIP.addItem(i1, 1_111_111);
        indexIP.addItem(i2, 1_222_222);
        indexIP.addItem(i3, 1_333_333);

        float[] input = new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        Index.normalize(input);

        QueryTuple cosineQT = indexCosine.knnQuery(input, 3);
        QueryTuple ipQT = indexCosine.knnQuery(input, 3);

        assertArrayEquals(cosineQT.getCoefficients(), ipQT.getCoefficients(), 0.000001f);
        assertArrayEquals(cosineQT.getIds(), ipQT.getIds());

        indexIP.clear();
        indexCosine.clear();
        }

    @Test
    public void testSimpleQueryOf5ElementsAndDimension7IP()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.IP, 7);
        index.initialize(7);

        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}, 5);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}, 6);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}, 7);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.85f}, 8);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f}, 9);

        float[] input = new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        QueryTuple ipQT = index.knnQuery(input, 4);

        assertArrayEquals(new int[] {5, 6, 7, 8}, ipQT.getIds());
        assertArrayEquals(new float[] {-6.0f, -5.95f, -5.9f, -5.85f}, ipQT.getCoefficients(), 0.000001f);
        index.clear();
        }

    @Test
    public void testQueryOnEmptyIndex() throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.IP, 7);
        index.initialize(7);

        float[] input = new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        QueryTuple ipQT = index.knnQuery(input, 4);

        assertTrue(ipQT.empty());
        assertEquals(0, ipQT.count());
        index.clear();
        }

    @Test
    public void testSimpleQueryWhereIndexHasFewerElements()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.IP, 7);
        index.initialize(7);

        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}, 5);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}, 6);

        float[] input = new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        QueryTuple ipQT = index.knnQuery(input, 4);

        assertFalse(ipQT.empty());
        assertEquals(2, ipQT.count());
        assertArrayEquals(new int[] {5, 6}, ipQT.getIds());
        assertArrayEquals(new float[] {-6.0f, -5.95f}, ipQT.getCoefficients(), 0.000001f);
        index.clear();
        }

    @Test
    public void testSimpleQueryOf5ElementsAndDimension7Cosine()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.COSINE, 7);
        index.initialize(7);

        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}), 14);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}), 13);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}), 12);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.85f}), 11);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f}), 10);

        float[] input = Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f});
        QueryTuple ipQT = index.knnQuery(input, 4);

        assertArrayEquals(new int[] {14, 13, 12, 11}, ipQT.getIds());
        assertArrayEquals(new float[] {-2.3841858E-7f, 1.552105E-4f, 6.2948465E-4f, 0.001435399f}, ipQT.getCoefficients(), 0.000001f);
        index.clear();
        }

    @Test
    public void testSimpleQueryOf5ElementsAndDimension7CosineWithFilter()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.COSINE, 7);
        index.initialize(7);

        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}), 14);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}), 13);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}), 12);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.85f}), 11);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f}), 10);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.75f}), 9);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.7f}), 8);

        // filter to allow only even id values
        Hnswlib.QueryFilter filter = id -> id % 2 == 0;

        float[] input = Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f});
        QueryTuple ipQT = index.knnQuery(input, 4, filter);

        assertArrayEquals(new int[] {14, 12, 10, 8}, ipQT.getIds());
        assertArrayEquals(new float[] {-2.3841858E-7f, 6.2948465E-4f, 0.0025850534f, 0.005960822f}, ipQT.getCoefficients(), 0.000001f);
        index.clear();
        }

    @Test
    public void testSimpleQueryOf5ElementsAndDimension7CosineWithFilterNoneMatch()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.COSINE, 7);
        index.initialize(7);

        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}), 14);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}), 13);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}), 12);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.85f}), 11);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f}), 10);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.75f}), 9);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.7f}), 8);

        // filter to allow only even id values
        Hnswlib.QueryFilter filter = id -> false;

        float[] input = Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f});
        QueryTuple ipQT = index.knnQuery(input, 4, filter);

        assertTrue(ipQT.empty());
        assertEquals(0, ipQT.count());
        index.clear();
        }

    @Test
    public void testSimpleQueryOf5ElementsAndDimension7CosineWithFilterWithFewerMatches()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.COSINE, 7);
        index.initialize(7);

        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}), 14);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}), 13);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}), 12);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.85f}), 11);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f}), 10);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.75f}), 9);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.7f}), 8);

        // filter to allow only even id values
        Hnswlib.QueryFilter filter = id -> id % 2 == 0 && id != 12;

        float[] input = Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f});
        QueryTuple ipQT = index.knnQuery(input, 4, filter);

        assertEquals(3, ipQT.count());
        assertArrayEquals(new int[] {14, 10, 8, 0}, ipQT.getIds());
        assertArrayEquals(new float[] {-2.3841858E-7f, 0.0025850534f, 0.005960822f, 0.0f}, ipQT.getCoefficients(), 0.000001f);
        index.clear();
        }

    @Test
    public void testSimpleQueryOf5ElementsAndDimension7L2()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.L2, 7);
        index.initialize(7);

        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}, 48);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.85f}, 10);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}, 35);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f}, 1);
        index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}, 33);

        float[] input = new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        QueryTuple ipQT = index.knnQuery(input, 4);

        assertArrayEquals(new int[] {33, 35, 48, 10}, ipQT.getIds());
        assertArrayEquals(new float[] {0.0f, 0.002500001f, 0.010000004f, 0.022499993f}, ipQT.getCoefficients(), 0.000001f);
        index.clear();
        }

    @Test
    public void testDoubleClear() throws UnexpectedNativeException
        {
        assertThrows(OnceIndexIsClearedItCannotBeReusedException.class, () ->
            {
            Index idx = createIndexInstance(SpaceName.IP, 30);
            idx.initialize(3);
            idx.clear();
            idx.clear();
            });
        }

    @Test
    public void testUsageAfterClear1() throws UnexpectedNativeException
        {
        assertThrows(OnceIndexIsClearedItCannotBeReusedException.class, () ->
            {
            Index idx = createIndexInstance(SpaceName.IP, 30);
            idx.clear();
            idx.initialize(30);
            });
        }

    @Test
    public void testUsageAfterClear2() throws UnexpectedNativeException
        {
        assertThrows(OnceIndexIsClearedItCannotBeReusedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.IP, 30);
            index.initialize(30);
            index.clear();
            index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}, 48);
            });
        }

    @Test
    public void testUsageAfterClear3() throws UnexpectedNativeException
        {
        assertThrows(OnceIndexIsClearedItCannotBeReusedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.IP, 30);
            index.initialize(30);
            index.addItem(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}, 48);
            index.clear();
            float[] input = new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
            if (index.getLength() == 0)
                {
                // we force the test to pass if the length is returned as zero because the
                // knnQuery method will short-cut and return immediately
                throw new OnceIndexIsClearedItCannotBeReusedException();
                }
            index.knnQuery(input, 4);
            });
        }

    @Test
    public void testTryingDoubleClearDueToGCWhenReferenceIsLost()
            throws UnexpectedNativeException
        {
        Index index = createIndexInstance(SpaceName.IP, 30);
        index.initialize(30);
        index.clear();
        index = createIndexInstance(SpaceName.IP, 30);
        int counter = 10;
        while (counter-- > 0)
            {
            System.gc();
            }
        index.initialize(30);
        assertNotNull(index);
        }

    @Test
    public void testGetData()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 3);
        index.initialize();
        float[] vector = {1F, 2F, 3F};
        index.addItem(vector);
        assertTrue(index.hasId(0));
        Optional<float[]> data = index.getData(0);
        assertTrue(data.isPresent());
        assertArrayEquals(vector, data.get(), 0.0f);
        assertFalse(index.hasId(1));
        assertFalse(index.getData(1).isPresent());

        float[] vector2 = {1F, 2F, 3F};
        index.addItem(vector2, 1230);
        assertTrue(index.hasId(1230));
        assertFalse(index.hasId(1231));

        index.clear();
        assertFalse(index.hasId(1230));
        assertFalse(index.hasId(1231));
        }

    @Test
    public void testGetDataWhenIndexCleared()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 3);
        index.initialize();
        index.clear();
        assertFalse(index.hasId(1202));
        Index index2 = createIndexInstance(SpaceName.COSINE, 1);
        assertFalse(index2.hasId(123));
        }

    @Test
    public void testUseAddItemIndexWithoutInitialize()
        {
        assertThrows(IndexNotInitializedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.COSINE, 1);
            index.addItem(new float[1]);
            });
        }

    @Test
    public void testUseKnnQueryIndexWithoutInitialize()
        {
        assertThrows(IndexNotInitializedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.COSINE, 1);
            index.knnQuery(new float[1], 1);
            });
        }

    @Test
    public void testGetMWithoutInitializeIndex()
        {
        assertThrows(IndexNotInitializedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.COSINE, 1);
            index.getM();
            });
        }

    @Test
    public void testGetEfWithoutInitializeIndex()
        {
        assertThrows(IndexNotInitializedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.COSINE, 1);
            index.getEf();
            });
        }

    @Test
    public void testGetEfConstructionWithoutInitializeIndex()
        {
        assertThrows(IndexNotInitializedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.COSINE, 1);
            index.getEfConstruction();
            });
        }

    @Test
    public void testMarkAsDeleted()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 7);
        index.initialize(7);

        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}), 14);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.95f}), 13);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.9f}), 12);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.85f}), 11);
        index.addItem(Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f}), 10);

        float[] input = Index.normalize(new float[] {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f});
        QueryTuple ipQT = index.knnQuery(input, 4);

        assertArrayEquals(new int[] {14, 13, 12, 11}, ipQT.getIds());
        assertArrayEquals(new float[] {-2.3841858E-7f, 1.552105E-4f, 6.2948465E-4f, 0.001435399f}, ipQT.getCoefficients(), 0.000001f);

        index.markDeleted(13);
        QueryTuple ipQT2 = index.knnQuery(input, 4);
        assertArrayEquals(new int[] {14, 12, 11, 10}, ipQT2.getIds());
        assertArrayEquals(new float[] {-2.3841858E-7f, 6.2948465E-4f, 0.001435399f, 0.0025851727f}, ipQT2.getCoefficients(), 0.000001f);

        index.markDeleted(12);
        QueryTuple ipQT3 = index.knnQuery(input, 3);
        assertArrayEquals(new int[] {14, 11, 10}, ipQT3.getIds());
        assertArrayEquals(new float[] {-2.3841858E-7f, 0.001435399f, 0.0025851727f}, ipQT3.getCoefficients(), 0.000001f);

        index.clear();
        }

    }
