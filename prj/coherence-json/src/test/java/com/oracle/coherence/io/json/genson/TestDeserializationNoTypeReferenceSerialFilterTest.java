/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json.genson;

import com.oracle.coherence.io.json.JsonSerializer;

import com.oracle.coherence.io.json.genson.bean.Image;

import com.tangosol.io.ByteArrayReadBuffer;

import com.tangosol.util.Base;

import java.io.ObjectInputFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JSON Deserialization test with serialization filtering enabled
 * and passing no explicit type reference.
 *
 * @author rl 20224.19
 * @since 22.06
 */
public class TestDeserializationNoTypeReferenceSerialFilterTest
    {
    @Test
    void testDeserializationImplicitTypeWithJdkSerialFilter()
        {
        String              sPayload   = "{\"@class\":\"com.oracle.coherence.io.json.genson.bean.Image\",\"height\":0,\"width\":0}";
        ByteArrayReadBuffer buf        = new ByteArrayReadBuffer(sPayload.getBytes());

        ObjectInputFilter.Config.setSerialFilter(ObjectInputFilter.Config.createFilter("!" + Image.class.getName() + ";java.lang.Object"));

        JsonSerializer serializer = new JsonSerializer(Base.getContextClassLoader(),
                                                       builder -> builder.setEnforceTypeAliases(false),
                                                       false);

        assertThrows(JsonBindingException.class, () -> serializer.deserialize(buf.getBufferInput()));
        }
    }
