/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import com.oracle.coherence.testing.AbstractFunctionalTest;

import static org.junit.Assert.*;

/**
* A collection of functional tests for a Coherence*Extend proxy that uses
* authorized hosts addresses and try to connect to an address that is not
* authorized.
*
* @author tam  2013.10.17
*/
public class AuthorizedHostsRejectedTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AuthorizedHostsRejectedTests()
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
        startCacheServer("AuthorizedHostsRejectedTests", "extend",
                "authorized-hosts-rejected-cache-config.xml");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("AuthorizedHostsRejectedTests");
        }

    // ----- AuthorizedHostsRejectedTests tests -----------------------------

    /**
    * Test case for COH-10501.
    * Ensure that non-authorized hosts fails.
    */
    @Test
    public void connect()
        {
    	try 
    	    {
            getNamedCache("dist-extend-direct");
            // we should not get here. If it does it means that an non-authorized host failed
            // to be rejected
            assertTrue("Authorized hosts check did not work", true);
    	    }
    	catch (Exception e) 
    	    {
            // this will cause an exception due to the unauthorized host.
            // Lets validate that the JMX stats are updated
    		assertTrue("Did not get expected exception", e.getMessage().indexOf("could not establish a connection") != -1);
    	    }
        }
    
       }
