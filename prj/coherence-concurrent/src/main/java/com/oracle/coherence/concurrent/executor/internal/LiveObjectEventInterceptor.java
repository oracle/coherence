/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.concurrent.executor.ComposableContinuation;
import com.oracle.coherence.concurrent.executor.PortableAbstractProcessor;
import com.oracle.coherence.concurrent.executor.ContinuationService;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;

import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.DaemonThreadFactory;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SafeHashSet;
import com.tangosol.util.UID;

import java.io.IOException;

import java.util.Collections;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@link LiveObjectEventInterceptor} is the {@link EventInterceptor} that forwards {@link Event}s to objects
 * implements {@link LiveObject}.
 * <p>
 * IMPORTANT: The delegation of events will occur on Coherence Service threads. It is up to the {@link LiveObject}
 * implementations to ensure that appropriate processing occurs on non-Coherence-Service threads to prevent deadlock
 * from occurring.
 *
 * @author bo, lh
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LiveObjectEventInterceptor
        implements EventDispatcherAwareInterceptor, MemberListener
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link LiveObjectEventInterceptor}.
     */
    public LiveObjectEventInterceptor()
        {
        f_executorService       = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("LiveObjectThread-"));
        f_continuationService   = new ContinuationService<>(new DaemonThreadFactory("ContinuationService-"));
        f_mapLeaseExpiryTimes   = new ConcurrentHashMap<>();
        f_mapMemberAwareObjects = new ConcurrentHashMap<>();

        // schedule checking leases
        f_executorService.scheduleAtFixedRate(new LeaseInspectorRunnable(), LEASE_INSPECTION_DELAY_MS,
                LEASE_INSPECTION_DELAY_MS, TimeUnit.MILLISECONDS);
        }

    // ----- EventInterceptor interface -------------------------------------

    @Override
    public void onEvent(Event event)
        {
        Set<BinaryEntry> setBinaryEntries = Collections.emptySet();
        Set<BinaryEntry> setAssignments   = Collections.emptySet();
        Set<BinaryEntry> setTasks         = Collections.emptySet();
        Cause            cause            = Cause.REGULAR;

        if (event instanceof TransactionEvent)
            {
            setBinaryEntries = ((TransactionEvent) event).getEntrySet();
            }
        else if (event instanceof TransferEvent)
            {
            setBinaryEntries = ((TransferEvent) event).getEntries().get(Caches.EXECUTORS_CACHE_NAME);

            if (setBinaryEntries == null)
                {
                setBinaryEntries = Collections.emptySet();
                }

            setAssignments = ((TransferEvent) event).getEntries().get(Caches.ASSIGNMENTS_CACHE_NAME);

            if (setAssignments == null)
                {
                setAssignments =Collections.emptySet();
                }

            setTasks = ((TransferEvent) event).getEntries().get(Caches.TASKS_CACHE_NAME);

            if (setTasks == null)
                {
                setTasks = Collections.emptySet();
                }

            cause = Cause.PARTITIONING;
            }

        for (BinaryEntry binaryEntry : setBinaryEntries)
            {
            processEntry(event, binaryEntry, cause);
            }

        for (BinaryEntry binaryEntry : setAssignments)
            {
            processEntry(event, binaryEntry, cause);
            }

        for (BinaryEntry binaryEntry : setTasks)
            {
            processEntry(event, binaryEntry, cause);
            }
        }

    // ----- EventDispatcherAwareInterceptor interface ----------------------

    @Override
    public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
        {
        if (dispatcher instanceof PartitionedServiceDispatcher)
            {
            m_cacheService = (CacheService) ((PartitionedServiceDispatcher) dispatcher).getService();
            m_cacheService.addMemberListener(this);

            dispatcher.addEventInterceptor(sIdentifier, this);

            Hook.addShutdownHook(m_cacheService.getBackingMapManager().getCacheFactory().getResourceRegistry(),
                                 () ->
                                      {
                                      f_executorService.shutdown();
                                      f_continuationService.shutdown();
                                      });
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Gets the cache service with which this interceptor has been registered.
     *
     * @return the {@link CacheService}
     */
    public CacheService getCacheService()
        {
        return m_cacheService;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Process an {@link BinaryEntry} from an interceptor event.
     *
     * @param event        the {@link Event}
     * @param binaryEntry  the {@link BinaryEntry}
     * @param cause        the cause for the {@link Event}
     */
    @SuppressWarnings("ConstantConditions")
    protected void processEntry(Event event,
                                BinaryEntry binaryEntry,
                                Cause cause)
        {
        Object          oValue;
        Object          oKey         = binaryEntry.getKey();
        EntryEvent.Type type         = EntryEvent.Type.INSERTING; // set the default to a type not used
        boolean         fTransfer    = event instanceof TransferEvent;
        boolean         fTransaction = event instanceof TransactionEvent;

        if (fTransfer)
            {
            TransferEvent.Type transType = ((TransferEvent) event).getType();

            if (transType == TransferEvent.Type.ARRIVED || transType == TransferEvent.Type.RECOVERED)
                {
                type = EntryEvent.Type.INSERTED;
                }
            else if (transType == TransferEvent.Type.DEPARTING)
                {
                type = EntryEvent.Type.REMOVED;
                }
            }
        else if (fTransaction)
            {
            if (((TransactionEvent) event).getType() == TransactionEvent.Type.COMMITTED)
                {
                if (binaryEntry.getOriginalValue() == null)
                    {
                    type = EntryEvent.Type.INSERTED;
                    }
                else if (binaryEntry.getValue() == null)
                    {
                    type = EntryEvent.Type.REMOVED;
                    }
                else
                    {
                    type = EntryEvent.Type.UPDATED;
                    }
                }
            }

        BackingMapManagerContext context = binaryEntry.getContext();

        if (context.isKeyOwned(binaryEntry.getBinaryKey()))
            {
            if (isDecorationRemoved(binaryEntry, ExternalizableHelper.DECO_STORE))
                {
                cause = Cause.STORE_COMPLETED;
                }
            else
                {
                cause = event instanceof CacheEvent
                        && ((CacheEvent) event).isSynthetic() ? Cause.EVICTION : Cause.REGULAR;
                }
            }

        switch (type)
            {
            case INSERTED:
                oValue = binaryEntry.getValue();

                // insert the lease expiry time for Leased objects
                if (oValue instanceof Leased)
                    {
                    f_mapLeaseExpiryTimes.put(oKey, ((Leased) oValue).getLeaseExpiryTime());
                    }

                // insert ClusterMemberAware objects for receiving membership events
                if (oValue instanceof ClusterMemberAware)
                    {
                    UID uid = ((ClusterMemberAware) oValue).getUid();

                    if (uid != null)
                        {
                        // check if uid is still in the cluster
                        boolean fMemberFound = false;

                        // explicit cast is required for Coherence 14.1.1 and earlier
                        //noinspection RedundantCast
                        for (Member member : (Set<Member>) CacheFactory.getCluster().getMemberSet())
                            {
                            if (member.getUid().equals(uid))
                                {
                                fMemberFound = true;
                                break;
                                }
                            }

                        if (fMemberFound)
                            {
                            Set<Object> set = f_mapMemberAwareObjects.get(uid);

                            ExecutorTrace.log(() -> String.format("Adding [%s] into ClusterMemberAware table for UID [%s].",
                                                              oKey, uid));

                            if (set == null)
                                {
                                set = new SafeHashSet();
                                f_mapMemberAwareObjects.put(uid, set);
                                }

                            set.add(oKey);
                            }
                        else
                            {
                            ExecutorTrace.log(() -> String.format("Member with UID [%s] has left the cluster.  Invoking MemberLeft event on [%s].", uid, oKey));
                            f_executorService.submit(new MemberEventRunnable(MemberEvent.MEMBER_LEFT,
                                                                             Collections.singleton(oKey)));
                            }
                        }
                    }

                if (oValue instanceof LiveObject)
                    {
                    ComposableContinuation continuation;

                    if (fTransfer)
                        {
                        continuation =
                            ((LiveObject) oValue).onInserted((CacheService) ((TransferEvent) event).getDispatcher()
                                                                .getService(),
                                                            binaryEntry,
                                                            cause);
                        }
                    else if (fTransaction)
                        {
                        continuation =
                            ((LiveObject) oValue).onInserted((CacheService) ((TransactionEvent) event).getDispatcher()
                                                                .getService(),
                                                            binaryEntry,
                                                            cause);
                        }
                    else
                        {
                        continuation = null;
                        }

                    f_continuationService.submit(continuation, binaryEntry.getKey());
                    }

                break;

            case UPDATED:
                oValue = binaryEntry.getValue();

                // update the lease expiry time for Leased objects
                if (oValue instanceof Leased)
                    {
                    f_mapLeaseExpiryTimes.put(oKey, ((Leased) oValue).getLeaseExpiryTime());
                    }

                // no ClusterMemberAware related actions required

                if (oValue instanceof LiveObject)
                    {
                    ComposableContinuation continuation;

                    if (fTransfer)
                        {
                        continuation =
                            ((LiveObject) oValue).onUpdated((CacheService) ((TransferEvent) event).getDispatcher()
                                                               .getService(),
                                                           binaryEntry,
                                                           cause);
                        }
                    else if (fTransaction)
                        {
                        continuation =
                            ((LiveObject) oValue).onUpdated((CacheService) ((TransactionEvent) event).getDispatcher()
                                                               .getService(),
                                                           binaryEntry,
                                                           cause);
                        }
                    else
                        {
                        continuation = null;
                        }

                    f_continuationService.submit(continuation, binaryEntry.getKey());
                    }

                break;

            case REMOVED:
                oValue = binaryEntry.getOriginalValue();

                // remove the lease expiry time for Leased objects
                if (oValue instanceof Leased)
                    {
                    f_mapLeaseExpiryTimes.remove(oKey);
                    }

                // remove ClusterMemberAware objects
                if (oValue instanceof ClusterMemberAware)
                    {
                    UID uid = ((ClusterMemberAware) oValue).getUid();

                    if (uid != null)
                        {
                        Set<Object> set = f_mapMemberAwareObjects.get(uid);

                        ExecutorTrace.log(() -> String.format("Removing [%s] from ClusterMemberAware table for UID [%s].",
                                                          oKey, uid));

                        if (set != null)
                            {
                            set.remove(oKey);

                            if (set.isEmpty())
                                {
                                f_mapMemberAwareObjects.remove(uid);
                                }
                            }
                        }
                    }

                if (oValue instanceof LiveObject)
                    {
                    ComposableContinuation continuation;

                    if (fTransfer)
                        {
                        continuation =
                            ((LiveObject) oValue).onDeleted((CacheService) ((TransferEvent) event).getDispatcher()
                                                               .getService(),
                                                           binaryEntry,
                                                           cause);
                        }
                    else if (fTransaction)
                        {
                        continuation =
                            ((LiveObject) oValue).onDeleted((CacheService) ((TransactionEvent) event).getDispatcher()
                                                               .getService(),
                                                           binaryEntry,
                                                           cause);
                        }
                    else
                        {
                        continuation = null;
                        }

                    f_continuationService.submit(continuation, binaryEntry.getKey());
                    }

                break;
            }
        }

    /**
     * Helper method to submit a {@link MemberEventRunnable} on a {@link MemberEvent}.
     *
     * @param event  the {@link MemberEvent} to process.
     */
    private void onMemberEvent(MemberEvent event)
        {
        Set<Object> set = f_mapMemberAwareObjects.get(event.getMember().getUid());

        if (set != null)
            {
            f_executorService.submit(new MemberEventRunnable(event.getId(), set));
            }
        }

    /**
     * Determines whether the given decoration has been removed from the event's new value, i.e., the decoration exists
     * on the old value but not on the new.
     *
     * @param binEntry       the {@link BinaryEntry}
     * @param nDecorationId  the decoration
     *
     * @return true if the decoration has been removed for the new value
     */
    protected boolean isDecorationRemoved(BinaryEntry binEntry, int nDecorationId)
        {
        Binary binOldValue = binEntry.getOriginalBinaryValue();
        Binary binNewValue = binEntry.getBinaryValue();

        BackingMapManagerContext context = binEntry.getBackingMapContext().getManagerContext();

        return (binOldValue != null
                && context.isInternalValueDecorated(binOldValue, nDecorationId)
                && !context.isInternalValueDecorated(binNewValue, nDecorationId));
        }

    // ----- MemberListener interface ---------------------------------------

    @Override
    public void memberJoined(MemberEvent memberEvent)
        {
        onMemberEvent(memberEvent);
        }

    @Override
    public void memberLeaving(MemberEvent memberEvent)
        {
        onMemberEvent(memberEvent);
        }

    @Override
    public void memberLeft(MemberEvent memberEvent)
        {
        onMemberEvent(memberEvent);
        }

    // ----- inner class: ExpiredProcessor ----------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to notify a {@link Leased} that
     * it has expired.
     */
    public static class ExpiredProcessor
            extends PortableAbstractProcessor<String, Leased, Boolean>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link ExpiredProcessor} (required for serialization).
         */
        public ExpiredProcessor()
            {
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Boolean process(InvocableMap.Entry entry)
            {
            if (entry.isPresent() && entry.getValue() instanceof Leased)
                {
                Leased leased = (Leased) entry.getValue();

                if (leased.onLeaseExpiry())
                    {
                    entry.setValue(leased);
                    }

                return true;
                }
            else
                {
                ExecutorTrace.log(() -> String.format("ExpiredProcessor can't expire [%s] as it is not present.",
                                                  entry.getKey()));

                return false;
                }
            }
        }

    // ----- inner class: LeaseInspectorRunnable ----------------------------

    /**
     * A {@link Runnable} to asynchronously check the status of the currently {@link Leased} entries, notifying them if
     * they are expired and removing them as required.
     */
    class LeaseInspectorRunnable
            implements Runnable
        {
        // ----- Runnable interface -----------------------------------------

        @Override
        public void run()
            {
            long   ldtCurrentTime = CacheFactory.getSafeTimeMillis();
            String sCacheName     = Caches.EXECUTORS_CACHE_NAME;

           ExecutorTrace.log(() -> String.format("Commenced Inspecting Lease Expiry Times for [%s].", sCacheName));

            for (Object oKey : f_mapLeaseExpiryTimes.keySet())
                {
                Long ldtExpiryTime = f_mapLeaseExpiryTimes.get(oKey);

                if (ldtExpiryTime != null && ldtExpiryTime <= ldtCurrentTime)
                    {
                    ExecutorTrace.log(() -> String.format("Lease for [%s] has expired for [%s].", oKey, sCacheName));

                    boolean updated = (Boolean) getCacheService()
                            .ensureCache(sCacheName, null).invoke(oKey, new ExpiredProcessor());

                    if (!updated)
                        {
                        ExecutorTrace.log(() -> String.format("Removing Lease for [%s] as the lease could not be updated.",
                                                          oKey));

                        f_mapLeaseExpiryTimes.remove(oKey);
                        }
                    }
                else
                    {
                    ExecutorTrace.log(() -> String.format("Lease for [%s] is valid for [%s].", oKey, sCacheName));
                    }
                }

            ExecutorTrace.log(() -> String.format("Completed Inspecting Lease Expiry Times for [%s].", sCacheName));
            }
        }

    // ----- inner class: MemberAwareProcessor ------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to notify a {@link ClusterMemberAware} of a {@link MemberEvent}.
     */
    public static class MemberAwareProcessor
            extends PortableAbstractProcessor<String, ClusterMemberAware, Boolean>
        {
        // ----- constructors -----------------------------------

        /**
         * Constructs an {@link MemberAwareProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public MemberAwareProcessor()
            {
            }

        /**
         * The {@link MemberAwareProcessor} constructor.
         *
         * @param nId  the {@link MemberEvent#getId()}
         */
        public MemberAwareProcessor(int nId)
            {
            m_nId = nId;
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Boolean process(InvocableMap.Entry entry)
            {
            if (entry.isPresent() && entry.getValue() instanceof ClusterMemberAware)
                {
                ClusterMemberAware memberAware = (ClusterMemberAware) entry.getValue();
                boolean            fSubmit = false;

                switch (m_nId)
                    {
                    case MemberEvent.MEMBER_JOINED:
                        fSubmit = memberAware.onMemberJoined();
                        break;

                    case MemberEvent.MEMBER_LEAVING:
                        fSubmit = memberAware.onMemberLeaving();
                        break;

                    case MemberEvent.MEMBER_LEFT:
                        fSubmit = memberAware.onMemberLeft();
                        break;
                    }

                if (fSubmit)
                    {
                    entry.setValue(memberAware);
                    }

                return true;
                }
            else
                {
                if (!entry.isPresent())
                    {
                    ExecutorTrace.log(() -> String.format("MemberAwareProcessor can't call [%s] as it is not present.",
                                                      entry.getKey()));
                    }
                else
                    {
                    ExecutorTrace.log(() -> String.format("MemberAwareProcessor can't call [%s] as it does not implement ClusterMemberAware.", entry.getKey()));
                    }

                return false;
                }
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_nId = in.readInt(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_nId);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link MemberEvent#getId()}.
         */
        protected int m_nId;
        }

    /**
     * A {@link Runnable} to asynchronously notify a set of {@link ClusterMemberAware} entries of a {@link
     * MemberEvent}.
     */
    class MemberEventRunnable
            implements Runnable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link MemberEventRunnable}.
         *
         * @param nId             the {@link MemberEvent#getId()}
         * @param setMemberAware  the set of keys for {@link ClusterMemberAware} entries
         */
        MemberEventRunnable(int nId, Set<Object> setMemberAware)
            {
            f_nId            = nId;
            f_setMemberAware = setMemberAware;
            }

        @Override
        public void run()
            {
            Caches.executors(getCacheService()).invokeAll(f_setMemberAware, new MemberAwareProcessor(f_nId));
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link MemberEvent#getId()}.
         */
        protected final int f_nId;

        /**
         * The set of keys for {@link ClusterMemberAware} entries.
         */
        protected final Set<Object> f_setMemberAware;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The delay in milliseconds between successive attempts to inspect the expiry of local {@link Leased} objects.
     */
    public static long LEASE_INSPECTION_DELAY_MS = 10 * 1000; // ten seconds

    // ----- data members ---------------------------------------------------

    /**
     * The CacheService to which the EventInterceptor has been associated.
     */
    protected CacheService m_cacheService;

    /**
     * An {@link ExecutorService} on which to perform background operations.
     */
    protected final ScheduledExecutorService f_executorService;

    /**
     * A {@link ContinuationService} for tracking, composing and executing
     * {@link ComposableContinuation}s.
     */
    protected final ContinuationService<Object> f_continuationService;

    /**
     * A map of keys for {@link Leased} cache entry values and their lease expiry times.
     */
    protected final ConcurrentHashMap<Object, Long> f_mapLeaseExpiryTimes;

    /**
     * A map of keys for {@link ClusterMemberAware} cache entry values.
     */
    protected final ConcurrentHashMap<UID, Set<Object>> f_mapMemberAwareObjects;
    }
