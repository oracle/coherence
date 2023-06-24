/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test ;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertTrue;


/**
 * Validate server side log messages when rest server misconfigured to not have rest pof types.
 * Client only gets a response status of internal server status of 500 for this case.
 *
 * @author jf 2022.01.14
 */
public class PofMisconfiguredRestTests
        extends DefaultPofRestTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void startup()
        {
        Properties props = new Properties();

        props.put("test.pof.config", "rest/rest-misconfigured-pof-config.xml");

        CoherenceClusterMember clusterMember = startCacheServer("PofMisconfiguredRestTests", "rest", FILE_SERVER_CFG_CACHE, props);
        Eventually.assertDeferred(() -> clusterMember.isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    @AfterClass
    public static void cleanup() throws IOException
        {
        stopCacheServer("PofMisconfiguredRestTests", true);

        // validate in server log that there are large number of "unknown user type" for rest types
        validateLogMessages(System.getProperty("test.project.dir"), "target/test-output/functional/PofMisconfiguredRestTests.out",
                            "unknown user type", 50);
        validateLogMessages(System.getProperty("test.project.dir"), "target/test-output/functional/PofMisconfiguredRestTests.out",
                            "unknown user type: com.tangosol.coherence.rest.util.StaticContent", 16);
        }

    // ----- test methods ---------------------------------------------------

    // Override all tests that fail trying to pof serialize a coherence-rest module pof type and fail.
    // Ensure that is an server internal failure and no longer a bad request of 400.

    @Test
    public void testSetAggregationPlain()
        {
        try
            {
            super.testSetAggregationPlain();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTNamedQuery() throws Exception
        {
        try
            {
            super.testJsonPTNamedQuery();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTDirectQuery() throws Exception
        {
        try
            {
            super.testJsonPTDirectQuery();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTDeleteExisting()
        {
        try
            {
            super.testJsonPTDeleteExisting();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testBinaryPTValue() throws Exception
        {
        try
            {
            super.testBinaryPTValue();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTNamedQueryPartial()
        {
        try
            {
            super.testJsonPTNamedQueryPartial();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTMultiplierProcessingJson()
        {
        try
            {
            super.testJsonPTMultiplierProcessingJson();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testAggregationXml()
        {
        try
            {
            super.testAggregationXml();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testProcessingJson()
        {
        try
            {
            super.testProcessingJson();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTProcessing()
        {
        try
            {
            super.testJsonPTProcessing();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testBinaryPTKeyAndValue() throws Exception
        {
        try
            {
            super.testBinaryPTKeyAndValue();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testNamedQueryAggregators()
        {
        try
            {
            super.testNamedQueryAggregators();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testProcessingXml()
        {
        try
            {
            super.testProcessingXml();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testDistinctValuesXml()
        {
        try
            {
            super.testDistinctValuesXml();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testMultiplierProcessingXml()
        {
        try
            {
            super.testMultiplierProcessingXml();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testBinaryPTKeyAndValueWithImage() throws Exception
        {
        try
            {
            super.testBinaryPTKeyAndValueWithImage();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTNamedQueryAggregators()
        {
        try
            {
            super.testJsonPTNamedQueryAggregators();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testAggregationJson()
        {
        try
            {
            super.testAggregationJson();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testDistinctValuesJson()
        {
        try
            {
            super.testDistinctValuesJson();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testCustomAggregationJson()
        {
        try
            {
            super.testCustomAggregationJson();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTKeyAndValue() throws Exception
        {
        // skip for PofMisconfiguredRestTests
        }

    @Test
    public void testJsonPTNamedQueryParams() throws Exception
        {
        try
            {
            super.testJsonPTNamedQueryParams();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTValue() throws Exception
        {
        try
            {
            super.testProcessingJson();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testBinaryPTValueWithImage() throws Exception
        {
        try
            {
            super.testBinaryPTValueWithImage();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testMultiplierProcessingJson()
        {
        try
            {
            super.testMultiplierProcessingJson();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testAggregationPlain()
        {
        try
            {
            super.testAggregationPlain();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    @Test
    public void testJsonPTNamedQueryKeys()
        {
        try
            {
            super.testJsonPTNamedQueryKeys();
            }
        catch (AssertionError e)
            {
            // check that is server internal error and not status 400
            assertTrue(e.getMessage().contains("expected:<200> but was:<500>"));
            }
        }

    // ----- helpers --------------------------------------------------------

    public static void validateLogMessages(String sDir, String sServerLogFilename, String sLogContains, int cCount)
            throws IOException
        {
        File           fileServerOut = new File(sDir, sServerLogFilename);
        int            i             = 0;

        try (FileReader     fileReader = new FileReader(fileServerOut);
             BufferedReader reader     = new BufferedReader(fileReader))
            {
            String sLine = reader.readLine();

            while (sLine != null)
                {
                if (sLine.contains(sLogContains))
                    {
                    i++;
                    }
                sLine = reader.readLine();
                }
            }
        Assert.assertThat("validate more than " + cCount + " log messages containing \"" + sLogContains + "\"",
                          i, greaterThanOrEqualTo(cCount));
        }
    }