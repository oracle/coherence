/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
 * A qualifier annotation used to indicate a configuration resource URI.
 *
 * @author Aleks Seovic  2020.06.15
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigUri
    {
    /**
     * The URI used to identify a specific config resource.
     *
     * @return the URI used to identify a specific config resource
     */
    @Nonbinding String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link ConfigUri} annotation.
     */
    class Literal
            extends AnnotationLiteral<ConfigUri>
            implements ConfigUri
        {
        /**
         * Construct {@link ConfigUri.Literal} instance.
         *
         * @param sURI  the config resource URI
         */
        private Literal(String sURI)
            {
            m_sURI = sURI;
            }

        /**
         * Create a {@link ConfigUri.Literal}.
         *
         * @param sURI  the config resource URI
         *
         * @return a {@link ConfigUri.Literal} with the specified value
         */
        public static Literal of(String sURI)
            {
            return new Literal(sURI);
            }

        /**
         * The name used to identify a specific resource.
         *
         * @return the name used to identify a specific resource
         */
        public String value()
            {
            return m_sURI;
            }

        // ---- data members ------------------------------------------------

        /**
         * The config resource URI.
         */
        private final String m_sURI;
        }
    }
