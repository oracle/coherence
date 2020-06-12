/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.tangosol.io.pof.SimplePofContext;

import io.grpc.StatusRuntimeException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Aleks Seovic  2019.10.11
 */
class PofMarshallerTest
    {
    @Test
    void testConfigFile()
        {
        PofMarshaller<String> m = new PofMarshaller<>("pof-config.xml", String.class);
        assertThat(m.parse(m.stream("test")), is("test"));
        }

    @Test
    void testPofContext()
        {
        PofMarshaller<String> m = new PofMarshaller<>(new SimplePofContext(), String.class);
        assertThat(m.parse(m.stream("test")), is("test"));
        }

    @Test
    void testStreamException()
        {
        PofMarshaller<Object> m = new PofMarshaller<>(new SimplePofContext(), Object.class);
        assertThrows(StatusRuntimeException.class, () -> m.stream(new Object()));
        }

    @Test
    void testParseException()
        {
        PofMarshaller<Object> m = new PofMarshaller<>(new SimplePofContext(), Object.class);
        assertThrows(StatusRuntimeException.class, () -> m.parse(null));
        }
    }
