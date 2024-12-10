/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder.ProviderBuilder;
import com.tangosol.net.PasswordProvider;
import com.tangosol.net.ssl.CertificateLoader;
import com.tangosol.net.ssl.KeyStoreLoader;
import com.tangosol.net.ssl.PrivateKeyLoader;

import java.security.cert.Certificate;
import java.util.List;

/**
 * The trust-manager or identity-manager configuration.
 * 
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface ManagerDependencies
    {
    /**
     * Get algorithm name for this {@link ManagerDependencies}
     *
     * @return algorithm name
     */
    String getAlgorithm();

    /**
     * Get {@link ProviderBuilder} for this {@link ManagerDependencies}
     *
     * @return provider builder
     */
    ProviderBuilder getProviderBuilder();

    /**
     * Get {@link KeystoreDependencies} for this {@link ManagerDependencies}
     *
     * @return {@link KeystoreDependencies} representing configured/defaulted values for keystore
     */
    KeystoreDependencies getKeystoreDependencies();

    /**
     * Get passwordProvider for this Manager.
     *
     * @return passwordProvider for this Manager.
     */
    PasswordProvider getPrivateKeyPasswordProvider();

    /**
     * Add a {@link KeyStoreListener} to be notified when keystores,
     * keys or certs are loaded.
     *
     * @param listener  the listener to add
     */
    void addListener(KeyStoreListener listener);

    /**
     * Remove a {@link KeyStoreListener}.
     *
     * @param listener  the listener to be removed
     */
    void removeListener(KeyStoreListener listener);

    /**
     * Return a list of the registered KeyStoreListener{@link KeyStoreListener listeners}.
     *
     * @return a list of the registered KeyStoreListener{@link KeyStoreListener listeners}
     */
    List<KeyStoreListener> getListeners();

    /**
     * Returns the optional {@link PrivateKeyLoader} to use to load the private key.
     *
     * @return the optional {@link KeyStoreLoader} to use to load the private key
     */
    PrivateKeyLoader getPrivateKeyLoader();

    /**
     * Returns the optional {@link CertificateLoader certificate loaders} to use to load
     * the {@link Certificate certificates}.
     *
     * @return  the optional {@link CertificateLoader certificate loaders} to
     *          use to load the {@link Certificate certificates}.
     */
    CertificateLoader[] getCertificateLoaders();

    // ----- inner interface Aware ------------------------------------------

    /**
     * Implemented by classes that should be aware of the {@link ManagerDependencies}.
     */
    interface Aware
        {
        /**
         * Set the {@link ManagerDependencies}
         *
         * @param deps  the {@link ManagerDependencies}
         */
        void setDependencies(ManagerDependencies deps);
        }
    }
