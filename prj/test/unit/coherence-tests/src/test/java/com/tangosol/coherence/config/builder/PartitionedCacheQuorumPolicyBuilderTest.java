/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.ActionPolicy;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

import static com.tangosol.net.ConfigurableQuorumPolicy.PartitionedCacheQuorumPolicy;
import static junit.framework.TestCase.assertTrue;

/**
 *  {@link PartitionedCacheQuorumPolicyBuilderTest} provides unit tests for {@link PartitionedCacheQuorumPolicyBuilder}s.
 *
 * @author jf 2015.02.16
 *
 * @since 12.2.1
 */
public class PartitionedCacheQuorumPolicyBuilderTest
    {
    /**
     * Ensure that we can instantiate a minimal PartitionedCacheQuorumPolicyBuilder with no xml config supplied.
     */
    @Test
    public void testMinimal()
        {
        final AddressProviderBuilder NULL_RECOVERY_HOSTS = null;
        final XmlElement             NULL_XML_CONFIG     = null;
        final int THRESHOLD_VALUE = 2;
        PartitionedCacheQuorumPolicyBuilder bldr = new PartitionedCacheQuorumPolicyBuilder(NULL_RECOVERY_HOSTS,
                                                       NULL_XML_CONFIG);

        for (PartitionedCacheQuorumPolicy.ActionRule action : PartitionedCacheQuorumPolicy.ActionRule.values())
            {
            bldr.addQuorumRule(action.getElementName(), action.getMask(), THRESHOLD_VALUE, 0.0f);
            }

        PartitionedCacheQuorumPolicy policy = (PartitionedCacheQuorumPolicy) bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        assertTrue(policy instanceof PartitionedCacheQuorumPolicy);
        }

    /**
     * Ensure detection of invalid constraint.
     */
    @Test
    public void testInvalid()
        {
        final AddressProviderBuilder NULL_RECOVERY_HOSTS = null;
        final XmlElement NULL_XML_CONFIG = null;
        final int INVALID_THRESHOLD_VALUE = -1;
        PartitionedCacheQuorumPolicyBuilder bldr = new PartitionedCacheQuorumPolicyBuilder(NULL_RECOVERY_HOSTS,
                NULL_XML_CONFIG);

        for (PartitionedCacheQuorumPolicy.ActionRule action : PartitionedCacheQuorumPolicy.ActionRule.values())
            {
            bldr.addQuorumRule(action.getElementName(), action.getMask(), INVALID_THRESHOLD_VALUE, 0.0f);
            }

        ActionPolicy policy = null;

        try
            {
            policy = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
            assertEquals("expected a CacheException realizing an invalid PartitionedCacheQuorumPolicyBuilder", null, policy);
            }
        catch (ConfigurationException e)
            {
            // expected
            e.printStackTrace();
            }
        }
    }
