/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.inject;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation used to hold multiple repeatable {@link Name} annotations.
 *
 * @author Jonathan Knight  2020.11.26
 * @since 20.12
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Names
    {
    /**
     * The name used to identify a specific resource.
     *
     * @return the name used to identify a specific resource
     */
    @Nonbinding Name[] value() default {};

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Names} annotation.
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Literal
            extends AnnotationLiteral<Names>
            implements Names
        {
        /**
         * Construct {@link Literal} instance.
         *
         * @param aName  the repeated {@link Name} annotations
         */
        private Literal(Name... aName)
            {
            m_aName = aName;
            }

        /**
         * Create a {@link Names} {@link Literal}.
         *
         * @param aName  the repeated {@link Name} annotations
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(Name... aName)
            {
            return new Literal(aName);
            }

        /**
         * The name used to identify a specific resource.
         *
         * @return the name used to identify a specific resource
         */
        public Name[] value()
            {
            return m_aName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The repeatable {@link Name} annotations.
         */
        private final Name[] m_aName;
        }
    }
