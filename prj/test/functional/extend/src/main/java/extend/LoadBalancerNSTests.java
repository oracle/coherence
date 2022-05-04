/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.tangosol.net.Member;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;


/**
 * A collection of functional tests for the Coherence*Extend load balancing
 * policies with name service configuration.
 *
 * @author wl  2012.04.11
 */
public class LoadBalancerNSTests
        extends AbstractLoadBalancerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public LoadBalancerNSTests()
        {
        super("client-cache-config-lb-nameservice.xml");
        m_configDefault = "server-cache-config-lb-ns-default.xml";
        m_configProxy   = "server-cache-config-lb-ns-proxy.xml";
        m_configCustom  = "server-cache-config-lb-ns-custom.xml";
        }


    // ------- AbstractLoadBalancerTests  methods ---------------------------

    /**
    * {@inheritDoc}
    */
    @Test
    public void testClient()
        {
        // disabled as NameService proxy server dynamic lookup is random
        }

    /**
    * {@inheritDoc}
    */
    @Test
    public void testGreedy()
        {
        // disabled as NameService proxy server dynamic lookup is random
        }

    /**
    * {@inheritDoc}
    */
    void setPortBefore1(Properties props) {}

    /**
    * {@inheritDoc}
    */
    void setPortBefore2(Properties props) {}

    /**
    * {@inheritDoc}
    */
    void setPortAfter(String sServerName) {}
    }
