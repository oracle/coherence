/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package tcmp;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.OperationalContext;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.Properties;

import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
* A collection of functional tests for TCMP.
*
* @author mf 2010.07.01
*/
public class ClusteringTests
        extends AbstractFunctionalTest
    {
    // ----- test methods ---------------------------------------------------

    /**
    * Test that a cluster of the expected size formed
    */
    @Test
    public void testMembership()
        {
        Cluster cluster = CacheFactory.ensureCluster();
        try
            {
            startServers(getProject(), getProject(), getProperties(), SERVER_COUNT);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(SERVER_COUNT + 1));
            }
        finally
            {
            stopServers(getProject(), SERVER_COUNT);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
    * Test the default implementation of OperationalContext.
    */
    @Test
    public void testOperationalContext()
        {
        Cluster cluster = CacheFactory.getCluster();

        assertTrue(cluster instanceof OperationalContext);
        OperationalContext ctx = (OperationalContext) cluster;

        assertEquals (3, ctx.getEdition());
        assertEquals ("CE", ctx.getEditionName());
        assertEquals (1, ctx.getFilterMap().size());
        assertEquals (2, ctx.getSerializerMap().size());
        assertNotNull(ctx.getIdentityAsserter());
        assertNotNull(ctx.getIdentityTransformer());
        assertFalse  (ctx.isSubjectScopingEnabled());
        assertNotNull(ctx.getSocketProviderFactory());
        assertNotNull(ctx.getLocalMember());
        }

    // ---- helper methods --------------------------------------------------

    public static CoherenceClusterMember startServers(String sName, String sProject, Properties props, int c)
        {
        CoherenceClusterMember member = null;
        for (int i = 0; i < c; ++i)
            {
            member = startCacheServer(sName +i, sProject, null, props);
            }
        return member;
        }

    public static void stopServers(String sName, int c)
        {
        for (int i = 0; i < c; ++i)
            {
            try
                {
                stopCacheServer(sName + i);
                }
            catch (Exception e)
                {
                e.printStackTrace();
                }
            }
        }

    /**
    * Return the project associated with the test.
    *
    * @return  the project associated with the test
    */
    public String getProject()
        {
        return System.getProperty("test.project","tcmp");
        }

    /**
    * Return the server startup properties.
    *
    * @return server startup properties
    */
    public Properties getProperties()
        {
        return new Properties();
        }

    public static final int SERVER_COUNT = 2;
    }
