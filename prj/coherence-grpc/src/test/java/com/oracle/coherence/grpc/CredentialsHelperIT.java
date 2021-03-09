/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.tangosol.config.ConfigurationException;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerCredentials;
import io.grpc.TlsServerCredentials;
import io.netty.handler.ssl.ClientAuth;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class CredentialsHelperIT
    {
    @BeforeAll
    static void createKeys() throws Exception
        {
        Path dir = Files.createTempDirectory("coherence");
        fileKey = dir.resolve("server.key");
        write(fileKey, KEY_CONTENT);
        fileCert = dir.resolve("server.pem");
        write(fileCert, CERT_CONTENT);
        fileCA = dir.resolve("ca.pem");
        write(fileCA, CA_CONTENT);
        filePassword = dir.resolve("key-pass.txt");
        write(filePassword, PASSWORD);
        }

    @BeforeEach
    void clearProperties()
        {
        System.getProperties().keySet().stream()
                .map(String.class::cast)
                .filter(s -> s.startsWith("coherence.grpc."))
                .forEach(System::clearProperty);
        }

    @Test
    void shouldBeInsecureServerByDefault()
        {
        ServerCredentials credentials = CredentialsHelper.createServerCredentials();
        assertThat(credentials, is(instanceOf(InsecureServerCredentials.class)));
        }

    @Test
    void shouldBeSpecificallyInsecureServer()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_INSECURE);
        ServerCredentials credentials = CredentialsHelper.createServerCredentials();
        assertThat(credentials, is(instanceOf(InsecureServerCredentials.class)));
        }

    @Test
    void shouldBeTLSServer()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_KEY, fileKey.toString());
        System.setProperty(Requests.PROP_TLS_CERT, fileCert.toString());
        ServerCredentials credentials = CredentialsHelper.createServerCredentials();
        assertThat(credentials, is(instanceOf(TlsServerCredentials.class)));

        TlsServerCredentials t = (TlsServerCredentials) credentials;
        assertThat(t.getPrivateKeyPassword(), is(nullValue()));
        assertThat(t.getPrivateKey(), is(KEY_CONTENT.getBytes(StandardCharsets.UTF_8)));
        assertThat(t.getCertificateChain(), is(CERT_CONTENT.getBytes(StandardCharsets.UTF_8)));
        }

    //@Test
    void shouldBeTLSServerWithCA()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_KEY, fileKey.toString());
        System.setProperty(Requests.PROP_TLS_CERT, fileCert.toString());
        System.setProperty(Requests.PROP_TLS_CA, fileCA.toString());
        System.setProperty(Requests.PROP_TLS_CLIENT_AUTH, ClientAuth.OPTIONAL.name());
        ServerCredentials credentials = CredentialsHelper.createServerCredentials();
        assertThat(credentials, is(instanceOf(TlsServerCredentials.class)));

        TlsServerCredentials t = (TlsServerCredentials) credentials;
        assertThat(t.getPrivateKeyPassword(), is(nullValue()));
        assertThat(t.getPrivateKey(), is(KEY_CONTENT.getBytes(StandardCharsets.UTF_8)));
        assertThat(t.getCertificateChain(), is(CERT_CONTENT.getBytes(StandardCharsets.UTF_8)));
        }

    @Test
    void shouldBeTLSServerWithPassword()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_KEY, fileKey.toString());
        System.setProperty(Requests.PROP_TLS_CERT, fileCert.toString());
        System.setProperty(Requests.PROP_TLS_KEYPASS, "p455w0rd");
        ServerCredentials credentials = CredentialsHelper.createServerCredentials();
        assertThat(credentials, is(instanceOf(TlsServerCredentials.class)));

        TlsServerCredentials t = (TlsServerCredentials) credentials;
        assertThat(t.getPrivateKeyPassword(), is("p455w0rd"));
        assertThat(t.getPrivateKey(), is(KEY_CONTENT.getBytes(StandardCharsets.UTF_8)));
        assertThat(t.getCertificateChain(), is(CERT_CONTENT.getBytes(StandardCharsets.UTF_8)));
        }

    @Test
    void shouldBeTLSServerWithPasswordFromFile()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_KEY, fileKey.toString());
        System.setProperty(Requests.PROP_TLS_CERT, fileCert.toString());
        System.setProperty(Requests.PROP_TLS_KEYPASS_URI, filePassword.toUri().toString());
        ServerCredentials credentials = CredentialsHelper.createServerCredentials();
        assertThat(credentials, is(instanceOf(TlsServerCredentials.class)));

        TlsServerCredentials t = (TlsServerCredentials) credentials;
        assertThat(t.getPrivateKeyPassword(), is(PASSWORD));
        assertThat(t.getPrivateKey(), is(KEY_CONTENT.getBytes(StandardCharsets.UTF_8)));
        assertThat(t.getCertificateChain(), is(CERT_CONTENT.getBytes(StandardCharsets.UTF_8)));
        }

    @Test
    void shouldBeInvalidServer()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_PLAINTEXT);
        assertThrows(ConfigurationException.class, CredentialsHelper::createServerCredentials);
        }

    @Test
    void shouldBeInvalidTLSServerIfKeyAndCertMissing()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);

        assertThrows(ConfigurationException.class, CredentialsHelper::createServerCredentials);
        }

    @Test
    void shouldBeInvalidTLSServerIfKeyMissing()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_CERT, fileCert.toString());

        assertThrows(ConfigurationException.class, CredentialsHelper::createServerCredentials);
        }

    @Test
    void shouldBeInvalidTLSServerIfCertMissing()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_KEY, fileKey.toString());

        assertThrows(ConfigurationException.class, CredentialsHelper::createServerCredentials);
        }


    @Test
    void shouldBeInvalidIfTLSKeyMissing()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_KEY, "foo");
        System.setProperty(Requests.PROP_TLS_CERT, fileCert.toString());
        assertThrows(ConfigurationException.class, CredentialsHelper::createServerCredentials);
        }

    @Test
    void shouldBeInvalidIfTLSCertMissing()
        {
        System.setProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_TLS);
        System.setProperty(Requests.PROP_TLS_KEY, fileKey.toString());
        System.setProperty(Requests.PROP_TLS_CERT, "bar");
        assertThrows(ConfigurationException.class, CredentialsHelper::createServerCredentials);
        }


    private static void write(Path path, String sText) throws IOException
        {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path.toFile())))
            {
            writer.print(sText);
            writer.flush();
            }
        }

    // ----- data members ---------------------------------------------------

    public static final String KEY_CONTENT = "test private key";
    public static final String CERT_CONTENT = "test cert";
    public static final String CA_CONTENT = "test CA";
    public static final String PASSWORD = "secret";

    static Path fileKey;

    static Path fileCert;

    static Path fileCA;

    static Path filePassword;
    }
