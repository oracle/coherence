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
 * A qualifier annotation used when injecting Coherence resource to indicate
 * that those resource should be obtained from a specific {@link
 * com.tangosol.net.ConfigurableCacheFactory}.
 *
 * @author Jonathan Knight  2019.10.20
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Session
    {
    /**
     * The URI used to identify a specific {@link
     * com.tangosol.net.ConfigurableCacheFactory}.
     *
     * @return the URI used to identify a specific
     *         {@link com.tangosol.net.ConfigurableCacheFactory}
     */
    @Nonbinding String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Session} annotation.
     */
    class Literal
            extends AnnotationLiteral<Session>
            implements Session
        {
        /**
         * Construct {@code Literal} instacne.
         *
         * @param sUri  the URI used to identify a specific
         *              {@link com.tangosol.net.ConfigurableCacheFactory}
         */
        private Literal(String sUri)
            {
            m_sValue = sUri;
            }

        /**
         * Create a {@link Session.Literal}.
         *
         * @param sUri  the URI used to identify a specific
         *              {@link com.tangosol.net.ConfigurableCacheFactory}
         *
         * @return a {@link Session.Literal} with the specified URI
         */
        public static Literal of(String sUri)
            {
            return new Literal(sUri);
            }

        /**
         * Obtain the name value.
         *
         * @return the name value
         */
        public String value()
            {
            return m_sValue;
            }

        // ---- data members ------------------------------------------------

        /**
         * The name value for this literal.
         */
        private final String m_sValue;
        }
    }
