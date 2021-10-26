/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.oracle.coherence.concurrent.locks.CoherenceReadWriteLock.RemoveHoldersProcessor;

import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedMap;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.TransferEvent.Type;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Base;
import com.tangosol.util.EnumerationIterator;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import com.tangosol.util.processor.AsynchronousProcessor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This LockHolderCleaner is triggered by members leaving the service or partitions
 * arriving on this member. In the former case we know which LockHolders need to
 * be removed whereas on primary transfer we make sure all lock holders represent
 * valid members.
 *
 * @author hr  2021.08.19
 */
public class LockHolderCleaner
        implements MemberListener, EventDispatcherAwareInterceptor<TransferEvent>
    {
    // ----- EventInterceptor methods ---------------------------------------

    @Override
    public void onEvent(TransferEvent event)
        {
        if (event.getType() == Type.ARRIVED)
            {
            PartitionedService service = event.getDispatcher().getService();

            enqueueUpdate(event.getPartitionId(), event.getEntries().keySet(), service.getPartitionCount());
            }
        }

    // ----- EventDispatcherAwareInterceptor methods ------------------------

    @Override
    public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
        {
        if (dispatcher instanceof PartitionedServiceDispatcher)
            {
            String sServiceName = ((PartitionedServiceDispatcher) dispatcher).getService().getInfo().getServiceName();
            if (sServiceName.equals(m_sServiceName))
                {
                Set<Type> setTypes =
                        Collections.singletonMap(Type.ARRIVED, null).keySet();

                dispatcher.addEventInterceptor(null, this, setTypes, false);
                }
            }
        }

    // ----- MemberListener methods -----------------------------------------

    @Override
    public void memberJoined(MemberEvent evt)
        {
        PartitionedService service = (PartitionedService) evt.getService();

        if (evt.isLocal() && ((DistributedCacheService) service).isLocalStorageEnabled())
            {
            m_sServiceName = service.getInfo().getServiceName();

            // register this event interceptor
            ((CacheService) service).getBackingMapManager().getCacheFactory().getInterceptorRegistry()
                    .registerEventInterceptor(this);
            }
        }

    @Override
    public void memberLeaving(MemberEvent evt)
        {
        }

    @Override
    public void memberLeft(MemberEvent evt)
        {
        PartitionedService service = (PartitionedService) evt.getService();

        Member memberThis = service.getCluster().getLocalMember();
        Member memberLeft = evt.getMember();

        if (((DistributedCacheService) service).isLocalStorageEnabled())
            {
            CacheService serviceCache = (CacheService) service;
            PartitionSet partsOwned   = service.getOwnedPartitions(memberThis);

            ConfigurableCacheFactory ccf = ((CacheService) service).getBackingMapManager().getCacheFactory();

            for (Iterator<String> iterNames = new EnumerationIterator<String>(serviceCache.getCacheNames());
                 iterNames.hasNext(); )
                {
                String sCacheName = iterNames.next();

                NamedMap<String, LockReferenceCounter> cacheLocks = ccf.ensureCache(sCacheName, null);

                cacheLocks.async().invokeAll(
                        new PartitionedFilter<>(AlwaysFilter.INSTANCE(), partsOwned),
                        new RemoveHoldersProcessor(memberLeft.getId()));
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Enqueue the provided partition and cache names for a 'clean up'.
     * <p>
     * The clean up is fired on a background thread allowing bundling of subsequently
     * enqueued partitions. The background thread is fired 1s after the first
     * enqueue.
     *
     * @param iPartition     the partition to enqueue
     * @param setCacheNames  the cache names to check
     * @param cParts         the number of partitions in the associated service
     */
    protected synchronized void enqueueUpdate(int iPartition, Set<String> setCacheNames, int cParts)
        {
        Set<String>  setCaches = m_setCacheNames;
        PartitionSet parts     = m_partsCheck;
        if (parts == null)
            {
            setCaches = m_setCacheNames = new HashSet<>(setCacheNames.size());
            parts     = m_partsCheck    = new PartitionSet(cParts); // barrier

            Base.makeThread(null, new CheckHoldersRunnable(), "CheckLockHolders")
                    .start();
            }

        parts.add(iPartition);
        setCaches.addAll(setCacheNames);
        }

    // ----- inner class: CheckHoldersRunnable ------------------------------

    /**
     * A {@link Runnable} that fires an {@link AsynchronousProcessor} to validate
     * lock holders.
     */
    protected class CheckHoldersRunnable
            implements Runnable
        {
        // ----- Runnable methods -------------------------------------------

        @Override
        public void run()
            {
            Base.sleep(1_000L);

            Set<String>  setCaches;
            PartitionSet parts;

            synchronized (LockHolderCleaner.this)
                {
                setCaches = m_setCacheNames;
                parts     = m_partsCheck;

                m_setCacheNames = null;
                m_partsCheck    = null; // barrier done last
                }

            for (String sCacheName : setCaches)
                {
                AsyncNamedMap<String, LockReferenceCounter> cache =
                        CacheFactory.<String, LockReferenceCounter>getCache(sCacheName).async();

                cache.invokeAll(new PartitionedFilter<>(AlwaysFilter.INSTANCE(), parts),
                            new RemoveHoldersProcessor(-1));
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The service name.
     */
    protected volatile String m_sServiceName;

    /**
     * A {@link PartitionSet} to fire the check against.
     */
    protected volatile PartitionSet m_partsCheck;

    /**
     * The cache names to check against.
     */
    protected Set<String> m_setCacheNames;
    }
