/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * Defines an interface that must be implemented by all schema sources.
 *
 * @param <TExternal>  the type of the external representation of the types
 *                     handled by this schema source
 * @param <PExternal>  the type of the external representation of the properties
 *                     handled by this schema source
 *
 * @author as  2013.06.26
 */
public interface SchemaSource<TExternal, PExternal>
        extends TypeHandler<ExtensibleType, TExternal>, PropertyHandler<ExtensibleProperty, PExternal>
    {
    /**
     * Populate specified {@link Schema} with the types discoverable by this
     * schema source.
     *
     * @param schema  the schema to populate
     */
    void populateSchema(Schema schema);
    }
