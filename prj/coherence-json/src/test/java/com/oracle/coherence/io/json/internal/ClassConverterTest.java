/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.reflect.VisibilityFilter;

import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link ClassConverter}.
 *
 * @since 20.06
 */
class ClassConverterTest
    {
    // ----- test setup -----------------------------------------------------

    @BeforeAll
    static void configure()
        {
        s_genson = new GensonBuilder()
                .useClassMetadata(true)
                .useClassMetadataWithStaticType(false)
                .useFields(true, VisibilityFilter.PRIVATE)
                .useMethods(false)
                .useIndentation(false)
                .withConverterFactory(ClassConverter.Factory.INSTANCE)
                .create();
        }

    // ----- test cases -----------------------------------------------------

    @SuppressWarnings("RedundantThrows")
    @Test
    void testBeanWithClassField() throws Exception
        {
        final BeanWithClassField expected = new BeanWithClassField(Long.class);
        assertEquals(expected, s_genson.deserialize(s_genson.serialize(expected), BeanWithClassField.class));
        }

    // ----- inner class: BeanWithClassField --------------------------------

    public static class BeanWithClassField
        {
        // ----- constructors -----------------------------------------------

        @SuppressWarnings("unused")
        public BeanWithClassField()
            {
            }

        // ----- public methods ---------------------------------------------

        public BeanWithClassField(Class<?> clazz)
            {
            this.m_clazz = clazz;
            }

        // ----- Object methods ---------------------------------------------

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
            BeanWithClassField that = (BeanWithClassField) o;
            return Objects.equals(m_clazz, that.m_clazz);
            }

        public int hashCode()
            {
            return Objects.hash(m_clazz);
            }

        // ----- data members -----------------------------------------------

        protected Class<?> m_clazz;
        }

    // ----- data members ---------------------------------------------------

    protected static Genson s_genson;
    }
