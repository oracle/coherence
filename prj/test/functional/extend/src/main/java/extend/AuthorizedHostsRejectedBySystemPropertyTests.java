/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import java.util.Properties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import static org.junit.Assert.*;

/**
 * A collection of functional tests for a Coherence*Extend proxy that uses
 * system property coherence.extend.authorized hosts addresses and try to connect to an address that is not
 * authorized.
 *
 * @author jf  2025.04.17
 */
public class AuthorizedHostsRejectedBySystemPropertyTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public AuthorizedHostsRejectedBySystemPropertyTests()
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
        Properties props = new Properties();

        props.put("coherence.extend.authorized.hosts", "baddomain.bad,2.3.4.5");
        startCacheServer("AuthorizedHostsRejectedBySystemPropertyTests", "extend",
                         "authorized-hosts-cache-config-sysprop.xml", props);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("AuthorizedHostsRejectedBySystemPropertyTests");
        }

    // ----- AuthorizedHostsRejectedBySystemPropertyTests tests -----------------------------

   /**
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
            fail("Authorized hosts check did not work");
    	    }
    	catch (Exception e) 
    	    {
            // this will cause an exception due to the unauthorized host.
    		assertTrue("Did not get expected exception", e.getMessage().indexOf("could not establish a connection") != -1);
    	    }
        }
    }
