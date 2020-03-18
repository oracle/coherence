/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.options;

import com.oracle.coherence.common.util.Options;

import com.tangosol.net.Session;
import com.tangosol.util.Base;

/**
 * An {@link com.tangosol.net.Session.Option} specifying how to resolve
 * the {@link ClassLoader} to use when creating a {@link Session}
 * with a {@link com.tangosol.net.SessionProvider}.
 *
 * @see Session
 * @see com.tangosol.net.SessionProvider
 *
 * @author bo  2015.07.27
 */
public interface WithClassLoader extends Session.Option
    {

    // ----- WithClassLoader methods ----------------------------------------

    /**
     * Acquire the {@link ClassLoader} to use.
     *
     * @return  the {@link ClassLoader}
     */
    ClassLoader getClassLoader();

    // ----- helper methods -------------------------------------------------

    /**
     * Obtains a {@link WithClassLoader} that auto-detects
     * the {@link ClassLoader} based on the calling context.
     *
     * @return a {@link WithClassLoader}
     */
    @Options.Default
    static WithClassLoader autoDetect()
        {
        return () -> Base.ensureClassLoader(null);
        }

    /**
     * Obtains a {@link WithClassLoader} for the specified not-null {@link ClassLoader}.
     *
     * @param loader  the {@link ClassLoader}
     *
     * @return a {@link WithClassLoader}
     */
    static WithClassLoader using(ClassLoader loader)
        {
        Base.azzert(loader != null, "ClassLoader can't be null");

        return () -> loader;
        }
    }
