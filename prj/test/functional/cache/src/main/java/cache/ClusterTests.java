/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.oracle.coherence.common.base.Disposable;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;


import com.oracle.coherence.testing.AbstractFunctionalTest;


/**
* Test harness for Cluster.
*
* @author cp 2010-06-xx
*/
public class ClusterTests
        extends AbstractFunctionalTest
    {
    /**
    * Default constructor.
    */
    public ClusterTests()
        {
        }

    /**
    * Test the ability to manage resources (COH-4268) in the cluster.
    *
    * @throws Exception
    */
    @Test
    public void testResourceManagement()
            throws Exception
        {
        Cluster cluster = CacheFactory.ensureCluster();

        // First testing that you can't register a null resource
        try
            {
            cluster.registerResource("test", null);
            fail("It shouldn't be possible to register a null resource");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        catch (Throwable t)
            {
            fail("Only NullPointerException allowed (" + t + ")");
            }

        // A disposable to use in the tests
        Disposable disp = new Disposable()
            {
            @Override
            public void dispose()
                {
                }
            };

        // Testing basic retrieval and storage of resource
        cluster.registerResource("test", disp);
        assertTrue(cluster.getResource("test") == disp);

        // Testing storing of the same resource again, which is allowed
        cluster.registerResource("test", disp);

        // Now testing registering another resource with the same name, not allowed
        Disposable anotherDisp = new Disposable()
            {
            @Override
            public void dispose()
                {
                }
            };
        try
            {
            cluster.registerResource("test", anotherDisp);
            fail("It shouldn't be possible to register another resource with the same name");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        catch (Throwable t)
            {
            fail("Only IllegalArgumentException allowed (" + t + ")");
            }

        // but if we unregister the resource first it is possible to register
        // a different resource
        assertTrue(cluster.unregisterResource("test") == disp);
        cluster.registerResource("test", anotherDisp);
        out("JournalTests.testResourceManagement finished");
        }
    }
