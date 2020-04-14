/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * The {@code ExtensibleProperty} implementation.
 *
 * @author as  2013.06.03
 */
public class ExtensibleProperty
        implements Property<CanonicalTypeDescriptor>
    {
    // ---- Property implementation -----------------------------------------

    @Override
    public String getName()
        {
        return m_name;
        }

    @Override
    public CanonicalTypeDescriptor getType()
        {
        return m_type;
        }

    @Override
    public <T extends Property> T getExtension(Class<T> clzExtension)
        {
        return (T) m_extensions.get(clzExtension.getName());
        }

    @Override
    public void accept(SchemaVisitor visitor)
        {
        visitor.visitProperty(this);
        for (Property ext : getExtensions())
            {
            ext.accept(visitor);
            }
        }

    // ---- public API ------------------------------------------------------

    /**
     * Set the property name.
     *
     * @param name  the property name
     */
    public void setName(String name)
        {
        m_name = name;
        }

    /**
     * Set the property type.
     *
     * @param type  the property type
     */
    public void setType(CanonicalTypeDescriptor type)
        {
        m_type = type;
        }

    /**
     * Add the specified extension to this property.
     *
     * @param extension  the extension to add
     */
    public <T extends Property> void addExtension(T extension)
        {
        m_extensions.put(extension.getClass().getName(), extension);
        }

    /**
     * Return all the extensions this property has.
     *
     * @return all the extensions this property has
     */
    public Collection<Property> getExtensions()
        {
        return Collections.unmodifiableCollection(m_extensions.values());
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return "ExtensibleProperty{" +
               "name='" + m_name + '\'' +
               ", type=" + m_type +
               '}';
        }

    // ---- data members ----------------------------------------------------

    private String m_name;
    private CanonicalTypeDescriptor m_type;

    private Map<String, Property> m_extensions = new HashMap<>();
    }
