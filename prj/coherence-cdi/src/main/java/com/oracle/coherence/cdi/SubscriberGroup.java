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
 * A qualifier annotation used when injecting {@link com.tangosol.net.topic.Subscriber}
 * to a {@link com.tangosol.net.topic.NamedTopic} to indicate the name of the
 * subscriber group that the subscriber should belong to.
 *
 * @author Jonathan Knight  2019.10.23
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscriberGroup
    {
    /**
     * The name of the subscriber group.
     *
     * @return the name of the subscriber group
     */
    @Nonbinding String value();

    // ---- inner class: Literal --------------------------------------------
    
    /**
     * An annotation literal for the {@link SubscriberGroup} annotation.
     */
    class Literal
            extends AnnotationLiteral<SubscriberGroup>
            implements SubscriberGroup
        {
        /**
         * Construct {@code Literal} instance
         * 
         * @param sName  the name of the subscriber group
         */
        private Literal(String sName)
            {
            f_sName = sName;
            }

        /**
         * Create a {@link SubscriberGroup.Literal}.
         *
         * @param sName  the name of the subscriber group
         *
         * @return a {@link SubscriberGroup.Literal} with the specified value
         */
        public static Literal of(String sName)
            {
            return new Literal(sName);
            }

        /**
         * The name of the subscriber group.
         *
         * @return the name of the subscriber group
         */
        public String value()
            {
            return f_sName;
            }

        // ---- data members ------------------------------------------------

        /**
         * The name of the subscriber group.
         */
        private final String f_sName;
        }
    }
