/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.NamedCache;

import java.io.FileNotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

/**
 * Test overriding the metrics context root path.
 *
 * @author er  2023.07.24
 */
public class PathOverrideMetricsTests
        extends SimpleMetricsTests
    {
    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        Properties props = setupProperties();

        props.put("coherence.metrics.http.path", PATH);

        CoherenceClusterMember clusterMember = startCacheServer("PathOverrideMetricsTests", "metrics", FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("PathOverrideMetricsTests");
        }

    // ----- test -----------------------------------------------------------

    /**
     * Test that 404 is returned for invalid path.
     */
    @Test
    public void testNegativePath()
            throws Exception
        {
        NamedCache cache = getNamedCache("dist-test1");
        cache.put(1, "hello");

        assertClusterSize(cache);

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "dist-test1");

        // uses the correct path
        Eventually.assertThat(invoking(this).getCacheMetric(s_nMetricsHttpPort, "Coherence.Cache.Size", tags), is(1L));

        try
            {
            m_sPath = "/metrics";
            // should fail
            getCacheMetric(s_nMetricsHttpPort, "Coherence.Cache.Size", tags);
            fail("Expected exception");
            }
        catch (FileNotFoundException e)
            {
            // expected
            }
        finally
            {
            cache.destroy();
            }
        }

    @Override
    protected String composeURL(int port)
        {
        return "http://127.0.0.1:" + port + m_sPath;
        }

    /**
     * Server side context root path override for metrics.
     */
    static final String PATH = "/coherence";

    /**
     * Client side context root path for metrics.
     */
    protected String m_sPath = PATH;
    }
