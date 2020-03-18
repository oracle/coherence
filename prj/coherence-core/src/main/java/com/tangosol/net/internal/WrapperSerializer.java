/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.Serializer;

import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.util.Base;

import java.io.IOException;


/**
* Serializer implementation that wraps another Serializer.
* <p>
* Note: this class also extends ClassLoader so that it can be passed to
* methods that require a ClassLoader. This allows a Serializer to be
* "injected" without requiring a change to existing APIs. If the wrapped
* Serializer implements ClassLoaderAware, the WrapperSerializer delegates
* {@link ClassLoader#loadClass(String)} to the Serializer's context
* ClassLoader; otherwise, a ClassLoader appropriate for the calling context
* is used.
*
* @author jh  2008.03.25
*/
public class WrapperSerializer
        extends ClassLoader
        implements Serializer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new WrapperSerializer that delegates to the given Serializer.
    *
    * @param serializer  the wrapped Serializer; must not be null
    */
    public WrapperSerializer(Serializer serializer)
        {
        if (serializer == null)
            {
            throw new IllegalArgumentException();
            }
        m_serializer = serializer;
        }


    // ----- ClassLoader methods --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Class loadClass(String sName)
            throws ClassNotFoundException
        {
        return getClassLoader().loadClass(sName);
        }


    // ----- Serializer interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(BufferOutput out, Object o)
            throws IOException
        {
        getSerializer().serialize(out, o);
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(BufferInput in)
            throws IOException
        {
        return getSerializer().deserialize(in);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the delegate ClassLoader.
    * <p>
    * If the wrapped Serializer is an instance of ClassLoaderAware, the
    * Serializer's context ClassLoader is returned; otherwise, a ClassLoader
    * appropriate for the calling context is used.
    *
    * @return the delegate ClassLoader
    */
    public ClassLoader getClassLoader()
        {
        Serializer serializer = getSerializer();
        return Base.ensureClassLoader(
                serializer instanceof ClassLoaderAware
                    ? ((ClassLoaderAware) serializer).getContextClassLoader()
                    : null);
        }

    /**
    * Return the wrapped Serializer.
    *
    * @return the wrapped Serializer
    */
    public Serializer getSerializer()
        {
        return m_serializer;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped Serializer.
    */
    protected final Serializer m_serializer;
    }
