/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.options;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import java.util.Objects;

/**
 * A {@link Session.Option} to set the name to use for a {@link Session}.
 *
 * @author Jonathan Knight  2020.11.04
 * @since 20.12
 */
public interface WithName
        extends Session.Option
    {
    // ----- WithName methods -----------------------------------------------

    /**
     * Returns the name to use for the {@link Session}.
     *
     * @return  the name to use for the {@link Session}
     */
    String getName();

    // ----- helper methods -------------------------------------------------

    /**
     * Returns a {@link WithName} option to set the specified Session name.
     *
     * @param sName  the name to use
     *
     * @return a {@link WithName} option to set the specified name
     * @throws NullPointerException if the name is null
     */
    static WithName of(String sName)
        {
        Objects.requireNonNull(sName);
        return () -> sName;
        }

    /**
     * Returns a {@link WithName} option using the default name.
     *
     * @return a {@link WithName} option using the default name
     * @see Coherence#DEFAULT_NAME
     */
    static WithName defaultName()
        {
        return () -> Coherence.DEFAULT_NAME;
        }

    /**
     * Returns a {@link WithName} option with a {@code null} name.
     *
     * @return a {@link WithName} option a {@code null} name
     */
    static WithName none()
        {
        return () -> null;
        }
    }
