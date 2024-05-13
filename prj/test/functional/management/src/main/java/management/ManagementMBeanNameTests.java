/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.discovery.NSLookup;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;

import com.tangosol.net.management.MBeanAccessor;
import com.tangosol.net.management.MBeanServerProxy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tangosol.internal.management.resources.AbstractManagementResource.SERVICES;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test is for the case where Coherence is running in a JVM alongside "products" that
 * introduce their own MBeans outside the Coherence domain, but with the same key/value
 * pairs in the ObjectNames. For example, in the test below we create a bean with "type=Service"
 * in the name. If Management over REST is not using the correct domain then it will pick up
 * the custom MBean and this will cause an error.
 *
 * This test also overrides the domain used in the Coherence MBean names to ensure that the
 * correct domain is used to discover MBeans.
 */
public class ManagementMBeanNameTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        AvailablePortIterator ports = new AvailablePortIterator(35000);

        System.setProperty("coherence.cluster", "Test");
        System.setProperty("coherence.member", "Storage");
        System.setProperty("coherence.management.http", "all");
        System.setProperty("coherence.management.http.override-port", String.valueOf(ports.next()));
        System.setProperty("coherence.management.extendedmbeanname", "true");
        System.setProperty("coherence.management.port", String.valueOf(ports.next()));

        CacheFactory.ensureCluster();
        s_coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);

        s_client = ClientBuilder.newBuilder().build();

        registerMBean();
        }

    @AfterClass
    public static void cleanup()
        {
        s_coherence.close();
        s_client.close();
        }

    @Test
    public void shouldGetServicesWhenCustomMBeanRegistered()
        {
        MBeanServerProxy                 proxy    = CacheFactory.ensureCluster().getManagement().getMBeanServerProxy();
        MBeanAccessor                    accessor = new MBeanAccessor(proxy);
        MBeanAccessor.QueryBuilder       query    = new MBeanAccessor.QueryBuilder().withBaseQuery("Test:type=Service");
        Map<String, Map<String, Object>> map      = accessor.getAttributes(query.build());

        // We should find the custom MBean
        assertThat(map.size(), is(1));

        // The Management over REST request should not break with the custom MBean present
        WebTarget target  = getBaseTarget().path(SERVICES);
        Response response = target.request().get();
        assertThat(response.getStatus(), is(200));
        }

    // ----- helper methods -------------------------------------------------

    private static void registerMBean() throws Exception
        {
        MBeanServer mbs   = ManagementFactory.getPlatformMBeanServer();
        ObjectName  name  = new ObjectName("Test:type=Service");
        Custom      mbean = new Custom();
        mbs.registerMBean(mbean, name);
        }

    public WebTarget getBaseTarget()
        {
        return getBaseTarget(s_client);
        }

    public WebTarget getBaseTarget(Client client)
        {
        try
            {
            if (m_baseURI == null)
                {
                int nPort = s_coherence.getCluster().getDependencies().getLocalPort();
                m_baseURI = NSLookup.lookupHTTPManagementURL(new InetSocketAddress("127.0.0.1", nPort)).iterator().next().toURI();

                Logger.info("Management HTTP Acceptor lookup returned: " + m_baseURI);
                }
            return client.target(m_baseURI);
            }
        catch(IOException | URISyntaxException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    // ----- inner interface: CustomMBean -----------------------------------

    /**
     * A basic custom MBean interface.
     */
    public interface CustomMBean
        {
        /**
         * A simple custom MBean attribute.
         *
         * @return the numeric value
         */
        int getNumber();
        }

    // ----- inner class: CustomMBean -----------------------------------

    /**
     * A basic custom MBean.
     */
    public static class Custom
            implements CustomMBean
        {
        @Override
        public int getNumber()
            {
            return 19;
            }
        }

    // ----- data members ---------------------------------------------------

    private static Coherence s_coherence;

    private static Client s_client;

    private URI m_baseURI;
    }
