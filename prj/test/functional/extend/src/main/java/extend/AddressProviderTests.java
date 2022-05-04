/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;


/**
* A collection of AddressProvider functional tests for Coherence*Extend.
*
* @author nsa  2009.09.28
*/
public class AddressProviderTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AddressProviderTests()
        {
        super("client-cache-config-no-named-initiator.xml");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        Properties props = System.getProperties();
        if (!props.containsKey("test.extend.address.remote"))
            {
            props.setProperty("test.extend.address.remote", LocalPlatform.get().getLoopbackAddress().getHostAddress());
            }
        props.setProperty("test.extend.port", "32000");

        CoherenceClusterMember clusterMember = startCacheServer("AddressProviderTests", "extend",
                "address-provider-cache-config.xml");
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("AddressProviderTests");
        }


    // ----- AddressProvider tests ------------------------------------------

    /**
    * Connect to a Coherence*Extend proxy that uses an AddressProvider.
    */
    @Test
    public void connect()
        {
        sleep(300);
        getNamedCache("dist-extend-direct");
        }
    }
