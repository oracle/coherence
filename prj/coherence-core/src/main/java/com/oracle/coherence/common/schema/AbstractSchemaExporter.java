/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


/**
 * An abstract base class for {@link SchemaExporter} implementations.
 *
 * @author as  2013.12.02
 */
public abstract class AbstractSchemaExporter<TExternal, PExternal>
        implements SchemaExporter<TExternal, PExternal>
    {
    // ---- SchemaExporter implementation -----------------------------------

    @Override
    public Class<ExtensibleType> getInternalTypeClass()
        {
        return ExtensibleType.class;
        }

    @Override
    public void importType(ExtensibleType type, TExternal source, Schema schema)
        {
        }

    @Override
    public Class<ExtensibleProperty> getInternalPropertyClass()
        {
        return ExtensibleProperty.class;
        }

    @Override
    public void importProperty(ExtensibleProperty property, PExternal source, Schema schema)
        {
        }

    @Override
    public ExtensibleType createType(ExtensibleType parent)
        {
        return new ExtensibleType();
        }

    @Override
    public ExtensibleProperty createProperty(ExtensibleProperty parent)
        {
        return new ExtensibleProperty();
        }
    }
