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
 * Get the value from cache if present, invoke the method and cache the result
 * otherwise.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheGet
    {
    /**
     * An annotation literal for the {@link CacheGet} annotation.
     */
    class Literal
            extends AnnotationLiteral<CacheGet>
            implements CacheGet
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
