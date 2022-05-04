/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A collection of functional tests for Coherence*Extend clients that
 * use a custom proxy.
 *
 * @author prollman  2013.03.27
 *
 * @since 12.1.3
 */
public class CustomProxyTests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public CustomProxyTests()
        {
        super("dist-test", "client-cache-config-custom-proxy.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("CustomProxyTests", "server-cache-config-custom-proxy.xml");
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("CustomProxyTests");
        }

    // ----- Extend client tests --------------------------------------------

    /**
     * The Coherence*Extend proxy specifies
     * <thread-count>1</thread-count>. Verify the correct thread count.
     */
    @Test
    public void testThreadCount()
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService("ExtendTcpInvocationService");

        ProxyServiceDynamicThreadSizingTests.ThreadCountInvocable task =
            new ProxyServiceDynamicThreadSizingTests.ThreadCountInvocable("ExtendTcpCustomProxyService");
        task.setValue(0);
        Map map = service.query(task, null);

        assertTrue(map != null);
        assertTrue(map.size() == 1);

        Object oMember = map.keySet().iterator().next();
        assertTrue(equals(oMember, service.getCluster().getLocalMember()));

        Object oResult = map.values().iterator().next();
        assertTrue(oResult instanceof Integer);
        assertEquals(1, ((Integer) oResult).intValue());
        }

    /**
     * Test cache Destroy/recreation with custom proxy. (COH-9302)
     * Tests scenario where client cannot find cache on the restart
     * because the custom proxy was not being called.
     */
    @Test
    public void testGetCache()
        {
        HashMap<String, String> map = new HashMap();
        map.put("MyKey1", "MyValue1");
        map.put("MyKey2", "MyValue2");

        getNamedCache();

        // call an invocable to destroy the cache
        destroyCache(m_sCache);

        // AbstractFunctionalTest.getNamedCache()
        NamedCache cache = getFactory().ensureCache(m_sCache, getClass().getClassLoader());
        cache.putAll(map);
        getFactory().destroyCache(cache);
        }

    /**
     * Destroy the hidden cache through an Invocable so the
     * client doesn't know it's happening.
     *
     * @param sName  Name of the cache to destroy.
     */
    private void destroyCache(String sName)
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService("ExtendTcpInvocationService");

        CacheDestroyInvocable invocable = new CacheDestroyInvocable();
        invocable.setCacheName(sName);
        try 
            {
            service.query(invocable, null);
            }
        catch (Exception e)
            {
            // ignore
            }
        }
    }