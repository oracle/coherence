/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;

import javax.inject.Qualifier;

/**
 * A qualifier annotation used to indicate a specific cache name.
 *
 * @author Aleks Seovic  2020.04.01
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheName
    {
    /**
     * Obtain the value used to identify a specific cache.
     *
     * @return value used to identify a specific cache
     */
    String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link CacheName} annotation.
     */
    class Literal
            extends AnnotationLiteral<CacheName>
            implements CacheName
        {
        /**
         * Construct {@link CacheName.Literal} instance.
         *
         * @param sName  the cache name
         */
        private Literal(String sName)
            {
            f_sName = sName;
            }

        /**
         * Create a {@link CacheName.Literal}.
         *
         * @param sName  the cache name
         *
         * @return a {@link CacheName.Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name used to identify a specific cache.
         *
         * @return the name used to identify a specific cache
         */
        public String value()
            {
            return f_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The cache name.
         */
        private final String f_sName;
        }
    }
