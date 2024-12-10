/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;


/**
* A factory for {@link PeerX509TrustManager} instances.
*
* @author jh  2010.05.11
*/
public class PeerX509TrustManagerFactory
        extends TrustManagerFactorySpi
    {
    // ----- TrustManagerFactorySpi methods ---------------------------------

    /**
    * Return one trust manager for each type of trust material.
    *
    * @return one trust manager for each type of trust material
    */
    protected TrustManager[] engineGetTrustManagers()
        {
        return m_aTrustManager;
        }

    /**
    * Initialize this factory with a source of certificate authorities and
    * related trust material.
    *
    * @param keyStore  the trust material source
    */
    protected void engineInit(KeyStore keyStore)
            throws KeyStoreException
        {
        m_aTrustManager = new TrustManager[] {new PeerX509TrustManager(keyStore)};
        }

    /**
    * Initialize this factory with a source of provider-specific key material.
    *
    * @param params  provider-specific key material
    */
    protected void engineInit(ManagerFactoryParameters params)
            throws InvalidAlgorithmParameterException
        {
        throw new UnsupportedOperationException();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The TrustManager array returned by this TrustManagerFactory.
    */
    private volatile TrustManager[] m_aTrustManager;
    }
