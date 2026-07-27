/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.tangosol.coherence.reporter.ReportBatch;
import com.tangosol.coherence.reporter.Reporter;
import com.tangosol.coherence.reporter.ReporterSecurity;
import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import java.net.InetSocketAddress;

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import static org.junit.Assert.*;


/**
 * A collection of functional tests for testing the runTabularReport*
 * implementations.
 *
 * @author tam 2013-11-29
 * @see Reporter
 */
public class TabularDataTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        String sCacheConfig = "reporter-cache-config.xml";

        System.setProperty(CacheConfig.PROPERTY, sCacheConfig);
        System.setProperty("coherence.management", "local-only");
        System.setProperty("coherence.distributed.localstorage", "false");

        AbstractFunctionalTest._startup();

        String                 sServer = "TabularReportXml";
        CoherenceClusterMember member  = AbstractFunctionalTest.startCacheServer(sServer + "-1", getProjectName(), sCacheConfig);
        Eventually.assertThat(invoking(member).isServiceRunning("Management"), is(true));
        member = AbstractFunctionalTest.startCacheServer(sServer + "-2", getProjectName());
        Eventually.assertThat(invoking(member).isServiceRunning("Management"), is(true));

        CacheFactory.getService("TEST_INVOCATION_SERVICE");
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test the runTabularReportXml functionality.
     */
    @Test
    public void shouldRunStandardReportAsXmlString()
        {
        ReportBatch batch = new ReportBatch();

        // run standard report
        TabularData data = batch.runTabularReport(sXmlReport) ;

        Set<?> setKeys = data.keySet();
        assertTrue("Size of keys should be 1", setKeys != null && setKeys.size() == 1);

        // loop through each key, which are the rows of data
        // there will only be one row for the Cluster which is the size
        for (Object oKey : setKeys)
            {
            // get the columns as an array
            Object[] aoColumns = ((Collection<Object>) oKey).toArray();

            for (int i = 0; i < aoColumns.length ; i++)
                {
                System.err.println("Entry " + i + " is " + aoColumns[i]);
                }

            assertTrue("Column count should be 3 column but count is " + aoColumns.length, aoColumns.length == 3);
            assertTrue("Cluster size should be 3 but is " + aoColumns[2],
                       Integer.parseInt(aoColumns[2].toString()) == 3);
            }
        }

    @Test
    public void shouldRunReportGroupAsXmlString()
        {
        ReportBatch batch = new ReportBatch();

        // run report-group
        TabularData dataGroup = batch.runTabularReport(sXmlReportGroup) ;

        Set<?> setKeysGroup = dataGroup.keySet();
        assertTrue("Size of keys should be 1 but is " + setKeysGroup.size(), setKeysGroup != null && setKeysGroup.size() == 1);

        // loop through each key, which are the rows of data
        // there will only be one row for the Cluster which is the size
        for (Object oKey : setKeysGroup)
            {
            // get the columns as an array
            Object[] aoColumns = ((Collection<Object>) oKey).toArray();

            // because this is a report group, this is a TabularDataSupport
            assertTrue(aoColumns[0] instanceof TabularDataSupport);
            }
        }

    @Test
    public void shouldRunReportAsURI()
        {
        ReportBatch batch = new ReportBatch();

        // run report-group
        TabularData dataNormal = batch.runTabularReport("reports/report-service.xml") ;

        Set<?> setKeysNormal = dataNormal.keySet();
        System.out.println(">>>>>>>>>>>>> Data >>>>>>>>>>>");
        for (Object o : setKeysNormal)
            {
            System.out.println(o);
            }
        System.out.println(">>>>>>>>>>>>> Data >>>>>>>>>>>");
        System.out.flush();
        assertThat("Size of keys should be 1", setKeysNormal, hasSize(1));

        // loop through each key, which are the rows of data
        // there will only be only one row for  _TestInvocation_
        for (Object oKey : setKeysNormal)
            {
            // get the columns as an array
            Object[] aoColumns = ((Collection<Object>) oKey).toArray();

            for (int i = 0; i < aoColumns.length ; i++)
                {
                System.err.println("Entry " + i + " is " + aoColumns[i]);
                }

            assertTrue("Column count should be 15 column but count is " + aoColumns.length, aoColumns.length == 24);
            assertTrue("Service Name should be TEST_INVOCATION_SERVICE but is " + aoColumns[3],
                                       "TEST_INVOCATION_SERVICE".equals(aoColumns[3].toString()) );
            }
        }

    @Test
    public void shouldRejectRemoteReportResourceBeforeConnection()
            throws Exception
        {
        AtomicInteger cRequests = new AtomicInteger();
        HttpServer    server    = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange ->
            {
            cRequests.incrementAndGet();
            byte[] abBody = "remote-report-sentinel".getBytes();
            exchange.sendResponseHeaders(200, abBody.length);
            exchange.getResponseBody().write(abBody);
            exchange.close();
            });
        server.start();

        try
            {
            String sUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/report.xml";
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                new ReportBatch().runTabularReport(sUrl);
                fail("remote report URL should be rejected");
                }
            catch (IllegalArgumentException expected)
                {
                // expected
                }

            assertThat(cRequests.get(), is(0));
            }
        finally
            {
            server.stop(0);
            }
        }

    @Test
    public void shouldRunApprovedRemoteReportResource()
            throws Exception
        {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/report.xml", exchange ->
            {
            byte[] abBody = sXmlReport.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, abBody.length);
            exchange.getResponseBody().write(abBody);
            exchange.close();
            });
        server.start();

        String sOld = System.getProperty("coherence.management.report.remote.allowed");
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            String sBase = "http://127.0.0.1:" + server.getAddress().getPort();
            System.setProperty("coherence.management.report.remote.allowed", sBase);

            TabularData data = new ReportBatch().runTabularReport(sBase + "/report.xml");
            assertNotNull(data);
            }
        finally
            {
            restoreProperty("coherence.management.report.remote.allowed", sOld);
            server.stop(0);
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOutOfRootFileUrl()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            new ReportBatch().runTabularReport(new java.io.File("/etc/passwd").toURI().toString());
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectWorkingDirectoryFileOutsideApprovedInputRoot()
            throws Exception
        {
        File file = new File("target/unapproved-reporter-input.xml");
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), sXmlReport.getBytes(StandardCharsets.UTF_8));
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            new ReportBatch().runTabularReport(file.toURI().toString());
            }
        finally
            {
            file.delete();
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOutputPathOutsideApprovedRoot()
            throws IOException
        {
        File tempDirectory = FileHelper.createTempDir();
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            new ReportBatch().setOutputPath(tempDirectory.getAbsolutePath());
            }
        finally
            {
            FileHelper.deleteDirSilent(tempDirectory);
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOutputPathTraversal()
        {
        new ReportBatch().setOutputPath("../reporter-escape");
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectCompatibilityReportFileOutsideSelectedOutputDirectory()
            throws IOException
        {
        File root = FileHelper.createTempDir();
        try
            {
            File file = new File(root.getParentFile(), root.getName() + "-escape.txt");
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
                {
                ReporterSecurity.validateOutputFile(file.getCanonicalPath(), root.getCanonicalPath(), "reporter-core");
                }
            }
        finally
            {
            FileHelper.deleteDirSilent(root);
            }
        }

    @Test
    public void shouldWriteReportUnderApprovedOutputRoot()
            throws IOException
        {
        File   root = FileHelper.createTempDir();
        File   dir  = new File(root, "approved");
        String sOld = System.getProperty("coherence.reporter.output.directory");

        assertTrue(dir.mkdirs());
        System.setProperty("coherence.reporter.output.directory", root.getAbsolutePath());
        try
            {
            ReportBatch batch = new ReportBatch();
            batch.setOutputPath("approved");
            batch.runReport(sXmlReport);

            String[] asFiles = dir.list();
            assertNotNull(asFiles);
            assertTrue("approved output root should contain report output", asFiles.length > 0);
            }
        finally
            {
            restoreProperty("coherence.reporter.output.directory", sOld);
            FileHelper.deleteDirSilent(root);
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectReportFileNameTraversal()
        {
        new ReportBatch().runTabularReport(sXmlReportWithTraversalFileName);
        }

    @Test
    public void shouldRejectReportGroupRemoteLocationBeforeConnection()
            throws Exception
        {
        AtomicInteger cRequests = new AtomicInteger();
        HttpServer    server    = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange ->
            {
            cRequests.incrementAndGet();
            byte[] abBody = "remote-group-sentinel".getBytes();
            exchange.sendResponseHeaders(200, abBody.length);
            exchange.getResponseBody().write(abBody);
            exchange.close();
            });
        server.start();

        try
            {
            String sUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/group-report.xml";
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                new ReportBatch().runTabularReport("<report-group><report-list><report-config><location>"
                        + sUrl + "</location></report-config></report-list></report-group>");
                fail("remote report-group location should be rejected");
                }
            catch (IllegalArgumentException expected)
                {
                // expected
                }

            assertThat(cRequests.get(), is(0));
            }
        finally
            {
            server.stop(0);
            }
        }

    @Test
    public void shouldGetTabularTypeAsReportFileName()
        {
        ReportBatch batch = new ReportBatch();

        // run report-group
        TabularData dataNormal = batch.runTabularReport("test-report-group.xml") ;
        assertNotNull(dataNormal);
        assertThat("Number of composite data rows must be 1", dataNormal.values(), hasSize(1));

        CompositeData tableTabulars = ((CompositeData) dataNormal.values().iterator().next());
        assertThat("Number of tabular data entries must be 2", tableTabulars.values(), hasSize(2));

        Iterator iterator = tableTabulars.values().iterator();
        TabularData reportData = (TabularData) iterator.next();
        assertThat("The tabular data type name must be the report name", reportData.getTabularType().getTypeName(), is("test-report-cluster-config.xml"));
        assertThat("The tabular data type desc must be the report name", reportData.getTabularType().getDescription(), is("test-report-cluster-config.xml"));

        reportData = (TabularData) iterator.next();
        assertThat("The tabular data type name must be the report name", reportData.getTabularType().getTypeName(), is("test-report-node-config.xml"));
        assertThat("The tabular data type desc must be the correct description", reportData.getTabularType().getDescription(), is("Node Details Report"));

        }

    @Test
    public void shouldGetTabularTypeAsDefault()
        {
        ReportBatch batch = new ReportBatch();

        // run individual report with report contents being passed
        TabularData dataNormal = batch.runTabularReport(sXmlReport) ;
        assertNotNull(dataNormal);

        assertThat("The tabular data type name must be the default name", dataNormal.getTabularType().getTypeName(), is("coherence-report.xml"));
        assertThat("The tabular data type desc must be the default name", dataNormal.getTabularType().getDescription(), is("coherence-report.xml"));
        }

    @Test
    public void shouldGetTabularTypeWithReportURI()
        {
        ReportBatch batch = new ReportBatch();

        // run individual report with report contents being passed
        TabularData dataNormal = batch.runTabularReport("test-report-node-config.xml") ;
        assertNotNull(dataNormal);

        assertThat("The tabular data type name must be the default name", dataNormal.getTabularType().getTypeName(), is("test-report-node-config.xml"));
        assertThat("The tabular data type desc must be the correct description", dataNormal.getTabularType().getDescription(), is("Node Details Report"));
        }

    @Test
    public void shouldGetTabularTypeWithGroupAsXmlString()
        {
        ReportBatch batch = new ReportBatch();

        // run report-group
        TabularData dataNormal = batch.runTabularReport(sXmlReportGroupWithClusterReports) ;
        assertNotNull(dataNormal);
        assertThat("Number of composite data rows must be 1", dataNormal.values(), hasSize(1));

        CompositeData tableTabulars = ((CompositeData) dataNormal.values().iterator().next());
        assertThat("Number of tabular data entries must be 2", tableTabulars.values(), hasSize(2));

        Iterator iterator = tableTabulars.values().iterator();
        TabularData reportData = (TabularData) iterator.next();
        assertThat("The tabular data type name must be the report name", reportData.getTabularType().getTypeName(), is("test-report-cluster-config.xml"));
        assertThat("The tabular data type desc must be the report name", reportData.getTabularType().getDescription(), is("test-report-cluster-config.xml"));

        reportData = (TabularData) iterator.next();
        assertThat("The tabular data type name must be the report name", reportData.getTabularType().getTypeName(), is("test-report-node-config.xml"));
        assertThat("The tabular data type desc must be the correct description", reportData.getTabularType().getDescription(), is("Node Details Report"));
        }

    /**
     * Return the project name.
     */
    public static String getProjectName()
        {
        return "reporter";
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

    private static final String sXmlReport =
         "<report-config >\n" +
                 "    <report>\n" +
                 "        <file-name>{date}-cluster-stats.txt</file-name>\n" +
                 "        <delim>{tab}</delim>\n" +
                 "        <filters />\n" +
                 "        <query>\n" +
                 "            <pattern>Coherence:type=Cluster,*</pattern>\n" +
                 "        </query>\n" +
                 "        <row>\n" +
                 "            <column id=\"BatchCounter\">\n" +
                 "                <type>global</type>\n" +
                 "                <name>{batch-counter}</name>\n" +
                 "                <header>Batch Counter</header>\n" +
                 "            </column>\n" +
                 "            <column id=\"ClusterSize\">\n" +
                 "                <name>ClusterSize</name>\n" +
                 "            </column>\n" +
                 "        </row>\n" +
                 "    </report>\n" +
                 "</report-config> ";

    private static final String sXmlReportWithTraversalFileName =
         "<report-config>\n" +
                 "    <report>\n" +
                 "        <file-name>../escape.txt</file-name>\n" +
                 "        <query>\n" +
                 "            <pattern>Coherence:type=Cluster,*</pattern>\n" +
                 "        </query>\n" +
                 "        <row>\n" +
                 "            <column id=\"BatchCounter\">\n" +
                 "                <type>global</type>\n" +
                 "                <name>{batch-counter}</name>\n" +
                 "            </column>\n" +
                 "        </row>\n" +
                 "    </report>\n" +
                 "</report-config> ";

    private static final String sXmlReportGroup =
            "<report-group > <frequency>1m</frequency> <output-directory>./</output-directory>" +
            " <report-list> <report-config> <location>reports/report-service.xml</location> </report-config> " +
            " <report-config> <location>reports/report-cache-size.xml</location> </report-config> " +
            " </report-list> </report-group>";

    private static final String sXmlReportGroupWithClusterReports =
            "<report-group > <frequency>1m</frequency> <output-directory>./</output-directory>" +
            " <report-list> <report-config> <location>test-report-cluster-config.xml</location> </report-config> " +
            " <report-config> <location>test-report-node-config.xml</location> </report-config> " +
            " </report-list> </report-group>";

    }
