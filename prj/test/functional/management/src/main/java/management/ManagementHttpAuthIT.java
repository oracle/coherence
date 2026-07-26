/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;
import com.tangosol.coherence.component.util.safeService.SafeProxyService;
import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.net.URI;
import java.net.URL;

import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Functional tests for management HTTP authentication defaults.
 *
 * @author jk  2026.05.14
 * @since 26.04
 */
public class ManagementHttpAuthIT
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void startup()
        {
        setupProps();
        }

    @Test
    public void shouldRejectUnauthenticatedGetInHardenedModeDefault()
            throws Exception
        {
        try (CoherenceClusterMember member = startServer("ManagementHttpAuthHardenedDefaultGet",
                props("prod", CoherenceMode.SECURITY_MODE_HARDENED, null)))
            {
            WebTarget target = target(managementUrl(member));
            try (Response response = target.request().get())
                {
                assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
                }
            }
        }

    @Test
    public void shouldRejectUnauthenticatedPostInHardenedModeDefault()
            throws Exception
        {
        try (CoherenceClusterMember member = startServer("ManagementHttpAuthHardenedDefaultPost",
                props("dev", CoherenceMode.SECURITY_MODE_HARDENED, null)))
            {
            WebTarget target = target(managementUrl(member)).path("logClusterState");
            try (Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity("{}", MediaType.APPLICATION_JSON_TYPE)))
                {
                assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
                }
            }
        }

    @Test
    public void shouldAllowBasicWithColonPasswordInHardenedMode()
            throws Exception
        {
        try (CoherenceClusterMember member = startServer("ManagementHttpAuthBasicColon",
                props("prod", CoherenceMode.SECURITY_MODE_HARDENED, null)))
            {
            WebTarget target = target(managementUrl(member));
            try (Response response = target.request()
                    .header("Authorization", "Basic " + AbstractHttpServer.toBase64("client:pass:word"))
                    .get())
                {
                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                }
            }
        }

    @Test
    public void shouldPreserveExplicitNoneWithWarning()
            throws Exception
        {
        String sName = "ManagementHttpAuthExplicitNone";
        try (CoherenceClusterMember member = startServer(sName,
                props("prod", CoherenceMode.SECURITY_MODE_HARDENED, "none")))
            {
            WebTarget target = target(managementUrl(member));
            try (Response response = target.request().get())
                {
                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                }

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
                        && line.contains(HttpAuthProperty.MANAGEMENT))
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
        CoherenceClusterMember member = startCacheServer(sName, PROJECT_NAME, null, props, true);
        Eventually.assertThat(invoking(member).isServiceRunning(HttpHelper.getServiceName()), is(true));
        return member;
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
        props.put("coherence.management", "all");
        props.put("coherence.management.http", "all");
        props.put("coherence.management.http.override-port", "0");
        props.put("com.sun.management.jmxremote.port", String.valueOf(LocalPlatform.get().getAvailablePorts().next()));
        props.put("java.security.auth.login.config", loginConfig().getAbsolutePath());
        if (sAuth != null)
            {
            props.put(HttpAuthProperty.MANAGEMENT, sAuth);
            }
        return props;
        }

    private URL managementUrl(CoherenceClusterMember member)
            throws Exception
        {
        int nPort = member.invoke(GetManagementPort.INSTANCE);
        assertThat(nPort > 0, is(true));
        return URI.create("http://127.0.0.1:" + nPort + "/management/coherence/cluster").toURL();
        }

    private WebTarget target(URL url)
        {
        Client client = ClientBuilder.newClient();
        return client.target(url.toString());
        }

    private File loginConfig()
            throws IOException
        {
        File file = File.createTempFile("management-http-auth", ".login");
        file.deleteOnExit();
        try (FileWriter writer = new FileWriter(file))
            {
            writer.write("CoherenceREST { management.ColonPasswordLoginModule required; };");
            }
        return file;
        }

    private interface HttpAuthProperty
        {
        String MANAGEMENT = "coherence.management.http.auth";
        }

    /**
     * Return the assigned management HTTP acceptor port from inside the remote member.
     */
    public enum GetManagementPort
            implements RemoteCallable<Integer>
        {
        INSTANCE;

        @Override
        public Integer call()
            {
            Cluster cluster = CacheFactory.getCluster();
            Object  service = cluster.getService(HttpHelper.getServiceName());
            if (service instanceof SafeProxyService)
                {
                service = ((SafeProxyService) service).getRunningService();
                }

            ProxyService proxyService = (ProxyService) service;
            HttpAcceptor acceptor     = (HttpAcceptor) proxyService.getAcceptor();
            return acceptor == null ? 0 : acceptor.getListenPort();
            }
        }

    private static final String PROJECT_NAME = "management";
    }
