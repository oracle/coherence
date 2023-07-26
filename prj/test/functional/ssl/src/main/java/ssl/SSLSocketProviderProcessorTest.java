/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.oracle.coherence.common.internal.net.WrapperSocket;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;

import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;
import static org.hamcrest.collection.ArrayMatching.hasItemInArray;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.fail;
import static com.oracle.coherence.testing.util.SSLSocketProviderBuilderHelper.loadDependencies;


/**
* Unit test of the SSLSocketProviderProcessor class.
*
* @author jf  2015.11.10
*/
public class SSLSocketProviderProcessorTest
    {
    @Test
    public void testConstructor()
        {
        new SSLSocketProvider();
        }

    @Test
    public void testSimpleClientConfiguration()
            throws IOException
        {
        SSLSocketProviderDefaultDependencies sslDeps = loadDependencies("ssl-config-client.xml");
        SSLSocketProvider                    provider = new SSLSocketProvider(sslDeps);

        SSLContext ctx = sslDeps.getSSLContext();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(nullValue()));

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

    @Test
    public void testSimpleServerConfiguration()
            throws IOException
        {
        SSLSocketProviderDefaultDependencies sslDeps = loadDependencies("ssl-config-server.xml");
        SSLSocketProvider                    provider = new SSLSocketProvider(sslDeps);

        SSLContext ctx = sslDeps.getSSLContext();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(ctx.getProvider(), is(notNullValue()));

        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(nullValue()));
        assertThat(sslDeps.getEnabledCipherSuites(), is(nullValue()));
        assertThat(sslDeps.getEnabledProtocolVersions(), is(nullValue()));
        assertThat(sslDeps.getClientAuth(), is(SSLSocketProvider.ClientAuthMode.required));

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

    @Test
    public void testServerConfigurationUsingBlacklist()
            throws IOException
        {
        SSLSocketProviderDefaultDependencies sslDeps = loadDependencies("ssl-config-server-blacklist.xml");
        SSLContext                           ctx     = sslDeps.getSSLContext();

        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(nullValue()));
        String[] versions = sslDeps.getEnabledProtocolVersions();
        assertThat(versions, is(notNullValue()));
        assertThat(versions, not(hasItemInArray("SSLv3")));

        String[] ciphers = sslDeps.getEnabledCipherSuites();
        assertThat(ciphers, is(notNullValue()));
        assertThat(ciphers.length, is(greaterThan(0)));
        assertThat(ciphers, not(hasItemInArray("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256")));

        SSLSocketProvider provider = new SSLSocketProvider(sslDeps);
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


    @Test
    public void testCustomConfiguration()
            throws IOException
        {
        SSLSocketProviderDefaultDependencies sslDeps  = loadDependencies("ssl-config-custom.xml");
        SSLSocketProvider                    provider = new SSLSocketProvider(sslDeps);

        SSLContext ctx = sslDeps.getSSLContext();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getProtocol(), is(SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL));
        assertThat(sslDeps.getExecutor(), is(notNullValue()));
        assertThat(sslDeps.getHostnameVerifier(), is(notNullValue()));

        HostnameVerifier verifier = sslDeps.getHostnameVerifier();
        assertThat(verifier, instanceOf(CustomHostnameVerifier.class));
        assertThat(((CustomHostnameVerifier) verifier).isAllowed(), is(true));

        assertThat(sslDeps.getEnabledProtocolVersions(), is(arrayContainingInAnyOrder("knockknock", "slowboat", "jet")));

        assertThat(sslDeps.getEnabledCipherSuites(), is(arrayContainingInAnyOrder("twizzlers", "snickers")));

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
