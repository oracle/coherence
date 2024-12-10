/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema.annotation.internal;

import com.tangosol.io.pof.generator.PortableTypeGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For internal use only.
 * 
 * Marks portable class as instrumented after it has been processed by the
 * {@link PortableTypeGenerator}.
 *
 * @author as  2013.04.23
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrumented
    {
    }
