/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.bedrock.runtime.LocalPlatform;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
* A collection of functional tests for a Coherence*Extend proxy that is
* missing a configured address.
*
* @author nsa  2009.09.28
*/
public class MissingAddressTests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public MissingAddressTests()
        {
        super("dist-extend-direct", "client-cache-config-no-named-initiator.xml");
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
            // Note: TcpAcceptor defaults to the NameService address if its
            //       acceptor-config is missing an address
            props.setProperty("test.extend.address.remote",
                    LocalPlatform.get().getLoopbackAddress().getHostAddress());
            }
        startCacheServerWithProxy("MissingAddressTests", "missing-address-cache-config.xml");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("MissingAddressTests");
        }


    // ----- AddressProvider tests ------------------------------------------

    /**
    * Connect to a Coherence*Extend proxy that is missing a configured
    * address.
    */
    @Test
    public void connect()
        {
        getNamedCache();
        }
    }
