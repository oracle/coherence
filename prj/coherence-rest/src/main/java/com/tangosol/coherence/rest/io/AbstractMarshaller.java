/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

/**
 * Abstract base class for built-in marshallers.
 *
 * @author as  2011.07.10
 */
public abstract class AbstractMarshaller<T>
        implements Marshaller<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a marshaller instance.
     *
     * @param clzRoot  class of the root object this marshaller is for
     */
    public AbstractMarshaller(Class<T> clzRoot)
        {
        if (clzRoot == null)
            {
            throw new IllegalArgumentException("null root class");
            }
        m_clzRoot = clzRoot;
        }

    // ---- methods ---------------------------------------------------------

    /**
     * Return class of the root object this marshaller is for.
     *
     * @return root object class
     */
    protected Class<T> getRootClass()
        {
        return m_clzRoot;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Class of the root object this marshaller is for.
     */
    private final Class<T> m_clzRoot;
    }
