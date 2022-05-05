/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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

import com.tangosol.net.CacheFactory;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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
