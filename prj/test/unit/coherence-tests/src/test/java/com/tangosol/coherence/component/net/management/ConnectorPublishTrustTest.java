/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.management;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.net.InetAddress;

import java.util.Collections;

import javax.management.remote.JMXServiceURL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for management connector publish decoding.
 *
 * @author OpenAI  2026.05.17
 *
 * @since 26.07
 */
public class ConnectorPublishTrustTest
    {
    @Test
    public void shouldReadCompatiblePassivePublish()
            throws Exception
        {
        JMXServiceURL url     = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:9000/jmxrmi");
        InetAddress   address = InetAddress.getByName("127.0.0.1");

        Connector.Publish publish = readPublish(writePublish(url, Collections.singleton(address)));

        assertEquals(url, publish.getJMXServiceURL());
        assertEquals(Collections.singleton(address), publish.getListenAddresses());
        assertNull(publish.getInvocationSender());
        }

    @Test
    public void shouldAcceptPublishFromDynamicSenior()
            throws Exception
        {
        com.tangosol.coherence.component.net.Member memberSenior = member(1);
        TestConnector connector = new TestConnector(memberSenior);
        Connector.Publish publish = new Connector.Publish();
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:9000/jmxrmi");
        InetAddress address = InetAddress.getByName("127.0.0.1");

        publish.setInvocationSender(memberSenior);
        publish.setJMXServiceURL(url);
        publish.setListenAddresses(Collections.singleton(address));

        connector.onPublish(publish);

        assertEquals(url, connector.getJmxServiceUrl());
        assertEquals(Collections.singleton(address), connector.getJmxListenAddresses());
        }

    @Test
    public void shouldIgnorePublishFromNonSenior()
            throws Exception
        {
        com.tangosol.coherence.component.net.Member memberSenior = member(1);
        com.tangosol.coherence.component.net.Member memberOther  = member(2);
        TestConnector connector = new TestConnector(memberSenior);
        Connector.Publish publish = new Connector.Publish();
        JMXServiceURL urlOriginal = new JMXServiceURL("service:jmx:rmi://127.0.0.1:9000");
        JMXServiceURL urlPublish  = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:9001/jmxrmi");

        connector.setJmxServiceUrl(urlOriginal);
        connector.setJmxListenAddresses(Collections.emptySet());
        publish.setInvocationSender(memberOther);
        publish.setJMXServiceURL(urlPublish);
        publish.setListenAddresses(Collections.singleton(InetAddress.getByName("127.0.0.1")));

        connector.onPublish(publish);

        assertEquals(urlOriginal, connector.getJmxServiceUrl());
        assertEquals(Collections.emptySet(), connector.getJmxListenAddresses());
        }

    @Test
    public void shouldReadLegacyDynamicJmxStubPublish()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            assertReadsDynamicJmxStubPublish();
            }
        }

    @Test
    public void shouldReadDevDynamicJmxStubPublish()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.dev())
            {
            assertReadsDynamicJmxStubPublish();
            }
        }

    @Test
    public void shouldRejectUnsafeJmxStubPublishInProd()
            throws Exception
        {
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://127.0.0.1:9000/stub/abcd");

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            assertRejected(() -> readPublish(writePublish(url, Collections.emptySet())));
            }
        }

    @Test
    public void shouldRejectUnexpectedUrlObjectBeforeCallbacks()
            throws Exception
        {
        Exploit.reset();

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput out = buffer.getBufferOutput();
        ExternalizableHelper.writeObject(out, new Exploit());
        ExternalizableHelper.writeCollection(out, Collections.emptySet());

        assertRejected(() -> readPublish(buffer.toBinary().getBufferInput()));
        assertFalse(Exploit.wasRead());
        }

    @Test
    public void shouldRejectUnexpectedListenAddressBeforeCallbacks()
            throws Exception
        {
        Exploit.reset();

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput out = buffer.getBufferOutput();
        ExternalizableHelper.writeObject(out, new JMXServiceURL("service:jmx:rmi://127.0.0.1:9000"));
        out.writeInt(1);
        ExternalizableHelper.writeObject(out, new Exploit());

        assertRejected(() -> readPublish(buffer.toBinary().getBufferInput()));
        assertFalse(Exploit.wasRead());
        }

    private static ReadBuffer.BufferInput writePublish(JMXServiceURL url, java.util.Collection<?> addresses)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput out = buffer.getBufferOutput();
        ExternalizableHelper.writeObject(out, url);
        ExternalizableHelper.writeCollection(out, addresses);
        return buffer.toBinary().getBufferInput();
        }

    private static Connector.Publish readPublish(ReadBuffer.BufferInput input)
            throws IOException
        {
        Connector.Publish publish = new Connector.Publish();
        publish.readExternal(input);
        return publish;
        }

    private static void assertReadsDynamicJmxStubPublish()
            throws Exception
        {
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://127.0.0.1:9000/stub/abcd");

        Connector.Publish publish = readPublish(writePublish(url, Collections.emptySet()));

        assertEquals(url, publish.getJMXServiceURL());
        assertEquals(Collections.emptySet(), publish.getListenAddresses());
        }

    private static com.tangosol.coherence.component.net.Member member(int nId)
            throws Exception
        {
        InetAddress address = InetAddress.getByName("127.0.0." + nId);
        com.tangosol.coherence.component.net.Member member = new com.tangosol.coherence.component.net.Member();
        member.configureDead(nId, new UUID(System.currentTimeMillis(), address, 7574 + nId, nId),
                System.currentTimeMillis());
        return member;
        }

    private static void assertRejected(ThrowingRunnable runnable)
        {
        try
            {
            runnable.run();
            fail("expected rejection");
            }
        catch (IOException | RuntimeException e)
            {
            // expected
            }
        }

    private interface ThrowingRunnable
        {
        void run()
                throws IOException;
        }

    public static class Exploit
            implements Serializable
        {
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fRead = true;
            in.defaultReadObject();
            }

        static void reset()
            {
            s_fRead = false;
            }

        static boolean wasRead()
            {
            return s_fRead;
            }

        private static boolean s_fRead;
        }

    public static class TestConnector
            extends Connector
        {
        TestConnector(com.tangosol.net.Member memberDynamicSenior)
            {
            m_memberDynamicSenior = memberDynamicSenior;
            }

        @Override
        public com.tangosol.net.Member getDynamicSenior()
            {
            return m_memberDynamicSenior;
            }

        private final com.tangosol.net.Member m_memberDynamicSenior;
        }
    }
