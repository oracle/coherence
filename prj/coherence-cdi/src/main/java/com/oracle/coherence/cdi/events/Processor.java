/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import com.oracle.coherence.cdi.AnnotationLiteral;
import com.tangosol.util.InvocableMap;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation used to indicate processor class when observing {@link
 * com.tangosol.net.events.annotation.EntryProcessorEvents}.
 *
 * @author Aleks Seovic  2020.04.01
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Processor
    {
    /**
     * The processor class.
     *
     * @return the processor class
     */
    Class<? extends InvocableMap.EntryProcessor> value();

    // ---- inner class: Literal --------------------------------------------

    /**
     * An annotation literal for the {@link Processor}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<Processor>
            implements Processor
        {
        /**
         * Construct {@code Literal} instance.
         *
         * @param clzProcessor  the processor class
         */
        private Literal(Class<? extends InvocableMap.EntryProcessor> clzProcessor)
            {
            this.f_clzProcessor = clzProcessor;
            }

        /**
         * Create a {@link Processor.Literal}.
         *
         * @param clzProcessor  the processor class
         *
         * @return a {@link Processor.Literal}
         *         with the specified value
         */
        public static Literal of(Class<? extends InvocableMap.EntryProcessor> clzProcessor)
            {
            return new Literal(clzProcessor);
            }

        /**
         * The processor class.
         *
         * @return the processor class
         */
        public Class<? extends InvocableMap.EntryProcessor> value()
            {
            return f_clzProcessor;
            }

        // ---- data members ------------------------------------------------

        /**
         * The processor class.
         */
        private final Class<? extends InvocableMap.EntryProcessor> f_clzProcessor;
        }
    }
