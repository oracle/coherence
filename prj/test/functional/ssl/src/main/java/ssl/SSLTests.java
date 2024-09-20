/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.oracle.coherence.common.internal.net.ssl.SSLSocketChannel;

import com.oracle.coherence.testing.net.EchoClient;
import com.oracle.coherence.testing.net.EchoNIOClient;
import com.oracle.coherence.testing.net.EchoNIOServer;
import com.oracle.coherence.testing.net.EchoServer;

import java.nio.channels.SocketChannel;
import java.util.Set;
import javax.net.ssl.SSLSession;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ssl.SSLException;
import static org.junit.Assert.*;


/**
* Functional tests for SSL.
*
* @author jh  2010.04.26
*/
public class SSLTests
        extends BaseSocketProviderTests
    {
    @Before
    public void setPort()
            throws IOException
        {
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
    public void testNonSSLServerConfig()
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer((String) null);

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("SSL exception expected");
            }
        catch (SSLException e)
            {
            // expected
            }
        catch (IOException e)
            {
            // COH-4002: Handle IOException here since a RST can happen from the
            //           server side when it closes the connection with data
            //           left in the buffer.
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testSimpleConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer("provider-config-server.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.connect();
            for (int i = 0, c = getIterations(); i < c; ++i)
                {
                String sMsgI = sMsg + i;
                assertEquals(client.echo(sMsgI), sMsgI);
                }
            assertEquals(server.getConnectionCount(), 1);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testGuestServerConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer("provider-config-guest.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.connect();
            for (int i = 0, c = getIterations(); i < c; ++i)
                {
                String sMsgI = sMsg + i;
                assertEquals(client.echo(sMsgI), sMsgI);
                }
            assertEquals(server.getConnectionCount(), 1);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testRogueClientConfig()
        {
        EchoClient client = createClient("provider-config-rogue-trust.xml");
        EchoServer server = createServer("provider-config-server-trust.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("I/O exception expected");
            }
        catch (IOException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testRogueServerConfig()
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer("provider-config-rogue.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("SSL exception expected");
            }
        catch (Exception e)
            {
            // expected
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
        EchoClient client = createClient("provider-config-client-peer.xml");
        EchoServer server = createServer("provider-config-server.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testTrustedServerConfigPwdFile()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-peer-pwd-file.xml");
        EchoServer server = createServer("provider-config-server-pwd-file.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testUntrustedServerConfig()
        {
        EchoClient client = createClient("provider-config-client-peer.xml");
        EchoServer server = createServer("provider-config-guest.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("SSL exception expected");
            }
        catch (Exception e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testTrustedClientConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-trust.xml");
        EchoServer server = createServer("provider-config-server-peer.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.connect();
            for (int i = 0, c = getIterations(); i < c; ++i)
                {
                String sMsgI = sMsg + i;
                assertEquals(client.echo(sMsgI), sMsgI);
                }
            assertEquals(server.getConnectionCount(), 1);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testCustomKeyStoreLoader()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-custom-keystore.xml");
        EchoServer server = createServer("provider-config-server-peer.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.connect();
            for (int i = 0, c = getIterations(); i < c; ++i)
                {
                String sMsgI = sMsg + i;
                assertEquals(client.echo(sMsgI), sMsgI);
                }
            assertEquals(server.getConnectionCount(), 1);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testUntrustedClientConfig()
        {
        EchoClient client = createClient("provider-config-guest-trust.xml");
        EchoServer server = createServer("provider-config-server-peer.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("IO exception expected");
            }
        catch (IOException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testUntrustedClientConfigWithClientAuthNone()
            throws IOException
        {
        EchoClient client = createClient("provider-config-guest-trust.xml");
        EchoServer server = createServer("provider-config-server-peer-client-auth-none.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testUntrustedClientConfigWithClientAuthWanted()
            throws IOException
        {
        EchoClient client = createClient("provider-config-guest-trust.xml");
        EchoServer server = createServer("provider-config-server-peer-client-auth-wanted.xml");
        untrustedServerConfigTest(client, server);
        }

    @Test
    public void testTrustOnlyClientConfigWithClientAuthNone()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer("provider-config-server-peer-client-auth-none.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testTrustOnlyClientConfigWithClientAuthWanted()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer("provider-config-server-peer-client-auth-wanted.xml");
        trustedServerConfigTest(client, server);
        }

    @Test
    public void testTrustOnlyClientConfigWithClientAuthRequired()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer("provider-config-server-peer-client-auth-required.xml");
        untrustedServerConfigTest(client, server);
        }

    @Test
    public void testClientHostnameVerifierConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-hostname.xml");
        EchoServer server = createServer("provider-config-guest.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("SSL exception expected");
            }
        catch (SSLException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testClientDefaultHostnameVerifierConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-hostname-default.xml");
        EchoServer server = createServer("provider-config-guest.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("SSL exception expected");
            }
        catch (SSLException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testClientAllowHostnameVerifierConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-hostname-allow.xml");
        EchoServer server = createServer("provider-config-guest.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testServerHostnameVerifierConfig()
        {
        EchoClient client = createClient("provider-config-client-trust.xml");
        EchoServer server = createServer("provider-config-server-hostname.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("IO exception expected");
            }
        catch (IOException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testServerDefaultHostnameVerifierConfig()
        {
        EchoClient client = createClient("provider-config-client-trust.xml");
        EchoServer server = createServer("provider-config-server-hostname-default.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            if (client instanceof EchoNIOClient)
                {
                EchoNIOClient nioClient = (EchoNIOClient) client;
                SocketChannel channel   = nioClient.getChannel();
                if (channel instanceof SSLSocketChannel)
                    {
                    SSLSocketChannel sslChannel = (SSLSocketChannel) channel;
                    SSLSession       session    = sslChannel.openSSLEngine().getSession();
                    session.getPeerCertificates();
                    }
                }
            else if (server instanceof EchoNIOServer)
                {
                EchoNIOServer      nioServer = (EchoNIOServer) server;
                Set<SocketChannel> channels  = nioServer.getChannels();
                SocketChannel      channel   = channels.iterator().next();
                if (channel instanceof SSLSocketChannel)
                    {
                    SSLSocketChannel sslChannel = (SSLSocketChannel) channels.iterator().next();
                    SSLSession       session    = sslChannel.openSSLEngine().getSession();
                    session.getPeerCertificates();
                    }
                }
            fail("IOException expected");
            }
        catch (IOException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testServerRejectHostnameVerifierConfig()
        {
        EchoClient client = createClient("provider-config-client-trust.xml");
        EchoServer server = createServer("provider-config-server-hostname-default.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            if (client instanceof EchoNIOClient)
                {
                EchoNIOClient nioClient = (EchoNIOClient) client;
                SocketChannel channel   = nioClient.getChannel();
                if (channel instanceof SSLSocketChannel)
                    {
                    SSLSocketChannel sslChannel = (SSLSocketChannel) channel;
                    SSLSession       session    = sslChannel.openSSLEngine().getSession();
                    session.getPeerCertificates();
                    }
                }
            else if (server instanceof EchoNIOServer)
                {
                EchoNIOServer      nioServer = (EchoNIOServer) server;
                Set<SocketChannel> channels  = nioServer.getChannels();
                SocketChannel      channel
                        = channels.iterator().next();
                if (channel instanceof SSLSocketChannel)
                    {
                    SSLSocketChannel sslChannel = (SSLSocketChannel) channels.iterator().next();
                    SSLSession       session    = sslChannel.openSSLEngine().getSession();
                    session.getPeerCertificates();
                    }
                }
            fail("IOException expected");
            }
        catch (IOException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testServerAcceptHostnameVerifierConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-trust.xml");
        EchoServer server = createServer("provider-config-server-hostname-accept.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testMutualTrustConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-peer.xml");
        EchoServer server = createServer("provider-config-server-peer.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.connect();
            for (int i = 0, c = getIterations(); i < c; ++i)
                {
                String sMsgI = sMsg + i;
                assertEquals(client.echo(sMsgI), sMsgI);
                }
            assertEquals(server.getConnectionCount(), 1);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    @Test
    public void testMutualSelfSignedTrustConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-ss.xml");
        EchoServer server = createServer("provider-config-ss.xml");

        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.connect();
            for (int i = 0, c = getIterations(); i < c; ++i)
                {
                String sMsgI = sMsg + i;
                assertEquals(client.echo(sMsgI), sMsgI);
                }
            assertEquals(server.getConnectionCount(), 1);
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }

    /**
     * Test to verify the password-provider approach works well with the existing SSL setup.
     */
    @Test
    public void testTrustedClientConfigUsingPasswordProvider_COH12077()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-trust-password-provider.xml");
        EchoServer server = createServer("provider-config-server-peer.xml");

        trustedServerConfigTest(client, server);
        }

    protected void untrustedServerConfigTest(EchoClient client, EchoServer server)
        {
        final String sMsg = "HELLO!";

        server.start();
        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.echo(sMsg);
            fail("IO exception expected");
            }
        catch (IOException e)
            {
            // expected
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }
    }
