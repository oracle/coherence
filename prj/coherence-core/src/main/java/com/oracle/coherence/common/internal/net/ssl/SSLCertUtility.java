/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.ssl;


import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import java.io.ByteArrayInputStream;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * SSL certificate utilities.
 *
 * @author Jonathan Knight  2020.12.07
 * @since 20.12
 */
public class SSLCertUtility
    {
    private static final String WILDCARD_PREFIX = "*.";

    /**
     * Converts a {@link Certificate} to X509Certificate
     *
     * @param cert  the certificate to convert
     * @return an X509Certificate
     * @throws CertificateException if the conversion fails
     */
    public static X509Certificate toX509(Certificate cert) throws CertificateException
        {
        if (cert == null)
            {
            return null;
            }
        if (cert instanceof X509Certificate)
            {
            return (X509Certificate) cert;
            }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
        }

    /**
     * Returns the peer leaf certificate for a SSL session.
     *
     * @param session  the SSL session
     * @return the peer leaf certificate for the SSL session
     */
    public static X509Certificate getPeerLeafCert(SSLSession session)
        {
        try
            {
            Certificate[] certs = session.getPeerCertificates();
            return ((certs == null || certs.length == 0) ? null : toX509(certs[0]));
            }
        catch (CertificateException | SSLPeerUnverifiedException ignored)
            {
            return null;
            }
        }

    /**
     * Returns the common name from a certificate.
     *
     * @param cert  the {@link X509Certificate}
     *
     * @return the certificate common name value string
     */
    public static String getCommonName(X509Certificate cert)
        {
        String cn = null;
        if (cert != null)
            {
            String dn = cert.getSubjectX500Principal().getName();
            int index = dn.indexOf("CN=");
            if (index >= 0)
                {
                boolean containsEscapeCharacters = false;
                int start = index + 3;
                int end = dn.indexOf(',', start);
                while (end > 0 && dn.charAt(end - 1) == '\\')
                    {
                    containsEscapeCharacters = true;
                    end = dn.indexOf(",", end + 1);
                    }
                if (end < 0)
                    {
                    end = dn.length();
                    }
                cn = dn.substring(start, end);

                // Unescape
                if (containsEscapeCharacters)
                    {
                    int cnLength = cn.length();
                    StringBuffer buf = new StringBuffer(cnLength);
                    for (int i = 0; i < cnLength; i++)
                        {
                        char c = cn.charAt(i);
                        if (c == '\\')
                            {
                            if (++i == cnLength)
                                {
                                break;
                                }
                            c = cn.charAt(i);
                            }
                        buf.append(c);
                        }
                    cn = buf.toString();
                    }
                }
            }
        return cn;
        }

    /**
     * Returns the common name from the peer leaf certificate of a SSL session.
     *
     * @param session  the {@link SSLSession}
     * @return the common name of peer certificate, or null if it is not available
     */
    public static String getCommonName(SSLSession session)
        {
        return getCommonName(getPeerLeafCert(session));
        }

    /**
     * Returns a collection of DNS SubjectAlternativeNames from peer certificate, or empty collection if none.
     *
     * @param session                 the {@link SSLSession}
     * @param fWildcardSANDNSNames    whether to include wildcard SAN DNS Names
     * @param fNonWildcardSANDNSNames whether to include non-wildcard SAN DNS Names
     *
     * @return a collection of DNS SubjectAlternativeNames from peer certificate, or empty collection if none.
     */
    public static Collection<String> getDNSSubjAltNames(SSLSession session,
                                                        boolean fWildcardSANDNSNames,
                                                        boolean fNonWildcardSANDNSNames)
        {
        X509Certificate cert = getPeerLeafCert(session);
        if (cert == null)
            {
            return null;
            }

        Collection<List<?>> sansCollection;
        try
            {
            sansCollection = cert.getSubjectAlternativeNames();
            }
        catch (CertificateParsingException cpe)
            {
            return null;
            }

        if (sansCollection == null)
            {
            return null;
            }

        List<String> dnsNames = new ArrayList<>();

        for (List<?> generalName : sansCollection)
            {
            // Fetching General Name from SAN
            ListIterator<?> li = generalName.listIterator();
            //For each SAN, loop through its attributes to get DNS name /Hostname
            while (li.hasNext())
                {
                Object ob = li.next();
                if (ob instanceof Integer)
                    {
                    int index = (Integer) ob;
                    if (index == 2 && li.hasNext())
                        {
                        // index 2 is dnsName/hostname
                        String dnsName = (String) li.next();
                        if (!fWildcardSANDNSNames && dnsName != null && dnsName.startsWith(WILDCARD_PREFIX))
                            {
                            continue;
                            }

                        if (!fNonWildcardSANDNSNames && dnsName != null && !dnsName.startsWith(WILDCARD_PREFIX))
                            {
                            continue;
                            }

                        dnsNames.add(dnsName);
                        }
                    }
                }
            }
        return dnsNames;
        }
    }
