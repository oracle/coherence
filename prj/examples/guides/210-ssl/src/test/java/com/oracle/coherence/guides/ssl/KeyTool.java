/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.ssl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.SimpleApplication;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.Executable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * A simple utility to create test self-signed private keys, certificates and keystores.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
public class KeyTool {
    /**
     * The location of the JDK keytool executable.
     */
    private static final String KEY_TOOL = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool";

    /**
     * Assert that openssl and Java keytool can be used to create test keys and certs.
     *
     * @throws AssertionError if the check fails
     */
    public static void assertCanCreateKeys() {
        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                Executable.named("openssl"),
                Arguments.of("version"),
                Console.system())) {
            assertThat("OpenSSL does not exist", application.waitFor() == 0);
        }

        assertThat("Java keytool does not exist", new File(KEY_TOOL).exists());
    }

    /**
     * Create a self-signed CA cert.
     *
     * @param fileBuild   the build target directory below which the certificate directory will be created
     * @param name       the name for the CA cert and cert CN
     * @param storeType  they type of the keystore to create
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createCACert(File fileBuild, String name, String storeType) throws IOException {
        File   fileClasses  = new File(fileBuild, "test-classes");
        File   fileCerts    = new File(fileClasses, "certs");
        File   fileKey      = new File(fileCerts, name + "-ca.key");
        File   fileCert     = new File(fileCerts, name + "-ca.cert");
        File   fileKeystore = new File(fileCerts, name + "-ca.jks");
        String sKeyPass     = "s3cr37";
        String sStorePass   = "s3cr37";

        if (fileCerts.exists()) {
            Files.deleteIfExists(fileKey.toPath());
            Files.deleteIfExists(fileCert.toPath());
            Files.deleteIfExists(fileKeystore.toPath());
        }
        else {
            assertThat(fileCerts.mkdirs(), is(true));
        }

        runOpenSSL(Arguments.of("genrsa", "-passout", "pass:" + sKeyPass,
                "-aes256", "-out", fileKey.getAbsolutePath(), "4096"));

        runOpenSSL(Arguments.of("req", "-passin", "pass:" + sKeyPass, "-new", "-x509",
                "-days", "3650", "-subj", "/CN=" + name,
                "-key", fileKey.getAbsolutePath(),
                "-out", fileCert.getAbsolutePath()));

        runKeytool(Arguments.of("-import", "-storepass", sStorePass, "-noprompt", "-trustcacerts",
                "-alias", name, "-file", fileCert.getAbsolutePath(),
                "-keystore", fileKeystore.getAbsolutePath(),
                "-deststoretype", storeType));

        return new KeyAndCert(fileKey, null, null, fileCert, sKeyPass, null, fileKeystore, sStorePass);
    }

    /**
     * Create a self-signed private key and cert.
     *
     * @param fileBuild     the build target directory below which the key and cert directory will be created
     * @param keyAndCertCA  the {@link KeyAndCert} holder containing the CA cert to use to sign the key and cert
     * @param name         the name for the CA cert and cert CN
     *
     * @return the {@link KeyAndCert} holder containing the CA cert details
     *
     * @throws IOException if the cert creation fails
     */
    public static KeyAndCert createKeyCertPair(File fileBuild, KeyAndCert keyAndCertCA, String name) throws IOException {
        File   fileClasses  = new File(fileBuild, "test-classes");
        File   fileCerts    = new File(fileClasses, "certs");
        File   fileSign     = new File(fileCerts, name + "-signing.key");
        File   fileKey      = new File(fileCerts, name + ".key");
        File   filePEM      = new File(fileCerts, name + ".pem");
        File   filePEMNoPwd = new File(fileCerts, name + "-nopass.pem");
        File   fileCert     = new File(fileCerts, name + ".cert");
        File   fileP12      = new File(fileCerts, name + ".p12");
        File   fileKeystore = new File(fileCerts, name + ".jks");
        String keyPass      = "pa55w0rd";
        String storePass    = "pa55w0rd";

        if (fileCerts.exists()) {
            Files.deleteIfExists(fileSign.toPath());
            Files.deleteIfExists(fileKey.toPath());
            Files.deleteIfExists(filePEM.toPath());
            Files.deleteIfExists(filePEMNoPwd.toPath());
            Files.deleteIfExists(fileCert.toPath());
            Files.deleteIfExists(fileP12.toPath());
            Files.deleteIfExists(fileKeystore.toPath());
        }
        else {
            assertThat(fileCerts.mkdirs(), is(true));
        }

        // Generate key:
        runOpenSSL(Arguments.of("genrsa", "-passout", "pass:" + keyPass, "-aes256",
                "-out", fileKey.getAbsolutePath(), "4096"));

        // Generate signing request:
        runOpenSSL(Arguments.of("req", "-passin", "pass:" + keyPass, "-new", "-subj", "/CN=" + name,
                "-key", fileKey.getAbsolutePath(),
                "-out", fileSign.getAbsolutePath()));

        // Generate Self-signed certificate:
        runOpenSSL(Arguments.of("x509", "-req", "-passin", "pass:" + keyAndCertCA.keyPass, "-days", "3650",
                "-in", fileSign.getAbsolutePath(),
                "-CA", keyAndCertCA.fileCert.getAbsolutePath(),
                "-CAkey", keyAndCertCA.fileKey.getAbsolutePath(),
                "-set_serial", "01",
                "-out", fileCert.getAbsolutePath()));

        // convert the key to PEM format
        toPem(fileKey, filePEM, keyPass);

        // Remove passphrase from key:
        runOpenSSL(Arguments.of("rsa", "-passin", "pass:" + keyPass,
                "-in", fileKey.getAbsolutePath(),
                "-out", fileKey.getAbsolutePath()));

        // convert the password-less key to PEM format
        toPem(fileKey, filePEMNoPwd, null);

        // Create PKCS12 keystore
        runOpenSSL(Arguments.of("pkcs12", "-export", "-passout", "pass:" + storePass, "-name", name,
                "-inkey", filePEMNoPwd.getAbsolutePath(),
                "-in", fileCert.getAbsolutePath(),
                "-out", fileP12.getAbsolutePath()));

        // Create Java keystore
        runKeytool(Arguments.of("-importkeystore", "-storepass", storePass, "-noprompt",
                "-srcstoretype", "jks", "-destkeypass", keyPass,
                "-srcstorepass", storePass,
                "-srckeystore", fileP12.getAbsolutePath(),
                "-destkeystore", fileKeystore.getAbsolutePath()));

        return new KeyAndCert(fileKey, filePEM, filePEMNoPwd, fileCert, keyPass, fileP12, fileKeystore, storePass);
    }

    private static void toPem(File fileKey, File filePEM, String keyPass) {
        Arguments arguments = Arguments.of("pkcs8", "-topk8", "-outform", "pem", "-v1", "PBE-MD5-DES",
                "-in", fileKey.getAbsolutePath(),
                "-out", filePEM.getAbsolutePath());

        if (keyPass != null) {
            arguments = arguments.with("-passin", "pass:" + keyPass, "-passout", "pass:" + keyPass);
        }
        else {
            arguments = arguments.with("-nocrypt");
        }

        runOpenSSL(arguments);
    }

    private static void runKeytool(Arguments arguments) {
        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                Executable.named(KEY_TOOL),
                arguments,
                Console.system())) {
            assertThat(application.waitFor(), is(0));
        }
    }

    private static void runOpenSSL(Arguments arguments) {
        try (Application application = LocalPlatform.get().launch(SimpleApplication.class,
                Executable.named("openssl"),
                arguments,
                Console.system())) {
            assertThat(application.waitFor(), is(0));
        }
    }

    /**
     * A simple holder for a key and cert.
     */
    public static class KeyAndCert {
        protected final File   fileKey;
        protected final File   fileKeyPEM;
        protected final File   fileKeyPEMNoPass;
        protected final File   fileCert;
        protected final String keyPass;
        protected final File   fileKeystore;
        protected final File   fileP12;
        protected final String storePass;

        public KeyAndCert(File fileKey, File fileKeyPEM, File fileKeyPEMNoPass, File fileCert, String keyPass,
                          File fileP12, File fileKeystore, String storePass) {
            this.fileKey = fileKey;
            this.fileKeyPEM = fileKeyPEM;
            this.fileKeyPEMNoPass = fileKeyPEMNoPass;
            this.fileCert = fileCert;
            this.keyPass = keyPass;
            this.fileKeystore = fileKeystore;
            this.fileP12 = fileP12;
            this.storePass = storePass;
        }

        public char[] storePassword() {
            return storePass == null ? null : storePass.toCharArray();
        }

        public String storePasswordString() {
            return storePass;
        }

        public char[] keyPassword() {
            return keyPass == null ? null : keyPass.toCharArray();
        }

        public String keyPasswordString() {
            return keyPass;
        }
    }
}
