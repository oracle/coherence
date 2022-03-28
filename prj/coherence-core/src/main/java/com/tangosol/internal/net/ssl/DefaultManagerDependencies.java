/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder.ProviderBuilder;
import com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.net.PasswordProvider;
import com.tangosol.net.ssl.CertificateLoader;
import com.tangosol.net.ssl.KeyStoreLoader;
import com.tangosol.net.ssl.PrivateKeyLoader;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents either identity-manager or trust-manager config and defaults.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class DefaultManagerDependencies
        implements ManagerDependencies
    {
    // ----- constructors ------------------------------------------------

    /**
     * Constructs {@link DefaultManagerDependencies}
     *
     * @param sNameManagerKind either identity-manager or trust-manager
     */
    public DefaultManagerDependencies(String sNameManagerKind)
        {
        f_sNameManagerKind = sNameManagerKind;
        }

    /**
     * Get algorithm
     *
     * @return configured algorithm or default
     */
    @Override
    public String getAlgorithm()
        {
        if (m_sAlgorithm == null)
            {
            // compute default
            if (f_sNameManagerKind.equals("trust-manager"))
                {
                m_sAlgorithm = SSLSocketProviderDefaultDependencies.DEFAULT_TRUST_ALGORITHM;
                }
            else if (f_sNameManagerKind.equals("identity-manager"))
                {
                m_sAlgorithm = SSLSocketProviderDefaultDependencies.DEFAULT_IDENTITY_ALGORITHM;
                }
            else
                {
                throw new IllegalArgumentException("unknown manager: " + f_sNameManagerKind + "; expected either identity-manager or trust-manager");
                }
            }
        return m_sAlgorithm;
        }

    @Override
    public ProviderBuilder getProviderBuilder()
        {
        return m_bldrProvider;
        }

    @Override
    public KeystoreDependencies getKeystoreDependencies()
        {
        return m_depsKeystore;
        }

    @Override
    public PasswordProvider getPrivateKeyPasswordProvider()
        {
        if (null == m_passProvider)
            {
            ParameterizedBuilder<PasswordProvider> bldr =
                    PasswordProviderBuilderProcessor.getNullPasswordProviderBuilder();
            m_passProvider = bldr.realize(null, null, null);
            }
        return m_passProvider;
        }

    @Override
    public void addListener(KeyStoreListener listener)
        {
        if (listener != null)
            {
            m_listeners.add(new SafeKeyStoreListener(listener));
            }
        }

    @Override
    public void removeListener(KeyStoreListener listener)
        {
        if (listener != null)
            {
            m_listeners.remove(listener);
            }
        }

    @Override
    public List<KeyStoreListener> getListeners()
        {
        return m_listeners;
        }

    /**
     * Set the optional {@link PrivateKeyLoader} to use to load the private key.
     *
     * @param loader  the optional {@link KeyStoreLoader} to use to load the private key
     */
    public void setPrivateKeyLoader(PrivateKeyLoader loader)
        {
        m_privateKeyLoader = loader;
        }

    /**
     * Returns the optional {@link PrivateKeyLoader} to use to load the private key.
     *
     * @return the optional {@link KeyStoreLoader} to use to load the private key
     */
    public PrivateKeyLoader getPrivateKeyLoader()
        {
        return m_privateKeyLoader;
        }

    /**
     * Set the optional {@link CertificateLoader certificate loaders} to use to load
     * the {@link Certificate certificates}.
     *
     * @param aLoader  the optional {@link CertificateLoader certificate loaders} to
     *                 use to load the {@link Certificate certificates}.
     */
    public void setCertificateLoaders(CertificateLoader[] aLoader)
        {
        m_aCertificateLoader = aLoader;
        }

    /**
     * Returns the optional {@link CertificateLoader certificate loaders} to use to load
     * the {@link Certificate certificates}.
     *
     * @return  the optional {@link CertificateLoader certificate loaders} to
     *          use to load the {@link Certificate certificates}.
     */
    public CertificateLoader[] getCertificateLoaders()
        {
        return m_aCertificateLoader;
        }

    /**
     * set the private key password using a PasswordProvider
     *
     * @param sPassword password
     */
    @Injectable("password")
    public void setPassword(String sPassword)
        {
        ParameterizedBuilder<PasswordProvider> bldr =
                PasswordProviderBuilderProcessor.getPasswordProviderBuilderForPasswordStr(sPassword);
        m_passProvider = bldr.realize(null, null, null);
        }

    /**
     * set the private key password using a {@link com.tangosol.net.URLPasswordProvider}
     *
     * @param bldrPassProvider the URL password provider
     */
    @Injectable("password-url")
    public void setPasswordURL(ParameterizedBuilder<PasswordProvider> bldrPassProvider)
        {
        setPasswordProvider(bldrPassProvider);
        }

    /**
     * set key-store password-provider
     *
     * @param bldrPasswordProvider password-provider builder
     */
    @Injectable("password-provider")
    public void setPasswordProvider(ParameterizedBuilder<PasswordProvider> bldrPasswordProvider)
        {
        ParameterizedBuilder<PasswordProvider> bldr =
                    bldrPasswordProvider == null
                    ? PasswordProviderBuilderProcessor.getNullPasswordProviderBuilder()
                    : bldrPasswordProvider;
        m_passProvider = bldr.realize(null, null, null);
        }

    /**
     * set key-store algorithm
     *
     * @param sAlgorithm algorithm
     */
    @Injectable("algorithm")
    public void setAlgorithm(String sAlgorithm)
        {
        this.m_sAlgorithm = sAlgorithm;
        }

    /**
     * set key-store dependencies
     *
     * @param deps key-store configured and defaulted dependencies
     */
    @Injectable("key-store")
    public void setKeystore(KeystoreDependencies deps)
        {
        m_depsKeystore = deps == null ? KeystoreDependencies.NullImplementation : deps;
        }

    /**
     * set manager provider builder
     *
     * @param m_bldrProvider provider builder
     */
    @Injectable("provider")
    public void setProviderBuilder(ProviderBuilder m_bldrProvider)
        {
        this.m_bldrProvider = m_bldrProvider;
        }

    // ----- constants -------------------------------------------------------

    /**
     * Either identity-manager or trust-manager.
     */
    private final String f_sNameManagerKind;

    // ----- data members ----------------------------------------------------

    /**
     * An optional custom {@link java.security.Provider} builder.
     */
    private ProviderBuilder m_bldrProvider;

    /**
     * The optional {@link KeystoreDependencies} if a keystore is being configured.
     */
    private KeystoreDependencies m_depsKeystore = KeystoreDependencies.NullImplementation;

    /**
     * The keystore algorithm
     */
    private String m_sAlgorithm;

    /**
     * An optional private key credentials {@link PasswordProvider}.
     */
    private PasswordProvider m_passProvider;

    /**
     * The optional {@link PrivateKeyLoader} to use to load the {@link PrivateKey}.
     */
    private PrivateKeyLoader m_privateKeyLoader;

    /**
     * The optional {@link CertificateLoader} array to use to load the {@link Certificate Certificates}.
     */
    private CertificateLoader[] m_aCertificateLoader;

    /**
     * The {@link KeyStoreListener listeners} to notify when keystores, keys or certs are loaded.
     */
    private final List<KeyStoreListener> m_listeners = new ArrayList<>();
    }
