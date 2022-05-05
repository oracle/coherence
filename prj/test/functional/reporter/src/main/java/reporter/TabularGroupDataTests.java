/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package reporter;

import com.tangosol.coherence.reporter.ReportBatch;
import com.tangosol.coherence.reporter.Reporter;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * A collection of functional tests for testing the runTabularGroupReport
 * implementations.
 *
 * @author bb 2013-12-11
 * @see Reporter
 */
public class TabularGroupDataTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "local-only");
        System.setProperty("coherence.distributed.localstorage", "false");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Test the runTabularReportXml functionality.
     */
    @Test
    public void testTabularReportXml()
        {
        String sServer = "TabularGroupReportXml";

        try
            {
            Cluster cluster = CacheFactory.ensureCluster();

            startCacheServer(sServer + "-1", getProjectName());
            startCacheServer(sServer + "-2", getProjectName());

            ReportBatch batch = new ReportBatch();

            System.err.println("**** Test report-group as XML String");
            Map<String, String> mapXmlReport = new HashMap<String, String>();
            mapXmlReport.put("sClusterReport", sXmlReport);

            // run group report
            TabularData data = batch.runTabularGroupReport("sGroupReport", mapXmlReport) ;

            Set<?> setKeys = data.keySet();
            assertTrue("Size of keys should be 1", setKeys != null && setKeys.size() == 1);

            // loop through each key, which are the rows of data
            // there will only be one row for the Cluster which is the size
            for (Object oKey : setKeys)
                {
                // get the columns as an array
                Object[] aoColumns = ((Collection<Object>) oKey).toArray();

                // because this is a report group, this is a TabularDataSupport
                assertTrue(aoColumns[0] instanceof TabularDataSupport);
                }
            }
        catch (Exception e)
            {
            e.printStackTrace();
            throw new RuntimeException("Error from test: " + e.getMessage());
            }
        finally
            {
            stopCacheServer(sServer + "-1");
            stopCacheServer(sServer + "-2");
            }

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

    }
