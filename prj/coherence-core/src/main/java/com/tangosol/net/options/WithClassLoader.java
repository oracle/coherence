/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.options;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.util.Options;

import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;

/**
 * An option specifying how to resolve the {@link ClassLoader} to use when
 * creating a {@link Session}, {@link NamedMap}, {@link com.tangosol.net.NamedCache},
 * or {@link com.tangosol.net.topic.NamedTopic}.
 *
 * @author bo  2015.07.27
 */
public interface WithClassLoader
        extends Session.Option, NamedMap.Option, NamedCollection.Option
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
        return () -> Classes.ensureClassLoader(null);
        }

    /**
     * Obtains a {@link WithClassLoader} that returns the
     * {@link NullImplementation} class loader.
     *
     * @return a {@link WithClassLoader} that returns the
     *         {@link NullImplementation} class loader
     */
    static WithClassLoader nullImplementation()
        {
        return NullImplementation::getClassLoader;
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
