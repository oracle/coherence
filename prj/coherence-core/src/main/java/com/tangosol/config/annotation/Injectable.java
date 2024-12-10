/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that a Java Bean setter method may be injected with a value.
 *
 * @author bo  2011.06.15
 * @since Coherence 12.1.2
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Injectable
    {
    /**
     * The optional name that of which may be used to choose the
     * appropriate value to inject when multiple values of the same type
     * are available to be injected.
     *
     * @return  the name to be used for choosing an appropriate value to inject
     */
    String value() default "";
    }
