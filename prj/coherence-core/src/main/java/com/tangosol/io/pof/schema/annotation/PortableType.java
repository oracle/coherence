/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema.annotation;

import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PortableTypeSerializer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation that marks class as portable and optionally defines
 * type identifier, implementation version, and serializer for it.
 *
 * @author as  2013.04.23
 * @since  12.2.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PortableType
    {
    /**
     * Type identifier.
     *
     * @return type identifier
     */
    int id();

    /**
     * Implementation version.
     *
     * @return implementation version
     */
    int version() default 0;

    /**
     * The class of the serializer for this type.
     *
     * @return the class of the serializer for this type
     */
    Class<? extends PofSerializer> serializer() default PortableTypeSerializer.class;
    }
