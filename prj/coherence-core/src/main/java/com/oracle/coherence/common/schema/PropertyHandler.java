/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


/**
 * An interface that all property handlers must implement.
 *
 * @param <TInternal>  the internal representation of property metadata
 * @param <TExternal>  the external representation of property metadata
 *
 * @author as  2013.06.26
 */
public interface PropertyHandler<TInternal extends Property, TExternal>
    {
    /**
     * Return the class of the internal property representation this property
     * handler understands.
     *
     * @return the class of the internal property representation this property
     *         handler understands
     */
    Class<TInternal> getInternalPropertyClass();

    /**
     * Return the class of the external property representation this property
     * handler understands.
     *
     * @return the class of the external property representation this property
     *         handler understands
     */
    Class<TExternal> getExternalPropertyClass();

    /**
     * Create the internal property representation and associate it with the
     * specified extensible property.
     *
     * @param parent  the extensible property created property should be
     *                associated with
     *
     * @return the internal property representation
     */
    TInternal createProperty(ExtensibleProperty parent);

    /**
     * Import specified property into the schema.
     *
     * @param property  the internal property to populate and import
     * @param source    the external source to read property metadata from
     * @param schema    the schema to import property into
     */
    void importProperty(TInternal property, TExternal source, Schema schema);

    /**
     * Export specified property from the schema.
     *
     * @param property  the internal property to export
     * @param target    the external target to populate based on the internal
     *                  property metadata
     * @param schema    the schema to export property from
     */
    void exportProperty(TInternal property, TExternal target, Schema schema);
    }
