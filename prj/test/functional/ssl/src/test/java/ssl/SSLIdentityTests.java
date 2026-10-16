/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.oracle.coherence.common.net.InetSocketAddressWithHostname;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.common.internal.net.ssl.SSLSocket;
import com.oracle.coherence.common.internal.net.WrapperSocket;
import com.oracle.coherence.testing.util.CoherenceModeHelper;
import com.tangosol.internal.util.CoherenceMode;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ConnectionPendingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLServerSocket;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Real TLS identity and policy rejection tests using CA-signed certificates.
 * No permissive trust manager or localhost verifier is used.
 *
 * @author phf  2026.10.08
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class SSLIdentityTests
    {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> transports()
        {
        return Arrays.stream(Transport.values()).map(t -> new Object[] {t}).collect(java.util.stream.Collectors.toList());
        }

    public SSLIdentityTests(Transport transport)
        {
        f_transport = transport;
        }

    @BeforeClass
    public static void createCertificates() throws Exception
        {
        f_loopback = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        // warm the cache before constructing numeric addresses used by the tests
        String sCachedName = f_loopback.getHostName();
        Path target = java.nio.file.Paths.get(System.getProperty("project.build.directory", "target"));
        Files.createDirectories(target);
        f_directory = Files.createTempDirectory(target, "tls-identity-");
        keytool("-genkeypair", "-alias", "ca", "-keyalg", "EC", "-dname", "CN=TLS identity test CA",
                "-ext", "BC=ca:true", "-ext", "KU=keyCertSign", "-validity", "30", "-keystore", "ca.p12");
        keytool("-genkeypair", "-alias", "leaf", "-keyalg", "EC", "-dname", "CN=127.0.0.1",
                "-validity", "30", "-keystore", "leaf.p12");
        keytool("-certreq", "-alias", "leaf", "-keystore", "leaf.p12", "-file", "leaf.csr");
        KeyStore ca = loadStore("ca.p12");
        f_ca = ca.getCertificate("ca");
        f_key = (PrivateKey) loadStore("leaf.p12").getKey("leaf", PASSWORD);
        for (String[] fixture : new String[][] {
                {"ip", "IP:127.0.0.1"}, {"wrong-ip", "IP:127.0.0.2"},
                {"numeric-dns", "DNS:127.0.0.1"}, {"abbreviated-dns", "DNS:127.1"},
                {"cn", ""}, {"dns", "DNS:localhost"}, {"cached-dns", "DNS:" + sCachedName},
                {"explicit-cached-dns", "DNS:cached.example"},
                {"mixed", "DNS:other.example,IP:127.0.0.1"}, {"other-dns", "DNS:other.example"},
                {"ipv6", "IP:::1"}, {"wrong-ipv6", "IP:::2"}})
            {
            List<String> args = new ArrayList<>(java.util.Arrays.asList("-gencert", "-alias", "ca", "-keystore", "ca.p12",
                    "-infile", "leaf.csr", "-outfile", fixture[0] + ".cer", "-validity", "30",
                    "-ext", "KU=digitalSignature", "-ext", "EKU=serverAuth,clientAuth"));
            if (!fixture[1].isEmpty())
                {
                args.addAll(java.util.Arrays.asList("-ext", "SAN=" + fixture[1]));
                }
            keytool(args.toArray(new String[0]));
            try (java.io.InputStream input = Files.newInputStream(f_directory.resolve(fixture[0] + ".cer")))
                {
                f_certificates.put(fixture[0], CertificateFactory.getInstance("X.509").generateCertificate(input));
                }
            }
        }

    @Test(timeout = 20000)
    public void shouldAcceptMatchingIP() throws Exception
        {
        assertAccepted(exchange("ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 0, false));
        }

    @Test(timeout = 20000)
    public void shouldAcceptExplicitDNS() throws Exception
        {
        assertAccepted(exchange("dns", null, "localhost", false, SSLSocketProvider.ClientAuthMode.none, 0, false));
        }

    @Test(timeout = 20000)
    public void shouldRejectWrongIPSAN() throws Exception
        {
        assertClientRejected(exchange("wrong-ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 0, false), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldRejectNumericDNSSAN() throws Exception
        {
        assertClientRejected(exchange("numeric-dns", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 0, false), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldRejectNumericCN() throws Exception
        {
        assertClientRejected(exchange("cn", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 0, false), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldRejectAbbreviatedNumericDNSSAN() throws Exception
        {
        assertClientRejected(exchange("abbreviated-dns", null, "127.1", false, SSLSocketProvider.ClientAuthMode.none, 0, false), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldRejectDNSMismatchDespiteMatchingConnectedIP() throws Exception
        {
        assertClientRejected(exchange("mixed", null, "service.example", false, SSLSocketProvider.ClientAuthMode.none, 0, false), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldIgnoreWarmedReverseLookupName() throws Exception
        {
        assertClientRejected(exchange("cached-dns", null, null, true, SSLSocketProvider.ClientAuthMode.none, 0, false),
                f_loopback.getHostName().equals(f_loopback.getHostAddress()) ? "Unacceptible peer" : "Ambiguous peer identity");
        }

    @Test(timeout = 20000)
    public void shouldRejectOrdinaryDNSDespiteMatchingIPSAN() throws Exception
        {
        assertClientRejected(exchange("mixed", null, "localhost", AddressKind.ORDINARY_DNS,
                SSLSocketProvider.ClientAuthMode.none, 0, false), "Ambiguous peer identity");
        }

    @Test(timeout = 20000)
    public void shouldRejectConvertedOrdinaryDNSDespiteMatchingIPSAN() throws Exception
        {
        assertClientRejected(exchange("mixed", null, "localhost", AddressKind.CONVERTED_DNS,
                SSLSocketProvider.ClientAuthMode.none, 0, false), "Ambiguous peer identity");
        }

    @Test(timeout = 20000)
    public void shouldRetainDNSAfterDefaultPortSubstitution() throws Exception
        {
        assertClientRejected(exchange("mixed", null, "service.example", AddressKind.PORT_SUBSTITUTED,
                SSLSocketProvider.ClientAuthMode.none, 0, false), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldRetainDNSAfterSocketPortSubstitution() throws Exception
        {
        assertClientRejected(exchange("mixed", null, "service.example", AddressKind.SOCKET_PORT_SUBSTITUTED,
                SSLSocketProvider.ClientAuthMode.none, 0, false), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldRejectWrappingAnotherSocketWithRetainedIdentity() throws Exception
        {
        try (CoherenceModeHelper.ModeScope mode = CoherenceModeHelper.securityHardened())
            {
            SSLSocketProvider clientProvider = provider(context(null), SSLSocketProvider.ClientAuthMode.none, 0, false);
            try (SSLServerSocket first = (SSLServerSocket) context("dns").getServerSocketFactory()
                        .createServerSocket(0, 1, InetAddress.getByAddress(f_loopback.getAddress()));
                 SSLServerSocket second = (SSLServerSocket) context(f_transport == Transport.ADAPTER ? "other-dns" : "dns")
                        .getServerSocketFactory().createServerSocket(0, 1, InetAddress.getByAddress(f_loopback.getAddress()));
                 TestExecutor executor = new TestExecutor(2);
                 Socket other = new Socket())
                {
                first.setSoTimeout(5000);
                second.setSoTimeout(5000);
                java.util.concurrent.Future<?> firstResult = executor.submit(() -> consumeHandshake(first));
                java.util.concurrent.Future<?> secondResult = executor.submit(() -> consumeHandshake(second));
                other.connect(new InetSocketAddress(InetAddress.getByAddress(f_loopback.getAddress()), second.getLocalPort()), 5000);
                other.setSoTimeout(5000);
                AtomicReference<IOException> rejection = new AtomicReference<>();
                try (SSLSocket client = new SSLSocket(new Socket(), clientProvider)
                    {
                    @Override
                    public javax.net.ssl.SSLSocket wrapSocket(Socket delegate, boolean fClientMode) throws IOException
                        {
                        // exercise another delegate while the original hook's context is active
                        if (f_transport == Transport.NIO && fClientMode)
                            {
                            try (javax.net.ssl.SSLSocket unexpected = super.wrapSocket(other, true))
                                {
                                throw new AssertionError("another socket authenticated with the retained name");
                                }
                            catch (IOException e)
                                {
                                rejection.set(e);
                                }
                            }
                        return super.wrapSocket(delegate, fClientMode);
                        }
                    })
                    {
                    String expected = f_transport == Transport.ADAPTER ? "other.example" : "localhost";
                    SocketAddress destination = new InetSocketAddressWithHostname(expected,
                            InetAddress.getByAddress(f_loopback.getAddress()), first.getLocalPort());
                    if (f_transport == Transport.ADAPTER)
                        {
                        assertThrows(SSLException.class, () -> client.connect(destination, 5000));
                        }
                    else
                        {
                        client.connect(destination, 5000);
                        }
                    if (f_transport != Transport.NIO)
                        {
                        rejection.set(assertThrows(SSLException.class, () -> client.wrapSocket(other, true)));
                        }
                    assertNotNull(rejection.get());
                    assertTrue(rejection.get().toString(), contains(rejection.get(), "Unacceptible peer"));
                    }
                firstResult.get(7, TimeUnit.SECONDS);
                secondResult.get(7, TimeUnit.SECONDS);
                }
            }
        }

    private static IOException consumeHandshake(SSLServerSocket server)
        {
        try (javax.net.ssl.SSLSocket accepted = (javax.net.ssl.SSLSocket) server.accept())
            {
            accepted.setSoTimeout(5000);
            accepted.startHandshake();
            assertEquals("no application data may pass these wrapping probes", -1, accepted.getInputStream().read());
            return null;
            }
        catch (IOException e)
            {
            return e;
            }
        }

    @Test(timeout = 20000)
    public void shouldRetainTwoArgumentValidationPolicy() throws Exception
        {
        assertClientRejected(exchange("ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 2, false), "legacy two-argument policy");
        if (f_transport == Transport.BLOCKING)
            {
            assertClientRejected(exchange("ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 4, false), "legacy wrapping policy");
            }
        }

    @Test(timeout = 20000)
    public void shouldRetainThreeArgumentValidationPolicy() throws Exception
        {
        assertClientRejected(exchange("ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 3, false), "legacy three-argument policy");
        }

    @Test(timeout = 20000)
    public void shouldRequireClientCertificate() throws Exception
        {
        Outcome result = exchange("ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.required, 0, false);
        assertNotNull("required client authentication must fail", result.server);
        assertNotNull("client must observe rejection", result.client);
        }

    @Test(timeout = 20000)
    public void shouldAcceptRequiredMatchingClientCertificate() throws Exception
        {
        assertAccepted(exchange("ip", "ip", "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.required, 0, false));
        }

    @Test(timeout = 20000)
    public void shouldAcceptDirectServerValidationWithCachedName() throws Exception
        {
        assertAccepted(exchangeDirectServerValidation("ip", 0));
        }

    @Test(timeout = 20000)
    public void shouldRejectWrongIPInDirectServerValidation() throws Exception
        {
        assertServerRejected(exchangeDirectServerValidation("wrong-ip", 0));
        }

    @Test(timeout = 20000)
    public void shouldRejectCachedDNSInDirectServerValidation() throws Exception
        {
        assertServerRejected(exchangeDirectServerValidation("explicit-cached-dns", 0));
        }

    @Test(timeout = 20000)
    public void shouldRetainLegacyPolicyInDirectServerValidation() throws Exception
        {
        Outcome result = exchangeDirectServerValidation("ip", 2);
        assertNotNull(result.server);
        assertTrue(result.server.toString(), contains(result.server, "legacy two-argument policy"));
        assertNotNull(result.client);
        assertEquals(-1, result.echo);
        }

    @Test(timeout = 20000)
    public void shouldRejectRequiredWrongClientIdentity() throws Exception
        {
        assertServerRejected(exchange("ip", "wrong-ip", "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.required, 0, false));
        }

    @Test(timeout = 20000)
    public void shouldAllowWantedAnonymousClient() throws Exception
        {
        assertAccepted(exchange("ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.wanted, 0, false));
        }

    @Test(timeout = 20000)
    public void shouldRejectWantedWrongClientIdentity() throws Exception
        {
        assertServerRejected(exchange("ip", "wrong-ip", "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.wanted, 0, false));
        }

    @Test(timeout = 20000)
    public void shouldNotExemptExplicitServerVerifier() throws Exception
        {
        assertServerRejected(exchange("ip", null, "127.0.0.1", false, SSLSocketProvider.ClientAuthMode.none, 0, true));
        }

    @Test(timeout = 20000)
    public void shouldAcceptLiveIPv6Identity() throws Exception
        {
        org.junit.Assume.assumeFalse(Boolean.getBoolean("java.net.preferIPv4Stack"));
        assertAccepted(exchange("ipv6", null, "::1", AddressKind.RETAINED,
                SSLSocketProvider.ClientAuthMode.none, 0, false, InetAddress.getByName("::1")));
        }

    @Test(timeout = 20000)
    public void shouldRejectWrongLiveIPv6Identity() throws Exception
        {
        org.junit.Assume.assumeFalse(Boolean.getBoolean("java.net.preferIPv4Stack"));
        assertClientRejected(exchange("wrong-ipv6", null, "::1", AddressKind.RETAINED,
                SSLSocketProvider.ClientAuthMode.none, 0, false, InetAddress.getByName("::1")), "Unacceptible peer");
        }

    @Test(timeout = 20000)
    public void shouldRejectIncompleteHandshake() throws Exception
        {
        try (CoherenceModeHelper.ModeScope mode = CoherenceModeHelper.securityHardened();
             ServerSocket server = new ServerSocket(0, 1, InetAddress.getByAddress(f_loopback.getAddress()));
             TestExecutor executor = new TestExecutor(1))
            {
            server.setSoTimeout(5000);
            java.util.concurrent.Future<?> result = executor.submit(() ->
                {
                try (Socket accepted = server.accept())
                    {
                    accepted.setSoTimeout(5000);
                    assertEquals("client must start TLS before application data", 22, accepted.getInputStream().read());
                    }
                return null;
                });
            Outcome client = exchangeClient(provider(context(null), SSLSocketProvider.ClientAuthMode.none, 0, false),
                    new InetSocketAddressWithHostname("127.0.0.1", InetAddress.getByAddress(f_loopback.getAddress()), server.getLocalPort()));
            assertNotNull("incomplete TLS cannot authenticate the peer", client.client);
            assertEquals(-1, client.echo);
            result.get(7, TimeUnit.SECONDS);
            }
        }

    @Test(timeout = 20000)
    public void shouldAcceptNioServerAnonymousClient() throws Exception
        {
        assertAccepted(exchangeNioServer(null, SSLSocketProvider.ClientAuthMode.none, false));
        }

    @Test(timeout = 20000)
    public void shouldRequireNioServerClientCertificate() throws Exception
        {
        Outcome result = exchangeNioServer(null, SSLSocketProvider.ClientAuthMode.required, false);
        assertNotNull(result.server);
        assertNotNull(result.client);
        }

    @Test(timeout = 20000)
    public void shouldAcceptNioServerMatchingRequiredClient() throws Exception
        {
        assertAccepted(exchangeNioServer("ip", SSLSocketProvider.ClientAuthMode.required, false));
        }

    @Test(timeout = 20000)
    public void shouldRejectNioServerWrongRequiredClient() throws Exception
        {
        assertServerRejected(exchangeNioServer("wrong-ip", SSLSocketProvider.ClientAuthMode.required, false));
        }

    @Test(timeout = 20000)
    public void shouldAcceptNioServerWantedAnonymousClient() throws Exception
        {
        assertAccepted(exchangeNioServer(null, SSLSocketProvider.ClientAuthMode.wanted, false));
        }

    @Test(timeout = 20000)
    public void shouldRejectNioServerWrongWantedClient() throws Exception
        {
        assertServerRejected(exchangeNioServer("wrong-ip", SSLSocketProvider.ClientAuthMode.wanted, false));
        }

    @Test(timeout = 20000)
    public void shouldNotExemptNioServerExplicitVerifier() throws Exception
        {
        assertServerRejected(exchangeNioServer(null, SSLSocketProvider.ClientAuthMode.none, true));
        }

    private Outcome exchangeDirectServerValidation(String clientCertificate, int policy) throws Exception
        {
        try (CoherenceModeHelper.ModeScope mode = CoherenceModeHelper.securityHardened())
            {
            assertTrue(CoherenceMode.isSecurityHardeningEnabled());
            SSLSocketProvider serverProvider = provider(context("ip"), SSLSocketProvider.ClientAuthMode.required, policy, false);
            try (SSLServerSocket server = (SSLServerSocket) context("ip").getServerSocketFactory()
                        .createServerSocket(0, 1, InetAddress.getByAddress(f_loopback.getAddress()));
                 TestExecutor executor = new TestExecutor(1))
                {
                server.setNeedClientAuth(true);
                server.setSoTimeout(5000);
                java.util.concurrent.Future<IOException> serverResult = executor.submit(() ->
                    {
                    try (javax.net.ssl.SSLSocket accepted = (javax.net.ssl.SSLSocket) server.accept())
                        {
                        accepted.setSoTimeout(5000);
                        accepted.startHandshake();
                        // retain the real peer bytes with a deterministic cached reverse name
                        InetAddress cached = InetAddress.getByAddress("cached.example", accepted.getInetAddress().getAddress());
                        Socket peer = new WrapperSocket(accepted)
                            {
                            @Override
                            public InetAddress getInetAddress() { return cached; }

                            @Override
                            public SocketAddress getRemoteSocketAddress()
                                {
                                return new InetSocketAddress(cached, accepted.getPort());
                                }
                            };
                        serverProvider.ensureSessionValidity(accepted.getSession(), peer, false);
                        int value = accepted.getInputStream().read();
                        if (value != 73)
                            {
                            throw new IOException("client did not send the expected application byte");
                            }
                        accepted.getOutputStream().write(value);
                        accepted.getOutputStream().flush();
                        return (IOException) null;
                        }
                    catch (IOException e)
                        {
                        return e;
                        }
                    });
                Outcome client = exchangeClient(provider(context(clientCertificate), SSLSocketProvider.ClientAuthMode.none, 0, false),
                        new InetSocketAddressWithHostname("127.0.0.1", InetAddress.getByAddress(f_loopback.getAddress()), server.getLocalPort()));
                if (client.client != null)
                    {
                    server.close();
                    }
                return new Outcome(client.client, serverResult.get(7, TimeUnit.SECONDS), client.echo);
                }
            }
        }

    private Outcome exchangeNioServer(String clientCertificate, SSLSocketProvider.ClientAuthMode auth,
            boolean rejectVerifier) throws Exception
        {
        try (CoherenceModeHelper.ModeScope mode = CoherenceModeHelper.securityHardened())
            {
            SSLSocketProvider serverProvider = provider(context("ip"), auth, 0, rejectVerifier);
            try (ServerSocketChannel server = serverProvider.openServerSocketChannel();
                 TestExecutor executor = new TestExecutor(1))
                {
                server.configureBlocking(false);
                server.bind(new InetSocketAddress(InetAddress.getByAddress(f_loopback.getAddress()), 0));
                java.util.concurrent.Future<IOException> serverResult = executor.submit(() ->
                    {
                    try (Selector listener = server.provider().openSelector())
                        {
                        server.register(listener, SelectionKey.OP_ACCEPT);
                        long acceptDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                        SocketChannel channel;
                        while ((channel = server.accept()) == null)
                            {
                            select(listener, acceptDeadline);
                            }
                    try (SocketChannel accepted = channel; Selector selector = accepted.provider().openSelector())
                        {
                        accepted.configureBlocking(false);
                        accepted.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                        ByteBuffer input = ByteBuffer.allocate(1);
                        while (input.hasRemaining())
                            {
                            select(selector, deadline);
                            if (accepted.read(input) < 0)
                                {
                                throw new IOException("client closed before application data");
                                }
                            }
                        assertEquals(73, input.get(0));
                        input.flip();
                        while (input.hasRemaining())
                            {
                            select(selector, deadline);
                            accepted.write(input);
                            }
                        return (IOException) null;
                        }
                        }
                    catch (IOException e)
                        {
                        return e;
                        }
                    });
                Outcome client = exchangeClient(provider(context(clientCertificate), SSLSocketProvider.ClientAuthMode.none, 0, false),
                        new InetSocketAddressWithHostname("127.0.0.1", InetAddress.getByAddress(f_loopback.getAddress()),
                                ((InetSocketAddress) server.getLocalAddress()).getPort()));
                if (client.client != null)
                    {
                    server.close();
                    }
                return new Outcome(client.client, serverResult.get(7, TimeUnit.SECONDS), client.echo);
                }
            }
        }

    private Outcome exchange(String sServerCert, String sClientCert, String sPeer, boolean fNumericAddress,
            SSLSocketProvider.ClientAuthMode auth, int nPolicy, boolean fRejectServer) throws Exception
        {
        return exchange(sServerCert, sClientCert, sPeer, fNumericAddress ? AddressKind.NUMERIC : AddressKind.RETAINED,
                auth, nPolicy, fRejectServer);
        }

    private Outcome exchange(String sServerCert, String sClientCert, String sPeer, AddressKind kind,
            SSLSocketProvider.ClientAuthMode auth, int nPolicy, boolean fRejectServer) throws Exception
        {
        return exchange(sServerCert, sClientCert, sPeer, kind, auth, nPolicy, fRejectServer, f_loopback);
        }

    private Outcome exchange(String sServerCert, String sClientCert, String sPeer, AddressKind kind,
            SSLSocketProvider.ClientAuthMode auth, int nPolicy, boolean fRejectServer, InetAddress bindAddress) throws Exception
        {
        try (CoherenceModeHelper.ModeScope mode = CoherenceModeHelper.securityHardened())
            {
            assertTrue(CoherenceMode.isSecurityHardeningEnabled());
            SSLSocketProvider serverProvider = provider(context(sServerCert), auth, 0, fRejectServer);
            SSLSocketProvider clientProvider = provider(context(sClientCert), SSLSocketProvider.ClientAuthMode.none, nPolicy, false);
            try (ServerSocket server = serverProvider.openServerSocket(); TestExecutor executor = new TestExecutor(1))
                {
                server.bind(new InetSocketAddress(bindAddress, 0));
                server.setSoTimeout(5000);
                java.util.concurrent.Future<IOException> serverResult = executor.submit(() ->
                    {
                    try (Socket accepted = server.accept())
                        {
                        int value = accepted.getInputStream().read();
                        if (value != 73)
                            {
                            throw new IOException("client did not send the expected application byte");
                            }
                        accepted.getOutputStream().write(value);
                        accepted.getOutputStream().flush();
                        return (IOException) null;
                        }
                    catch (IOException e)
                        {
                        return e;
                        }
                    });
                int port = server.getLocalPort();
                InetAddress numeric = InetAddress.getByAddress(bindAddress.getAddress());
                SocketAddress destination;
                switch (kind)
                    {
                    case NUMERIC: destination = new InetSocketAddress(f_loopback, port);
                        break;
                    case ORDINARY_DNS: destination = new InetSocketAddress(sPeer, port);
                        break;
                    case CONVERTED_DNS: destination = new com.oracle.coherence.common.net.InetSocketAddress32(new InetSocketAddress(sPeer, port));
                        break;
                    case PORT_SUBSTITUTED: destination = new com.tangosol.net.internal.SubstitutionAddressProvider(
                            new com.tangosol.net.AddressProvider()
                                {
                                public InetSocketAddress getNextAddress() { return new InetSocketAddressWithHostname(sPeer, numeric, 0); }
                                public void accept() {}
                                public void reject(Throwable error) {}
                                }, port).getNextAddress();
                        break;
                    case SOCKET_PORT_SUBSTITUTED: destination = new com.tangosol.net.internal.SubstitutionSocketAddressProvider(
                            new com.tangosol.net.SocketAddressProvider()
                                {
                                public SocketAddress getNextAddress() { return new InetSocketAddressWithHostname(sPeer, numeric, port); }
                                public void accept() {}
                                public void reject(Throwable error) {}
                                }, numeric, port, -1).getNextAddress();
                        break;
                    default: destination = new InetSocketAddressWithHostname(sPeer, bindAddress, port);
                        break;
                    }
                Outcome clientResult = exchangeClient(clientProvider, destination);
                if (clientResult.client != null)
                    {
                    // identity provenance can be rejected before any connection reaches accept
                    server.close();
                    }
                return new Outcome(clientResult.client, serverResult.get(7, TimeUnit.SECONDS), clientResult.echo);
                }
            }
        }

    private Outcome exchangeClient(SSLSocketProvider clientProvider, SocketAddress destination) throws Exception
        {
        IOException clientError = null;
        int nEcho = -1;
        try
            {
            if (f_transport == Transport.BLOCKING)
                {
                try (Socket client = clientProvider.openSocket())
                    {
                    client.setSoTimeout(5000);
                    client.connect(destination, 5000);
                    client.getOutputStream().write(73);
                    client.getOutputStream().flush();
                    nEcho = client.getInputStream().read();
                    if (nEcho < 0)
                        {
                        throw new IOException("peer rejected the connection");
                        }
                    }
                }
            else
                {
                try (SocketChannel client = clientProvider.openSocketChannel();
                     Selector selector = client.provider().openSelector())
                    {
                    long lDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    if (f_transport == Transport.ADAPTER)
                        {
                        client.socket().connect(destination, 5000);
                        client.configureBlocking(false);
                        }
                    else
                        {
                        client.configureBlocking(false);
                        }
                    SelectionKey key = client.register(selector, SelectionKey.OP_CONNECT);
                    if (f_transport != Transport.ADAPTER && !client.connect(destination))
                        {
                        assertThrows(ConnectionPendingException.class, () -> client.connect(
                                new InetSocketAddressWithHostname("replacement.example", f_loopback, com.oracle.coherence.common.net.InetAddresses.getPort(destination))));
                        while (!client.finishConnect())
                            {
                            select(selector, lDeadline);
                            }
                        }
                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    ByteBuffer output = ByteBuffer.wrap(new byte[] {73});
                    while (output.hasRemaining())
                        {
                        select(selector, lDeadline);
                        client.write(output);
                        }
                    ByteBuffer input = ByteBuffer.allocate(1);
                    while (input.hasRemaining())
                        {
                        select(selector, lDeadline);
                        if (client.read(input) < 0)
                            {
                            throw new IOException("peer rejected the connection");
                            }
                        }
                    nEcho = input.get(0);
                    }
                }
            }
        catch (IOException e)
            {
            clientError = e;
            }
        return new Outcome(clientError, null, nEcho);
        }

    private static void select(Selector selector, long lDeadline) throws IOException
        {
        if (System.nanoTime() >= lDeadline)
            {
            throw new IOException("TLS exchange exceeded its deadline");
            }
        selector.select(100);
        selector.selectedKeys().clear();
        }

    private static SSLSocketProvider provider(SSLContext context, SSLSocketProvider.ClientAuthMode auth,
            int nPolicy, boolean fRejectServer)
        {
        SSLSocketProvider.DefaultDependencies deps = new SSLSocketProvider.DefaultDependencies().setSSLContext(context).setClientAuth(auth)
                .setDelegate(new TcpSocketProvider()
                    {
                    @Override
                    public ServerSocket openServerSocket() throws IOException
                        {
                        return new ServerSocket()
                            {
                            @Override
                            public Socket accept() throws IOException
                                {
                                Socket socket = super.accept();
                                socket.setSoTimeout(5000);
                                return socket;
                                }
                            };
                        }
                    });
        if (fRejectServer)
            {
            deps.setHostnameVerifier((sHost, session) -> false);
            }
        switch (nPolicy)
            {
            case 2: return new SSLSocketProvider(deps)
                {
                @Override
                public void ensureSessionValidity(SSLSession session, Socket socket) throws SSLException
                    {
                    throw new SSLException("legacy two-argument policy");
                    }
                };
            case 3: return new SSLSocketProvider(deps)
                {
                @Override
                public void ensureSessionValidity(SSLSession session, Socket socket, boolean fClientMode) throws SSLException
                    {
                    throw new SSLException("legacy three-argument policy");
                    }
                };
            case 4: return new SSLSocketProvider(deps)
                {
                @Override
                public Socket openSocket() throws IOException
                    {
                    return new SSLSocket(new Socket(), this)
                        {
                        @Override
                        public javax.net.ssl.SSLSocket wrapSocket(Socket socket, boolean fClientMode) throws IOException
                            {
                            throw new SSLException("legacy wrapping policy");
                            }
                        };
                    }
                };
            default: return new SSLSocketProvider(deps);
            }
        }

    private static SSLContext context(String sCert) throws Exception
        {
        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, PASSWORD);
        trust.setCertificateEntry("ca", f_ca);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);
        KeyManager[] managers = new KeyManager[0];
        if (sCert != null)
            {
            // prove trust-chain acceptance independently of hostname verification
            ((X509TrustManager) tmf.getTrustManagers()[0]).checkServerTrusted(
                    new X509Certificate[] {(X509Certificate) f_certificates.get(sCert), (X509Certificate) f_ca},
                    "ECDHE_ECDSA");
            KeyStore identity = KeyStore.getInstance("PKCS12");
            identity.load(null, PASSWORD);
            identity.setKeyEntry("leaf", f_key, PASSWORD, new Certificate[] {f_certificates.get(sCert), f_ca});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(identity, PASSWORD);
            managers = kmf.getKeyManagers();
            }
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(managers, tmf.getTrustManagers(), null);
        return context;
        }

    private static KeyStore loadStore(String sName) throws Exception
        {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (java.io.InputStream input = Files.newInputStream(f_directory.resolve(sName)))
            {
            store.load(input, PASSWORD);
            }
        return store;
        }

    private static void keytool(String... args) throws Exception
        {
        String sExecutable = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        List<String> command = new ArrayList<>();
        command.add(java.nio.file.Paths.get(System.getProperty("java.home"), "bin", sExecutable).toString());
        command.addAll(java.util.Arrays.asList(args));
        command.addAll(java.util.Arrays.asList("-storepass", "password", "-keypass", "password", "-storetype", "PKCS12", "-noprompt"));
        Path log = Files.createTempFile(f_directory, "keytool-", ".log");
        Process process = new ProcessBuilder(command).directory(f_directory.toFile()).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS))
            {
            process.destroyForcibly();
            throw new IOException("keytool timed out: " + new String(Files.readAllBytes(log), java.nio.charset.StandardCharsets.UTF_8));
            }
        assertEquals(new String(Files.readAllBytes(log), java.nio.charset.StandardCharsets.UTF_8), 0, process.exitValue());
        }

    private static void assertAccepted(Outcome result)
        {
        assertNull("client failure: " + result.client, result.client);
        assertNull("server failure: " + result.server, result.server);
        assertEquals(73, result.echo);
        }

    private static void assertClientRejected(Outcome result, String sPolicy)
        {
        assertNotNull("client must reject the certificate or policy", result.client);
        assertTrue(result.client.toString(), contains(result.client, sPolicy));
        }

    private static void assertServerRejected(Outcome result)
        {
        assertNotNull("server must reject the client identity", result.server);
        assertTrue(result.server.toString(), contains(result.server, "Unacceptible peer"));
        assertNotNull("client must observe server rejection", result.client);
        }

    private static boolean contains(Throwable error, String sText)
        {
        for (Throwable e = error; e != null; e = e.getCause())
            {
            if (e.getMessage() != null && e.getMessage().contains(sText))
                {
                return true;
                }
            }
        return false;
        }

    private enum AddressKind {RETAINED, NUMERIC, ORDINARY_DNS, CONVERTED_DNS, PORT_SUBSTITUTED, SOCKET_PORT_SUBSTITUTED}

    private enum Transport {BLOCKING, NIO, ADAPTER}

    private static class Outcome
        {
        private final IOException client;
        private final IOException server;
        private final int echo;

        private Outcome(IOException client, IOException server, int echo)
            {
            this.client = client;
            this.server = server;
            this.echo = echo;
            }
        }

    private final Transport f_transport;
    private static final char[] PASSWORD = "password".toCharArray();
    private static final Map<String, Certificate> f_certificates = new HashMap<>();
    private static InetAddress f_loopback;
    private static Path f_directory;
    private static Certificate f_ca;
    private static PrivateKey f_key;
    
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
