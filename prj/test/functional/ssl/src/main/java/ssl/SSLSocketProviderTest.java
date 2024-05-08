/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;

import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;
import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;

import com.oracle.coherence.common.internal.net.WrapperSocket;

import com.oracle.coherence.common.net.SSLSocketProvider;

import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static com.oracle.coherence.testing.util.SSLSocketProviderBuilderHelper.loadDependencies;


/**
* Unit test of the SSLSocketProvider class.
*
* @author jh  2010.04.26
*/
public class SSLSocketProviderTest
    {
    @Test
    public void testConstructor()
        {
        new SSLSocketProvider();
        }

    @Test
    public void testInvalidConfiguration()
        {
        try
            {
            //noinspection deprecation
            new LegacyXmlSSLSocketProviderDependencies(null);
            fail("Expected IllegalArgumentException");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testSimpleClientConfiguration()
        {
        SSLSocketProviderDefaultDependencies sslDeps = loadDependencies("ssl-config-client.xml");
        SSLContext ctx = sslDeps.getSSLContext();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(nullValue()));
        }

    @Test
    public void testSimpleServerConfiguration()
        {
        SSLSocketProviderDefaultDependencies sslDeps = loadDependencies("ssl-config-server.xml");
        SSLContext ctx = sslDeps.getSSLContext();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(nullValue()));
        }

    @Test
    public void testSimpleServerP12Configuration()
        {
        SSLSocketProviderDefaultDependencies sslDeps = loadDependencies("ssl-config-p12-server.xml");
        SSLContext ctx = sslDeps.getSSLContext();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(nullValue()));
        }

    @Test
    public void testCustomConfiguration()
            throws IOException
        {
        SSLSocketProviderDefaultDependencies sslDeps = loadDependencies("ssl-config-custom.xml");
        SSLSocketProvider provider = new SSLSocketProvider(sslDeps);
        SSLContext ctx = sslDeps.getSSLContext();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(notNullValue()));

        HostnameVerifier verifier = sslDeps.getHostnameVerifier();
        assertThat(verifier, instanceOf(CustomHostnameVerifier.class));
        assertThat(((CustomHostnameVerifier) verifier).isAllowed(), is(true));

        try
            {
            provider.ensureSessionValidity(
                sslDeps.getSSLContext().createSSLEngine().getSession(),
                new WrapperSocket(provider.openSocket())
                    {
                    public InetAddress getInetAddress()
                        {
                        try
                            {
                            return InetAddress.getLocalHost();
                            }
                        catch (UnknownHostException e)
                            {
                            throw new IllegalStateException();
                            }
                        }
                    });
            }
        catch (SSLException sse)    
            {
            fail("SSLException: "+sse);
            }
        }
    }
