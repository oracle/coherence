/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;
import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.CacheService;
import com.tangosol.net.ServiceInfo;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import java.lang.reflect.Proxy;

import java.security.Principal;
import java.security.PrivilegedAction;

import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for advisory subject capture and manager serialization.
 */
public class ConcurrentSubjectCaptureTest
    {
    @Test
    public void shouldCaptureLocalSubject()
        {
        Subject subject = subject("local");
        ClusteredExecutorService service = new ClusteredExecutorService(cacheService(CacheService.TYPE_DISTRIBUTED));

        Subject captured = Subject.doAs(subject, (PrivilegedAction<Subject>) service::captureSubmitterSubject);

        assertSame(subject, captured);
        }

    @Test
    public void shouldForceRemoteClientSubjectToNull()
        {
        Subject subject = subject("remote");
        ClusteredExecutorService service = new ClusteredExecutorService(cacheService(CacheService.TYPE_REMOTE));

        Subject captured = Subject.doAs(subject, (PrivilegedAction<Subject>) service::captureSubmitterSubject);

        assertNull(captured);
        }

    @Test
    public void shouldForceRemoteGrpcClientSubjectToNull()
        {
        Subject subject = subject("remote-grpc");
        ClusteredExecutorService service = new ClusteredExecutorService(cacheService(CacheService.TYPE_REMOTE_GRPC));

        Subject captured = Subject.doAs(subject, (PrivilegedAction<Subject>) service::captureSubmitterSubject);

        assertNull(captured);
        }

    @Test
    public void shouldRoundTripExternalizableLiteSubject()
        {
        Subject subject = subject("roundtrip");
        ClusteredTaskManager<?, ?, ?> manager = subjectOnlyManager(subject);

        ClusteredTaskManager<?, ?, ?> result = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(manager));

        assertEquals(principal(subject).getName(), principal(result.getSubject()).getName());
        }

    @Test
    public void shouldReadOldExternalizableLiteFormWithNullSubject()
            throws IOException
        {
        ClusteredTaskManager<?, ?, ?> result = new ClusteredTaskManager<>();

        result.readExternal(new DataInputStream(new ByteArrayInputStream(oldFormBytes())));

        assertNull(result.getSubject());
        }

    @Test
    public void shouldRoundTripPofSubjectAtReservedIndex()
        {
        Subject                subject = subject("pof");
        ConfigurablePofContext context = new ConfigurablePofContext("coherence-concurrent-pof-config.xml");
        Binary                 binary  = ExternalizableHelper.toBinary(subjectOnlyManager(subject), context);

        ClusteredTaskManager<?, ?, ?> result = ExternalizableHelper.fromBinary(binary, context);

        assertEquals(principal(subject).getName(), principal(result.getSubject()).getName());
        }

    @Test
    public void shouldRemainNullSubjectCompatibleWithoutDoAs()
        {
        assertDoesNotThrow(() -> manager(null).enforceInstallGate());
        }

    private static ClusteredTaskManager<?, ?, ?> manager(Subject subject)
        {
        return new ClusteredTaskManager<>("test", new ConcurrentTaskInstallGateTest.AnnotatedTask(),
                new ExecutionStrategyBuilder().build(), null, Predicates.never(), null, null,
                OptionsByType.empty(), subject);
        }

    private static ClusteredTaskManager<?, ?, ?> subjectOnlyManager(Subject subject)
        {
        return new ClusteredTaskManager<>("test", null, null, null, null, null, null,
                OptionsByType.empty(), subject);
        }

    private static byte[] oldFormBytes()
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);

        ExternalizableHelper.writeUTF(out, "old-form");
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeLong(out, -1L);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeInt(out, 0);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeInt(out, 1);
        ExternalizableHelper.writeInt(out, 0);
        ExternalizableHelper.writeLong(out, 0L);
        ExternalizableHelper.writeLong(out, 0L);
        out.writeBoolean(false);
        out.writeBoolean(false);
        ExternalizableHelper.writeObject(out, ClusteredTaskManager.State.ORCHESTRATED);
        ExternalizableHelper.writeMap(out, Collections.emptyMap());
        out.flush();

        return outBytes.toByteArray();
        }

    private static Subject subject(String sName)
        {
        return new Subject(false, Set.of(new TestPrincipal(sName)), Set.of(), Set.of());
        }

    private static Principal principal(Subject subject)
        {
        return subject.getPrincipals().iterator().next();
        }

    private static CacheService cacheService(String sType)
        {
        ServiceInfo info = (ServiceInfo) Proxy.newProxyInstance(ConcurrentSubjectCaptureTest.class.getClassLoader(),
                new Class<?>[] {ServiceInfo.class},
                (proxy, method, args) -> "getServiceType".equals(method.getName()) ? sType : null);

        return (CacheService) Proxy.newProxyInstance(ConcurrentSubjectCaptureTest.class.getClassLoader(),
                new Class<?>[] {CacheService.class},
                (proxy, method, args) -> "getInfo".equals(method.getName()) ? info : null);
        }

    public static class TestPrincipal
            implements Principal, Serializable
        {
        public TestPrincipal(String sName)
            {
            m_sName = sName;
            }

        @Override
        public String getName()
            {
            return m_sName;
            }

        @Override
        public boolean equals(Object o)
            {
            return o instanceof TestPrincipal && m_sName.equals(((TestPrincipal) o).m_sName);
            }

        @Override
        public int hashCode()
            {
            return m_sName.hashCode();
            }

        private final String m_sName;
        }
    }
