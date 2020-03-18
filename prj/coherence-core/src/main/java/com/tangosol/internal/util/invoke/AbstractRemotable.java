/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;


import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for remotable classes.
 *
 * @author as  2015.08.28
 */
public abstract class AbstractRemotable<T>
        implements Remotable<T>
    {
    /**
     * Construct AbstractRemotable instance.
     *
     * @param aCtorArgs  remote constructor arguments
     */
    public AbstractRemotable(Object... aCtorArgs)
        {
        m_aCtorArgs = aCtorArgs;
        }

    // ---- Remotable interface -----------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public RemoteConstructor<T> getRemoteConstructor()
        {
        RemoteConstructor<T> constructor = m_constructor;
        if (constructor == null)
            {
            constructor = m_constructor =
                    RemotableSupport.get(getClass().getClassLoader())
                            .createRemoteConstructor((Class<? extends T>) getClass(), m_aCtorArgs);
            }
        return constructor;
        }

    @Override
    public void setRemoteConstructor(RemoteConstructor<T> constructor)
        {
        m_constructor = constructor;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Remote constructor to use.
     */
    @JsonbProperty("remoteConstructor")
    private RemoteConstructor<T> m_constructor;

    /**
     * An array of remote constructor arguments.
     */
    @JsonbProperty("remoteConstructor")
    private Object[] m_aCtorArgs;
    }
