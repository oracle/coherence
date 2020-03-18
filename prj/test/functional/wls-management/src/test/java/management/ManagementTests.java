/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.tangosol.coherence.component.manageable.ModelAdapter;
import com.tangosol.coherence.component.net.management.gateway.Local;
import com.tangosol.coherence.component.net.management.model.LocalModel;
import com.tangosol.coherence.http.DefaultHttpServer;
import com.tangosol.coherence.management.RestManagement;
import com.tangosol.coherence.management.internal.VersionUtils;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;
import org.glassfish.jersey.jettison.JettisonConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2019.05.30
 */
@SuppressWarnings("unchecked")
public class ManagementTests
    {
    @BeforeClass
    public static void startServer() throws Exception
        {
        System.setProperty(ClusterName.PROPERTY, "foo");
        System.setProperty(IPv4Preferred.JAVA_NET_PREFER_IPV4_STACK, "true");

        s_dcs = DefaultCacheServer.startServerDaemon();

        Map<String, ResourceConfig> mapConfig = new HashMap<>();

        mapConfig.put(URI_ROOT, new ManagementApp());

        s_server = new DefaultHttpServer();
        s_server.setLocalAddress("0.0.0.0");
        s_server.setLocalPort(LocalPlatform.get().getAvailablePorts().next());
        s_server.setResourceConfig(mapConfig);
        s_server.start();

        CLUSTER_NAMES.forEach(ManagementTests::createFakeClusterMBean);

        s_client = ClientBuilder.newBuilder().build();
        s_baseURI = URI.create("http://127.0.0.1:" + s_server.getListenPort() + URI_ROOT + "/coherence");
        }

    @AfterClass
    public static void shutdown() throws Exception
        {
        s_client.close();
        s_server.stop();
        s_dcs.shutdownServer();
        }

    @Test
    public void shouldGetVersions() throws Exception
        {
        Response response = s_client.target(s_baseURI)
                                    .request(MediaType.APPLICATION_JSON_TYPE)
                                    .get();

        assertThat(response.getStatus(), is(200));

        Map map = s_mapper.readValue(response.readEntity(String.class), Map.class);

        assertThat(map, is(notNullValue()));
        assertThat(map.containsKey("links"), is(true));
        assertThat(map.containsKey("items"), is(true));

        List<Map> list   = (List<Map>) map.get("items");
        boolean   fHasV1 = list.stream().anyMatch(m -> VersionUtils.V1.equals(m.get("version")));

        assertThat(fHasV1, is(true));
        }

    @Test
    public void shouldGetClusters() throws Exception
        {
        Response response = s_client.target(s_baseURI)
                                    .path("latest/clusters")
                                    .request(MediaType.APPLICATION_JSON_TYPE)
                                    .get();

        assertThat(response.getStatus(), is(200));

        Map map = s_mapper.readValue(response.readEntity(String.class), Map.class);

        assertThat(map, is(notNullValue()));
        assertThat(map.containsKey("links"), is(true));
        assertThat(map.containsKey("items"), is(true));

        List list = (List<Map<String, Object>>) map.get("items");
        assertThat(list, is(notNullValue()));
        assertThat(list.size(), is(2));
        }

    @Test
    public void shouldSearch()
        {
        String sSearchJson = "{" +
                "\"links\":[],"  +
                "\"fields\":[]," +
                  "\"children\":{" +
                    "\"services\":{" +
                      "\"fields\":[\"name\",\"type\"]," +
                      "\"links\":[],"  +
                        "\"children\":{" +
                          "\"members\":{" +
                            "\"fields\":[\"name\",\"eventBacklog\",\"joinTime\"]" +
                          "}" +
                        "}" +
                      "}" +
                    "}" +
                  "}";

        Response response = s_client.target(s_baseURI)
                                    .path("latest/clusters/foo/search")
                                    .request()
                                    .post(Entity.entity(sSearchJson, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(200));
        }

    @Test
    public void shouldGetSpecificCluster() throws Exception
        {
        for (String sName : CLUSTER_NAMES)
            {
            shouldGetSpecificCluster(sName);
            }
        }

    private void shouldGetSpecificCluster(String sName) throws Exception
        {
        Response response = s_client.target(s_baseURI)
                                    .path("latest/clusters/" + sName)
                                    .request(MediaType.APPLICATION_JSON_TYPE)
                                    .get();

        assertThat(response.getStatus(), is(200));

        Map map = s_mapper.readValue(response.readEntity(String.class), Map.class);

        assertThat(map, is(notNullValue()));
        assertThat(map.containsKey("links"), is(true));
        assertThat(map.containsKey("clusterSize"), is(true));
        assertThat(map.get("clusterSize"), is(1));
        }

    // ----- helper methods -------------------------------------------------

    private static void createFakeClusterMBean( String clusterName)
        {
        Local        local = (Local) CacheFactory.ensureCluster().getManagement();
        String       sName = Registry.CLUSTER_TYPE + ",cluster=" + clusterName;
        LocalModel   model = (LocalModel) local.getLocalModels().get(Registry.CLUSTER_TYPE);
        ModelAdapter mBean = local.instantiateModelMBean(model);

        mBean.set_Model(model);

        local.registerModelMBean(sName, model);
        }

    // ----- inner class: ManagementApp -------------------------------------

    @ApplicationPath(URI_ROOT)
    public static class ManagementApp
            extends ResourceConfig
        {
        public ManagementApp()
            {
            register(JettisonConfig.DEFAULT);
            register(JettisonFeature.class);

            RestManagement.configure(this, new MBeanServerSupplier(), () -> CLUSTER_NAMES);
            }
        }

    public static class MBeanServerSupplier
            implements Supplier<MBeanServer>
        {
        @Override
        public MBeanServer get()
            {
            return MBeanHelper.findMBeanServer();
            }
        }

    // ----- constants ------------------------------------------------------

    private static final String URI_ROOT = "/wls";

    private static final Set<String> CLUSTER_NAMES = new HashSet<>(Arrays.asList("foo", "bar"));

    // ----- data members ---------------------------------------------------

    private static DefaultCacheServer s_dcs;

    private static DefaultHttpServer s_server;

    private static final ObjectMapper s_mapper = new ObjectMapper();

    private static Client s_client;

    private static URI s_baseURI;
    }
