/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.cpp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * An annotation that can be applied to Java fields to specify the C++ type
 * that should be used to represent that property.
 *
 * @author as  2013.11.21
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CppProperty
    {
    /**
     * The fully qualified name of the C++ type that should be used for
     * this property.
     *
     * @return the name of the C++ property type
     */
    String type() default "";
    }
