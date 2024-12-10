/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.SimpleApplication;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.Executable;

import com.tangosol.net.InetAddressHelper;

import org.junit.Assume;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.Inet4Address;
import java.net.InetAddress;

import java.nio.file.Files;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A simple utility to create test self-signed private keys, certificates and keystores.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
public class KeyTool
    {
    /**
     * Assert that openssl and Java keytool can be used to create test keys and certs.
     *
     * @throws org.junit.AssumptionViolatedException if the check fails
     */
    public static void assertCanCreateKeys()
        {
        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                                                                  Executable.named("openssl"),
                                                                  Arguments.of("version"),
                                                                  Console.system()))
            {
            Assume.assumeTrue("OpenSSL does not exist on the PATH",application.waitFor() == 0);
            }

        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                                                                  Executable.named(KEY_TOOL),
                                                                  Arguments.of("-h"),
                                                                  Console.none()))
            {
            Assume.assumeTrue("Java keytool does not exist at " + KEY_TOOL,application.waitFor() == 0);
            }
        }

    /**
     * Return {@code true} if openssl and Java keytool can be used to create test keys and certs.
     *
     * @return {@code true} if openssl and Java keytool can be used to create test keys and certs
     */
    public static boolean canCreateKeys()
        {
        try
            {
            assertCanCreateKeys();
            return true;
            }
        catch (Throwable e)
            {
            return false;
            }
        }

    /**
     * Create a self-signed CA cert.
     *
     * @param fileBuild   the build target directory below which the certificate directory will be created
     * @param sName       the name for the CA cert and cert CN
     * @param sStoreType  they type of the keystore to create
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createCACert(File fileBuild, String sName, String sStoreType) throws IOException
        {
        File   fileClasses  = new File(fileBuild, "test-classes");
        File   fileCerts    = new File(fileClasses, "certs");
        File   fileKey      = new File(fileCerts, sName + "-ca.key");
        File   fileCert     = new File(fileCerts, sName + "-ca.cert");
        File   fileKeystore = new File(fileCerts, sName + "-ca.jks");
        String sKeyPass    = "s3cr37";
        String sStorePass  = "s3cr37";
        String sCN         = sName;

        if (fileCerts.exists())
            {
            Files.deleteIfExists(fileKey.toPath());
            Files.deleteIfExists(fileCert.toPath());
            Files.deleteIfExists(fileKeystore.toPath());
            }
        else
            {
            assertThat(fileCerts.mkdirs(), is(true));
            }

        runOpenSSL(Arguments.of("genrsa", "-passout", "pass:" + sKeyPass,
                                "-aes256", "-out", fileKey.getAbsolutePath(),  "4096"));

        runOpenSSL(Arguments.of("req", "-passin", "pass:" + sKeyPass, "-new", "-x509",
                                "-days", "3650", "-subj", "/CN=" + sCN,
                                "-key", fileKey.getAbsolutePath(),
                                "-out", fileCert.getAbsolutePath()));

        runKeytool(Arguments.of("-import", "-storepass", sStorePass, "-noprompt", "-trustcacerts",
            "-alias", sCN, "-file", fileCert.getAbsolutePath(),
            "-keystore", fileKeystore.getAbsolutePath(),
            "-deststoretype", sStoreType));

        return new KeyAndCert(fileKey, null, null, fileCert, sKeyPass, null, fileKeystore, sStorePass);
        }

    /**
     * Create a self-signed private key and cert.
     *
     * @param fileBuild     the build target directory below which the key and cert directory will be created
     * @param keyAndCertCA  the {@link KeyAndCert} holder containing the CA cert to use to sign the key and cert
     * @param sName         the name for the CA cert and cert CN
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createKeyCertPair(File fileBuild, KeyAndCert keyAndCertCA, String sName) throws IOException
        {
        return createKeyCertPair(fileBuild, keyAndCertCA, sName, null);
        }

    /**
     * Create a self-signed private key and cert.
     *
     * @param fileBuild         the build target directory below which the key and cert directory will be created
     * @param keyAndCertCA      the {@link KeyAndCert} holder containing the CA cert to use to sign the key and cert
     * @param sName             the name for the CA cert and cert CN
     * @param sExtendedKeyUsage  the optional value for the certificates extended key usage
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createKeyCertPair(File fileBuild, KeyAndCert keyAndCertCA, String sName, String sExtendedKeyUsage) throws IOException
        {
        File  fileClasses   = new File(fileBuild, "test-classes");
        File  fileCerts     = new File(fileClasses, "certs");
        File  fileSign      = new File(fileCerts, sName + "-signing.key");
        File  fileKey       = new File(fileCerts, sName + ".key");
        File  filePEM       = new File(fileCerts, sName + ".pem");
        File  filePEMNoPwd  = new File(fileCerts, sName + "-nopass.pem");
        File  fileCert      = new File(fileCerts, sName + ".cert");
        File  fileP12       = new File(fileCerts, sName + ".p12");
        File  fileKeystore  = new File(fileCerts, sName + ".jks");
        File  fileConfig    = new File(fileCerts, "csr-details.txt");

        String sKeyPass     = "pa55w0rd";  // For PKCS12 key stores, both passwords must be the same
        String sStorePass   = "pa55w0rd";

        if (fileCerts.exists())
            {
            Files.deleteIfExists(fileSign.toPath());
            Files.deleteIfExists(fileKey.toPath());
            Files.deleteIfExists(filePEM.toPath());
            Files.deleteIfExists(filePEMNoPwd.toPath());
            Files.deleteIfExists(fileCert.toPath());
            Files.deleteIfExists(fileP12.toPath());
            Files.deleteIfExists(fileKeystore.toPath());
            Files.deleteIfExists(fileConfig.toPath());
            }
        else
            {
            assertThat(fileCerts.mkdirs(), is(true));
            }

        // Generate OpenSSL X509 extensions
        try (PrintStream out = new PrintStream(new FileOutputStream(fileConfig)))
            {
            out.println("[CustomSection]");
            if (sExtendedKeyUsage != null && !sExtendedKeyUsage.isEmpty())
                {
                out.println("extendedKeyUsage = " + sExtendedKeyUsage);
                }
            else
                {
                out.println("extendedKeyUsage = serverAuth,clientAuth");
                }

            out.print("subjectAltName = DNS:" + InetAddress.getLocalHost().getHostName());
            List<InetAddress> listAddress = InetAddressHelper.getAllLocalAddresses();
            for (InetAddress address : listAddress)
                {
                if (address instanceof Inet4Address)
                    {
                    out.printf(",IP:%s", address.getHostAddress());
                    }
                }
            }

        // Generate key:
        runOpenSSL(Arguments.of("genrsa", "-passout", "pass:" + sKeyPass, "-aes256",
            "-out", fileKey.getAbsolutePath(), "4096"));

        // Generate signing request:
        runOpenSSL(Arguments.of("req", "-passin", "pass:" + sKeyPass, "-new", "-subj", "/CN=" + sName,
                                "-key", fileKey.getAbsolutePath(),
                                "-out", fileSign.getAbsolutePath()));

        // Generate Self-signed certificate:
        runOpenSSL(Arguments.of("x509", "-req", "-passin", "pass:" + keyAndCertCA.m_sKeyPass, "-days", "3650",
                    "-in", fileSign.getAbsolutePath(),
                    "-CA", keyAndCertCA.m_fileCert.getAbsolutePath(),
                    "-CAkey", keyAndCertCA.m_fileKey.getAbsolutePath(),
                    "-set_serial", "01",
                    "-extensions", "CustomSection",
                    "-out", fileCert.getAbsolutePath(),
                    "-extfile", fileConfig.getAbsolutePath()));

        runOpenSSL(Arguments.of("x509", "-in", fileCert.getAbsolutePath(), "-text", "-noout"));

        // convert the key to PEM format
        toPem(fileKey, filePEM, sKeyPass);

        // Remove passphrase from key:
        runOpenSSL(Arguments.of("rsa", "-passin", "pass:" + sKeyPass,
                "-in", fileKey.getAbsolutePath(),
                "-out", fileKey.getAbsolutePath()));

        // convert the password-less key to PEM format
        toPem(fileKey, filePEMNoPwd, null);

        // Create PKCS12 keystore
        runOpenSSL(Arguments.of("pkcs12", "-export", "-passout", "pass:" + sStorePass, "-name", sName,
                "-inkey", filePEMNoPwd.getAbsolutePath(),
                "-in", fileCert.getAbsolutePath(),
                "-out", fileP12.getAbsolutePath()));

        // Create Java keystore
        runKeytool(Arguments.of("-importkeystore", "-storepass", sStorePass, "-noprompt",
                "-srcstoretype", "jks", "-destkeypass", sKeyPass,
                "-srcstorepass", sStorePass,
                "-srckeystore", fileP12.getAbsolutePath(),
                "-destkeystore", fileKeystore.getAbsolutePath()));

        return new KeyAndCert(fileKey, filePEM, filePEMNoPwd, fileCert, sKeyPass, fileP12, fileKeystore, sStorePass);
        }

    // ----- helper methods -------------------------------------------------

    private static void toPem(File fileKey, File filePEM, String sKeyPass)
        {
        Arguments arguments = Arguments.of("pkcs8", "-topk8", "-outform", "pem", "-v1", "PBE-SHA1-3DES",
                                           "-in", fileKey.getAbsolutePath(),
                                           "-out", filePEM.getAbsolutePath());

        if (sKeyPass != null)
            {
            arguments = arguments.with("-passin", "pass:" + sKeyPass, "-passout", "pass:" + sKeyPass);
            }
        else
            {
            arguments = arguments.with("-nocrypt");
            }

        runOpenSSL(arguments);
        }

    private static void runKeytool(Arguments arguments)
        {
        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                                                                  Executable.named(KEY_TOOL),
                                                                  arguments,
                                                                  Console.system()))
            {
            assertThat(application.waitFor(), is(0));
            }
        }

    private static void runOpenSSL(Arguments arguments)
        {
        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                                                                  Executable.named("openssl"),
                                                                  arguments,
                                                                  Console.system()))
            {
            assertThat(application.waitFor(), is(0));
            }
        }

    // ----- inner class: KeyAndCert ----------------------------------------

    /**
     * A simple holder for a key and cert.
     */
    public static class KeyAndCert
        {
        /**
         * Create a {@link KeyAndCert}.
         *
         * @param fileKey           the encrypted private key file
         * @param fileKeyPEM        the encrypted private key file in PEM format
         * @param fileKeyPEMNoPass  the unencrypted private key file in PEM format
         * @param fileCert          the certificate file
         * @param sKeyPass          the private key password
         * @param fileP12           the PKCS12 key store
         * @param fileKeystore      the JKS key store
         * @param sStorePass        the key store password
         */
        public KeyAndCert(File fileKey, File fileKeyPEM, File fileKeyPEMNoPass, File fileCert, String sKeyPass,
                          File fileP12, File fileKeystore, String sStorePass)
            {
            m_fileKey          = fileKey;
            m_fileKeyPEM       = fileKeyPEM;
            m_fileKeyPEMNoPass = fileKeyPEMNoPass;
            m_fileCert         = fileCert;
            m_sKeyPass         = sKeyPass;
            m_fileKeystore     = fileKeystore;
            m_fileP12          = fileP12;
            m_sStorePass       = sStorePass;
            }

        /**
         * Returns the key store password.
         *
         * @return the key store password
         */
        public char[] storePassword()
            {
            return m_sStorePass == null ? null : m_sStorePass.toCharArray();
            }

        /**
         * Returns the key store password.
         *
         * @return the key store password
         */
        public String storePasswordString()
            {
            return m_sStorePass;
            }

        /**
         * Returns the private key password.
         *
         * @return the private key password
         */
        public char[] keyPassword()
            {
            return m_sKeyPass == null ? null : m_sKeyPass.toCharArray();
            }

        /**
         * Returns the private key password.
         *
         * @return the private key password
         */
        public String keyPasswordString()
            {
            return m_sKeyPass;
            }

        /**
         * Returns the encrypted private key file.
         *
         * @return the encrypted private key file
         */
        public File getKey()
            {
            return m_fileKey;
            }

        /**
         * Returns the URI of the encrypted private key file.
         *
         * @return the URI of the encrypted private key file
         */
        public String getKeyURI()
            {
            return m_fileKey.toURI().toASCIIString();
            }

        /**
         * Returns the encrypted private key file in PEM format.
         *
         * @return the encrypted private key file in PEM format
         */
        public File getKeyPEM()
            {
            return m_fileKeyPEM;
            }

        /**
         * Returns the URI of the encrypted private key in PEM format.
         *
         * @return the URI of the encrypted private key file in PEM format
         */
        public String getKeyPEMURI()
            {
            return m_fileKeyPEM.toURI().toASCIIString();
            }

        /**
         * Returns the unencrypted private key file in PEM format.
         *
         * @return the unencrypted private key file in PEM format
         */
        public File getKeyPEMNoPass()
            {
            return m_fileKeyPEMNoPass;
            }

        /**
         * Returns the URI of the unencrypted private key in PEM format.
         *
         * @return the URI of the unencrypted private key file in PEM format
         */
        public String getKeyPEMNoPassURI()
            {
            return m_fileKeyPEMNoPass.toURI().toASCIIString();
            }

        /**
         * Returns the certificate file.
         *
         * @return the certificate file
         */
        public File getCert()
            {
            return m_fileCert;
            }

        /**
         * Returns the certificate URI.
         *
         * @return the certificate URI
         */
        public String getCertURI()
            {
            return m_fileCert.toURI().toASCIIString();
            }

        /**
         * Returns the private key password.
         *
         * @return the private key password
         */
        public String getKeyPass()
            {
            return m_sKeyPass;
            }

        /**
         * Returns the JKS key store file.
         *
         * @return the JKS key store file
         */
        public File getKeystore()
            {
            return m_fileKeystore;
            }

        /**
         * Returns the JKS key store URI.
         *
         * @return the JKS key store URI
         */
        public String getKeystoreURI()
            {
            return m_fileKeystore.toURI().toASCIIString();
            }

        /**
         * Returns the PKCS12 key store file.
         *
         * @return the PKCS12 key store file
         */
        public File getP12Keystore()
            {
            return m_fileP12;
            }

        /**
         * Returns the PKCS12 key store URI.
         *
         * @return the PKCS12 key store URI
         */
        public String getP12KeystoreURI()
            {
            return m_fileP12.toURI().toASCIIString();
            }

        // ----- data members ---------------------------------------------------

        /**
         * The location of the encrypted private key file.
         */
        private final File m_fileKey;

        /**
         * The location of the private key file in encrypted PEM format.
         */
        private final File m_fileKeyPEM;

        /**
         * The location of the private key file in PEM format.
         */
        private final File m_fileKeyPEMNoPass;

        /**
         * The location of the private certificate file.
         */
        private final File m_fileCert;

        /**
         * The encrypted private key file password.
         */
        private final String m_sKeyPass;

        /**
         * The Java key store file.
         */
        private final File m_fileKeystore;

        /**
         * The Java key store file in PKCS12 format.
         */
        private final File m_fileP12;

        /**
         * The key store password.
         */
        private final String m_sStorePass;
        }

    // ----- constants ---------------------------------------------------------------

    /**
     * The location of the JDK keytool executable.
     */
    private static final String KEY_TOOL = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool";
    }
