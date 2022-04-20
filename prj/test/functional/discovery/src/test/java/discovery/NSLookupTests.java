/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package discovery;

import com.oracle.bedrock.deferred.Deferred;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.deferred.PermanentlyUnavailableException;
import com.oracle.bedrock.deferred.TemporarilyUnavailableException;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.tangosol.discovery.NSLookup;

import com.tangosol.util.Base;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Tests for NSLookup helper class.
 *
 * @author bb  2014.04.15
 */
public class NSLookupTests
    {
    /**
     * Test the behavior of {@link NSLookup#lookupJMXServiceURL(java.net.SocketAddress)} ()},
     */
    @Test
    public void testLookup()
        {
        final LocalPlatform         platform     = LocalPlatform.get();
        final String                hostName     = platform.getLoopbackAddress().getHostAddress();
        final AvailablePortIterator ports        = platform.getAvailablePorts();
        final int                   nClusterPort = ports.next();

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder();

        clusterBuilder.include(1,
                               CoherenceCacheServer.class,
                               DisplayName.of("NSLookup"),
                               ClusterName.of("NSLookup"),
                               OperationalOverride.of("common/tangosol-coherence-override.xml"),
                               SystemProperty.of("test.multicast.address",
                                                 AbstractFunctionalTest.generateUniqueAddress(true)),
                               SystemProperty.of("test.multicast.port", nClusterPort),
                               SystemProperty.of("coherence.management", "dynamic"),
                               JmxProfile.enabled(),
                               JmxProfile.authentication(false),
                               JmxProfile.hostname(hostName)
                               );

        try (CoherenceCluster testCluster = clusterBuilder.build())
            {
            // ensure the clusters are established
            Eventually.assertThat(invoking(testCluster).getClusterSize(), is(1));

            Deferred<JMXServiceURL> deferedJmxURL = new Deferred<JMXServiceURL>()
                {
                public JMXServiceURL get()
                        throws TemporarilyUnavailableException, PermanentlyUnavailableException
                    {
                    if (m_jmxServiceURL == null)
                        {
                        try
                            {
                            m_jmxServiceURL = NSLookup.lookupJMXServiceURL(
                                    new InetSocketAddress(hostName, nClusterPort));
                            }
                        catch (IOException ioe)
                            {
                            throw new TemporarilyUnavailableException(this, ioe);
                            }
                        }
                    return m_jmxServiceURL;
                    }

                public Class<JMXServiceURL> getDeferredClass()
                    {
                    return null;
                    }

                protected JMXServiceURL m_jmxServiceURL;
                };

            int i = 0;
            while (deferedJmxURL.get() == null)
                {
                Thread.sleep(1000);
                if (i++ > 5)
                    {
                        break;
                    }
                }
            if (deferedJmxURL.get() == null)
                {
                fail("lookup failed: deferedJmxURL is NULL");
                }

            JMXServiceURL         jmxServiceURL = deferedJmxURL.get();
            JMXConnector          jmxConnector  = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection conn          = jmxConnector.getMBeanServerConnection();

            Set clusterSet = conn.queryNames(new javax.management.ObjectName("Coherence:type=Cluster,*"), null);

            assertNotNull(clusterSet);
            assertEquals(1, clusterSet.size());
            }
        catch (Exception e)
            {
            fail("lookup failed " + Base.printStackTrace(e));
            }
        }
    }
