/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.util.Collection;


/**
 * An interface that each type implementation must support.
 *
 * @param <P>  the type of {@link Property}s for this type
 * @param <D>  the type of {@link TypeDescriptor} for this type
 *
 * @author as  2013.07.03
 */
public interface Type<P extends Property, D extends TypeDescriptor>
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
     * Return the descriptor for this type.
     *
     * @return the descriptor for this type
     */
    D getDescriptor();

    /**
     * Return the property with a specified name.
     *
     * @param propertyName  the name of the property to return
     *
     * @return the property with a specified name
     */
    P getProperty(String propertyName);

    /**
     * Return a collection of all properties within this type.
     *
     * @return a collection of all properties within this type
     */
    Collection<P> getProperties();

    /**
     * Return the specified extension for this type.
     *
     * @param extensionType  the type of the extension to return
     *
     * @return the specified extension
     */
    <T extends Type> T getExtension(Class<T> extensionType);

    // ---- Visitor pattern  ------------------------------------------------

    /**
     * An acceptor for the {@link SchemaVisitor}.
     *
     * @param visitor  the visitor
     */
    void accept(SchemaVisitor visitor);
    }
