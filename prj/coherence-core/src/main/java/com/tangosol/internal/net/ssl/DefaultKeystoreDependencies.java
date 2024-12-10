/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.PasswordProvider;

import com.tangosol.net.ssl.KeyStoreLoader;

import java.security.KeyStore;

/**
 * A default implementation of {@link KeystoreDependencies}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class DefaultKeystoreDependencies
        implements KeystoreDependencies
    {
    // ----- KeystoreDependencies methods --------------------------------

    /**
     * Get the configured/defaulted keystore defaults.
     *
     * @return the keystore type
     */
    @Override
    public String getType()
        {
        return m_sType;
        }

    /**
     * Returns the optional {@link KeyStoreLoader} to use to load the {@link KeyStore}.
     *
     * @return the optional {@link KeyStoreLoader} to use to load the {@link KeyStore}
     */
    @Override
    public KeyStoreLoader getKeyStoreLoader()
        {
        return m_keyStoreLoader;
        }

    /**
     * Get the configured private key passwordProvider.
     *
     * @return private key passwordProvider.
     */
    @Override
    public PasswordProvider getPasswordProvider()
        {
        if (null == m_passProvider)
            {
            ParameterizedBuilder<PasswordProvider> builder =
                    PasswordProviderBuilderProcessor.getNullPasswordProviderBuilder();
            m_passProvider = builder.realize(null, null, null);
            }
        return m_passProvider;
        }

    // ----- DefaultKeystoreDependencies methods -------------------------

    /**
     * Set the keystore type.
     *
     * @param sType the keystore type
     */
    @Injectable("type")
    public void setType(String sType)
        {
        m_sType = sType;
        }

    /**
     * Set the keystore password using a PasswordProvider.
     *
     * @param sPassword the keystore password
     */
    @Injectable("password")
    public void setPassword(String sPassword)
        {
        ParameterizedBuilder<PasswordProvider> builder =
                PasswordProviderBuilderProcessor.getPasswordProviderBuilderForPasswordStr(sPassword);
        m_passProvider = builder.realize(null, null, null);
        }

    /**
     * set keystore password using a {@link com.tangosol.net.URLPasswordProvider}
     *
     * @param builder the URL password provider
     */
    @Injectable("password-url")
    public void setPasswordURL(ParameterizedBuilder<PasswordProvider> builder)
        {
        setPasswordProvider(builder);
        }

    /**
     * Set the keystore password-provider
     *
     * @param builder the keystore password provider
     */
    @Injectable("password-provider")
    public void setPasswordProvider(ParameterizedBuilder<PasswordProvider> builder)
        {
        m_passProvider = builder == null
                ? PasswordProvider.NullImplementation
                : builder.realize(null, null, null);
        }

    /**
     * Set the optional {@link KeyStoreLoader} to use to load the {@link KeyStore}.
     *
     * @param loader  the optional {@link KeyStoreLoader} to use to load the {@link KeyStore}
     */
    public void setKeyStoreLoader(KeyStoreLoader loader)
        {
        if (loader.isEnabled())
            {
            m_keyStoreLoader = loader;
            }
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "keyStore=" + m_keyStoreLoader;
        }

    // ----- data members ------------------------------------------------

    /**
     * passwordProvider for keyStore to fetch password
     */
    private PasswordProvider m_passProvider;

    /**
     * keystore type
     */
    private String m_sType = SSLSocketProvider.Dependencies.DEFAULT_KEYSTORE_TYPE;

    /**
     * The optional {@link KeyStoreLoader} to use to load the {@link KeyStore}.
     */
    private KeyStoreLoader m_keyStoreLoader;
    }
