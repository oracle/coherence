/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;


/**
 * An abstract class that should be used as a base class for the custom
 * {@link TypeHandler} implementations.
 *
 * @param <TInternal>  the internal representation of type metadata
 * @param <TExternal>  the external representation of type metadata
 *
 * @author as  2013.11.20
 */
@SuppressWarnings("unchecked")
public abstract class AbstractTypeHandler<TInternal extends Type, TExternal>
        implements TypeHandler<TInternal, TExternal>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code AbstractTypeHandler} instance.
     */
    protected AbstractTypeHandler()
        {
        Class clazz = getClass();
        java.lang.reflect.Type superclass = clazz.getGenericSuperclass();

        java.lang.reflect.Type internalType = ((ParameterizedType) superclass).getActualTypeArguments()[0];

        while (!superclass.toString().startsWith("com.oracle.coherence.common.schema.AbstractTypeHandler"))
            {
            clazz = clazz.getSuperclass();
            superclass = clazz.getGenericSuperclass();
            }
        java.lang.reflect.Type externalType = ((ParameterizedType) superclass).getActualTypeArguments()[1];

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
     * Set the internal class this type handler is for, and ensure that it
     * implements a constructor that accepts {@link ExtensibleType} instance
     * as a sole argument.
     *
     * @param internalType  the internal class this type handler is for; must
     *                      have a constructor that accepts {@link ExtensibleType}
     *                      instance as a sole argument
     */
    protected void setInternalClass(Class<TInternal> internalType)
        {
        m_internalClass = internalType;
        try
            {
            m_ctorInternal = m_internalClass.getConstructor(ExtensibleType.class);
            }
        catch (NoSuchMethodException e)
            {
            throw new RuntimeException("Type class " + m_internalClass.getName() +
                " does not implement a constructor that accepts " + ExtensibleType.class.getName());
            }
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public Class<TInternal> getInternalTypeClass()
        {
        return m_internalClass;
        }

    @Override
    public Class<TExternal> getExternalTypeClass()
        {
        return m_externalClass;
        }

    @Override
    public TInternal createType(ExtensibleType parent)
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
     * No-op implementation, which should be overridden by the concrete type
     * handler implementations responsible for importing type information from
     * an external source.
     *
     * @inheritDoc
     */
    @Override
    public void importType(TInternal type, TExternal source, Schema schema)
        {
        // no-op implementation
        }

    /**
     * No-op implementation, which should be overridden by the concrete type
     * handler implementations responsible for exporting types from a schema.
     *
     * @inheritDoc
     */
    @Override
    public void exportType(TInternal type, TExternal target, Schema schema)
        {
        }

    // ---- data members ----------------------------------------------------

    private Class<TInternal> m_internalClass;
    private Class<TExternal> m_externalClass;

    private Constructor<TInternal> m_ctorInternal;
    }
