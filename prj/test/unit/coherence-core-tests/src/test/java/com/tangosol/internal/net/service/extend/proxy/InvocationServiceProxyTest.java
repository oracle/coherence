/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.component.net.extend.message.Response;
import com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.ServiceInfo;
import com.tangosol.net.messaging.Message;

import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.OperationReason;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Proxy;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for InvocationService proxy policy wiring.
 *
 * @author Aleks Seovic  2026.05.07
 * @since 26.04
 */
public class InvocationServiceProxyTest
    {
    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED, m_sInvocationEnabledOld);
        CoherenceModeHelper.reset();
        SerializationTelemetry.resetForTesting();
        }

    @Test
    public void defaultsToDisabledInProdMode()
        {
        setMode("prod");

        assertFalse(new DefaultInvocationServiceProxyDependencies().isEnabled());
        }

    @Test
    public void defaultsToEnabledInDevMode()
        {
        setMode("dev");

        assertTrue(new DefaultInvocationServiceProxyDependencies().isEnabled());
        }

    @Test
    public void defaultsToEnabledInLegacyMode()
        {
        setMode("legacy");

        assertTrue(new DefaultInvocationServiceProxyDependencies().isEnabled());
        }

    @Test
    public void respectsExplicitEnabledTrueInProdMode_systemProperty()
        {
        setMode("prod");
        System.setProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED, "true");

        assertTrue(new DefaultInvocationServiceProxyDependencies().isEnabled());
        }

    /**
     * The environment variable alias cannot be set portably inside a running
     * JVM, so this pins the Config-mediated lookup path used for both the
     * dotted system property and COHERENCE_INVOCATION_ENABLED.
     */
    @Test
    public void respectsExplicitEnabledTrueInProdMode_envVar()
        {
        setMode("prod");
        System.setProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED, "true");

        assertEquals("true", Config.getProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED));
        assertTrue(new DefaultInvocationServiceProxyDependencies().isEnabled());
        }

    @Test
    public void respectsExplicitEnabledTrueInProdMode_operationalConfig()
        {
        setMode("prod");

        DefaultInvocationServiceProxyDependencies deps = fromXml("<invocation-service-proxy>"
                + "<enabled>true</enabled></invocation-service-proxy>");

        assertTrue(deps.isEnabled());
        assertEquals(Boolean.TRUE, deps.getOperationalConfigEnabled());
        }

    @Test
    public void respectsExplicitEnabledFalseInDevMode()
        {
        setMode("dev");

        DefaultInvocationServiceProxyDependencies deps = fromXml("<invocation-service-proxy>"
                + "<enabled>false</enabled></invocation-service-proxy>");

        assertFalse(deps.isEnabled());
        assertEquals(Boolean.FALSE, deps.getOperationalConfigEnabled());
        }

    @Test
    public void systemPropertyOverridesOperationalConfig()
        {
        setMode("prod");
        System.setProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED, "false");

        DefaultInvocationServiceProxyDependencies deps = fromXml("<invocation-service-proxy>"
                + "<enabled>true</enabled></invocation-service-proxy>");

        assertFalse(deps.isEnabled());
        assertEquals("false", deps.getSystemPropertyValue());
        assertEquals(Boolean.TRUE, deps.getOperationalConfigEnabled());
        }

    @Test
    public void blankSystemPropertyIsUnset()
        {
        setMode("dev");
        System.setProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED, " ");

        DefaultInvocationServiceProxyDependencies deps = new DefaultInvocationServiceProxyDependencies();

        assertTrue(deps.isEnabled());
        assertNull(deps.getSystemPropertyValue());
        }

    @Test
    public void allowsAnnotatedInvocableAndRecordsTelemetry()
        {
        setMode("prod");
        ExposedInvocationRequest request = request(new AnnotatedInvocable());
        AtomicBoolean fQueried = new AtomicBoolean();
        request.setInvocationService(service(fQueried));

        request.runWith(new SimpleResponse());

        assertTrue(fQueried.get());
        assertCounter("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=allowed,mode=prod,sub_reason=policy}");
        }

    @Test
    public void rejectsUnannotatedInvocableBeforeQueryAndRecordsTelemetry()
        {
        setMode("dev");
        ExposedInvocationRequest request = request(new PlainInvocable());
        AtomicBoolean fQueried = new AtomicBoolean();
        request.setInvocationService(service(fQueried));

        SecurityException e = assertThrows(SecurityException.class, () -> request.runWith(new SimpleResponse()));

        assertTrue(e.getMessage().contains(PlainInvocable.class.getName()));
        assertFalse(fQueried.get());
        assertCounter("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=rejected,mode=dev,sub_reason=policy}");
        }

    @Test
    public void allowsUnannotatedInvocableInLegacyAndRecordsWouldRejectTelemetry()
        {
        setMode("legacy");
        ExposedInvocationRequest request = request(new PlainInvocable());
        AtomicBoolean fQueried = new AtomicBoolean();
        request.setInvocationService(service(fQueried));

        request.runWith(new SimpleResponse());

        assertTrue(fQueried.get());
        assertCounter("coh.executable.policy_check{result=would_reject,class="
                + PlainInvocable.class.getName() + ",reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}");
        }

    @Test
    public void clusterInternalInvocationServiceIgnoresProxyEnabledFlag()
        {
        setMode("prod");
        System.setProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED, "false");
        PlainInvocable task = new PlainInvocable();
        AtomicBoolean fQueried = new AtomicBoolean();

        Map<?, ?> mapResult = clusterInternalService(fQueried).query(task, null);

        assertTrue(fQueried.get());
        assertEquals("result", mapResult.get("member"));
        assertTrue(SerializationTelemetry.snapshot().isEmpty());
        }

    @Test
    public void rejectsUnannotatedPriorityTaskBeforeSchedulingCallback()
        {
        setMode("dev");
        PriorityInvocable task = new PriorityInvocable();
        ExposedInvocationRequest request = request(task);

        SecurityException e = assertThrows(SecurityException.class, request::getSchedulingPriority);

        assertTrue(e.getMessage().contains(PriorityInvocable.class.getName()));
        assertFalse(task.wasSchedulingPriorityCalled());
        assertCounter("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=rejected,mode=dev,sub_reason=policy}");
        }

    @Test
    public void rejectsUnannotatedPriorityTaskBeforeRunCanceledCallback()
        {
        setMode("dev");
        PriorityInvocable task = new PriorityInvocable();
        ExposedInvocationRequest request = request(task);

        request.runCanceled(true);

        assertFalse(task.wasRunCanceledCalled());
        assertCounter("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=rejected,mode=dev,sub_reason=policy}");
        }

    @Test
    public void priorityTaskPolicyFailureIsCachedWithoutDuplicateTelemetry()
        {
        setMode("dev");
        PriorityInvocable task = new PriorityInvocable();
        ExposedInvocationRequest request = request(task);

        assertThrows(SecurityException.class, request::getSchedulingPriority);
        assertThrows(SecurityException.class, request::getRequestTimeoutMillis);
        request.runCanceled(false);

        assertFalse(task.wasSchedulingPriorityCalled());
        assertFalse(task.wasRequestTimeoutCalled());
        assertFalse(task.wasRunCanceledCalled());
        assertCounter("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=rejected,mode=dev,sub_reason=policy}", 1L);
        }

    @Test
    public void allowsAnnotatedPriorityTaskCallbacksAndNormalRunWithOneTelemetryTuple()
        {
        setMode("prod");
        AnnotatedPriorityInvocable task = new AnnotatedPriorityInvocable();
        ExposedInvocationRequest request = request(task);
        AtomicBoolean fQueried = new AtomicBoolean();
        request.setInvocationService(service(fQueried));

        assertEquals(PriorityTask.SCHEDULE_IMMEDIATE, request.getSchedulingPriority());
        assertEquals(123L, request.getExecutionTimeoutMillis());
        assertEquals(456L, request.getRequestTimeoutMillis());
        request.runCanceled(false);
        request.runWith(new SimpleResponse());

        assertTrue(task.wasSchedulingPriorityCalled());
        assertTrue(task.wasExecutionTimeoutCalled());
        assertTrue(task.wasRequestTimeoutCalled());
        assertTrue(task.wasRunCanceledCalled());
        assertTrue(fQueried.get());
        assertCounter("coh.executable.policy_check{reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=allowed,mode=prod,sub_reason=policy}", 1L);
        }

    @Test
    public void legacyPriorityTaskCallbackDoesNotBecomeLiveRejection()
        {
        setMode("legacy");
        PriorityInvocable task = new PriorityInvocable();
        ExposedInvocationRequest request = request(task);
        AtomicBoolean fQueried = new AtomicBoolean();
        request.setInvocationService(service(fQueried));

        assertEquals(PriorityTask.SCHEDULE_IMMEDIATE, request.getSchedulingPriority());
        request.runWith(new SimpleResponse());

        assertTrue(task.wasSchedulingPriorityCalled());
        assertTrue(fQueried.get());
        assertCounter("coh.executable.policy_check{result=would_reject,class="
                + PriorityInvocable.class.getName() + ",reason=" + OperationReason.INVOKE.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}", 1L);
        assertTrue(SerializationTelemetry.snapshot().keySet().stream()
                .noneMatch(s -> s.contains("result=rejected") || s.contains("result=allowed")));
        }

    private static DefaultInvocationServiceProxyDependencies fromXml(String sXml)
        {
        return LegacyXmlInvocationServiceProxyHelper.fromXml(XmlHelper.loadXml(sXml),
                new DefaultInvocationServiceProxyDependencies());
        }

    private static ExposedInvocationRequest request(Invocable task)
        {
        ExposedInvocationRequest request = new ExposedInvocationRequest();
        request.setChannel(new TestChannel());
        request.setTask(task);
        request.prepareResponse();
        return request;
        }

    private static InvocationService clusterInternalService(AtomicBoolean fQueried)
        {
        return (InvocationService) Proxy.newProxyInstance(InvocationService.class.getClassLoader(),
                new Class<?>[] {InvocationService.class},
                (proxy, method, args) ->
                    {
                    if ("query".equals(method.getName()))
                        {
                        Invocable task = (Invocable) args[0];
                        task.init((InvocationService) proxy);
                        task.run();
                        fQueried.set(true);
                        return Collections.singletonMap("member", "result");
                        }
                    return defaultValue(method.getReturnType());
                    });
        }

    private static InvocationService service(AtomicBoolean fQueried)
        {
        return (InvocationService) Proxy.newProxyInstance(InvocationService.class.getClassLoader(),
                new Class<?>[] {InvocationService.class},
                (proxy, method, args) ->
                    {
                    if ("query".equals(method.getName()))
                        {
                        fQueried.set(true);
                        return Collections.singletonMap("member", "result");
                        }
                    if ("getInfo".equals(method.getName()))
                        {
                        return (ServiceInfo) proxy;
                        }
                    if ("getServiceType".equals(method.getName()))
                        {
                        return InvocationService.TYPE_REMOTE;
                        }
                    return defaultValue(method.getReturnType());
                    });
        }

    private static Object defaultValue(Class<?> clz)
        {
        if (clz == boolean.class)
            {
            return false;
            }
        if (clz == int.class)
            {
            return 0;
            }
        if (clz == void.class)
            {
            return null;
            }
        return null;
        }

    private static void setMode(String sMode)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        CoherenceModeHelper.reset();
        SerializationTelemetry.resetForTesting();
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static void assertCounter(String sKey)
        {
        assertCounter(sKey, 1L);
        }

    private static void assertCounter(String sKey, long cExpected)
        {
        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertEquals("missing " + sKey + " in " + map, Long.valueOf(cExpected), map.get(sKey));
        }

    public static class ExposedInvocationRequest
            extends InvocationServiceFactory.InvocationRequest
        {
        public void runWith(Response response)
            {
            onRun(response);
            }

        public void prepareResponse()
            {
            setResponse(new SimpleResponse());
            }
        }

    public static class SimpleResponse
            extends Response
        {
        public SimpleResponse()
            {
            super(null, null, true);
            }
        }

    public static class TestChannel
            extends Channel
        {
        @Override
        public Subject getSubject()
            {
            return null;
            }

        @Override
        public void gateEnter()
            {
            }

        @Override
        public void gateExit()
            {
            }

        @Override
        public void send(Message message)
            {
            m_message = message;
            }

        private Message m_message;
        }

    @Remote.Executable
    public static class AnnotatedInvocable
            extends PlainInvocable
        {
        }

    @Remote.Executable
    public static class AnnotatedPriorityInvocable
            extends PriorityInvocable
        {
        }

    public static class PlainInvocable
            implements Invocable
        {
        @Override
        public void init(InvocationService service)
            {
            }

        @Override
        public void run()
            {
            }

        @Override
        public Object getResult()
            {
            return null;
            }
        }

    public static class PriorityInvocable
            extends PlainInvocable
            implements PriorityTask
        {
        @Override
        public long getExecutionTimeoutMillis()
            {
            m_fExecutionTimeoutCalled.set(true);
            return 123L;
            }

        @Override
        public long getRequestTimeoutMillis()
            {
            m_fRequestTimeoutCalled.set(true);
            return 456L;
            }

        @Override
        public int getSchedulingPriority()
            {
            m_fSchedulingPriorityCalled.set(true);
            return SCHEDULE_IMMEDIATE;
            }

        @Override
        public void runCanceled(boolean fAbandoned)
            {
            m_fRunCanceledCalled.set(true);
            }

        public boolean wasExecutionTimeoutCalled()
            {
            return m_fExecutionTimeoutCalled.get();
            }

        public boolean wasRequestTimeoutCalled()
            {
            return m_fRequestTimeoutCalled.get();
            }

        public boolean wasSchedulingPriorityCalled()
            {
            return m_fSchedulingPriorityCalled.get();
            }

        public boolean wasRunCanceledCalled()
            {
            return m_fRunCanceledCalled.get();
            }

        private final AtomicBoolean m_fExecutionTimeoutCalled = new AtomicBoolean();

        private final AtomicBoolean m_fRequestTimeoutCalled = new AtomicBoolean();

        private final AtomicBoolean m_fSchedulingPriorityCalled = new AtomicBoolean();

        private final AtomicBoolean m_fRunCanceledCalled = new AtomicBoolean();
        }

    private final String m_sModeOld = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);

    private final String m_sInvocationEnabledOld =
            System.getProperty(DefaultInvocationServiceProxyDependencies.PROP_INVOCATION_ENABLED);
    }
