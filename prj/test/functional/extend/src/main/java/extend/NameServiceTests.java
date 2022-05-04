/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.tangosol.net.Member;

/**
* A collection of functional tests for Coherence*Extend that use the
* name service.
*
* @author wl  2012.2.24
*/
public class NameServiceTests
        extends AbstractExtendTests
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public NameServiceTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT, "client-cache-config-nameservice.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("NameServiceTests", "extend", "server-cache-config-nameservice.xml");
        Member member = findCacheServer("NameServiceTests");
        System.setProperty("test.extend.port", String.valueOf(member.getPort()));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NameServiceTests");
        }
    }