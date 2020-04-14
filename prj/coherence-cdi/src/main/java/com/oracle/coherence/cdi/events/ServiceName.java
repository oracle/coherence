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
 * A qualifier annotation used to indicate a specific service name.
 *
 * @author Aleks Seovic  2020.04.01
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceName
    {
    /**
     * The value used to identify a specific service.
     *
     * @return the value used to identify a specific service
     */
    String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link ServiceName} annotation.
     */
    class Literal
            extends AnnotationLiteral<ServiceName>
            implements ServiceName
        {
        /**
         * Construct {@link ServiceName.Literal} instance.
         *
         * @param sName  the service name
         */
        private Literal(String sName)
            {
            f_sName = sName;
            }

        /**
         * Create a {@link ServiceName.Literal}.
         *
         * @param sName  the service name
         *
         * @return a {@link ServiceName.Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name used to identify a specific service.
         *
         * @return the name used to identify a specific service
         */
        public String value()
            {
            return f_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The service name.
         */
        private final String f_sName;
        }
    }
