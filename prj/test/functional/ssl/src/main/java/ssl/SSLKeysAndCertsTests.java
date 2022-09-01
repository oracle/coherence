/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.testing.net.EchoClient;
import com.oracle.coherence.testing.net.EchoServer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.oracle.coherence.testing.util.KeyTool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;


public class SSLKeysAndCertsTests
        extends BaseSocketProviderTests
    {
    @BeforeClass
    public static void _setup() throws Exception
        {
        KeyTool.assertCanCreateKeys();

        File fileBuild = MavenProjectFileUtils.locateBuildFolder(SSLKeysAndCertsTests.class);

        s_serverCACert     = KeyTool.createCACert(fileBuild,"server-ca", "PKCS12");
        s_serverKeyAndCert = KeyTool.createKeyCertPair(fileBuild, s_serverCACert, "server");
        s_clientCACert     = KeyTool.createCACert(fileBuild,"client-ca", "PKCS12");
        s_clientKeyAndCert = KeyTool.createKeyCertPair(fileBuild, s_clientCACert, "client");
        s_badCACert        = KeyTool.createCACert(fileBuild,"bad-ca", "PKCS12");
        s_badKeyAndCert    = KeyTool.createKeyCertPair(fileBuild, s_badCACert, "bad");
        }

    @Before
    public void setPort()
            throws IOException
        {
        System.setProperty("coherence.security.client.key", s_clientKeyAndCert.getKeyPEMNoPass().getAbsolutePath());
        System.setProperty("coherence.security.client.encrypted.key", s_clientKeyAndCert.getKeyPEM().getAbsolutePath());
        System.setProperty("coherence.security.client.password", s_clientKeyAndCert.keyPasswordString());
        System.setProperty("coherence.security.client.cert", s_clientKeyAndCert.getCert().getAbsolutePath());
        System.setProperty("coherence.security.client.ca.cert", s_clientCACert.getCert().getAbsolutePath());
        System.setProperty("coherence.security.server.key", s_serverKeyAndCert.getKeyPEMNoPass().getAbsolutePath());
        System.setProperty("coherence.security.server.encrypted.key", s_serverKeyAndCert.getKeyPEM().getAbsolutePath());
        System.setProperty("coherence.security.server.password", s_serverKeyAndCert.keyPasswordString());
        System.setProperty("coherence.security.server.cert", s_serverKeyAndCert.getCert().getAbsolutePath());
        System.setProperty("coherence.security.server.ca.cert", s_serverCACert.getCert().getAbsolutePath());

		int port;
		
		do
			{
		    ServerSocket server = new ServerSocket(0);
            port = server.getLocalPort();
            server.close();
		    }
		while (port == 65535); // doesn't support sub-ports	

        System.setProperty("test.extend.port", String.valueOf(port));
        }

    @Test
    public void testSimpleConfig()
            throws IOException
        {
        EchoClient client = createNIOClient("provider-config-client-ca-cert.xml");
        EchoServer server = createNIOServer("provider-config-server-key-and-cert.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testCertRefresh() throws Exception
        {
        Path pathCerts = Files.createTempDirectory("test");
        File fileCA    = new File(pathCerts.toFile(), "ca.cert");

        try (OutputStream out = new FileOutputStream(fileCA))
            {
            Files.copy(s_badCACert.getCert().toPath(), out);
            }

        // use an invalid CA cert first
        System.setProperty("coherence.security.server.ca.cert", fileCA.getAbsolutePath());

        EchoClient client = createNIOClient("provider-config-client-refresh-ca-cert.xml");
        EchoServer server = createNIOServer("provider-config-server-key-and-cert.xml");

        server.start();
        try
            {
            assertThat(server.getConnectionCount(), is(0));
            client.connect();

            // client should fail due to bad CA cert
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                {
                try
                    {
                    client.echo("foo");
                    }
                catch (IOException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

            assertThat(ex.getCause(), is(instanceOf(IOException.class)));

            // refresh the CA cert the client using a valid CA cert
            try (OutputStream out = new FileOutputStream(fileCA))
                {
                Files.copy(s_serverCACert.getCert().toPath(), out);
                }

            // Allow the SSLContext time to update its certs
            // The client should eventually work
            Eventually.assertDeferred(() -> safeWrite(client,"test"), is("test"));
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testTrustedServerConfig()
            throws IOException
        {
        EchoClient client = createNIOClient("provider-config-client-key-cert-and-ca-cert.xml");
        EchoServer server = createNIOServer("provider-config-server-key-cert-and-ca-cert.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testCustomKeyAndCertLoaders()
            throws IOException
        {
        EchoClient client = createNIOClient("provider-config-client-custom-key-and-cert.xml");
        EchoServer server = createNIOServer("provider-config-server-key-cert-and-ca-cert.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testTrustedServerConfigEncryptedKeys()
            throws IOException
        {
        EchoClient client = createNIOClient("provider-config-client-encrypted-key-cert-and-ca-cert.xml");
        EchoServer server = createNIOServer("provider-config-server-encrypted-key-cert-and-ca-cert.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testMutualTrustConfig()
            throws IOException
        {
        EchoClient client = createNIOClient("provider-config-client-key-and-cert-peer.xml");
        EchoServer server = createNIOServer("provider-config-server-key-and-cert-peer.xml");
        trustedServerConfigTest(client, server);
        }

    // ----- helper methods -------------------------------------------------

    private String safeWrite(EchoClient client, String s)
        {
        try
            {
            client.connect();
            return client.echo(s);
            }
        catch (IOException e)
            {
            return null;
            }
        finally
            {
            client.disconnect();
            }
        }

    // ----- data members ---------------------------------------------------

    protected static KeyTool.KeyAndCert s_serverCACert;

    protected static KeyTool.KeyAndCert s_serverKeyAndCert;

    protected static KeyTool.KeyAndCert s_clientCACert;

    protected static KeyTool.KeyAndCert s_clientKeyAndCert;

    protected static KeyTool.KeyAndCert s_badCACert;

    protected static KeyTool.KeyAndCert s_badKeyAndCert;
    }
