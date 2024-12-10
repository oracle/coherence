/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import com.oracle.coherence.cdi.AnnotationLiteral;
import com.tangosol.util.MapListener;

import javax.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation that can be applied to {@link MapListener} CDI
 * observers to register them as lite listeners.
 *
 * @author Aleks Seovic  2020.04.01
 * @since 20.06
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Lite
    {
    /**
     * An annotation literal for the {@link Lite}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<Lite>
            implements Lite
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
