/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.io.SerializationRole;
import com.tangosol.net.Member;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.NotificationManager;
import com.tangosol.net.management.Registry;
import com.tangosol.util.OperationReason;

import java.security.Principal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.security.auth.Subject;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SerializationTelemetry}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SerializationTelemetryTest
    {
    @Before
    public void resetTelemetry()
        {
        SerializationTelemetry.resetForTesting();
        CoherenceModeHelper.reset();
        }

    @Test
    public void testRecordHelpersUseCurrentRole()
        {
        try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.GRPC))
            {
            SerializationTelemetry.recordFilterCheck("rejected", "test-filter", Runtime.class, null);
            SerializationTelemetry.recordFmtCheck("rejected", "test-fmt", 255);
            SerializationTelemetry.recordPofCheck("rejected", "test-pof", 123);
            SerializationTelemetry.recordLambdaBytecodeCheck("rejected", "test-lambda");
            SerializationTelemetry.recordExecutablePolicyCheck("allowed", getClass(), OperationReason.PROCESS_ENTRY,
                    SerializationRole.GRPC, null);
            }

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertCounter(map, "coh.serialization.filter_check{result=rejected,reason=test-filter,mode="
                + mode() + ",route=GRPC,principal=-}");
        assertCounter(map, "coh.serialization.fmt_check{result=rejected,reason=test-fmt,mode="
                + mode() + ",fmt=255,route=GRPC}");
        assertCounter(map, "coh.serialization.pof_check{result=rejected,reason=test-pof,mode="
                + mode() + ",type_id=123,route=GRPC}");
        assertCounter(map, "coh.serialization.lambda_bytecode_check{result=rejected,reason=test-lambda,mode="
                + mode() + ",route=GRPC}");
        assertCounter(map, "coh.executable.policy_check{reason=PROCESS_ENTRY,role=GRPC,result=allowed,mode="
                + mode() + "}");
        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    @Test
    public void testRegisterPerTupleMBeans() throws JMException
        {
        RecordingRegistry registry = new RecordingRegistry();
        SerializationTelemetry.register(registry);

        try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.GRPC))
            {
            SerializationTelemetry.recordFilterCheck("rejected", "test-filter", Runtime.class, null);
            SerializationTelemetry.recordFmtCheck("rejected", "test-fmt", 255);
            SerializationTelemetry.recordPofCheck("allowed", "registered-type", 123);
            SerializationTelemetry.recordExecutablePolicyCheck("allowed", getClass(), OperationReason.PROCESS_ENTRY,
                    SerializationRole.GRPC, null);
            }

        Map<String, Object> mapBeans = registry.serializationGateBeans();
        assertEquals(4, mapBeans.size());
        assertMBean(mapBeans, "metric=filter_check", "route=GRPC", "mode=" + mode(),
                "result=rejected", "reason=test-filter");
        assertMBean(mapBeans, "metric=fmt_check", "route=GRPC", "mode=" + mode(),
                "result=rejected", "reason=test-fmt", "fmt=255");
        assertMBean(mapBeans, "metric=pof_check", "route=GRPC", "mode=" + mode(),
                "result=allowed", "reason=registered-type", "type_id=123");
        assertMBean(mapBeans, "metric=executable_policy_check", "route=GRPC", "mode=" + mode(),
                "result=allowed", "reason=PROCESS_ENTRY");

        for (Object oBean : mapBeans.values())
            {
            assertEquals(1L, ((DynamicMBean) oBean).getAttribute("Count"));
            }
        }

    @Test
    public void testRegisterPerTupleMBeansCapsCardinality() throws JMException
        {
        RecordingRegistry registry = new RecordingRegistry();
        SerializationTelemetry.register(registry);

        int cRecord = SerializationTelemetry.MAX_REGISTERED_TUPLES + 5;
        try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.REST))
            {
            for (int i = 0; i < cRecord; i++)
                {
                SerializationTelemetry.recordFmtCheck("allowed", "test-fmt-" + i, i);
                }
            }

        Map<String, Object> mapBeans = registry.serializationGateBeans();
        assertEquals(SerializationTelemetry.MAX_REGISTERED_TUPLES, mapBeans.size());

        Object oOverflow = findMBean(mapBeans, "metric=filter_check", "route=UNCLASSIFIED", "mode=" + mode(),
                "result=rejected", "reason=tag-ceiling-reached");
        assertEquals(6L, ((DynamicMBean) oOverflow).getAttribute("Count"));
        }

    @Test
    public void testConcurrentRegistrationDoesNotBlockBehindFirstRegister() throws Exception
        {
        BlockingFirstRegisterRegistry registry = new BlockingFirstRegisterRegistry();
        SerializationTelemetry.register(registry);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> futureFirst = executor.submit(() -> SerializationTelemetry.recordPofCheck("allowed", "first", 1));

        try
            {
            assertTrue("first telemetry registration did not start", registry.awaitFirstRegister());

            Future<?> futureSecond = executor.submit(() -> SerializationTelemetry.recordPofCheck("allowed", "second", 2));
            futureSecond.get(5, TimeUnit.SECONDS);
            assertFalse("first registration should still be waiting", futureFirst.isDone());
            }
        finally
            {
            registry.releaseFirstRegister();
            futureFirst.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
            }
        }

    @Test
    public void testLogMessageFormat()
        {
        Subject subject = new Subject(true, Collections.singleton((Principal) () -> "alice"),
                Collections.emptySet(), Collections.emptySet());
        assertEquals("route=REST|gate=filter|principal=alice|denied-class=java.lang.Runtime|reason=blocked",
                SerializationTelemetry.formatRejectionForTesting("filter", "REST", subject,
                        Runtime.class.getName(), "blocked"));
        }

    private static void assertCounter(Map<String, Long> map, String sKey)
        {
        assertTrue("missing counter " + sKey + " in " + map, map.containsKey(sKey));
        assertEquals(Long.valueOf(1), map.get(sKey));
        }

    private static void assertMBean(Map<String, Object> mapBeans, String... asParts) throws JMException
        {
        assertEquals(1L, ((DynamicMBean) findMBean(mapBeans, asParts)).getAttribute("Count"));
        }

    private static Object findMBean(Map<String, Object> mapBeans, String... asParts)
        {
        for (Map.Entry<String, Object> entry : mapBeans.entrySet())
            {
            boolean fMatch = true;
            for (String sPart : asParts)
                {
                fMatch &= entry.getKey().contains(sPart);
                }

            if (fMatch)
                {
                return entry.getValue();
                }
            }

        throw new AssertionError("missing MBean with " + String.join(",", asParts) + " in "
                + mapBeans.keySet().stream().collect(Collectors.joining("\n")));
        }

    private static String mode()
        {
        return CoherenceMode.current().name().toLowerCase(Locale.ROOT);
        }

    // ----- inner class: RecordingRegistry ----------------------------------

    private static class RecordingRegistry
            implements Registry
        {
        @Override
        public String getDomainName()
            {
            return "Coherence";
            }

        @Override
        public String ensureGlobalName(String sName)
            {
            return sName + ",nodeId=1";
            }

        @Override
        public String ensureGlobalName(String sName, Member member)
            {
            return ensureGlobalName(sName);
            }

        @Override
        public boolean isRegistered(String sName)
            {
            return f_mapBeans.containsKey(sName);
            }

        @Override
        public void register(String sName, Object oBean)
            {
            f_mapBeans.put(sName, oBean);
            }

        @Override
        public void unregister(String sName)
            {
            f_mapBeans.remove(sName);
            }

        @Override
        public NotificationManager getNotificationManager()
            {
            return null;
            }

        @Override
        public MBeanServerProxy getMBeanServerProxy()
            {
            return null;
            }

        private Map<String, Object> serializationGateBeans()
            {
            return f_mapBeans.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().contains(SerializationTelemetry.MBEAN_NAME))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }

        private final Map<String, Object> f_mapBeans = new LinkedHashMap<>();
        }

    // ----- inner class: BlockingFirstRegisterRegistry ----------------------

    private static class BlockingFirstRegisterRegistry
            extends RecordingRegistry
        {
        @Override
        public void register(String sName, Object oBean)
            {
            if (f_blockFirst.compareAndSet(true, false))
                {
                f_firstRegisterStarted.countDown();
                try
                    {
                    f_releaseFirstRegister.await(10, TimeUnit.SECONDS);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                    }
                }

            super.register(sName, oBean);
            }

        private boolean awaitFirstRegister() throws InterruptedException
            {
            return f_firstRegisterStarted.await(5, TimeUnit.SECONDS);
            }

        private void releaseFirstRegister()
            {
            f_releaseFirstRegister.countDown();
            }

        private final AtomicBoolean f_blockFirst = new AtomicBoolean(true);

        private final CountDownLatch f_firstRegisterStarted = new CountDownLatch(1);

        private final CountDownLatch f_releaseFirstRegister = new CountDownLatch(1);
        }
    }
