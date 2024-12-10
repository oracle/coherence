/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * Defines an interface that must be implemented by all schema exporters.
 *
 * @param <TExternal>  the type of the external representation of the types
 *                     handled by this schema exporter
 * @param <PExternal>  the type of the external representation of the properties
 *                     handled by this schema exporter
 *
 * @author as  2013.11.21
 */
public interface SchemaExporter<TExternal, PExternal>
        extends TypeHandler<ExtensibleType, TExternal>, PropertyHandler<ExtensibleProperty, PExternal>
    {
    /**
     * Export specified schema.
     *
     * @param schema  the schema to export
     */
    void export(Schema schema);
    }
