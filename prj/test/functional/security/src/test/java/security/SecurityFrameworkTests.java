/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package security;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.security.Security;

import common.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import static org.junit.Assert.*;


/**
 * Tests of the security framework.
 *
 * @author dag 2009.10.06
 */
public class SecurityFrameworkTests
        extends AbstractFunctionalTest
    {
    // ----- SecurityFrameworkTests methods ---------------------------------

    /**
    * Start the cache server for this test class.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("java.security.auth.login.config", "login.config");
        System.setProperty("coherence.cluster", "BOSTON");
            System.setProperty("coherence.security.log", "true");

        AbstractFunctionalTest._startup();
        startCacheServer("SecurityFrameworkTests", "security", CACHE_CONFIG, null);
        }

    /**
    * Stop the cache server for this test class.
    */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("SecurityFrameworkTests");
        }

    /**
    * Test of the security framework.
    */
    @Test
    public void frameworkTest()
        {
        assertTrue(Security.ENABLED);

        Subject    subject = Security.login("manager", "private".toCharArray());
        NamedCache cache   = (NamedCache) Security.runAs(subject, ACTION);

        assertTrue(cache != null);
        }

    /**
    * Test getting a cache from a different Subject.
    */
    @Test
    public void cachePermissionTest()
        {
        assertTrue(Security.ENABLED);

        Subject subject = Security.login("manager", "private".toCharArray());

        Security.runAs(subject, ACTION);
        // try with same subject
        Security.runAs(subject, ACTION);

        // change Subject and see if access is permitted
        subject = Security.login("admin", "private".toCharArray());
        try
            {
            Security.runAs(subject, ACTION);
            // failed if no security exception
            fail();
            }
        catch (Exception e)
            {
            assertTrue(e instanceof SecurityException);
            }
        }


    // ----- nested class: GetCacheAction -----------------------------------

    /**
    * Privileged action that gets a cache
    */
    public static class GetCacheAction
                implements PrivilegedAction
        {
        public GetCacheAction(String sCache)
            {
            m_sCache = sCache;
            }

        public Object run()
            {
            return CacheFactory.getCache(m_sCache);
            }

        private static String m_sCache;
        }

    /**
    * Cache name for tests
    */
    public static final String TEST_CACHE = "dist-test";

    /**
    * Privileged action for tests
    */
    public static final GetCacheAction ACTION = new GetCacheAction(TEST_CACHE);

    /**
     * Federated cache config
     */
    public static final String CACHE_CONFIG = "with-dist-cache-config.xml";
    }
