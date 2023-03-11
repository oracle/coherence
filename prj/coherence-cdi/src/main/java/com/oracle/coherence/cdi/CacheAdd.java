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
 * Never get the value from cache, get it from method and cache the result.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheAdd
    {
    /**
     * An annotation literal for the {@link CacheAdd} annotation.
     */
    class Literal
            extends AnnotationLiteral<CacheAdd>
            implements CacheAdd
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
