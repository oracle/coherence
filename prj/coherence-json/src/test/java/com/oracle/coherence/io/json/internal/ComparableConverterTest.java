/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.ext.jsonb.JsonbBundle;
import com.oracle.coherence.io.json.genson.reflect.VisibilityFilter;

import java.util.Objects;

import javax.json.bind.annotation.JsonbProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link ComparableConverter}.
 *
 * @since 20.06
 */
class ComparableConverterTest
    {
    // ----- test configuration ---------------------------------------------

    @BeforeAll
    static void configure()
        {
        s_genson = new GensonBuilder()
                .useClassMetadata(true)
                .useClassMetadataWithStaticType(false)
                .useFields(true, VisibilityFilter.PRIVATE)
                .useMethods(false)
                .useIndentation(true)
                .withConverterFactory(ComparableConverter.Factory.INSTANCE)
                .create();
        }

    // ----- test cases -----------------------------------------------------

    @Test
    void testDeserializationOfStringToComparable()
        {
        Comparable result = s_genson.deserialize("\"Hello\"", Comparable.class);
        assertTrue(result instanceof String);
        assertEquals("Hello", result);
        }

    @Test
    void testDeserializationOfNumberToComparable()
        {
        Comparable result = s_genson.deserialize("" + Integer.MAX_VALUE, Comparable.class);
        assertTrue(result instanceof Integer);
        assertEquals(Integer.MAX_VALUE, result);

        result = s_genson.deserialize("" + Long.MAX_VALUE, Comparable.class);
        assertTrue(result instanceof Long);
        assertEquals(Long.MAX_VALUE, result);

        result = s_genson.deserialize("" + Double.MAX_VALUE, Comparable.class);
        assertTrue(result instanceof Double);
        assertEquals(Double.MAX_VALUE, result);
        }

    @Test
    void testDeserializationOfBooleanToComparable()
        {
        Comparable result = s_genson.deserialize("true", Comparable.class);
        assertTrue(result instanceof Boolean);
        assertTrue(Boolean.valueOf(result.toString()));
        }

    @SuppressWarnings("unchecked")
    @Test
    void testDeserializationOfCustomComparable()
        {
        Genson         genson = new GensonBuilder().useClassMetadata(true).withBundle(new JsonbBundle()).create();
        ComparableBean result = genson.deserialize(
                "{\"@class\":\"com.oracle.coherence.io.json.internal.ComparableConverterTest$ComparableBean\","
                + "\"value\":{\"@class\":\"com.oracle.coherence.io.json.internal"
                + ".ComparableConverterTest$CustomComparable\",\"value\":\"test\"}}",
                ComparableBean.class);
        assertEquals(new ComparableBean(new CustomComparable("test")), result);
        }

    // ----- inner class: ComparableBean ------------------------------------

    public static class ComparableBean<T extends Comparable>
        {
        // ----- constructors -----------------------------------------------

        @SuppressWarnings("unused")
        public ComparableBean()
            {
            }

        public ComparableBean(final T value)
            {
            this.m_value = value;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(final Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            final ComparableBean<?> that = (ComparableBean<?>) o;
            return Objects.equals(m_value, that.m_value);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_value);
            }

        // ----- data members -----------------------------------------------

        @JsonbProperty("value")
        protected T m_value;
        }

    // ----- inner class: CustomComparable ----------------------------------

    public static class CustomComparable
            implements Comparable<String>
        {
        // ----- constructors -----------------------------------------------

        @SuppressWarnings("unused")
        public CustomComparable()
            {
            }

        public CustomComparable(final String sValue)
            {
            this.m_sValue = sValue;
            }

        // ----- Comparable interface ---------------------------------------

        @Override
        public int compareTo(final String o)
            {
            return 0;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(final Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            final CustomComparable that = (CustomComparable) o;
            return Objects.equals(m_sValue, that.m_sValue);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_sValue);
            }

        // ----- data members -----------------------------------------------

        @JsonbProperty("value")
        protected String m_sValue;
        }

    // ----- data members ---------------------------------------------------

    protected static Genson s_genson;
    }
