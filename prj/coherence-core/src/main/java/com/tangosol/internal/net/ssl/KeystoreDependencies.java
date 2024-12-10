/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.net.PasswordProvider;

import com.tangosol.net.ssl.KeyStoreLoader;

import java.security.KeyStore;

/**
 * The dependencies for a keystore.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface KeystoreDependencies
    {
    /**
     * Returns keystore type, defaults to JKS.
     *
     * @return get key-store type
     */
    default String getType()
        {
        return SSLSocketProvider.Dependencies.DEFAULT_KEYSTORE_TYPE;
        }

    /**
     * Returns the optional {@link KeyStoreLoader} to use to load the {@link KeyStore}.
     *
     * @return the optional {@link KeyStoreLoader} to use to load the {@link KeyStore}
     */
    KeyStoreLoader getKeyStoreLoader();

    /**
     * Get the {@link PasswordProvider} for the keystore.
     *
     * @return the {@link PasswordProvider} for the keystore
     */
    PasswordProvider getPasswordProvider();

    /**
     * A default null {@link KeystoreDependencies}.
     */
    KeystoreDependencies NullImplementation = new KeystoreDependencies()
        {
        @Override
        public KeyStoreLoader getKeyStoreLoader()
            {
            return null;
            }

        @Override
        public PasswordProvider getPasswordProvider()
            {
            return PasswordProvider.NullImplementation;
            }
        };
    }
