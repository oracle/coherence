/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.WrapperCacheService;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.util.Map;

/**
 * This in the custom proxy service for CustomProxyTestsBug27862015.
 *
 * @author tam  2018.04.18
 */
public class CustomProxyServiceBug27862015
        extends WrapperCacheService
    {
    /**
     * Create a new WrapperCacheService that delegates to the given
     * CacheService instance.
     *
     * @param service the CacheService to wrap
     */
    public CustomProxyServiceBug27862015(CacheService service)
        {
        super(service);
        }

    public CustomProxyServiceBug27862015(CacheService service, boolean eventTest)
        {
        super(service);
        f_eventTest = eventTest;
        }

    @Override
    public NamedCache ensureCache(String sName, ClassLoader loader)
        {
        log("Entering ensureCache() for " + sName);

        // get the coherence cache
        NamedCache cache = super.ensureCache(sName, loader);
        cache = new CustomLoggingCache(cache, cache.getCacheName());

        log("Leaving ensureCache() for " +sName);

        return cache;
        }

    public static class CustomLoggingCache
            extends WrapperNamedCache
        {
        public CustomLoggingCache(Map map, String sName)
            {
            super(map, sName);
            }

        @Override
        public Object put(Object oKey, Object oValue)
            {
            long start = System.nanoTime();
            Object ret = super.put(oKey, oValue);
            log("put(Object,Object) start=" +start + ", ret=" + ret + ", key=" + oKey + ", value=" + oValue);
            return ret;
            }

        // the following works
        @Override
        public void addMapListener(MapListener listener)
            {
            long start = System.nanoTime();

            // this fails now when using > 3.7.1.9 but was ok with <= 3.7.1.9
            super.addMapListener(new CustomLoggingMapListener(getCacheName(), listener));

            log("addMapListener(MapListener), start=" + start + ", listener=" + listener);
            }

        }

    public static class CustomLoggingMapListener extends AbstractMapListener
        {

        public  CustomLoggingMapListener (String sCacheName, MapListener listener)
            {
            f_cacheName = sCacheName;
            f_listener = listener;
            }

        @Override
        public void entryInserted(MapEvent evt)
            {
            log("entryInserted: " + evt.toString() + ", cacheName=" + f_cacheName + ", listener="  + f_listener);
            f_listener.entryInserted(evt);
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            log("entryUpdated: " + evt.toString() + ", cacheName=" + f_cacheName + ", listener="  + f_listener);
            f_listener.entryUpdated(evt);
            }
        
        @Override
        public void entryDeleted(MapEvent evt)
            {
            log("entryDeleted: " + evt.toString() + ", cacheName=" + f_cacheName + ", listener="  + f_listener);
            f_listener.entryDeleted(evt);
            }
        
        private final String f_cacheName;
        private final MapListener f_listener;
        }

     //----- data members -----------------------------------------------------

    /**
     * Flag whether doing event test, so hold reference to the cache
     */
    private boolean     f_eventTest;

    private static void log(String sMessage)
        {
        Logger.info("LOG: " + sMessage);
        }

    }
