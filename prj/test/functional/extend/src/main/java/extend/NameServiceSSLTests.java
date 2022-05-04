/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.net.Member;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.hamcrest.CoreMatchers.is;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

/**
 * A collection of functional tests for Coherence*Extend that use the
 * name service.  Name service is not configured for SSL, but the
 * client and proxy are.
 *
 * @author par  2014.8.13
 * @since 12.2.1
 */
public class NameServiceSSLTests
        extends AbstractExtendTests
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public NameServiceSSLTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT, "client-cache-config-nameservice-ssl.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy =
                startCacheServer("NameServiceSSLTests", "extend", "server-cache-config-nameservice-ssl.xml");
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxySSLService"), is(true));

        Member member = findCacheServer("NameServiceSSLTests");
        System.setProperty("test.extend.port", String.valueOf(member.getPort()));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NameServiceSSLTests");
        }
    }
