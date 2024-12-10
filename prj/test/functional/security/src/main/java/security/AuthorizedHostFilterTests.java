/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.CacheFactory;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* Functional test of authorized host filter based on
* {@link SysPropAuthorizedHostFilter}
*
* @author pp  2010.02.15
*/
public class AuthorizedHostFilterTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.override", "security-coherence-override.xml");
        System.setProperty("java.security.auth.login.config", "login.config");
        }

    /**
    * Asserts that new members are denied access to a running cluster
    * with a configured authorized host filter that returns false for
    * new hosts
    */
    @Test
    public void testAccessDenied() throws Exception
        {
        Properties props = new Properties();
        props.setProperty(SysPropAuthorizedHostFilter.DENY_ACCESS_PROPERTY, "false");

        CoherenceClusterMember clusterMember = startCacheServer("AuthorizedHostFilterTests", "security", null, props, false);
        Eventually.assertThat(invoking(clusterMember).getClusterSize(), is(1));

        try
            {
            clusterMember.submit(new DenyClusterAccess());
            CacheFactory.ensureCluster();
            fail("Should not be able to join cluster");
            }
        catch (RuntimeException t)
            {
            // expected
            }
        finally
            {
            stopCacheServer("AuthorizedHostFilterTests");
            }
        }
    }
