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
 * Used by the {@link PortableTypeGenerator} to annotate field with the numeric
 * index that will be used for serialization.
 *
 * @author as  2013.04.23
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PofIndex
    {
    /**
     * Integer index of the field in a POF stream.
     *
     * @return  index of the field in a POF stream
     */
    int value();
    }
