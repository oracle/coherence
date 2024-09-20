/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.io.json.JsonSerializer;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DocumentChunkTest
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
        Map<String, Object> metadata = Map.of("one", "value-one", "two", "value-two");
        DocumentChunk       chunk    = new DocumentChunk("test", metadata);
        Binary              binary   = ExternalizableHelper.toBinary(chunk, serializer);
        DocumentChunk       result   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        assertThat(result.vector(), is(nullValue()));
        assertThat(result.text(), is(chunk.text()));
        assertThat(result.metadata(), is(chunk.metadata()));
        }

    @Test
    public void shouldSerializeWithoutMetadataUsingJava() 
        {
        shouldSerializeWithoutMetadata(DEFAULT);
        }

    @Test
    public void shouldSerializeWithoutMetadataUsingPof() 
        {
        shouldSerializeWithoutMetadata(POF);
        }

    @Test
    public void shouldSerializeWithoutMetadataUsingJson() 
        {
        shouldSerializeWithoutMetadata(JSON);
        }

    public void shouldSerializeWithoutMetadata(Serializer serializer) 
        {
        DocumentChunk       chunk    = new DocumentChunk("test");
        Binary              binary   = ExternalizableHelper.toBinary(chunk, serializer);
        DocumentChunk       result   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        assertThat(result.vector(), is(nullValue()));
        assertThat(result.text(), is(chunk.text()));
        assertThat(result.metadata(), is(Map.of()));
        }

    @Test
    public void shouldSerializeWithVectorUsingJava() 
        {
        shouldSerializeWithVector(DEFAULT);
        }

    @Test
    public void shouldSerializeWithVectorUsingPof() 
        {
        shouldSerializeWithVector(POF);
        }

    @Test
    public void shouldSerializeWithVectorUsingJson() 
        {
        shouldSerializeWithVector(JSON);
        }

    public void shouldSerializeWithVector(Serializer serializer) 
        {
        Map<String, Object> metadata = Map.of("one", "value-one", "two", "value-two");
        Float32Vector       vector   = new Float32Vector(new float[]{1.0f, 2.0f, 3.0f});
        DocumentChunk       chunk    = new DocumentChunk("test", metadata, vector);
        Binary              binary   = ExternalizableHelper.toBinary(chunk, serializer);
        DocumentChunk       result   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        assertThat(result.vector(), is(vector));
        assertThat(result.text(), is(chunk.text()));
        assertThat(result.metadata(), is(chunk.metadata()));
        }

    @Test
    public void shouldSerializeIdUsingJava() 
        {
        shouldSerializeId(DEFAULT);
        }

    @Test
    public void shouldSerializeIdUsingPof() 
        {
        shouldSerializeId(POF);
        }

    @Test
    public void shouldSerializeIdUsingJson() 
        {
        shouldSerializeId(JSON);
        }

    public void shouldSerializeId(Serializer serializer) 
        {
        DocumentChunk.Id id     = new DocumentChunk.Id("test", 19);
        Binary           binary = ExternalizableHelper.toBinary(id, serializer);
        DocumentChunk.Id result = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        assertThat(result.docId(), is(id.docId()));
        assertThat(result.index(), is(result.index()));
        }

    @Test
    public void shouldCreateWithTextOnly()
        {
        DocumentChunk chunk = new DocumentChunk("test");
        assertThat(chunk.text(), is("test"));
        assertThat(chunk.vector(), is(nullValue()));
        assertThat(chunk.isEmbedded(), is(false));
        assertThat(chunk.metadata(), is(Map.of()));
        }

    @Test
    public void shouldCreateWithTextAndMetadata()
        {
        Map<String, Object> metadata = Map.of("one", "value-one", "two", "value-two");
        DocumentChunk       chunk    = new DocumentChunk("test", metadata);
        assertThat(chunk.text(), is("test"));
        assertThat(chunk.vector(), is(nullValue()));
        assertThat(chunk.isEmbedded(), is(false));
        assertThat(chunk.metadata(), is(metadata));
        }

    @Test
    public void shouldCreateWithTextAndVectorAndEmptyMetadata()
        {
        Float32Vector vector = new Float32Vector(new float[]{1.0f, 2.0f, 3.0f});
        DocumentChunk chunk  = new DocumentChunk("test", vector);
        assertThat(chunk.text(), is("test"));
        assertThat(chunk.vector(), is(vector));
        assertThat(chunk.isEmbedded(), is(true));
        assertThat(chunk.metadata(), is(Map.of()));
        }

    @Test
    public void shouldSetVector()
        {
        Float32Vector vector = new Float32Vector(new float[]{1.0f, 2.0f, 3.0f});
        DocumentChunk chunk  = new DocumentChunk("test");

        assertThat(chunk.vector(), is(nullValue()));
        assertThat(chunk.isEmbedded(), is(false));

        chunk.setVector(vector);
        assertThat(chunk.vector(), is(vector));
        assertThat(chunk.isEmbedded(), is(true));
        }

    @Test
    public void shouldSetVectorFloats()
        {
        float[]       floats = {1.0f, 2.0f, 3.0f};
        Float32Vector vector = new Float32Vector(floats);
        DocumentChunk chunk  = new DocumentChunk("test");

        assertThat(chunk.vector(), is(nullValue()));
        assertThat(chunk.isEmbedded(), is(false));

        chunk.setVector(floats);
        assertThat(chunk.vector(), is(vector));
        assertThat(chunk.isEmbedded(), is(true));
        }

    // ----- data members ---------------------------------------------------

    public static final Serializer DEFAULT = new DefaultSerializer();

    public static final Serializer POF = new ConfigurablePofContext();

    public static final Serializer JSON = new JsonSerializer();
    }
