/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * The {@code ExtensibleType} implementation.
 *
 * @author as  2013.06.03
 */
@SuppressWarnings("unchecked")
public class ExtensibleType
        implements Type<ExtensibleProperty, CanonicalTypeDescriptor>
    {
    // ---- Type implementation ---------------------------------------------

    @Override
    public String getNamespace()
        {
        return m_descriptor.getNamespace();
        }

    @Override
    public String getName()
        {
        return m_descriptor.getName();
        }

    @Override
    public String getFullName()
        {
        return m_descriptor.getFullName();
        }

    @Override
    public CanonicalTypeDescriptor getDescriptor()
        {
        return m_descriptor;
        }

    @Override
    public ExtensibleProperty getProperty(String propertyName)
        {
        return m_properties.get(propertyName);
        }

    @Override
    public Collection<ExtensibleProperty> getProperties()
        {
        return Collections.unmodifiableCollection(m_properties.values());
        }

    @Override
    public <T extends Type> T getExtension(Class<T> clzExtension)
        {
        return (T) m_extensions.get(clzExtension.getName());
        }

    @Override
    public void accept(SchemaVisitor visitor)
        {
        visitor.visitType(this);
        for (Type ext : getExtensions())
            {
            ext.accept(visitor);
            }
        for (ExtensibleProperty property : getProperties())
            {
            property.accept(visitor);
            }
        }

    // ---- public API ------------------------------------------------------

    /**
     * Set a descriptor for this type.
     *
     * @param descriptor  a descriptor to set
     */
    public void setDescriptor(CanonicalTypeDescriptor descriptor)
        {
        m_descriptor = descriptor;
        }

    /**
     * Return the base type (supertype) for this type.
     *
     * @return the base type (supertype) for this type
     */
    public CanonicalTypeDescriptor getBase()
        {
        return m_base;
        }

    /**
     * Set the base type (supertype) for this type.
     *
     * @param base  the base type (supertype) for this type
     */
    public void setBase(CanonicalTypeDescriptor base)
        {
        m_base = base;
        }

    public boolean isExternal()
        {
        return m_fExternal;
        }

    public void setExternal(boolean fExternal)
        {
        m_fExternal = fExternal;
        }

    /**
     * Return a set of interfaces implemented by this type.
     *
     * @return a set of interfaces implemented by this type
     */
    public Set<CanonicalTypeDescriptor> getInterfaces()
        {
        return Collections.unmodifiableSet(m_interfaces);
        }

    /**
     * Add the specified interface to this type.
     *
     * @param descriptor  the interface to add
     */
    public void addInterface(CanonicalTypeDescriptor descriptor)
        {
        m_interfaces.add(descriptor);
        }

    /**
     * Add the specified property to this type.
     *
     * @param property  the property to add
     */
    public void addProperty(ExtensibleProperty property)
        {
        m_properties.put(property.getName(), property);
        
        for (Type<?, ?> ext : m_extensions.values())
            {
            if (ext instanceof PropertyAware)
                {
                ((PropertyAware) ext).propertyAdded(property);
                }
            }
        }

    /**
     * Add the specified extension to this type.
     *
     * @param extension  the extension to add
     */
    public <T extends Type> void addExtension(T extension)
        {
        m_extensions.put(extension.getClass().getName(), extension);
        }

    /**
     * Return all the extensions this type has.
     *
     * @return all the extensions this type has
     */
    public Collection<Type> getExtensions()
        {
        return Collections.unmodifiableCollection(m_extensions.values());
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return "ExtensibleType{" +
               "descriptor=" + m_descriptor +
               ", base=" + m_base +
               ", external=" + m_fExternal +
               ", interfaces=" + m_interfaces +
               '}';
        }

    // ---- data members ----------------------------------------------------

    private CanonicalTypeDescriptor m_descriptor;
    private CanonicalTypeDescriptor m_base;
    private boolean m_fExternal;
    private Set<CanonicalTypeDescriptor> m_interfaces = new LinkedHashSet<>();

    private Map<String, ExtensibleProperty> m_properties = new LinkedHashMap<>();
    private Map<String, Type> m_extensions = new HashMap<>();
    }
