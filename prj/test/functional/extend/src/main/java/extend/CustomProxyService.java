/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.WrapperCacheService;

import com.tangosol.util.Base;

import java.util.HashMap;

/**
 * A custom proxy service used by the Coherence*Extend tests
 * to reproduce the issue of COH-9302 and COH-10772.
 *
 * This proxy service maps the incoming cache name to
 * a cache name known to the proxy/cache, but unknown to the
 * client.
 *
 * @author par  2013.04.10
 *
 * @since @BUILDVERSION@
 */
public class CustomProxyService
	extends WrapperCacheService
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new CustomProxyService that delegates to the given service
     * but doesn't hide the cache. Used for COH-10772.
     *
     * @param service    Service this instance delegates to
     * @param eventTest  Running event test, so don't hide the cachename.
     */
    public CustomProxyService(CacheService service, boolean eventTest)
        {
        super(service);
        f_eventTest = eventTest;
        }

    /**
     * Create a new CustomProxyService that delegates to the given service.
     * @param service
     */
    public CustomProxyService(CacheService service)
        {
        super(service);
        f_eventTest = false;
        }

    // ----- CacheService interface -----------------------------------------

    /**
     * Returns the hidden cache name and
     * delegates ensuring the hidden cache to the CacheService.
     *
     * @param sName  client requested cache name
     * @param loader classloader used to load the cache
     *
     * @return test named cache, always
     */
    public NamedCache ensureCache(String sName, ClassLoader loader)
        {
        m_loader = loader;
        if (f_eventTest)
            {
            // Proxy holds reference to the test cache.
            final String strName = sName; 
            final ClassLoader tmpLoader = loader;
            Thread t = Base.makeThread(null, new Runnable()
                {
                @Override public void run()
                   {
                   NamedCache m_cache = CacheFactory.getCache(strName, tmpLoader);
                   }
                }, "ProxyCacheHolder");
            t.start();
            return super.ensureCache(sName, loader);
            }
        else
            {
            return super.ensureCache(CACHENAME, m_loader);
            }
        }

    /**
     * Maps the cache to the hidden cache name and
     * delegates destroying the hidden cache to the CacheService.
     *
     * @param map  client requested cache to destroy
     */
    @Override
    public void destroyCache(NamedCache map)
        {
        if (f_eventTest)
            {
            super.destroyCache(m_cache);
            }
        else
            {
            super.destroyCache(super.ensureCache(CACHENAME, m_loader));
            }
        }

    //----- constants --------------------------------------------------------

    /**
     * Name of the cache that is always returned.
     */
    private static final String CACHENAME = "test-cache";

    //----- data members -----------------------------------------------------

    /**
     * Loader used to instantiate cache.
     */
    private ClassLoader m_loader;

    /**
     * Flag whether doing event test, so hold reference to the cache
     */
    private boolean     f_eventTest;

    /**
     * Reference to the cache held by proxy.
     */
    private NamedCache  m_cache;
    }