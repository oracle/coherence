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
 * A qualifier annotation used for any LOST event.
 *
 * @author Aleks Seovic  2020.04.01
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Lost
    {
    /**
     * An annotation literal for the {@link com.oracle.coherence.cdi.events.Lost}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<Lost>
            implements Lost
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
