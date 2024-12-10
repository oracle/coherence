/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * An abstract base class for type extension implementations.
 *
 * @author as  2013.07.08
 */
@SuppressWarnings("unchecked")
public abstract class AbstractType<P extends Property, TD extends TypeDescriptor>
        implements Type<P, TD>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code AbstractType} instance.
     *
     * @param parent  the parent {@code ExtensibleType} instance
     */
    protected AbstractType(ExtensibleType parent)
        {
        m_parent = parent;

        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        java.lang.reflect.Type[] actualTypeArguments = ((ParameterizedType) superclass).getActualTypeArguments();
        this.m_propertyClass = (Class<P>) actualTypeArguments[0];
        this.m_typeDescriptorClass = actualTypeArguments.length > 1
                                     ? (Class<TD>) actualTypeArguments[1]
                                     : (Class<TD>) CanonicalTypeDescriptor.class;
        }

    // ---- Type implementation ---------------------------------------------

    @Override
    public String getNamespace()
        {
        if (getDescriptor() == null)
            {
            return null;
            }
        return getDescriptor().getNamespace();
        }

    @Override
    public String getName()
        {
        if (getDescriptor() == null)
            {
            return null;
            }
        return getDescriptor().getName();
        }

    @Override
    public String getFullName()
        {
        if (getDescriptor() == null)
            {
            return null;
            }
        return getDescriptor().getFullName();
        }

    @Override
    public <T extends Type> T getExtension(Class<T> clzExtension)
        {
        return m_parent.getExtension(clzExtension);
        }

    @Override
    public void accept(SchemaVisitor visitor)
        {
        visitor.visitType(this);
        }

    @Override
    public P getProperty(String propertyName)
        {
        return m_parent.getProperty(propertyName).getExtension(m_propertyClass);
        }

    @Override
    public List<P> getProperties()
        {
        Collection<ExtensibleProperty> properties = m_parent.getProperties();
        List<P> results = new ArrayList<>(properties.size());
        for (ExtensibleProperty p : properties)
            {
            P property = p.getExtension(m_propertyClass);
            if (property != null)
                {
                results.add(property);
                }
            }
        return results;
        }

    // ---- public API ------------------------------------------------------

    /**
     * Return the parent {@code ExtensibleType} instance.
     *
     * @return the parent {@code ExtensibleType} instance
     */
    public ExtensibleType getParent()
        {
        return m_parent;
        }

    // ---- Object methods --------------------------------------------------

    public String toString()
        {
        return getClass().getSimpleName() + "{" +
               "name=" + getName() +
               ", desc=" + getDescriptor() +
               '}';
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Return the property class.
     *
     * @return the property class
     */
    protected Class<P> getPropertyClass()
        {
        return m_propertyClass;
        }

    /**
     * Return the type descriptor class.
     *
     * @return the type descriptor class
     */
    protected Class<TD> getTypeDescriptorClass()
        {
        return m_typeDescriptorClass;
        }

    // ---- data members ----------------------------------------------------

    private ExtensibleType m_parent;
    private Class<P> m_propertyClass;
    private Class<TD> m_typeDescriptorClass;
    }
