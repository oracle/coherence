/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.security;


import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;


/**
* X509TrustManager implementation that requires the peer's certificate to be
* present in a configured key store.
*
* @author jh  2010.05.11
*/
public class PeerX509TrustManager
        implements X509TrustManager
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Create a new PeerTrustManager that requires the peer's certificate to
    * be present in the given key store.
    *
    * @param keyStore  the key store that contains the certificates of
    *                  trusted peers
    */
    public PeerX509TrustManager(KeyStore keyStore)
        {
        if (keyStore == null)
            {
            throw new IllegalArgumentException();
            }
        m_keyStore = keyStore;
        }


    // ----- PeerTrustManager methods ---------------------------------------

    /**
    * Determine if the leaf certificate in the given certificate chain is
    * contained in the trusted peer key store.
    *
    * @param aCert      the certificate chain
    * @param sAuthType  the authentication type
    *
    * @throws CertificateException  if the certificate chain is not trusted
    */
    public void checkPeerTrusted(X509Certificate[] aCert, String sAuthType)
            throws CertificateException
        {
        if (aCert == null || aCert.length == 0)
            {
            throw new IllegalArgumentException("Missing required certificate chain");
            }
        if (aCert == null || aCert.length == 0 || sAuthType == null || sAuthType.length() == 0)
            {
            throw new IllegalArgumentException("Missing required authentication type");
            }

        try
            {
            if (m_keyStore.getCertificateAlias(aCert[0]) == null)
                {
                throw new CertificateException("Untrusted peer: " +
                        getCommonName(aCert[0].getSubjectDN()));
                }
            }
        catch (KeyStoreException e)
            {
            throw new CertificateException(e);
            }
        }


    // ----- X509TrustManager interface -------------------------------------

    /**
    * Determine if the leaf certificate in the given certificate chain is
    * contained in the trusted peer key store.
    *
    * @param aCert      the certificate chain
    * @param sAuthType  the authentication type
    *
    * @throws CertificateException  if the certificate chain is not trusted
    */
    public void checkClientTrusted(X509Certificate[] aCert, String sAuthType)
            throws CertificateException
        {
        checkPeerTrusted(aCert, sAuthType);
        }

    /**
    * Determine if the leaf certificate in the given certificate chain is
    * contained in the trusted peer key store.
    *
    * @param aCert      the certificate chain
    * @param sAuthType  the authentication type
    *
    * @throws CertificateException  if the certificate chain is not trusted
    */
    public void checkServerTrusted(X509Certificate[] aCert, String sAuthType)
            throws CertificateException
        {
        checkPeerTrusted(aCert, sAuthType);
        }

    /**
    * Return an array of certificate authority certificates which are trusted
    * for authenticating peers. Since this trust manager only checks the
    * leaf certificate of supplied certification chains, this method always
    * returns an empty array.
    *
    * @return the array of certificate authority certificates; always an
    *         empty array
    */
    public X509Certificate[] getAcceptedIssuers()
        {
        return EMPTY_CERTS;
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Return the common name of the given principal
    *
    * @param principal  the principal
    *
    * @return the common name of the given principal or null if the principal
    *         doesn't have a common name
    */
    protected String getCommonName(Principal principal)
        {
        String sCN = null;
        String sDN = principal.getName();

        int i = sDN.toUpperCase().indexOf(CN_PREFIX);
        if (i != -1)
            {
            i += CN_PREFIX_LENGTH;
            int j = sDN.indexOf(",", i);
            if (j == -1)
                {
                j = sDN.length();
                }
            sCN = sDN.substring(i, j);
            }

        return sCN;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The key store used by this TrustManager.
    */
    protected final KeyStore m_keyStore;


    // ----- constants ------------------------------------------------------

    /**
    * The alogorithm used by this TrustManager.
    */
    public static final String ALGORITHM = "PeerX509";

    /**
    * The prefix of a principal CN (common name) attribute.
    */
    private static final String CN_PREFIX = "CN=";

    /**
    * The length of a principal CN (common name) attribute prefix.
    */
    private static final int CN_PREFIX_LENGTH = CN_PREFIX.length();

    /**
    * Empty array of CA certificates.
    */
    private static final X509Certificate[] EMPTY_CERTS = new X509Certificate[0];
    }
