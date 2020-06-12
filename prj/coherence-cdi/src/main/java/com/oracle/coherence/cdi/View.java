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
 * A qualifier annotation used when injecting a cache view.
 *
 * @author Jonathan Knight  2019.10.24
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface View
    {
    /**
     * A flag that is {@code true} to cache both the keys and values of the
     * materialized view locally, or {@code false} to only cache the keys (the
     * default value is {@code true}).
     *
     * @return {@code true} to indicate that values should be cached or
     *         {@code false} to indicate that only keys should be cached
     */
    @Nonbinding boolean cacheValues() default true;

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link View} annotation.
     */
    class Literal
            extends AnnotationLiteral<View>
            implements View
        {
        /**
         * Construct {@code Literal} instance.
         *
         * @param fCacheValues a flag that is {@code true} to cache both the keys
         *                     and values of the materialized view locally, or
         *                     {@code false} to only cache the keys
         */
        private Literal(boolean fCacheValues)
            {
            this.f_fCacheValues = fCacheValues;
            }

        /**
         * Create a {@link View.Literal}.
         *
         * @param fCacheValues a flag that is {@code true} to cache both the keys
         *                     and values of the materialized view locally, or
         *                     {@code false} to only cache the keys
         *
         * @return a {@link View.Literal} with the specified value
         */
        public static Literal of(boolean fCacheValues)
            {
            return new Literal(fCacheValues);
            }

        /**
         * Obtain the flag that is {@code true} to cache both the keys and
         * values of the materialized view locally, or {@code false} to only
         * cache the keys (the default value is {@code true}).
         *
         * @return {@code true} to indicate that values should be cache or
         * {@code false} to indicate that only keys should be cached.
         */
        @Override
        public boolean cacheValues()
            {
            return f_fCacheValues;
            }

        // ---- constants -------------------------------------------------------

        /**
         * A singleton instance of {@link View.Literal}
         * with the cache values flag set to true.
         */
        public static final Literal INSTANCE = Literal.of(true);

        // ---- data members ----------------------------------------------------

        /**
         * A flag that is {@code true} to cache both the keys and values of the
         * materialized view locally, or {@code false} to only cache the keys.
         */
        private final boolean f_fCacheValues;
        }
    }
