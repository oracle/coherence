/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.tangosol.net.PasswordProvider;
import com.tangosol.net.ssl.CertificateLoader;
import com.tangosol.net.ssl.EmptyKeyStoreLoader;
import com.tangosol.net.ssl.KeyStoreLoader;
import com.tangosol.net.ssl.PrivateKeyLoader;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractManagerBuilder
    {
    /**
     * Create a {@link KeyStore} from the specified {@link KeystoreDependencies}.
     *
     *
     * @param depsManager          the {@link ManagerDependencies}
     * @param depsKeyStore         the {@link KeystoreDependencies} to use to build the {@link KeyStore}
     * @param keyPasswordProvider  an optional {@link PasswordProvider} to provide a private key password
     * @param listeners            the {@link KeyStoreListener listeners to be notified when keystores are loaded}
     * @param fIdentity            {@code true} it loading an identity manager, or {@code false} if loading a
     *                             trust manager
     * @param sbDescr              a {@link StringBuilder} to add the keystore description to
     *
     * @return  a {@link KeyStore} built from the {@link KeystoreDependencies}, or {@code null} if the dependencies
     *          do not specify a {@link KeyStore}
     *
     * @throws GeneralSecurityException if the {@link TrustManager} could not be created. For example, if no
     *                                  {@link java.security.Provider} supports a {@link java.security.KeyStoreSpi}
     *                                  implementation for the specified store type, or if the algorithm used to check
     *                                  the integrity of the keystore cannot be found, or if any of the certificates
     *                                  in the keystore could not be loaded.
     * @throws IOException              if there is an error loading the {@link KeyStore}, keys, certs or related data
     */
    protected KeyStore resolveKeystore(ManagerDependencies    depsManager,
                                       KeystoreDependencies   depsKeyStore,
                                       PasswordProvider       keyPasswordProvider,
                                       List<KeyStoreListener> listeners,
                                       boolean                fIdentity,
                                       StringBuilder          sbDescr)

            throws GeneralSecurityException, IOException
        {
        String         sType          = depsKeyStore.getType();
        KeyStoreLoader keyStoreLoader = depsKeyStore.getKeyStoreLoader();

        if (keyStoreLoader != null)
            {
            // create a keystore from the keystore name
            PasswordProvider passwordProvider = depsKeyStore.getPasswordProvider();
            KeyStore         keyStore         = keyStoreLoader.load(sType, passwordProvider);

            if (keyStore != null)
                {
                sbDescr.append(keyStoreLoader);
                onKeyStore(keyStore, passwordProvider, listeners, fIdentity);
                return keyStore;
                }
            }
        else
            {
            keyStoreLoader = EmptyKeyStoreLoader.INSTANCE;
            }

        PrivateKeyLoader    privateKeyLoader   = depsManager.getPrivateKeyLoader();
        CertificateLoader[] aCertificateLoader = depsManager.getCertificateLoaders();
        Certificate[]       aCertificate       = null;
        boolean             fComma             = false;

        if (privateKeyLoader != null)
            {
            sbDescr.append("key=[").append(privateKeyLoader).append("]");
            fComma = true;
            }

        if (aCertificateLoader != null && aCertificateLoader.length > 0)
            {
            if (fComma)
                {
                sbDescr.append(", ");
                }
            sbDescr.append("certs=").append(Arrays.toString(aCertificateLoader));
            }

        if (aCertificateLoader != null && aCertificateLoader.length > 0)
            {
            List<Certificate> listCert = new ArrayList<>();
            for (CertificateLoader loader : aCertificateLoader)
                {
                Certificate[] ac = loader.load();
                if (ac != null)
                    {
                    listCert.addAll(Arrays.asList(ac));
                    }
                }

            aCertificate = listCert.isEmpty() ? null : listCert.toArray(new Certificate[0]);
            }

        if (privateKeyLoader != null)
            {
            // create an identity store from private key and cert list
            PrivateKey key      = privateKeyLoader.load(keyPasswordProvider);
            KeyStore   keyStore = keyStoreLoader.load(sType, PasswordProvider.NullImplementation, key, keyPasswordProvider, aCertificate);

            onIdentityStore(keyStore, key, keyPasswordProvider, aCertificate, listeners);
            return keyStore;
            }

        if (aCertificate != null)
            {
            // create a trust store from a cert list
            KeyStore keyStore = keyStoreLoader.load(sType, PasswordProvider.NullImplementation, aCertificate);

            onCerts(keyStore, aCertificate, listeners, fIdentity);
            return keyStore;
            }

        return null;
        }

    /**
     * Returns {@code true} if the keystore, keys or certs should be refreshed.
     *
     * @param depsManager   the {@link ManagerDependencies}
     * @param depsKeyStore  the {@link KeystoreDependencies}
     *
     * @return {@code true} if the keystore, keys or certs should be refreshed
     */
    protected boolean shouldRefresh(ManagerDependencies depsManager, KeystoreDependencies depsKeyStore)
        {
        if (depsManager == null)
            {
            return true;
            }

        KeyStoreLoader keyStoreLoader = depsKeyStore == null ? null : depsKeyStore.getKeyStoreLoader();

        if (keyStoreLoader != null && keyStoreLoader.isRefreshable())
            {
            return true;
            }

        PrivateKeyLoader privateKeyLoader = depsManager.getPrivateKeyLoader();
        if (privateKeyLoader != null && privateKeyLoader.isRefreshable())
            {
            return true;
            }

        CertificateLoader[] aCertificateLoader = depsManager.getCertificateLoaders();
        if (aCertificateLoader != null)
            {
            return Arrays.stream(aCertificateLoader).anyMatch(CertificateLoader::isRefreshable);
            }

        return false;
        }

    protected void onKeyStore(KeyStore keyStore, PasswordProvider provider, List<KeyStoreListener> listeners, boolean fIdentity)
        {
        if (fIdentity)
            {
            listeners.forEach(l -> l.identityStoreLoaded(keyStore, provider));
            }
        else
            {
            listeners.forEach(l -> l.trustStoreLoaded(keyStore, provider));
            }
        }

    protected void onCerts(KeyStore keyStore, Certificate[] aCert, List<KeyStoreListener> listeners, boolean fIdentity)
        {
        if (fIdentity)
            {
            listeners.forEach(l -> l.identityStoreLoaded(null, null, aCert));
            listeners.forEach(l -> l.identityStoreLoaded(keyStore, PasswordProvider.NullImplementation));
            }
        else
            {
            listeners.forEach(l -> l.trustStoreLoaded(aCert));
            listeners.forEach(l -> l.trustStoreLoaded(keyStore, PasswordProvider.NullImplementation));
            }
        }

    protected void onIdentityStore(KeyStore keyStore, PrivateKey key, PasswordProvider keyProvider, Certificate[] aCert, List<KeyStoreListener> listeners)
        {
        listeners.forEach(l -> l.identityStoreLoaded(key, keyProvider, aCert));
        listeners.forEach(l -> l.identityStoreLoaded(keyStore, PasswordProvider.NullImplementation));
        }
    }
