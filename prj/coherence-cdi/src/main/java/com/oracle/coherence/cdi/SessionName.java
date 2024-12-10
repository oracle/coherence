/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.Coherence;

import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation used when injecting Coherence resource to indicate a
 * specific Session name.
 *
 * @author Jonathan Knight  2020.11.05
 * @since 20.12
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface SessionName
    {
    /**
     * The name used to identify a specific session.
     *
     * @return the name used to identify a specific session
     */
    @Nonbinding String value() default Coherence.DEFAULT_NAME;

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link SessionName} annotation.
     */
    class Literal
            extends AnnotationLiteral<SessionName>
            implements SessionName
        {
        /**
         * Construct {@link Literal} instance.
         *
         * @param sName  the session name
         */
        private Literal(String sName)
            {
            m_sName = sName;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param sName  the session name
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name used to identify a specific session.
         *
         * @return the name used to identify a specific session
         */
        public String value()
            {
            return m_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The session name.
         */
        private final String m_sName;
        }
    }
