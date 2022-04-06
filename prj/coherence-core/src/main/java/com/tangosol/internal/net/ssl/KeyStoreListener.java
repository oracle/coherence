/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.tangosol.net.PasswordProvider;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * An interface implemented by classes that need to be aware when keystores,
 * keys or certs are loaded.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface KeyStoreListener
    {
    /**
     * Notification that an identity manager {@link KeyStore} was loaded
     *
     * @param keyStore  the identity manager {@link KeyStore}
     * @param provider  the {@link PasswordProvider} for a protected {@link KeyStore}
     */
    default void identityStoreLoaded(KeyStore keyStore, PasswordProvider provider)
        {
        }

    /**
     * Notification that an identity manager {@link PrivateKey} and
     * {@link Certificate certificates} were loaded.
     *
     * @param key       the identity manager {@link PrivateKey}
     * @param provider  the {@link PasswordProvider} for encrypted keys
     * @param aCert     the identity manager {@link Certificate certificates}
     */
    default void identityStoreLoaded(PrivateKey key, PasswordProvider provider, Certificate[] aCert)
        {
        }

    /**
     * Notification that a trust manager {@link KeyStore}  was loaded.
     *
     * @param keyStore  the trust manager {@link KeyStore} from the last refresh
     * @param provider  the {@link PasswordProvider} for a protected {@link KeyStore}
     */
    default void trustStoreLoaded(KeyStore keyStore, PasswordProvider provider)
        {
        }

    /**
     * Notification that trust manager {@link Certificate certificates} were loaded.
     *
     * @param aCert  the trust manager {@link Certificate certificates}
     */
    default void trustStoreLoaded(Certificate[] aCert)
        {
        }
    }