/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import com.oracle.coherence.cdi.AnnotationLiteral;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation used for any STOPPED event.
 *
 * @author Jonathan Knight  2020.11.10
 * @since 20.12
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Stopped
    {
    /**
     * An annotation literal for the {@link Stopped}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<Stopped>
            implements Stopped
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
