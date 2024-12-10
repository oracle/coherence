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
 * An annotation that can be applied to Java classes to specify the name of the
 * corresponding C++ type.
 *
 * @author as  2013.11.21
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CppType
    {
    /**
     * The fully qualified name of the C++ type this type should map to.
     *
     * @return the name of the C++ type
     */
    String name() default "";
    }
