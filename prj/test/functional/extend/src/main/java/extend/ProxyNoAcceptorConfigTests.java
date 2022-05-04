/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.net.Member;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
* A collection of functional tests for Coherence*Extend that use the
* name service without specifying acceptor configuration.
*
* @author lh  2013.07.08
*/
public class ProxyNoAcceptorConfigTests
        extends AbstractExtendTests
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ProxyNoAcceptorConfigTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT, "client-cache-config-no-acceptor.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("ProxyNoAcceptorConfigTests", "extend", "server-cache-config-no-acceptor.xml");
        Member member = findCacheServer("ProxyNoAcceptorConfigTests");
        System.setProperty("test.extend.port", String.valueOf(member.getPort()));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ProxyNoAcceptorConfigTests");
        }

    /**
    * {@inheritDoc}
    */
    @Test
    public void destroyRemote()
        {
        // requires ExtendTcpInvocationService
        }
    /**
     * {@inheritDoc}
     */
    @Test
    public void testIsDestroyed()
        {
        // requires ExtendTcpInvocationService
        }


    /**
    * {@inheritDoc}
    */
    @Test
    public void destroyRemoteWithMapListener()
        {
        // COH-9690
        // This method requires acceptor configuration with filter specified.
        }

    /**
    * {@inheritDoc}
    */
    @Test
    public void destroyRemoteWithMapListenerForKey()
        {
        // COH-9690
        // This method requires acceptor configuration with filter specified.
        }

    /**
    * {@inheritDoc}
    */
    @Test
    public void destroyRemoteWithMapListenerForFilter()
        {
        // COH-9690
        // This method requires acceptor configuration with filter specified.
        }
    }
