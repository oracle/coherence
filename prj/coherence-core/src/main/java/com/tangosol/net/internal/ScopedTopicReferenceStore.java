/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.IdentityHolder;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.security.Security;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.SegmentedConcurrentMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ScopedTopicReferenceStore} holds scoped {@link NamedTopic} references.
 * <p>
 * {@link NamedTopic} references are scoped by ClassLoader and, optionally, Subject.
 * ScopedTopicReferenceStore requires no explicit input about Subjects from its clients.
 * Subject scoping is configured in the operational configuration and applies only to
 * remote topics.
 * <p>
 * Thread safety documented in {@link AbstractScopedReferenceStore}.
 *
 * @author vp 2022.05.17.
 * @since 22.06
 */
@SuppressWarnings("rawtypes")
public class ScopedTopicReferenceStore
        extends ScopedReferenceStore<NamedTopic>
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Create a {@link ScopedTopicReferenceStore} to hold references to the topics.
     */
    public ScopedTopicReferenceStore()
        {
        super(NamedTopic.class, NamedTopic::isActive, NamedTopic::getName, NamedTopic::getService);
        }

    /**
     * Remove topic references from this store for all destroyed and released topics.
     */
    public void clearInactiveTopicRefs()
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

                    if (oHolder instanceof NamedTopic)
                        {
                        NamedTopic topic = (NamedTopic) entry.getValue();

                        if (topic.isDestroyed() || topic.isReleased())
                            {
                            iter.remove();
                            internalReleaseTopic(topic);
                            }
                        }
                    else if (oHolder instanceof SubjectScopedReference)
                        {
                        Collection col = ((SubjectScopedReference) oHolder).values();

                        if (!col.isEmpty())
                            {
                            NamedTopic topic = (NamedTopic) col.iterator().next();
                            if (topic.isDestroyed() || topic.isReleased())
                                {
                                iter.remove();
                                internalReleaseTopic(topic);
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
     * Retrieve the topic reference associated with the ClassLoader (and
     * Subject if applicable).
     *
     * @param sTopicName  the name of the topic
     * @param loader      the cache's ClassLoader
     *
     * @return the topic reference
     */
    public NamedTopic getTopic(String sTopicName, ClassLoader loader)
        {
        Map mapByLoader = (Map) m_mapByName.get(sTopicName);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == null || oHolder instanceof NamedTopic)
                    {
                    return (NamedTopic) oHolder;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    return (NamedTopic) ((SubjectScopedReference) oHolder).get();
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
     * Store a topic reference with the supplied ClassLoader.
     *
     * @param topic   the topic reference
     * @param loader  the ClassLoader
     */
    public void putTopic(NamedTopic topic, ClassLoader loader)
        {
        if (topic.isReleased())
            {
            throw new IllegalArgumentException("Storing a released topic is not allowed: " + topic.getName());
            }

        ConcurrentMap mapByName = m_mapByName;
        String sTopicName = topic.getName();
        Map mapByLoader = (Map) mapByName.get(sTopicName);

        if (mapByLoader == null)
            {
            mapByLoader = new WeakHashMap();
            mapByName.put(sTopicName, mapByLoader);
            }

        if (ScopedServiceReferenceStore.isRemoteServiceType(topic.getService().getInfo().getServiceType())
            && Security.SUBJECT_SCOPED)
            {
            SubjectScopedReference scopedRef = (SubjectScopedReference) mapByLoader.get(loader);

            if (scopedRef == null)
                {
                scopedRef = new SubjectScopedReference();
                mapByLoader.put(loader, scopedRef);
                }

            scopedRef.set(topic);
            }
        else
            {
            mapByLoader.put(loader, topic);
            }
        }

    /**
     * Store a topic reference with the supplied ClassLoader only if
     * it does not already exist.
     *
     * @param topic   the topic reference
     * @param loader  the ClassLoader
     *
     * @return the previous value associated with the specified loader,
     *         or null if there was no mapping for the key and put succeeded
     */
    public Object putTopicIfAbsent(NamedTopic topic, ClassLoader loader)
        {
        if (topic.isReleased())
            {
            throw new IllegalArgumentException("Storing a released topic is not allowed: " + topic.getName());
            }

        SegmentedConcurrentMap mapByName = m_mapByName;

        String sTopicName = topic.getName();
        Object oResult;
        Map    mapByLoader;
        do
            {
            mapByLoader = (Map) mapByName.get(sTopicName);
            if (mapByLoader == null)
                {
                mapByLoader = new WeakHashMap();

                Map mapTmp = (Map) mapByName.putIfAbsent(sTopicName, mapByLoader);

                if (mapTmp != null)
                    {
                    mapByLoader = mapTmp;
                    }
                }

            if (ScopedServiceReferenceStore.isRemoteServiceType(topic.getService().getInfo().getServiceType())
            && Security.SUBJECT_SCOPED)
                {
                SubjectScopedReference scopedRef = new SubjectScopedReference();

                oResult = putLoaderIfAbsent(mapByLoader, loader, scopedRef);

                if (oResult != null)
                    {
                    scopedRef = (SubjectScopedReference) oResult;
                    }

                oResult = scopedRef.putIfAbsent(topic);
                }
            else
                {
                oResult = putLoaderIfAbsent(mapByLoader, loader, topic);
                }
            }
         while (mapByName.get(sTopicName) != mapByLoader);

        return oResult;
        }

    /**
     * Remove the referenced topic item from the store using the supplied
     * ClassLoader.
     *
     * @param topic   the topic reference
     * @param loader  the ClassLoader
     *
     * @return whether the item was found
     */
    public boolean releaseTopic(NamedTopic topic, ClassLoader loader)
        {
        return releaseTopic(topic, loader, null);
        }

    /**
     * Atomically remove the referenced topic item from the store and execute <code>postRelease</code>.
     *
     * @param topic        the topic reference
     * @param loader       the ClassLoader
     * @param postRelease  run after topic reference removed from store
     *
     * @return whether the item was found.
     */
    public boolean releaseTopic(NamedTopic topic, ClassLoader loader, Runnable postRelease)
        {
        Map                        mapByName   = m_mapByName;
        String                     sTopicName  = topic.getName();
        Map                        mapByLoader = (Map) mapByName.get(sTopicName);
        boolean                    fFound      = false;
        Object                     oPending    = null;
        IdentityHolder<NamedTopic> topicId     = new IdentityHolder<>(topic);

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Object oHolder = mapByLoader.get(loader);

                if (oHolder == topic)
                    {
                    // remove the mapping
                    mapByLoader.remove(loader);
                    fFound = true;
                    }
                else if (oHolder instanceof SubjectScopedReference)
                    {
                    SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                    if (scopedRef.get() == topic)
                        {
                        scopedRef.remove();
                        fFound = true;

                        if (scopedRef.isEmpty())
                            {
                            mapByLoader.remove(loader);
                            }
                        }
                    }

                // run postRelease outside of synchronize block
                if (fFound && postRelease != null)
                    {
                    f_mapPending.put(topicId, oPending = new Object());
                    }

                // remove the loader map if this was the last topic by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sTopicName);
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
                f_mapPending.remove(topicId, oPending);

                synchronized (oPending)
                    {
                    oPending.notifyAll();
                    }
                }
            }

        if (!fFound)
            {
            // wait if there exist a pending postRelease for cacheId
            awaitPending(topicId);
            }

        return fFound;
        }

    /**
     * Remove the referenced topic item from the store.
     *
     * @param topic  the topic reference
     *
     * @return whether the item was found
     */
    public boolean releaseTopic(NamedTopic topic)
        {
        Map     mapByName   = m_mapByName;
        String  sTopicName  = topic.getName();
        Map     mapByLoader = (Map) mapByName.get(sTopicName);
        boolean fFound      = false;

        if (mapByLoader != null)
            {
            synchronized (mapByLoader)
                {
                Collection col = mapByLoader.values();

                // Assume it's a collection of NamedTopics references
                fFound = col.remove(topic);

                if (!fFound)
                    {
                    if (topic instanceof ClassLoaderAware)
                        {
                        return releaseTopic(topic, ((ClassLoaderAware) topic).getContextClassLoader());
                        }

                    // could be a collection of SubjectScopedReferences
                    for (Iterator iter = col.iterator(); iter.hasNext(); )
                        {
                        Object oHolder = iter.next();

                        if (oHolder instanceof SubjectScopedReference)
                            {
                            SubjectScopedReference scopedRef = (SubjectScopedReference) oHolder;

                            if (scopedRef.get() == topic)
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

                // remove the loader map if this was the last topic by
                // this name
                if (mapByLoader.isEmpty())
                    {
                    mapByName.remove(sTopicName);
                    }
                }
            }

        return fFound;
        }


    protected void awaitPending(IdentityHolder<NamedTopic> topicId)
        {
        Object oPending = f_mapPending.get(topicId);

        if (oPending != null)
            {
            synchronized (oPending)
                {
                if (oPending == f_mapPending.get(topicId))
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

    private void internalReleaseTopic(NamedTopic topic)
        {
        try
            {
            topic.release();
            }
        catch (RuntimeException e)
            {

            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Map of NamedTopic id to an Object, used as a semaphore to block threads requiring a released topic.
     */
    protected Map<IdentityHolder<NamedTopic>, Object> f_mapPending = new ConcurrentHashMap<>();
    }
