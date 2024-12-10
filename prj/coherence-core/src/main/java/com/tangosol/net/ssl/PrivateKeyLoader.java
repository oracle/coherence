/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.tangosol.net.PasswordProvider;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;

/**
 * A class that can create a {@link PrivateKey}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface PrivateKeyLoader
    {
    /**
     * Load a named {@link PrivateKey}.
     *
     * @param password  an optional {@link PasswordProvider} for encrypted keys
     *
     * @return the {@link PrivateKey} or {@code null} if no key could be loaded
     */
    PrivateKey load(PasswordProvider password)
            throws GeneralSecurityException, IOException;

    /**
     * Return {@code true} if this {@link PrivateKeyLoader} is enabled,
     * or {@code false} if {@link PrivateKeyLoader} should not be used.
     *
     * @return {@code true} if this {@link PrivateKeyLoader} is enabled,
     *         or {@code false} if {@link PrivateKeyLoader} should not
     *         be used
     */
    default boolean isEnabled()
        {
        return true;
        }

    /**
     * Return {@code true} if the {@link PrivateKey} loaded previously
     * by this loader should be refreshed.
     *
     * @return the default implementation always returns {@code true}
     */
    default boolean isRefreshable()
        {
        return true;
        }
    }
