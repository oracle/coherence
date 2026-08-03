/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.internal.util.security.RemoteInstallGate;
import com.tangosol.internal.util.security.SecurityConfig;

import com.tangosol.net.events.EventInterceptor;

import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for cache-config declared executable advisories.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.07
 */
public class StartupAdvisoryLoggingTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @Before
    public void setup()
        {
        m_listMessages = new ArrayList<>();
        m_loaderOld    = Thread.currentThread().getContextClassLoader();
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        RemoteInstallGate.resetAdvisoryForTesting();
        RemoteInstallGate.setAdvisoryLoggerForTesting(m_listMessages::add);
        }

    @After
    public void cleanup()
        {
        Thread.currentThread().setContextClassLoader(m_loaderOld);
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        RemoteInstallGate.resetAdvisoryForTesting();
        }

    @Test
    public void adviceFiresForUnannotatedNonAllowlistedClass()
        {
        Set<String> setDedup = new HashSet<>();

        RemoteInstallGate.adviseDeclaredMapTrigger(new PlainTrigger(), setDedup);
        RemoteInstallGate.adviseDeclaredMapTrigger(new PlainTrigger(), setDedup);
        RemoteInstallGate.adviseDeclaredEventInterceptor(new PlainInterceptor(), setDedup);
        RemoteInstallGate.adviseDeclaredEventInterceptor(new PlainInterceptor(), setDedup);
        RemoteInstallGate.adviseDeclaredTopicSubscriber(new PlainFilter(), new PlainExtractor(), setDedup);
        RemoteInstallGate.adviseDeclaredTopicSubscriber(new PlainFilter(), new PlainExtractor(), setDedup);

        assertEquals(m_listMessages.toString(), 4, m_listMessages.size());
        assertTrue(m_listMessages.get(0).contains("Declared MapTrigger class "
                + PlainTrigger.class.getName()));
        assertTrue(m_listMessages.get(1).contains("Declared EventInterceptor class "
                + PlainInterceptor.class.getName()));
        assertTrue(m_listMessages.get(2).contains("Declared topic subscriber filter class "
                + PlainFilter.class.getName()));
        assertTrue(m_listMessages.get(3).contains("Declared topic subscriber extractor class "
                + PlainExtractor.class.getName()));
        assertTrue(m_listMessages.get(0).contains("remote-install of this class would be refused"));
        assertTrue(m_listMessages.get(1).contains("remote-install of this class would be refused"));
        assertTrue(m_listMessages.get(2).contains("remote-install of this class would be refused"));
        assertTrue(m_listMessages.get(3).contains("remote-install of this class would be refused"));
        }

    @Test
    public void annotatedDeclaredClassesDoNotWarn()
        {
        Set<String> setDedup = new HashSet<>();

        RemoteInstallGate.adviseDeclaredMapTrigger(new AnnotatedTrigger(), setDedup);
        RemoteInstallGate.adviseDeclaredEventInterceptor(new AnnotatedInterceptor(), setDedup);
        RemoteInstallGate.adviseDeclaredTopicSubscriber(new AnnotatedFilter(), new AnnotatedExtractor(), setDedup);

        assertTrue(m_listMessages.toString(), m_listMessages.isEmpty());
        }

    @Test
    public void adviceSuppressedForAllowlistedClass()
            throws Exception
        {
        withConfig(entry(XmlExecutableTrigger.class.getName(), "manual", true));

        RemoteInstallGate.adviseDeclaredMapTrigger(new XmlExecutableTrigger(), new HashSet<>());

        assertTrue(m_listMessages.toString(), m_listMessages.isEmpty());
        }

    @Test
    public void adviceDedupIsScopedToBootstrapPass()
        {
        RemoteInstallGate.adviseDeclaredMapTrigger(new PlainTrigger(), new HashSet<>());
        RemoteInstallGate.adviseDeclaredMapTrigger(new PlainTrigger(), new HashSet<>());

        assertEquals(m_listMessages.toString(), 2, m_listMessages.size());
        }

    private void withConfig(String... asEntries)
            throws Exception
        {
        Path dir  = m_folder.newFolder().toPath();
        Path path = dir.resolve(SecurityConfig.RESOURCE_SECURITY_CONFIG);
        Files.createDirectories(path.getParent());
        Files.write(path, xml(asEntries).getBytes(StandardCharsets.UTF_8));
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {dir.toUri().toURL()},
                m_loaderOld));
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        }

    private static String entry(String sName, String sSource, boolean fExecutable)
        {
        return "    <class name=\"" + sName + "\" source=\"" + sSource + "\" executable=\""
                + fExecutable + "\"/>";
        }

    private static String xml(String... asEntries)
        {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n")
          .append("<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\"")
          .append(" version=\"1.0\">\n")
          .append("  <allowed-classes>\n");
        for (String sEntry : asEntries)
            {
            sb.append(sEntry).append('\n');
            }
        sb.append("  </allowed-classes>\n")
          .append("</security-config>\n");
        return sb.toString();
        }

    private static void resetSecurityConfig()
        {
        try
            {
            var method = SecurityConfig.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
            }
        }

    public static class PlainTrigger
            implements MapTrigger<String, String>
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            }
        }

    public static class XmlExecutableTrigger
            implements MapTrigger<String, String>
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            }
        }

    @Remote.Executable
    public static class AnnotatedTrigger
            implements MapTrigger<String, String>
        {
        @Override
        public void process(Entry<String, String> entry)
            {
            }
        }

    public static class PlainInterceptor
            implements EventInterceptor
        {
        @Override
        public void onEvent(com.tangosol.net.events.Event event)
            {
            }
        }

    public static class PlainFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class PlainExtractor
            implements ValueExtractor<String, String>
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    @Remote.Executable
    public static class AnnotatedInterceptor
            implements EventInterceptor
        {
        @Override
        public void onEvent(com.tangosol.net.events.Event event)
            {
            }
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    @Remote.Executable
    public static class AnnotatedExtractor
            implements ValueExtractor<String, String>
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    private List<String> m_listMessages;
    private ClassLoader m_loaderOld;
    }
