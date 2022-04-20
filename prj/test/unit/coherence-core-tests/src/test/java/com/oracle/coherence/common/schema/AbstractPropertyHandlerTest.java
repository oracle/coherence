/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.schema.lang.java.JavaProperty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author as  2013.11.20
 */
public class AbstractPropertyHandlerTest
    {
    @Test
    public void testGenericArguments()
        {
        TestPropertyHandler t = new TestPropertyHandler();
        assertEquals(JavaProperty.class, t.getInternalPropertyClass());
        assertEquals(String.class, t.getExternalPropertyClass());
        }

    private static class TestPropertyHandler
            extends AbstractPropertyHandler<JavaProperty, String>
        {
        public JavaProperty createProperty(ExtensibleProperty parent)
            {
            return null;
            }

        public void importProperty(JavaProperty property, String source, Schema schema)
            {
            }
        }
    }
