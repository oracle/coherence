/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.security;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.SimpleApplication;

import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.Executable;

import com.tangosol.net.InetAddressHelper;

import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.Inet4Address;
import java.net.InetAddress;

import java.nio.file.Files;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A simple utility to create test self-signed private keys, certificates and keystores.
 *
 * @author Jonathan Knight  2025.04.11
 */
public class KeyTool
    {
    /**
     * Assert that openssl and Java keytool can be used to create test keys and certs.
     *
     * @throws org.opentest4j.TestAbortedException if the check fails
     */
    public static void assertCanCreateKeys()
        {
        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                                                                  Executable.named("openssl"),
                                                                  Arguments.of("version"),
                                                                  Console.system()))
            {

            Assumptions.assumeTrue(application.waitFor() == 0, "OpenSSL does not exist on the PATH");
            }

        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                                                                  Executable.named(KEY_TOOL),
                                                                  Arguments.of("-h"),
                                                                  Console.none()))
            {
            Assumptions.assumeTrue(application.waitFor() == 0, "Java keytool does not exist at " + KEY_TOOL);
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
     * @param fileParent   the build target directory below which the certificate directory will be created
     * @param sName       the name for the CA cert and cert CN
     * @param sStoreType  they type of the keystore to create
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createCACert(File fileParent, String sName, String sStoreType) throws IOException
        {
        File   fileCerts    = new File(fileParent, "certs");
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

        return new KeyAndCert(sName, fileKey, null, null, fileCert, sKeyPass, null, fileKeystore, sStorePass);
        }

    public static KeyAndCertSet merge(File fileParent, String sName, KeyAndCert... sources) throws IOException
        {
        if (sources == null || sources.length == 0)
            {
            throw new IllegalArgumentException("No sources specified");
            }

        File   fileCerts    = new File(fileParent, "certs");
        File   fileP12      = new File(fileCerts, sName + ".p12");
        File   fileKeystore = new File(fileCerts, sName + ".jks");
        File   fileCertsP12 = new File(fileCerts, sName + "-certs.p12");
        File   fileCertsJKS = new File(fileCerts, sName + "-certs.jks");
        String sKeyPass     = "pa55w0rd";  // For PKCS12 key stores, both passwords must be the same
        String sStorePass   = "pa55w0rd";

        String sP12Path      = fileP12.getAbsolutePath();
        String sJksPath      = fileKeystore.getAbsolutePath();
        String sP12CertsPath = fileCertsP12.getAbsolutePath();
        String sJksCertsPath = fileCertsJKS.getAbsolutePath();

        Map<String, KeyAndCert> map = new HashMap<>();

        Files.deleteIfExists(fileP12.toPath());
        Files.deleteIfExists(fileKeystore.toPath());
        Files.deleteIfExists(fileCertsP12.toPath());
        Files.deleteIfExists(fileCertsJKS.toPath());

        for (KeyAndCert source : sources)
            {
            map.put(source.m_sName, source);

            String sCert = source.m_fileCert.getAbsolutePath();
            runKeytool(Arguments.of("-import", "-alias", source.m_sName, "-storepass",  sStorePass,
                    "-noprompt", "-file", sCert, "-storetype", "PKCS12", "-keystore", sJksCertsPath));

            runKeytool(Arguments.of("-import", "-alias", source.m_sName, "-storepass",  sStorePass,
                    "-noprompt", "-file", sCert, "-storetype", "JKS", "-keystore", sP12CertsPath));

            runKeytool(Arguments.of("-importkeystore",
                    "-destkeystore", sP12Path, "-destalias", source.m_sName,
                    "-deststoretype", "PKCS12", "-deststorepass", sStorePass, "-destkeypass", sKeyPass,
                    "-srckeystore", source.m_fileP12.getAbsolutePath(), "-srcalias", source.m_sName,
                    "-srcstoretype", "PKCS12", "-srcstorepass", source.m_sStorePass, "-srckeypass", source.m_sKeyPass
            ));

            runKeytool(Arguments.of("-importkeystore",
                    "-destkeystore", sJksPath, "-destalias", source.m_sName,
                    "-deststoretype", "JKS", "-deststorepass", sStorePass, "-destkeypass", sKeyPass,
                    "-srckeystore", source.m_fileKeystore.getAbsolutePath(), "-srcalias", source.m_sName,
                    "-srcstoretype", "JKS", "-srcstorepass", source.m_sStorePass, "-srckeypass", source.m_sKeyPass
            ));
            }

        return new KeyAndCertSet(map, fileKeystore, fileP12, fileCertsJKS, fileCertsP12, sKeyPass, sStorePass);
        }

    /**
     * Create a self-signed private key and cert.
     *
     * @param fileParent     the build target directory below which the key and cert directory will be created
     * @param keyAndCertCA  the {@link KeyAndCert} holder containing the CA cert to use to sign the key and cert
     * @param sName         the name for the CA cert and cert CN
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createKeyCertPair(File fileParent, KeyAndCert keyAndCertCA, String sName) throws IOException
        {
        return KeyTool.createKeyCertPair(fileParent, sName, keyAndCertCA, sName, null);
        }

    /**
     * Create a self-signed private key and cert.
     *
     * @param fileParent     the build target directory below which the key and cert directory will be created
     * @param keyAndCertCA  the {@link KeyAndCert} holder containing the CA cert to use to sign the key and cert
     * @param sName         the name for the CA cert and cert CN
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createKeyCertPair(File fileParent, String sFilePrefix, KeyAndCert keyAndCertCA, String sName) throws IOException
        {
        return createKeyCertPair(fileParent, sFilePrefix, keyAndCertCA, sName, null);
        }

    /**
     * Create a self-signed private key and cert.
     *
     * @param fileParent         the build target directory below which the key and cert directory will be created
     * @param keyAndCertCA      the {@link KeyAndCert} holder containing the CA cert to use to sign the key and cert
     * @param sName             the name for the CA cert and cert CN
     * @param sExtendedKeyUsage  the optional value for the certificates extended key usage
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createKeyCertPair(File fileParent, KeyAndCert keyAndCertCA, String sName, String sExtendedKeyUsage) throws IOException
        {
        return createKeyCertPair(fileParent, sName, keyAndCertCA, sExtendedKeyUsage, sExtendedKeyUsage);
        }

    /**
     * Create a self-signed private key and cert.
     *
     * @param fileParent          the build target directory below which the key and cert directory will be created
     * @param sFilePrefix        the prefix to use on the file names
     * @param keyAndCertCA       the {@link KeyAndCert} holder containing the CA cert to use to sign the key and cert
     * @param sName              the name for the CA cert and cert CN
     * @param sExtendedKeyUsage  the optional value for the certificates extended key usage
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createKeyCertPair(File fileParent, String sFilePrefix, KeyAndCert keyAndCertCA, String sName, String sExtendedKeyUsage) throws IOException
        {
        File  fileCerts     = new File(fileParent, "certs");
        File  fileSign      = new File(fileCerts, sFilePrefix + "-signing.key");
        File  fileKey       = new File(fileCerts, sFilePrefix + ".key");
        File  filePEM       = new File(fileCerts, sFilePrefix + ".pem");
        File  filePEMNoPwd  = new File(fileCerts, sFilePrefix + "-nopass.pem");
        File  fileCert      = new File(fileCerts, sFilePrefix + ".cert");
        File  fileP12       = new File(fileCerts, sFilePrefix + ".p12");
        File  fileKeystore  = new File(fileCerts, sFilePrefix + ".jks");
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

        return new KeyAndCert(sName, fileKey, filePEM, filePEMNoPwd, fileCert, sKeyPass, fileP12, fileKeystore, sStorePass);
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

    // ----- inner class: KeyAndCertSet -------------------------------------

    public static class KeyAndCertSet
        {
        public KeyAndCertSet(Map<String, KeyAndCert> map, File fileKeystore, File fileP12,
                File fileCerts, File fileCertsP12, String sKeyPass, String sStorePass)
            {
            m_map          = map;
            m_fileKeystore = fileKeystore;
            m_fileP12      = fileP12;
            m_fileCerts    = fileCerts;
            m_fileCertsP12 = fileCertsP12;
            m_sKeyPass     = sKeyPass;
            m_sStorePass   = sStorePass;
            }

        /**
         * Return a {@link KeyAndCert} from this set.
         *
         * @param sName  the name of the {@link KeyAndCert} to return
         *
         * @return the named {@link KeyAndCert} from this set
         */
        public KeyAndCert getKeyAndCert(String sName)
            {
            return m_map.get(sName);
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

        /**
         * Returns the PKCS12 certs store file.
         *
         * @return the PKCS12 certs store file
         */
        public File getP12Certs()
            {
            return m_fileCertsP12;
            }

        /**
         * Returns the PKCS12 certs store URI.
         *
         * @return the PKCS12 certs store URI
         */
        public String getP12CertsURI()
            {
            return m_fileCertsP12.toURI().toASCIIString();
            }

        /**
         * Returns the JKS certs store file.
         *
         * @return the JKS certs store file
         */
        public File getCerts()
            {
            return m_fileCerts;
            }

        /**
         * Returns the JKS certs store URI.
         *
         * @return the JKS certs store URI
         */
        public String getCertsURI()
            {
            return m_fileCerts.toURI().toASCIIString();
            }

        // ----- data members ---------------------------------------------------

        /**
         * The {@link KeyAndCert} instances this set contains.
         */
        private final Map<String, KeyAndCert> m_map;

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
         * The Java key store file containing the certs.
         */
        private final File m_fileCerts;

        /**
         * The Java key store file containing the certs in PKCS12 format.
         */
        private final File m_fileCertsP12;

        /**
         * The key store password.
         */
        private final String m_sStorePass;
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
         * @param sName             the name used for the CN attribute in the key and cert
         * @param fileKey           the encrypted private key file
         * @param fileKeyPEM        the encrypted private key file in PEM format
         * @param fileKeyPEMNoPass  the unencrypted private key file in PEM format
         * @param fileCert          the certificate file
         * @param sKeyPass          the private key password
         * @param fileP12           the PKCS12 key store
         * @param fileKeystore      the JKS key store
         * @param sStorePass        the key store password
         */
        public KeyAndCert(String sName, File fileKey, File fileKeyPEM, File fileKeyPEMNoPass, File fileCert, String sKeyPass,
                          File fileP12, File fileKeystore, String sStorePass)
            {
            m_sName            = sName;
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
         * Return the name used for the CN attribute in the key and cert.
         *
         * @return the name used for the CN attribute in the key and cert
         */
        public String getName()
            {
            return m_sName;
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
         * The name used for the CN attribute of the key and cert.
         */
        private final String m_sName;

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
