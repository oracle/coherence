/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;

import com.tangosol.coherence.config.CacheMapping;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;
import static com.tangosol.net.cache.TypeAssertion.withRawTypes;
import static com.tangosol.net.cache.TypeAssertion.withTypes;


/**
 * Unit tests for TypeAssertion.
 *
 * @author jf  2015.08.06
 */

public class TypeAssertionTest
    {
    @Test
    public void WithTypesAssertionTest()
        {
            TypeAssertion<MyKey, MyValue> assertion = withTypes(MyKey.class, MyValue.class);
            CacheMapping                  mapping   = mock(CacheMapping.class);

            when(mapping.usesRawTypes()).thenReturn(false);
            when(mapping.getKeyClassName()).thenReturn(MyKey.class.getName());
            when(mapping.getValueClassName()).thenReturn(MyValue.class.getName());
            assertTrue(assertion.assertTypeSafety("TypeAssertionTestCache", mapping));
        }

    @Test(expected = IllegalArgumentException.class)
    public void invalidParameterWithTypesTest()
        {
        withTypes(null, String.class);
        }

    @Test
    public void warningUseRawTypesWithNonRawTypesMappingTest()
        {
        CacheMapping                  mapping   = mock(CacheMapping.class);

        when(mapping.usesRawTypes()).thenReturn(false);
        when(mapping.getKeyClassName()).thenReturn(MyKey.class.getName());
        when(mapping.getValueClassName()).thenReturn(MyValue.class.getName());

        assertFalse(withRawTypes().assertTypeSafety("TypeAssertionTestCache", mapping));
        }

    @Test
    public void warningWithTypesWithMappingRawTypesTest()
        {
        TypeAssertion<MyKey, MyValue> assertion = withTypes(MyKey.class, MyValue.class);
        CacheMapping                  mapping   = mock(CacheMapping.class);

        when(mapping.usesRawTypes()).thenReturn(true);
        assertFalse(assertion.assertTypeSafety("TypeAssertionTestCache", mapping));
        }


    @Test
    public void WithTypesAssertionEqualsTest()
        {
        TypeAssertion<MyKey, MyValue> assertion = withTypes(MyKey.class, MyValue.class);

        assertTrue(assertion.equals(assertion));
        assertEquals(assertion.hashCode(), assertion.hashCode());

        assertTrue(assertion.equals(withTypes(MyKey.class, MyValue.class)));
        assertEquals(assertion.hashCode(), withTypes(MyKey.class, MyValue.class).hashCode());

        assertFalse(assertion.equals(withTypes(MyKey.class, String.class)));
        }

    @Test
    public void TypeAssertionEqualsTest()
        {
        assertEquals(withRawTypes(), withRawTypes());
        assertEquals(withoutTypeChecking(), withoutTypeChecking());
        assertNotEquals(withRawTypes(), withoutTypeChecking());
        assertNotEquals(withRawTypes(), withTypes(MyKey.class, MyValue.class));
        assertNotEquals(withoutTypeChecking(), withTypes(MyKey.class, MyValue.class));
        }

    @Test(expected = IllegalArgumentException.class)
    public void ensureThrowsIllegalArgumentWithTypesAssertion1Test()
        {
        TypeAssertion<String, MyValue> assertion = withTypes(String.class, MyValue.class);
        CacheMapping                  mapping   = mock(CacheMapping.class);

        when(mapping.usesRawTypes()).thenReturn(false);
        when(mapping.getKeyClassName()).thenReturn(MyKey.class.getName());
        when(mapping.getValueClassName()).thenReturn(MyValue.class.getName());
        assertion.assertTypeSafety("TypeAssertionTestCache", mapping);
        }

    public static class MyKey
        {
        }

    public static class MyValue
        {
        }
    }
