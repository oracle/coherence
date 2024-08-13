/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package hnsw;

import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.hnsw.HnswIndex;
import com.oracle.coherence.io.json.JsonSerializer;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
                                                                .setSpaceName("Foo")
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

    // ----- data members ---------------------------------------------------

    public static final Serializer DEFAULT = new DefaultSerializer();

    public static final Serializer POF = new ConfigurablePofContext();

    public static final Serializer JSON = new JsonSerializer();
    }
