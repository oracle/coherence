/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.event;

import com.oracle.coherence.inject.AnnotationLiteral;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation used for any ACTIVATING event.
 *
 * @author Aleks Seovic  2020.04.01
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Activating
    {
    /**
     * An annotation literal for the {@link Activating}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<Activating>
            implements Activating
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
