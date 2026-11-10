/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.discovery;

import com.oracle.coherence.testing.SystemPropertyResource;

import com.tangosol.net.internal.NameServiceValuePolicy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.nio.charset.StandardCharsets;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import javax.management.remote.rmi.RMIServer;

import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Positive wire fixtures and rejection cases for the passive stub grammar.
 *
 * @author phf  2026.11.04
 * @since 27.01
 */
public class RmiStubValidatorTest
    {
    @Test
    public void shouldAcceptIndependentJdk21And25Fixtures()
            throws Exception
        {
        Properties fixtures = fixtures();
        assertEquals(12, fixtures.size());
        for (String name : fixtures.stringPropertyNames())
            {
            JMXServiceURL url = new JMXServiceURL(fixtures.getProperty(name));
            assertSame(name, url, NSLookup.validateJMXServiceURL(url));
            assertSame(name, url, NameServiceValuePolicy.validateJmxServiceURL(url));
            NameServiceValuePolicy.validateLookupResult(NSLookup.JMX_CONNECTOR_URL, url.toString());
            }
        }

    @Test
    public void shouldRejectEveryTruncationAndTrailingObject()
            throws Exception
        {
        for (String value : fixtures().stringPropertyNames())
            {
            byte[] bytes = bytes(value);
            for (int length = 0; length < bytes.length; length++)
                {
                reject(Arrays.copyOf(bytes, length));
                }
            for (byte tag : new byte[] {0x70, 0x73, 0x79, 0x78, 0})
                {
                byte[] extra = Arrays.copyOf(bytes, bytes.length + 1);
                extra[bytes.length] = tag;
                reject(extra);
                }
            }
        }

    @Test
    public void shouldRejectNoncanonicalBase64AndOversizedInput()
            throws Exception
        {
        String encoded = Base64.getEncoder().encodeToString(bytes("25-static-modern"));
        assertThrows(IOException.class, () -> RmiStubValidator.validate(encoded + "\n"));
        assertThrows(IOException.class, () -> RmiStubValidator.validate(encoded.substring(0, encoded.length() - 1)));
        assertThrows(IOException.class, () -> RmiStubValidator.validate(encoded + ";ignored"));
        assertThrows(IOException.class, () -> RmiStubValidator.validate("A".repeat(6000)));
        assertThrows(IOException.class, () -> RmiStubValidator.validate(null));
        assertThrows(IOException.class, () -> RmiStubValidator.validate(""));
        byte[] original = bytes("25-static-modern");
        int block = index(original, "UnicastRef2") - 4;
        int length = Byte.toUnsignedInt(original[block + 1]);
        ByteArrayOutputStream split = new ByteArrayOutputStream();
        split.write(original, 0, block);
        split.write(new byte[] {0x77, 1, original[block + 2], 0x77, (byte) (length - 1)});
        split.write(original, block + 3, original.length - block - 3);
        for (byte[] value : new byte[][] {original, split.toByteArray()})
            {
            String canonical = encode(value);
            RmiStubValidator.validate(canonical);
            int position = canonical.length() - (canonical.endsWith("==") ? 3 : 2);
            String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
            int index = alphabet.indexOf(canonical.charAt(position));
            String noncanonical = canonical.substring(0, position) + alphabet.charAt(index | 1)
                    + canonical.substring(position + 1);
            assertTrue(Arrays.equals(value, Base64.getDecoder().decode(noncanonical)));
            assertThrows(IOException.class, () -> RmiStubValidator.validate(noncanonical));
            }
        }

    @Test
    public void shouldRejectUnknownDescriptorsAndSchemaChanges()
            throws Exception
        {
        byte[] original = bytes("25-static-modern");
        byte[] changed = original.clone();
        changed[0] = 0;
        reject(changed);
        changed = original.clone();
        changed[3] = 4;
        reject(changed);
        int name = index(original, "javax.management.remote.rmi.RMIServerImpl_Stub");
        changed = original.clone();
        changed[name] = 'X';
        reject(changed);
        int uid = name + "javax.management.remote.rmi.RMIServerImpl_Stub".length();
        for (int offset : new int[] {uid, uid + 8, uid + 9, uid + 10, uid + 11})
            {
            changed = original.clone();
            changed[offset] ^= 1;
            reject(changed);
            }
        changed = original.clone();
        changed[name - 2] = 0x7f;
        changed[name - 1] = (byte) 0xff;
        reject(changed);
        }

    @Test
    public void shouldRejectUnknownReferenceTagsPortsAndFlags()
            throws Exception
        {
        byte[] original = bytes("25-static-modern");
        int ref = index(original, "UnicastRef2");
        byte[] changed = original.clone();
        changed[ref] = 'X';
        reject(changed);
        changed = original.clone();
        changed[ref + "UnicastRef2".length()] = 2;
        reject(changed);
        int host = index(original, "127.0.0.1");
        for (int port : new int[] {0, -1, 65536})
            {
            changed = original.clone();
            int offset = host + "127.0.0.1".length();
            changed[offset] = (byte) (port >>> 24);
            changed[offset + 1] = (byte) (port >>> 16);
            changed[offset + 2] = (byte) (port >>> 8);
            changed[offset + 3] = (byte) port;
            reject(changed);
            }
        changed = original.clone();
        changed[host] = 0;
        reject(changed);
        changed = original.clone();
        changed[original.length - 2] = 1;
        reject(changed);
        }

    @Test
    public void shouldRejectAdditionalProxyInterfacesAndHandlers()
            throws Exception
        {
        byte[] original = bytes("25-proxy-modern");
        for (int count : new int[] {0, 2, Integer.MAX_VALUE, -1})
            {
            byte[] changed = original.clone();
            changed[6] = (byte) (count >>> 24);
            changed[7] = (byte) (count >>> 16);
            changed[8] = (byte) (count >>> 8);
            changed[9] = (byte) count;
            reject(changed);
            }
        byte[] changed = original.clone();
        changed[index(original, "java.rmi.server.RemoteObjectInvocationHandler")] = 'X';
        reject(changed);
        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {RMIServer.class},
                new CallbackHandler());
        callbacks.set(0);
        reject(serialize(proxy));
        assertEquals(0, callbacks.get());
        }

    @Test
    public void shouldRejectCallbackObjectsAndSocketFactoriesWithoutExecutingThem()
            throws Exception
        {
        callbacks.set(0);
        reject(serialize(new CallbackObject()));
        assertEquals(0, callbacks.get());
        for (RMIClientSocketFactory factory : new RMIClientSocketFactory[] {new CallbackFactory(), new SslSubclass()})
            {
            try (Fixture fixture = new Fixture(factory))
                {
                reject(payload(fixture.url()));
                assertEquals(0, callbacks.get());
                assertEquals(0, fixture.accepted.get());
                }
            }
        }

    @Test
    public void shouldHandleSplitAndLongBlocksAndRejectSkippedTokens()
            throws Exception
        {
        byte[] original = bytes("25-static-modern");
        RmiStubValidator.validate(encode(reframe(original, false, false)));
        RmiStubValidator.validate(encode(reframe(original, true, false)));
        reject(reframe(original, false, true));
        int block = index(original, "UnicastRef2") - 4;
        byte[] changed = original.clone();
        changed[block + 1] = 0;
        reject(changed);
        changed = original.clone();
        changed[block + 1] = (byte) 0xff;
        reject(changed);
        for (int length : new int[] {0, -1, 4097, Integer.MAX_VALUE})
            {
            changed = reframe(original, true, false);
            changed[block + 1] = (byte) (length >>> 24);
            changed[block + 2] = (byte) (length >>> 16);
            changed[block + 3] = (byte) (length >>> 8);
            changed[block + 4] = (byte) length;
            reject(changed);
            }
        }

    @Test
    public void shouldValidateModifiedUtfAcrossBlockBoundaries()
            throws Exception
        {
        byte[] original = bytes("25-static-modern");
        for (String host : Arrays.asList("::1", "localhost", "\u0800".repeat(1024)))
            {
            RmiStubValidator.validate(encode(withHost(original, host)));
            }
        for (String host : Arrays.asList("", "\u0800".repeat(1025), "bad\nname", "bad\u0000name"))
            {
            reject(withHost(original, host));
            }
        byte[] malformed = withHost(original, "localhost");
        int block = index(original, "UnicastRef2") - 4;
        // each primitive byte has a two-byte block header; the host starts
        // after the reference UTF, format byte and host UTF length
        malformed[block + 16 * 3 + 2] = (byte) 0xff;
        reject(malformed);
        }

    @Test
    public void shouldBoundMalformedInputWithoutUnexpectedExceptions()
            throws Exception
        {
        Random random = new Random(34079L);
        for (int i = 0; i < 10000; i++)
            {
            byte[] bytes = new byte[random.nextInt(1200)];
            random.nextBytes(bytes);
            reject(bytes);
            }
        }

    @Test
    public void shouldValidateWithoutNetworkActivityAndThenConnectNormally()
            throws Exception
        {
        try (Fixture fixture = new Fixture(null))
            {
            JMXServiceURL url = fixture.url();
            assertEquals(0, fixture.accepted.get());
            for (int i = 0; i < 1000; i++)
                {
                NSLookup.validateJMXServiceURL(url);
                NameServiceValuePolicy.validateManagementPublishJmxServiceURL(url);
                NameServiceValuePolicy.validateLookupResult(NSLookup.JMX_CONNECTOR_URL, url.toString());
                }
            Thread.sleep(100);
            assertEquals("Passive validation contacted the RMI endpoint", 0, fixture.accepted.get());
            JMXServiceURL reframed = new JMXServiceURL("rmi", url.getHost(), url.getPort(),
                    "/stub/" + encode(reframe(payload(url), true, false)));
            NSLookup.validateJMXServiceURL(reframed);
            try (JMXConnector client = JMXConnectorFactory.connect(reframed))
                {
                assertEquals(ManagementFactory.getPlatformMBeanServer().getDefaultDomain(),
                        client.getMBeanServerConnection().getDefaultDomain());
                }
            assertTrue("Observer did not see the real client connection", fixture.accepted.get() > 0);
            }
        }

    private static Properties fixtures()
            throws IOException
        {
        Properties values = new Properties();
        try (InputStream in = RmiStubValidatorTest.class.getResourceAsStream("/jmx-stub-fixtures.properties"))
            {
            if (in == null)
                {
                throw new IOException("Missing JMX stub fixtures");
                }
            values.load(in);
            }
        return values;
        }

    private static byte[] bytes(String name)
            throws Exception
        {
        return payload(new JMXServiceURL(fixtures().getProperty(name)));
        }

    private static byte[] payload(JMXServiceURL url)
        {
        return Base64.getDecoder().decode(url.getURLPath().substring(6));
        }

    private static String encode(byte[] bytes)
        {
        return Base64.getEncoder().encodeToString(bytes);
        }

    private static void reject(byte[] bytes)
        {
        assertThrows(IOException.class, () -> RmiStubValidator.validate(encode(bytes)));
        }

    private static int index(byte[] bytes, String value)
        {
        byte[] expected = value.getBytes(StandardCharsets.UTF_8);
        outer: for (int i = 0; i <= bytes.length - expected.length; i++)
            {
            for (int j = 0; j < expected.length; j++)
                {
                if (bytes[i + j] != expected[j])
                    {
                    continue outer;
                    }
                }
            return i;
            }
        throw new AssertionError(value);
        }

    private static byte[] serialize(Object value)
            throws IOException
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            out.writeObject(value);
            }
        return bytes.toByteArray();
        }

    private static byte[] reframe(byte[] bytes, boolean longBlocks, boolean injectToken)
            throws IOException
        {
        int start = index(bytes, "UnicastRef2") - 4;
        int length = Byte.toUnsignedInt(bytes[start + 1]);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(result);
        out.write(bytes, 0, start);
        for (int i = 0; i < length; i++)
            {
            if (injectToken && i == 5)
                {
                out.writeByte(0x70);
                }
            out.writeByte(longBlocks ? 0x7a : 0x77);
            if (longBlocks)
                {
                out.writeInt(1);
                }
            else
                {
                out.writeByte(1);
                }
            out.writeByte(bytes[start + 2 + i]);
            }
        out.write(bytes, start + 2 + length, bytes.length - start - 2 - length);
        return result.toByteArray();
        }

    private static byte[] withHost(byte[] bytes, String host)
            throws IOException
        {
        int start = index(bytes, "UnicastRef2") - 4;
        int length = Byte.toUnsignedInt(bytes[start + 1]);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes, start + 2, length));
        ByteArrayOutputStream primitives = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(primitives);
        data.writeUTF(input.readUTF());
        data.writeByte(input.readUnsignedByte());
        input.readUTF();
        data.writeUTF(host);
        data.write(input.readAllBytes());
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(result);
        out.write(bytes, 0, start);
        byte[] values = primitives.toByteArray();
        int chunk = host.length() > 100 ? 255 : 1;
        for (int i = 0; i < values.length; i += chunk)
            {
            int count = Math.min(chunk, values.length - i);
            out.writeByte(0x77);
            out.writeByte(count);
            out.write(values, i, count);
            }
        out.write(bytes, start + 2 + length, bytes.length - start - 2 - length);
        return result.toByteArray();
        }

    private static class Fixture
            implements AutoCloseable
        {
        Fixture(RMIClientSocketFactory factory)
                throws Exception
            {
            hostname = new SystemPropertyResource("java.rmi.server.hostname", "127.0.0.1");
            try
                {
                Map<String, Object> environment = new HashMap<>();
                environment.put("jmx.remote.x.daemon", "true");
                if (factory != null)
                    {
                    environment.put("jmx.remote.rmi.client.socket.factory", factory);
                    }
                environment.put("jmx.remote.rmi.server.socket.factory", (RMIServerSocketFactory) port ->
                    {
                    listener = new ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"))
                        {
                        @Override
                        public Socket accept()
                                throws IOException
                            {
                            Socket socket = super.accept();
                            synchronized (sockets)
                                {
                                sockets.add(socket);
                                }
                            accepted.incrementAndGet();
                            return socket;
                            }
                        };
                    return listener;
                    });
                server = JMXConnectorServerFactory.newJMXConnectorServer(
                        new JMXServiceURL("rmi", "127.0.0.1", 0), environment,
                        ManagementFactory.getPlatformMBeanServer());
                server.start();
                }
            catch (Exception | Error e)
                {
                close();
                throw e;
                }
            }

        JMXServiceURL url()
            {
            return server.getAddress();
            }

        @Override
        public void close()
                throws Exception
            {
            try
                {
                if (server != null)
                    {
                    server.stop();
                    }
                }
            finally
                {
                try
                    {
                    if (listener != null)
                        {
                        listener.close();
                        }
                    synchronized (sockets)
                        {
                        for (Socket socket : sockets)
                            {
                            socket.close();
                            }
                        }
                    }
                finally
                    {
                    hostname.close();
                    }
                }
            }

        final AtomicInteger accepted = new AtomicInteger();
        final Set<Socket> sockets = new HashSet<>();
        final SystemPropertyResource hostname;
        ServerSocket listener;
        JMXConnectorServer server;
        }

    private static class CallbackObject
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
        }

    private static class CallbackHandler
            extends CallbackObject
            implements InvocationHandler
        {
        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] arguments)
            {
            callbacks.incrementAndGet();
            return null;
            }
        }

    private static class CallbackFactory
            extends CallbackObject
            implements RMIClientSocketFactory
        {
        @Override
        public Socket createSocket(String host, int port)
                throws IOException
            {
            callbacks.incrementAndGet();
            throw new IOException("Unexpected socket factory execution");
            }
        }

    private static class SslSubclass
            extends SslRMIClientSocketFactory
        {
        private Object readResolve()
            {
            callbacks.incrementAndGet();
            return this;
            }
        }

    private static final AtomicInteger callbacks = new AtomicInteger();
    }
