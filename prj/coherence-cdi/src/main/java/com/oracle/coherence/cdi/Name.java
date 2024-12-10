/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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

    /**
     * Return {@code true} to indicate whether name is a regular expression.
     *
     * @return {@code true} to indicate whether name is a regular expression
     */
    @Nonbinding boolean regex() default false;

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Name} annotation.
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Literal
            extends AnnotationLiteral<Name>
            implements Name
        {
        /**
         * Construct {@link Name.Literal} instance.
         *
         * @param sName  the resource name
         */
        private Literal(String sName)
            {
            this(sName, false);
            }

        /**
         * Construct {@link Name.Literal} instance.
         *
         * @param sName   the resource name
         * @param fRegex  {@code true} to indicate whether name is a regular expression
         */
        public Literal(String sName, boolean fRegex)
            {
            m_sName  = sName;
            m_fRegex = fRegex;
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
        @Override
        public String value()
            {
            return m_sName;
            }

        /**
         * Return {@code true} to indicate whether name is a regular expression.
         *
         * @return {@code true} to indicate whether name is a regular expression
         */
        @Override
        public boolean regex()
            {
            return m_fRegex;
            }

        // ---- data members ------------------------------------------------

        /**
         * The resource name.
         */
        private final String m_sName;

        /**
         * {@code true} to indicate whether name is a regular expression.
         */
        private final boolean m_fRegex;
        }
    }
