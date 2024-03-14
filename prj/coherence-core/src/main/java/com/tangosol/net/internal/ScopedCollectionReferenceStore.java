/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.IdentityHolder;
import com.tangosol.io.ClassLoaderAware;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.security.Security;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.SegmentedConcurrentMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ScopedCollectionReferenceStore} holds scoped {@link NamedCollection} references.
 * <p>
 * {@link NamedCollection} references are scoped by ClassLoader and, optionally, Subject.
 * ScopedCollectionReferenceStore requires no explicit input about Subjects from its clients.
 * Subject scoping is configured in the operational configuration and applies only to
 * remote collections.
 * <p>
 * Thread safety documented in {@link AbstractScopedReferenceStore}.
 */
@SuppressWarnings({"rawtypes", "unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
public class ScopedCollectionReferenceStore<C extends NamedCollection>
        extends ScopedReferenceStore<C>
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Create a {@link ScopedCollectionReferenceStore} to hold references to the collections.
     */
    public ScopedCollectionReferenceStore(Class<C> type)
        {
        super(type, C::isActive, C::getName, C::getService);
        }

    /**
     * Remove collection references from this store for all destroyed and released collections.
     */
    public void clearInactiveCollectionRefs()
        {
        for (Iterator<Map> iterByName = m_mapByName.values().iterator(); iterByName.hasNext();)
            {
            Map mapByLoader = iterByName.next();

            synchronized (mapByLoader)
                {
                for (Iterator iter = mapByLoader.entrySet().iterator(); iter.hasNext();)
                    {
                    Map.Entry entry = (Map.Entry) iter.next();

                    Object oHolder = entry.getValue();

                    if (f_clsType.isInstance(oHolder))
                        {
                        C collection = (C) entry.getValue();

                        if (collection.isDestroyed() || collection.isReleased())
                            {
                            iter.remove();
                            internalReleaseCollection(collection);
                            }
                        }
                    else if (oHolder instanceof SubjectScopedReference)
                        {
                        Collection col = ((SubjectScopedReference) oHolder).values();

                        if (!col.isEmpty())
                            {
                            C collection = (C) col.iterator().next();
                            if (collection.isDestroyed() || collection.isReleased())
                                {
                                iter.remove();
                                internalReleaseCollection(collection);
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
     * Retrieve the collection reference associated with the ClassLoader (and
     * Subject if applicable).
     *
     * @param sCollectionName  the name of the collection
     * @param loader           the cache's ClassLoader
     *
     * @return the collection reference
     */
    public C getCollection(String sCollectionName, ClassLoader loader)
        {
        Map mapByLoader = (Map) m_mapByName.get(sCollectionName);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == null || f_clsType.isInstance(oHolder))
                    {
                    return (C) oHolder;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    return (C) ((SubjectScopedReference) oHolder).get();
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
     * Store a collection reference with the supplied ClassLoader.
     *
     * @param col     the collection reference
     * @param loader  the ClassLoader
     */
    public void putCollection(C col, ClassLoader loader)
        {
        if (col.isReleased())
            {
            throw new IllegalArgumentException("Storing a released collection is not allowed: " + col.getName());
            }

        ConcurrentMap mapByName = m_mapByName;
        String sCollectionName = col.getName();
        Map mapByLoader = (Map) mapByName.get(sCollectionName);

        if (mapByLoader == null)
            {
            mapByLoader = new WeakHashMap();
            mapByName.put(sCollectionName, mapByLoader);
            }

        if (ScopedServiceReferenceStore.isRemoteServiceType(col.getService().getInfo().getServiceType())
            && Security.SUBJECT_SCOPED)
            {
            SubjectScopedReference scopedRef = (SubjectScopedReference) mapByLoader.get(loader);

            if (scopedRef == null)
                {
                scopedRef = new SubjectScopedReference();
                mapByLoader.put(loader, scopedRef);
                }

            scopedRef.set(col);
            }
        else
            {
            mapByLoader.put(loader, col);
            }
        }

    /**
     * Store a collection reference with the supplied ClassLoader only if
     * it does not already exist.
     *
     * @param collection   the collection reference
     * @param loader       the ClassLoader
     *
     * @return the previous value associated with the specified loader,
     *         or null if there was no mapping for the key and put succeeded
     */
    public Object putCollectionIfAbsent(C collection, ClassLoader loader)
        {
        if (collection.isReleased())
            {
            throw new IllegalArgumentException("Storing a released collection is not allowed: " + collection.getName());
            }

        SegmentedConcurrentMap mapByName = m_mapByName;

        String sCollectionName = collection.getName();
        Object oResult;
        Map    mapByLoader;
        do
            {
            mapByLoader = (Map) mapByName.get(sCollectionName);
            if (mapByLoader == null)
                {
                mapByLoader = new WeakHashMap();

                Map mapTmp = (Map) mapByName.putIfAbsent(sCollectionName, mapByLoader);

                if (mapTmp != null)
                    {
                    mapByLoader = mapTmp;
                    }
                }

            if (ScopedServiceReferenceStore.isRemoteServiceType(collection.getService().getInfo().getServiceType())
            && Security.SUBJECT_SCOPED)
                {
                SubjectScopedReference scopedRef = new SubjectScopedReference();

                oResult = putLoaderIfAbsent(mapByLoader, loader, scopedRef);

                if (oResult != null)
                    {
                    scopedRef = (SubjectScopedReference) oResult;
                    }

                oResult = scopedRef.putIfAbsent(collection);
                }
            else
                {
                oResult = putLoaderIfAbsent(mapByLoader, loader, collection);
                }
            }
         while (mapByName.get(sCollectionName) != mapByLoader);

        return oResult;
        }

    /**
     * Remove the referenced collection item from the store using the supplied
     * ClassLoader.
     *
     * @param collection   the collection reference
     * @param loader       the ClassLoader
     *
     * @return whether the item was found
     */
    public boolean releaseCollection(C collection, ClassLoader loader)
        {
        return releaseCollection(collection, loader, null);
        }

    /**
     * Atomically remove the referenced collection item from the store and execute <code>postRelease</code>.
     *
     * @param collection   the collection reference
     * @param loader       the ClassLoader
     * @param postRelease  run after collection reference removed from store
     *
     * @return whether the item was found.
     */
    public boolean releaseCollection(C collection, ClassLoader loader, Runnable postRelease)
        {
        Map               mapByName       = m_mapByName;
        String            sCollectionName = collection.getName();
        Map               mapByLoader     = (Map) mapByName.get(sCollectionName);
        boolean           fFound          = false;
        Object            oPending        = null;
        IdentityHolder<C> collectionId    = new IdentityHolder<>(collection);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == collection)
                    {
                    // remove the mapping
                    mapByLoader.remove(loader);
                    fFound = true;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                    if (scopedRef.get() == collection)
                        {
                        scopedRef.remove();
                        fFound = true;

                        if (scopedRef.isEmpty())
                            {
                            mapByLoader.remove(loader);
                            }
                        }
                    }

                // run postRelease outside the synchronize block
                if (fFound && postRelease != null)
                    {
                    f_mapPending.put(collectionId, oPending = new Object());
                    }

                // remove the loader map if this was the last collection by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sCollectionName);
                    }
                }
            }

        // run and signal to any waiting threads
        if (oPending != null)
            {
            try
                {
                postRelease.run();
                }
            finally
                {
                f_mapPending.remove(collectionId, oPending);

                synchronized (oPending)
                    {
                    oPending.notifyAll();
                    }
                }
            }

        if (!fFound)
            {
            // wait if there exist a pending postRelease for cacheId
            awaitPending(collectionId);
            }

        return fFound;
        }

    /**
     * Remove the referenced collection item from the store.
     *
     * @param collection  the collection reference
     *
     * @return whether the item was found
     */
    public boolean releaseCollection(C collection)
        {
        Map     mapByName       = m_mapByName;
        String  sCollectionName = collection.getName();
        Map     mapByLoader     = (Map) mapByName.get(sCollectionName);
        boolean fFound          = false;

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Collection col = mapByLoader.values();

                // Assume it's a collection of Cs references
                fFound = col.remove(collection);

                if (!fFound)
                    {
                    if (collection instanceof ClassLoaderAware)
                        {
                        return releaseCollection(collection, ((ClassLoaderAware) collection).getContextClassLoader());
                        }

                    // could be a collection of SubjectScopedReferences
                    for (Iterator iter = col.iterator(); iter.hasNext(); )
                        {
                        Object oHolder = iter.next();

                        if (oHolder instanceof SubjectScopedReference)
                            {
                            SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                            if (scopedRef.get() == collection)
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

                // remove the loader map if this was the last collection by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sCollectionName);
                    }
                }
            }

        return fFound;
        }


    protected void awaitPending(IdentityHolder<C> collectionId)
        {
        Object oPending = f_mapPending.get(collectionId);

        if (oPending != null)
            {
            synchronized (oPending)
                {
                if (oPending == f_mapPending.get(collectionId))
                    {
                    try
                        {
                        Blocking.wait(oPending);
                        }
                    catch (InterruptedException e)
                        {
                        // ignore
                        }
                    }
                }
            }
        }

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

    private void internalReleaseCollection(C col)
        {
        try
            {
            col.release();
            }
        catch (RuntimeException e)
            {
            // ignored
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Map of NamedCollection id to an Object, used as a semaphore to block threads requiring a released collection.
     */
    protected Map<IdentityHolder<C>, Object> f_mapPending = new ConcurrentHashMap<>();
    }
