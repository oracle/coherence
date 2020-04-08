/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.util.Base;

import common.AbstractFunctionalTest;
import common.AbstractTestInfrastructure;

import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Base test class for open tracing tests.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public class AbstractTracingTest
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle ---------------------------------------------

    /**
     * Shadow this so that we have control of the cluster lifecycle without having
     * to reinvent the wheel.
     */
    @SuppressWarnings("CheckStyle")
    @BeforeClass
    public static void _startup()
        {
        }

    // ----- test methods -----------------------------------------------

    /**
     * No explicit override file which enables tracing, therefore tracing will be disabled.
     */
    @Test
    public void shouldBeDisabledByDefault()
        {
        runTest(() -> assertFalse("Tracing should NOT be enabled.", TracingHelper.isEnabled()), null);
        }

    // ----- helper methods ---------------------------------------------

    /**
     * A callback that may be implemented by test classes to perform additional actions once the initial cluster
     * has been started.
     */
    protected void onClusterStart()
        {
        }

    /**
     * Start the cluster using the provided properties and override files.
     *
     * @param props         the system properties to expose to the process starting the cluster
     * @param sOverrideXml  the override file name
     */
    @SuppressWarnings("CheckStyle")
    public void _startCluster(Properties props, String sOverrideXml)
        {
        _startCluster(props, "cache-config.xml", sOverrideXml);
        }

    /**
     * Start the cluster using the provided properties and override files.
     *
     * @param props         the system properties to expose to the process starting the cluster
     * @param sCacheConfig  the cache config file name
     * @param sOverrideXml  the override file name
     */
    public void _startCluster(Properties props, String sCacheConfig, String sOverrideXml)
        {
        if (props == null)
            {
            props = new Properties();
            }

        if (!props.containsKey("tangosol.coherence.distributed.localstorage")
            && !props.containsKey("coherence.distributed.localstorage"))
            {
            props.setProperty("tangosol.coherence.distributed.localstorage", "true");
            }
        if (sOverrideXml != null && !sOverrideXml.isEmpty())
            {
            props.setProperty("tangosol.coherence.override", sOverrideXml);
            }
        props.setProperty("test.name", m_testName.getMethodName());

        m_origProperties = (Properties) System.getProperties().clone();

        for (Map.Entry<Object, Object> entry : props.entrySet())
            {
            System.setProperty(entry.getKey().toString(), entry.getValue().toString());
            }

        AbstractTestInfrastructure._startup();
        }

    /**
     * Stop the currently running cluster.
     */
    protected void _stopCluster()
        {
        AbstractTestInfrastructure._shutdown();
        if (m_origProperties != null)
            {
            System.setProperties(m_origProperties);
            }
        }

    /**
     * The default properties to be used when starting a cluster.
     *
     * @return the default properties to be used when starting a cluster
     */
    protected Properties getDefaultProperties()
        {
        Properties props = new Properties();
        props.put("tangosol.coherence.log.level", "9");
        props.put("test.log.level", "9");
        return props;
        }

    /**
     * Run the provided {@link TestBody test}.
     *
     * @param testBody the {@link TestBody test}
     */
    protected void runTest(TestBody testBody)
        {
        runTest(testBody, getDefaultProperties(), "tracing-enabled.xml");
        }

    /**
     * Run the provided {@link TestBody test} with the cluster configured with the provided
     * override file.
     *
     * @param testBody      the {@link TestBody test}
     * @param sOverrideXml  the override file name
     */
    @SuppressWarnings("SameParameterValue")
    protected void runTest(TestBody testBody, String sOverrideXml)
        {
        runTest(testBody, getDefaultProperties(), sOverrideXml);
        }

    /**
     * Run the provided {@link TestBody test} with the cluster configured using the provided
     * {@link Properties} and override file.
     *
     * @param testBody      the {@link TestBody test}
     * @param properties    the system properties to expose to the cluster
     * @param sOverrideXml  the override file name
     */
    protected void runTest(TestBody testBody, Properties properties, String sOverrideXml)
        {
        runTest(testBody, properties, "cache-config.xml", sOverrideXml);
        }

    /**
     * Run the provided {@link TestBody test} with the cluster configured using the provided
     * {@link Properties} and override file.
     *
     * @param testBody      the {@link TestBody test}
     * @param properties    the system properties to expose to the cluster
     * @param sCacheConfig  the cache configuration file name
     * @param sOverrideXml  the override file name
     */
    protected void runTest(TestBody testBody, Properties properties, String sCacheConfig, String sOverrideXml)
        {
        try
            {
            _startCluster(properties, sCacheConfig, sOverrideXml);
            onClusterStart();
            testBody.run();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            _stopCluster();
            }
        }

    // ----- inner class: TestBody ------------------------------------------

    /**
     * {@link FunctionalInterface} for providing test body logic for execution.
     */
    @FunctionalInterface
    protected interface TestBody
        {
        /**
         * Run the test body logic.
         *
         * @throws Exception if the test throws an unexpected exception
         */
        void run() throws Exception;
        }

    // ----- data members ---------------------------------------------------

    /**
     * System properties at the time of the test invocation to be restored once the test completes.
     */
    protected Properties m_origProperties;
    }
