/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.io.json;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.internal.JsonClassMetadataPolicy;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.util.filter.AlwaysFilter;

import java.awt.Shape;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for JSON class metadata policy.
 *
 * @author Vaso Putica  2026.05.09
 * @since 26.07
 */
class JsonClassMetadataPolicyTest
    {
    @Test
    void shouldAllowConfiguredAliasInDevMode()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            Object o = deserializeWithCoherenceAliases("{\"@class\":\"filter.AlwaysFilter\"}");

            assertThat(o, is(AlwaysFilter.INSTANCE()));
            }
        }

    @Test
    void shouldAllowCompatibilityAliasInProdMode()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            Object o = deserializeWithCoherenceAliases("{\"@class\":\"util.filter.AlwaysFilter\"}");

            assertThat(o, is(AlwaysFilter.INSTANCE()));
            }
        }

    @Test
    void shouldRejectFqcnMetadataInDevMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(JsonBindingException.class, () ->
                    deserializeWithCoherenceAliases("{\"@class\":\"com.tangosol.util.filter.AlwaysFilter\"}"));
            }
        }

    @Test
    void shouldRejectJavaAliasEnforcementBypassInDevMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(JsonBindingException.class, () ->
                    deserializeWithCoherenceAliases("{\"@class\":\"java.math.BigDecimal\",\"value\":\"1\"}"));
            }
        }

    @Test
    void shouldRejectJavaxAliasEnforcementBypassInProdMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(JsonBindingException.class, () ->
                    deserializeWithCoherenceAliases("{\"@class\":\"javax.naming.Name\"}"));
            }
        }

    @Test
    void shouldRejectPackageAliasInDevMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            Genson genson = new GensonBuilder()
                    .addPackageAlias("awt", "java.awt")
                    .create();

            assertThrows(JsonBindingException.class,
                    () -> genson.deserialize("{\"@class\":\"awt.Rectangle\"}", Shape.class));
            }
        }

    @Test
    void shouldPreserveLegacyFqcnMetadataCompatibility()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            Genson genson = new GensonBuilder().useClassMetadata(true).create();

            Object o = genson.deserialize("{\"@class\":\"java.util.LinkedHashMap\",\"value\":1}", Object.class);

            assertInstanceOf(java.util.LinkedHashMap.class, o);
            }
        }

    @Test
    void shouldPreserveLegacyPackageAliasCompatibility()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            Genson genson = new GensonBuilder()
                    .addPackageAlias("awt", "java.awt")
                    .create();

            assertInstanceOf(java.awt.Rectangle.class,
                    genson.deserialize("{\"@class\":\"awt.Rectangle\"}", Shape.class));
            }
        }

    @Test
    void shouldPreserveClassForDynamicAliasCompatibilityInDevMode()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            Genson genson = new GensonBuilder().useClassMetadata(true).create();
            String sAlias = genson.aliasFor(Sentinel.class);

            assertThat(genson.classFor(sAlias), is(Sentinel.class));
            }
        }

    @Test
    void shouldRejectDynamicAliasForInboundMetadataInDevMode()
        {
        String sProperty = Sentinel.class.getName() + ".initialized";
        System.clearProperty(sProperty);

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            Genson genson = new GensonBuilder().useClassMetadata(true).create();
            String sAlias = genson.aliasFor(Sentinel.class);

            assertThrows(JsonBindingException.class,
                    () -> genson.deserialize("{\"@class\":\"" + sAlias + "\"}", Object.class));
            }

        assertFalse(Boolean.getBoolean(sProperty));
        }

    @Test
    void shouldRejectBeforeClassLoadingOrInitializationInDevMode()
        {
        String sProperty = Sentinel.class.getName() + ".initialized";
        System.clearProperty(sProperty);

        RecordingClassLoader loader = new RecordingClassLoader(Thread.currentThread().getContextClassLoader());
        Genson genson = new GensonBuilder()
                .useClassMetadata(true)
                .withClassLoader(loader)
                .create();

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(JsonBindingException.class, () ->
                    genson.deserialize("{\"@class\":\"" + Sentinel.class.getName() + "\"}", Object.class));
            }

        assertFalse(loader.wasRequested(Sentinel.class.getName()));
        assertFalse(Boolean.getBoolean(sProperty));
        }

    @Test
    void shouldPreserveLegacyMetadataResolutionWithoutInitialization()
            throws Exception
        {
        String sProperty = Sentinel.class.getName() + ".initialized";
        System.clearProperty(sProperty);

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            Class<?> clz = JsonClassMetadataPolicy.resolveClassMetadata(Sentinel.class.getName(),
                    Map.of(), new HashMap<>(), Map.of(), Map.of(), false,
                    Thread.currentThread().getContextClassLoader());

            assertThat(clz, is(Sentinel.class));
            }

        assertFalse(Boolean.getBoolean(sProperty));
        }


    private Object deserializeWithCoherenceAliases(String sJson)
            throws Exception
        {
        JsonSerializer serializer = new JsonSerializer();
        return serializer.underlying().deserialize(sJson, Object.class);
        }


    static class RecordingClassLoader
            extends ClassLoader
        {
        RecordingClassLoader(ClassLoader parent)
            {
            super(parent);
            }

        @Override
        public Class<?> loadClass(String name)
                throws ClassNotFoundException
            {
            if (Sentinel.class.getName().equals(name))
                {
                m_fRequested.set(true);
                }
            return super.loadClass(name);
            }

        boolean wasRequested(String sClassName)
            {
            return Sentinel.class.getName().equals(sClassName) && m_fRequested.get();
            }

        private final AtomicBoolean m_fRequested = new AtomicBoolean();
        }


    static class Sentinel
        {
        static
            {
            System.setProperty(Sentinel.class.getName() + ".initialized", "true");
            }
        }
    }
