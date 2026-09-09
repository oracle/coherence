/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.message.Response;
import com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ListenerFilterRequest;
import com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ListenerKeyRequest;
import com.tangosol.coherence.component.net.extend.proxy.MapListenerProxy;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.SecurityConfig;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.NamedCache;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for Extend listener-filter request install gates.
 *
 * @author Aleks Seovic  2026.07.15
 * @since 26.04
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ListenerFilterInstallGateTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld         = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        resetPolicyState();
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        resetPolicyState();
        }

    @Test
    public void shouldRejectPlainFilterOnlyListenerInHardenedDevMode()
        {
        setMode("dev", CoherenceMode.SECURITY_MODE_HARDENED);
        ExposedListenerFilterRequest request = filterRequest(new PlainFilter(), null, true);

        assertThrows(SecurityException.class, () -> request.runWith(null));

        assertEquals(0, ((RecordingMapListenerProxy) request.getChannel().getAttribute(
                com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.ATTR_LISTENER)).m_cFilterAdds);
        }

    @Test
    public void shouldRejectPlainRequestFilterWithTriggerInHardenedProdMode()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED);
        ExposedListenerFilterRequest request = filterRequest(new PlainFilter(), new AnnotatedTrigger(), false);

        assertThrows(SecurityException.class, () -> request.runWith(null));
        }

    @Test
    public void shouldInstallAnnotatedAndXmlAllowedListenerFiltersInHardenedMode()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED);

        ExposedListenerFilterRequest annotated = filterRequest(new AnnotatedFilter(), null, true);
        annotated.runWith(null);
        assertEquals(1, ((RecordingMapListenerProxy) annotated.getChannel().getAttribute(
                com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.ATTR_LISTENER)).m_cFilterAdds);

        ExposedListenerFilterRequest configured = filterRequest(AlwaysFilter.INSTANCE, null, true);
        configured.runWith(null);
        assertEquals(1, ((RecordingMapListenerProxy) configured.getChannel().getAttribute(
                com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.ATTR_LISTENER)).m_cFilterAdds);
        }

    @Test
    public void shouldShadowPlainListenerFilterWhenSecurityModeIsUnsetOrCompatibility()
        {
        for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_COMPATIBILITY})
            {
            setMode("prod", sSecurityMode);
            ExposedListenerFilterRequest request = filterRequest(new PlainFilter(), null, true);

            request.runWith(null);

            assertEquals(1, ((RecordingMapListenerProxy) request.getChannel().getAttribute(
                    com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.ATTR_LISTENER)).m_cFilterAdds);
            assertEquals(Long.valueOf(1L), SerializationTelemetry.snapshot().get(wouldRejectKey(PlainFilter.class)));
            }
        }

    @Test
    public void shouldLeaveKeyOnlyListenerRequestUnaffectedInHardenedMode()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED);
        Channel channel = channel();
        ExposedListenerKeyRequest request = new ExposedListenerKeyRequest();
        request.setChannel(channel);
        request.setNamedCache(namedCache());
        request.setKey("key");
        request.setAdd(true);

        request.runWith(null);

        assertEquals(1, ((RecordingMapListenerProxy) channel.getAttribute(
                com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.ATTR_LISTENER)).m_cKeyAdds);
        }

    private static ExposedListenerFilterRequest filterRequest(Filter<?> filter, MapTrigger trigger, boolean fAdd)
        {
        ExposedListenerFilterRequest request = new ExposedListenerFilterRequest();
        request.setChannel(channel());
        request.setNamedCache(namedCache());
        request.setFilter(filter);
        request.setTrigger(trigger);
        request.setAdd(fAdd);
        return request;
        }

    private static Channel channel()
        {
        Channel channel = new Channel();
        channel.setAttribute(com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.ATTR_LISTENER,
                new RecordingMapListenerProxy());
        return channel;
        }

    private static NamedCache namedCache()
        {
        return (NamedCache) Proxy.newProxyInstance(NamedCache.class.getClassLoader(), new Class<?>[] {NamedCache.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
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
        if (clz == long.class)
            {
            return 0L;
            }
        return null;
        }

    private static void setMode(String sMode, String sSecurityMode)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
        resetPolicyState();
        }

    private static void resetPolicyState()
        {
        CoherenceModeHelper.reset();
        SerializationTelemetry.resetForTesting();
        RemoteExecutablePolicy.resetForTesting();
        try
            {
            java.lang.reflect.Method method = SecurityConfig.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
            }
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

    private static String wouldRejectKey(Class<?> clz)
        {
        return "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + OperationReason.EVALUATE_FILTER.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name() + "}";
        }

    public static class ExposedListenerFilterRequest
            extends NamedCacheFactory$ListenerFilterRequest
        {
        public void runWith(Response response)
            {
            onRun(response);
            }
        }

    public static class ExposedListenerKeyRequest
            extends NamedCacheFactory$ListenerKeyRequest
        {
        public void runWith(Response response)
            {
            onRun(response);
            }
        }

    public static class RecordingMapListenerProxy
            extends MapListenerProxy
        {
        @Override
        public void addListener(NamedCache cache, Filter filter, long lFilterId, boolean fLite, boolean fPriming)
            {
            m_cFilterAdds++;
            }

        @Override
        public void addListener(NamedCache cache, Object oKey, boolean fLite, boolean fPriming)
            {
            m_cKeyAdds++;
            }

        private int m_cFilterAdds;
        private int m_cKeyAdds;
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<Object>
        {
        @Override
        public boolean evaluate(Object value)
            {
            return true;
            }
        }

    public static class PlainFilter
            implements Filter<Object>
        {
        @Override
        public boolean evaluate(Object value)
            {
            return true;
            }
        }

    @Remote.Executable
    public static class AnnotatedTrigger
            implements MapTrigger<Object, Object>
        {
        @Override
        public void process(Entry entry)
            {
            }
        }

    private String m_sModeOld;
    private String m_sSecurityModeOld;
    }
