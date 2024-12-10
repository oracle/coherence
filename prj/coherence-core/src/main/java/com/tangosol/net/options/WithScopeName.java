/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.options;

import com.oracle.coherence.common.util.Options;

import com.tangosol.net.Coherence;

import com.tangosol.net.Session;

import java.util.Objects;

/**
 * A {@link Session.Option} to set the scope name to use for a {@link Session}.
 *
 * @author Jonathan Knight  2020.11.04
 * @since 20.12
 */
public interface WithScopeName
        extends Session.Option
    {
    // ----- WithScope methods ----------------------------------------------

    /**
     * Returns the scope name to use for the {@link Session}.
     *
     * @return  the scope name to use for the {@link Session}
     */
    String getScopeName();

    // ----- helper methods -------------------------------------------------

    /**
     * Returns a {@link WithScopeName} option to set the specified scope name.
     *
     * @param sScope  the scope name to use
     *
     * @return a {@link WithScopeName} option to set the specified scope name
     * @throws NullPointerException if the scope name is null
     */
    static WithScopeName of(String sScope)
        {
        Objects.requireNonNull(sScope);
        return () -> sScope;
        }

    /**
     * Returns a {@link WithScopeName} option using the default scope name.
     *
     * @return a {@link WithScopeName} option using the default scope name
     * @see Coherence#DEFAULT_SCOPE
     */
    static WithScopeName defaultScope()
        {
        return () -> Coherence.DEFAULT_SCOPE;
        }

    /**
     * Returns a {@link WithScopeName} option using no scope name.
     *
     * @return a {@link WithScopeName} option using no scope name
     */
    @Options.Default
    static WithScopeName none()
        {
        return () -> null;
        }
    }
