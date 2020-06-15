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
 * A qualifier annotation used for any BACKLOG event.
 *
 * @author Aleks Seovic  2020.04.13
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Backlog
    {
    /**
     * Obtain the type of backlog event.
     *
     * @return the type of backlog event
     */
     Type value();

    // ---- inner class: Type -----------------------------------------------

    /**
     * The backlog event type.
     */
    enum Type
        {
        /**
         * Indicates that a participant was previously
         * backlogged but is no longer so.
         */
        NORMAL,

        /**
         * Indicates that a participant is backlogged; if
         * the participant is remote it indicates the
         * remote participant has more work than it can handle;
         * if the participant is local it indicates this
         * participant has more work than it can handle.
         */
        EXCESSIVE
        }

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Backlog} annotation.
     */
    class Literal
            extends AnnotationLiteral<Backlog>
            implements Backlog
        {
        /**
         * Construct {@link Literal} instance.
         *
         * @param type  the backlog event type
         */
        private Literal(Type type)
            {
            f_type = type;
            }

        /**
         * Create a {@link Literal}.
         *
         * @param type  the backlog event type
         *
         * @return a {@link Literal} with the specified value
         */
        public static Literal of(Type type)
            {
            return new Literal(type);
            }

        /**
         * The backlog event type.
         *
         * @return the backlog event type
         */
        public Type value()
            {
            return f_type;
            }

        // ---- data members ------------------------------------------------

        /**
         * The backlog event type.
         */
        private final Type f_type;
        }
    }
