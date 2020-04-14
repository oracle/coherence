/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.lang.reflect.ParameterizedType;


/**
 * An abstract base class for property extension implementations.
 *
 * @author as  2013.07.08
 */
@SuppressWarnings("unchecked")
public abstract class AbstractProperty<TD extends TypeDescriptor>
        implements Property<TD>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code AbstractProperty} instance.
     *
     * @param parent  the parent {@code ExtensibleProperty} instance
     */
    protected AbstractProperty(ExtensibleProperty parent)
        {
        m_parent = parent;

        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        try
            {
            this.m_typeDescriptorClass = (Class<TD>)
                    ((ParameterizedType) superclass).getActualTypeArguments()[0];
            }
        catch (Exception e)
            {
            this.m_typeDescriptorClass = (Class<TD>) CanonicalTypeDescriptor.class;
            }
        }

    // ---- Property implementation -----------------------------------------

    @Override
    public String getName()
        {
        return m_parent.getName();
        }

    @Override
    public <T extends Property> T getExtension(Class<T> extensionType)
        {
        return m_parent.getExtension(extensionType);
        }

    @Override
    public void accept(SchemaVisitor visitor)
        {
        visitor.visitProperty(this);
        }

    // ---- public API ------------------------------------------------------

    /**
     * Return the parent {@code ExtensibleProperty} instance.
     *
     * @return the parent {@code ExtensibleProperty} instance
     */
    public ExtensibleProperty getParent()
        {
        return m_parent;
        }

    // ---- Object methods --------------------------------------------------

    public String toString()
        {
        return getClass().getSimpleName() + "{" +
               "name=" + getName() +
               ", type=" + getType() +
               '}';
        }

    // ---- helper methods --------------------------------------------------

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

    private ExtensibleProperty m_parent;
    private Class<TD> m_typeDescriptorClass;
    }
