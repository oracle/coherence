/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package reporter;

import com.tangosol.coherence.reporter.DataSource;
import com.tangosol.coherence.reporter.JMXQueryHandler;
import com.tangosol.coherence.reporter.QueryHandler;
import com.tangosol.coherence.reporter.Reporter;
import com.tangosol.coherence.reporter.ReporterSecurity;
import com.tangosol.coherence.reporter.extractor.AttributeExtractor;
import com.tangosol.coherence.reporter.extractor.JoinExtractor;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

import com.oracle.coherence.testing.SystemPropertyResource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import static org.junit.Assert.*;

/**
 * Join discovery regression tests through the actual Reporter pipeline and
 * shipped cache-effectiveness report, using an in-memory MBeanServer.
 *
 * @author phf  2026.10.06
 * @since 26.10
 */
public class JoinResolutionTests
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @Test
    public void shouldResolveEachStorageTargetOnceIncludingDeltaReset() throws Exception
        {
        for (int cMembers : new int[] {1, 3})
            {
            Fixture fixture = new Fixture(120, cMembers);
            TestReporter reporter = new TestReporter(fixture);
            for (int nRun = 0; nRun < 3; nRun++)
                {
                if (nRun > 0)
                    {
                    for (Map.Entry<ObjectName, Bean> entry : fixture.m_storage.entrySet())
                        {
                        Bean bean = entry.getValue();
                        bean.m_values.put("EvictionCount", 100L + nRun * 30L);
                        if (nRun == 2)
                            {
                            fixture.m_real.unregisterMBean(entry.getKey());
                            fixture.m_real.registerMBean(bean, new ObjectName(entry.getKey() + ",member=replacement"));
                            }
                        }
                    }
                fixture.m_queries.clear();
                // reusing the batch number must still start fresh discovery
                TabularData data = reporter.run(STORAGE_REPORT, "", "storage", 1, null, null, false, true);
                assertNotNull(data);
                assertEquals(120, data.size());
                assertEquals(120 * cMembers, fixture.calls("StorageManager"));
                assertEquals(120 * cMembers, fixture.distinct("StorageManager"));
                for (Object value : data.values())
                    {
                    CompositeData row = (CompositeData) value;
                    assertEquals((nRun == 0 ? 100.0 : 30.0) * cMembers,
                            Double.parseDouble(row.get("evictions").toString()), 0.0);
                    }
                assertClosed(reporter, fixture);
                }
            }
        }

    @Test
    public void shouldShareNodeNamesAcrossColumnsAndRefreshBetweenRuns() throws Exception
        {
        Fixture fixture = new Fixture(120, 1);
        fixture.extendNodes("first");
        TestReporter reporter = new TestReporter(fixture);
        TabularData data = reporter.run(nodeReport("Coherence:type=Node,nodeId={node},*"),
                "", "nodes", 1, null, null, false, true);
        assertNodeRows(data, 120, "storage");
        assertEquals(3, fixture.calls("Node"));

        for (Bean bean : fixture.m_nodes)
            {
            bean.m_values.put("RoleName", "changed");
            }
        fixture.extendNodes("next");
        fixture.m_queries.clear();
        data = reporter.run("", "", "nodes", 1, null, null, false, true);
        assertNodeRows(data, 120, "changed");
        assertEquals(3, fixture.calls("Node"));
        assertClosed(reporter, fixture);

        fixture.m_queries.clear();
        new TestReporter(fixture).run(nodeReport("Coherence:type=Node,nodeId={node},member=next"),
                "", "exact", 1, null, null, false, true);
        assertEquals(0, fixture.calls("Node"));
        }

    @Test
    public void shouldCloseScopesForFileTabularAndCombinedOutput() throws Exception
        {
        for (int nMode = 1; nMode <= 3; nMode++)
            {
            Fixture fixture = new Fixture(6, 1);
            TestReporter reporter = new TestReporter(fixture);
            File dir = m_folder.newFolder();
            try (SystemPropertyResource ignored = new SystemPropertyResource(
                    "coherence.reporter.output.directory", dir.getCanonicalPath()))
                {
                TabularData data = reporter.run(nodeReport("Coherence:type=Node,nodeId={node},*"),
                        dir.getCanonicalPath(), "output", 1, null, null, (nMode & 1) != 0, (nMode & 2) != 0);
                assertEquals(3, fixture.calls("Node"));
                if ((nMode & 1) != 0)
                    {
                    assertTrue(Files.readString(new File(dir, "join-resolution.txt").toPath()).contains("storage"));
                    }
                if ((nMode & 2) != 0)
                    {
                    assertNodeRows(data, 6, "storage");
                    }
                assertClosed(reporter, fixture);
                }
            }
        }

    @Test
    public void shouldCloseScopeForEmptyReport() throws Exception
        {
        Fixture fixture = new Fixture(0, 1);
        TestReporter reporter = new TestReporter(fixture);
        assertNull(reporter.run(nodeReport("Coherence:type=Node,nodeId={node},*"),
                "", "empty", 1, null, null, false, true));
        assertEquals(0, fixture.calls("Node"));
        assertClosed(reporter, fixture);
        }

    @Test
    public void shouldCloseScopeOnInitializationExtractionOutputAndDeltaResetFailure() throws Exception
        {
        for (Failure failure : new Failure[] {Failure.INIT, Failure.EXTRACT, Failure.FILE, Failure.TABULAR, Failure.RESET})
            {
            Fixture fixture = new Fixture(3, 1);
            TestReporter reporter = new TestReporter(fixture);
            reporter.m_failure = failure;
            fixture.m_failQuery = failure == Failure.EXTRACT;
            ReporterSecurity.ReportSource previous = ReporterSecurity.currentReportSource();
            RuntimeException error = assertThrows(RuntimeException.class, () -> reporter.run(STORAGE_REPORT,
                    "", "failure", 1, null, null, failure == Failure.FILE, failure != Failure.FILE));
            assertInjectedFailure(failure, error);
            assertEquals(previous, ReporterSecurity.currentReportSource());
            assertEquals(failure == Failure.RESET, reporter.m_handler.m_postProcessed);
            fixture.m_failQuery = false;
            fixture.m_failAttribute = false;
            assertClosed(reporter, fixture);
            }
        }

    @Test
    public void shouldKeepMissingAndAmbiguousJoinsUnresolvedUntilNextScope() throws Exception
        {
        ObjectName pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
        ObjectName first   = new ObjectName("Coherence:type=Node,nodeId=1,member=first");
        ObjectName second  = new ObjectName("Coherence:type=Node,nodeId=1,member=second");
        for (int cMatches : new int[] {0, 2})
            {
            Fixture fixture = new Fixture(0, 1);
            fixture.m_real.unregisterMBean(new ObjectName("Coherence:type=Node,nodeId=1"));
            Bean bean = new Bean(fixture);
            bean.m_values.put("Value", 10L);
            if (cMatches == 2)
                {
                fixture.m_real.registerMBean(bean, first);
                fixture.m_real.registerMBean(new Bean(fixture), second);
                }
            DataSource source = joinSource(fixture);
            JoinExtractor join = valueJoin(source, fixture);
            try (DataSource.JoinResolution scope = source.beginJoinResolution())
                {
                assertEquals(pattern, source.resolveJoinTarget(fixture.m_server, pattern));
                assertEquals(AttributeExtractor.DEFAULT_VALUE, join.extract("1"));
                if (cMatches == 0)
                    {
                    fixture.m_real.registerMBean(bean, first);
                    }
                else
                    {
                    fixture.m_real.unregisterMBean(second);
                    }
                bean.m_values.put("Value", 20L);
                assertEquals(pattern, source.resolveJoinTarget(fixture.m_server, pattern));
                assertEquals(AttributeExtractor.DEFAULT_VALUE, join.extract("1"));
                assertEquals(1, fixture.calls("Node"));
                }
            try (DataSource.JoinResolution scope = source.beginJoinResolution())
                {
                assertEquals(first, source.resolveJoinTarget(fixture.m_server, pattern));
                assertEquals(20L, join.extract("1"));
                bean.m_values.put("Value", 30L);
                assertEquals(30L, join.extract("1"));
                assertEquals(2, fixture.calls("Node"));
                }
            }
        }

    @Test
    public void shouldRefreshUniqueJoinsAfterTargetsDisappearOrBecomeAmbiguous() throws Exception
        {
        ObjectName pattern = new ObjectName("Coherence:type=Node,nodeId=1,*");
        ObjectName first   = new ObjectName("Coherence:type=Node,nodeId=1");
        ObjectName second  = new ObjectName("Coherence:type=Node,nodeId=1,member=second");
        for (int cMatches : new int[] {0, 2})
            {
            Fixture fixture = new Fixture(0, 1);
            Bean bean = fixture.m_nodes.get(0);
            bean.m_values.put("Value", 10L);
            DataSource source = joinSource(fixture);
            JoinExtractor join = valueJoin(source, fixture);
            try (DataSource.JoinResolution scope = source.beginJoinResolution())
                {
                assertEquals(10L, join.extract("1"));
                if (cMatches == 0)
                    {
                    fixture.m_real.unregisterMBean(first);
                    }
                else
                    {
                    fixture.m_real.registerMBean(new Bean(fixture), second);
                    bean.m_values.put("Value", 30L);
                    }
                assertEquals(first, source.resolveJoinTarget(fixture.m_server, pattern));
                assertEquals(cMatches == 0 ? AttributeExtractor.DEFAULT_VALUE : 30L, join.extract("1"));
                assertEquals(1, fixture.calls("Node"));
                }
            try (DataSource.JoinResolution scope = source.beginJoinResolution())
                {
                assertEquals(pattern, source.resolveJoinTarget(fixture.m_server, pattern));
                assertEquals(AttributeExtractor.DEFAULT_VALUE, join.extract("1"));
                if (cMatches == 0)
                    {
                    fixture.m_real.registerMBean(bean, first);
                    }
                else
                    {
                    fixture.m_real.unregisterMBean(second);
                    }
                bean.m_values.put("Value", 40L);
                assertEquals(AttributeExtractor.DEFAULT_VALUE, join.extract("1"));
                assertEquals(2, fixture.calls("Node"));
                }
            try (DataSource.JoinResolution scope = source.beginJoinResolution())
                {
                assertEquals(first, source.resolveJoinTarget(fixture.m_server, pattern));
                assertEquals(40L, join.extract("1"));
                assertEquals(3, fixture.calls("Node"));
                }
            }
        }

    @Test
    public void shouldKeepIndependentConcurrentReportsIsolated() throws Exception
        {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
            {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++)
                {
                futures.add(executor.submit(() ->
                    {
                    try
                        {
                        Fixture fixture = new Fixture(120, 3);
                        TestReporter reporter = new TestReporter(fixture);
                        assertEquals(120, reporter.run(STORAGE_REPORT, "", "concurrent", 1,
                                null, null, false, true).size());
                        assertEquals(360, fixture.calls("StorageManager"));
                        assertClosed(reporter, fixture);
                        }
                    catch (Exception e)
                        {
                        throw new RuntimeException(e);
                        }
                    }));
                }
            for (Future<?> future : futures)
                {
                future.get(30, TimeUnit.SECONDS);
                }
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    private static void assertInjectedFailure(Failure failure, RuntimeException error)
        {
        String message;
        Class<? extends RuntimeException> type;
        switch (failure)
            {
            case INIT:
                message = "initialization failed";
                type = IllegalStateException.class;
                break;
            case EXTRACT:
                message = "query denied";
                type = SecurityException.class;
                break;
            case FILE:
                message = "file output failed";
                type = IllegalStateException.class;
                break;
            case TABULAR:
                message = "tabular output failed";
                type = IllegalStateException.class;
                break;
            case RESET:
                message = "delta attribute denied";
                type = SecurityException.class;
                break;
            default:
                throw new AssertionError("No expected failure for " + failure);
            }
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            {
            if (type.isInstance(cause) && message.equals(cause.getMessage()))
                {
                return;
                }
            }
        throw new AssertionError("Expected " + failure + " injection: " + message, error);
        }

    private static DataSource joinSource(Fixture fixture)
        {
        return new DataSource()
            {
            @Override
            public MBeanServer getMBeanServer()
                {
                return fixture.m_server;
                }
            };
        }

    private static JoinExtractor valueJoin(DataSource source, Fixture fixture)
        {
        return new JoinExtractor(new ValueExtractor[] {IdentityExtractor.INSTANCE},
                "Coherence:type=Node,nodeId={node},*",
                new AttributeExtractor("Value", ',', false, fixture.m_server), fixture.m_server,
                pattern -> source.resolveJoinTarget(fixture.m_server, pattern));
        }

    private static void assertNodeRows(TabularData data, int cRows, String role)
        {
        assertNotNull(data);
        assertEquals(cRows, data.size());
        for (Object value : data.values())
            {
            CompositeData row = (CompositeData) value;
            assertEquals(role, row.get("role"));
            assertTrue(row.get("machine").toString().startsWith("machine"));
            }
        }

    private static void assertClosed(TestReporter reporter, Fixture fixture) throws Exception
        {
        ObjectName pattern = new ObjectName("Coherence:type=StorageManager,service=probe,cache=cache0,nodeId=1,*");
        int cBefore = fixture.calls("StorageManager");
        reporter.m_handler.m_dataSource.resolveJoinTarget(fixture.m_server, pattern);
        reporter.m_handler.m_dataSource.resolveJoinTarget(fixture.m_server, pattern);
        assertEquals(cBefore + 2, fixture.calls("StorageManager"));
        try (DataSource.JoinResolution ignored = reporter.m_handler.beginJoinResolution())
            {
            // a new execution is allowed after completion or failure
            }
        }

    private static String nodeReport(String pattern)
        {
        return "<report-config><report><file-name>join-resolution.txt</file-name>"
                + "<query><pattern>Coherence:type=Cache,*</pattern></query><row>"
                + "<column id=\"node\"><type>key</type><name>nodeId</name></column>"
                + "<column id=\"cache\"><type>key</type><name>name</name></column>"
                + "<column id=\"role\"><name>RoleName</name><query><pattern>" + pattern + "</pattern></query></column>"
                + "<column id=\"machine\"><name>MachineName</name><query><pattern>" + pattern + "</pattern></query></column>"
                + "</row></report></report-config>";
        }

    private enum Failure {NONE, INIT, EXTRACT, FILE, TABULAR, RESET}

    private static class TestReporter extends Reporter
        {
        TestReporter(Fixture fixture)
            {
            m_fixture = fixture;
            }

        @Override
        public QueryHandler ensureQueryHandler(XmlElement config, XmlElement query, long batch)
            {
            if (m_queryHandler == null)
                {
                m_handler = new TestHandler(m_fixture, this);
                m_handler.setContext(query, config);
                m_queryHandler = m_handler;
                }
            return super.ensureQueryHandler(config, query, batch);
            }

        @Override
        protected void writeReportFile(String path, QueryHandler handler)
            {
            if (m_failure == Failure.FILE)
                {
                throw new IllegalStateException("file output failed");
                }
            super.writeReportFile(path, handler);
            }

        @Override
        protected TabularData tabular(QueryHandler handler, String type)
            {
            if (m_failure == Failure.TABULAR)
                {
                throw new IllegalStateException("tabular output failed");
                }
            return super.tabular(handler, type);
            }

        private final Fixture m_fixture;
        private TestHandler m_handler;
        private Failure m_failure = Failure.NONE;
        }

    private static class TestHandler extends JMXQueryHandler
        {
        TestHandler(Fixture fixture, TestReporter reporter)
            {
            m_fixture = fixture;
            m_reporter = reporter;
            m_source = m_dataSource = new DataSource()
                {
                @Override
                public MBeanServer getMBeanServer()
                    {
                    return fixture.m_server;
                    }
                };
            }

        @Override
        public void execute()
            {
            if (m_reporter.m_failure == Failure.INIT)
                {
                throw new IllegalStateException("initialization failed");
                }
            super.execute();
            }

        @Override
        public void postProcess()
            {
            m_postProcessed = true;
            m_fixture.m_failAttribute = m_reporter.m_failure == Failure.RESET;
            super.postProcess();
            }

        private final Fixture m_fixture;
        private final TestReporter m_reporter;
        private final DataSource m_dataSource;
        private boolean m_postProcessed;
        }

    private static class Fixture
        {
        Fixture(int cCaches, int cMembers) throws Exception
            {
            m_real = MBeanServerFactory.newMBeanServer("Coherence");
            for (int nNode = 1; nNode <= 3; nNode++)
                {
                Bean bean = new Bean(this);
                bean.m_values.put("RoleName", "storage");
                bean.m_values.put("MachineName", "machine" + nNode);
                m_nodes.add(bean);
                m_real.registerMBean(bean, new ObjectName("Coherence:type=Node,nodeId=" + nNode));
                }
            for (int i = 0; i < cCaches; i++)
                {
                for (int nCopy = 0; nCopy < cMembers; nCopy++)
                    {
                    int nNode = cMembers == 3 ? nCopy + 1 : i % 3 + 1;
                    m_real.registerMBean(new Bean(this), new ObjectName("Coherence:type=Cache,service=probe,name=cache"
                            + i + ",nodeId=" + nNode + ",tier=back"));
                    ObjectName name = new ObjectName("Coherence:type=StorageManager,service=probe,cache=cache"
                            + i + ",nodeId=" + nNode);
                    Bean bean = new Bean(this);
                    m_storage.put(name, bean);
                    m_real.registerMBean(bean, name);
                    }
                }
            m_server = (MBeanServer) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] {MBeanServer.class}, (proxy, method, args) ->
                        {
                        if (method.getName().equals("queryNames") && args[0] != null)
                            {
                            ObjectName pattern = (ObjectName) args[0];
                            m_queries.merge(pattern, 1, Integer::sum);
                            if (m_failQuery && "StorageManager".equals(pattern.getKeyProperty("type")))
                                {
                                throw new SecurityException("query denied");
                                }
                            }
                        try
                            {
                            return method.invoke(m_real, args);
                            }
                        catch (InvocationTargetException e)
                            {
                            throw e.getCause();
                            }
                        });
            }

        void extendNodes(String member) throws Exception
            {
            for (ObjectName name : m_real.queryNames(new ObjectName("Coherence:type=Node,*"), null))
                {
                m_real.unregisterMBean(name);
                }
            for (int nNode = 1; nNode <= 3; nNode++)
                {
                m_real.registerMBean(m_nodes.get(nNode - 1),
                        new ObjectName("Coherence:type=Node,nodeId=" + nNode + ",member=" + member));
                }
            }

        int calls(String type)
            {
            return m_queries.entrySet().stream().filter(e -> type.equals(e.getKey().getKeyProperty("type")))
                    .mapToInt(Map.Entry::getValue).sum();
            }

        long distinct(String type)
            {
            return m_queries.keySet().stream().filter(name -> type.equals(name.getKeyProperty("type"))).count();
            }

        final MBeanServer m_real;
        final MBeanServer m_server;
        final Map<ObjectName, Integer> m_queries = new HashMap<>();
        final Map<ObjectName, Bean> m_storage = new HashMap<>();
        final List<Bean> m_nodes = new ArrayList<>();
        boolean m_failQuery;
        boolean m_failAttribute;
        }

    private static class Bean implements DynamicMBean
        {
        Bean(Fixture fixture)
            {
            m_fixture = fixture;
            }

        @Override
        public Object getAttribute(String name)
            {
            if (m_fixture.m_failAttribute && name.equals("EvictionCount"))
                {
                throw new SecurityException("delta attribute denied");
                }
            return m_values.getOrDefault(name, 100L);
            }

        @Override
        public AttributeList getAttributes(String[] names)
            {
            AttributeList list = new AttributeList();
            for (String name : names)
                {
                list.add(new Attribute(name, getAttribute(name)));
                }
            return list;
            }

        @Override
        public void setAttribute(Attribute value) {throw new UnsupportedOperationException();}

        @Override
        public AttributeList setAttributes(AttributeList values) {throw new UnsupportedOperationException();}

        @Override
        public Object invoke(String action, Object[] params, String[] signature) {throw new UnsupportedOperationException();}

        @Override
        public MBeanInfo getMBeanInfo()
            {
            return new MBeanInfo(getClass().getName(), "reporter test bean", new MBeanAttributeInfo[0], null, null, null);
            }

        final Map<String, Object> m_values = new HashMap<>();
        private final Fixture m_fixture;
        }

    private static final String STORAGE_REPORT = "reports/report-cache-effectiveness.xml";
    }
