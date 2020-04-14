/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
 * specific topic name.
 *
 * @author Jonathan Knight  2019.10.23
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Topic
    {
    /**
     * The name of the topic.
     *
     * @return the name of the topic
     */
    @Nonbinding String value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link com.oracle.coherence.cdi.Topic}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<Topic>
            implements Topic
        {
        /**
         * Construct {@code Literal} instance.
         *
         * @param sName  the name of the topic
         */
        private Literal(String sName)
            {
            this.f_sName = sName;
            }

        /**
         * Create a {@link com.oracle.coherence.cdi.Topic.Literal}.
         *
         * @param sName  the name of the topic
         *
         * @return a {@link com.oracle.coherence.cdi.Topic.Literal} with the
         *         specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name of the topic.
         *
         * @return the name of the topic
         */
        public String value()
            {
            return f_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The name of the topic.
         */
        private final String f_sName;
        }
    }
