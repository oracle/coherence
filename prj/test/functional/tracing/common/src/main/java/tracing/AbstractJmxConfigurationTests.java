/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;

import java.util.Properties;

import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.within;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Base test case for validation JMX configuration of Tracing operations.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@SuppressWarnings("DuplicatedCode")
public abstract class AbstractJmxConfigurationTests
        extends AbstractTracingTest
    {
    // ----- methods from AbstractTracingTest -------------------------------

    @Override
    public void _startCluster(Properties props, String sOverrideXml)
        {
        Properties propsLocal = props == null ? new Properties() : props;

        propsLocal.put("tangosol.coherence.role", "node1");

        super._startCluster(propsLocal, sOverrideXml);
        }

    // ----- test methods ---------------------------------------------------

    @Override
    @Ignore
    @Test
    public void shouldBeDisabledByDefault()
        {
        }

    /**
     * Validate tracing can be dynamically enabled/disabled on a per-member basis.
     */
    public void testDynamicTracingConfigurationOnNode()
        {
        runTest(() ->
            {
            CoherenceClusterMember member2 = startMember(2);
            MBeanServer            server  = MBeanHelper.findMBeanServer();

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);

            // enable tracing
            setTracingConfigurationForNode(server, member2, 1.0f);

            checkTracingJMXAttribute(server, member2, 1.0f);
            checkTracingStatusOnMember(member2, true);

            // disable tracing
            setTracingConfigurationForNode(server, member2, -1.0f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);

            // enable tracing
            setTracingConfigurationForNode(server, member2, 1.0f);

            checkTracingJMXAttribute(server, member2, 1.0f);
            checkTracingStatusOnMember(member2, true);

            // disable tracing
            setTracingConfigurationForNode(server, member2, -1.0f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);
            }, "management-enabled.xml");
        }

    /**
     * Validate tracing can be dynamically enabled/disabled for <em>all</em> members.
     */
    public void testDynamicTracingConfigurationClusterWide()
        {
        runTest(() ->
            {
            CoherenceClusterMember member2 = startMember(2);
            CoherenceClusterMember member3 = startMember(3);
            MBeanServer            server  = MBeanHelper.findMBeanServer();

            // initial state
            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingJMXAttribute(server, member3, -1.0f);
            checkTracingStatusOnMember(member2, false);
            checkTracingStatusOnMember(member3, false);

            // enable tracing
            updateTracingConfigurationForCluster(server, null, 1.0f);

            checkTracingJMXAttribute(server, member2, 1.0f);
            checkTracingJMXAttribute(server, member3, 1.0f);
            checkTracingStatusOnMember(member2, true);
            checkTracingStatusOnMember(member3, true);

            // disable tracing
            updateTracingConfigurationForCluster(server, null, -1.0f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingJMXAttribute(server, member3, -1.0f);
            checkTracingStatusOnMember(member2, false);
            checkTracingStatusOnMember(member3, false);
            }, "management-enabled.xml");
        }

    /**
     * Validate tracing can be dynamically enabled/disabled for members within a specific role.
     */
    public void testDynamicTracingConfigurationClusterWideWithRoles()
        {
        runTest(() ->
            {
            CoherenceClusterMember member2 = startMember(2);
            CoherenceClusterMember member3 = startMember(3);
            MBeanServer            server  = MBeanHelper.findMBeanServer();

            // initial state
            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingJMXAttribute(server, member3, -1.0f);
            checkTracingStatusOnMember(member2, false);
            checkTracingStatusOnMember(member3, false);

            // enable tracing
            updateTracingConfigurationForCluster(server, "node2", 1.0f);

            checkTracingJMXAttribute(server, member2, 1.0f);
            checkTracingJMXAttribute(server, member3, -1.0f);
            checkTracingStatusOnMember(member2, true);
            checkTracingStatusOnMember(member3, false);

            // disable tracing
            updateTracingConfigurationForCluster(server, "node2", -1.0f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingJMXAttribute(server, member3, -1.0f);
            checkTracingStatusOnMember(member2, false);
            checkTracingStatusOnMember(member3, false);
            }, "management-enabled.xml");
        }

    /**
     * Validate tracing ration value boundary handling.
     * @throws Exception if an error occurs during testing
     */
    public void testTracingRatioBounds() throws Exception
        {
        runTest(() ->
            {
            CoherenceClusterMember member2 = startMember(2);

            MBeanServer server = MBeanHelper.findMBeanServer();

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);

            // upper bound
            updateTracingConfigurationForCluster(server, null, 100.0f);

            checkTracingJMXAttribute(server, member2, 1.0f);
            checkTracingStatusOnMember(member2, true);

            // check values between can be set
            updateTracingConfigurationForCluster(server, null, 0.5f);

            checkTracingJMXAttribute(server, member2, 0.5f);
            checkTracingStatusOnMember(member2, true);

            // lower bound
            updateTracingConfigurationForCluster(server, null, -100.0f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);

            // check lower bound between -1.0 and zero
            updateTracingConfigurationForCluster(server, null, -0.5f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);

            // null value
            try
                {
                updateTracingConfigurationForCluster(server, null, null);
                }
            catch (MBeanException me)
                {
                assertThat(me.getCause(), instanceOf(IllegalArgumentException.class));
                }

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);
            }, "management-enabled.xml");
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Verify the result of invoking {@link TracingHelper#isEnabled()} on the specified member returns
     * the expected result.
     *
     * @param member          the target {@link CoherenceClusterMember}
     * @param fExpectedStatus  the expected result from the remove {@link TracingHelper#isEnabled()} call
     */
    protected void checkTracingStatusOnMember(CoherenceClusterMember member, boolean fExpectedStatus)
        {
        Eventually.assertDeferred(() -> member.invoke(
                (RemoteCallable<Boolean>) TracingHelper::isEnabled),
                is(fExpectedStatus),
                within(10, TimeUnit.SECONDS));
        }

    /**
     * Verify the tracing ratio configuration for the provided cluster member matches the expected value.
     *
     * @param server         the {@link MBeanServer} to query
     * @param member         the {@link CoherenceClusterMember} of interest
     * @param fExpectedValue  the expected value
     *
     * @throws Exception if an unexpected error occurs
     */
    protected void checkTracingJMXAttribute(MBeanServer server, CoherenceClusterMember member, float fExpectedValue)
            throws Exception
        {
        Eventually.assertDeferred(() -> getTracingConfigurationForMember(server, member.getLocalMemberId()),
                              is(fExpectedValue),
                              within(10, TimeUnit.SECONDS));
        }

    /**
     * Update the tracing configuration at the cluster-level via JMX.
     *
     * @param server         the {@link MBeanServer}
     * @param sRole          the member roles the changes should be scoped to
     * @param fTracingRatio  the new tracing ratio
     *
     * @throws Exception if an unexpected error occurs
     */
    protected void updateTracingConfigurationForCluster(MBeanServer server, String sRole, Float fTracingRatio)
            throws Exception
        {
        server.invoke(CLUSTER_OBJECT_NAME,
                      "configureTracing",
                      new Object[] {sRole, fTracingRatio},
                      new String[] {"java.lang.String", "java.lang.Float"});
        }

    /**
     * Start a second member for the cluster to ensure cluster-wide configuration of tracing.
     * {@code nId} values should be monotonically increasing from {@code 2}.
     *
     * @param nId  the node ID to start.
     *
     * @return the started {@link CoherenceClusterMember}
     *
     * @throws IllegalArgumentException if {@code nId} is {@code 1}
     */
    protected CoherenceClusterMember startMember(int nId)
        {
        if (nId == 1)
            {
            throw new IllegalArgumentException();
            }
        Cluster cluster = CacheFactory.ensureCluster();

        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.role", "node" + nId);
        propsMain.putAll(getDefaultProperties());

        assertTrue(cluster.isRunning());
        assertEquals("cluster already exists", nId - 1, cluster.getMemberSet().size());

        CoherenceClusterMember clusterMember =
                startCacheServer(m_testName.getMethodName() + "-member" + nId, "jaeger", null, propsMain);

        Eventually.assertDeferred(clusterMember::getClusterSize, is(nId));

        return clusterMember;
        }

    /**
     * Return the current value for the {@value #TRACING_ATTRIBUTE} JMX attribute.
     * <p>
     * NOTE: This method is public due to Bedrock {@code invoking()} method requirements.
     *
     * @param server    the {@link MBeanServer} to query
     * @param memberId  member ID
     *
     * @return the current configuration value or {@code null} if the attribute can't be retrieved.
     *
     * @throws Exception if an error occurs processing the test
     */
    public Float getTracingConfigurationForMember(MBeanServer server, int memberId)
        {
        try
            {
            ObjectName  oBeanName = new ObjectName("Coherence:type=Node,nodeId=" + memberId);

            return (Float) server.getAttribute(oBeanName, TRACING_ATTRIBUTE);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Set the configuration value for the {@value #TRACING_ATTRIBUTE} JMX attribute.
     *
     * @param server          the {@link MBeanServer} to query
     * @param member          the {@link CoherenceClusterMember} of interest
     * @param fSamplingRatio  new sampling ration
     *
     * @throws Exception if an error occurs processing the test
     */
    @SuppressWarnings("SameParameterValue")
    protected void setTracingConfigurationForNode(MBeanServer server,
                                                  CoherenceClusterMember member,
                                                  float fSamplingRatio)
            throws Exception
        {
        ObjectName  oBeanName = new ObjectName("Coherence:type=Node,nodeId=" + member.getLocalMemberId());

        server.setAttribute(oBeanName, new Attribute(TRACING_ATTRIBUTE, fSamplingRatio));
        }

    // ----- data members ---------------------------------------------------

    /**
     * Constant for the {@code TracingSamplingRatio} attribute.
     */
    protected static final String TRACING_ATTRIBUTE = "TracingSamplingRatio";

    /**
     * JMX {@link ObjectName} {@code Coherence:type=Cluster}.
     */
    protected static final ObjectName CLUSTER_OBJECT_NAME;
    static
        {
        try
            {
            CLUSTER_OBJECT_NAME = new ObjectName("Coherence:type=Cluster");
            }
        catch (MalformedObjectNameException e)
            {
            throw new IllegalStateException("Somehow Coherence:type=Cluster is now an invalid ObjectName");
            }
        }
    }

