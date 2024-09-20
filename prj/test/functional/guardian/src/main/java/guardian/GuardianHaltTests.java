/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package guardian;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;

import com.tangosol.coherence.component.net.Cluster.NameService;
import com.tangosol.coherence.component.util.SafeCluster;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import java.net.ServerSocket;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
* Test cluster halt in separate test.
*
* @author jf 2024.04.19
*/
public class GuardianHaltTests
    {
    // ----- test methods -------------------------------------------------

    /**
     * Test Cluster.halt()
     */
    @Test
    public void testClusterHalt()
        {
        try
            {
            logWarn("testClusterHalt", true);
            System.setProperty("coherence.member", "GuardianHaltTestsMember");

            // test terminate of a task on the service thread
            CacheService service = startService("PartitionedCacheDefaultPolicies");
            NamedCache cache = service.ensureCache("foo", null);
            SafeCluster cluster = (SafeCluster) service.getCluster();

            assertTrue("Service was not running", service.isRunning());
            cache.put("key", "value");

            cluster.getCluster().halt();
            NameService nameService = cluster.getCluster().getNameService();
            ServerSocket serverSocket = nameService.getClusterSocket();
            assertTrue(serverSocket.isClosed());
            serverSocket = ((NameService.TcpAcceptor) nameService.getAcceptor())
                    .getProcessor().getServerSocket();
            assertTrue(serverSocket.isClosed());
            Eventually.assertDeferred(() -> service.isRunning(), is(false));
            Eventually.assertDeferred(() -> cluster.getCluster().isHalted(), is(true));

            try
                {
                cluster.ensureRunningCluster();
                fail("expected IllegalStateException to be called when ensureRunningCluster() when cluster is halted.");
                }
            catch (IllegalStateException ise)
                {
                assertTrue(ise.getMessage().contains("This cluster member's JVM process must be restarted."));
                }
            }
        catch (Throwable t)
            {
            // ignore exceptions thrown by halt
            }
        logWarn("testClusterHalt", false);
        }

    /**
    * Return a CacheService by the specified name.
    *
    * @return a CacheService that has been started
    */
    protected CacheService startService(String sName)
        {
        return (CacheService) CacheFactory.getService(sName);
        }

    /**
    * Log a warning (that can be easily scraped) that guardian errors (and
    * stack traces) are expected.
    *
    * @param sTestName  the name of the test
    * @param fHeader    if true, log the header, else the footer
    */
    protected void logWarn(String sTestName, boolean fHeader)
        {
        // Note: use System.out instead of Base.out here to avoid interfering
        //       with the tests' initialization of Coherence
        if (fHeader)
            {
            System.out.println("+++ " + sTestName + ": This test is expected to produce guardian error messages +++");
            }
        else
            {
            System.out.println("--- " + sTestName + " ---");
            }
        }

    /**
     * A JUnit rule that will cause the test to fail if it runs too long.
     * A thread dump will be generated on failure.
     */
    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout
            = ThreadDumpOnTimeoutRule.after(5, TimeUnit.MINUTES, true);
    }
