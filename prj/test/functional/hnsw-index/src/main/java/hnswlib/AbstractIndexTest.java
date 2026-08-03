/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
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
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.StandardCopyOption;
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

    private static final int RESULT_QUERY_CANNOT_RETURN = 3;
    private static final int SIZE_T_BYTES = Native.SIZE_T_SIZE;
    private static final int LINKLIST_SIZE_BYTES = Integer.BYTES;
    private static final int TABLEINT_BYTES = Integer.BYTES;
    private static final long OFFSET_LEVEL0 = 0;
    private static final long OFFSET_MAX_ELEMENTS = OFFSET_LEVEL0 + SIZE_T_BYTES;
    private static final long OFFSET_CUR_ELEMENT_COUNT = OFFSET_MAX_ELEMENTS + SIZE_T_BYTES;
    private static final long OFFSET_SIZE_DATA_PER_ELEMENT = OFFSET_CUR_ELEMENT_COUNT + SIZE_T_BYTES;
    private static final long OFFSET_LABEL_OFFSET = OFFSET_SIZE_DATA_PER_ELEMENT + SIZE_T_BYTES;
    private static final long OFFSET_OFFSET_DATA = OFFSET_LABEL_OFFSET + SIZE_T_BYTES;
    private static final long OFFSET_MAXLEVEL = OFFSET_OFFSET_DATA + SIZE_T_BYTES;
    private static final long OFFSET_ENTERPOINT = OFFSET_MAXLEVEL + Integer.BYTES;
    private static final long OFFSET_MAX_M = OFFSET_ENTERPOINT + TABLEINT_BYTES;
    private static final long OFFSET_MAX_M0 = OFFSET_MAX_M + SIZE_T_BYTES;
    private static final long OFFSET_M = OFFSET_MAX_M0 + SIZE_T_BYTES;
    private static final long OFFSET_MULT = OFFSET_M + SIZE_T_BYTES;
    private static final long OFFSET_EF_CONSTRUCTION = OFFSET_MULT + Double.BYTES;
    private static final long HEADER_BYTES = OFFSET_EF_CONSTRUCTION + SIZE_T_BYTES;

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
    public void testRejectInvalidDimensions()
        {
        assertThrows(NullPointerException.class, () -> createIndexInstance(null, 3));
        assertThrows(IllegalArgumentException.class, () -> createIndexInstance(SpaceName.COSINE, -1));
        assertThrows(IllegalArgumentException.class, () -> createIndexInstance(SpaceName.COSINE, 0));
        assertThrows(IllegalArgumentException.class, () -> createIndexInstance(SpaceName.COSINE, Index.MAX_DIMENSION + 1));
        }

    @Test
    public void testRejectInvalidInitializationParameters()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 3);
        try
            {
            assertThrows(IllegalArgumentException.class, () -> index.initialize(0, 16, 200, 100, false));
            assertThrows(IllegalArgumentException.class, () -> index.initialize(Index.MAX_ELEMENTS + 1, 16, 200, 100, false));
            assertThrows(IllegalArgumentException.class, () -> index.initialize(10, 1, 200, 100, false));
            assertThrows(IllegalArgumentException.class, () -> index.initialize(10, Index.MAX_M + 1, 200, 100, false));
            assertThrows(IllegalArgumentException.class, () -> index.initialize(10, 16, 0, 100, false));
            assertThrows(IllegalArgumentException.class, () -> index.initialize(10, 16, Index.MAX_EF_CONSTRUCTION + 1, 100, false));
            }
        finally
            {
            index.clear();
            }
        }

    @Test
    public void testRejectInvalidLoadParameters() throws IOException
        {
        File tempFile = File.createTempFile("index", "sm");
        try
            {
            Index index = createIndexInstance(SpaceName.COSINE, 3);
            try
                {
                Path path = Paths.get(tempFile.getAbsolutePath());
                assertThrows(NullPointerException.class, () -> index.load(null, 1));
                assertThrows(IllegalArgumentException.class, () -> index.load(path, 0));
                assertThrows(IllegalArgumentException.class, () -> index.load(path, Index.MAX_ELEMENTS + 1));
                }
            finally
                {
                index.clear();
                }
            }
        finally
            {
            assertTrue(tempFile.delete());
            }
        }

    @Test
    public void testRejectInvalidResizeParameters()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 3);
        try
            {
            index.initialize(2);
            index.addItem(new float[] {1.0f, 2.0f, 3.0f}, 1);
            index.addItem(new float[] {1.1f, 2.1f, 3.1f}, 2);
            assertThrows(IllegalArgumentException.class, () -> resize(index, 0));
            assertThrows(IllegalArgumentException.class, () -> resize(index, Index.MAX_ELEMENTS + 1));
            assertThrows(IllegalArgumentException.class, () -> resize(index, 1));
            }
        finally
            {
            index.clear();
            }
        }

    @Test
    public void testRejectInvalidEf()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 3);
        try
            {
            index.initialize(2);
            assertThrows(IllegalArgumentException.class, () -> index.setEf(0));
            assertThrows(IllegalArgumentException.class, () -> index.setEf(Index.MAX_EF_SEARCH + 1));
            }
        finally
            {
            index.clear();
            }
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
    public void testLoadAllowsOverrideBelowSavedMaxWhenCurrentCountFits()
            throws IOException
        {
        Path path = createPopulatedIndexFile();
        try
            {
            Index index = createIndexInstance(SpaceName.L2, 3);
            try
                {
                index.load(path, 8);
                assertEquals(8, index.getLength());
                assertEquals(8, index.getMaxLength());
                }
            finally
                {
                index.clear();
                }
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void testLoadEmptySavedIndexMarksJavaWrapperInitialized()
            throws IOException
        {
        Path path = createEmptyIndexFile();
        try
            {
            Index index = createIndexInstance(SpaceName.L2, 3);
            try
                {
                index.load(path, 8);
                QueryTuple queryTuple = index.knnQuery(new float[] {1.0f, 2.0f, 3.0f}, 1);
                assertEquals(QueryTuple.EMPTY, queryTuple);
                assertEquals(0, index.getLength());
                }
            finally
                {
                index.clear();
                }
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void testLoadSavedIndexMarksJavaWrapperInitializedForMetadata()
            throws IOException
        {
        Path path = createPopulatedIndexFile();
        try
            {
            Index index = createIndexInstance(SpaceName.L2, 3);
            try
                {
                index.load(path, 16);
                assertEquals(2, index.getM());
                assertEquals(10, index.getEf());
                assertEquals(4, index.getEfConstruction());
                }
            finally
                {
                index.clear();
                }
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void testRejectCorruptSavedIndexHeaderMetadata()
            throws IOException
        {
        assertRejectsSavedIndex("cur_element_count > max_elements",
                path -> writeSizeT(path, OFFSET_CUR_ELEMENT_COUNT, 17));
        assertRejectsSavedIndex("file max_elements above cap",
                path -> writeSizeT(path, OFFSET_MAX_ELEMENTS, Index.MAX_ELEMENTS + 1L));
        assertRejectsSavedIndex("size_data_per_element mismatch",
                path -> writeSizeT(path, OFFSET_SIZE_DATA_PER_ELEMENT, readLayout(path).sizeDataPerElement + 1));
        assertRejectsSavedIndex("overflow-prone base data block",
                path -> writeSizeT(path, OFFSET_SIZE_DATA_PER_ELEMENT, Long.MAX_VALUE / readLayout(path).curElementCount + 1));
        assertRejectsSavedIndex("invalid M",
                path -> writeSizeT(path, OFFSET_M, 1));
        assertRejectsSavedIndex("invalid maxM",
                path -> writeSizeT(path, OFFSET_MAX_M, readLayout(path).m + 1));
        assertRejectsSavedIndex("invalid maxM0",
                path -> writeSizeT(path, OFFSET_MAX_M0, readLayout(path).m * 2 + 1));
        assertRejectsSavedIndex("invalid mult",
                path -> writeDouble(path, OFFSET_MULT, 0.0d));
        assertRejectsSavedIndex("invalid ef_construction",
                path -> writeSizeT(path, OFFSET_EF_CONSTRUCTION, readLayout(path).m - 1));
        assertRejectsSavedIndex("invalid enterpoint_node",
                path -> writeInt(path, OFFSET_ENTERPOINT, (int) readLayout(path).curElementCount));
        }

    @Test
    public void testRejectLoadOverrideBelowCurrentCount()
            throws IOException
        {
        Path path = createPopulatedIndexFile();
        try
            {
            Index index = createIndexInstance(SpaceName.L2, 3);
            try
                {
                assertThrows(UnexpectedNativeException.class, () -> index.load(path, 7));
                }
            finally
                {
                index.clear();
                }
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void testRejectInvalidEmptySavedIndexSentinel()
            throws IOException
        {
        Path path = createEmptyIndexFile();
        try
            {
            writeInt(path, OFFSET_MAXLEVEL, 0);
            Index index = createIndexInstance(SpaceName.L2, 3);
            try
                {
                assertThrows(UnexpectedNativeException.class, () -> index.load(path, 8));
                }
            finally
                {
                index.clear();
                }
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void testRejectCorruptSavedIndexLinkMetadata()
            throws IOException
        {
        assertRejectsSavedIndex("invalid linkListSize divisibility",
                path -> writeInt(path, findFirstUpperLinkSizeOffset(path), (int) readLayout(path).sizeLinksPerElement + 1));
        assertRejectsSavedIndex("upper linkListSize exceeds maxlevel",
                path ->
                    {
                    IndexFileLayout layout = readLayout(path);
                    writeInt(path, findFirstUpperLinkSizeOffset(path), (int) (layout.sizeLinksPerElement * (layout.maxLevel + 1)));
                    });
        assertRejectsSavedIndex("level-0 count above maxM0",
                path ->
                    {
                    IndexFileLayout layout = readLayout(path);
                    writeInt(path, layout.level0Offset(0), (int) layout.maxM0 + 1);
                    });
        assertRejectsSavedIndex("upper count above maxM",
                path ->
                    {
                    IndexFileLayout layout = readLayout(path);
                    writeInt(path, findFirstUpperLinkDataOffset(path), (int) layout.maxM + 1);
                    });
        assertRejectsSavedIndex("neighbor id outside current count",
                path ->
                    {
                    IndexFileLayout layout = readLayout(path);
                    writeInt(path, layout.level0Offset(0), 1);
                    writeInt(path, layout.level0Offset(0) + LINKLIST_SIZE_BYTES, (int) layout.curElementCount);
                    });
        assertRejectsSavedIndex("duplicate neighbor id",
                path ->
                    {
                    IndexFileLayout layout = readLayout(path);
                    writeInt(path, layout.level0Offset(0), 2);
                    writeInt(path, layout.level0Offset(0) + LINKLIST_SIZE_BYTES, 1);
                    writeInt(path, layout.level0Offset(0) + LINKLIST_SIZE_BYTES + TABLEINT_BYTES, 1);
                    });
        assertRejectsSavedIndex("self-neighbor id",
                path ->
                    {
                    IndexFileLayout layout = readLayout(path);
                    writeInt(path, layout.level0Offset(0), 1);
                    writeInt(path, layout.level0Offset(0) + LINKLIST_SIZE_BYTES, 0);
                    });
        assertRejectsSavedIndex("upper neighbor missing same level",
                AbstractIndexTest::writeUpperNeighborWithoutSameLevel);
        assertRejectsSavedIndex("maxlevel exceeds loaded levels",
                path -> writeInt(path, OFFSET_MAXLEVEL, readLayout(path).maxLevel + 1));
        }

    @Test
    public void testCorruptLoadPreservesInitializedIndex()
            throws IOException
        {
        Path validPath = createPopulatedIndexFile();
        Path corruptPath = Files.createTempFile("hnsw-corrupt-load", ".idx");
        Files.copy(validPath, corruptPath, StandardCopyOption.REPLACE_EXISTING);
        writeSizeT(corruptPath, OFFSET_CUR_ELEMENT_COUNT, 17);

        Index index = createIndexInstance(SpaceName.L2, 3);
        try
            {
            index.initialize(4, 2, 4, 100, false);
            index.addItem(new float[] {1.0f, 2.0f, 3.0f}, 42);
            assertThrows(UnexpectedNativeException.class, () -> index.load(corruptPath, 16));
            assertEquals(1, index.getLength());
            assertTrue(index.hasId(42));
            assertEquals(2, index.getM());
            QueryTuple queryTuple = index.knnQuery(new float[] {1.0f, 2.0f, 3.0f}, 1);
            assertEquals(42, queryTuple.getIds()[0]);
            }
        finally
            {
            index.clear();
            Files.deleteIfExists(validPath);
            Files.deleteIfExists(corruptPath);
            }
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
    public void testQueryEmptyException() throws UnexpectedNativeException
        {
        Index idx = createIndexInstance(SpaceName.COSINE, 3);
        idx.initialize(300);
        QueryTuple queryTuple = idx.knnQuery(new float[] {1.3f, 1.4f, 1.5f}, 3);
        assertTrue(queryTuple.empty());
        }

    @Test
    public void testRejectInvalidVectorsAndQueryCount()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 3);
        try
            {
            index.initialize(3);
            assertThrows(NullPointerException.class, () -> index.addItem(null));
            assertThrows(IllegalArgumentException.class, () -> index.addItem(new float[] {1.0f, 2.0f}));
            assertThrows(NullPointerException.class, () -> index.knnQuery(null, 1));
            assertThrows(IllegalArgumentException.class, () -> index.knnQuery(new float[] {1.0f, 2.0f}, 1));
            assertThrows(IllegalArgumentException.class, () -> index.knnQuery(new float[] {1.0f, 2.0f, 3.0f}, 0));
            assertThrows(IllegalArgumentException.class, () -> index.knnQuery(new float[] {1.0f, 2.0f, 3.0f}, -1));
            assertThrows(NullPointerException.class, () -> index.computeSimilarity(null, new float[] {1.0f, 2.0f, 3.0f}));
            assertThrows(IllegalArgumentException.class,
                    () -> index.computeSimilarity(new float[] {1.0f, 2.0f}, new float[] {1.0f, 2.0f, 3.0f}));
            }
        finally
            {
            index.clear();
            }
        }

    @Test
    public void testNativeFilteredQueryRejectsNullCallback()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 3);
        try
            {
            index.initialize(1);
            index.addItem(new float[] {1.0f, 2.0f, 3.0f}, 1);

            int result = nativeLibrary().knnFilterQuery(
                    nativeReference(index),
                    new float[] {1.0f, 2.0f, 3.0f},
                    1,
                    null,
                    new int[1],
                    new float[1]);

            assertEquals(RESULT_QUERY_CANNOT_RETURN, result);
            }
        finally
            {
            index.clear();
            }
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
            Index index = createIndexInstance(SpaceName.IP, 7);
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

    private static void resize(Index index, int maxSize)
        {
        try
            {
            Method method = Index.class.getDeclaredMethod("resize", int.class);
            method.setAccessible(true);
            method.invoke(index, maxSize);
            }
        catch (NoSuchMethodException | IllegalAccessException e)
            {
            throw new RuntimeException(e);
            }
        catch (InvocationTargetException e)
            {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException)
                {
                throw runtimeException;
                }
            throw new RuntimeException(cause);
            }
        }

    private static Path createPopulatedIndexFile()
            throws IOException
        {
        Path path = Files.createTempFile("hnsw-valid", ".idx");
        Index index = new Index(SpaceName.L2, 3);
        try
            {
            index.initialize(16, 2, 4, 100, false);
            for (int i = 0; i < 8; i++)
                {
                index.addItem(new float[] {i, i + 1.0f, i + 2.0f}, i + 1);
                }
            index.save(path);
            return path;
            }
        catch (RuntimeException e)
            {
            Files.deleteIfExists(path);
            throw e;
            }
        finally
            {
            index.clear();
            }
        }

    private static Path createEmptyIndexFile()
            throws IOException
        {
        Path path = Files.createTempFile("hnsw-empty", ".idx");
        Index index = new Index(SpaceName.L2, 3);
        try
            {
            index.initialize(8, 2, 4, 100, false);
            index.save(path);
            return path;
            }
        catch (RuntimeException e)
            {
            Files.deleteIfExists(path);
            throw e;
            }
        finally
            {
            index.clear();
            }
        }

    private static void assertRejectsSavedIndex(String description, FileMutation mutation)
            throws IOException
        {
        Path path = createPopulatedIndexFile();
        try
            {
            mutation.apply(path);
            Index index = new Index(SpaceName.L2, 3);
            try
                {
                assertThrows(UnexpectedNativeException.class, () -> index.load(path, 16), description);
                }
            finally
                {
                index.clear();
                }
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    private static IndexFileLayout readLayout(Path path)
            throws IOException
        {
        long sizeDataPerElement = readSizeT(path, OFFSET_SIZE_DATA_PER_ELEMENT);
        long curElementCount    = readSizeT(path, OFFSET_CUR_ELEMENT_COUNT);
        int  maxLevel           = readInt(path, OFFSET_MAXLEVEL);
        long maxM               = readSizeT(path, OFFSET_MAX_M);
        long maxM0              = readSizeT(path, OFFSET_MAX_M0);
        long m                  = readSizeT(path, OFFSET_M);
        long sizeLinksPerLevel  = maxM * TABLEINT_BYTES + LINKLIST_SIZE_BYTES;
        return new IndexFileLayout(sizeDataPerElement, curElementCount, maxLevel, maxM, maxM0, m,
                sizeLinksPerLevel);
        }

    private static long findFirstUpperLinkSizeOffset(Path path)
            throws IOException
        {
        IndexFileLayout layout = readLayout(path);
        long offset = layout.upperSectionOffset();
        for (int i = 0; i < layout.curElementCount; i++)
            {
            int linkListSize = readInt(path, offset);
            if (linkListSize > 0)
                {
                return offset;
                }
            offset += LINKLIST_SIZE_BYTES + Integer.toUnsignedLong(linkListSize);
            }
        throw new AssertionError("saved test index did not contain an upper-level link list");
        }

    private static long findFirstUpperLinkDataOffset(Path path)
            throws IOException
        {
        return findFirstUpperLinkSizeOffset(path) + LINKLIST_SIZE_BYTES;
        }

    private static void writeUpperNeighborWithoutSameLevel(Path path)
            throws IOException
        {
        int[]         elementLevels = readElementLevels(path);
        UpperLinkList linkList      = findHighestUpperLinkList(path, elementLevels);
        int           neighbor      = findElementBelowLevel(elementLevels, linkList.level);

        long linkListOffset = linkList.dataOffset + (long) (linkList.level - 1) * readLayout(path).sizeLinksPerElement;
        writeInt(path, linkListOffset, 1);
        writeInt(path, linkListOffset + LINKLIST_SIZE_BYTES, neighbor);
        }

    private static int[] readElementLevels(Path path)
            throws IOException
        {
        IndexFileLayout layout = readLayout(path);
        int[]           levels = new int[(int) layout.curElementCount];
        long            offset = layout.upperSectionOffset();
        for (int i = 0; i < levels.length; i++)
            {
            int linkListSize = readInt(path, offset);
            levels[i] = linkListSize == 0 ? 0 : (int) (Integer.toUnsignedLong(linkListSize) / layout.sizeLinksPerElement);
            offset += LINKLIST_SIZE_BYTES + Integer.toUnsignedLong(linkListSize);
            }
        return levels;
        }

    private static UpperLinkList findHighestUpperLinkList(Path path, int[] elementLevels)
            throws IOException
        {
        IndexFileLayout layout = readLayout(path);
        long            offset = layout.upperSectionOffset();
        UpperLinkList   result = null;
        for (int i = 0; i < elementLevels.length; i++)
            {
            int linkListSize = readInt(path, offset);
            if (elementLevels[i] > 0
                    && (result == null || elementLevels[i] > result.level))
                {
                result = new UpperLinkList(elementLevels[i], offset + LINKLIST_SIZE_BYTES);
                }
            offset += LINKLIST_SIZE_BYTES + Integer.toUnsignedLong(linkListSize);
            }
        if (result == null)
            {
            throw new AssertionError("saved test index did not contain an upper-level link list");
            }
        return result;
        }

    private static int findElementBelowLevel(int[] elementLevels, int level)
        {
        for (int i = 0; i < elementLevels.length; i++)
            {
            if (elementLevels[i] < level)
                {
                return i;
                }
            }
        throw new AssertionError("saved test index did not contain a lower-level neighbor candidate");
        }

    private static long readSizeT(Path path, long offset)
            throws IOException
        {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r"))
            {
            file.seek(offset);
            byte[] bytes = new byte[SIZE_T_BYTES];
            file.readFully(bytes);
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            return SIZE_T_BYTES == Long.BYTES ? buffer.getLong() : Integer.toUnsignedLong(buffer.getInt());
            }
        }

    private static int readInt(Path path, long offset)
            throws IOException
        {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r"))
            {
            file.seek(offset);
            byte[] bytes = new byte[Integer.BYTES];
            file.readFully(bytes);
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            }
        }

    private static void writeSizeT(Path path, long offset, long value)
            throws IOException
        {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE_T_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        if (SIZE_T_BYTES == Long.BYTES)
            {
            buffer.putLong(value);
            }
        else
            {
            buffer.putInt((int) value);
            }
        writeBytes(path, offset, buffer.array());
        }

    private static void writeInt(Path path, long offset, int value)
            throws IOException
        {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        writeBytes(path, offset, buffer.array());
        }

    private static void writeDouble(Path path, long offset, double value)
            throws IOException
        {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(value);
        writeBytes(path, offset, buffer.array());
        }

    private static void writeBytes(Path path, long offset, byte[] bytes)
            throws IOException
        {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw"))
            {
            file.seek(offset);
            file.write(bytes);
            }
        }

    @FunctionalInterface
    private interface FileMutation
        {
        void apply(Path path) throws IOException;
        }

    private static class IndexFileLayout
        {
        IndexFileLayout(long sizeDataPerElement, long curElementCount, int maxLevel, long maxM, long maxM0, long m,
                long sizeLinksPerElement)
            {
            this.sizeDataPerElement = sizeDataPerElement;
            this.curElementCount = curElementCount;
            this.maxLevel = maxLevel;
            this.maxM = maxM;
            this.maxM0 = maxM0;
            this.m = m;
            this.sizeLinksPerElement = sizeLinksPerElement;
            }

        long level0Offset(int element)
            {
            return HEADER_BYTES + element * sizeDataPerElement;
            }

        long upperSectionOffset()
            {
            return HEADER_BYTES + curElementCount * sizeDataPerElement;
            }

        final long sizeDataPerElement;
        final long curElementCount;
        final int  maxLevel;
        final long maxM;
        final long maxM0;
        final long m;
        final long sizeLinksPerElement;
        }

    private static class UpperLinkList
        {
        UpperLinkList(int level, long dataOffset)
            {
            this.level = level;
            this.dataOffset = dataOffset;
            }

        final int  level;
        final long dataOffset;
        }

    private static Hnswlib nativeLibrary()
        {
        try
            {
            Field field = Index.class.getDeclaredField("hnswlib");
            field.setAccessible(true);
            return (Hnswlib) field.get(null);
            }
        catch (NoSuchFieldException | IllegalAccessException e)
            {
            throw new RuntimeException(e);
            }
        }

    private static Pointer nativeReference(Index index)
        {
        try
            {
            Field field = Index.class.getDeclaredField("reference");
            field.setAccessible(true);
            return (Pointer) field.get(index);
            }
        catch (NoSuchFieldException | IllegalAccessException e)
            {
            throw new RuntimeException(e);
            }
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
