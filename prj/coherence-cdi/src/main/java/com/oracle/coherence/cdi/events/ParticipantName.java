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
 * A qualifier annotation used to indicate a specific participant name.
 *
 * @author Aleks Seovic  2020.04.13
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ParticipantName
    {
    /**
     * The participant name.
     *
     * @return the participant name
     */
    String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link ParticipantName} annotation.
     */
    class Literal
            extends AnnotationLiteral<ParticipantName>
            implements ParticipantName
        {
        /**
         * Construct {@link ParticipantName.Literal} instance.
         *
         * @param sName  the participant name
         */
        private Literal(String sName)
            {
            f_sName = sName;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param sName  the participant name
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The participant name.
         *
         * @return the participant name
         */
        public String value()
            {
            return f_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The participant name.
         */
        private final String f_sName;
        }
    }
