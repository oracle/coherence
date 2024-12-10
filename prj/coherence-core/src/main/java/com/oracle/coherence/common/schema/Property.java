/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * An interface that each property implementation must support.
 *
 * @param <D>  the type of {@link TypeDescriptor} for this property
 *
 * @author as  2013.07.11
 */
public interface Property<D extends TypeDescriptor>
    {
    /**
     * Return the property name.
     *
     * @return the property name
     */
    String getName();

    /**
     * Return the property type.
     *
     * @return the property type
     */
    D getType();

    /**
     * Return the specified extension for this property.
     *
     * @param clzExtension  the type of the extension to return
     *
     * @return the specified extension
     */
    <T extends Property> T getExtension(Class<T> clzExtension);

    // ---- Visitor pattern  ------------------------------------------------

    /**
     * An acceptor for the {@link SchemaVisitor}.
     *
     * @param visitor  the visitor
     */
    void accept(SchemaVisitor visitor);
    }
