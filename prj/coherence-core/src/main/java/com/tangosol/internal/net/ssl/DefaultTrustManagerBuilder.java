/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder.ProviderBuilder;
import com.tangosol.net.PasswordProvider;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Provider;
import java.util.List;

/**
 * A default implementation of a {@link TrustManagersBuilder}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class DefaultTrustManagerBuilder
        extends AbstractManagerBuilder
        implements TrustManagersBuilder
    {
    @Override
    public TrustManager[] buildTrustManagers(ManagerDependencies deps, StringBuilder sb)
            throws GeneralSecurityException, IOException
        {
        if (deps == null)
            {
            sb.append("trust=unspecified");
            return null;
            }

        TrustManagerFactory    factory      = createTrustManagerFactory(deps);
        KeystoreDependencies   depsKeystore = deps.getKeystoreDependencies();
        List<KeyStoreListener> listeners    = deps.getListeners();

        sb.append("trust=")
                .append(deps.getAlgorithm())
                .append('/');

        KeyStore keyStore = resolveKeystore(deps, depsKeystore, PasswordProvider.NullImplementation, listeners, false, sb);

        factory.init(keyStore);

        return factory.getTrustManagers();
        }

    @Override
    public boolean isRefreshable(ManagerDependencies deps)
        {
        KeystoreDependencies depsKeyStore = deps == null ? null : deps.getKeystoreDependencies();
        return shouldRefresh(deps, depsKeyStore);
        }

    /**
     * Create a {@link TrustManagerFactory}.
     *
     * @param deps  the {@link ManagerDependencies trust manager dependencies}
     *
     * @return a {@link TrustManagerFactory}
     */
    protected TrustManagerFactory createTrustManagerFactory(ManagerDependencies deps)
            throws GeneralSecurityException
        {
        TrustManagerFactory factory         = null;
        ProviderBuilder     providerBuilder = deps.getProviderBuilder();
        String              sAlgorithm      = deps.getAlgorithm();

        if (providerBuilder != null)
            {
            Provider provider = providerBuilder.realize(null, null, null);

            if (provider == null)
                {
                String sName = providerBuilder.getName();

                if (sName != null)
                    {
                    factory = TrustManagerFactory.getInstance(sAlgorithm, sName);
                    }
                }
            else
                {
                if (provider instanceof ManagerDependencies.Aware)
                    {
                    ((ManagerDependencies.Aware) provider).setDependencies(deps);
                    }
                factory = TrustManagerFactory.getInstance(sAlgorithm, provider);
                }
            }

        if (factory == null)
            {
            factory = TrustManagerFactory.getInstance(sAlgorithm);
            }

        return factory;
        }
    }
