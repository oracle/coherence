/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerProxy;

import java.util.Properties;

import javax.management.Attribute;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Base test case for validation JMX configuration of Tracing operations.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@SuppressWarnings("DuplicatedCode")
public abstract class AbstractJmxConfigurationIT
        extends AbstractTracingIT
    {
    // ----- methods from AbstractTracingTest -------------------------------

    @Override
    public void _startCluster(Properties props, String sOverrideXml)
        {
        Properties propsLocal = props == null ? new Properties() : props;

        propsLocal.put("coherence.role", "node1");
        propsLocal.put("java.net.preferIPv4Stack", "true");

        super._startCluster(propsLocal, sOverrideXml);
        }

    @Override
    protected Properties getDefaultProperties()
        {
        Properties propsDefault = super.getDefaultProperties();
        propsDefault.setProperty("coherence.management.refresh.timeout", "900ms");
        return propsDefault;
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
    @Test
    public void testDynamicTracingConfigurationOnNode()
        {
        runTest(() ->
                {
            CoherenceClusterMember member2 = startMember(2);
            MBeanServer            server  = MBeanHelper.findMBeanServer();

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);

            // enable tracing
            setTracingConfigurationForMember(server, member2, 1.0f);

            checkTracingJMXAttribute(server, member2, 1.0f);
            checkTracingStatusOnMember(member2, true);

            // disable tracing
            setTracingConfigurationForMember(server, member2, -1.0f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);

            // enable tracing
            setTracingConfigurationForMember(server, member2, 1.0f);

            checkTracingJMXAttribute(server, member2, 1.0f);
            checkTracingStatusOnMember(member2, true);

            // disable tracing
            setTracingConfigurationForMember(server, member2, -1.0f);

            checkTracingJMXAttribute(server, member2, -1.0f);
            checkTracingStatusOnMember(member2, false);
            }, "management-enabled.xml");
        }

    /**
     * Validate tracing can be dynamically enabled/disabled for <em>all</em> members.
     */
    @Test
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
    @Test
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
    @Test
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
        try
            {
            Eventually.assertDeferred(() -> member.invoke(
                (RemoteCallable<Boolean>) TracingHelper::isEnabled),
                                  is(fExpectedStatus));
            }
        catch (AssertionError e)
            {
            heapdump(member);
            throw e;
            }
        }

    /**
     * Verify the tracing ratio configuration for the provided cluster member matches the expected value.
     *
     * @param server          the {@link MBeanServer} to query
     * @param member          the {@link CoherenceClusterMember} of interest
     * @param fExpectedValue  the expected value
     */
    protected void checkTracingJMXAttribute(MBeanServer server, CoherenceClusterMember member, float fExpectedValue)
        {
        try
            {
            Eventually.assertDeferred(() -> getTracingConfigurationForMember(server, member), is(fExpectedValue));
            }
        catch (AssertionError e)
            {
            heapdump(member);
            throw e;
            }
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
     * Return the current value for the {@value #TRACING_ATTRIBUTE} JMX attribute.
     * <p>
     * NOTE: This method is public due to Bedrock {@code invoking()} method requirements.
     *
     * @param server  the {@link MBeanServer} to query
     * @param member  the {@link CoherenceClusterMember} of interest
     *
     * @return the current configuration value or {@code null} if the attribute can't be retrieved.
     */
    public Float getTracingConfigurationForMember(MBeanServer server, CoherenceClusterMember member)
        {
        try
            {
            ObjectName oBeanName = getObjectNameForNodeMBean(member);

            return (Float) server.getAttribute(oBeanName, TRACING_ATTRIBUTE);
            }
        catch (Exception e)
            {
            return Float.NaN;
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
    protected void setTracingConfigurationForMember(MBeanServer server,
                                                    CoherenceClusterMember member,
                                                    float fSamplingRatio)
            throws Exception
        {
        ObjectName oBeanName = getObjectNameForNodeMBean(member);

        server.setAttribute(oBeanName, new Attribute(TRACING_ATTRIBUTE, fSamplingRatio));
        }

    @Override
    protected CoherenceClusterMember startMember(int nId, Properties props)
        {
        CoherenceClusterMember member = super.startMember(nId, props);

        Cluster cluster = CacheFactory.ensureCluster();
        Service svcMgmt = cluster.ensureService("Management", "Invocation");

        Eventually.assertDeferred(() -> svcMgmt.getInfo().getServiceMembers().size(), is(nId));

        // verify we have the node mbean available
        String           sNodeMBeanName   = getObjectNameForNodeMBean(member).toString();
        MBeanServerProxy mBeanServerProxy = cluster.getManagement().getMBeanServerProxy();

        Eventually.assertDeferred(() -> mBeanServerProxy.isMBeanRegistered(sNodeMBeanName), is(true));

        return member;
        }

    /**
     * Return the {@link ObjectName} for the provided cluster member's node mbean.
     *
     * @param member  the cluster member for which to obtain an {@link ObjectName} instance
     *
     * @return the {@link ObjectName} for the provided cluster member's node mbean
     *
     * @throws RuntimeException if the {@link ObjectName} can't be created
     */
    protected ObjectName getObjectNameForNodeMBean(CoherenceClusterMember member)
        {
        try
            {
            return new ObjectName("Coherence:type=Node,nodeId=" + member.getLocalMemberId());
            }
        catch (MalformedObjectNameException e)
            {
            throw ensureRuntimeException(e, "Unable to create ObjectName for Node MBean");
            }
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

