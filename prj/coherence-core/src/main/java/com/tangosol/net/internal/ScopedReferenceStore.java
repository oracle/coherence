/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.Service;
import com.tangosol.net.security.Security;

import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.SegmentedConcurrentMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.function.Function;

/**
 * {@link ScopedReferenceStore} holds scoped {@link Object} references.
 * <p>
 * References are scoped by ClassLoader and, optionally, Subject.
 * ScopedReferenceStore requires no explicit input about Subjects from its
 * clients. Subject scoping is configured in the operational configuration
 * and applies only to remote references.
 * <p>
 * Thread safety documented in {@link AbstractScopedReferenceStore}.
 *
 * @author jk 2015.06.25
 *
 * @since Coherence 14.1.1
 */
public class ScopedReferenceStore<R>
        extends AbstractScopedReferenceStore
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Create a {@link ScopedReferenceStore} to hold references to the specified types.
     *
     * @param clsType           the {@link Class} of the type of reference held in this store
     * @param supplierIsActive  the {@link Function} that returns the active status of a reference
     * @param supplierName      the {@link Function} that returns the name of a reference
     * @param supplierService   the {@link Function} that returns the {@link Service} of a reference
     */
    public ScopedReferenceStore(Class<R> clsType, Function<R,Boolean> supplierIsActive,
                                Function<R,String> supplierName, Function<R,Service> supplierService)
        {
        f_clsType          = clsType;
        f_supplierIsActive = supplierIsActive;
        f_supplierName     = supplierName;
        f_supplierService  = supplierService;
        }


    // ----- ScopedReferenceStore methods -----------------------------------

    /**
     * Remove references from this store that are inactive.
     *
     * @param sName  the name of the Referenceable
     *
     * @return a Collection of the ClassLoader hash codes for the ClassLoaders
     *         associated with inactive references that have been
     *         cleared; null if the store is empty.
     */
    public Collection<Integer> clearInactiveRefs(String sName)
        {
        Map mapByLoader = (Map) m_mapByName.get(sName);

        if (mapByLoader == null)
            {
            return null;    // nothing stored yet
            }

        Set<Integer> setHashCode;

        synchronized (mapByLoader)
            {
            setHashCode = mapByLoader.isEmpty() ? Collections.emptySet() : new HashSet();

            for (Iterator iter = mapByLoader.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry   entry     = (Map.Entry) iter.next();

                ClassLoader loaderTmp = (ClassLoader) entry.getKey();
                Object      oHolder   = entry.getValue();

                if (f_clsType.isAssignableFrom(oHolder.getClass()))
                    {
                    R referenceTmp = (R) entry.getValue();

                    if (!f_supplierIsActive.apply(referenceTmp))
                        {
                        setHashCode.add(loaderTmp == null ? 0 : loaderTmp.hashCode());
                        iter.remove();
                        }
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    for (Object o : ((SubjectScopedReference) oHolder).values())
                        {
                        R referenceTmp = (R) o;

                        if (!f_supplierIsActive.apply(referenceTmp))
                            {
                            setHashCode.add(loaderTmp == null ? 0 : loaderTmp.hashCode());
                            iter.remove();
                            }
                        }
                    }
                else
                    {
                    throw new UnsupportedOperationException();
                    }
                }
            }

        return setHashCode;
        }

    /**
     * Retrieve the Referenceable associated with the ClassLoader (and
     * Subject if applicable).
     *
     * @param sName   the name of the Referenceable
     * @param loader  the Referenceable's ClassLoader
     *
     * @return the Referenceable
     */
    public R get(String sName, ClassLoader loader)
        {
        Map mapByLoader = (Map) m_mapByName.get(sName);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == null || f_clsType.isAssignableFrom(oHolder.getClass()))
                    {
                    return (R) oHolder;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    return (R) ((SubjectScopedReference) oHolder).get();
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
     * Retrieve all references in the store.
     *
     * @return all references
     */
    public Collection<R> getAll()
        {
        Set        setRef    = new HashSet();
        Collection colLoader = m_mapByName.values();

        for (Object aColLoader : colLoader)
            {
            Map mapByLoader = (Map) aColLoader;

            synchronized (mapByLoader)
                {
                for (Iterator iter = mapByLoader.values().iterator(); iter.hasNext(); )
                    {
                    Object oHolder = iter.next();

                    if (oHolder instanceof SubjectScopedReference)
                        {
                        setRef.addAll(((SubjectScopedReference) oHolder).values());
                        }
                    else if (f_clsType.isAssignableFrom(oHolder.getClass()))
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
     * Retrieve all references for this name.
     *
     * @param sName  the name of the Referenceable
     *
     * @return all references for this name.
     */
    public Collection<R> getAll(String sName)
        {
        Set setRef      = new HashSet();
        Map mapByLoader = (Map) m_mapByName.get(sName);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Collection col = mapByLoader.values();

                for (Object oHolder : col)
                    {
                    if (oHolder instanceof SubjectScopedReference)
                        {
                        setRef.addAll(((SubjectScopedReference) oHolder).values());
                        }
                    else if (f_clsType.isAssignableFrom(oHolder.getClass()))
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
     * Store a reference with the supplied ClassLoader.
     *
     * @param reference  the Referenceable
     * @param loader     the ClassLoader
     */
    public void put(R reference, ClassLoader loader)
        {
        ConcurrentMap mapByName = m_mapByName;
        String        sName     = f_supplierName.apply(reference);

        Map           mapByLoader = (Map) mapByName.get(sName);

        if (mapByLoader == null)
            {
            mapByLoader = new WeakHashMap();
            mapByName.put(sName, mapByLoader);
            }

        Service service = f_supplierService.apply(reference);

        if (service != null && ScopedServiceReferenceStore.isRemoteServiceType(service.getInfo().getServiceType())
            && Security.SUBJECT_SCOPED)
            {
            SubjectScopedReference scopedRef = (SubjectScopedReference) mapByLoader.get(loader);

            if (scopedRef == null)
                {
                scopedRef = new SubjectScopedReference();
                mapByLoader.put(loader, scopedRef);
                }

            scopedRef.set(reference);
            }
        else
            {
            mapByLoader.put(loader, reference);
            }
        }

    /**
     * Store a reference with the supplied ClassLoader only if
     * it does not already exist.
     *
     * @param reference  the Referenceable
     * @param loader     the ClassLoader
     *
     * @return the previous value associated with the specified loader,
     *         or null if there was no mapping for the key and put succeeded
     */
    public Object putIfAbsent(R reference, ClassLoader loader)
        {
        SegmentedConcurrentMap mapByName  = m_mapByName;

        String                 sName = f_supplierName.apply(reference);
        Object                 oResult;

        Map                    mapByLoader = (Map) mapByName.get(sName);

        if (mapByLoader == null)
            {
            mapByLoader = new WeakHashMap();

            Map mapTmp = (Map) mapByName.putIfAbsent(sName, mapByLoader);

            if (mapTmp != null)
                {
                mapByLoader = mapTmp;
                }
            }

        Service service = f_supplierService.apply(reference);

        if (service != null && ScopedServiceReferenceStore.isRemoteServiceType(service.getInfo().getServiceType())
            && Security.SUBJECT_SCOPED)
            {
            SubjectScopedReference scopedRef = new SubjectScopedReference();

            oResult = putLoaderIfAbsent(mapByLoader, loader, scopedRef);

            if (oResult != null)
                {
                scopedRef = (SubjectScopedReference) oResult;
                }

            oResult = scopedRef.putIfAbsent(reference);
            }
        else
            {
            oResult = putLoaderIfAbsent(mapByLoader, loader, reference);
            }

        return oResult;
        }

    /**
     * Remove the Referenceable from the store using the supplied
     * ClassLoader.
     *
     * @param reference  the Referenceable
     * @param loader     the ClassLoader
     *
     * @return whether the item was found
     */
    public boolean release(R reference, ClassLoader loader)
        {
        Map     mapByName   = m_mapByName;
        String  sName       = f_supplierName.apply(reference);
        Map     mapByLoader = (Map) mapByName.get(sName);
        boolean fFound      = false;

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == reference)
                    {
                    // remove the mapping
                    mapByLoader.remove(loader);
                    fFound = true;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                    if (scopedRef.get() == reference)
                        {
                        scopedRef.remove();
                        fFound = true;

                        if (scopedRef.isEmpty())
                            {
                            mapByLoader.remove(loader);
                            }
                        }
                    }

                // remove the loader map if this was the last reference by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sName);
                    }
                }
            }

        return fFound;
        }

    /**
     * Remove the Referenceable from the store.
     *
     * @param reference  the Referenceable
     *
     * @return whether the item was found
     */
    public boolean release(R reference)
        {
        Map     mapByName   = m_mapByName;
        String  sName       = f_supplierName.apply(reference);
        Map     mapByLoader = (Map) mapByName.get(sName);
        boolean fFound      = false;

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Collection col = mapByLoader.values();

                // Assume it's a reference of Referencable references
                fFound = col.remove(reference);

                if (!fFound)
                    {
                    if (reference instanceof ClassLoaderAware)
                        {
                        return release(reference, ((ClassLoaderAware) reference).getContextClassLoader());
                        }

                    // could be a reference of SubjectScopedReferences
                    for (Iterator iter = col.iterator(); iter.hasNext(); )
                        {
                        Object oHolder = iter.next();

                        if (oHolder instanceof SubjectScopedReference)
                            {
                            SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                            if (scopedRef.get() == reference)
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

                // remove the loader map if this was the last reference by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sName);
                    }
                }
            }

        return fFound;
        }

    /**
     * Store a loader reference with the supplied value (a reference or subject reference)
     * only if it does not already exist.
     *
     * @param map     the map
     * @param loader  the ClassLoader
     * @param oValue  the Referenceable or Subject reference
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

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Class} of the references that this store holds.
     */
    protected final Class<R> f_clsType;

    /**
     * The {@link Function} that can return the active state of a referenced value.
     */
    private final Function<R,Boolean> f_supplierIsActive;

    /**
     * The {@link Function} that can return the name of a referenced value.
     */
    private final Function<R,String> f_supplierName;

    /**
     * The {@link Function} that can return the {@link Service} of a referenced value.
     */
    private final Function<R,Service> f_supplierService;
    }
