/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cache.grpc.client;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * A qualifier annotation used to indicate that the resource bing injected
 * is remote.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Remote
    {
    // ----- inner class: Literal -------------------------------------------

    /**
     * An annotation literal for the {@link Remote} annotation.
     */
    class Literal
            extends AnnotationLiteral<Remote>
            implements Remote
        {
        public static final Remote.Literal INSTANCE = new Remote.Literal();
        }
    }
