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
import java.security.KeyStore;
import java.security.PrivateKey;

import java.security.cert.Certificate;

/**
 * A class that can create a {@link KeyStore}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface KeyStoreLoader
    {
    /**
     * Load a named {@link KeyStore}.
     *
     * @param sType     the {@link KeyStore} type
     * @param password  an optional {@link PasswordProvider} for the {@link KeyStore} password
     *
     * @return the {@link KeyStore} or {@code null} if no key could be loaded
     */
    KeyStore load(String sType, PasswordProvider password)
            throws GeneralSecurityException, IOException;

    /**
     * Return {@code true} if this {@link KeyStoreLoader} is enabled,
     * or {@code false} if {@link KeyStoreLoader} should not be used.
     *
     * @return {@code true} if this {@link KeyStoreLoader} is enabled,
     *         or {@code false} if {@link KeyStoreLoader} should not
     *         be used
     */
    default boolean isEnabled()
        {
        return true;
        }

    /**
     * Return {@code true} if the {@link KeyStore} loaded previously
     * by this loader should be refreshed.
     *
     * @return the default implementation always returns {@code true}
     */
    default boolean isRefreshable()
        {
        return true;
        }

    /**
     * Create a {@link KeyStore} of the specified type.
     *
     * @param sType  the type of the {@link KeyStore} to create
     *
     * @return a {@link KeyStore} of the specified type
     */
    default KeyStore createKeyStore(String sType)
            throws GeneralSecurityException, IOException
        {
        return KeyStore.getInstance(sType);
        }

    /**
     * Create a {@link KeyStore} and load a {@link PrivateKey key} and {@link Certificate certs}.
     *
     * @param sType        the {@link KeyStore} type
     * @param password     an optional {@link PasswordProvider} for the {@link KeyStore} password
     * @param key          an optional {@link PrivateKey} to load into the {@link KeyStore}
     * @param keyPassword  an optional {@link PasswordProvider} for the {@link PrivateKey} password
     * @param aCert        an optional array of {@link Certificate certs} to load into the {@link KeyStore}
     *
     * @return the {@link KeyStore} or {@code null} if no key could be loaded
     */
    default KeyStore load(String sType, PasswordProvider password, PrivateKey key, PasswordProvider keyPassword, Certificate[] aCert)
            throws GeneralSecurityException, IOException
        {
        char[] acPassword    = null;
        char[] acKeyPassword = null;

        try
            {
            KeyStore keyStore = createKeyStore(sType);

            acPassword = password == null ? null : password.get();
            if (acPassword == null)
                {
                acPassword = new char[0];
                }

            keyStore.load(null, acPassword);

            if (key != null)
                {
                if (aCert == null)
                    {
                    aCert = new Certificate[0];
                    }

                acKeyPassword = keyPassword == null ? null : keyPassword.get();
                if (acKeyPassword == null)
                    {
                    acKeyPassword = new char[0];
                    }

                keyStore.setKeyEntry("key", key, acKeyPassword, aCert);
                }
            else if (aCert != null)
                {
                for (int i = 0; i < aCert.length; i++)
                    {
                    keyStore.setCertificateEntry("cert-" + i, aCert[i]);
                    }
                }

            return keyStore;
            }
        finally
            {
            PasswordProvider.reset(acPassword, acKeyPassword);
            }
        }

    /**
     * Create a {@link KeyStore} and load {@link Certificate certs}.
     *
     * @param sType     the {@link KeyStore} type
     * @param password  an optional {@link PasswordProvider} for the {@link KeyStore} password
     * @param aCert     an array of {@link Certificate certs} to load into the {@link KeyStore}
     *
     * @return the {@link KeyStore} or {@code null} if no key could be loaded
     */
    default KeyStore load(String sType, PasswordProvider password, Certificate[] aCert)
            throws GeneralSecurityException, IOException
        {
        char[] acPassword = null;

        try
            {
            acPassword = password == null ? null : password.get();
            if (acPassword == null)
                {
                acPassword = new char[0];
                }

            KeyStore keyStore = createKeyStore(sType);
            keyStore.load(null, acPassword);

            for (int i = 0; i < aCert.length; i++)
                {
                keyStore.setCertificateEntry("cert-" + i, aCert[i]);
                }

            return keyStore;
            }
        finally
            {
            PasswordProvider.reset(acPassword);
            }
        }
    }
