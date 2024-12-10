/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.util.UID;
import com.tangosol.util.UUID;

import java.util.Date;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link DefaultKeyConverter}.
 *
 * @author ic  2011.06.27
 */
public class DefaultKeyConverterTest
    {
    /**
     * Test for {@link DefaultKeyConverter#toString(Object)} method.
     */
    @SuppressWarnings({"deprecation"})
    @Test
    public void testKeyToString()
        {
        assertEquals("6", new DefaultKeyConverter(Integer.class).toString(6));
        assertEquals("9", new DefaultKeyConverter(Long.class).toString(9L));
        assertEquals("key", new DefaultKeyConverter(String.class).toString("key"));
        assertEquals("true", new DefaultKeyConverter(Boolean.class).toString("true"));
        assertEquals("false", new DefaultKeyConverter(Boolean.class).toString("false"));
        assertEquals("2011-06-20", new DefaultKeyConverter(Date.class).toString(new Date(111, 5, 20)));
        assertEquals("00000130D616F9160A00007D5103F178888311EE87130690C126C92D01BD31B8",
                     new DefaultKeyConverter(UUID.class).toString(
                         new UUID("00000130D616F9160A00007D5103F178888311EE87130690C126C92D01BD31B8")));
        assertEquals("0AAF0A1400000130D61A4AD3D7287649",
                     new DefaultKeyConverter(UID.class).toString(new UID("0AAF0A1400000130D61A4AD3D7287649")));
        assertEquals("17b3fb90-ed89-410f-b971-6b90dd1ae078",
                     new DefaultKeyConverter(java.util.UUID.class).toString(
                         java.util.UUID.fromString("17b3fb90-ed89-410f-b971-6b90dd1ae078")));
        }

    /**
     * Test for {@link DefaultKeyConverter#fromString(String)} method.
     */
    @SuppressWarnings({"deprecation"})
    @Test
    public void testKeyFromString()
        {
        assertEquals(6, new DefaultKeyConverter(Integer.class).fromString("6"));
        assertEquals(11L, new DefaultKeyConverter(Long.class).fromString("11"));
        assertEquals("key", new DefaultKeyConverter(String.class).fromString("key"));
        assertEquals(true, new DefaultKeyConverter(Boolean.class).fromString("true"));
        assertEquals(false, new DefaultKeyConverter(Boolean.class).fromString("false"));
        assertEquals(new Date(111, 5, 20), new DefaultKeyConverter(Date.class).fromString("2011-06-20"));
        assertEquals(new UUID("00000130D616F9160A00007D5103F178888311EE87130690C126C92D01BD31B8"),
                     new DefaultKeyConverter(UUID.class).fromString(
                         "00000130D616F9160A00007D5103F178888311EE87130690C126C92D01BD31B8"));
        assertEquals(new UID("0AAF0A1400000130D61A4AD3D7287649"),
                     new DefaultKeyConverter(UID.class).fromString("0AAF0A1400000130D61A4AD3D7287649"));
        assertEquals(java.util.UUID.fromString("17b3fb90-ed89-410f-b971-6b90dd1ae078"),
                     new DefaultKeyConverter(java.util.UUID.class).fromString("17b3fb90-ed89-410f-b971-6b90dd1ae078"));
        }


    /**
     * Test for different key creation strategies (constructor that accepts a single string argument, or for a static
     * fromString, parse and valueOf method).
     */
    @Test
    public void testKeyCreation()
        {
        assertEquals(new CtorKey("ctorKey"), new DefaultKeyConverter(CtorKey.class).fromString("ctorKey"));
        assertEquals(new FromStringKey("fromStringKey"),
                     new DefaultKeyConverter(FromStringKey.class).fromString("fromStringKey"));
        assertEquals(new ParseKey("parseKey"), new DefaultKeyConverter(ParseKey.class).fromString("parseKey"));
        assertEquals(new ValueOfKey("valueOfKey"), new DefaultKeyConverter(ValueOfKey.class).fromString("valueOfKey"));
        }

    /**
     * Test exception thrown when invalid key class is used.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidKeyCreation()
        {
        assertEquals(new InvalidKey("invalid"), new DefaultKeyConverter(InvalidKey.class).fromString("invalid"));
        }

    // ---- inner class: AbstractKey ----------------------------------------

    /**
     * Base key class used for testing purposies.
     */
    static abstract class AbstractKey
        {
        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            AbstractKey that = (AbstractKey) o;
            return m_sKey.equals(that.m_sKey);
            }

        @Override
        public int hashCode()
            {
            return m_sKey.hashCode();
            }

        @Override
        public String toString()
            {
            return m_sKey;
            }

        protected String m_sKey;
        }

    // ---- inner class: CtorKey --------------------------------------------

    /**
     * Key class with a public constructor that accepts a single string argument.
     */
    static class CtorKey
            extends AbstractKey
        {
        public CtorKey(String key)
            {
            m_sKey = key;
            }
        }

    // ---- inner class: ValueOfKey -----------------------------------------

    /**
     * Key class with static valueOf method.
     */
    static class ValueOfKey
            extends AbstractKey
        {
        private ValueOfKey(String key)
            {
            m_sKey = key;
            }

        public static ValueOfKey valueOf(String s)
            {
            return new ValueOfKey(s);
            }
        }

    // ---- inner class: ParseKey -----------------------------------------

    /**
     * Key class with static parse method.
     */
    static class ParseKey
            extends AbstractKey
        {
        private ParseKey(String key)
            {
            m_sKey = key;
            }

        public static ParseKey parse(String s)
            {
            return new ParseKey(s);
            }
        }

    // ---- inner class: FromStringKey -----------------------------------------

    /**
     * Key class with static fromString method.
     */
    static class FromStringKey
            extends AbstractKey
        {
        private FromStringKey(String key)
            {
            m_sKey = key;
            }

        public static FromStringKey fromString(String s)
            {
            return new FromStringKey(s);
            }
        }

    // ---- inner class: InvalidKey -----------------------------------------

    /**
     * Invalid key class (without public constructor that accepts a single
     * string argument, or static fromString, parse or valueOf method.
     */
    static class InvalidKey
            extends AbstractKey
        {
        private InvalidKey(String key)
            {
            m_sKey = key;
            }
        }
    }
