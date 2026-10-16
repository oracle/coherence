/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter;

import com.tangosol.coherence.reporter.extractor.AttributeExtractor;
import com.tangosol.coherence.reporter.extractor.JoinExtractor;
import com.tangosol.coherence.reporter.extractor.SubQueryExtractor;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for execution-scoped wildcard join name resolution.
 *
 * @author phf  2026.10.06
 * @since 26.10
 */
public class DataSourceJoinResolutionTest
    {
    @Test
    public void shouldShareNamesAcrossExtractorsAndPropertyOrder() throws Exception
        {
        MBeanServer server  = mock(MBeanServer.class);
        DataSource  source  = source(server);
        ObjectName  pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
        ObjectName  target  = new ObjectName("Coherence:type=Node,nodeId=1,member=one");
        when(server.queryNames(pattern, null)).thenReturn(Set.of(target));
        JoinExtractor first = join(source, server, "Coherence:type=Node,nodeId={node},*", IdentityExtractor.INSTANCE);
        JoinExtractor other = join(source, server, "Coherence:nodeId={node},type=Node,*", IdentityExtractor.INSTANCE);

        DataSource.JoinResolution scope = source.beginJoinResolution();
        try
            {
            assertEquals(target, first.extract("1"));
            assertEquals(target, other.extract("1"));
            source.postProcess();
            assertEquals(target, first.extract("1"));
            verify(server).queryNames(pattern, null);
            assertEquals(1, targets(scope).size());
            }
        finally
            {
            scope.close();
            }
        assertTrue(targets(scope).isEmpty());
        assertEquals(target, first.extract("1"));
        assertEquals(target, other.extract("1"));
        verify(server, times(3)).queryNames(pattern, null);
        }

    @Test
    public void shouldKeepAllExpandedNamePropertiesInKey() throws Exception
        {
        MBeanServer server = mock(MBeanServer.class);
        DataSource source = source(server);
        when(server.queryNames(any(ObjectName.class), isNull())).thenReturn(Collections.emptySet());
        String[] names =
            {
            "Coherence:type=StorageManager,service=s,cache=c,nodeId=1,*",
            "Coherence:type=StorageManager,service=t,cache=c,nodeId=1,*",
            "Coherence:type=StorageManager,service=s,cache=d,nodeId=1,*",
            "Coherence:type=StorageManager,service=s,cache=c,nodeId=2,*",
            "Other:type=StorageManager,service=s,cache=c,nodeId=1,*",
            "Coherence:type=StorageManager,service=s,cache=c,nodeId=1,domainPartition=p,*"
            };
        try (DataSource.JoinResolution scope = source.beginJoinResolution())
            {
            for (String name : names)
                {
                ObjectName pattern = new ObjectName(name);
                assertEquals(pattern, source.resolveJoinTarget(server, pattern));
                assertEquals(pattern, source.resolveJoinTarget(server, pattern));
                verify(server).queryNames(pattern, null);
                }
            assertEquals(names.length, targets(scope).size());
            }
        }

    @Test
    public void shouldFreezeMissingAmbiguousAndUniqueResultsUntilNextExecution() throws Exception
        {
        MBeanServer server  = mock(MBeanServer.class);
        DataSource  source  = source(server);
        ObjectName  pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
        ObjectName  first   = new ObjectName("Coherence:type=Node,nodeId=1,member=one");
        ObjectName  second  = new ObjectName("Coherence:type=Node,nodeId=1,member=two");
        for (Set<ObjectName> initial : Set.of(Collections.<ObjectName>emptySet(), Set.of(first, second), Set.of(first)))
            {
            when(server.queryNames(pattern, null)).thenReturn(initial);
            ObjectName expected = initial.size() == 1 ? first : pattern;
            try (DataSource.JoinResolution scope = source.beginJoinResolution())
                {
                assertEquals(expected, source.resolveJoinTarget(server, pattern));
                when(server.queryNames(pattern, null)).thenReturn(Set.of(second));
                assertEquals(expected, source.resolveJoinTarget(server, pattern));
                }
            try (DataSource.JoinResolution scope = source.beginJoinResolution())
                {
                assertEquals(second, source.resolveJoinTarget(server, pattern));
                }
            }
        verify(server, times(6)).queryNames(pattern, null);
        }

    @Test
    public void shouldReadLiveAttributesAndHandleDisappearance() throws Exception
        {
        MBeanServer server  = mock(MBeanServer.class);
        DataSource  source  = source(server);
        ObjectName  pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
        ObjectName  target  = new ObjectName("Coherence:type=Node,nodeId=1,member=one");
        when(server.queryNames(pattern, null)).thenReturn(Set.of(target));
        when(server.getAttribute(target, "Value")).thenReturn(10L, 20L)
                .thenThrow(new InstanceNotFoundException()).thenReturn(30L);
        JoinExtractor join = join(source, server, "Coherence:type=Node,nodeId={node},*",
                new AttributeExtractor("Value", ',', false, server));
        try (DataSource.JoinResolution scope = source.beginJoinResolution())
            {
            assertEquals(10L, join.extract("1"));
            assertEquals(20L, join.extract("1"));
            assertEquals(AttributeExtractor.DEFAULT_VALUE, join.extract("1"));
            assertEquals(30L, join.extract("1"));
            }
        verify(server).queryNames(pattern, null);
        verify(server, times(4)).getAttribute(target, "Value");
        }

    @Test
    public void shouldNotCacheQueryExceptions() throws Exception
        {
        MBeanServer server  = mock(MBeanServer.class);
        DataSource  source  = source(server);
        ObjectName  pattern = new ObjectName("Coherence:type=Node,*");
        when(server.queryNames(pattern, null)).thenThrow(new SecurityException("denied"))
                .thenReturn(Collections.emptySet());
        try (DataSource.JoinResolution scope = source.beginJoinResolution())
            {
            assertThrows(SecurityException.class, () -> source.resolveJoinTarget(server, pattern));
            assertTrue(targets(scope).isEmpty());
            assertEquals(pattern, source.resolveJoinTarget(server, pattern));
            assertEquals(pattern, source.resolveJoinTarget(server, pattern));
            }
        verify(server, times(2)).queryNames(pattern, null);
        }

    @Test
    public void shouldIsolateServersAndSources() throws Exception
        {
        MBeanServer server = mock(MBeanServer.class);
        MBeanServer other  = mock(MBeanServer.class);
        DataSource source  = source(server);
        DataSource nested  = source(server);
        ObjectName pattern = new ObjectName("Coherence:type=Node,*");
        ObjectName first   = new ObjectName("Coherence:type=Node,nodeId=1");
        ObjectName second  = new ObjectName("Coherence:type=Node,nodeId=2");
        when(server.queryNames(pattern, null)).thenReturn(Set.of(first));
        when(other.queryNames(pattern, null)).thenReturn(Set.of(second));
        try (DataSource.JoinResolution scope = source.beginJoinResolution())
            {
            assertEquals(first, source.resolveJoinTarget(server, pattern));
            try (DataSource.JoinResolution inner = nested.beginJoinResolution())
                {
                assertEquals(first, nested.resolveJoinTarget(server, pattern));
                }
            assertEquals(first, source.resolveJoinTarget(server, pattern));
            assertEquals(second, source.resolveJoinTarget(other, pattern));
            assertEquals(second, source.resolveJoinTarget(other, pattern));
            }
        verify(server, times(2)).queryNames(pattern, null);
        verify(other, times(2)).queryNames(pattern, null);
        }

    @Test
    public void shouldReleaseEntriesOnFailureAndAllowIdempotentClose() throws Exception
        {
        MBeanServer server  = mock(MBeanServer.class);
        DataSource  source  = source(server);
        ObjectName  pattern = new ObjectName("Coherence:type=Node,*");
        when(server.queryNames(pattern, null)).thenReturn(Collections.emptySet());
        DataSource.JoinResolution scope = source.beginJoinResolution();
        assertThrows(IllegalStateException.class, source::beginJoinResolution);
        assertThrows(IllegalArgumentException.class, () ->
            {
            try (DataSource.JoinResolution ignored = scope)
                {
                source.resolveJoinTarget(server, pattern);
                throw new IllegalArgumentException("failed output");
                }
            });
        assertTrue(targets(scope).isEmpty());
        try (DataSource.JoinResolution next = source.beginJoinResolution())
            {
            source.resolveJoinTarget(server, pattern);
            scope.close();
            source.resolveJoinTarget(server, pattern);
            }
        verify(server, times(2)).queryNames(pattern, null);
        }

    @Test
    public void shouldScopeActualSubqueriesWithoutClearingOuterResolution() throws Exception
        {
        MBeanServer server = mock(MBeanServer.class);
        Set<ObjectName> caches = new HashSet<>();
        for (int i = 0; i < 6; i++)
            {
            caches.add(new ObjectName("Coherence:type=Cache,name=c" + i + ",nodeId=" + (i % 3 + 1)));
            }
        when(server.queryNames(any(ObjectName.class), any())).thenAnswer(call ->
            {
            ObjectName pattern = call.getArgument(0);
            return "Cache".equals(pattern.getKeyProperty("type")) ? caches : Set.of(new ObjectName(
                    "Coherence:type=Node,nodeId=" + pattern.getKeyProperty("nodeId") + ",member=one"));
            });
        when(server.getAttribute(any(ObjectName.class), eq("Value"))).thenReturn(100L);
        XmlElement config = XmlHelper.loadXml("<report><query><pattern>Coherence:type=Cache,*</pattern>"
                + "<params><column-ref>node</column-ref><column-ref>value</column-ref><column-ref>sum</column-ref></params>"
                + "</query><row>"
                + "<column id=\"node\"><type>key</type><name>nodeId</name></column>"
                + "<column id=\"value\"><name>Value</name><query><pattern>Coherence:type=Node,nodeId={node},*</pattern>"
                + "</query></column><column id=\"sum\"><type>function</type><function-name>sum</function-name>"
                + "<column-ref>value</column-ref></column></row></report>");
        try (MockedStatic<MBeanHelper> helper = mockStatic(MBeanHelper.class))
            {
            helper.when(MBeanHelper::findMBeanServer).thenReturn(server);
            JMXQueryHandler outer = new JMXQueryHandler();
            outer.setContext(config.getElement("query"), config);
            // retain the source so the outer scope can be checked around inner execution
            DataSource source = outer.m_source;
            ObjectName pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
            try (DataSource.JoinResolution scope = outer.beginJoinResolution())
                {
                source.resolveJoinTarget(server, pattern);
                SubQueryExtractor subquery = new SubQueryExtractor(new ValueExtractor[0],
                        config.getElement("query"), outer, "sum");
                assertEquals(600.0, ((Number) subquery.extract(null)).doubleValue(), 0.0);
                assertEquals(600.0, ((Number) subquery.extract(null)).doubleValue(), 0.0);
                source.resolveJoinTarget(server, pattern);
                verify(server, times(3)).queryNames(pattern, null);
                for (int nNode = 2; nNode <= 3; nNode++)
                    {
                    verify(server, times(2)).queryNames(new ObjectName("Coherence:type=Node,nodeId=" + nNode + ",*"), null);
                    }
                assertEquals(1, targets(scope).size());
                }
            }
        }

    private static DataSource source(MBeanServer server)
        {
        DataSource source = new DataSource();
        source.m_mbs = server;
        return source;
        }

    private static JoinExtractor join(DataSource source, MBeanServer server, String template, ValueExtractor value)
        {
        return new JoinExtractor(new ValueExtractor[] {IdentityExtractor.INSTANCE}, template, value, server,
                pattern -> source.resolveJoinTarget(server, pattern));
        }

    private static Map<?, ?> targets(DataSource.JoinResolution scope) throws Exception
        {
        Field field = scope.getClass().getDeclaredField("f_mapTargets");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(scope);
        }
    }
