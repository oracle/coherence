/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

    /**
     * Return the IP subject alternative names from the peer leaf certificate.
     *
     * @param session  the SSL session
     *
     * @return the IP SANs, or an empty collection if none are available
     *
     * @since 26.10
     */
    public static Collection<String> getIPSubjAltNames(SSLSession session)
        {
        List<String>    listNames = new ArrayList<>();
        X509Certificate cert      = getPeerLeafCert(session);
        if (cert != null)
            {
            try
                {
                Collection<List<?>> colNames = cert.getSubjectAlternativeNames();
                if (colNames != null)
                    {
                    for (List<?> name : colNames)
                        {
                        if (name.size() == 2 && Integer.valueOf(7).equals(name.get(0))
                                && name.get(1) instanceof String)
                            {
                            listNames.add((String) name.get(1));
                            }
                        }
                    }
                }
            catch (CertificateParsingException ignored)
                {
                }
            }
        return listNames;
        }

    /**
     * Parse an IPv4 or IPv6 literal without resolving a DNS name.
     *
     * @param sAddress  an address literal; IPv6 brackets and scope are allowed
     *
     * @return the address bytes, or {@code null} if the input is not a literal
     *
     * @since 26.10
     */
    public static byte[] getIPAddress(String sAddress)
        {
        if (sAddress == null || sAddress.isEmpty())
            {
            return null;
            }

        if (sAddress.startsWith("[") && sAddress.endsWith("]"))
            {
            sAddress = sAddress.substring(1, sAddress.length() - 1);
            if (sAddress.indexOf(':') < 0)
                {
                return null;
                }
            }
        if (sAddress.indexOf(':') >= 0)
            {
            int ofScope = sAddress.indexOf('%');
            if (ofScope >= 0)
                {
                if (ofScope == sAddress.length() - 1 || sAddress.indexOf('%', ofScope + 1) >= 0)
                    {
                    return null;
                    }
                for (int i = ofScope + 1; i < sAddress.length(); i++)
                    {
                    char ch = sAddress.charAt(i);
                    if (!(ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z'
                            || ch >= 'A' && ch <= 'Z' || ch == '_' || ch == '-' || ch == '.'))
                        {
                        return null;
                        }
                    }
                sAddress = sAddress.substring(0, ofScope);
                }
            int ofCompression = sAddress.indexOf("::");
            if (ofCompression >= 0 && sAddress.indexOf("::", ofCompression + 2) >= 0)
                {
                return null;
                }
            String sLeft  = ofCompression < 0 ? sAddress : sAddress.substring(0, ofCompression);
            String sRight = ofCompression < 0 ? "" : sAddress.substring(ofCompression + 2);
            List<Integer> listLeft  = parseIPv6Groups(sLeft, ofCompression < 0);
            List<Integer> listRight = parseIPv6Groups(sRight, true);
            if (listLeft == null || listRight == null)
                {
                return null;
                }
            int cGroups = listLeft.size() + listRight.size();
            if (ofCompression < 0 ? cGroups != 8 : cGroups >= 8)
                {
                return null;
                }
            byte[] abAddress = new byte[16];
            int    of        = 0;
            for (int nGroup : listLeft)
                {
                abAddress[of++] = (byte) (nGroup >>> 8);
                abAddress[of++] = (byte) nGroup;
                }
            of = 16 - listRight.size() * 2;
            for (int nGroup : listRight)
                {
                abAddress[of++] = (byte) (nGroup >>> 8);
                abAddress[of++] = (byte) nGroup;
                }
            // use the same IPv4-mapped representation as InetAddress
            boolean fMapped = abAddress[10] == (byte) 0xff && abAddress[11] == (byte) 0xff;
            for (int i = 0; i < 10 && fMapped; i++)
                {
                fMapped = abAddress[i] == 0;
                }
            return fMapped ? java.util.Arrays.copyOfRange(abAddress, 12, 16) : abAddress;
            }

        String[] asOctets = sAddress.split("\\.", -1);
        if (asOctets.length < 1 || asOctets.length > 4)
            {
            return null;
            }
        long nAddress = 0;
        for (int i = 0; i < asOctets.length; i++)
            {
            String sOctet = asOctets[i];
            if (sOctet.isEmpty())
                {
                return null;
                }
            long nOctet = 0;
            long nMax   = i == asOctets.length - 1 ? (1L << (8 * (5 - asOctets.length))) - 1 : 255;
            for (int j = 0; j < sOctet.length(); j++)
                {
                char ch = sOctet.charAt(j);
                if (ch < '0' || ch > '9')
                    {
                    return null;
                    }
                nOctet = nOctet * 10 + ch - '0';
                if (nOctet > nMax)
                    {
                    return null;
                    }
                }
            nAddress = i == asOctets.length - 1
                    ? nAddress << (8 * (5 - asOctets.length)) | nOctet : nAddress << 8 | nOctet;
            }
        return new byte[] {(byte) (nAddress >>> 24), (byte) (nAddress >>> 16),
                (byte) (nAddress >>> 8), (byte) nAddress};
        }

    /**
     * Return whether an invalid literal must be rejected instead of being
     * interpreted as a DNS identity. This includes alternative hexadecimal
     * IPv4 spellings accepted by some platform resolvers.
     *
     * @param sAddress  the reference identity
     *
     * @return whether the identity has numeric address syntax
     *
     * @since 26.10
     */
    public static boolean hasIPAddressSyntax(String sAddress)
        {
        return sAddress != null && (sAddress.indexOf(':') >= 0 || sAddress.indexOf('[') >= 0
                || sAddress.indexOf(']') >= 0 || sAddress.indexOf('%') >= 0
                || sAddress.matches("[0-9.]+")
                || sAddress.matches("(?i)(?:0x[0-9a-f]+|[0-9]+)(?:\\.(?:0x[0-9a-f]+|[0-9]+)){0,3}"));
        }

    private static List<Integer> parseIPv6Groups(String sGroups, boolean fAllowIPv4)
        {
        List<Integer> list = new ArrayList<>();
        if (sGroups.isEmpty())
            {
            return list;
            }
        String[] asGroups = sGroups.split(":", -1);
        for (int i = 0; i < asGroups.length; i++)
            {
            String sGroup = asGroups[i];
            if (sGroup.indexOf('.') >= 0)
                {
                if (!fAllowIPv4 || i != asGroups.length - 1 || sGroup.split("\\.", -1).length != 4)
                    {
                    return null;
                    }
                byte[] ab = getIPAddress(sGroup);
                if (ab == null)
                    {
                    return null;
                    }
                list.add((ab[0] & 255) << 8 | ab[1] & 255);
                list.add((ab[2] & 255) << 8 | ab[3] & 255);
                }
            else
                {
                if (sGroup.isEmpty() || sGroup.length() > 4)
                    {
                    return null;
                    }
                int nGroup = 0;
                for (int j = 0; j < sGroup.length(); j++)
                    {
                    char ch = sGroup.charAt(j);
                    int n = ch >= '0' && ch <= '9' ? ch - '0'
                            : ch >= 'a' && ch <= 'f' ? ch - 'a' + 10
                            : ch >= 'A' && ch <= 'F' ? ch - 'A' + 10 : -1;
                    if (n < 0)
                        {
                        return null;
                        }
                    nGroup = nGroup << 4 | n;
                    }
                list.add(nGroup);
                }
            }
        return list;
        }

    /**
     * Determines if an X509Certificate has been issued by another X509Certificate.
     *
     * @param cert   an X509Certificate containing the certificate to be checked
     *
     * @param issuer an X509Certificate that issued the cert certificate
     *
     * @return a boolean indicating if the certificate was issued by the issuer.
     *
     * @since 23.09
     */
    public static boolean isIssuedBy(X509Certificate cert, X509Certificate issuer)
        {
        if (!cert.getIssuerX500Principal().equals(issuer.getSubjectX500Principal()))
            {
            return false;
            }

        try
            {
            cert.verify(issuer.getPublicKey());
            return true;
            }
        catch (Exception e)
            {
            return false;
            }
        }

    /**
     * Determines if an X509Certificate is self-signed.
     *
     * @param cert An X509Certificate
     *
     * @return a boolean indicating if the certificate is self-signed.
     *
     * @since 23.09
     */
    public static boolean isSelfSigned(X509Certificate cert)
        {
        return isIssuedBy(cert, cert);
        }

    /**
     * Determines if an SSLSession uses self-signed certificates.
     *
     * @param session An SSLSession
     *
     * @return a boolean indicating if a self-signed certificate is used.
     *
     * @since 23.09
     */
    public static boolean useSelfSigned(SSLSession session)
        {
        try
            {
            Certificate[] certs = session.getLocalCertificates();
            if (certs != null && certs.length > 0 &&
                isSelfSigned(toX509(certs[0])))
                {
                return true;
                }

            certs = session.getPeerCertificates();
            if (certs != null && certs.length > 0 &&
                    isSelfSigned(toX509(certs[0])))
                {
                return true;
                }
            }
        catch (CertificateException | SSLPeerUnverifiedException ignored)
            {
            return false;
            }

        return false;
        }
    }
