/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.discovery.NSLookup;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.Coherence;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;

import java.util.Collection;
import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Functional tests for metrics HTTP authentication defaults.
 *
 * @author jk  2026.05.14
 * @since 26.07
 */
public class MetricsAuthIT
        extends AbstractMetricsFunctionalTest
    {
    @Test
    public void shouldRejectUnauthenticatedScrapeInProdDefault()
            throws Exception
        {
        try (CoherenceClusterMember member = startServer("MetricsAuthProdDefault", props("prod", null)))
            {
            HttpURLConnection con = connection(metricsUrl(member), null);
            assertThat(con.getResponseCode(), is(HttpURLConnection.HTTP_UNAUTHORIZED));
            }
        }

    @Test
    public void shouldAllowBasicWithColonPassword()
            throws Exception
        {
        try (CoherenceClusterMember member = startServer("MetricsAuthBasicColon", props("dev", null)))
            {
            HttpURLConnection con = connection(metricsUrl(member), "client:pass:word");
            assertThat(con.getResponseCode(), is(HttpURLConnection.HTTP_OK));
            }
        }

    @Test
    public void shouldPreserveExplicitNoneWithWarning()
            throws Exception
        {
        String sName = "MetricsAuthExplicitNone";
        try (CoherenceClusterMember member = startServer(sName, props("prod", "none")))
            {
            HttpURLConnection con = connection(metricsUrl(member), null);
            assertThat(con.getResponseCode(), is(HttpURLConnection.HTTP_OK));

            File fileLog = new File(ensureOutputDir(PROJECT_NAME), sName + ".out");
            Eventually.assertThat(invoking(this).logContainsAuthNoneWarning(fileLog), is(true));
            }
        }

    public boolean logContainsAuthNoneWarning(File fileLog)
            throws IOException
        {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileLog)))
            {
            String line;
            while ((line = reader.readLine()) != null)
                {
                if (line.contains("HTTP auth is disabled")
                        && line.contains(HttpAuthProperty.METRICS))
                    {
                    return true;
                    }
                }
            }
        return false;
        }

    private CoherenceClusterMember startServer(String sName, Properties props)
        {
        CoherenceClusterMember member = AbstractTestInfrastructure.startCacheServer(sName, PROJECT_NAME,
                FILE_SERVER_CFG_CACHE, props, true, null, Coherence.class);
        Eventually.assertThat(invoking(member).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        return member;
        }

    private URL metricsUrl(CoherenceClusterMember member)
            throws Exception
        {
        int nPort = Integer.getInteger("test.multicast.port");
        Collection<URL> urls = NSLookup.lookupHTTPMetricsURL(new InetSocketAddress("127.0.0.1", nPort));
        assertThat(urls.size(), is(1));
        return urls.iterator().next();
        }

    private HttpURLConnection connection(URL url, String sCredentials)
            throws IOException
        {
        HttpURLConnection con = (HttpURLConnection) URI.create(url.toString()).toURL().openConnection();
        con.setRequestProperty("Accept", "application/json");
        if (sCredentials != null)
            {
            con.setRequestProperty("Authorization", "Basic " + AbstractHttpServer.toBase64(sCredentials));
            }
        return con;
        }

    private Properties props(String sMode, String sAuth)
            throws IOException
        {
        Properties props = new Properties();
        props.put("coherence.mode", sMode);
        props.put(MetricsHttpHelper.PROP_METRICS_ENABLED, "true");
        props.put("coherence.metrics.http.port", "0");
        props.put("coherence.management.extendedmbeanname", "true");
        props.put("java.security.auth.login.config", loginConfig().getAbsolutePath());
        if (sAuth != null)
            {
            props.put(HttpAuthProperty.METRICS, sAuth);
            }
        return props;
        }

    private File loginConfig()
            throws IOException
        {
        File file = File.createTempFile("metrics-http-auth", ".login");
        file.deleteOnExit();
        try (FileWriter writer = new FileWriter(file))
            {
            writer.write("CoherenceREST { metrics.ColonPasswordLoginModule required; };");
            }
        return file;
        }

    private interface HttpAuthProperty
        {
        String METRICS = "coherence.metrics.http.auth";
        }

    private static final String PROJECT_NAME = "metrics";
    private static final String FILE_SERVER_CFG_CACHE = "coherence-cache-config.xml";
    }
