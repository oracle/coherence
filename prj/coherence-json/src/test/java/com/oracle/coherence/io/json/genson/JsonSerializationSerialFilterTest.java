/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.genson;

import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.util.Base;

import java.io.ObjectInputFilter;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JSON serialization tests.
 *
 * @author rl 20224.19
 * @since 22.06
 */
public class JsonSerializationSerialFilterTest
    {
    @Test
    void testSerializationWithJdkSerialFilter()
        {
        ObjectInputFilter.Config.setSerialFilter(ObjectInputFilter.Config.createFilter("!java.util.Date"));

        JsonSerializer serializer = new JsonSerializer(Base.getContextClassLoader(),
                                                       builder -> builder.setEnforceTypeAliases(false),
                                                       false);
        ByteArrayWriteBuffer buf  = new ByteArrayWriteBuffer(512);

        assertThrows(JsonBindingException.class, () -> serializer.serialize(buf.getBufferOutput(), new Date()));
        }
    }
