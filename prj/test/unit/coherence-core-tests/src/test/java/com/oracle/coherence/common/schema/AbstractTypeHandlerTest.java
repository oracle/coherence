/*
 * Copyright (c) 2000, 2022, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.schema.lang.java.JavaType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author as  2013.11.20
 */
public class AbstractTypeHandlerTest
    {
    @Test
    public void testGenericArguments()
        {
        TestTypeHandler t = new TestTypeHandler();
        assertEquals(JavaType.class, t.getInternalTypeClass());
        assertEquals(Integer.class, t.getExternalTypeClass());
        }

    private static class TestTypeHandler
            extends AbstractTypeHandler<JavaType, Integer>
        {
        public JavaType createType(ExtensibleType parent)
            {
            return null;
            }

        public void importType(JavaType type, Integer source, Schema schema)
            {
            }
        }
    }
