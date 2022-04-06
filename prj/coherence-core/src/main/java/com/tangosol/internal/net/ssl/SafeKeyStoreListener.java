/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.PasswordProvider;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Objects;

/**
 * An implementation of a {@link KeyStoreListener} that safely
 * delegates to a wrapped {@link KeyStoreListener}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class SafeKeyStoreListener
        implements KeyStoreListener
    {
    // ----- constructors -----------------------------------------------

    public SafeKeyStoreListener(KeyStoreListener delegate)
        {
        m_delegate = delegate;
        }

    // ----- KeyStoreListener methods -----------------------------------

    @Override
    public void identityStoreLoaded(KeyStore keyStore, PasswordProvider provider)
        {
        try
            {
            m_delegate.identityStoreLoaded(keyStore, provider);
            }
        catch (Throwable t)
            {
            Logger.err(t);
            }
        }

    @Override
    public void identityStoreLoaded(PrivateKey key, PasswordProvider provider, Certificate[] aCert)
        {
        try
            {
            m_delegate.identityStoreLoaded(key, provider, aCert);
            }
        catch (Throwable t)
            {
            Logger.err(t);
            }
        }

    @Override
    public void trustStoreLoaded(KeyStore keyStore, PasswordProvider provider)
        {
        try
            {
            m_delegate.trustStoreLoaded(keyStore, provider);
            }
        catch (Throwable t)
            {
            Logger.err(t);
            }
        }

    @Override
    public void trustStoreLoaded(Certificate[] aCert)
        {
        try
            {
            m_delegate.trustStoreLoaded(aCert);
            }
        catch (Throwable t)
            {
            Logger.err(t);
            }
        }

    // ----- Object methods ---------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null)
            {
            return false;
            }

        if (o instanceof SafeKeyStoreListener)
            {
            SafeKeyStoreListener that = (SafeKeyStoreListener) o;
            return Objects.equals(m_delegate, that.m_delegate);
            }
        else if (o instanceof KeyStoreListener)
            {
            return Objects.equals(m_delegate, o);
            }
        return false;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_delegate);
        }

    // ----- data members -----------------------------------------------

    private final KeyStoreListener m_delegate;
    }
