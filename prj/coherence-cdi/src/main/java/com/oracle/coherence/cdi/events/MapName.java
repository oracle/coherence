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
 * A qualifier annotation used to indicate a specific map name.
 *
 * @author Aleks Seovic  2020.06.09
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface MapName
    {
    /**
     * Obtain the value used to identify a specific map.
     *
     * @return value used to identify a specific map
     */
    String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link MapName} annotation.
     */
    class Literal
            extends AnnotationLiteral<MapName>
            implements MapName
        {
        /**
         * Construct {@link Literal} instance.
         *
         * @param sName  the map name
         */
        private Literal(String sName)
            {
            f_sName = sName;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param sName  the map name
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name used to identify a specific map.
         *
         * @return the name used to identify a specific map
         */
        public String value()
            {
            return f_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The map name.
         */
        private final String f_sName;
        }
    }
