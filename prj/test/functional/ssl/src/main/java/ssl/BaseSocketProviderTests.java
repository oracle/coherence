/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.oracle.bedrock.runtime.LocalPlatform;
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
import com.oracle.coherence.testing.net.EchoClient;
import com.oracle.coherence.testing.net.EchoNIOClient;
import com.oracle.coherence.testing.net.EchoNIOServer;
import com.oracle.coherence.testing.net.EchoServer;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
* Functional tests for SSL.
*
* @author jh  2010.04.26
*/
public abstract class BaseSocketProviderTests
    {
    protected EchoClient createClient(SocketProvider provider)
        {
        return new EchoClient(provider, getPort());
        }

    protected EchoServer createServer(SocketProvider provider)
        {
        return new EchoServer(provider, getPort());
        }

    protected EchoClient createClient(String sFile)
        {
        return createClient(createSocketProvider(sFile));
        }

    protected EchoServer createServer(String sFile)
        {
        return createServer(createSocketProvider(sFile));
        }

    protected EchoClient createNIOClient(SocketProvider provider)
        {
        return new EchoNIOClient(provider, getPort());
        }

    protected EchoServer createNIOServer(SocketProvider provider)
        {
        return new EchoNIOServer(provider, getPort());
        }

    protected EchoClient createNIOClient(String sFile)
        {
        return createNIOClient(createSocketProvider(sFile));
        }

    protected EchoServer createNIOServer(String sFile)
        {
        return createNIOServer(createSocketProvider(sFile));
        }

    protected SocketProvider createSocketProvider(String sFile)
        {
        XmlDocument xml = null;
        SocketProviderFactory factory = new SocketProviderFactory();
        if (sFile == null)
            {
            return factory.getDemultiplexedSocketProvider(null, null, 1,false);
            }
        if (sFile.length() == 0)
            {
            throw new IllegalArgumentException();
            }

        xml = XmlHelper.loadFileOrResource(sFile, null);
        SocketProviderBuilder bldr = getSocketProviderBuilder(xml);
        return bldr.getDemultiplexedSocketProvider(1);
        }

    protected SocketProviderBuilder getSocketProviderBuilder(String sFile)
        {
        XmlDocument xml = XmlHelper.loadFileOrResource(sFile, null);
        return getSocketProviderBuilder(xml);
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
        return Integer.getInteger("test.extend.port", LocalPlatform.get().getAvailablePorts().next());
        }

    protected int getIterations()
        {
        return 100;
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
