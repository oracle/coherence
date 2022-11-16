/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;


import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.config.expression.ChainedParameterResolver;
import com.tangosol.config.expression.ScopedParameterResolver;
import com.tangosol.config.expression.SystemEnvironmentParameterResolver;
import com.tangosol.config.expression.SystemPropertyParameterResolver;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import net.EchoClient;
import net.EchoServer;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.net.ssl.SSLException;
import static org.junit.Assert.*;


/**
* Functional tests for SSL.
*
* @author jh  2010.04.26
*/
public class SSLTests
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
            throws IOException
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
            throws IOException
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
            throws IOException
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
            throws IOException
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
    public void testUntrustedClientConfig()
            throws IOException
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
            throws IOException
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
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-trust.xml");
        EchoServer server = createServer("provider-config-server-hostname-default.xml");

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
    public void testServerAllowHostnameVerifierConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-trust.xml");
        EchoServer server = createServer("provider-config-server-hostname-allow.xml");

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
     *
     * @throws IOException
     */
    @Test
    public void testTrustedClientConfigUsingPasswordProvider_COH12077()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client-trust-password-provider.xml");
        EchoServer server = createServer("provider-config-server-peer.xml");

        trustedServerConfigTest(client, server);
        }

    // ----- helper methods -------------------------------------------------

    protected EchoClient createClient(SocketProvider provider)
        {
        return new EchoClient(provider, getPort());
        }

    protected EchoServer createServer(SocketProvider provider)
        {
        return new EchoServer(provider, getPort());
        }

    protected EchoClient createClient(String sFile)
            throws IOException
        {
        return createClient(createSocketProvider(sFile));
        }

    protected EchoServer createServer(String sFile)
            throws IOException
        {
        return createServer(createSocketProvider(sFile));
        }

    protected SocketProvider createSocketProvider(String sFile)
            throws IOException
        {
        XmlDocument xml = null;
        SocketProviderFactory factory = new SocketProviderFactory();
        if (sFile == null)
            {
            return factory.getDemultiplexedSocketProvider(null, null, 1);
            }
        if (sFile.length() == 0)
            {
            throw new IllegalArgumentException();
            }

        xml = XmlHelper.loadFileOrResource(sFile, null);
        SocketProviderBuilder bldr = getSocketProviderBuilder(xml);
        return bldr.getDemultiplexedSocketProvider(1);
        }

    protected SocketProviderBuilder getSocketProviderBuilder(XmlDocument xml)
        {
        DefaultClusterDependencies deps = new DefaultClusterDependencies();

        deps.setSocketProviderFactory(new SocketProviderFactory());

        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(dependencies, null);

        // add the default namespace handler
        NamespaceHandler handler = dependencies.getDefaultNamespaceHandler();

        if (handler != null)
            {
            ctxClusterConfig.ensureNamespaceHandler("", handler);
            }

        dependencies.setResourceRegistry(new SimpleResourceRegistry());

        // a ResourceRegistry for the cluster (this will be discarded after parsing)
        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        // establish a default ParameterResolver based on the System properties
        // COH-9952 wrap the code in privileged block for upper-stack products
        ScopedParameterResolver resolver = AccessController.doPrivileged(new PrivilegedAction<ScopedParameterResolver>()
            {
            public ScopedParameterResolver run()
                {
                return new ScopedParameterResolver(
                        new ChainedParameterResolver(
                                new SystemPropertyParameterResolver(),
                                new SystemEnvironmentParameterResolver()));
                }
            });

        // finish configuring the dependencies
        dependencies.setResourceRegistry(resourceRegistry);
        dependencies.setDefaultParameterResolver(resolver);
        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dependencies.setClassLoader(Base.getContextClassLoader());


        // add the ParameterizedBuilderRegistry as a Cookie so we can look it up
        ctxClusterConfig.addCookie(ParameterizedBuilderRegistry.class, deps.getBuilderRegistry());
        ctxClusterConfig.addCookie(DefaultClusterDependencies.class, deps);
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(ctxClusterConfig, xml);

        return (SocketProviderBuilder) ctxSocketProviders.processDocument(xml);
        }

    protected int getPort()
        {
        return Integer.getInteger("test.extend.port", 7778);
        }

    protected int getIterations()
        {
        return 1000;
        }

    protected void trustedServerConfigTest(EchoClient client, EchoServer server)
            throws IOException
        {
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
    }
