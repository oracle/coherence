/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.component.net.extend.Channel;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.NullImplementation;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for serializer-local default limit-policy caching.
 *
 * @author Aleks Seovic  2026.08.26
 * @since 14.1.2
 */
public class SerializerLimitPolicyCachingTest
    {
    @After
    public void cleanup()
        {
        if (m_sElements == null)
            {
            System.clearProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS);
            }
        else
            {
            System.setProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS, m_sElements);
            }
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldCacheDefaultPolicyPerSerializer()
            throws ReflectiveOperationException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            System.setProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS, "7");

            assertCachesDefault(new DefaultSerializer());
            assertCachesDefault(new SimplePofContext());
            assertCachesDefault(new ConfigurablePofContext());
            assertCachesDefault(newNullPofContext());

            Channel channel = new Channel();
            channel.setSerializer(new ConfigurablePofContext());
            assertCachesDefault(channel);
            }
        }

    private static Serializer newNullPofContext()
            throws ReflectiveOperationException
        {
        var constructor = NullImplementation.NullPofContext.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
        }

    private static void assertCachesDefault(Serializer serializer)
        {
        SerializationLimitPolicy policy = serializer.getLimitPolicy();
        assertEquals(Integer.valueOf(7), policy.getMaxElements());

        System.setProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS, "9");
        assertSame(policy, serializer.getLimitPolicy());
        assertEquals(Integer.valueOf(7), serializer.getLimitPolicy().getMaxElements());

        System.setProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS, "7");
        }

    private final String m_sElements = System.getProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS);
    }
