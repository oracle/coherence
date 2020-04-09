/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * Defines an interface that a visitor passed to {@link Schema#accept(SchemaVisitor)}
 * method has to implement.
 *
 * @author as  2013.07.25
 */
public interface SchemaVisitor
    {
    /**
     * A method that will be called once for each {@link ExtensibleType} within
     * the {@link Schema} this visitor is passed to.
     *
     * @param type  the {@code ExtensibleType} to visit
     */
    void visitType(ExtensibleType type);

    /**
     * A method that will be called once for each type extension added to an
     * {@link ExtensibleType}.
     *
     * @param type  the type extension to visit
     */
    void visitType(Type type);

    /**
     * A method that will be called once for each property of an {@link ExtensibleType}.
     *
     * @param property  the {@code ExtensibleProperty} to visit
     */
    void visitProperty(ExtensibleProperty property);

    /**
     * A method that will be called once for each property extension added to an
     * {@link ExtensibleProperty}.
     *
     * @param property  the property extension to visit
     */
    void visitProperty(Property property);
    }
