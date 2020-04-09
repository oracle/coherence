/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.util.Collection;


/**
 * An abstract class that should be used as a base class for the custom
 * {@link SchemaSource} implementations.
 *
 * @param <TExternal>  the type of the external representation of the types
 *                     handled by this schema source
 * @param <PExternal>  the type of the external representation of the properties
 *                     handled by this schema source
 *
 * @author as  2013.07.11
 */
public abstract class AbstractSchemaSource<TExternal, PExternal>
        implements SchemaSource<TExternal, PExternal>
    {
    // ---- abstract methods ------------------------------------------------

    /**
     * Return the property name from the specified external representation.
     *
     * @param source  the external representation of a property
     *
     * @return the property name from the specified external representation
     */
    protected abstract String getPropertyName(PExternal source);

    /**
     * Return a collection of available properties from the specified external
     * type representation.
     *
     * @param source  the external representation of a type
     *
     * @return a collection of available properties from the specified external
     *         type representation
     */
    protected abstract Collection<PExternal> getProperties(TExternal source);

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public Class<ExtensibleType> getInternalTypeClass()
        {
        return ExtensibleType.class;
        }

    @Override
    public ExtensibleType createType(ExtensibleType parent)
        {
        return new ExtensibleType();
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public Class<ExtensibleProperty> getInternalPropertyClass()
        {
        return ExtensibleProperty.class;
        }

    @Override
    public ExtensibleProperty createProperty(ExtensibleProperty parent)
        {
        return new ExtensibleProperty();
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Helper method that can be used to correctly populate an {@link
     * ExtensibleType} instance based on registered type handler and external
     * source of type metadata.
     *
     * @param schema  the schema instance that is being populated
     * @param type    the {@code ExtensibleType} instance to populate
     * @param source  the external source of type metadata
     *
     * @return the populated {@code ExtensibleType} instance
     */
    protected ExtensibleType populateTypeInternal(Schema schema, ExtensibleType type, TExternal source)
        {
        if (type == null)
            {
            type = createType(null);
            }
        importType(type, source, schema);

        for (TypeHandler handler : schema.getTypeHandlers(getExternalTypeClass()))
            {
            Type ext = type.getExtension(handler.getInternalTypeClass());
            if (ext == null)
                {
                ext = handler.createType(type);
                type.addExtension(ext);
                }
            handler.importType(ext, source, schema);
            }

        for (PExternal propertySource : getProperties(source))
            {
            ExtensibleProperty property = type.getProperty(getPropertyName(propertySource));
            if (property == null)
                {
                property = createProperty(null);
                }
            importProperty(property, propertySource, schema);

            for (PropertyHandler handler : schema.getPropertyHandlers(
                    getExternalPropertyClass()))
                {
                Property ext = property.getExtension(handler.getInternalPropertyClass());
                if (ext == null)
                    {
                    ext = handler.createProperty(property);
                    property.addExtension(ext);
                    }
                handler.importProperty(ext, propertySource, schema);
                }
            type.addProperty(property);
            }

        return type;
        }
    }
