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
import com.tangosol.net.ConfigurableQuorumPolicy;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import org.junit.Test;

import static com.tangosol.net.ConfigurableQuorumPolicy.ProxyQuorumPolicy;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 *  {@link ProxyQuorumPolicyBuilderTest} provides unit tests for {@link ProxyQuorumPolicyBuilder}s.
 *
 * @author jf 2015.02.16
 *
 * @since 12.2.1
 */
public class ProxyQuorumPolicyBuilderTest
    {
    /**
     * Ensure that we can instantiate a minimal ProxyQuorumPolicyBuilder with no xml config supplied.
     */
    @Test
    public void testMinimal()
        {
        ProxyQuorumPolicyBuilder bldr = new ProxyQuorumPolicyBuilder(2, null);
        ActionPolicy policy = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);

        assertTrue(policy instanceof ProxyQuorumPolicy);
        }

    /**
     * Ensure detection of invalid constraint.
     */
    @Test
    public void testInvalid()
        {
        final XmlElement             NULL_XML_CONFIG         = null;
        final int                    INVALID_THRESHOLD_VALUE = -1;
        ProxyQuorumPolicyBuilder bldr = new ProxyQuorumPolicyBuilder(INVALID_THRESHOLD_VALUE, NULL_XML_CONFIG);

        ActionPolicy policy = null;

        try
            {
            policy = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
            assertEquals("expected a CacheException realizing an invalid ProxyQuorumPolicyBuilder", null, policy);
            }
        catch (ConfigurationException e)
            {
            // expected
            e.printStackTrace();
            }
        }
    }
