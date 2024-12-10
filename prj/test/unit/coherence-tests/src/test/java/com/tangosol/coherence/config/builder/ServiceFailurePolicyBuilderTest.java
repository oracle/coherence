/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.testing.TestHelper;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy;

import com.tangosol.net.ServiceFailurePolicy;

import com.tangosol.run.xml.XmlElement;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 *  {@link ServiceFailurePolicyBuilderTest} provides unit tests for {@link ServiceFailurePolicyBuilder}s.
 *
 * @author jf 2015.02.16
 *
 * @since 12.2.1
 */
public class ServiceFailurePolicyBuilderTest
    {
    /**
     * Test {@link ServiceFailurePolicyBuilder} constructor used for defaulting within program.
     */
    @Test
    public void testDefaultsNoXml()
        {
        final ParameterResolver RESOLVER = new NullParameterResolver();
        ServiceFailurePolicyBuilder builder =
            new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER);
        ServiceFailurePolicy policy = builder.realize(RESOLVER, null, null);

        assertTrue(policy instanceof DefaultServiceFailurePolicy);
        assertEquals(DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER,
                     ((DefaultServiceFailurePolicy) policy).getPolicyType());

        builder = new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS);
        policy = builder.realize(RESOLVER, null, null);
        assertTrue(policy instanceof DefaultServiceFailurePolicy);
        assertEquals(DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS,
                ((DefaultServiceFailurePolicy) policy).getPolicyType());

        builder = new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_LOGGING);
        policy = builder.realize(RESOLVER, null, null);
        assertTrue(policy instanceof DefaultServiceFailurePolicy);
        assertEquals(DefaultServiceFailurePolicy.POLICY_LOGGING,
                ((DefaultServiceFailurePolicy) policy).getPolicyType());
        }

    /**
     * Test {@link ServiceFailurePolicyBuilder} constructor used for defaults in cache-config.
     */
    @Test
    public void testDefaultsCacheConfig()
        {
        final ParameterResolver RESOLVER = new NullParameterResolver();
        XmlElement xmlConfig = null;

        ServiceFailurePolicyBuilder builder =
                new ServiceFailurePolicyBuilder("exit-cluster", xmlConfig);
        ServiceFailurePolicy policy = builder.realize(RESOLVER, null, null);

        assertTrue(policy instanceof DefaultServiceFailurePolicy);
        assertEquals(DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER,
                ((DefaultServiceFailurePolicy) policy).getPolicyType());

        builder = new ServiceFailurePolicyBuilder("exit-process", xmlConfig);
        policy = builder.realize(RESOLVER, null, null);
        assertTrue(policy instanceof DefaultServiceFailurePolicy);
        assertEquals(DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS,
                ((DefaultServiceFailurePolicy) policy).getPolicyType());

        builder = new ServiceFailurePolicyBuilder("logging", xmlConfig);
        policy = builder.realize(RESOLVER, null, null);
        assertTrue(policy instanceof DefaultServiceFailurePolicy);
        assertEquals(DefaultServiceFailurePolicy.POLICY_LOGGING,
                ((DefaultServiceFailurePolicy) policy).getPolicyType());
        }

    /**
     * Test invalid default.
     */
    @Test(expected = ConfigurationException.class)
    public void testInvalidDefaultsCacheConfig()
        {
        final ParameterResolver RESOLVER = new NullParameterResolver();
        XmlElement xmlConfig = TestHelper.parseXmlString("<service-failure-policy>invalid-service-failure-policy-type</service-failure-policy>");

        ServiceFailurePolicyBuilder builder =
                new ServiceFailurePolicyBuilder("invalid-service-failure-policy-type", xmlConfig);
        builder.realize(RESOLVER, null, null);
        }
    }
