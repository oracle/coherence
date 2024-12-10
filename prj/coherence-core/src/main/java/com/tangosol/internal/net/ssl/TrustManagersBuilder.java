/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import javax.net.ssl.TrustManager;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * A class that can build an array of {@link TrustManager} instances.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface TrustManagersBuilder
    {
    /**
     * Return the {@link TrustManager} array built from the specified dependencies.
     *
     * @param deps  the {@link ManagerDependencies} to use to build the {@link TrustManager} array
     * @param sb    the {@link StringBuilder} to append a description to
     *
     * @return an array of {@link TrustManager} instances, or {@link null} if the dependencies do
     *         not provide dependencies for any {@link TrustManager} instances
     *
     * @throws GeneralSecurityException if the {@link TrustManager} could not be created. For example, if no
     *                                  {@link java.security.Provider} supports a {@link java.security.KeyStoreSpi}
     *                                  implementation for the specified store type, or if the algorithm used to check
     *                                  the integrity of the keystore cannot be found, or if any of the certificates
     *                                  in the keystore could not be loaded.
     * @throws IOException              if there is an error loading the {@link KeyStore}, keys, certs or related data
     */
    TrustManager[] buildTrustManagers(ManagerDependencies deps, StringBuilder sb) throws GeneralSecurityException, IOException;

    /**
     * Return {@code true} if the KeyManager instances should be refreshed.
     *
     * @return {@code true} if the KeyManager instances should be refreshed
     */
    default boolean isRefreshable(ManagerDependencies deps)
        {
        return true;
        }

    /**
     * A no-op implementation of {@link TrustManagersBuilder} that always builds a null {@link TrustManager} array.
     */
    TrustManagersBuilder NullTrustManagersBuilder = (deps, sb) -> null;
    }
