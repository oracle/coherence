/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.base.Reads;
import com.tangosol.net.PasswordProvider;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import javax.crypto.spec.PBEKeySpec;

import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.security.KeyFactory;
import java.security.PrivateKey;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to read PEM encoded keys and certificates.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
public class PemReader
    {
    /**
     * This is a utility class and cannot be constructed.
     */
    private PemReader()
        {
        }

    // ----- PemReader methods ----------------------------------------------

    /**
     * Read a {@link PrivateKey} in PEM format.
     *
     * @param in        the PEM file data
     * @param provider  the optional credentials provider for an encrypted private key
     *
     * @return the {@link PrivateKey} loaded from the data
     *
     * @throws IOException       if the data cannot be read
     * @throws SecurityException if the key file is in an unsupported format or
     *                           cannot be decoded
     */
    public static PrivateKey readPrivateKey(InputStream in, PasswordProvider provider) throws IOException
        {
        PrivateKeyInfo pkInfo = readPrivateKeyBytes(Reads.read(in));
        switch (pkInfo.type)
            {
            case "PKCS1-RSA":
                throw new SecurityException("PKCS#1 RSA private key is not supported");
            case "PKCS1-DSA":
                throw new SecurityException("PKCS#1 DSA private key is not supported");
            case "PKCS1-EC":
                throw new SecurityException("PKCS#1 EC private key is not supported");
            case "PKCS8":
            default:
                return pkcs8(generateKeySpec(pkInfo.bytes, provider));
            }
        }

    /**
     * Read an array of {@link X509Certificate} instances.
     *
     * @param in     the certificate data
     *
     * @return the {@link PrivateKey} loaded from the data
     *
     * @throws IOException        if the data cannot be read
     * @throws SecurityException  if the {@link CertificateFactory} cannot be created
     *                            or the {@link InputStream} contains no certificates
     */
    public static Certificate[] readCertificates(InputStream in) throws IOException
        {
        CertificateFactory cf;
        try
            {
            cf = CertificateFactory.getInstance("X.509");
            }
        catch (CertificateException e)
            {
            throw new SecurityException("Failed to create certificate factory", e);
            }

        List<Certificate> certs   = new ArrayList<>();
        String            content = new String(Reads.read(in), StandardCharsets.US_ASCII);
        Matcher           matcher = CERT_PATTERN.matcher(content);
        int               start   = 0;

        while (matcher.find(start))
            {
            byte[] base64 = matcher.group(1).getBytes(StandardCharsets.US_ASCII);
            byte[] der    = Base64.getMimeDecoder().decode(base64);
            try
                {
                certs.add(cf.generateCertificate(new ByteArrayInputStream(der)));
                }
            catch (Exception e)
                {
                throw new IOException("Failed to read certificate from bytes", e);
                }

            start = matcher.end();
            }

        if (certs.isEmpty())
            {
            throw new SecurityException("Found no certificates in input stream");
            }

        return certs.toArray(new Certificate[0]);
        }

    // ----- helper methods -------------------------------------------------

    private static PrivateKey pkcs8(KeySpec keySpec)
        {
        try
            {
            return rsaPrivateKey(keySpec);
            }
        catch (Exception rsaException)
            {
            try
                {
                return dsaPrivateKey(keySpec);
                }
            catch (Exception dsaException)
                {
                try
                    {
                    return ecPrivateKey(keySpec);
                    }
                catch (Exception ecException)
                    {
                    SecurityException e = new SecurityException("Failed to get private key. It is not RSA, DSA or EC.");
                    e.addSuppressed(rsaException);
                    e.addSuppressed(dsaException);
                    e.addSuppressed(ecException);
                    throw e;
                    }
                }
            }
        }

    private static PrivateKey ecPrivateKey(KeySpec keySpec)
        {
        try
            {
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
            }
        catch (Exception e)
            {
            throw new SecurityException("Failed to get EC private key", e);
            }
        }

    private static PrivateKey dsaPrivateKey(KeySpec keySpec)
        {
        try
            {
            return KeyFactory.getInstance("DSA").generatePrivate(keySpec);
            }
        catch (Exception e)
            {
            throw new SecurityException("Failed to get DSA private key", e);
            }
        }

    private static PrivateKey rsaPrivateKey(KeySpec keySpec)
        {
        try
            {
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            }
        catch (Exception e)
            {
            throw new SecurityException("Failed to get RSA private key", e);
            }
        }

    private static KeySpec generateKeySpec(byte[] keyBytes, PasswordProvider provider)
        {
        char[] acPassword = null;

        try
            {
            acPassword = provider == null ? null : provider.get();

            if (acPassword == null || acPassword.length == 0)
                {
                return new PKCS8EncodedKeySpec(keyBytes);
                }

            try
                {
                EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(keyBytes);
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
                PBEKeySpec pbeKeySpec = new PBEKeySpec(acPassword);
                SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);

                Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
                cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());

                return encryptedPrivateKeyInfo.getKeySpec(cipher);
                }
            catch (Exception e)
                {
                throw new SecurityException("Failed to create key spec for key", e);
                }
            }
        finally
            {
            PasswordProvider.reset(acPassword);
            }
        }

    private static PrivateKeyInfo readPrivateKeyBytes(byte[] ab)
        {
        String  content = new String(ab, StandardCharsets.US_ASCII);
        Matcher matcher = KEY_PATTERN.matcher(content);

        if (!matcher.find())
            {
            throw new SecurityException("Could not find a PKCS#8 private key in input stream");
            }

        byte[] base64 = matcher.group(1).getBytes(StandardCharsets.US_ASCII);
        String type;
        if (content.startsWith("-----BEGIN PRIVATE KEY-----")
                || content.startsWith("-----BEGIN ENCRYPTED PRIVATE KEY-----"))
            {
            // in this case, we do not know the type, we must try all
            type = "PKCS8";
            }
        else if (content.startsWith("-----BEGIN RSA PRIVATE KEY-----"))
            {
            type = "PKCS1-RSA";
            }
        else if (content.startsWith("-----BEGIN DSA PRIVATE KEY-----"))
            {
            type = "PKCS1-DSA";
            }
        else if (content.startsWith("-----BEGIN EC PRIVATE KEY-----"))
            {
            type = "PKCS1-EC";
            }
        else
            {
            int firstEol = content.indexOf("\n");
            if (firstEol < 1)
                {
                throw new SecurityException("Could not find a PKCS#8 private key in input stream");
                }
            else
                {
                throw new SecurityException("Unsupported key type: " + content.substring(0, firstEol));
                }
            }
        return new PrivateKeyInfo(type, Base64.getMimeDecoder().decode(base64));
        }

    // ----- inner class: PrivateKeyInfo ------------------------------------

    /**
     * A simple holder for information about a private key.
     */
    private static final class PrivateKeyInfo
        {
        private PrivateKeyInfo(String type, byte[] bytes)
            {
            this.type = type;
            this.bytes = bytes;
            }

        private final String type;

        private final byte[] bytes;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A regular expression matching a PEM certificate file.
     */
    private static final Pattern CERT_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" // Header
                    + "([a-z0-9+/=\\r\\n]+)"                        // Base64 text
                    + "-+END\\s+.*CERTIFICATE[^-]*-+",              // Footer
            Pattern.CASE_INSENSITIVE);


    /**
     * A regular expression matching a PEM key file.
     */
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" // Header
                    + "([a-z0-9+/=\\r\\n]+)"                           // Base64 text
                    + "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",              // Footer
            Pattern.CASE_INSENSITIVE);
    /**
     * A regular expression matching a public key file.
     */
    private static final Pattern PUBLIC_KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*PUBLIC\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+"  // Header
                    + "([a-z0-9+/=\\r\\n\\s]+)"                        // Base64 text
                    + "-+END\\s+.*PUBLIC\\s+KEY[^-]*-+",               // Footer
            Pattern.CASE_INSENSITIVE);
    }
