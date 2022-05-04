/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
* A collection of functional tests for a Coherence*Extend proxy that uses
* authorized hosts addresses.
*
* @author lsho  2011.09.28
*/
public class AuthorizedHostsTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AuthorizedHostsTests()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember clusterMember = startCacheServer("AuthorizedHostsTests", "extend",
                                                "authorized-hosts-cache-config.xml");
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("AuthorizedHostsTests");
        }

    // ----- AddressProvider tests ------------------------------------------

    /**
    * Test case for COH-5725.
    * Connect to a Coherence*Extend proxy that has <authorized-hosts>
    * configured with an unknown host.
    */
    @Test
    public void connect()
        {
        getNamedCache("dist-extend-direct");
        }
    }
