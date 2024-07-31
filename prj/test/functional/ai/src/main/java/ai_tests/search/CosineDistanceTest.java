/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.search;

import com.oracle.coherence.ai.distance.CosineDistance;
import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CosineDistanceTest
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
        CosineDistance<?> similarity = new CosineDistance<>();
        Binary            binary     = ExternalizableHelper.toBinary(similarity, serializer);
        CosineDistance<?> result     = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(notNullValue()));
        }
    }
