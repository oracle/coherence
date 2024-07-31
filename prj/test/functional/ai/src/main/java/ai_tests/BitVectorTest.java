/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.oracle.coherence.ai.BitVector;
import com.oracle.coherence.io.json.JsonSerializer;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BitVectorTest
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
        BitVector bitVector = new BitVector(BitSet.valueOf(new byte[]{1, 2, 3, 4}));
        Binary    binary    = ExternalizableHelper.toBinary(bitVector, serializer);
        BitVector result = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(bitVector));
        }

    @Test
    public void shouldDeserializeFromJsonString() throws Exception
        {
        String    json     = "{\"@class\":\"ai.BitVector\",\"bits\":\"0x01020304\"}";
        Object    result   = JSON.deserialize(json, Object.class);
        BitVector expected = new BitVector(BitSet.valueOf(new byte[]{1, 2, 3, 4}));
        assertThat(result, is(expected));
        }

    // ----- data members ---------------------------------------------------

    public static final DefaultSerializer DEFAULT = new DefaultSerializer();

    public static final ConfigurablePofContext POF = new ConfigurablePofContext();

    public static final JsonSerializer JSON = new JsonSerializer();
    }
