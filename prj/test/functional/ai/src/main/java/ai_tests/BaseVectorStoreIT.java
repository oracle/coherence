/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.VectorStore;
import com.tangosol.net.Session;
import com.tangosol.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class BaseVectorStoreIT
    {
    @BeforeEach
    void setTestName(TestInfo info)
        {
        m_sTestName = info.getDisplayName();
        }

    protected static Stream<Arguments> stores()
        {
        List<Arguments> list = new ArrayList<>();

        list.add(Arguments.of("double", new StoreFactory(VectorStore::ofDoubles, VectorStore::ofDoubles)));
        list.add(Arguments.of("float", new StoreFactory(VectorStore::ofFloats, VectorStore::ofFloats)));
        list.add(Arguments.of("int", new StoreFactory(VectorStore::ofInts, VectorStore::ofInts)));
        list.add(Arguments.of("long", new StoreFactory(VectorStore::ofLongs, VectorStore::ofLongs)));
        list.add(Arguments.of("short", new StoreFactory(VectorStore::ofShorts, VectorStore::ofShorts)));

        return list.stream();
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddDoubles(String ignored, StoreFactory factory)
        {
        double[] doubles = new double[]{1.0, 2.0, 3.0};
        String   sKey    = "one";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addDoubles(sKey, doubles);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertDoubleVector(optVector, sKey, doubles, null);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddDoublesWithMetadata(String ignored, StoreFactory factory)
        {
        double[] doubles   = new double[]{1.0, 2.0, 3.0};
        String   sKey      = "one";
        String   sMetadata = "one-meta";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addDoubles(sKey, doubles, sMetadata);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertDoubleVector(optVector, sKey, doubles, sMetadata);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddArraysOfDoubles(String ignored, StoreFactory factory)
        {
        Vector.SimpleKeySequence sequence = new Vector.SimpleKeySequence(0L);
        int                      cVector  = 150;
        double[][]               doubles  = new double[cVector][];

        for (int i = 0; i < cVector; i++)
            {
            double nOffset = i * 10;
            doubles[i] = new double[]{nOffset + 0, nOffset + 1, nOffset + 2};
            }

        VectorStore<?, Vector.Key, Void> store = factory.getStore(m_sTestName);
        store.addDoubles(doubles, sequence);

        UUID uuid = sequence.uuid();
        for (int i = 0; i < cVector; i++)
            {
            Vector.Key key    = new Vector.Key(uuid, i);
            double[]   vector = doubles[i];
            Optional<? extends Vector<?, Vector.Key, Void>> optVector = store.getVector(key);
            assertDoubleVector(optVector, key, vector, null);
            }
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddFloats(String ignored, StoreFactory factory)
        {
        float[] floats = new float[]{1.0f, 2.0f, 3.0f};
        String  sKey   = "one";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addFloats(sKey, floats);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertFloatVector(optVector, sKey, floats, null);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddFloatsWithMetadata(String ignored, StoreFactory factory)
        {
        float[] floats    = new float[]{1.0f, 2.0f, 3.0f};
        String  sKey      = "one";
        String  sMetadata = "one-meta";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addFloats(sKey, floats, sMetadata);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertFloatVector(optVector, sKey, floats, sMetadata);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddArraysOfFloats(String ignored, StoreFactory factory)
        {
        Vector.SimpleKeySequence sequence = new Vector.SimpleKeySequence(0L);
        int                      cVector  = 150;
        float[][]                floats   = new float[cVector][];

        for (int i = 0; i < cVector; i++)
            {
            float nOffset = i * 10;
            floats[i] = new float[]{nOffset + 0, nOffset + 1, nOffset + 2};
            }

        VectorStore<?, Vector.Key, Void> store = factory.getStore(m_sTestName);
        store.addFloats(floats, sequence);

        UUID uuid = sequence.uuid();
        for (int i = 0; i < cVector; i++)
            {
            Vector.Key key    = new Vector.Key(uuid, i);
            float[]    vector = floats[i];
            Optional<? extends Vector<?, Vector.Key, Void>> optVector = store.getVector(key);
            assertFloatVector(optVector, key, vector, null);
            }
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddInts(String ignored, StoreFactory factory)
        {
        int[]  ints = new int[]{1, 2, 3};
        String sKey = "one";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addInts(sKey, ints);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertIntVector(optVector, sKey, ints, null);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddIntsWithMetadata(String ignored, StoreFactory factory)
        {
        int[]  ints      = new int[]{1, 2, 3};
        String sKey      = "one";
        String sMetadata = "one-meta";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addInts(sKey, ints, sMetadata);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertIntVector(optVector, sKey, ints, sMetadata);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddArraysOfInts(String ignored, StoreFactory factory)
        {
        Vector.SimpleKeySequence sequence = new Vector.SimpleKeySequence(0L);
        int                      cVector  = 150;
        int[][]                  ints     = new int[cVector][];

        for (int i = 0; i < cVector; i++)
            {
            int nOffset = i * 10;
            ints[i] = new int[]{nOffset, nOffset + 1, nOffset + 2};
            }

        VectorStore<?, Vector.Key, Void> store = factory.getStore(m_sTestName);
        store.addInts(ints, sequence);

        UUID uuid = sequence.uuid();
        for (int i = 0; i < cVector; i++)
            {
            Vector.Key key    = new Vector.Key(uuid, i);
            int[]      vector = ints[i];
            Optional<? extends Vector<?, Vector.Key, Void>> optVector = store.getVector(key);
            assertIntVector(optVector, key, vector, null);
            }
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddLongs(String ignored, StoreFactory factory)
        {
        long[] longs = new long[]{100L, 200L, 300L};
        String sKey   = "one";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addLongs(sKey, longs);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertLongVector(optVector, sKey, longs, null);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddLongsWithMetadata(String ignored, StoreFactory factory)
        {
        long[] longs    = new long[]{100L, 200L, 300L};
        String sKey      = "one";
        String sMetadata = "one-meta";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addLongs(sKey, longs, sMetadata);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertLongVector(optVector, sKey, longs, sMetadata);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddArraysOfLongs(String ignored, StoreFactory factory)
        {
        Vector.SimpleKeySequence sequence = new Vector.SimpleKeySequence(0L);
        int                      cVector  = 150;
        long[][]                 longs  = new long[cVector][];

        for (int i = 0; i < cVector; i++)
            {
            long nOffset = i * 10;
            longs[i] = new long[]{nOffset, nOffset + 1, nOffset + 2};
            }

        VectorStore<?, Vector.Key, Void> store = factory.getStore(m_sTestName);
        store.addLongs(longs, sequence);

        UUID uuid = sequence.uuid();
        for (int i = 0; i < cVector; i++)
            {
            Vector.Key key    = new Vector.Key(uuid, i);
            long[]     vector = longs[i];
            Optional<? extends Vector<?, Vector.Key, Void>> optVector = store.getVector(key);
            assertLongVector(optVector, key, vector, null);
            }
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddShorts(String ignored, StoreFactory factory)
        {
        short[] shorts = new short[]{9, 8, 7};
        String  sKey   = "one";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addShorts(sKey, shorts);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertShortVector(optVector, sKey, shorts, null);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddShortsWithMetadata(String ignored, StoreFactory factory)
        {
        short[] shorts    = new short[]{9, 8, 7};
        String  sKey      = "one";
        String  sMetadata = "one-meta";

        VectorStore<?, String, String> store = factory.getStore(m_sTestName);
        store.addShorts(sKey, shorts, sMetadata);

        Optional<? extends Vector<?, String, String>> optVector = store.getVector(sKey);
        assertShortVector(optVector, sKey, shorts, sMetadata);
        }

    @ParameterizedTest(name = "{index} store={0}")
    @MethodSource("stores")
    public void shouldAddArraysOfShorts(String ignored, StoreFactory factory)
        {
        Vector.SimpleKeySequence sequence = new Vector.SimpleKeySequence(0L);
        int                      cVector  = 150;
        short[][]                shorts   = new short[cVector][];
        short                    multiple = 10;

        for (short i = 0; i < cVector; i++)
            {
            short nOffset =  (short) (i * multiple);
            shorts[i] = new short[]{nOffset, (short) (nOffset + 1), (short) (nOffset + 2)};
            }

        VectorStore<?, Vector.Key, Void> store = factory.getStore(m_sTestName);
        store.addShorts(shorts, sequence);

        UUID uuid = sequence.uuid();
        for (int i = 0; i < cVector; i++)
            {
            Vector.Key key    = new Vector.Key(uuid, i);
            short[]   vector = shorts[i];
            Optional<? extends Vector<?, Vector.Key, Void>> optVector = store.getVector(key);
            assertShortVector(optVector, key, vector, null);
            }
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected <K, M> void assertDoubleVector(Optional<? extends Vector<?, K, M>> optVector, K key, double[] doubles, M metadata)
        {
        assertThat(optVector.isPresent(), is(true));
        assertDoubleVector(optVector.get(), key, doubles, metadata);
        }

    protected <K, M> void assertDoubleVector(Vector<?, K, M> vector, K key, double[] doubles, M metadata)
        {
        Vector<double[], K, M> vectorDoubles = vector.asDoubles();
        double[]               rounded       = Arrays.stream(vectorDoubles.getVector())
                                                     .mapToInt(d -> (int) (d * 10000))
                                                     .mapToDouble(i -> (double) i / 10000)
                                                     .toArray();

        assertThat(rounded, is(doubles));
        assertThat(vector.getKey(), is(key));

        Optional<M> optMetadata = vector.getMetadata();
        assertThat(optMetadata, is(notNullValue()));
        if (metadata == null)
            {
            assertThat(optMetadata.isPresent(), is(false));
            }
        else
            {
            assertThat(optMetadata.isPresent(), is(true));
            assertThat(optMetadata.get(), is(metadata));
            }
        }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected <K, M> void assertFloatVector(Optional<? extends Vector<?, K, M>> optVector, K key, float[] floats, M metadata)
        {
        assertThat(optVector.isPresent(), is(true));
        assertFloatVector(optVector.get(), key, floats, metadata);
        }

    protected <K, M> void assertFloatVector(Vector<?, K, M> vector, K key, float[] floats, M metadata)
        {
        Vector<float[], K, M> vectorFloats = vector.asFloats();
        assertThat(vectorFloats.getVector(), is(floats));
        assertThat(vector.getKey(), is(key));

        Optional<M> optMetadata = vector.getMetadata();
        assertThat(optMetadata, is(notNullValue()));
        if (metadata == null)
            {
            assertThat(optMetadata.isPresent(), is(false));
            }
        else
            {
            assertThat(optMetadata.isPresent(), is(true));
            assertThat(optMetadata.get(), is(metadata));
            }
        }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected <K, M> void assertIntVector(Optional<? extends Vector<?, K, M>> optVector, K key, int[] ints, M metadata)
        {
        assertThat(optVector.isPresent(), is(true));
        assertIntVector(optVector.get(), key, ints, metadata);
        }

    protected <K, M> void assertIntVector(Vector<?, K, M> vector, K key, int[] ints, M metadata)
        {
        Vector<int[], K, M> vectorInts = vector.asInts();
        assertThat(vectorInts.getVector(), is(ints));
        assertThat(vector.getKey(), is(key));

        Optional<M> optMetadata = vector.getMetadata();
        assertThat(optMetadata, is(notNullValue()));
        if (metadata == null)
            {
            assertThat(optMetadata.isPresent(), is(false));
            }
        else
            {
            assertThat(optMetadata.isPresent(), is(true));
            assertThat(optMetadata.get(), is(metadata));
            }
        }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected <K, M> void assertLongVector(Optional<? extends Vector<?, K, M>> optVector, K key, long[] longs, M metadata)
        {
        assertThat(optVector.isPresent(), is(true));
        assertLongVector(optVector.get(), key, longs, metadata);
        }

    protected <K, M> void assertLongVector(Vector<?, K, M> vector, K key, long[] longs, M metadata)
        {
        Vector<long[], K, M> vectorLongs = vector.asLongs();
        assertThat(vectorLongs.getVector(), is(longs));
        assertThat(vector.getKey(), is(key));

        Optional<M> optMetadata = vector.getMetadata();
        assertThat(optMetadata, is(notNullValue()));
        if (metadata == null)
            {
            assertThat(optMetadata.isPresent(), is(false));
            }
        else
            {
            assertThat(optMetadata.isPresent(), is(true));
            assertThat(optMetadata.get(), is(metadata));
            }
        }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected <K, M> void assertShortVector(Optional<? extends Vector<?, K, M>> optVector, K key, short[] shorts, M metadata)
        {
        assertThat(optVector.isPresent(), is(true));
        assertShortVector(optVector.get(), key, shorts, metadata);
        }

    protected <K, M> void assertShortVector(Vector<?, K, M> vector, K key, short[] shorts, M metadata)
        {
        Vector<short[], K, M> vectorShorts = vector.asShorts();
        assertThat(vectorShorts.getVector(), is(shorts));
        assertThat(vector.getKey(), is(key));

        Optional<M> optMetadata = vector.getMetadata();
        assertThat(optMetadata, is(notNullValue()));
        if (metadata == null)
            {
            assertThat(optMetadata.isPresent(), is(false));
            }
        else
            {
            assertThat(optMetadata.isPresent(), is(true));
            assertThat(optMetadata.get(), is(metadata));
            }
        }


    // ----- inner interface: StoreFactory ----------------------------------

    /**
     * A factory for creating a {@link VectorStore} to use in tests.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class StoreFactory
        {
        public StoreFactory(Function<String, VectorStore> fnNamed, BiFunction<String, Session, VectorStore> fnSession)
            {
            m_fnNamed   = fnNamed;
            m_fnSession = fnSession;
            }

        public <K, M> VectorStore<?, K, M> getStore(String sName)
            {
            return m_fnNamed.apply(sName);
            }

        public <K, M> VectorStore<?, K, M> getStore(String sName, Session session)
            {
            return m_fnSession.apply(sName, session);
            }

        private final Function<String, VectorStore> m_fnNamed;

        private final BiFunction<String, Session, VectorStore> m_fnSession;
        }

    // ----- data members ---------------------------------------------------

    private String m_sTestName;
    }
