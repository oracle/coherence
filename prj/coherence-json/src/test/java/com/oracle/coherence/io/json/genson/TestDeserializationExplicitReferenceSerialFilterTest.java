/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.genson;

import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.util.Base;

import java.io.IOException;

import java.io.ObjectInputFilter;
import java.nio.ByteBuffer;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JSON Deserialization test with serialization filtering enabled
 * and passing an explicit type reference.
 *
 * @author rl 20224.19
 * @since 22.06
 */
public class TestDeserializationExplicitReferenceSerialFilterTest
    {
    @Test
    void testDeserializationExplicitTypeWithJdkSerialFilter()
        {
        ObjectInputFilter.Config.setSerialFilter(ObjectInputFilter.Config.createFilter("!java.util.Date"));

        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putLong(new Date().getTime());

        ByteArrayReadBuffer   buf        = new ByteArrayReadBuffer(byteBuffer.array());
        JsonSerializer        serializer = new JsonSerializer(Base.getContextClassLoader(),
                                                              builder -> builder.setEnforceTypeAliases(false),
                                                              false);

        assertThrows(JsonBindingException.class, () -> serializer.deserialize(buf.getBufferInput(), Date.class));
        }
    }
