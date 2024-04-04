/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.internal;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BinaryVectorTest
    {
    @Test
    public void shouldSerializeVectorUsingJava()
        {
        DefaultSerializer serializer = new DefaultSerializer();
        Binary            binVector  = new Binary("foo".getBytes(StandardCharsets.UTF_8));
        BinaryVector      vector     = new BinaryVector(Binary.NO_BINARY, serializer.getName(), binVector);
        Binary            binary     = ExternalizableHelper.toBinary(vector, serializer);
        BinaryVector      result     = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getVector(), is(binVector));
        Optional<ReadBuffer> optional = result.getMetadata();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(false));
        assertThat(result.getFormat(), is(serializer.getName()));
        }

    @Test
    public void shouldSerializeVectorWithMetadataUsingJava()
        {
        DefaultSerializer serializer  = new DefaultSerializer();
        Binary            binVector   = new Binary("foo".getBytes(StandardCharsets.UTF_8));
        Binary            binMetadata = ExternalizableHelper.toBinary("bar", serializer);
        BinaryVector      vector      = new BinaryVector(binMetadata, serializer.getName(), binVector);
        Binary            binary      = ExternalizableHelper.toBinary(vector, serializer);
        BinaryVector      result      = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getVector(), is(binVector));
        Optional<ReadBuffer> optional = result.getMetadata();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get(), is(binMetadata));
        assertThat(result.getFormat(), is(serializer.getName()));
        }

    @Test
    public void shouldSerializeVectorUsingPof()
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext();
        Binary                 binVector  = new Binary("foo".getBytes(StandardCharsets.UTF_8));
        BinaryVector           vector     = new BinaryVector(null, serializer.getName(), binVector);
        Binary                 binary     = ExternalizableHelper.toBinary(vector, serializer);
        BinaryVector           result     = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getVector(), is(binVector));
        Optional<ReadBuffer> optional = result.getMetadata();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(false));
        assertThat(result.getFormat(), is(serializer.getName()));
        }

    @Test
    public void shouldSerializeVectorWithMetadataUsingPof()
        {
        ConfigurablePofContext serializer  = new ConfigurablePofContext();
        Binary                 binVector   = new Binary("foo".getBytes(StandardCharsets.UTF_8));
        Binary                 binMetadata = ExternalizableHelper.toBinary("bar", serializer);
        BinaryVector           vector      = new BinaryVector(binMetadata, serializer.getName(), binVector);
        Binary                 binary      = ExternalizableHelper.toBinary(vector, serializer);
        BinaryVector           result      = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getVector(), is(binVector));
        Optional<ReadBuffer> optional = result.getMetadata();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get(), is(binMetadata));
        assertThat(result.getFormat(), is(serializer.getName()));
        }

    @Test
    public void shouldRoundTripSerialize()
        {
        ConfigurablePofContext serializerJson = new ConfigurablePofContext();
        ConfigurablePofContext serializerPof  = new ConfigurablePofContext();
        DefaultSerializer      serializerJava = new DefaultSerializer();
        Binary                 binMetadata    = ExternalizableHelper.toBinary("foo", serializerJson);
        Binary                 binVector      = new Binary("bar".getBytes(StandardCharsets.UTF_8));
        BinaryVector           vector         = new BinaryVector(binMetadata, serializerJson.getName(), binVector);
        Optional<ReadBuffer>   optional;

        Binary       binPof        = ExternalizableHelper.toBinary(vector, serializerPof);
        BinaryVector vectorFromPof = ExternalizableHelper.fromBinary(binPof, serializerPof);


        assertThat(vectorFromPof, is(notNullValue()));
        assertThat(vectorFromPof.getVector(), is(binVector));
        assertThat(vectorFromPof.getFormat(), is(serializerJson.getName()));

        optional = vectorFromPof.getMetadata();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get(), is(binMetadata));

        Binary       binJava        = ExternalizableHelper.toBinary(vectorFromPof, serializerJava);
        BinaryVector vectorFromJava = ExternalizableHelper.fromBinary(binJava, serializerJava);

        assertThat(vectorFromJava, is(notNullValue()));
        assertThat(vectorFromJava.getVector(), is(binVector));
        assertThat(vectorFromJava.getFormat(), is(serializerJson.getName()));

        optional = vectorFromJava.getMetadata();
        assertThat(optional.isPresent(), is(true));
        Object oMetadata = ExternalizableHelper.fromBinary(optional.get().toBinary(), serializerJson);
        assertThat(oMetadata, is("foo"));
        }
    }