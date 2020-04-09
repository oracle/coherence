/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


/**
 * Abstract base class for {@link PropertyHandler} implementations.
 *
 * @param <TInternal>  the internal representation of property metadata
 * @param <TExternal>  the external representation of property metadata
 *
 * @author as  2013.11.20
 */
@SuppressWarnings("unchecked")
public abstract class AbstractPropertyHandler<TInternal extends Property, TExternal>
        implements PropertyHandler<TInternal, TExternal>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code AbstractPropertyHandler} instance.
     */
    protected AbstractPropertyHandler()
        {
        Class clazz = getClass();
        Type superclass = clazz.getGenericSuperclass();

        Type internalType = ((ParameterizedType) superclass).getActualTypeArguments()[0];

        while (!superclass.toString().startsWith("com.oracle.coherence.common.schema.AbstractPropertyHandler"))
            {
            clazz = clazz.getSuperclass();
            superclass = clazz.getGenericSuperclass();
            }
        Type externalType = ((ParameterizedType) superclass).getActualTypeArguments()[1];

        if (internalType instanceof Class)
            {
            setInternalClass((Class<TInternal>) internalType);
            }

        if (externalType instanceof Class)
            {
            m_externalClass = (Class<TExternal>) externalType;
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Set the internal class this property handler is for, and ensure that it
     * implements a constructor that accepts {@link ExtensibleProperty} instance
     * as a sole argument.
     *
     * @param internalType  the internal class this type handler is for; must
     *                      have a constructor that accepts {@link ExtensibleProperty}
     *                      instance as a sole argument
     */
    protected void setInternalClass(Class<TInternal> internalType)
        {
        m_internalClass = internalType;
        try
            {
            m_ctorInternal = m_internalClass.getConstructor(ExtensibleProperty.class);
            }
        catch (NoSuchMethodException e)
            {
            throw new RuntimeException("Property class " + m_internalClass.getName() +
                " does not implement a constructor that accepts " + ExtensibleProperty.class.getName());
            }
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public Class<TInternal> getInternalPropertyClass()
        {
        return m_internalClass;
        }

    @Override
    public Class<TExternal> getExternalPropertyClass()
        {
        return m_externalClass;
        }

    @Override
    public TInternal createProperty(ExtensibleProperty parent)
        {
        try
            {
            return m_ctorInternal.newInstance(parent);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * No-op implementation, which should be overridden by the concrete property
     * handler implementations responsible for importing property information from
     * an external source.
     *
     * @inheritDoc
     */
    @Override
    public void importProperty(TInternal property, TExternal source, Schema schema)
        {
        }

    /**
     * No-op implementation, which should be overridden by the concrete property
     * handler implementations responsible for exporting properties from a schema.
     *
     * @inheritDoc
     */
    @Override
    public void exportProperty(TInternal property, TExternal target, Schema schema)
        {
        }

    // ---- data members ----------------------------------------------------

    private Class<TInternal> m_internalClass;
    private Class<TExternal> m_externalClass;

    private Constructor<TInternal> m_ctorInternal;
    }
