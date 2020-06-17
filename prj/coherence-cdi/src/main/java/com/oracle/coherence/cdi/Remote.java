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
 * A qualifier annotation used to indicate that the resource bing injected
 * is remote with the option to specify a name to further qualify the remote
 * connection.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Remote
    {
    /**
     * The name used to identify a specific remote connection.
     *
     * @return the name used to identify a specific remote connection
     */
    @Nonbinding String value() default DEFAULT_NAME;

    /**
     * Predefined constant for default remote connection name.
     */
    String DEFAULT_NAME = "default";

    // ----- inner class: Literal -------------------------------------------

    /**
     * An annotation literal for the {@link Remote} annotation.
     */
    class Literal
            extends AnnotationLiteral<Remote>
            implements Remote
        {
        /**
         * Construct {@code Literal} instacne.
         *
         * @param sValue  the scope name or URI used to identify a specific
         *                {@link com.tangosol.net.ConfigurableCacheFactory}
         */
        private Literal(String sValue)
            {
            m_sValue = sValue;
            }

        /**
         * Create a {@link Remote.Literal}.
         *
         * @param sValue  the remote connection name.
         *
         * @return a {@link Remote.Literal} with the specified name
         */
        public static Literal of(String sValue)
            {
            return new Literal(sValue);
            }

        /**
         * Obtain the remote connection name.
         *
         * @return the remote connection name
         */
        public String value()
            {
            return m_sValue;
            }

        // ----- constants  -------------------------------------------------

        public static final Remote.Literal DEFAULT = new Remote.Literal(DEFAULT_NAME);

        // ---- data members ------------------------------------------------

        /**
         * The remote connection name.
         */
        private final String m_sValue;
        }
    }
