/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tcmp;


import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.util.Resources;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Production TLS identity checks using the TCMP certificate fixtures.
 *
 * @author phf  2026.10.09
 */
public class SecuredProductionHostnameVerificationTests
    {
    // ----- tests ----------------------------------------------------------

    /**
     * A trusted localhost CN without an IP SAN must not identify 127.0.0.1.
     */
    @Test(timeout = 20000)
    public void testHostNameVerification() throws Exception
        {
        assertConnection("localhost.jks", false);
        }

    /**
     * The same numeric reference must accept a trusted matching IP SAN.
     */
    @Test(timeout = 20000)
    public void testMatchingIPSubjectAlternativeName() throws Exception
        {
        assertConnection("keystore.jks", true);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Check identity validation after a trusted, mutually authenticated TLS
     * handshake. Cluster startup retries transport failures internally, so a
     * direct connection exposes the rejection instead of a generic timeout.
     */
    private void assertConnection(String sKeyStore, boolean fAccept) throws Exception
        {
        KeyStore store = KeyStore.getInstance("JKS");
        try (var input = Resources.findFileOrResource(sKeyStore, getClass().getClassLoader()).openStream())
            {
            store.load(input, PASSWORD);
            }

        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, PASSWORD);
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), trust.getTrustManagers(), null);

        InetAddress                 loopback = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        AtomicReference<SSLSession> session  = new AtomicReference<>();
        AtomicReference<String>     hostname = new AtomicReference<>();
        var verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier();

        try (var mode = CoherenceModeHelper.prod();
             var security = CoherenceModeHelper.securityHardened();
             SSLServerSocket server = (SSLServerSocket) context.getServerSocketFactory()
                     .createServerSocket(0, 1, loopback);
             var executor = new TestExecutor(1))
            {
            server.setNeedClientAuth(true);
            server.setSoTimeout(TIMEOUT_MILLIS);
            var result = executor.submit(() ->
                {
                try (SSLSocket accepted = (SSLSocket) server.accept())
                    {
                    accepted.setSoTimeout(TIMEOUT_MILLIS);
                    accepted.startHandshake();
                    int n = accepted.getInputStream().read();
                    if (n >= 0)
                        {
                        accepted.getOutputStream().write(n);
                        accepted.getOutputStream().flush();
                        }
                    return (IOException) null;
                    }
                catch (IOException e)
                    {
                    return e;
                    }
                });

            SSLSocketProvider provider = new SSLSocketProvider(new SSLSocketProvider.DefaultDependencies()
                    .setSSLContext(context)
                    .setDelegate(TcpSocketProvider.INSTANCE)
                    .setClientAuth(SSLSocketProvider.ClientAuthMode.required)
                    .setHostnameVerifier((sHost, sslSession) ->
                        {
                        hostname.set(sHost);
                        session.set(sslSession);
                        return verifier.verify(sHost, sslSession);
                        }));

            try (Socket client = provider.openSocket())
                {
                client.setSoTimeout(TIMEOUT_MILLIS);
                InetSocketAddress address = new InetSocketAddress(loopback, server.getLocalPort());
                if (fAccept)
                    {
                    client.connect(address, TIMEOUT_MILLIS);
                    client.getOutputStream().write(73);
                    client.getOutputStream().flush();
                    assertEquals(73, client.getInputStream().read());
                    }
                else
                    {
                    SSLException e = assertThrows(SSLException.class,
                            () -> client.connect(address, TIMEOUT_MILLIS));
                    assertTrue(e.toString(), e.getMessage().contains("Unacceptable peer"));
                    }
                }

            IOException serverError = result.get(7, TimeUnit.SECONDS);
            if (fAccept)
                {
                assertNull(serverError);
                }

            assertEquals("127.0.0.1", hostname.get());
            assertNotNull("identity verification must observe a TLS session", session.get());
            assertTrue("trust validation must succeed before identity rejection", session.get().isValid());
            assertEquals(store.getCertificate("server"), session.get().getPeerCertificates()[0]);
            }
        }

    // ----- constants ------------------------------------------------------

    private static final char[] PASSWORD = "password".toCharArray();

    private static final int TIMEOUT_MILLIS = 5000;
    
    /**
     * Executor resource for Java versions before ExecutorService was AutoCloseable.
     */
    private static class TestExecutor extends java.util.concurrent.ThreadPoolExecutor
            implements AutoCloseable
        {
        private TestExecutor(int threads)
            {
            super(threads, threads, 0L, java.util.concurrent.TimeUnit.MILLISECONDS,
                    new java.util.concurrent.LinkedBlockingQueue<Runnable>());
            }

        @Override
        public void close() throws InterruptedException
            {
            shutdownNow();
            if (!awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS))
                {
                throw new IllegalStateException("TLS test executor did not terminate");
                }
            }
        }

}
