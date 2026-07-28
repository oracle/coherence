/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package hnsw;

import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.hnsw.HnswIndex;

import com.oracle.coherence.hnswlib.Index;
import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.AbstractMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HnswIndexTest
    {
    @Test
    public void shouldSerializeUsingJava()
        {
        shouldSerialize(DEFAULT);
        }

    @Test
    public void shouldSerializeUsingPof()
        {
        shouldSerialize(POF);
        }

    @Test
    public void shouldSerializeUsingJson()
        {
        shouldSerialize(JSON);
        }

    public void shouldSerialize(Serializer serializer)
        {
        ValueExtractor<?, Vector<float[]>> extractor = Extractors.extract("vector");
        HnswIndex<?, ?> index     = new HnswIndex<>(extractor, 1024)
                                                                .setMaxElements(1000)
                                                                .setM(32)
                                                                .setEfConstruction(100)
                                                                .setEfSearch(300)
                                                                .setSpaceName("L2")
                                                                .setRandomSeed(19);

        Binary          binary = ExternalizableHelper.toBinary(index, serializer);
        HnswIndex<?, ?> result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getExtractor(), is(index.getExtractor()));
        assertThat(result.getDimension(), is(index.getDimension()));
        assertThat(result.getSpaceName(), is(index.getSpaceName()));
        assertThat(result.getMaxElements(), is(index.getMaxElements()));
        assertThat(result.getM(), is(index.getM()));
        assertThat(result.getEfConstr(), is(index.getEfConstr()));
        assertThat(result.getEfSearch(), is(index.getEfSearch()));
        assertThat(result.getRandomSeed(), is(index.getRandomSeed()));
        }

    @Test
    public void shouldRejectInvalidConstructionAndSetters()
        {
        ValueExtractor<?, Vector<float[]>> extractor = Extractors.extract("vector");

        assertThrows(NullPointerException.class, () -> new HnswIndex<>(null, 3));
        assertThrows(IllegalArgumentException.class, () -> new HnswIndex<>(extractor, 0));
        assertThrows(IllegalArgumentException.class, () -> new HnswIndex<>(extractor, "bad", 3));

        HnswIndex<?, ?> index = new HnswIndex<>(extractor, 3);
        assertThrows(IllegalArgumentException.class, () -> index.setSpaceName(""));
        assertThrows(IllegalArgumentException.class, () -> index.setSpaceName("bad"));
        assertThrows(IllegalArgumentException.class, () -> index.setMaxElements(0));
        assertThrows(IllegalArgumentException.class, () -> index.setMaxElements(Index.MAX_ELEMENTS + 1));
        assertThrows(IllegalArgumentException.class, () -> index.setM(1));
        assertThrows(IllegalArgumentException.class, () -> index.setM(Index.MAX_M + 1));
        assertThrows(IllegalArgumentException.class, () -> index.setEfConstruction(0));
        assertThrows(IllegalArgumentException.class, () -> index.setEfConstruction(Index.MAX_EF_CONSTRUCTION + 1));
        assertThrows(IllegalArgumentException.class, () -> index.setEfSearch(0));
        assertThrows(IllegalArgumentException.class, () -> index.setEfSearch(Index.MAX_EF_SEARCH + 1));
        }

    @Test
    public void shouldRejectInvalidSerializedState()
        {
        shouldRejectInvalidSerializedState(DEFAULT);
        shouldRejectInvalidSerializedState(POF);
        }

    @Test
    public void shouldRejectInvalidMapIndexCreation()
        {
        HnswIndex<?, ?> index = new HnswIndex<>();

        assertThrows(NullPointerException.class, () -> index.new HnswMapIndex(null));
        }

    @Test
    public void shouldRejectInvalidQueryVectorAndCount()
        {
        ValueExtractor<?, Vector<float[]>> extractor = Extractors.extract("vector");
        HnswIndex<?, ?> index = new HnswIndex<>(extractor, 3);
        HnswIndex<?, ?>.HnswMapIndex mapIndex = index.new HnswMapIndex(null);
        try
            {
            assertThrows(IllegalArgumentException.class, () -> mapIndex.query(new Float32Vector(new float[] {1.0f, 2.0f}), 1, null));
            assertThrows(IllegalArgumentException.class, () -> mapIndex.query(new Float32Vector(new float[] {1.0f, 2.0f, 3.0f}), 0, null));
            assertThrows(NullPointerException.class, () -> mapIndex.query(null, 1, null));
            }
        finally
            {
            mapIndex.close();
            }
        }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void shouldRejectInvalidInsertAndUpdateVectors()
        {
        ValueExtractor<ValueWithVector, Vector<float[]>> extractor = ValueExtractor.of(ValueWithVector::getVector);
        HnswIndex<Object, ValueWithVector> index = new HnswIndex<>(extractor, 3);
        HnswIndex<Object, ValueWithVector>.HnswMapIndex mapIndex = index.new HnswMapIndex(null);
        try
            {
            ValueWithVector value = new ValueWithVector(new Float32Vector(new float[] {1.0f, 2.0f}));

            assertThrows(IllegalArgumentException.class,
                    () -> mapIndex.insert(new AbstractMap.SimpleEntry<>("key", value)));

            BinaryEntry entry = mock(BinaryEntry.class);
            when(entry.getValue()).thenReturn(value);
            when(entry.getBinaryKey()).thenReturn(new Binary(new byte[] {1}));

            assertThrows(IllegalArgumentException.class, () -> mapIndex.update(entry));
            }
        finally
            {
            mapIndex.close();
            }
        }

    private void shouldRejectInvalidSerializedState(Serializer serializer)
        {
        ValueExtractor<?, Vector<float[]>> extractor = Extractors.extract("vector");
        HnswIndex<?, ?> index = new HnswIndex<>(extractor, 3);
        setField(index, "m_nDimension", 0);

        Binary binary = ExternalizableHelper.toBinary(index, serializer);

        assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(binary, serializer));
        }

    private static void setField(Object target, String name, Object value)
        {
        try
            {
            Field field = HnswIndex.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
            }
        catch (ReflectiveOperationException e)
            {
            throw new RuntimeException(e);
            }
        }

    // ----- inner class: ValueWithVector ----------------------------------

    public static class ValueWithVector
        {
        ValueWithVector(Vector<float[]> vector)
            {
            m_vector = vector;
            }

        public Vector<float[]> getVector()
            {
            return m_vector;
            }

        private final Vector<float[]> m_vector;
        }

    // ----- data members ---------------------------------------------------

    public static final Serializer DEFAULT = new DefaultSerializer();

    public static final Serializer POF = new ConfigurablePofContext();

    public static final Serializer JSON = new JsonSerializer();
    }
