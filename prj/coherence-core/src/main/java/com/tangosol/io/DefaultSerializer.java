/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;

import java.lang.ref.WeakReference;

import javax.inject.Named;

/**
 * A Serializer implementation that uses the ExternalizableHelper implementation
 * for serialization and deserialization of objects.
 *
 * @author cp/jh  2006.07.21 (SimpleSerializer)
 * @since Coherence 3.4
 */
@Named(DefaultSerializer.NAME)
public final class DefaultSerializer
        implements Serializer, ClassLoaderAware
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public DefaultSerializer()
        {
        }

    /**
     * Construct a DefaultSerializer that will use the passed ClassLoader.
     *
     * @param loader  the ClassLoader to use for deserialization
     */
    public DefaultSerializer(ClassLoader loader)
        {
        setContextClassLoader(loader);
        }


    // ----- Serializer interface  -------------------------------------------

    @Override
    public void serialize(WriteBuffer.BufferOutput out, Object o)
            throws IOException
        {
        try
            {
            ExternalizableHelper.writeObject(out, o);
            }
        catch (RuntimeException e)
            {
            // Guarantee that runtime exceptions from called methods are
            // IOException
            IOException ioex = new IOException(e.getMessage());

            ioex.initCause(e);
            throw ioex;
            }
        }

    @Override
    public Object deserialize(ReadBuffer.BufferInput in)
            throws IOException
        {
        try
            {
            return ExternalizableHelper.readObject(in, getContextClassLoader());
            }
        catch (RuntimeException e)
            {
            // Guarantee that runtime exceptions from called methods are
            // IOException
            IOException ioex = new IOException(e.getMessage());

            ioex.initCause(e);
            throw ioex;
            }
        }

    @Override
    public String getName()
        {
        return NAME;
        }

    // ----- ClassLoaderAware interface --------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        WeakReference<ClassLoader> refLoader = m_refLoader;
        return refLoader == null ? null : refLoader.get();
        }

    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        assert m_refLoader == null;
        m_refLoader = loader == null ? null : new WeakReference<>(loader);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Return a description of this DefaultSerializer.
     *
     * @return a String representation of the DefaultSerializer object
     */
    public String toString()
        {
        return getClass().getName() + " {loader=" + getContextClassLoader() + '}';
        }

    // ----- constants -------------------------------------------------------

    /**
     * The name of this serializer.
     */
    public static final String NAME = "java";

    // ----- data members ----------------------------------------------------

    /**
     * The optional ClassLoader.
     */
    private WeakReference<ClassLoader> m_refLoader;
    }
