/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http.config;

import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.internal.net.service.peer.acceptor.DefaultHttpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.LegacyXmlHttpAcceptorHelper;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
public class LegacyXmlHttpAcceptorHelperTests
    {
    @Test
    public void shouldUseDiscoveredHttpServer()
        {
        String      sXmlFile = "default-http-acceptor.xml";
        XmlDocument xml      = XmlHelper.loadFileOrResource(sXmlFile, "test");

        DefaultHttpAcceptorDependencies dependencies = getDependencies(xml);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getHttpServer(), is(instanceOf(HttpServerStub.class)));
        }

    private DefaultHttpAcceptorDependencies getDependencies(XmlElement xml)
        {
        DefaultHttpAcceptorDependencies defaults = new DefaultHttpAcceptorDependencies();
        ClassLoader                     loader   = Base.getContextClassLoader();
        OperationalContext              ctx      = mock(OperationalContext.class);
        SocketProviderFactory           factory  = mock(SocketProviderFactory.class);
        SocketProvider                  provider = mock(SocketProvider.class);

        when(ctx.getSocketProviderFactory()).thenReturn(factory);
        when(factory.getSocketProvider(anyString())).thenReturn(provider);

        return LegacyXmlHttpAcceptorHelper.fromXml(xml, defaults, ctx, loader);
        }
    }
