/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

/**
 * A qualifier annotation used when injecting a {@link com.tangosol.io.Serializer}
 * to identify the specific {@link com.tangosol.io.Serializer} to inject.
 *
 * @author Jonathan Knight  2019.11.20
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializerFormat
    {
    /**
     * Obtain the value used to identify a specific serializer.
     *
     * @return value used to identify a specific serializer
     */
    @Nonbinding String value();

    // ---- inner class: Literal ----------------------------------------

    /**
     * An annotation literal for the {@link SerializerFormat} annotation.
     */
    class Literal
            extends AnnotationLiteral<SerializerFormat>
            implements SerializerFormat
        {
        /**
         * Construct {@code Literal} instance.
         *
         * @param sName  the name of the serializer
         */
        private Literal(String sName)
            {
            this.m_sName = sName;
            }

        /**
         * Create a {@link SerializerFormat.Literal}.
         *
         * @param sName  the name of the serializer
         *
         * @return a {@link SerializerFormat.Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name of the serializer.
         *
         * @return the name of the serializer
         */
        public String value()
            {
            return m_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The name of the serializer.
         */
        private final String m_sName;
        }
    }
