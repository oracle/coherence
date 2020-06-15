/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.NamedCache;

import com.tangosol.net.security.Security;

import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.SegmentedConcurrentMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * {@link ScopedCacheReferenceStore} holds scoped cache references.
 * <p>
 * Cache references are scoped by ClassLoader and, optionally, Subject.
 * ScopedCacheReferenceStore requires no explicit input about
 * Subjects from its clients. Subject scoping is configured in the operational
 * configuration and applies only to remote cache.
 * <p>
 * Thread safety documented in {@link AbstractScopedReferenceStore}.
 *
 * @author jf 2015.06.22
 *
 * @since Coherence 12.2.1
 */
public class ScopedCacheReferenceStore
        extends AbstractScopedReferenceStore
    {
    // ----- ScopedCacheReferenceStore methods ------------------------------------------------------------------------

    /**
     * Remove cache references from this store for all destroyed and released caches.
     */
    public void clearInactiveCacheRefs()
        {
        for (Iterator<Map> iterByName = m_mapByName.values().iterator(); iterByName.hasNext(); )
            {
            Map mapByLoader = iterByName.next();

            synchronized (mapByLoader)
                {
                for (Iterator iter = mapByLoader.entrySet().iterator(); iter.hasNext(); )
                    {
                    Map.Entry entry = (Map.Entry) iter.next();

                    Object oHolder = entry.getValue();

                    if (oHolder instanceof NamedCache)
                        {
                        NamedCache cache = (NamedCache) entry.getValue();

                        if (cache.isDestroyed() || cache.isReleased())
                            {
                            iter.remove();
                            internalReleaseCache(cache);
                            }
                        }
                    else if (oHolder instanceof SubjectScopedReference)
                        {
                        Collection col = ((SubjectScopedReference) oHolder).values();

                        if (!col.isEmpty())
                            {
                            // all the entries in the SubjectScopedReference refer
                            // to the same NamedCache instance, so we only need to
                            // check the first one
                            NamedCache cache = (NamedCache) col.iterator().next();
                            if (cache.isDestroyed() || cache.isReleased())
                                {
                                iter.remove();
                                internalReleaseCache(cache);
                                }
                            }
                        }
                    }

                if (mapByLoader.isEmpty())
                    {
                    iterByName.remove();
                    }
                }
            }
        }

    /**
     * Retrieve the cache reference associated with the ClassLoader (and
     * Subject if applicable).
     *
     * @param sCacheName  the name of the cache
     * @param loader      the cache's ClassLoader
     *
     * @return the cache reference
     */
    public NamedCache getCache(String sCacheName, ClassLoader loader)
        {
        Map mapByLoader = (Map) m_mapByName.get(sCacheName);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == null || oHolder instanceof NamedCache)
                    {
                    return (NamedCache) oHolder;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    return (NamedCache) ((SubjectScopedReference) oHolder).get();
                    }
                else
                    {
                    throw new UnsupportedOperationException();
                    }
                }
            }

        return null;
        }

    /**
     * Retrieve all cache references in the store.
     *
     * @return all cache references
     */
    public Collection getAllCaches()
        {
        Set             setRef    = new HashSet();
        Collection<Map> colLoader = m_mapByName.values();

        for (Map mapByLoader : colLoader)
            {
            synchronized (mapByLoader)
                {
                for (Object oHolder : mapByLoader.values())
                    {
                    if (oHolder instanceof SubjectScopedReference)
                        {
                        setRef.addAll(((SubjectScopedReference) oHolder).values());
                        }
                    else if (oHolder instanceof NamedCache)
                        {
                        setRef.add(oHolder);
                        }
                    else
                        {
                        throw new UnsupportedOperationException();
                        }
                    }
                }
            }

        return setRef;
        }

    /**
     * Retrieve all cache references for this name.
     *
     * @param sCacheName  the name of the cache
     *
     * @return all cache references for this name.
     */
    public Collection getAllCaches(String sCacheName)
        {
        Set setRef      = new HashSet();
        Map mapByLoader = (Map) m_mapByName.get(sCacheName);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Collection col = mapByLoader.values();

                for (Iterator iter = col.iterator(); iter.hasNext(); )
                    {
                    Object oHolder = iter.next();

                    if (oHolder instanceof SubjectScopedReference)
                        {
                        setRef.addAll(((SubjectScopedReference) oHolder).values());
                        }
                    else if (oHolder instanceof NamedCache)
                        {
                        setRef.add(oHolder);
                        }
                    else
                        {
                        throw new UnsupportedOperationException();
                        }
                    }
                }
            }

        return setRef;
        }

    /**
     * Store a cache reference with the supplied ClassLoader.
     *
     * @param cache   the cache reference
     * @param loader  the ClassLoader
     */
    public void putCache(NamedCache cache, ClassLoader loader)
        {
        if (cache.isReleased())
            {
            throw new IllegalArgumentException("Storing a released cache is not allowed: " + cache.getCacheName());
            }

        ConcurrentMap mapByName   = m_mapByName;
        String        sCacheName  = cache.getCacheName();
        Map           mapByLoader = (Map) mapByName.get(sCacheName);

        if (mapByLoader == null)
            {
            mapByLoader = new WeakHashMap();
            mapByName.put(sCacheName, mapByLoader);
            }

        if (ScopedServiceReferenceStore.isRemoteServiceType(cache.getCacheService().getInfo().getServiceType())
            && Security.SUBJECT_SCOPED)
            {
            SubjectScopedReference scopedRef = (SubjectScopedReference) mapByLoader.get(loader);

            if (scopedRef == null)
                {
                scopedRef = new SubjectScopedReference();
                mapByLoader.put(loader, scopedRef);
                }

            scopedRef.set(cache);
            }
        else
            {
            mapByLoader.put(loader, cache);
            }
        }

    /**
     * Store a cache reference with the supplied ClassLoader only if
     * it does not already exist.
     *
     * @param cache   the cache reference
     * @param loader  the ClassLoader
     *
     * @return the previous value associated with the specified loader,
     *         or null if there was no mapping for the key and put succeeded
     */
    public Object putCacheIfAbsent(NamedCache cache, ClassLoader loader)
        {
        if (cache.isReleased())
            {
            throw new IllegalArgumentException("Storing a released cache is not allowed: " + cache.getCacheName());
            }

        SegmentedConcurrentMap mapByName  = m_mapByName;

        String sCacheName  = cache.getCacheName();
        Object oResult;
        Map    mapByLoader;
        do
            {
            mapByLoader = (Map) mapByName.get(sCacheName);

            if (mapByLoader == null)
                {
                mapByLoader = new WeakHashMap();

                Map mapTmp = (Map) mapByName.putIfAbsent(sCacheName, mapByLoader);

                if (mapTmp != null)
                    {
                    mapByLoader = mapTmp;
                    }
                }

            if (ScopedServiceReferenceStore.isRemoteServiceType(cache.getCacheService().getInfo().getServiceType())
                    && Security.SUBJECT_SCOPED)
                {
                SubjectScopedReference scopedRef = new SubjectScopedReference();

                oResult = putLoaderIfAbsent(mapByLoader, loader, scopedRef);

                if (oResult != null)
                    {
                    scopedRef = (SubjectScopedReference) oResult;
                    }

                oResult = scopedRef.putIfAbsent(cache);
                }
            else
                {
                oResult = putLoaderIfAbsent(mapByLoader, loader, cache);
                }
            }
        while (mapByName.get(sCacheName) != mapByLoader);

        return oResult;
        }

    /**
     * Remove the referenced cache item from the store using the supplied
     * ClassLoader.
     *
     * @param cache   the cache reference
     * @param loader  the ClassLoader
     *
     * @return whether the item was found
     */
    public boolean releaseCache(NamedCache cache, ClassLoader loader)
        {
        Map     mapByName   = m_mapByName;
        String  sCacheName  = cache.getCacheName();
        Map     mapByLoader = (Map) mapByName.get(sCacheName);
        boolean fFound      = false;

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == cache)
                    {
                    // remove the mapping
                    mapByLoader.remove(loader);
                    fFound = true;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                    if (scopedRef.get() == cache)
                        {
                        scopedRef.remove();
                        fFound = true;

                        if (scopedRef.isEmpty())
                            {
                            mapByLoader.remove(loader);
                            }
                        }
                    }

                // remove the loader map if this was the last cache by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sCacheName);
                    }
                }
            }

        return fFound;
        }

    /**
     * Remove the referenced cache item from the store.
     *
     * @param cache  the cache reference
     *
     * @return whether the item was found
     */
    public boolean releaseCache(NamedCache cache)
        {
        Map     mapByName   = m_mapByName;
        String  sCacheName  = cache.getCacheName();
        Map     mapByLoader = (Map) mapByName.get(sCacheName);
        boolean fFound      = false;

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Collection col = mapByLoader.values();

                // Assume it's a collection of NamedCache references
                fFound = col.remove(cache);

                if (!fFound)
                    {
                    if (cache instanceof ClassLoaderAware)
                        {
                        return releaseCache(cache, ((ClassLoaderAware) cache).getContextClassLoader());
                        }

                    // could be a collection of SubjectScopedReferences
                    for (Iterator iter = col.iterator(); iter.hasNext(); )
                        {
                        Object oHolder = iter.next();

                        if (oHolder instanceof SubjectScopedReference)
                            {
                            SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                            if (scopedRef.get() == cache)
                                {
                                scopedRef.remove();
                                fFound = true;

                                if (scopedRef.isEmpty())
                                    {
                                    iter.remove();
                                    }

                                break;
                                }
                            }
                        else
                            {
                            // no sense continuing if these aren't
                            // SubjectScopeReferences
                            break;
                            }
                        }
                    }

                // remove the loader map if this was the last cache by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sCacheName);
                    }
                }
            }

        return fFound;
        }

    /**
     * Store a loader reference with the supplied value (a cache or subject reference)
     * only if it does not already exist.
     *
     * @param map     the map
     * @param loader  the ClassLoader
     * @param oValue  the cache or Subject reference
     *
     * @return the previous value associated with the specified loader,
     *         or null if there was no mapping for the key and put succeeded
     */
    private Object putLoaderIfAbsent(Map map, ClassLoader loader, Object oValue)
        {
        Object oResult;

        synchronized (map)
            {
            oResult = map.get(loader);

            if (oResult == null)
                {
                map.put(loader, oValue);
                }
            }

        return oResult;
        }

    /**
     * Release cache resources for inactive instances.
     *
     * @param cache inactive cache to release
     */
    private void internalReleaseCache(NamedCache cache)
        {
        try
            {
            cache.release();
            }
        catch (RuntimeException e)
            {
            // one of the following should be ignored:
            //   IllegalStateException("Cache is not active");
            //   RuntimeException("Storage is not configured");
            //   RuntimeException("Service has been terminated");
            }
        }
    }
