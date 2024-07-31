/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.search;

import com.oracle.coherence.ai.DistanceAlgorithm;
import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.Vector;

import com.oracle.coherence.ai.distance.CosineDistance;
import com.oracle.coherence.ai.search.BinaryQueryResult;
import com.oracle.coherence.ai.search.SimilaritySearch;
import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleStreamer;
import com.tangosol.util.ValueExtractor;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class SimilaritySearchTest
    {
    @Test
    public void shouldSerializeUsingJava() throws Exception
        {
        shouldSerialize(new DefaultSerializer());
        }

    @Test
    public void shouldSerializeUsingPof() throws Exception
        {
        shouldSerialize(new ConfigurablePofContext());
        }

    @Test
    public void shouldSerializeUsingJson() throws Exception
        {
        shouldSerialize(new JsonSerializer());
        }

    public void shouldSerialize(Serializer serializer) throws Exception
        {
        Vector<float[]> vector = new Float32Vector(new float[] {1, 2, 3.5f});
        ValueExtractor<String, Vector<float[]>> extractor = Extractors.extract("foo");
        Filter<?> filter = Filters.equal("foo", "bar");

        SimilaritySearch<String, String, float[]> aggregator = new SimilaritySearch<>(extractor, vector, 19);
        Binary                                    binary     = ExternalizableHelper.toBinary(aggregator.filter(filter).bruteForce(), serializer);
        SimilaritySearch<String, String, float[]> result     = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        assertThat(result.getExtractor(), is(extractor));
        assertThat(result.getVector(), is(vector));
        assertThat(result.getAlgorithm(), is(instanceOf(CosineDistance.class)));
        assertThat(result.getMaxResults(), is(19));
        assertThat(result.getFilter(), is(filter));
        assertThat(result.isBruteForce(), is(true));
        }

    @Test
    public void shouldUseBruteForce()
        {
        ValueExtractor<ValueWithVector, Vector<float[]>> extractor = ValueExtractor.of(ValueWithVector::getVector);
        DistanceAlgorithm<float[]> algorithm = mock(DistanceAlgorithm.class);
        Vector<float[]> vector = new Float32Vector(new float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f});

        when(algorithm.distance(any(Vector.class), any(Vector.class))).thenReturn(1.0d, 0.5d, 0.25d);

        SimilaritySearch<String, ValueWithVector, float[]> aggregator = new SimilaritySearch<>(extractor, vector, 10);

        List<InvocableMap.Entry<String, ValueWithVector>> list = new ArrayList<>();
        list.add(createEntry("one", new float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f}));
        list.add(createEntry("two", new float[] {11.0f, 12.0f, 13.0f, 14.0f, 15.0f, 16.0f}));
        list.add(createEntry("three", new float[] {21.0f, 22.0f, 23.0f, 24.0f, 25.0f, 26.0f}));

        assertThat(aggregator.algorithm(algorithm).bruteForce().accumulate(new SimpleStreamer<>(list)), is(true));

        ArgumentCaptor<Vector<float[]>> captor1 = ArgumentCaptor.forClass(Vector.class);
        ArgumentCaptor<Vector<float[]>> captor2 = ArgumentCaptor.forClass(Vector.class);
        verify(algorithm, times(3)).distance(captor1.capture(), captor2.capture());

        List<Vector<float[]>> vectors = captor2.getAllValues();
        assertThat(vectors.get(0), is(list.get(0).getValue().getVector()));
        assertThat(vectors.get(1), is(list.get(1).getValue().getVector()));
        assertThat(vectors.get(2), is(list.get(2).getValue().getVector()));

        List<BinaryQueryResult> listResult = aggregator.getPartialResult();
        assertThat(listResult.size(), is(3));
        assertThat(listResult.get(0).getDistance(), is(0.25d));
        assertThat(listResult.get(1).getDistance(), is(0.5d));
        assertThat(listResult.get(2).getDistance(), is(1.0d));
        }

    @Test
    public void shouldCreateAggregator()
        {
        ValueExtractor<ValueWithVector, Vector<float[]>> extractor = ValueExtractor.of(ValueWithVector::getVector);
        float[]                                          floats    = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
        Vector<float[]>                                  vector    = new Float32Vector(floats);

        SimilaritySearch<String, ValueWithVector, float[]> aggregator = new SimilaritySearch<>(extractor, vector, 10);

        assertThat(aggregator.getExtractor(), is(extractor));
        assertThat(aggregator.getMaxResults(), is(10));
        assertThat(aggregator.getVector(), is(vector));
        assertThat(aggregator.getAlgorithm(), is(instanceOf(CosineDistance.class)));
        assertThat(aggregator.getFilter(), is(nullValue()));
        assertThat(aggregator.isBruteForce(), is(false));

        InvocableMap.StreamingAggregator<String, ValueWithVector, List<BinaryQueryResult>, List<QueryResult<String, ValueWithVector>>> other
                = aggregator.supply();
        assertThat(other, is(instanceOf(SimilaritySearch.class)));

        SimilaritySearch<String, ValueWithVector, float[]> search = (SimilaritySearch<String, ValueWithVector, float[]>) other;
        assertThat(search.getExtractor(), is(extractor));
        assertThat(search.getMaxResults(), is(10));
        assertThat(search.getVector(), is(vector));
        assertThat(search.getAlgorithm(), is(instanceOf(CosineDistance.class)));
        assertThat(search.getFilter(), is(nullValue()));
        assertThat(search.isBruteForce(), is(false));
        }

    @Test
    public void shouldCreateAggregatorWithFilter()
        {
        ValueExtractor<ValueWithVector, Vector<float[]>> extractor = ValueExtractor.of(ValueWithVector::getVector);
        float[]                                          floats    = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
        Vector<float[]>                                  vector    = new Float32Vector(floats);
        Filter<DocumentChunk>                            filter    = Filters.equal("foo", "bar");

        SimilaritySearch<String, ValueWithVector, float[]> aggregator
                = new SimilaritySearch<>(extractor, vector, 10);

        aggregator.filter(filter);

        assertThat(aggregator.getExtractor(), is(extractor));
        assertThat(aggregator.getMaxResults(), is(10));
        assertThat(aggregator.getVector(), is(vector));
        assertThat(aggregator.getAlgorithm(), is(instanceOf(CosineDistance.class)));
        assertThat(aggregator.getFilter(), is(sameInstance(filter)));
        assertThat(aggregator.isBruteForce(), is(false));

        InvocableMap.StreamingAggregator<String, ValueWithVector, List<BinaryQueryResult>, List<QueryResult<String, ValueWithVector>>> other
                = aggregator.supply();
        assertThat(other, is(instanceOf(SimilaritySearch.class)));

        SimilaritySearch<String, ValueWithVector, float[]> search = (SimilaritySearch<String, ValueWithVector, float[]>) other;
        assertThat(search.getExtractor(), is(extractor));
        assertThat(search.getMaxResults(), is(10));
        assertThat(search.getVector(), is(vector));
        assertThat(search.getAlgorithm(), is(instanceOf(CosineDistance.class)));
        assertThat(search.getFilter(), is(sameInstance(filter)));
        assertThat(search.isBruteForce(), is(false));
        }

    @Test
    public void shouldCreateAggregatorWithBruteForce()
        {
        ValueExtractor<ValueWithVector, Vector<float[]>> extractor = ValueExtractor.of(ValueWithVector::getVector);
        float[]                                          floats    = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
        Vector<float[]>                                  vector    = new Float32Vector(floats);
        Filter<DocumentChunk>                            filter    = Filters.equal("foo", "bar");

        SimilaritySearch<String, ValueWithVector, float[]> aggregator
                = new SimilaritySearch<>(extractor, vector, 10);

        aggregator.bruteForce();

        assertThat(aggregator.getExtractor(), is(extractor));
        assertThat(aggregator.getMaxResults(), is(10));
        assertThat(aggregator.getVector(), is(vector));
        assertThat(aggregator.getAlgorithm(), is(instanceOf(CosineDistance.class)));
        assertThat(aggregator.getFilter(), is(nullValue()));
        assertThat(aggregator.isBruteForce(), is(true));

        InvocableMap.StreamingAggregator<String, ValueWithVector, List<BinaryQueryResult>, List<QueryResult<String, ValueWithVector>>> other
                = aggregator.supply();
        assertThat(other, is(instanceOf(SimilaritySearch.class)));

        SimilaritySearch<String, ValueWithVector, float[]> search = (SimilaritySearch<String, ValueWithVector, float[]>) other;
        assertThat(search.getExtractor(), is(extractor));
        assertThat(search.getMaxResults(), is(10));
        assertThat(search.getVector(), is(vector));
        assertThat(search.getAlgorithm(), is(instanceOf(CosineDistance.class)));
        assertThat(search.getFilter(), is(nullValue()));
        assertThat(search.isBruteForce(), is(true));
        }


    // ----- helper methods -------------------------------------------------

    protected BinaryEntry<String, ValueWithVector> createEntry(String key, float[] vector)
        {
        ValueWithVector value    = new ValueWithVector(new Float32Vector(vector), null);
        Binary          binValue = ExternalizableHelper.toBinary(value, SERIALIZER);
        Binary          binKey   = ExternalizableHelper.toBinary(key, SERIALIZER);

        BinaryEntry<String, ValueWithVector> entry = mock(BinaryEntry.class);
        when(entry.getKey()).thenReturn(key);
        when(entry.getValue()).thenReturn(value);
        when(entry.getBinaryKey()).thenReturn(binKey);
        when(entry.getBinaryValue()).thenReturn(binValue);
        when(entry.asBinaryEntry()).thenReturn(entry);
        when(entry.extract(ValueExtractor.of(ValueWithVector::getVector))).thenReturn(value.getVector());

        return entry;
        }

    // ----- data members ---------------------------------------------------

    public static final Serializer SERIALIZER = new DefaultSerializer();

    }
