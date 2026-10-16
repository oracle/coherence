/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.coherence.reporter.ReporterSecurity;
import com.tangosol.internal.util.security.CompatibilityWarnings.Category;
import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.internal.NameServiceValuePolicy;
import com.tangosol.net.management.ManagementInvocationPolicy;
import com.tangosol.run.xml.SaxParser;
import com.tangosol.util.Base;
import com.tangosol.util.OperationReason;
import com.tangosol.util.RemoteExecutablePolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.helpers.ParserFactory;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Coverage for the central guard and compatibility-only warning opt-ins.
 *
 * @author phf  2026.10.09
 * @since 26.10
 */
public class CompatibilityWarningsTest
    {
    @Before
    @After
    public void resetState()
            throws Exception
        {
        // isolate lifetime state without adding a production test API
        CoherenceModeHelper.clear();
        SerializationTelemetry.resetForTesting();
        for (Category category : productCategories())
            {
            loggedFlag(category).set(false);
            }
        }

    @Test
    public void shouldLimitEveryKnownCategoryIndependently()
            throws Exception
        {
        List<Category> categories = productCategories();
        assertFalse(categories.isEmpty());
        for (Category category : categories)
            {
            assertTrue(CompatibilityWarnings.shouldLog(category));
            assertFalse(CompatibilityWarnings.shouldLog(category));
            }
        assertEquals(categories.size(), loggedCategoryCount());
        assertTrue(CompatibilityWarnings.shouldLog(new Category()));
        assertTrue(CompatibilityWarnings.shouldLog(new Category()));
        }

    @Test
    public void shouldKeepUnknownCategoriesVisibleWithoutRetainingRequestValues()
            throws Exception
        {
        int cCategories = productCategories().size();
        try (MockedStatic<Logger> logger = mockStatic(Logger.class);
             CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            for (int i = 0; i < 1_000; i++)
                {
                assertTrue(CompatibilityWarnings.shouldLog(null));
                ManagementInvocationPolicy.reject("test", "application-" + i, "foreign-domain", "value", null);
                }
            logger.verify(() -> Logger.warn(startsWith("Allowed compatibility management TCMP")), times(1_000));
            }
        assertEquals(cCategories, productCategories().size());
        assertEquals(0, loggedCategoryCount());
        }

    @Test
    public void shouldAllowOnlyOneConcurrentWarning()
            throws Exception
        {
        CountDownLatch  latch    = new CountDownLatch(1);
        AtomicInteger   cLogged  = new AtomicInteger();
        Category        category = new Category();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<Future<?>> futures  = new ArrayList<>();
        try
            {
            for (int i = 0; i < 16; i++)
                {
                futures.add(executor.submit(() ->
                    {
                    latch.await();
                    for (int j = 0; j < 1_000; j++)
                        {
                        if (CompatibilityWarnings.shouldLog(category))
                            {
                            cLogged.incrementAndGet();
                            }
                        }
                    return null;
                    }));
                }
            latch.countDown();
            for (Future<?> future : futures)
                {
                future.get(10, TimeUnit.SECONDS);
                }
            assertEquals(1, cLogged.get());
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    @Test
    public void shouldLimitExecutableWarningsButCountEveryClassAndRoute()
        {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (int i = 0; i < 10; i++)
                    {
                    RemoteExecutablePolicy.current().enforce(FirstProcessor.class, OperationReason.PROCESS_ENTRY,
                            SerializationRole.EXTEND_PROXY, null);
                    RemoteExecutablePolicy.current().enforce(SecondProcessor.class, OperationReason.PROCESS_ENTRY,
                            SerializationRole.GRPC, null);
                    RemoteExecutablePolicy.current().enforce(FirstProcessor.class, OperationReason.AGGREGATE,
                            SerializationRole.CLUSTER, null);
                    }
                }
            logger.verify(() -> Logger.warn(contains("reason=PROCESS_ENTRY")), times(1));
            logger.verify(() -> Logger.warn(contains("reason=AGGREGATE")), times(1));
            assertEquals(30, countTelemetry("coh.executable.policy_check{result=would_reject"));

            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                for (int i = 0; i < 2; i++)
                    {
                    assertThrows(SecurityException.class, () -> RemoteExecutablePolicy.current().enforce(
                            FirstProcessor.class, OperationReason.PROCESS_ENTRY, SerializationRole.EXTEND_PROXY, null));
                    }
                }
            logger.verify(() -> Logger.warn(contains("reason=PROCESS_ENTRY")), times(3));
            assertEquals(2, countTelemetry("coh.executable.policy_check{reason=PROCESS_ENTRY"));
            }
        }

    @Test
    public void shouldLimitUnspecifiedExecutableWarningsAcrossRoutes()
            throws Exception
        {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class);
             CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            for (int i = 0; i < 10; i++)
                {
                SerializationTelemetry.recordExecutablePolicyCheck("would_reject", null, null,
                        SerializationRole.EXTEND_PROXY, null);
                SerializationTelemetry.recordExecutablePolicyCheck("would_reject", null, null,
                        SerializationRole.GRPC, null);
                }
            logger.verify(() -> Logger.warn(contains("reason=-")), times(1));
            assertEquals(20, countTelemetry("coh.executable.policy_check{result=would_reject"));
            assertEquals(1, loggedCategoryCount());
            }
        }

    @Test
    public void shouldLimitDynamicModeShadowsWithoutLimitingActualRejections()
        {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            for (int i = 0; i < 10; i++)
                {
                SerializationTelemetry.recordExecutablePolicyCheck("would_reject", FirstProcessor.class,
                        OperationReason.INVOKE, SerializationRole.EXTEND_PROXY, null,
                        SerializationTelemetry.SUB_REASON_MODE_GATE);
                }
            logger.verify(() -> Logger.warn(contains("gate=remote-executable-policy")), times(1));
            assertEquals(10, countTelemetry("coh.executable.policy_check{result=would_reject"));
            for (int i = 0; i < 2; i++)
                {
                SerializationTelemetry.logRejection("remote-executable-policy", "EXTEND_PROXY", null,
                        FirstProcessor.class.getName(), "INVOKE");
                }
            logger.verify(() -> Logger.warn(contains("gate=remote-executable-policy")), times(3));
            }
        }

    @Test
    public void shouldCountEveryConcurrentExecutableShadow()
            throws Exception
        {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            try
                {
                for (int i = 0; i < 8; i++)
                    {
                    futures.add(executor.submit(() ->
                        {
                        latch.await();
                        for (int j = 0; j < 1_000; j++)
                            {
                            SerializationTelemetry.recordExecutablePolicyCheck("would_reject", FirstProcessor.class,
                                    OperationReason.PROCESS_ENTRY, SerializationRole.EXTEND_PROXY, null);
                            }
                        return null;
                        }));
                    }
                latch.countDown();
                for (Future<?> future : futures)
                    {
                    future.get(10, TimeUnit.SECONDS);
                    }
                assertEquals(8_000, countTelemetry("coh.executable.policy_check{result=would_reject"));
                assertEquals(1, loggedCategoryCount());
                }
            finally
                {
                executor.shutdownNow();
                }
            }
        }

    @Test
    public void shouldLimitLambdaShadowsButNotHardFailures()
        {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (int i = 0; i < 10; i++)
                    {
                    LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("customer.Missing" + i,
                            LambdaBytecodeGate.Site.STATIC_LAMBDA);
                    LambdaBytecodeGate.ensureAllowed(result, LambdaBytecodeGate.Site.STATIC_LAMBDA);
                    }
                for (int i = 0; i < 2; i++)
                    {
                    LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkBytecode(new byte[0], LambdaBytecodeGate.Site.LAMBDA);
                    assertThrows(SecurityException.class,
                            () -> LambdaBytecodeGate.ensureAllowed(result, LambdaBytecodeGate.Site.LAMBDA));
                    }
                }
            logger.verify(() -> Logger.warn(contains("reason=security-config-missing")), times(1));
            logger.verify(() -> Logger.warn(contains("reason=invalid-bytecode")), times(2));
            assertEquals(10, countTelemetry("coh.serialization.lambda_bytecode_check{result=would_reject"));

            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                for (int i = 0; i < 2; i++)
                    {
                    LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("customer.Missing",
                            LambdaBytecodeGate.Site.STATIC_LAMBDA);
                    assertThrows(SecurityException.class,
                            () -> LambdaBytecodeGate.ensureAllowed(result, LambdaBytecodeGate.Site.STATIC_LAMBDA));
                    }
                }
            logger.verify(() -> Logger.warn(contains("reason=security-config-missing")), times(3));
            }
        }

    @Test
    public void shouldMigrateReporterCategoriesWithoutLimitingRejections()
            throws Exception
        {
        Map<String, Set<String>> mapCategories = new HashMap<>();
        mapCategories.put("reporter-resource-allowlist", new HashSet<>(Arrays.asList(
                "file-outside-root", "remote-source-not-approved", "unsupported-uri-scheme")));
        mapCategories.put("reporter-output-allowlist", Collections.singleton("output-outside-root"));
        mapCategories.put("reporter-objectname-allowlist", new HashSet<>(Arrays.asList(
                "object-name-not-allowed", "object-name-pattern-too-broad")));
        mapCategories.put("reporter-operation-allowlist", Collections.singleton("untrusted-method-column"));
        mapCategories.put("reporter-column-class-allowlist", Collections.singleton("unsupported-column-class"));
        Method shadow = ReporterSecurity.class.getDeclaredMethod("shadow",
                String.class, String.class, String.class, String.class, String.class);
        shadow.setAccessible(true);
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (Map.Entry<String, Set<String>> entry : mapCategories.entrySet())
                    {
                    for (String sReason : entry.getValue())
                        {
                        for (int i = 0; i < 10; i++)
                            {
                            shadow.invoke(null, "scope-" + i, "operation-" + i,
                                    entry.getKey(), sReason, "value-" + i);
                            }
                        logger.verify(() -> Logger.warn(contains("reason=" + sReason)), times(1));
                        }
                    }
                ReporterSecurity.validateObjectNamePattern("java.lang:type=Memory", "test");
                ReporterSecurity.validateObjectNamePattern("com.sun.management:type=DiagnosticCommand", "test");
                }
            logger.verify(() -> Logger.warn(startsWith("Allowed compatibility Reporter request")), times(8));
            assertEquals(8, loggedCategoryCount());

            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                for (int i = 0; i < 2; i++)
                    {
                    assertThrows(ReporterSecurity.ReporterSecurityException.class,
                            () -> ReporterSecurity.validateObjectNamePattern("java.lang:type=Memory", "test"));
                    }
                }
            logger.verify(() -> Logger.warn(startsWith("Rejected Reporter request")), times(2));

            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (int i = 0; i < 2; i++)
                    {
                    assertThrows(ReporterSecurity.ReporterSecurityException.class,
                            () -> ReporterSecurity.validateObjectNamePattern("not-an-object-name", "test"));
                    }
                }
            logger.verify(() -> Logger.warn(startsWith("Rejected Reporter request")), times(4));
            assertEquals(8, loggedCategoryCount());
            }
        }

    @Test
    public void shouldLimitManagementCategoriesWithoutChangingRejections()
            throws Exception
        {
        Map<String, Set<String>> mapCategories = new HashMap<>();
        mapCategories.put("remote-function", Collections.singleton("function-not-allowed"));
        mapCategories.put("object-name", Collections.singleton("foreign-domain"));
        mapCategories.put("attribute", Collections.singleton("attribute-not-writable"));
        mapCategories.put("remote-model-method", Collections.singleton("attribute-not-writable"));
        mapCategories.put("attribute-filter", Collections.singleton("filter-not-allowed"));
        mapCategories.put("query-filter", Collections.singleton("filter-not-allowed"));
        mapCategories.put("attribute-value", new HashSet<>(Arrays.asList("executable-value", "unsupported-value")));
        mapCategories.put("argument", new HashSet<>(Arrays.asList("executable-value", "unsupported-value")));
        int cCategories = 0;
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (Map.Entry<String, Set<String>> entry : mapCategories.entrySet())
                    {
                    for (String sReason : entry.getValue())
                        {
                        for (int i = 0; i < 10; i++)
                            {
                            ManagementInvocationPolicy.reject("scope-" + i,
                                    entry.getKey(), sReason, "value-" + i, null);
                            }
                        cCategories++;
                        }
                    }
                }
            int cExpected = cCategories;
            logger.verify(() -> Logger.warn(startsWith("Allowed compatibility management TCMP")), times(cExpected));
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                for (int i = 0; i < 2; i++)
                    {
                    assertThrows(SecurityException.class, () -> ManagementInvocationPolicy.reject(
                            "test", "object-name", "foreign-domain", "java.lang", null));
                    }
                }
            logger.verify(() -> Logger.warn(startsWith("Rejected management TCMP")), times(2));
            }
        }

    @Test
    public void shouldLimitJmxPublishWarningsAcrossDifferentUrls()
            throws Exception
        {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (int i = 0; i < 10; i++)
                    {
                    JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost/stub/value" + i);
                    assertSame(url, NameServiceValuePolicy.validateManagementPublishJmxServiceURL(url));
                    }
                }
            logger.verify(() -> Logger.warn(contains("reason=rmi-stub-url")), times(1));
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost/stub/value");
                assertThrows(IllegalArgumentException.class,
                        () -> NameServiceValuePolicy.validateManagementPublishJmxServiceURL(url));
                }
            }
        }

    @Test
    public void shouldLimitTlsWarningsAndPreserveBothLoggers()
            throws Exception
        {
        java.util.logging.Logger jul = mock(java.util.logging.Logger.class);
        SSLSocketProvider provider = new SSLSocketProvider(new SSLSocketProvider.DefaultDependencies().setLogger(jul));
        SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("unverified"));
        Socket socket = mock(Socket.class);
        try (MockedStatic<CacheFactory> cacheFactory = mockStatic(CacheFactory.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder builder = new SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder();
                builder.setAction(SSLSocketProviderDependenciesBuilder.ACTION_ALLOW);
                HostnameVerifier verifier = builder.realize(null, null, null);
                for (int i = 1; i <= 10; i++)
                    {
                    when(socket.getInetAddress()).thenReturn(InetAddress.getByAddress(new byte[] {127, 0, 0, (byte) i}));
                    provider.ensureSessionValidity(session, socket);
                    assertTrue(verifier.verify("peer-" + i + ".example", session));
                    }
                }
            verify(jul, times(1)).log(eq(Level.WARNING), contains("source=null"));
            cacheFactory.verify(() -> CacheFactory.log(contains("source=allow"), eq(Base.LOG_WARN)), times(1));
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                assertThrows(SSLException.class, () -> provider.ensureSessionValidity(session, socket));
                SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder builder = new SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder();
                builder.setAction(SSLSocketProviderDependenciesBuilder.ACTION_ALLOW);
                assertThrows(IllegalArgumentException.class, () -> builder.realize(null, null, null));
                }
            }
        }

    @Test
    public void shouldLimitXmlProtectionWarningsByTargetAndProtection()
            throws Exception
        {
        SchemaFactory factory = mock(SchemaFactory.class);
        Validator validator = mock(Validator.class);
        doThrow(new SAXNotRecognizedException("unsupported")).when(factory).setProperty(anyString(), any());
        doThrow(new SAXNotRecognizedException("unsupported")).when(validator).setProperty(anyString(), any());
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (int i = 0; i < 10; i++)
                    {
                    invokeSax("configureRequiredSchemaFactoryProtections", SchemaFactory.class, factory);
                    invokeSax("configureRequiredValidatorProtections", Validator.class, validator);
                    }
                }
            logger.verify(() -> Logger.warn(contains("could not be applied")), times(4));
            logger.verify(() -> Logger.warn(contains(XMLConstants.ACCESS_EXTERNAL_DTD)), times(2));
            logger.verify(() -> Logger.warn(contains(XMLConstants.ACCESS_EXTERNAL_SCHEMA)), times(2));
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                assertThrows(SAXException.class,
                        () -> invokeSax("configureRequiredSchemaFactoryProtections", SchemaFactory.class, factory));
                assertThrows(SAXException.class,
                        () -> invokeSax("configureRequiredValidatorProtections", Validator.class, validator));
                }
            }
        }

    @Test
    public void shouldLimitParserFallbackWarningsWithoutChangingFallback()
            throws Exception
        {
        SAXParserFactory factory = mock(SAXParserFactory.class);
        when(factory.newSAXParser()).thenThrow(new ParserConfigurationException("unsupported factory"));
        Parser parser = mock(Parser.class);
        try (MockedStatic<Logger> logger = mockStatic(Logger.class);
             MockedStatic<SAXParserFactory> factories = mockStatic(SAXParserFactory.class);
             MockedStatic<ParserFactory> fallbacks = mockStatic(ParserFactory.class))
            {
            factories.when(SAXParserFactory::newInstance).thenReturn(factory);
            fallbacks.when(ParserFactory::makeParser).thenReturn(parser);
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (int i = 0; i < 10; i++)
                    {
                    assertSame(parser, invokeSax("createParser", null, null));
                    }
                }
            logger.verify(() -> Logger.warn(contains("deprecated parser fallback")), times(1));
            fallbacks.verify(ParserFactory::makeParser, times(10));
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                assertThrows(SAXException.class, () -> invokeSax("createParser", null, null));
                }
            fallbacks.verify(ParserFactory::makeParser, times(10));
            }
        }

    @Test
    public void shouldLimitXmlParserWarningsByTargetAndFeature()
            throws Exception
        {
        SAXParserFactory factory = mock(SAXParserFactory.class);
        doThrow(new SAXNotRecognizedException("unsupported")).when(factory).setFeature(anyString(), anyBoolean());
        Parser parser = mock(Parser.class);
        try (MockedStatic<Logger> logger = mockStatic(Logger.class))
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                for (int i = 0; i < 10; i++)
                    {
                    invokeSax("configureRequiredParserFactoryProtections", SAXParserFactory.class, factory);
                    invokeSax("configureRequiredParserProtections", Parser.class, parser);
                    }
                }
            logger.verify(() -> Logger.warn(contains("could not be applied")), times(8));
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                assertThrows(SAXException.class,
                        () -> invokeSax("configureRequiredParserFactoryProtections", SAXParserFactory.class, factory));
                assertThrows(SAXException.class,
                        () -> invokeSax("configureRequiredParserProtections", Parser.class, parser));
                }
            }
        }

    // ----- helpers -------------------------------------------------------

    private static List<Category> productCategories()
            throws Exception
        {
        List<Category> categories = new ArrayList<>();
        for (Class<?> owner : new Class<?>[] {SerializationTelemetry.class, LambdaBytecodeGate.class,
                ManagementInvocationPolicy.class, NameServiceValuePolicy.class, ReporterSecurity.class,
                SSLSocketProvider.class, SSLSocketProviderDependenciesBuilder.class, SaxParser.class})
            {
            collectOwnerCategories(owner, categories);
            }
        return categories;
        }

    private static void collectOwnerCategories(Class<?> owner, List<Category> categories)
            throws Exception
        {
        for (Field field : owner.getDeclaredFields())
            {
            if (Modifier.isStatic(field.getModifiers()) && (field.getType() == Category.class
                    || Map.class.isAssignableFrom(field.getType()) || field.getType().isArray()))
                {
                field.setAccessible(true);
                collectCategories(field.get(null), categories);
                }
            }
        for (Class<?> nested : owner.getDeclaredClasses())
            {
            collectOwnerCategories(nested, categories);
            }
        }

    private static void collectCategories(Object value, List<Category> categories)
            throws Exception
        {
        if (value instanceof Category)
            {
            categories.add((Category) value);
            }
        else if (value instanceof Map<?, ?>)
            {
            for (Object child : ((Map<?, ?>) value).values())
                {
                collectCategories(child, categories);
                }
            }
        else if (value instanceof Object[])
            {
            for (Object child : (Object[]) value)
                {
                collectCategories(child, categories);
                }
            }
        else if (value != null && value.getClass().getEnclosingClass() == SaxParser.class)
            {
            // XML categories live on the existing fixed protection descriptors
            for (Field field : value.getClass().getDeclaredFields())
                {
                if (field.getType() == Category.class && !Modifier.isStatic(field.getModifiers()))
                    {
                    field.setAccessible(true);
                    collectCategories(field.get(value), categories);
                    }
                }
            }
        }

    private static AtomicBoolean loggedFlag(Category category)
            throws Exception
        {
        Field field = Category.class.getDeclaredField("f_logged");
        field.setAccessible(true);
        return (AtomicBoolean) field.get(category);
        }

    private static int loggedCategoryCount()
            throws Exception
        {
        int cLogged = 0;
        for (Category category : productCategories())
            {
            if (loggedFlag(category).get())
                {
                cLogged++;
                }
            }
        return cLogged;
        }

    private static long countTelemetry(String sPrefix)
        {
        return SerializationTelemetry.snapshot().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sPrefix)).mapToLong(Map.Entry::getValue).sum();
        }

    private static Object invokeSax(String sMethod, Class<?> clzArgument, Object argument)
            throws Exception
        {
        Method method = clzArgument == null ? SaxParser.class.getDeclaredMethod(sMethod)
                : SaxParser.class.getDeclaredMethod(sMethod, clzArgument);
        method.setAccessible(true);
        try
            {
            return clzArgument == null ? method.invoke(null) : method.invoke(null, argument);
            }
        catch (InvocationTargetException e)
            {
            if (e.getCause() instanceof Exception)
                {
                throw (Exception) e.getCause();
                }
            throw e;
            }
        }

    private static class FirstProcessor
        {
        }

    private static class SecondProcessor
        {
        }

    }
