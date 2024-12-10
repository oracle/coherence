/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.net.PasswordProvider;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Provider;

import java.util.List;

/**
 * A default implementation of a {@link KeyManagersBuilder}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class DefaultKeyManagerBuilder
        extends AbstractManagerBuilder
        implements KeyManagersBuilder
    {
    @Override
    public KeyManager[] buildKeyManagers(ManagerDependencies deps, StringBuilder sb)
            throws GeneralSecurityException, IOException
        {
        if (deps == null)
            {
            sb.append("identity=unspecified");
            return null;
            }

        char[] achPassword = null;

        try
            {
            KeyManagerFactory      factory             = createKeyManagerFactory(deps);
            PasswordProvider       keyPasswordProvider = deps.getPrivateKeyPasswordProvider();
            KeystoreDependencies   depsKeystore        = deps.getKeystoreDependencies();
            List<KeyStoreListener> listeners           = deps.getListeners();

            sb.append("identity=")
                    .append(deps.getAlgorithm())
                    .append('/');

            achPassword = keyPasswordProvider == null ? null : keyPasswordProvider.get();
            if (achPassword == null)
                {
                achPassword = new char[0];
                }

            KeyStore keyStore = resolveKeystore(deps, depsKeystore, keyPasswordProvider, listeners, true, sb);

            factory.init(keyStore, achPassword);
            return factory.getKeyManagers();
            }
        finally
            {
            PasswordProvider.reset(achPassword);
            }
        }

    @Override
    public boolean isRefreshable(ManagerDependencies deps)
        {
        KeystoreDependencies depsKeyStore = deps == null ? null : deps.getKeystoreDependencies();
        return shouldRefresh(deps, depsKeyStore);
        }

    protected KeyManagerFactory createKeyManagerFactory(ManagerDependencies deps)
            throws GeneralSecurityException
        {
        KeyManagerFactory factory         = null;
        SSLSocketProviderDependenciesBuilder.ProviderBuilder providerBuilder = deps.getProviderBuilder();
        String            sAlgorithm      = deps.getAlgorithm();


        if (providerBuilder != null)
            {
            Provider provider = providerBuilder.realize(null, null, null);

            if (provider == null)
                {
                String sName = providerBuilder.getName();

                if (sName != null)
                    {
                    factory = KeyManagerFactory.getInstance(sAlgorithm, sName);
                    }
                }
            else
                {
                if (provider instanceof ManagerDependencies.Aware)
                    {
                    ((ManagerDependencies.Aware) provider).setDependencies(deps);
                    }
                factory = KeyManagerFactory.getInstance(sAlgorithm, provider);
                }
            }

        if (factory == null)
            {
            factory = KeyManagerFactory.getInstance(sAlgorithm);
            }

        return factory;
        }
    }
