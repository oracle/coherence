/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.net.NonBlockingTcpDatagramSocket;
import com.tangosol.net.TcpDatagramSocket;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Live TLS tests for the numeric identity of TCP datagram destinations.
 * CA-signed certificates exercise identity rejection after trust succeeds.
 *
 * @author phf  2026.10.09
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class SSLDatagramIdentityTests
    {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> transports()
        {
        return Arrays.stream(Transport.values()).map(t -> new Object[] {t}).toList();
        }

    public SSLDatagramIdentityTests(Transport transport)
        {
        f_transport = transport;
        }

    @BeforeClass
    public static void createCertificates() throws Exception
        {
        f_loopback = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        Path target = Path.of(System.getProperty("project.build.directory", "target"));
        Files.createDirectories(target);
        f_directory = Files.createTempDirectory(target, "tls-datagram-");
        keytool("-genkeypair", "-alias", "ca", "-keyalg", "EC", "-dname", "CN=TLS datagram test CA",
                "-ext", "BC=ca:true", "-ext", "KU=keyCertSign", "-validity", "30", "-keystore", "ca.p12");
        keytool("-genkeypair", "-alias", "leaf", "-keyalg", "EC", "-dname", "CN=127.0.0.1",
                "-validity", "30", "-keystore", "leaf.p12");
        keytool("-certreq", "-alias", "leaf", "-keystore", "leaf.p12", "-file", "leaf.csr");
        KeyStore ca = loadStore("ca.p12");
        f_ca = ca.getCertificate("ca");
        f_key = (PrivateKey) loadStore("leaf.p12").getKey("leaf", PASSWORD);
        for (String[] fixture : new String[][] {
                {"ip", "IP:127.0.0.1"}, {"wrong-ip", "IP:127.0.0.2"},
                {"numeric-dns", "DNS:127.0.0.1"}, {"cached-dns", "DNS:cached.example"},
                {"cn", ""}, {"ipv6", "IP:::1"}, {"wrong-ipv6", "IP:::2"}})
            {
            List<String> args = new ArrayList<>(List.of("-gencert", "-alias", "ca", "-keystore", "ca.p12",
                    "-infile", "leaf.csr", "-outfile", fixture[0] + ".cer", "-validity", "30",
                    "-ext", "KU=digitalSignature", "-ext", "EKU=serverAuth,clientAuth"));
            if (!fixture[1].isEmpty())
                {
                args.addAll(List.of("-ext", "SAN=" + fixture[1]));
                }
            keytool(args.toArray(String[]::new));
            try (var input = Files.newInputStream(f_directory.resolve(fixture[0] + ".cer")))
                {
                f_certificates.put(fixture[0], CertificateFactory.getInstance("X.509").generateCertificate(input));
                }
            }
        }

    @Test(timeout = 20000)
    public void shouldAcceptCachedHostnameAndPreserveFraming() throws Exception
        {
        exchange("ip", cachedAddress(f_loopback), true, 0);
        }

    @Test(timeout = 20000)
    public void shouldAcceptNumericAddressAndPreserveFraming() throws Exception
        {
        exchange("ip", f_loopback, true, 0);
        }

    @Test(timeout = 20000)
    public void shouldAcceptCachedIPv6Hostname() throws Exception
        {
        assumeFalse(Boolean.getBoolean("java.net.preferIPv4Stack"));
        exchange("ipv6", cachedAddress(InetAddress.getByName("::1")), true, 0);
        }

    @Test(timeout = 20000)
    public void shouldRejectWrongIPSAN() throws Exception
        {
        exchange("wrong-ip", cachedAddress(f_loopback), false, 0);
        }

    @Test(timeout = 20000)
    public void shouldRejectNumericDNSSAN() throws Exception
        {
        exchange("numeric-dns", cachedAddress(f_loopback), false, 0);
        }

    @Test(timeout = 20000)
    public void shouldRejectMatchingCachedDNSSAN() throws Exception
        {
        exchange("cached-dns", cachedAddress(f_loopback), false, 0);
        }

    @Test(timeout = 20000)
    public void shouldRejectMatchingCachedDNSSANBeforeServerCompletes() throws Exception
        {
        exchange("cached-dns", cachedAddress(f_loopback), false, 0, true);
        }

    @Test(timeout = 20000)
    public void shouldRejectCNWithoutIPSAN() throws Exception
        {
        exchange("cn", cachedAddress(f_loopback), false, 0);
        }

    @Test(timeout = 20000)
    public void shouldRejectWrongIPv6SAN() throws Exception
        {
        assumeFalse(Boolean.getBoolean("java.net.preferIPv4Stack"));
        exchange("wrong-ipv6", cachedAddress(InetAddress.getByName("::1")), false, 0);
        }

    @Test(timeout = 20000)
    public void shouldRetainTwoArgumentPolicy() throws Exception
        {
        exchange("ip", cachedAddress(f_loopback), false, 2);
        }

    @Test(timeout = 20000)
    public void shouldRetainThreeArgumentPolicy() throws Exception
        {
        exchange("ip", cachedAddress(f_loopback), false, 3);
        }

    @Test(timeout = 20000)
    public void shouldExchangeDatagramsInBothDirections() throws Exception
        {
        try (var mode = CoherenceModeHelper.securityMode(null);
             var executor = Executors.newFixedThreadPool(2))
            {
            assertTrue(CoherenceMode.isSecurityHardeningEnabled());
            AtomicReference<Verification> firstVerification = new AtomicReference<>();
            AtomicReference<Verification> secondVerification = new AtomicReference<>();
            try (TcpDatagramSocket first = socket(provider(context("ip"), firstVerification, new AtomicBoolean(), 0));
                 TcpDatagramSocket second = socket(provider(context("ip"), secondVerification, new AtomicBoolean(), 0)))
                {
                first.bind(new InetSocketAddress(f_loopback, 0));
                second.bind(new InetSocketAddress(f_loopback, 0));
                first.setSoTimeout(100);
                second.setSoTimeout(100);
                var received = executor.submit(() -> receive(second));
                sendUntilDone(first, packet(cachedAddress(f_loopback), second.getLocalPort()), received);
                DatagramPacket request = received.get(7, TimeUnit.SECONDS);
                assertArrayEquals(PAYLOAD, Arrays.copyOfRange(request.getData(), request.getOffset(), request.getOffset() + request.getLength()));
                assertEquals(first.getLocalPort(), request.getPort());
                var echoed = executor.submit(() -> receive(first));
                sendUntilDone(second, packet(cachedAddress(f_loopback), request.getPort()), echoed);
                DatagramPacket response = echoed.get(7, TimeUnit.SECONDS);
                assertArrayEquals(PAYLOAD, Arrays.copyOfRange(response.getData(), response.getOffset(), response.getOffset() + response.getLength()));
                assertEquals(second.getLocalPort(), response.getPort());
                assertVerification(firstVerification.get(), "ip", f_loopback, true);
                assertVerification(secondVerification.get(), "ip", f_loopback, true);
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    private void exchange(String sCert, InetAddress destination, boolean fAccept, int nPolicy) throws Exception
        {
        exchange(sCert, destination, fAccept, nPolicy, false);
        }

    private void exchange(String sCert, InetAddress destination, boolean fAccept, int nPolicy,
            boolean fWaitForSender) throws Exception
        {
        InetAddress bindAddress = InetAddress.getByAddress(destination.getAddress());
        String sClientCert = destination.getAddress().length == 16 ? "ipv6" : "ip";
        AtomicReference<Verification> verification = new AtomicReference<>();
        AtomicBoolean policy = new AtomicBoolean();
        CountDownLatch senderDone = new CountDownLatch(fWaitForSender ? 1 : 0);
        try (var mode = CoherenceModeHelper.securityMode(null);
             SSLServerSocket server = (SSLServerSocket) context(sCert).getServerSocketFactory()
                     .createServerSocket(0, 1, bindAddress);
             var executor = Executors.newSingleThreadExecutor();
             TcpDatagramSocket client = socket(provider(context(sClientCert), verification, policy, nPolicy)))
            {
            assertTrue(CoherenceMode.isSecurityHardeningEnabled());
            server.setNeedClientAuth(true);
            server.setSoTimeout(TIMEOUT_MILLIS);
            client.bind(new InetSocketAddress(bindAddress, 0));
            DatagramPacket packet = packet(destination, server.getLocalPort());
            if (!destination.getHostName().equals(destination.getHostAddress()))
                {
                // the general SSL boundary must still reject ambiguous ordinary addresses
                assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(packet.getSocketAddress()));
                }
            var result = executor.submit(() ->
                {
                try (SSLSocket accepted = (SSLSocket) server.accept())
                    {
                    accepted.setSoTimeout(TIMEOUT_MILLIS);
                    accepted.startHandshake();
                    assertEquals(f_certificates.get(sClientCert), accepted.getSession().getPeerCertificates()[0]);
                    assertTrue(((ExtendedSSLSession) accepted.getSession()).getRequestedServerNames().isEmpty());
                    DataInputStream input = new DataInputStream(accepted.getInputStream());
                    if (!fAccept)
                        {
                        assertEquals("identity or policy rejection must precede datagram bytes", -1, input.read());
                        return (byte[]) null;
                        }
                    // verify the existing 16-byte connection header and length prefix
                    assertEquals(0x0DDF00DA, input.readInt());
                    assertEquals(0, input.readInt());
                    assertEquals(0, input.readInt());
                    assertEquals(client.getLocalPort(), input.readInt());
                    assertEquals(PAYLOAD.length, input.readInt());
                    return input.readNBytes(PAYLOAD.length);
                    }
                catch (IOException e)
                    {
                    if (fAccept)
                        {
                        throw e;
                        }
                    // rejection may reset TLS before the server finishes its handshake
                    return null;
                    }
                finally
                    {
                    // hold completion until sending stops to expose retries after rejection
                    assertTrue("sender must finish before the server task completes",
                            senderDone.await(7, TimeUnit.SECONDS));
                    }
                });
            try
                {
                sendUntilDone(client, packet, result, () ->
                    {
                    Verification observed = verification.get();
                    return observed != null && (!observed.accepted || policy.get());
                    });
                }
            finally
                {
                senderDone.countDown();
                }
            byte[] received = result.get(7, TimeUnit.SECONDS);
            if (fAccept)
                {
                assertArrayEquals(PAYLOAD, received);
                }
            assertVerification(verification.get(), sCert, destination, fAccept || nPolicy != 0);
            assertEquals(nPolicy != 0, policy.get());
            }
        }

    private static void sendUntilDone(TcpDatagramSocket socket, DatagramPacket packet,
            java.util.concurrent.Future<?> result) throws Exception
        {
        sendUntilDone(socket, packet, result, () -> false);
        }

    private static void sendUntilDone(TcpDatagramSocket socket, DatagramPacket packet,
            java.util.concurrent.Future<?> result, BooleanSupplier rejected) throws Exception
        {
        // include blocking connect and TLS negotiation in the send deadline
        try (var timeout = Timeout.after(6, TimeUnit.SECONDS))
            {
            while (!result.isDone() && !rejected.getAsBoolean())
                {
                socket.send(packet);
                if (!(socket instanceof NonBlockingTcpDatagramSocket))
                    {
                    // a blocking send already completes the handshake and verification
                    return;
                    }
                Blocking.sleep(20);
                }
            }
        }

    private static DatagramPacket receive(TcpDatagramSocket socket) throws IOException
        {
        long lDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7);
        while (true)
            {
            DatagramPacket packet = new DatagramPacket(new byte[64], 64);
            try
                {
                socket.receive(packet);
                return packet;
                }
            catch (SocketTimeoutException e)
                {
                if (System.nanoTime() >= lDeadline)
                    {
                    throw e;
                    }
                }
            }
        }

    private static DatagramPacket packet(InetAddress address, int nPort)
        {
        return new DatagramPacket(PAYLOAD.clone(), PAYLOAD.length, address, nPort);
        }

    private static InetAddress cachedAddress(InetAddress address) throws Exception
        {
        return InetAddress.getByAddress("cached.example", address.getAddress());
        }

    private TcpDatagramSocket socket(SSLSocketProvider provider) throws Exception
        {
        return f_transport == Transport.BLOCKING
                ? new TcpDatagramSocket(provider) : new NonBlockingTcpDatagramSocket(provider);
        }

    private static SSLSocketProvider provider(SSLContext context, AtomicReference<Verification> result,
            AtomicBoolean policy, int nPolicy)
        {
        var verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier();
        var deps = new SSLSocketProvider.DefaultDependencies().setSSLContext(context)
                .setDelegate(new TcpSocketProvider()
                    {
                    @Override
                    public Socket openSocket() throws IOException
                        {
                        Socket socket = super.openSocket();
                        socket.setSoTimeout(TIMEOUT_MILLIS);
                        return socket;
                        }
                    })
                .setClientAuth(SSLSocketProvider.ClientAuthMode.required)
                .setHostnameVerifier((sHost, session) ->
                    {
                    boolean fValid = verifier.verify(sHost, session);
                    try
                        {
                        result.set(new Verification(sHost, session.isValid(), session.getPeerCertificates()[0], fValid));
                        }
                    catch (SSLException e)
                        {
                        throw new AssertionError(e);
                        }
                    return fValid;
                    });
        return switch (nPolicy)
            {
            case 2 -> new SSLSocketProvider(deps)
                {
                @Override
                public void ensureSessionValidity(SSLSession session, Socket socket) throws SSLException
                    {
                    super.ensureSessionValidity(session, socket);
                    policy.set(true);
                    throw new SSLException("legacy two-argument datagram policy");
                    }
                };
            case 3 -> new SSLSocketProvider(deps)
                {
                @Override
                public void ensureSessionValidity(SSLSession session, Socket socket, boolean fClientMode) throws SSLException
                    {
                    super.ensureSessionValidity(session, socket, fClientMode);
                    policy.set(true);
                    throw new SSLException("legacy three-argument datagram policy");
                    }
                };
            default -> new SSLSocketProvider(deps);
            };
        }

    private static void assertVerification(Verification result, String sCert, InetAddress address, boolean fAccept)
        {
        assertNotNull("validation must reach a trusted TLS session", result);
        assertEquals(address.getHostAddress(), result.peer);
        assertTrue("trust must succeed before identity validation", result.validSession);
        assertEquals(f_certificates.get(sCert), result.certificate);
        assertEquals(fAccept, result.accepted);
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
        try (var input = Files.newInputStream(f_directory.resolve(sName)))
            {
            store.load(input, PASSWORD);
            }
        return store;
        }

    private static void keytool(String... args) throws Exception
        {
        String sExecutable = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", sExecutable).toString());
        command.addAll(List.of(args));
        command.addAll(List.of("-storepass", "password", "-keypass", "password", "-storetype", "PKCS12", "-noprompt"));
        Path log = Files.createTempFile(f_directory, "keytool-", ".log");
        Process process = new ProcessBuilder(command).directory(f_directory.toFile()).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS))
            {
            process.destroyForcibly();
            throw new IOException("keytool timed out: " + Files.readString(log));
            }
        assertEquals(Files.readString(log), 0, process.exitValue());
        }

    private enum Transport {BLOCKING, NIO}

    private record Verification(String peer, boolean validSession, Certificate certificate, boolean accepted) {}

    private final Transport f_transport;
    private static final int TIMEOUT_MILLIS = 5000;
    private static final byte[] PAYLOAD = {1, 23, 45, 67, 89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
    private static final char[] PASSWORD = "password".toCharArray();
    private static final Map<String, Certificate> f_certificates = new HashMap<>();
    private static InetAddress f_loopback;
    private static Path f_directory;
    private static Certificate f_ca;
    private static PrivateKey f_key;
    }
