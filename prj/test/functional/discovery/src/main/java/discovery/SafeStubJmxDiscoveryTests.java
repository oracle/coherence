/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package discovery;

import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import com.tangosol.coherence.component.net.management.Connector;

import com.tangosol.coherence.component.util.SafeCluster;

import com.tangosol.discovery.NSLookup;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.Member;

import com.tangosol.net.management.MBeanServerFinder;
import com.tangosol.net.management.Registry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import java.lang.management.ManagementFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

import java.rmi.registry.LocateRegistry;

import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Exercise passively validated Coherence-owned encoded connectors in isolated member JVMs.
 *
 * @author phf  2026.11.04
 * @since 27.01
 */
public class SafeStubJmxDiscoveryTests
    {
    @Test
    public void shouldDiscoverDefaultDynamicManagementOnEveryMember()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            TestMember senior = cluster.start();
            TestMember second = cluster.start();
            TestMember third  = cluster.start();
            cluster.awaitSize(3);
            JMXServiceURL url = awaitUrl(second);
            assertTrue(senior.ownsConnector());
            assertFalse(second.ownsConnector());
            assertFalse(third.ownsConnector());
            for (TestMember member : List.of(senior, second, third))
                {
                await("same encoded URL on every member", () -> url.equals(member.lookup()));
                assertDiscoveredMBeans(member, 3);
                }
            assertTrue(url.getURLPath().startsWith("/stub/"));
            assertThrows(IOException.class, () ->
                    LocateRegistry.getRegistry(url.getHost(), url.getPort()).list());
            cluster.assertNoStubRejections();
            }
        }

    @Test
    public void shouldReplaceTheEncodedEndpointOnFailover()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            TestMember senior    = cluster.start();
            TestMember successor = cluster.start();
            TestMember follower  = cluster.start();
            cluster.awaitSize(3);
            JMXServiceURL original = awaitUrl(follower);
            assertDiscoveredMBeans(follower, 3);
            senior.stopCoherence();
            cluster.awaitSize(2);
            await("successor endpoint", () -> follower.lookup() != null && !original.equals(follower.lookup()));
            assertTrue(successor.ownsConnector());
            assertDiscoveredMBeans(follower, 2);
            assertThrows(IOException.class, () -> JMXConnectorFactory.connect(original));
            cluster.assertNoStubRejections();
            }
        }

    @Test
    public void shouldClearDiscoveryWhenTheSuccessorFinderReturnsAnUnsupportedStub()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            TestMember senior = cluster.start();
            TestMember successor = cluster.start(Map.of(
                    "coherence.management.serverfactory", ExistingConnectorFinder.class.getName(),
                    "fixture.finder.url", "service:jmx:rmi:///stub/abcd"));
            TestMember follower = cluster.start();
            cluster.awaitSize(3);
            assertDiscoveredMBeans(follower, 3);
            senior.stopCoherence();
            cluster.awaitSize(2);
            await("local management survives an unsupported finder URL", () -> successor.nodeCount() == 2);
            await("old discovery address cleared on all remaining members", () ->
                    successor.lookup() == null && follower.lookup() == null);
            assertFalse(successor.ownsConnector());
            assertTrue(Files.readString(successor.log).contains("JMX discovery is unavailable"));
            cluster.assertNoStubRejections();
            }
        }

    @Test
    public void shouldUseAnExplicitRegistryPort()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            int port = cluster.port();
            TestMember member = cluster.start(Map.of(
                    "coherence.management.remote.registryport", Integer.toString(port),
                    "coherence.management.remote.connectionport", "0",
                    "com.sun.management.jmxremote.authenticate", "false",
                    "com.sun.management.jmxremote.ssl", "false"));
            assertEquals(port, registryAddress(awaitUrl(member)).getPort());
            assertDiscoveredMBeans(member, 1);
            cluster.assertNoStubRejections();
            }
        }

    @Test
    public void shouldPreserveAnExistingFinderConnectorAndItsLifecycle()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            int registryPort = cluster.port();
            String url = "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + registryPort + "/jmxrmi";
            TestMember member = cluster.start(Map.of(
                    "com.sun.management.jmxremote.port", Integer.toString(registryPort),
                    "com.sun.management.jmxremote.rmi.port", Integer.toString(cluster.port()),
                    "com.sun.management.jmxremote.host", "127.0.0.1",
                    "java.rmi.server.hostname", "127.0.0.1",
                    "com.sun.management.jmxremote.authenticate", "false",
                    "com.sun.management.jmxremote.ssl", "false",
                    "coherence.management.serverfactory", ExistingConnectorFinder.class.getName(),
                    "fixture.finder.url", url));
            assertEquals(new JMXServiceURL(url), awaitUrl(member));
            assertFalse(member.ownsConnector());
            assertDiscoveredMBeans(member, 1);
            member.stopCoherence();
            try (JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url)))
                {
                assertNotNull(connector.getMBeanServerConnection().getDefaultDomain());
                }
            assertEquals(1, LocateRegistry.getRegistry("127.0.0.1", registryPort).list().length);
            cluster.assertNoStubRejections();
            }
        }

    @Test
    public void shouldConnectUsingAnIpv6EncodedEndpoint()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            Path probe = cluster.directory.resolve("ipv6-availability.txt");
            cluster.runClient("ipv6-probe", List.of("-Djava.net.preferIPv4Stack=false"),
                    "ipv6-probe", probe.toString());
            String availability = Files.readString(probe);
            assumeTrue("IPv6 loopback is unavailable: " + availability, "available".equals(availability));

            TestMember member = cluster.start(Map.of("coherence.management.remote.host", "::1",
                    "java.rmi.server.hostname", "::1", "java.net.preferIPv4Stack", "false"));
            JMXServiceURL url = awaitUrl(member);
            assertTrue(url.getURLPath().startsWith("/stub/"));
            cluster.runClient("ipv6", List.of("-Djava.net.preferIPv4Stack=false"), "plain", url.toString(), "1");
            cluster.assertNoStubRejections();
            }
        }

    @Test
    public void shouldConnectUsingAProxyStubAndFailOverToAStaticStub()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            TestMember senior = cluster.start(Map.of("java.rmi.server.ignoreStubClasses", "true"));
            TestMember successor = cluster.start();
            cluster.awaitSize(2);
            JMXServiceURL original = awaitUrl(successor);
            assertDiscoveredMBeans(successor, 2);
            senior.stopCoherence();
            cluster.awaitSize(1);
            await("static successor after proxy senior", () ->
                    successor.lookup() != null && !original.equals(successor.lookup()));
            assertDiscoveredMBeans(successor, 1);
            cluster.assertNoStubRejections();
            }
        }

    @Test
    public void shouldRejectACallbackPayloadThroughActualManagementPublication()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            TestMember senior = cluster.start();
            TestMember follower = cluster.start();
            cluster.awaitSize(2);
            JMXServiceURL original = awaitUrl(follower);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes))
                {
                out.writeObject(new CallbackPayload());
                }
            Files.writeString(senior.directory.resolve("publish-url"),
                    "service:jmx:rmi:///stub/" + Base64.getEncoder().encodeToString(bytes.toByteArray()));
            await("wire publication rejected", () -> Files.readString(follower.log).contains("reason=rmi-stub-url"));
            assertEquals(original, follower.lookup());
            assertEquals("0", follower.state().getProperty("callbacks"));
            assertEquals("0", senior.state().getProperty("callbacks"));
            assertDiscoveredMBeans(follower, 2);
            }
        }

    @Test
    public void shouldRejectUnsupportedValuesAtRawAndStringNameServiceBoundaries()
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            TestMember member = cluster.start();
            JMXServiceURL original = awaitUrl(member);
            InetSocketAddress address = new InetSocketAddress("127.0.0.1",
                    Integer.parseInt(member.state().getProperty("port")));
            for (String name : List.of(NSLookup.JMX_CONNECTOR_URL,
                    NSLookup.NS_STRING_PREFIX + NSLookup.JMX_CONNECTOR_URL))
                {
                assertEquals(original.toString(), NSLookup.lookup(name, address, 5000));
                }
            Files.writeString(member.directory.resolve("override-url"), "service:jmx:rmi:///stub/abcd");
            await("unsupported URL installed behind the discovery boundary", () ->
                    "service:jmx:rmi:///stub/abcd".equals(member.state().getProperty("jmxUrl")));
            for (String name : List.of(NSLookup.JMX_CONNECTOR_URL,
                    NSLookup.NS_STRING_PREFIX + NSLookup.JMX_CONNECTOR_URL))
                {
                RuntimeException error = assertThrows(RuntimeException.class, () -> NSLookup.lookup(name, address, 5000));
                assertTrue(error.getCause() instanceof IOException);
                }
            assertEquals(1, member.nodeCount());
            assertEquals("0", member.state().getProperty("callbacks"));
            }
        }

    @Test
    public void shouldPreserveAuthenticationAndTlsForAnOwnedConnector()
            throws Exception
        {
        assertSecuredStub(false);
        }

    @Test
    public void shouldPreserveAuthenticationAndTlsForAProxyStub()
            throws Exception
        {
        assertSecuredStub(true);
        }

    private void assertSecuredStub(boolean proxy)
            throws Exception
        {
        try (TestCluster cluster = new TestCluster())
            {
            Path store = cluster.directory.resolve("agent.p12");
            Process keytool = new ProcessBuilder(
                    Path.of(System.getProperty("test.jvm.java.home", System.getProperty("java.home")), "bin", "keytool").toString(),
                    "-genkeypair", "-alias", "agent", "-keyalg", "RSA", "-keysize", "2048",
                    "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                    "-validity", "1", "-storetype", "PKCS12", "-keystore", store.toString(),
                    "-storepass", FIXTURE_PASSWORD, "-keypass", FIXTURE_PASSWORD, "-noprompt")
                    .redirectErrorStream(true).redirectOutput(cluster.directory.resolve("keytool.log").toFile()).start();
            try
                {
                assertTrue("keytool did not finish", keytool.waitFor(30, TimeUnit.SECONDS));
                assertEquals("see keytool.log", 0, keytool.exitValue());
                }
            finally
                {
                if (keytool.isAlive())
                    {
                    keytool.destroyForcibly();
                    keytool.waitFor(10, TimeUnit.SECONDS);
                    }
                }

            Path password = cluster.directory.resolve("jmx.password");
            Path access   = cluster.directory.resolve("jmx.access");
            Files.writeString(password, "monitor " + FIXTURE_PASSWORD + "\n");
            Files.writeString(access, "monitor readonly\n");
            for (Path credential : List.of(password, access))
                {
                if (Files.getFileStore(credential).supportsFileAttributeView("posix"))
                    {
                    Files.setPosixFilePermissions(credential, PosixFilePermissions.fromString("rw-------"));
                    }
                else
                    {
                    assumeTrue("Owner-only credential permissions are unavailable on this filesystem",
                            Files.getFileStore(credential).supportsFileAttributeView(AclFileAttributeView.class));
                    AclFileAttributeView acl = Files.getFileAttributeView(credential, AclFileAttributeView.class);
                    assumeNotNull(acl);
                    acl.setAcl(List.of(AclEntry.newBuilder()
                            .setType(AclEntryType.ALLOW)
                            .setPrincipal(acl.getOwner())
                            .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                            .build()));
                    }
                }

            Map<String, String> options = new HashMap<>();
            options.putAll(Map.of(
                    "com.sun.management.jmxremote.password.file", password.toString(),
                    "com.sun.management.jmxremote.access.file", access.toString(),
                    "javax.net.ssl.keyStore", store.toString(),
                    "javax.net.ssl.keyStorePassword", FIXTURE_PASSWORD,
                    "javax.net.ssl.trustStore", store.toString(),
                    "javax.net.ssl.trustStorePassword", FIXTURE_PASSWORD,
                    "coherence.management.remote.connectionport", "0",
                    "coherence.management.remote.host", "127.0.0.1",
                    "java.rmi.server.hostname", "127.0.0.1"));
            if (proxy)
                {
                options.put("java.rmi.server.ignoreStubClasses", "true");
                }
            TestMember member = cluster.start(options);
            assertTrue(awaitUrl(member).getURLPath().startsWith("/stub/"));
            cluster.runClient("secured", List.of("-Djavax.net.ssl.trustStore=" + store,
                            "-Djavax.net.ssl.trustStorePassword=" + FIXTURE_PASSWORD),
                    "secured", awaitUrl(member).toString(), "1", FIXTURE_PASSWORD);
            assertTrue(member.ownsConnector());
            JMXServiceURL url = member.lookup();
            member.stopCoherence();
            cluster.runClient("stopped", List.of("-Djavax.net.ssl.trustStore=" + store,
                            "-Djavax.net.ssl.trustStorePassword=" + FIXTURE_PASSWORD),
                    "stopped", url.toString(), "0", FIXTURE_PASSWORD);
            cluster.assertNoStubRejections();
            }
        }

    private static JMXServiceURL awaitUrl(TestMember member)
            throws Exception
        {
        await("JMX discovery; see " + member.log, () -> member.lookup() != null);
        JMXServiceURL url = member.lookup();
        return url;
        }

    private static URI registryAddress(JMXServiceURL url)
        {
        return URI.create(url.getURLPath().substring("/jndi/".length()));
        }

    private static void assertDiscoveredMBeans(TestMember member, int cMembers)
            throws Exception
        {
        try (JMXConnector connector = JMXConnectorFactory.connect(awaitUrl(member)))
            {
            MBeanServerConnection server = connector.getMBeanServerConnection();
            await("remote Coherence Node MBeans", () ->
                    server.queryNames(new ObjectName("Coherence:type=Node,*"), null).size() == cMembers);
            Set<ObjectName> names = server.queryNames(new ObjectName("Coherence:type=Cluster,*"), null);
            assertEquals(1, names.size());
            assertEquals(cMembers, server.getAttribute(names.iterator().next(), "ClusterSize"));
            }
        }

    private static void await(String sDescription, Callable<Boolean> condition)
            throws Exception
        {
        long      lDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        Exception last      = null;
        do
            {
            try
                {
                if (condition.call())
                    {
                    return;
                    }
                }
            catch (Exception e)
                {
                last = e;
                }
            Thread.sleep(100);
            }
        while (System.nanoTime() < lDeadline);

        throw new AssertionError("Timed out waiting for " + sDescription, last);
        }

    private static String memberClasspath()
            throws Exception
        {
        return new File(Coherence.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + new File(SafeStubJmxDiscoveryTests.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        }

    // ----- JVM fixtures --------------------------------------------------

    private static class TestCluster
            implements AutoCloseable
        {
        TestCluster()
                throws Exception
            {
            clusterPort = port();
            directory   = Path.of("target", "test-output", clusterName).toAbsolutePath();
            Files.createDirectories(directory);
            }

        TestMember start()
                throws Exception
            {
            return start(Map.of());
            }

        TestMember start(Map<String, String> options)
                throws Exception
            {
            TestMember member = new TestMember(this, members.size(), options);
            members.add(member);
            await("member startup; see " + member.log, () ->
                {
                assertTrue("Member exited; see " + member.log, member.process.isAlive());
                return Boolean.parseBoolean(member.state().getProperty("running"));
                });
            return member;
            }

        void awaitSize(int cMembers)
                throws Exception
            {
            await("cluster size " + cMembers + "; see " + directory, () ->
                {
                for (TestMember member : members)
                    {
                    if (!member.stopped && Integer.parseInt(member.state().getProperty("size", "0")) != cMembers)
                        {
                        return false;
                        }
                    }
                return true;
                });
            }

        int port()
                throws Exception
            {
            for (;;)
                {
                try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")))
                    {
                    int nPort = socket.getLocalPort();
                    if (ports.add(nPort))
                        {
                        return nPort;
                        }
                    }
                }
            }

        void assertNoStubRejections()
                throws Exception
            {
            for (TestMember member : members)
                {
                String sLog = Files.readString(member.log);
                assertFalse(member.log.toString(), sLog.contains("unsupported JMX service URL path: /stub/"));
                assertFalse(member.log.toString(), sLog.contains("rejected management JMX URL"));
                assertFalse(member.log.toString(), sLog.contains("reason=rmi-stub-url"));
                }
            }

        void runClient(String sName, List<String> options, String... arguments)
                throws Exception
            {
            Path log = directory.resolve(sName + "-client.log");
            List<String> command = new ArrayList<>(List.of(
                    Path.of(System.getProperty("test.jvm.java.home", System.getProperty("java.home")), "bin", "java").toString(),
                    "-cp", memberClasspath()));
            command.addAll(options);
            command.add(ClientMain.class.getName());
            command.addAll(List.of(arguments));
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().keySet().removeAll(List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"));
            Process process = builder.start();
            try
                {
                assertTrue("Client timed out; see " + log, process.waitFor(60, TimeUnit.SECONDS));
                assertEquals("Client failed; see " + log + "\n" + Files.readString(log), 0, process.exitValue());
                }
            finally
                {
                if (process.isAlive())
                    {
                    process.destroyForcibly();
                    process.waitFor(10, TimeUnit.SECONDS);
                    }
                }
            }

        @Override
        public void close()
                throws Exception
            {
            for (TestMember member : members)
                {
                member.process.destroy();
                }
            for (TestMember member : members)
                {
                if (!member.process.waitFor(20, TimeUnit.SECONDS))
                    {
                    member.process.destroyForcibly();
                    member.process.waitFor(20, TimeUnit.SECONDS);
                    }
                }
            }

        final String           clusterName = "SafeStubJmxDiscovery-" + UUID.randomUUID();
        final Set<Integer>     ports       = new HashSet<>();
        final List<TestMember> members     = new ArrayList<>();
        final int              clusterPort;
        final Path             directory;
        }

    private static class TestMember
        {
        TestMember(TestCluster cluster, int nMember, Map<String, String> options)
                throws Exception
            {
            directory    = cluster.directory.resolve("member-" + nMember);
            log          = directory.resolve("member.log");
            Files.createDirectories(directory);

            List<String> command = new ArrayList<>(List.of(
                    Path.of(System.getProperty("test.jvm.java.home", System.getProperty("java.home")), "bin", "java").toString(),
                    "-cp", memberClasspath(),
                    "-Dcoherence.cluster=" + cluster.clusterName,
                    "-Dcoherence.clusterport=" + cluster.clusterPort,
                    "-Dcoherence.wka=127.0.0.1",
                    "-Dcoherence.localhost=127.0.0.1",
                    "-Dcoherence.member=member-" + nMember,
                    "-Dcoherence.management=dynamic",
                    "-Dcoherence.security.mode=hardened",
                    "-Dcoherence.management.http=none"));
            options.forEach((key, value) -> command.add("-D" + key + '=' + value));
            command.add(MemberMain.class.getName());
            command.add(directory.toString());
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().keySet().removeAll(List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"));
            process = builder.start();
            }

        Properties state()
                throws Exception
            {
            Properties state = new Properties();
            try (InputStream in = Files.newInputStream(directory.resolve("state.properties")))
                {
                state.load(in);
                }
            return state;
            }

        int nodeCount()
                throws Exception
            {
            return Integer.parseInt(state().getProperty("nodes", "0"));
            }

        boolean ownsConnector()
                throws Exception
            {
            return Boolean.parseBoolean(state().getProperty("ownsConnector"));
            }

        JMXServiceURL lookup()
                throws Exception
            {
            return NSLookup.lookupJMXServiceURL(new InetSocketAddress("127.0.0.1",
                    Integer.parseInt(state().getProperty("port"))));
            }

        void stopCoherence()
                throws Exception
            {
            Files.createFile(directory.resolve("stop-coherence"));
            await("Coherence shutdown; see " + log, () -> !Boolean.parseBoolean(state().getProperty("running")));
            stopped = true;
            }

        final Path    directory;
        final Path    log;
        final Process process;
        boolean       stopped;
        }

    private static final String FIXTURE_PASSWORD = "coh34079-test-only";

    /**
     * Isolate SSL context state from the JUnit JVM.
     */
    public static class ClientMain
        {
        public static void main(String[] args)
                throws Exception
            {
            if ("ipv6-probe".equals(args[0]))
                {
                String availability;
                try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("::1")))
                    {
                    availability = "available";
                    }
                catch (SocketException e)
                    {
                    availability = e.toString();
                    }
                Files.writeString(Path.of(args[1]), availability);
                return;
                }

            JMXServiceURL url = new JMXServiceURL(args[1]);
            Map<String, Object> environment = new HashMap<>();
            environment.put("com.sun.jndi.rmi.factory.socket", new SslRMIClientSocketFactory());
            if ("stopped".equals(args[0]))
                {
                try (JMXConnector unexpected = JMXConnectorFactory.connect(url, environment))
                    {
                    throw new AssertionError("Stopped connector/registry remains available");
                    }
                catch (IOException expected) {}
                }
            else if ("plain".equals(args[0]))
                {
                try (JMXConnector connector = JMXConnectorFactory.connect(url))
                    {
                    awaitNodes(connector.getMBeanServerConnection(), Integer.parseInt(args[2]));
                    }
                }
            else
                {
                try (JMXConnector unexpected = JMXConnectorFactory.connect(url, environment))
                    {
                    throw new AssertionError("Owned connector accepted a connection without credentials");
                    }
                catch (SecurityException expected) {}
                environment.put(JMXConnector.CREDENTIALS, new String[] {"monitor", "wrong-password"});
                try (JMXConnector unexpected = JMXConnectorFactory.connect(url, environment))
                    {
                    throw new AssertionError("Owned connector accepted invalid credentials");
                    }
                catch (SecurityException expected) {}
                environment.put(JMXConnector.CREDENTIALS, new String[] {"monitor", args[3]});
                try (JMXConnector connector = JMXConnectorFactory.connect(url, environment))
                    {
                    MBeanServerConnection server = connector.getMBeanServerConnection();
                    awaitNodes(server, Integer.parseInt(args[2]));
                    try
                        {
                        server.createMBean("javax.management.timer.Timer", new ObjectName("fixture:type=Timer"));
                        throw new AssertionError("Read-only credentials allowed MBean creation");
                        }
                    catch (SecurityException expected) {}
                    }
                }
            System.out.println("PASS: " + args[0]);
            }
        }

    private static void awaitNodes(MBeanServerConnection server, int count)
            throws Exception
        {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (server.queryNames(new ObjectName("Coherence:type=Node,*"), null).size() != count)
            {
            if (System.nanoTime() > deadline)
                {
                throw new AssertionError("Missing Coherence MBeans");
                }
            Thread.sleep(100);
            }
        }

    /**
     * Harmless sentinel that must never be deserialized from a discovered URL.
     */
    public static class CallbackPayload
            implements Serializable
        {
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            callbacks.incrementAndGet();
            in.defaultReadObject();
            }

        private Object readResolve()
            {
            callbacks.incrementAndGet();
            return this;
            }

        private static final long serialVersionUID = 1L;
        }

    private static final AtomicInteger callbacks = new AtomicInteger();

    /**
     * Supply the stock JVM agent explicitly without exposing its private state.
     */
    public static class ExistingConnectorFinder
            implements MBeanServerFinder
        {
        @Override
        public MBeanServer findMBeanServer(String domain)
            {
            return ManagementFactory.getPlatformMBeanServer();
            }

        @Override
        public JMXServiceURL findJMXServiceUrl(String domain)
            {
            try
                {
                return new JMXServiceURL(System.getProperty("fixture.finder.url"));
                }
            catch (IOException e)
                {
                throw new IllegalArgumentException(e);
                }
            }
        }

    /**
     * A member whose JVM outlives its Coherence instance for cleanup assertions.
     */
    public static class MemberMain
        {
        public static void main(String[] args)
                throws Exception
            {
            Path        directory = Path.of(args[0]);
            Coherence   coherence = Coherence.clusterMember().start().join();
            Cluster     cluster   = CacheFactory.getCluster();
            MBeanServer server    = ManagementFactory.getPlatformMBeanServer();
            Connector   connector = (Connector) cluster.getService(Registry.SERVICE_NAME).getUserContext();
            ((SafeCluster) cluster).getCluster().getNameService().start();
            boolean     fRunning  = true;
            for (;;)
                {
                if (fRunning && Files.exists(directory.resolve("stop-coherence")))
                    {
                    coherence.close();
                    CacheFactory.shutdown();
                    fRunning = false;
                    }
                Path override = directory.resolve("override-url");
                if (fRunning && Files.exists(override))
                    {
                    connector.setJmxServiceUrl(new JMXServiceURL(Files.readString(override)));
                    Files.delete(override);
                    }
                Path publish = directory.resolve("publish-url");
                if (fRunning && Files.exists(publish))
                    {
                    Connector.Publish task = new Connector.Publish();
                    task.setJMXServiceURL(new JMXServiceURL(Files.readString(publish)));
                    task.setListenAddresses(List.of());
                    Set<Member> targets = new HashSet<>(connector.getService().getInfo().getServiceMembers());
                    targets.remove(cluster.getLocalMember());
                    connector.getService().execute(task, targets, null);
                    Files.delete(publish);
                    }
                Properties state = new Properties();
                state.setProperty("callbacks", Integer.toString(callbacks.get()));
                state.setProperty("running", Boolean.toString(fRunning));
                state.setProperty("ownsConnector", Boolean.toString(connector.getConnectorServer() != null));
                state.setProperty("jmxUrl", String.valueOf(connector.getJmxServiceUrl()));
                state.setProperty("nodes", Integer.toString(fRunning
                        ? server.queryNames(new ObjectName("Coherence:type=Node,*"), null).size() : 0));
                if (fRunning)
                    {
                    state.setProperty("size", Integer.toString(cluster.getMemberSet().size()));
                    int nPort = ((SafeCluster) cluster).getCluster().getNameService()
                            .getAcceptor().getProcessor().getServerSocket().getLocalPort();
                    state.setProperty("port", Integer.toString(MultiplexedSocketProvider.getBasePort(nPort)));
                    }
                Path temporary = directory.resolve("state.tmp");
                try (OutputStream out = Files.newOutputStream(temporary))
                    {
                    state.store(out, "dynamic management state");
                    }
                Files.move(temporary, directory.resolve("state.properties"),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                Thread.sleep(100);
                }
            }
        }
    }
