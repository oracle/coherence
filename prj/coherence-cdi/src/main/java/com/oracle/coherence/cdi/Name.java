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
 * A qualifier annotation used when injecting Coherence resource to indicate a
 * specific resource name.
 *
 * @author Jonathan Knight  2019.10.20
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Name
    {
    /**
     * The name used to identify a specific resource.
     *
     * @return the name used to identify a specific resource
     */
    @Nonbinding String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Name} annotation.
     */
    class Literal
            extends AnnotationLiteral<Name>
            implements Name
        {
        /**
         * Construct {@code Cache.Literal} instance.
         *
         * @param sName  the resource name
         */
        private Literal(String sName)
            {
            m_sName = sName;
            }

        /**
         * Create a {@link Name.Literal}.
         *
         * @param sName  the resource name
         *
         * @return a {@link Name.Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name used to identify a specific resource.
         *
         * @return the name used to identify a specific resource
         */
        public String value()
            {
            return m_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The resource name.
         */
        private final String m_sName;
        }
    }
