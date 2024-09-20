/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.io.json.JsonSerializer;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Float32VectorTest
    {
    @Test
    public void shouldSerializeUsingJava()
        {
        shouldSerialize(new DefaultSerializer());
        }

    @Test
    public void shouldSerializeUsingPof()
        {
        shouldSerialize(new ConfigurablePofContext());
        }

    @Test
    public void shouldSerializeUsingJson()
        {
        shouldSerialize(new JsonSerializer());
        }

    public void shouldSerialize(Serializer serializer)
        {
        Float32Vector float32Vector = new Float32Vector(new float[]{1.0f, 2.0f, 3.0f, 4.0f});
        Binary        binary        = ExternalizableHelper.toBinary(float32Vector, serializer);
        Float32Vector result        = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(float32Vector));
        }
    }
