/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import java.net.HttpURLConnection;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

/**
 * Functional tests for prometheus metrics end point over SSL.
 *
 * @author lh  2019.02.19
 * @since 12.2.1.4.0
 */
public class SimpleMetricsSSLTests
        extends AbstractMetricsTests
    {
    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        Properties props = setupProperties();
        props.put("coherence.metrics.http.provider", "mySSLProvider");
        props.put("coherence.security.keystore", "file:server.jks");
        props.put("coherence.security.store.password", "password");
        props.put("coherence.security.key.password", "private");
        props.put("coherence.security.truststore", "file:trust-server.jks");
        props.put("coherence.security.trust.password", "password");

        CoherenceClusterMember clusterMember = startCacheServer("SimpleMetricsSSLTests", "metrics", FILE_SERVER_CFG_CACHE, props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
        }

    @BeforeClass
    @SuppressWarnings("deprecation")
    public static void setupSSL()
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-client.xml", null);

        s_sslSocketFactory = new SSLSocketProvider(
                new LegacyXmlSSLSocketProviderDependencies(xml)).getDependencies().getSSLContext().getSocketFactory();
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("SimpleMetricsSSLTests");
        }

    @Override
    protected String composeURL(int port)
        {
        return "https://127.0.0.1:" + port + "/metrics";
        }

    @Override
    protected void modifyConnection(HttpURLConnection con)
        {
        HttpsURLConnection httpsCon = (HttpsURLConnection) con;

        httpsCon.setSSLSocketFactory(s_sslSocketFactory);
        httpsCon.setHostnameVerifier(new HostnameVerifier()
            {
            @Override
            public boolean verify(String s, SSLSession sslSession)
                {
                return true;
                }
            });
        }

    // ----- data members ----------------------------------------------------

    /**
     * The SSL socket provider for the HTTPS client.
     */
    private static SSLSocketFactory s_sslSocketFactory;
    }
