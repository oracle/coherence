/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put a value to cache AND call the method.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut
    {
    /**
     * An annotation literal for the {@link CachePut} annotation.
     */
    class Literal
            extends AnnotationLiteral<CachePut>
            implements CachePut
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
