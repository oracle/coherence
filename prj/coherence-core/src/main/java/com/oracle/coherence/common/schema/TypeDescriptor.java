/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.util.List;


/**
 * An interface that all type descriptors must implement.
 *
 * @author as  2013.08.26
 */
public interface TypeDescriptor<T extends TypeDescriptor>
    {
    /**
     * Return the type namespace.
     *
     * @return the type namespace
     */
    String getNamespace();

    /**
     * Return the type name.
     *
     * @return the type name
     */
    String getName();

    /**
     * Return the fully qualified type name.
     *
     * @return the fully qualified type name
     */
    String getFullName();

    /**
     * Return {@code true} if this type descriptor represents an array type.
     *
     * @return {@code true} if this type descriptor represents an array type,
     *         {@code false} otherwise
     */
    boolean isArray();

    /**
     * Return {@code true} if this type descriptor represents a generic type.
     *
     * @return {@code true} if this type descriptor represents a generic type,
     *         {@code false} otherwise
     */
    boolean isGenericType();

    /**
     * Return a list of type descriptors for the generic arguments.
     *
     * @return a list of type descriptors for the generic arguments
     */
    List<T> getGenericArguments();
    }
