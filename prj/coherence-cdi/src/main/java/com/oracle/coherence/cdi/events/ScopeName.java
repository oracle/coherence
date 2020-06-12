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
 * A qualifier annotation used to indicate a specific scope name.
 *
 * @author Aleks Seovic  2020.06.11
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopeName
    {
    /**
     * Obtain the value used to identify a specific scope.
     *
     * @return value used to identify a specific scope
     */
    String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link ScopeName} annotation.
     */
    class Literal
            extends AnnotationLiteral<ScopeName>
            implements ScopeName
        {
        /**
         * Construct {@link Literal} instance.
         *
         * @param sName  the scope name
         */
        private Literal(String sName)
            {
            f_sName = sName;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param sName  the scope name
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name used to identify a specific scope.
         *
         * @return the name used to identify a specific scope
         */
        public String value()
            {
            return f_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The scope name.
         */
        private final String f_sName;
        }
    }
