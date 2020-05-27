/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cache.grpc.client;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

/**
 * A qualifier annotation used when injecting a remote Coherence
 * resource to indicate a specific remote cache name.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteCache
    {
    /**
     * Obtain the value used to identify a specific cache name.
     *
     * @return value used to identify a specific cache name
     */
    @Nonbinding String value() default "";

    // ----- inner class: Literal -------------------------------------------

    /**
     * An annotation literal for the {@link RemoteCache} annotation.
     */
    class Literal
            extends AnnotationLiteral<RemoteCache>
            implements RemoteCache
        {
        // ----- constructors -----------------------------------------------

        protected Literal(String sValue)
            {
            this.f_sValue = sValue;
            }

        // ----- RemoteCache interface --------------------------------------

        /**
         * Obtain the name value.
         *
         * @return the name value
         */
        public String value()
            {
            return f_sValue;
            }

        // ----- public methods ---------------------------------------------

        /**
         * Create a {@link Literal}.
         *
         * @param value the name value
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(String value)
            {
            return new Literal(value);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name value for this literal.
         */
        protected final String f_sValue;
        }
    }
