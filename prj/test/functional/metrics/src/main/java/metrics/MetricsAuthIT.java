/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;
import com.tangosol.coherence.component.util.safeService.SafeProxyService;
import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Functional tests for metrics HTTP authentication defaults.
 *
 * @author jk  2026.05.14
 * @since 26.04
 */
public class MetricsAuthIT
        extends AbstractMetricsFunctionalTest
    {
    @Test
    public void shouldRejectUnauthenticatedScrapeInHardenedModeDefault()
            throws Exception
        {
        try (CoherenceClusterMember member = startServer("MetricsAuthHardenedDefault",
                props("prod", CoherenceMode.SECURITY_MODE_HARDENED, null)))
            {
            HttpURLConnection con = connection(metricsUrl(member), null);
            assertThat(con.getResponseCode(), is(HttpURLConnection.HTTP_UNAUTHORIZED));
            }
        }

    @Test
    public void shouldAllowBasicWithColonPasswordInHardenedMode()
            throws Exception
        {
        try (CoherenceClusterMember member = startServer("MetricsAuthBasicColon",
                props("prod", CoherenceMode.SECURITY_MODE_HARDENED, null)))
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
        try (CoherenceClusterMember member = startServer(sName,
                props("prod", CoherenceMode.SECURITY_MODE_HARDENED, "none")))
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
        props.put("coherence.cluster", sName + "-" + System.nanoTime());
        CoherenceClusterMember member = AbstractTestInfrastructure.startCacheServer(sName, PROJECT_NAME,
                FILE_SERVER_CFG_CACHE, props, true, null, Coherence.class);
        Eventually.assertThat(invoking(member).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        return member;
        }

    private URL metricsUrl(CoherenceClusterMember member)
            throws Exception
        {
        int nPort = member.invoke(GetMetricsPort.INSTANCE);
        assertThat(nPort > 0, is(true));
        return URI.create("http://127.0.0.1:" + nPort + "/metrics").toURL();
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
        return props(sMode, null, sAuth);
        }

    private Properties props(String sMode, String sSecurityMode, String sAuth)
            throws IOException
        {
        Properties props = new Properties();
        props.put("coherence.mode", sMode);
        if (sSecurityMode != null)
            {
            props.put(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
            }
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

    /**
     * Return the assigned metrics HTTP acceptor port from inside the remote member.
     */
    public enum GetMetricsPort
            implements RemoteCallable<Integer>
        {
        INSTANCE;

        @Override
        public Integer call()
            {
            Cluster cluster = CacheFactory.getCluster();
            Object  service = cluster.getService(MetricsHttpHelper.getServiceName());
            if (service instanceof SafeProxyService)
                {
                service = ((SafeProxyService) service).getRunningService();
                }

            ProxyService proxyService = (ProxyService) service;
            HttpAcceptor acceptor     = (HttpAcceptor) proxyService.getAcceptor();
            return acceptor == null ? 0 : acceptor.getListenPort();
            }
        }

    private static final String PROJECT_NAME = "metrics";
    private static final String FILE_SERVER_CFG_CACHE = "coherence-cache-config.xml";
    }
