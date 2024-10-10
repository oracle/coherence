/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.NonBlocking;
import com.tangosol.coherence.component.net.Lease;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.StorageRequest;
import com.tangosol.coherence.component.net.requestContext.AsyncContext;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Logger;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.Member;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceStoppedException;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.Listeners;
import com.tangosol.util.LiteMap;
import com.tangosol.util.LiteSet;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.PagedIterator;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.filter.LimitFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static com.tangosol.internal.util.VersionHelper.VERSION_14_1_1_2206_7;
import static com.tangosol.internal.util.VersionHelper.VERSION_23_09;
import static com.tangosol.internal.util.VersionHelper.VERSION_23_09_1;
import static com.tangosol.internal.util.VersionHelper.VERSION_14_1_2_0;
import static com.tangosol.internal.util.VersionHelper.isPatchCompatible;
import static com.tangosol.internal.util.VersionHelper.isVersionCompatible;

/**
 * The internal view of the distributed cache.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class BinaryMap
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.NamedCache
    {
    // ---- Fields declarations ----

    /**
     * Property AuthorizationEnabled
     *
     * Specifies whether or not the StorageAccessAuthorizer is configured
     * for this cache.
     */
    private boolean __m_AuthorizationEnabled;

    /**
     * Property CacheId
     *
     * The cache id this BinaryMap represents.
     *
     * @see PartitionedCache#StorageArray
     */
    private long __m_CacheId;

    /**
     * Property CacheName
     *
     * The cache name this BinaryMap represents.
     */
    private String __m_CacheName;

    /**
     * Property Confirmed
     *
     * Flag indicating whether or not the existence of the cache associated
     * with this $BinaryMap has been confirmed with the partition owners.
     *
     * @volatile
     */
    private volatile transient boolean __m_Confirmed;

    /**
     * Property Dispatcher
     *
     * An EventDispatcher that can dispatch cache lifecycle events.
     *
     * This is null for storage-enabled members and non-null otherwise.
     */
    private com.tangosol.net.events.internal.StorageDispatcher __m_Dispatcher;

    /**
     * Property FilterArray
     *
     * A LongArray of Filter objects indexed by the unique filter id. These
     * filter id values are used by the MapEvent message to specify what
     * filters caused a cache event.
     *
     * Note: all access (for update) to this array should be synchronized
     * on the ListenerSupport object.
     */
    private com.tangosol.util.LongArray __m_FilterArray;

    /**
     * Property ListenerSupport
     *
     */
    private transient com.tangosol.util.MapListenerSupport __m_ListenerSupport;

    /**
     * Property ReadLocator
     *
     * A BiFunction<Ownership,PartitionedService,Member> that returns the
     * Member to target read requests against.
     */
    private java.util.function.BiFunction __m_ReadLocator;

    /**
     * Property RESPONSE_MAP
     *
     * Indicates a response of a java.util.Map type.
     */
    public static final int RESPONSE_MAP = 3;

    /**
     * Property RESPONSE_SET
     *
     * Indicates a response of a java.util.Set type.
     */
    public static final int RESPONSE_SET = 2;

    /**
     * Property RESPONSE_SIMPLE
     *
     * Indicates a simple response (java.lang.Object).
     */
    public static final int RESPONSE_SIMPLE = 1;

    /**
     * Property RESPONSE_VOID
     *
     * Indicates a void (null) response.
     */
    public static final int RESPONSE_VOID = 0;
    private static com.tangosol.util.ListMap __mapChildren;

    // Static initializer
    static
        {
        __initStatic();
        }

    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Entry", BinaryMap.Entry.get_CLASS());
        __mapChildren.put("EntryAdvancer", BinaryMap.EntryAdvancer.get_CLASS());
        __mapChildren.put("KeyAdvancer", BinaryMap.KeyAdvancer.get_CLASS());
        __mapChildren.put("KeyRequestStatus", BinaryMap.KeyRequestStatus.get_CLASS());
        __mapChildren.put("KeySetRequestStatus", BinaryMap.KeySetRequestStatus.get_CLASS());
        __mapChildren.put("MapRequestStatus", BinaryMap.MapRequestStatus.get_CLASS());
        __mapChildren.put("PartialRequestStatus", BinaryMap.PartialRequestStatus.get_CLASS());
        }

    // Default constructor
    public BinaryMap()
        {
        this(null, null, true);
        }

    // Initializing constructor
    public BinaryMap(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);

        if (fInit)
            {
            __init();
            }
        }

    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setFilterArray(new com.tangosol.util.SparseArray());
            setListenerSupport(new com.tangosol.util.MapListenerSupport());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }

        // containment initialization: children
        _addChild(new BinaryMap.EntrySet("EntrySet", this, true), "EntrySet");
        _addChild(new BinaryMap.KeySet("KeySet", this, true), "KeySet");
        _addChild(new BinaryMap.Values("Values", this, true), "Values");

        // signal the end of the initialization
        set_Constructed(true);
        }

    // Private initializer
    protected void __initPrivate()
        {

        super.__initPrivate();
        }

    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
     * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new BinaryMap();
        }

    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
     * Property with auto-generated accessor that returns the Class object
     * for a given component.
     */
    public static Class get_CLASS()
        {
        return BinaryMap.class;
        }

    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design
     * time] parent component.
     *
     * Note: the class generator will ignore any custom implementation for
     * this behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this.get_Parent();
        }

    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
     * [static] children.
     *
     * Note: the class generator will ignore any custom implementation for
     * this behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }

    // From interface: com.tangosol.net.NamedCache
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.IndexRequest msg =
                    (PartitionedCache.IndexRequest) service.instantiateMessage("IndexRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setAdd(true);
            msg.setExtractor(extractor);
            msg.setOrdered(fOrdered);
            msg.setComparator(comparator);

            sendStorageRequest(msg);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;

        addMapListener(listener, (Filter) null, null, false, null);
        }

    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, com.tangosol.util.MapTrigger trigger, boolean fLite, com.tangosol.net.partition.VersionedPartitions versions)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;

        com.tangosol.util.MapListenerSupport support   = getListenerSupport();
        long    lFilterId = 0L;

        if (trigger == null)
            {
            synchronized (support)
                {
                boolean fWasEmpty = support.isEmpty(filter);

                if (!support.addListenerWithCheck(listener, filter, fLite))
                    {
                    return;
                    }

                lFilterId = fWasEmpty ? registerFilter(filter) : getFilterId(filter);
                }
            }

        try
            {
            sendMapListenerRequest(lFilterId, filter, trigger, fLite, versions);
            }
        catch (RuntimeException e)
            {
            if (lFilterId > 0)
                {
                synchronized (support)
                    {
                    support.removeListener(listener, filter);
                    getFilterArray().remove(lFilterId);
                    }
                }
            throw e;
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        throw new UnsupportedOperationException();
        }

    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        throw new UnsupportedOperationException();
        }

    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite, boolean fPriming, boolean fVersioned, long lVersion)
        {
        // import com.tangosol.util.Binary;

        Binary binKey = (Binary) oKey;

        boolean fNew = getListenerSupport().addListenerWithCheck(listener, binKey, fLite);

        // "priming" request should be sent regardless
        if (fNew || fPriming || fVersioned)
            {
            try
                {
                sendMapListenerRequest(binKey, fLite, fPriming, fVersioned, lVersion);
                }
            catch (RuntimeException e)
                {
                if (fNew)
                    {
                    getListenerSupport().removeListener(listener, binKey);
                    }
                throw e;
                }
            }
        }

    /**
     * Note: as of Coherence 12.2.1, this method is only called with fLite
     * == true and fPriming == true.
     */
    public void addMapListener(com.tangosol.util.MapListener listener, java.util.Set setBinKeys, boolean fLite, boolean fPriming, com.tangosol.net.partition.VersionedPartitions versions)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        Map mapKeys = new HashMap(); // <Binary, Boolean>

        for (Iterator iter = setBinKeys.iterator(); iter.hasNext();)
            {
            Binary binKey = (Binary) iter.next();

            boolean fNew = support.addListenerWithCheck(listener, binKey, fLite);

            // "priming" requests should be sent regardless
            if (fNew || fPriming)
                {
                mapKeys.put(binKey, Boolean.valueOf(fNew));
                }
            }

        if (!mapKeys.isEmpty())
            {
            try
                {
                sendMapListenerAllRequest(listener, mapKeys.keySet(), fLite, fPriming, /*fAdd*/true, versions);
                }
            catch (RuntimeException e)
                {
                // only unregister the "new" listeners
                for (Iterator iter = mapKeys.entrySet().iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                    if (entry.getValue() == Boolean.TRUE)
                        {
                        support.removeListener(listener, entry.getKey());
                        }
                    }

                throw e;
                }
            }
        }

    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.Binary binAgent, com.tangosol.net.partition.PartitionSet partitions, com.tangosol.net.PriorityTask task)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.AggregateFilterRequest msg =
                    (PartitionedCache.AggregateFilterRequest) service.instantiateMessage("AggregateFilterRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setFilter(filter);
            msg.setAggregatorBinary(binAgent);
            msg.copyPriorityAttributes(task);

            return mergePartialResponse(sendPartitionedRequest(msg, partitions, false));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e, RESPONSE_SIMPLE);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.Binary binAgent, com.tangosol.net.PriorityTask task)
        {
        return aggregate(filter, binAgent, makePartitionSet(), task);
        }

    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        throw new UnsupportedOperationException();
        }

    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent, com.tangosol.net.partition.PartitionSet partitions)
        {
        throw new UnsupportedOperationException();
        }

    public Object aggregate(java.util.Collection colKeys, com.tangosol.util.Binary binAgent, com.tangosol.net.PriorityTask task)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;
        // import java.util.Collections;
        // import java.util.HashSet;
        // import java.util.Set;

        Set setKeys = colKeys instanceof Set ? (Set) colKeys : new HashSet(colKeys);
        int cKeys   = setKeys.size();
        if (cKeys == 0)
            {
            return Collections.EMPTY_LIST;
            }

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.AggregateAllRequest msg =
                    (PartitionedCache.AggregateAllRequest) service.instantiateMessage("AggregateAllRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setAggregatorBinary(binAgent);
            msg.copyPriorityAttributes(task);

            return mergePartialResponse(sendPartitionedRequest(msg, setKeys));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e, RESPONSE_SIMPLE);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        throw new UnsupportedOperationException();
        }

    public void aggregateAsync(com.tangosol.util.Filter filter, com.tangosol.util.Binary binAgent, com.tangosol.net.partition.PartitionSet partitions, com.tangosol.net.PriorityTask task, com.tangosol.util.aggregator.AbstractAsynchronousAggregator asyncAggr, com.tangosol.util.Converter convUp)
        {
        // import Component.Net.RequestContext.AsyncContext;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.base.NonBlocking;

        // call "user" methods *before* creating the context
        long    lOrderId  = asyncAggr.getUnitOfOrderId();

        PartitionedCache             service     = getService();
        PartitionedCache.RequestCoordinator coordinator = service.getRequestCoordinator();
        AsyncContext context     = coordinator.createContext(this, asyncAggr, convUp);

        if (partitions.isEmpty())
            {
            context.processCompletion();
            return;
            }

        PartitionedCache.AggregateFilterRequest msg =
                (PartitionedCache.AggregateFilterRequest) service.instantiateMessage("AggregateFilterRequest");
        msg.setRequestContext(context);
        msg.setCacheId(getCacheId());
        msg.setFilter(filter);
        msg.setAggregatorBinary(binAgent);
        msg.copyPriorityAttributes(task);
        msg.setOrderId(lOrderId | (((long) service.getThisMember().getId()) << 32));

        context.setPartitionSet(partitions);

        boolean fSubmitted = coordinator.submitPartialRequest(msg, partitions, /*fRepeat*/false);
        
        if (fSubmitted)
            {
            if (!NonBlocking.isNonBlockingCaller())
                {
                service.flush();

                try
                    {
                    coordinator.drainBacklog(partitions, msg.checkTimeoutRemaining());
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // submit failed
            context.processCompletion();
            }
        }

    public void aggregateAsync(java.util.Collection colBinKeys, com.tangosol.util.Binary binAgent, com.tangosol.net.PriorityTask task, com.tangosol.util.aggregator.AbstractAsynchronousAggregator asyncAggr, com.tangosol.util.Converter convUp)
        {
        // import Component.Net.RequestContext.AsyncContext;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.base.NonBlocking;
        // import java.util.HashSet;
        // import java.util.Set;

        // call "user" methods *before* creating the context
        long    lOrderId  = asyncAggr.getUnitOfOrderId();

        PartitionedCache             service     = getService();
        PartitionedCache.RequestCoordinator coordinator = service.getRequestCoordinator();
        AsyncContext        context     = coordinator.createContext(this, asyncAggr, convUp);

        if (colBinKeys.size() == 0)
            {
            context.processCompletion();
            return;
            }

        PartitionedCache.AggregateAllRequest msg =
                (PartitionedCache.AggregateAllRequest) service.instantiateMessage("AggregateAllRequest");
        msg.setRequestContext(context);
        msg.setCacheId(getCacheId());
        msg.setAggregatorBinary(binAgent);
        msg.copyPriorityAttributes(task);
        msg.setOrderId(lOrderId | (((long) service.getThisMember().getId()) << 32));

        PartitionSet partitions = new PartitionSet(service.getPartitionCount());
        context.setPartitionSet(partitions);

        Set setKeys = colBinKeys instanceof Set ? (Set) colBinKeys : new HashSet(colBinKeys);

        if (coordinator.submitKeySetRequest(msg, setKeys, partitions, /*fRepeat*/false))
            {
            if (!NonBlocking.isNonBlockingCaller())
                {
                service.flush();

                try
                    {
                    coordinator.drainBacklog(partitions, msg.checkTimeoutRemaining());
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // submit failed
            context.processCompletion();
            }
        }

    /**
     * Check whether or not the response value for this request is valid.
     */
    protected boolean checkResponse(com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest msgRequest, Object oResponse)
        {
        // import com.tangosol.net.RequestPolicyException;
        // import com.tangosol.util.Base;

        try
            {
            return msgRequest.checkResponse(oResponse);
            }
        catch (RuntimeException e)
            {
            // RequestPolicyException could be thrown from the server; unwrap it before
            // rethrowing to clients which could be relying on a type-specific catch
            Throwable eOrig = Base.getOriginalException(e);
            if (eOrig instanceof RequestPolicyException)
                {
                e = (RuntimeException) eOrig;
                }

            throw logRemoteException(e);
            }
        }

    /**
     * Check whether or not the response value for this request is valid.
     */
    protected boolean checkResponse(com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.StorageRequest msgRequest, Object oResponse)
        {
        // import com.tangosol.net.RequestPolicyException;
        // import com.tangosol.util.Base;

        try
            {
            return msgRequest.checkResponse(oResponse);
            }
        catch (RuntimeException e)
            {
            // RequestPolicyException could be thrown from the server; unwrap it before
            // rethrowing to clients which could be relying on a type-specific catch
            Throwable eOrig = Base.getOriginalException(e);
            if (eOrig instanceof RequestPolicyException)
                {
                e = (RuntimeException) eOrig;
                }

            throw logRemoteException(e);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void clear()
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.ClearRequest msg =
                    (PartitionedCache.ClearRequest) service.instantiateMessage("ClearRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());

            mergePartialResponse(
                    sendPartitionedRequest(msg, makePartitionSet(), false));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e, RESPONSE_VOID);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    /**
     * Clear any state associated with the specified request status.
     */
    protected void clearStatus(com.tangosol.coherence.component.net.RequestStatus status)
        {
        if (status != null)
            {
            status.reset();
            }
        }

    /**
     * Collect rejected partitions based on the Set of rejected keys and the
     * original map containing keys (Set) or entries (Map) keyed by the
     * assumed owner Member object.
     *
     * @return Map<Integer, Member> keyed by partition numbers with values
     * being Members that rejected a request related to that partition (or
     * null for currently orphaned keys)
     */
    protected java.util.Map collectRejectedPartitions(java.util.Set setRejectedKeys, java.util.Map mapByOwner)
        {
        // import Component.Net.Member;
        // import com.tangosol.util.Binary;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        PartitionedCache service     = getService();
        Map     mapRejected = new HashMap(); // <Integer, Member>

        setRejectedKeys = new HashSet(setRejectedKeys); // clone

        for (Iterator iterMember = mapByOwner.entrySet().iterator(); iterMember.hasNext();)
            {
            java.util.Map.Entry  entry  = (java.util.Map.Entry) iterMember.next();
            Member member = (Member) entry.getKey();
            Object oKeys  = entry.getValue();

            Set setKeys = oKeys instanceof Set ?
                          (Set) oKeys : ((Map) oKeys).keySet();

            for (Iterator iter = setRejectedKeys.iterator(); iter.hasNext();)
                {
                Binary binRejectedKey = (Binary) iter.next();
                if (setKeys.contains(binRejectedKey))
                    {
                    mapRejected.put(Integer.valueOf(
                            service.getKeyPartition(binRejectedKey)), member);
                    iter.remove();
                    }
                }
            }

        if (!setRejectedKeys.isEmpty())
            {
            for (Iterator iter = setRejectedKeys.iterator(); iter.hasNext();)
                {
                Binary binRejectedKey = (Binary) iter.next();

                mapRejected.put(Integer.valueOf(
                        service.getKeyPartition(binRejectedKey)), null);
                }
            }

        return mapRejected;
        }

    /**
     * Confirm that the cache associated with this BinaryMap is known to all
     * partition owners in the service, returning true if the cache
     * existence could be confirmed, or false if the cache is no longer
     * "Active".
     *
     * Called on client threads only.
     */
    public boolean confirmCache()
        {
        // import com.tangosol.net.RequestPolicyException;

        if (!isConfirmed())
            {
            // a dedicated PartialRequest is used here instead of some other existing
            // "client" request (e.g. "size"), because as of 12.1.3, the client API
            // does not automatically retry due to the missing storage on the server-side
            // (it relies on tightened semantics of COH-4544).  Additionally, because
            // the BinaryMap must be confirmed by each client, a "size" operation
            // would be run on existing caches (with data) which could be both expensive
            // and visible to custom BM implementations

            PartitionedCache.StorageConfirmRequest msg =
                    (PartitionedCache.StorageConfirmRequest) getService().instantiateMessage("StorageConfirmRequest");
            msg.setCacheId(getCacheId());

            try
                {
                // we don't care about the responses, just that the request completes
                mergePartialResponse(
                        sendPartitionedRequest(msg, makePartitionSet(), true));
                }
            catch (RequestPolicyException e)
                {
                if (isActive())
                    {
                    // this is possible if there are no ownership-enabled members
                    // remaining, or if the restore/recovery of orphaned partitions
                    // is prevented by the quorum policy; in either case, consider
                    // the BinaryMap to be "confirmed" as it is in a usable state
                    // to be handed back to the client
                    }
                else
                    {
                    // this is possible for two reasons:
                    // 1) if there was a failure of the senior member after cache
                    //    creation but before all member have heard the config update
                    //    (and specifically the "new senior"); when the new senior
                    //    takes over, it sends a config sync not reflecting the new
                    //    cache, resulting in a "remove"
                    // 2) a concurrent destroyCache()
                    //
                    // while the semantics of case #2 are undefined, in both cases
                    // we repeat the ensureCache operation entirely

                    return false;
                    }
                }

            setConfirmed(true);
            }

        return true;
        }

    /**
     * Optimized implementation.
     */
    public boolean containsAll(java.util.Collection colKeys)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.HashSet;
        // import java.util.List;
        // import java.util.Set;

        Set setKeys = colKeys instanceof Set ? (Set) colKeys : new HashSet(colKeys);
        int cKeys   = setKeys.size();
        if (cKeys == 0)
            {
            return true;
            }
        if (cKeys == 1)
            {
            Binary binKey = (Binary) setKeys.iterator().next();
            return containsKey(binKey);
            }

        PartitionedCache.ContainsAllRequest msg =
                (PartitionedCache.ContainsAllRequest) getService().instantiateMessage("ContainsAllRequest");
        msg.setCacheId(getCacheId());

        RequestTimeoutException eTimeout = null;
        List listResponse;
        try
            {
            listResponse = sendPartitionedRequest(msg, setKeys);
            }
        catch (RequestTimeoutException e)
            {
            eTimeout     = e;
            listResponse = (List) e.getPartialResult();
            if (listResponse == null)
                {
                throw e;
                }
            }

        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            PartitionedCache.PartialValueResponse response = (PartitionedCache.PartialValueResponse) iter.next();

            RuntimeException exception = response.getException();
            if (exception != null)
                {
                throw exception;
                }

            Object oResult = response.getResult();
            if (oResult instanceof Boolean && !((Boolean) oResult).booleanValue())
                {
                return false;
                }
            }

        if (eTimeout != null)
            {
            // we don't really know
            eTimeout.setPartialResult(null);
            throw eTimeout;
            }
        return true;
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean containsKey(Object oKey)
        {
        // import com.tangosol.util.Binary;

        BinaryMap.KeyRequestStatus status = null;
        try
            {
            PartitionedCache service = getService();
            Binary  binKey  = (Binary) oKey;

            PartitionedCache.ContainsKeyRequest msg =
                    (PartitionedCache.ContainsKeyRequest) service.instantiateMessage("ContainsKeyRequest");
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);

            while (true)
                {
                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return false;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse = service.poll(msg);

                if (checkResponse(msg, oResponse))
                    {
                    return ((Boolean) oResponse).booleanValue();
                    }

                msg = (PartitionedCache.ContainsKeyRequest) msg.cloneMessage();
                }
            }
        finally
            {
            clearStatus(status);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean containsValue(Object oValue)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.List;

        PartitionedCache.ContainsValueRequest msg =
                (PartitionedCache.ContainsValueRequest) getService().instantiateMessage("ContainsValueRequest");
        msg.setCacheId(getCacheId());
        msg.setValue((Binary) oValue);

        RequestTimeoutException eTimeout = null;
        List listResponse;
        try
            {
            listResponse = sendPartitionedRequest(msg, makePartitionSet(), false);
            }
        catch (RequestTimeoutException e)
            {
            eTimeout     = e;
            listResponse = (List) e.getPartialResult();
            if (listResponse == null)
                {
                throw e;
                }
            }

        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            PartitionedCache.PartialValueResponse msgResponse = (PartitionedCache.PartialValueResponse) iter.next();

            RuntimeException exception = msgResponse.getException();
            if (exception != null)
                {
                throw exception;
                }

            Object oResult = msgResponse.getResult();
            if (oResult instanceof Boolean && ((Boolean) oResult).booleanValue())
                {
                return true;
                }
            }

        if (eTimeout != null)
            {
            // we don't really know
            eTimeout.setPartialResult(null);
            throw eTimeout;
            }
        return false;
        }

    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;

        ensureWriteAllowed();

        // almost all cleanup happens at $ConfigListener#entryDeleted

        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        synchronized (support)
            {
            support.clear();
            getFilterArray().clear();
            }
        }

    /**
     * Called on the service thread only.
     */
    public void dispatch(PartitionedCache.MapEvent msgEvent)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Util.CacheEvent as com.tangosol.coherence.component.util.CacheEvent;
        // import com.tangosol.internal.tracing.Scope;
        // import com.tangosol.internal.tracing.SpanContext;
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.net.cache.CacheEvent;
        // import com.tangosol.net.cache.CacheEvent$TransformationState as com.tangosol.net.cache.CacheEvent.TransformationState;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.LiteSet;
        // import com.tangosol.util.LongArray;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$FilterEvent as com.tangosol.util.MapListenerSupport.FilterEvent;
        // import java.util.Iterator;

        int         nEventType  = msgEvent.getEventType() & PartitionedCache.MapEvent.EVT_TYPE_MASK;
        Binary      binKey      = msgEvent.getKey();
        boolean     fSynthetic  = msgEvent.isSynthetic();
        boolean     fExpired    = msgEvent.isExpired();
        boolean     fPriming    = msgEvent.isPriming();
        long[]      alFilterId  = msgEvent.getFilterId();
        int         cFilters    = alFilterId == null ? 0 : alFilterId.length;
        int         iPartition  = msgEvent.getPartition();
        com.tangosol.util.MapListenerSupport     support     = getListenerSupport();
        SpanContext spanContext = msgEvent.getTracingSpanContext();
        PartitionedCache     service     = getService();
        CacheEvent event;

        // collect key based listeners; note: key is null for versioned events
        Listeners listeners = msgEvent.isTransformed() || binKey == null
                              ? null : support.getListeners(binKey);
        if (cFilters == 0)
            {
            // CacheEvents emitted by PartitionedCache are never transformable on the client
            event = new CacheEvent(this, nEventType, binKey,
                                   msgEvent.getOldValue(), msgEvent.getNewValue(), fSynthetic,
                                   com.tangosol.net.cache.CacheEvent.TransformationState.NON_TRANSFORMABLE, fPriming, fExpired);

            event = event.with(iPartition, msgEvent.getVersion());

            // synthetic version update / propagation events requires the client
            // to find all listeners registered with associated keys; events are only
            // dispatched to relevant listeners (@see CacheEvent.shouldDispatch)
            if (event.isVersionUpdate())
                {
                synchronized (support)
                    {
                    for (Iterator iter = support.getKeySet().iterator(); iter.hasNext(); )
                        {
                        Binary binKeyCheck = (Binary) iter.next();

                        if (service.getKeyPartition(binKeyCheck) == iPartition)
                            {
                            if (listeners == null)
                                {
                                listeners = new Listeners();
                                }
                            listeners.addAll(support.getListeners(binKeyCheck));
                            }
                        }
                    }
                }
            }
        else
            {
            // collect filter based listeners
            LongArray laFilters  = getFilterArray();
            LiteSet setFilters = new LiteSet();
            for (int i = 0; i < cFilters; i++)
                {
                long lFilterId = alFilterId[i];
                if (laFilters.exists(lFilterId))
                    {
                    Filter filter = (Filter) laFilters.get(lFilterId);

                    // clone the key listeners before merging filter listeners
                    Listeners listenersTemp = new Listeners();
                    if (listeners != null)
                        {
                        listenersTemp.addAll(listeners);
                        }

                    listeners = listenersTemp;
                    listeners.addAll(support.getListeners(filter));
                    setFilters.add(filter);
                    }
                }

            Filter[] aFilter = (Filter[]) setFilters.toArray(new Filter[setFilters.size()]);
            event = new com.tangosol.util.MapListenerSupport.FilterEvent(this, nEventType, binKey,
                                                                         msgEvent.getOldValue(), msgEvent.getNewValue(), fSynthetic,
                                                                         msgEvent.isTransformed() ? com.tangosol.net.cache.CacheEvent.TransformationState.TRANSFORMED : com.tangosol.net.cache.CacheEvent.TransformationState.NON_TRANSFORMABLE,
                                                                         fPriming, fExpired, aFilter);

            event = event.with(iPartition, msgEvent.getVersion());
            }

        if (listeners == null || listeners.isEmpty())
            {
            // this is an orphaned event that must've been leaked by the
            // removeMapListener(listener, oKey, ...) method; repeat the removal
            // while holding a synchronization monitor for the support object
            // to make sure no one adds a listener for this key concurrently
            // (we are still on the service thread here)
            synchronized (support)
                {
                if (cFilters == 0 && binKey != null && support.getListeners(binKey) == null)
                    {
                    com.tangosol.coherence.component.net.Member memberOwner = service.getPrimaryOwner(binKey);
                    if (memberOwner != null)
                        {
                        PartitionedCache.KeyListenerRequest msg =
                                (PartitionedCache.KeyListenerRequest) service.instantiateMessage("KeyListenerRequest");
                        msg.setCacheId(getCacheId());
                        msg.setKey(binKey);
                        msg.setAdd(false);

                        msg.addToMember(memberOwner);

                        service.post(msg);
                        }
                    }
                }
            }
        else if (spanContext == null)
            {
            com.tangosol.coherence.component.util.CacheEvent.dispatchSafe(event, listeners,
                                                                          getService().ensureEventDispatcher().getQueue());
            }
        else
            {
            PartitionedCache svc        = getService();
            com.tangosol.coherence.component.net.Member  memberFrom = msgEvent.getFromMember();
            Scope scope      = TracingHelper.isEnabled()
                               ? TracingHelper.getTracer().withSpan(svc.newTracingSpan("dispatch", msgEvent)
                                                                            .withAssociation("follows_from", spanContext)
                                                                            .withMetadata("member.source",
                                                                                          Long.valueOf(memberFrom == null ? -1 : memberFrom.getId()).longValue())
                                                                            .startSpan())
                               : null;
            try
                {
                com.tangosol.coherence.component.util.CacheEvent.dispatchSafe(event, listeners, svc.ensureEventDispatcher().getQueue());
                }
            finally
                {
                if (scope != null)
                    {
                    scope.close();
                    }
                }
            }
        }

    /**
     * Split the specified partitions into subsets (by owner) and create the
     * corresponding request status. If all partitions are currently unknown
     * (due to a redistribution), wait until all partitions are assigned (no
     * orphans allowed).
     *
     * @param partitions    the partition set to split
     *
     * @return the corresponding request status object
     */
    protected BinaryMap.PartialRequestStatus ensureRequestTarget(com.tangosol.net.partition.PartitionSet partitions, com.tangosol.coherence.component.net.message.RequestMessage msg)
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import java.util.Map;

        PartitionedCache               service = getService();
        BinaryMap.PartialRequestStatus status  = new BinaryMap.PartialRequestStatus();

        _linkChild(status);

        while (true)
            {
            // COH-3974 - check that the cache hasn't been concurrently destroyed
            if (!isActive())
                {
                clearStatus(status);
                throw onMissingStorage(partitions);
                }

            Map mapByOwner = service.splitByOwner(partitions, 0, service.getPartitionAssignments());
            status.setOrphanedPartitions((PartitionSet) mapByOwner.remove(null));
            status.setPartitionsByOwner(mapByOwner);

            if (status.getOrphanedPartitions() == null)
                {
                return status;
                }

            if (service.getOwnershipMemberSet().isEmpty())
                {
                PartitionSet setOrphans = status.getOrphanedPartitions();
                clearStatus(status);
                throw onMissingStorage(setOrphans);
                }

            _assert(!status.getOrphanedPartitions().isEmpty());

            waitForRedistribution(partitions, status, msg.getRequestTimeout());
            }
        }

    /**
     * Split the specified partitions into subsets (by owner) and create the
     * corresponding request status. If all partitions are currently unknown
     * (due to a redistribution), wait until at least some partitions are
     * assigned.
     *
     * @param partitions   the partition set to split
     * @param status         non-null value indicates that this call has
     * already been attempted once and a server rejected it due to a
     * re-distribution
     * @param msg            the RequestMessage to calculate the target for
     *
     *
     * @return the corresponding request status object
     */
    protected BinaryMap.PartialRequestStatus ensureRequestTarget(com.tangosol.net.partition.PartitionSet partitions, BinaryMap.PartialRequestStatus status, com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest msg)
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import java.util.Map;

        PartitionedCache service = getService();

        while (true)
            {
            if (status == null)
                {
                _linkChild(status = new BinaryMap.PartialRequestStatus());
                }
            else
                {
                waitForRedistribution(partitions, status, msg.getRequestTimeout());

                // COH-3974 - check that the cache hasn't been concurrently destroyed
                if (!isActive())
                    {
                    clearStatus(status);
                    throw onMissingStorage(partitions);
                    }
                }

            service.checkQuorum(msg, msg.isReadOnly());

            int[][] aaiOwners = service.getPartitionAssignments();

            Map          mapByOwner  = service.splitByOwner(partitions, 0, aaiOwners);
            PartitionSet partsOrphan = (PartitionSet) mapByOwner.remove(null);

            status.setPartitionsByOwner(mapByOwner);

            if (partsOrphan == null)
                {
                // common case
                status.setOrphanedPartitions(null);
                return status;
                }
            else
                {
                status.setOrphanedPartitions(partsOrphan);

                if (service.getPartitionConfigMap().isAssignmentCompleted())
                    {
                    if (aaiOwners == service.getPartitionAssignments())
                        {
                        // there were orphaned partitions, but the assignment has completed;
                        // proceed with sending the (partial) request
                        if (!mapByOwner.isEmpty())
                            {
                            return status;
                            }
                        }
                    else
                        {
                        // things moved around while splitting so our assignment snapshot is
                        // out-of-date; force an immediate re-check (skip the waitForRedistribution)
                        status = null;
                        continue;
                        }
                    }

                if (service.getOwnershipMemberSet().isEmpty())
                    {
                    PartitionSet setOrphans = status.getOrphanedPartitions();
                    clearStatus(status);
                    throw onMissingStorage(setOrphans);
                    }
                }
            }
        }

    /**
     * Calculate a primary owner for the specified Binary key and create the
     * corresponding request status. If an owner is currently unknown (due
     * to a redistribution), wait until it gets assigned.
     *
     * @param binKey    the key to calculate the primary owner for
     * @param status     non-null value indicates that this call has already
     * been attempted once and a server rejected it due to a
     * re-distribution; must be null first time around (when we assume the
     * ownership information is correct).
     * @param msg         the RequestMessage to calculate the target for
     *
     * @return the corresponding request status object
     */
    protected BinaryMap.KeyRequestStatus ensureRequestTarget(com.tangosol.util.Binary binKey, BinaryMap.KeyRequestStatus status, com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest msg)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;

        PartitionedCache service = getService();

        while (true)
            {
            int iPartition;

            if (status == null)
                {
                iPartition = service.getKeyPartition(binKey);
                status     = new BinaryMap.KeyRequestStatus();
                status.setPartition(iPartition);
                _linkChild(status);
                }
            else
                {
                iPartition = status.getPartition();

                waitForRedistribution(status, msg.getRequestTimeout());

                // COH-3974 - check that the cache hasn't been concurrently destroyed
                if (!isActive())
                    {
                    clearStatus(status);
                    throw onMissingStorage(service.instantiatePartitionSet(iPartition));
                    }
                }

            service.checkQuorum(msg, msg.isReadOnly());

            com.tangosol.coherence.component.net.Member memberTarget = getTarget(iPartition, msg);
            if (memberTarget != null)
                {
                status.setOwner(memberTarget);
                return status;
                }

            if (service.getOwnershipMemberSet().isEmpty())
                {
                clearStatus(status);
                throw onMissingStorage(service.instantiatePartitionSet(iPartition));
                }
            }
        }

    /**
     * Split the specified entries into subsets (by owner) and create the
     * corresponding request status. If all keys are currently unknown (due
     * to a redistribution), wait until they gets assigned.
     *
     * @param map       the entry map to split by owner
     * @param status    non-null value indicates that this call has already
     * been attempted once and a server rejected it due to a re-distribution
     * @param msg       the RequestMessage to calculate the target for
     *
     * @return the corresponding request status object
     */
    protected BinaryMap.MapRequestStatus ensureRequestTarget(java.util.Map map, BinaryMap.MapRequestStatus status, com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.MapRequest msg)
        {
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.ConverterCollections$ConverterMap as com.tangosol.util.ConverterCollections.ConverterMap;
        // import java.util.HashMap;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache service = getService();
        boolean fRetry  = false;

        if (status == null)
            {
            _linkChild(status = new BinaryMap.MapRequestStatus());
            }
        else
            {
            fRetry = true;
            waitForRedistribution(map, status, msg.getRequestTimeout());

            // COH-3974 - check that the cache hasn't been concurrently destroyed
            if (!isActive())
                {
                clearStatus(status);
                throw onMissingStorage(service.getKeyPartitions(map.keySet()));
                }
            }

        service.checkQuorum(msg, msg.isReadOnly());

        if (fRetry)
            {
            // the map passed as an argument already has all the remaining entries
            // split by partition, so there is no need to split them again
            status.setEntriesByPartition(map);
            }
        else
            {
            com.tangosol.util.ConverterCollections.ConverterMap mapConv = ((com.tangosol.util.ConverterCollections.ConverterMap) map);
            if (mapConv.getMap() instanceof com.tangosol.util.ConverterCollections.ConverterMap)
                {
                // we are processing a request from an Extend proxy that is configured with
                // a mismatched serializer, and need to deserialize all entries as direct
                // lookup against com.tangosol.util.ConverterCollections.ConverterMap doesn't work because of key decorations

                mapConv = ConverterCollections.getMap(new HashMap(mapConv.getMap()),
                                                      mapConv.getConverterKeyUp(),
                                                      mapConv.getConverterKeyDown(),
                                                      mapConv.getConverterValueUp(),
                                                      mapConv.getConverterValueDown());
                }

            Map mapByPartition = service.splitKeysByPartition(mapConv.keySet().iterator());
            if (mapByPartition.isEmpty() && map.isEmpty())
                {
                // COH-3505: map must have been concurrently emptied
                throw new ConcurrentModificationException();
                }

            for (Iterator it = mapByPartition.entrySet().iterator(); it.hasNext(); )
                {
                // replace partition key set with the subset of the original map containing those keys
                java.util.Map.Entry entry = (java.util.Map.Entry) it.next();
                entry.setValue(mapConv.subMap((Set) entry.getValue()));
                }


            status.setEntriesByPartition(mapByPartition);
            }

        status.setOrphanedEntries(new HashMap());
        status.setOrphanedPartitions(null);

        return status;
        }

    /**
     * Split the specified keys into subsets (by owner) and create the
     * corresponding request status. If all keys are currently unknown (due
     * to a redistribution), wait until they gets assigned.
     *
     * @param setKeys     the set of keys to split by owner
     * @param status       non-null value indicates that this call has
     * already been attempted once and a server rejected it due to a
     * re-distribution
     * @param msg           the RequestMessage to calculate the target for
     *
     * @return the corresponding request status object
     */
    protected BinaryMap.KeySetRequestStatus ensureRequestTarget(java.util.Set setKeys, BinaryMap.KeySetRequestStatus status, com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest msg)
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import java.util.ConcurrentModificationException;
        // import java.util.Map;
        // import java.util.Set;

        PartitionedCache service = getService();

        while (true)
            {
            if (status == null)
                {
                _linkChild(status = new BinaryMap.KeySetRequestStatus());
                }
            else
                {
                waitForRedistribution(setKeys, status, msg.getRequestTimeout());

                // COH-3974 - check that the cache hasn't been concurrently destroyed
                if (!isActive())
                    {
                    clearStatus(status);
                    throw onMissingStorage(service.getKeyPartitions(setKeys));
                    }
                }

            service.checkQuorum(msg, msg.isReadOnly());

            int[][] aaiOwners = service.getPartitionAssignments();

            int iStore = msg.isCoherentResult() ? 0 : -1;

            Map mapByOwner = service.splitKeysByOwner(setKeys.iterator(), iStore, aaiOwners);
            Set setOrphan  = (Set) mapByOwner.remove(null);

            status.setOrphanedKeys(setOrphan);
            status.setKeysByOwner(mapByOwner);

            if (setOrphan == null && !mapByOwner.isEmpty())
                {
                // common case
                status.setOrphanedPartitions(null);
                return status;
                }
            else
                {
                status.setOrphanedPartitions(service.getKeyPartitions(setOrphan));

                if (service.getPartitionConfigMap().isAssignmentCompleted())
                    {
                    if (aaiOwners == service.getPartitionAssignments())
                        {
                        // there were orphaned partitions, but the assignment has completed;
                        // proceed with sending the (partial) request
                        if (!mapByOwner.isEmpty())
                            {
                            return status;
                            }
                        }
                    else
                        {
                        // things moved around while splitting so our assignment snapshot is
                        // out-of-date; force an immediate re-check (skip the waitForRedistribution)
                        status = null;
                        continue;
                        }
                    }

                if (setKeys.isEmpty())
                    {
                    // COH-3505: setKeys must have been concurrently emptied
                    throw new ConcurrentModificationException();
                    }

                if (service.getOwnershipMemberSet().isEmpty())
                    {
                    PartitionSet setOrphans = status.getOrphanedPartitions();
                    clearStatus(status);
                    throw onMissingStorage(setOrphans);
                    }
                }
            }
        }

    /**
     * Ensure that the write action is currently allowed by the service
     * action policy.
     */
    protected void ensureWriteAllowed()
        {
        // import com.tangosol.net.ActionPolicy;
        // import com.tangosol.net.CacheService$CacheAction as com.tangosol.net.CacheService.CacheAction;
        // import com.tangosol.net.RequestPolicyException;

        PartitionedCache      service = getService();
        ActionPolicy policy  = service.getActionPolicy();
        if (!policy.isAllowed(service, com.tangosol.net.CacheService.CacheAction.WRITE))
            {
            throw new RequestPolicyException("Cache writes are disallowed by " + policy);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet()
        {
        return (BinaryMap.EntrySet) _findChild("EntrySet");
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        return entrySet(filter, makePartitionSet());
        }

    /**
     * Note: the passed-in PartitionSet will be changed.
     */
    public java.util.Set entrySet(com.tangosol.util.Filter filter, com.tangosol.net.partition.PartitionSet partitions)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.filter.LimitFilter;

        try
            {
            return filter instanceof LimitFilter
                   ?
                   limitQuerySequential(partitions, (LimitFilter) filter, false) :
                   mergeQueryResponse(query(partitions, filter, false));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e,
                                                   filter instanceof LimitFilter ? RESPONSE_VOID : RESPONSE_SET);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        return entrySet(filter, comparator, makePartitionSet());
        }

    /**
     * Note: the passed-in PartitionSet will be changed.
     */
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator, com.tangosol.net.partition.PartitionSet partitions)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.filter.LimitFilter;

        try
            {
            return filter instanceof LimitFilter ?
                   limitQueryDistributed(partitions, (LimitFilter) filter) :
                   mergeQueryResponse(query(partitions, filter, false));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e,
                                                   filter instanceof LimitFilter ? RESPONSE_VOID : RESPONSE_SET);
            }
        }

    /**
     * Estimate the number of members query concurrently.
     */
    protected void estimateBatchSize(PartitionedCache.QueryResponse msgResponse, com.tangosol.util.filter.LimitFilter filter, long cbScratch)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.SimpleMapEntry;

        if (msgResponse.getException() != null)
            {
            throw logRemoteException(msgResponse.getException());
            }

        long cbPage   = msgResponse.getBinarySize();
        int  cMembers = getService().getOwnershipMemberSet().size();

        int cBatch = cbPage == 0 ? 1 : (int) Math.min(cMembers, Math.max(1, (cbScratch + cbPage) / cbPage));
        filter.setBatchSize(cBatch);
        }

    // From interface: com.tangosol.net.NamedCache
    public Object get(Object oKey)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.util.Binary;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        BinaryMap.KeyRequestStatus status = null;
        try
            {
            Binary  binKey  = (Binary) oKey;

            PartitionedCache.GetRequest msg =
                    (PartitionedCache.GetRequest) service.instantiateMessage("GetRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);
            msg.setAllowBackupRead(getReadLocator() != null);

            while (true)
                {
                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return null;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse = service.poll(msg);

                if (checkResponse(msg, oResponse))
                    {
                    return (Binary) oResponse;
                    }

                msg = (PartitionedCache.GetRequest) msg.cloneMessage();
                }
            }
        finally
            {
            clearStatus(status);

            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        // import com.tangosol.util.LiteMap;
        // import java.util.HashSet;
        // import java.util.Set;

        Set setKeys = colKeys instanceof Set ? (Set) colKeys : new HashSet(colKeys);
        int cKeys   = setKeys.size();
        if (cKeys == 0)
            {
            return new LiteMap();
            }

        // even if the colKeys contains a single item we cannot delegate to "get()"
        // since "getAll()" never returns null values
        return getAll(setKeys, 0);
        }

    /**
     * Flavor of getAll() that asks to return a limited subset of the values
     * based on the total result set size.
     */
    public java.util.Map getAll(java.util.Set setKeys, int cSizeThreshold)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.GetAllRequest msg =
                    (PartitionedCache.GetAllRequest) service.instantiateMessage("GetAllRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setSizeThreshold(cSizeThreshold);
            msg.setAllowBackupRead(getReadLocator() != null);

            return mergePartialMapResponse(
                    sendPartitionedRequest(msg, setKeys));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e, RESPONSE_MAP);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // Accessor for the property "CacheId"
    /**
     * Getter for property CacheId.<p>
     * The cache id this BinaryMap represents.
     *
     * @see PartitionedCache#StorageArray
     */
    public long getCacheId()
        {
        return __m_CacheId;
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
     * The cache name this BinaryMap represents.
     */
    public String getCacheName()
        {
        return __m_CacheName;
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "CacheService"
    /**
     * Getter for property CacheService.<p>
     */
    public com.tangosol.net.CacheService getCacheService()
        {
        return (PartitionedCache) get_Module();
        }

    // Accessor for the property "Dispatcher"
    /**
     * Getter for property Dispatcher.<p>
     * An EventDispatcher that can dispatch cache lifecycle events.
     *
     * This is null for storage-enabled members and non-null otherwise.
     */
    public com.tangosol.net.events.internal.StorageDispatcher getDispatcher()
        {
        return __m_Dispatcher;
        }

    // Accessor for the property "FilterArray"
    /**
     * Getter for property FilterArray.<p>
     * A LongArray of Filter objects indexed by the unique filter id. These
     * filter id values are used by the MapEvent message to specify what
     * filters caused a cache event.
     *
     * Note: all access (for update) to this array should be synchronized on
     * the ListenerSupport object.
     */
    public com.tangosol.util.LongArray getFilterArray()
        {
        return __m_FilterArray;
        }

    /**
     * Return a filter id for the speicfied filter or zero is none could be
     * found. Note that since the filter ids are created by using
     * Service.getSUID() method, zero is guaranteed to be an invalid id.
     *
     * Note: all calls to this method should be synchronized using the
     * ListenerSupport object.
     */
    public long getFilterId(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.LongArray$Iterator as com.tangosol.util.LongArray.Iterator;

        for (com.tangosol.util.LongArray.Iterator iter = getFilterArray().iterator(); iter.hasNext();)
            {
            Filter filterThat = (Filter) iter.next();
            if (Base.equals(filter, filterThat))
                {
                return iter.getIndex();
                }
            }

        return 0L;
        }

    // Accessor for the property "ListenerSupport"
    /**
     * Getter for property ListenerSupport.<p>
     */
    public com.tangosol.util.MapListenerSupport getListenerSupport()
        {
        return __m_ListenerSupport;
        }

    /**
     * Return PartitionSet owned by the specified number of members.
     */
    public com.tangosol.net.partition.PartitionSet getPartitions(java.util.Map map, int cMembers)
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import java.util.Iterator;

        PartitionSet parts = new PartitionSet(getService().getPartitionCount());

        cMembers = Math.max(1, cMembers);

        for (Iterator iter = map.values().iterator(); iter.hasNext();)
            {
            parts.add((PartitionSet) iter.next());
            if (--cMembers == 0)
                {
                break;
                }
            }

        return parts;
        }

    // Accessor for the property "ReadLocator"
    /**
     * Getter for property ReadLocator.<p>
     * A BiFunction<Ownership,PartitionedService,Member> that returns the
     * Member to target read requests against.
     */
    public java.util.function.BiFunction getReadLocator()
        {
        return __m_ReadLocator;
        }

    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
     */
    public PartitionedCache getService()
        {
        return (PartitionedCache) get_Module();
        }

    public com.tangosol.coherence.component.net.Member getTarget(int iPartition, com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest msg)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import java.util.function.BiFunction;

        BiFunction functionTarget = getReadLocator();

        if (msg.isCoherentResult() || functionTarget == null)
            {
            return getService().getPrimaryOwner(iPartition);
            }

        PartitionedCache service = getService();

        return (com.tangosol.coherence.component.net.Member) functionTarget.apply(service.getPartitionOwnership(iPartition), service);
        }

    public Object invoke(com.tangosol.util.Binary binKey, com.tangosol.util.Binary binAgent, com.tangosol.net.PriorityTask task)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.util.Binary;

        PartitionedCache           service = getService();
        BinaryMap.KeyRequestStatus status  = null;
        com.tangosol.coherence.component.net.RequestContext           context = registerRequestContext(true);

        try
            {
            PartitionedCache.InvokeRequest msg =
                    (PartitionedCache.InvokeRequest) service.instantiateMessage("InvokeRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);
            msg.setProcessorBinary(binAgent);
            msg.copyPriorityAttributes(task);

            while (true)
                {
                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return null;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse = service.poll(msg);

                if (checkResponse(msg, oResponse))
                    {
                    return (Binary) oResponse;
                    }

                context.setOldestPendingSUID(service.getOldestPendingRequestSUID());

                msg = (PartitionedCache.InvokeRequest) msg.cloneMessage();
                }
            }
        finally
            {
            service.unregisterRequestContext(context);
            clearStatus(status);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Note: the passed-in PartitionSet will be changed.
     */
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.Binary binAgent, com.tangosol.net.partition.PartitionSet partitions, com.tangosol.net.PriorityTask task)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(true);
        try
            {
            PartitionedCache.InvokeFilterRequest msg =
                    (PartitionedCache.InvokeFilterRequest) service.instantiateMessage("InvokeFilterRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setFilter(filter);
            msg.setProcessorBinary(binAgent);
            msg.copyPriorityAttributes(task);

            try
                {
                return mergePartialMapResponse(sendPartitionedRequest(msg, partitions, false));
                }
            catch (RequestTimeoutException e)
                {
                throw processPartitionedRequestTimeout(e, RESPONSE_MAP);
                }
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        throw new UnsupportedOperationException();
        }

    public java.util.Map invokeAll(java.util.Collection colBinKeys, com.tangosol.util.Binary binAgent, com.tangosol.net.PriorityTask task)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.LiteMap;
        // import java.util.HashSet;
        // import java.util.Map;
        // import java.util.Set;

        if (colBinKeys.size() == 0)
            {
            return new LiteMap();
            }

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(true);
        try
            {
            PartitionedCache.InvokeAllRequest msg =
                    (PartitionedCache.InvokeAllRequest) service.instantiateMessage("InvokeAllRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setProcessorBinary(binAgent);
            msg.copyPriorityAttributes(task);

            try
                {
                Set setBinKeys = colBinKeys instanceof Set ? (Set) colBinKeys : new HashSet(colBinKeys);

                return mergePartialMapResponse(
                        sendPartitionedRequest(msg, setBinKeys));
                }
            catch (RequestTimeoutException e)
                {
                throw processPartitionedRequestTimeout(e, RESPONSE_MAP);
                }
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        throw new UnsupportedOperationException();
        }

    public void invokeAllAsync(com.tangosol.util.Filter filter, com.tangosol.util.Binary binAgent, com.tangosol.net.partition.PartitionSet partitions, com.tangosol.net.PriorityTask task, com.tangosol.util.processor.AbstractAsynchronousProcessor asyncProc, com.tangosol.util.Converter convUp)
        {
        // import Component.Net.RequestContext.AsyncContext;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.base.NonBlocking;

        // call "user" methods *before* creating the context
        long    lOrderId  = asyncProc.getUnitOfOrderId();

        PartitionedCache             service     = getService();
        PartitionedCache.RequestCoordinator coordinator = service.getRequestCoordinator();
        AsyncContext        context     = coordinator.createContext(this, asyncProc, convUp);

        if (partitions.isEmpty())
            {
            context.processCompletion();
            return;
            }

        PartitionedCache.InvokeFilterRequest msg =
                (PartitionedCache.InvokeFilterRequest) service.instantiateMessage("InvokeFilterRequest");
        msg.setRequestContext(context);
        msg.setCacheId(getCacheId());
        msg.setFilter(filter);
        msg.setProcessorBinary(binAgent);
        msg.copyPriorityAttributes(task);
        msg.setOrderId(lOrderId | (((long) service.getThisMember().getId()) << 32));

        context.setPartitionSet(partitions);

        if (coordinator.submitPartialRequest(msg, partitions, /*fRepeat*/false))
            {
            if (!NonBlocking.isNonBlockingCaller())
                {
                service.flush();

                try
                    {
                    coordinator.drainBacklog(partitions, msg.checkTimeoutRemaining());
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // submit failed
            context.processCompletion();
            }
        }

    public void invokeAllAsync(java.util.Collection colBinKeys, com.tangosol.util.Binary binAgent, com.tangosol.net.PriorityTask task, com.tangosol.util.processor.AbstractAsynchronousProcessor asyncProc, com.tangosol.util.Converter convUp)
        {
        // import Component.Net.RequestContext.AsyncContext;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.base.NonBlocking;
        // import java.util.HashSet;
        // import java.util.Set;

        // call "user" methods *before* creating the context
        long    lOrderId  = asyncProc.getUnitOfOrderId();

        PartitionedCache             service     = getService();
        PartitionedCache.RequestCoordinator coordinator = service.getRequestCoordinator();
        AsyncContext        context     = coordinator.createContext(this, asyncProc, convUp);

        if (colBinKeys.size() == 0)
            {
            context.processCompletion();
            return;
            }

        PartitionedCache.InvokeAllRequest msg =
                (PartitionedCache.InvokeAllRequest) service.instantiateMessage("InvokeAllRequest");
        msg.setRequestContext(context);
        msg.setCacheId(getCacheId());
        msg.setProcessorBinary(binAgent);
        msg.copyPriorityAttributes(task);
        msg.setOrderId(lOrderId | (((long) service.getThisMember().getId()) << 32));

        PartitionSet partitions = new PartitionSet(service.getPartitionCount());
        context.setPartitionSet(partitions);

        Set setKeys = colBinKeys instanceof Set ? (Set) colBinKeys : new HashSet(colBinKeys);

        if (coordinator.submitKeySetRequest(msg, setKeys, partitions, /*fRepeat*/false))
            {
            if (!NonBlocking.isNonBlockingCaller())
                {
                service.flush();

                try
                    {
                    coordinator.drainBacklog(partitions, msg.checkTimeoutRemaining());
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // submit failed
            context.processCompletion();
            }
        }

    public void invokeAsync(com.tangosol.util.Binary binKey, com.tangosol.util.Binary binAgent, com.tangosol.net.PriorityTask task, com.tangosol.util.processor.AbstractAsynchronousProcessor asyncProc, com.tangosol.util.Converter convUp)
        {
        // import Component.Net.RequestContext.AsyncContext;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.base.NonBlocking;

        // call "user" methods *before* creating the context
        long    lOrderId  = asyncProc.getUnitOfOrderId();

        PartitionedCache             service     = getService();
        PartitionedCache.RequestCoordinator coordinator = service.getRequestCoordinator();
        AsyncContext        context     = coordinator.createContext(this, asyncProc, convUp);

        PartitionedCache.InvokeRequest msg =
                (PartitionedCache.InvokeRequest) service.instantiateMessage("InvokeRequest");
        msg.setRequestContext(context);
        msg.setCacheId(getCacheId());
        msg.setKey(binKey);
        msg.setProcessorBinary(binAgent);
        msg.copyPriorityAttributes(task);
        msg.setOrderId(lOrderId | (((long) service.getThisMember().getId()) << 32));

        int nPartition = service.getKeyPartition(binKey);
        context.setPartition(nPartition);

        if (coordinator.submitKeyRequest(msg, nPartition))
            {
            if (!NonBlocking.isNonBlockingCaller())
                {
                service.flush();

                try
                    {
                    coordinator.drainBacklog(nPartition, msg.checkTimeoutRemaining());
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // submit failed
            context.processCompletion();
            }
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Active"
    /**
     * Getter for property Active.<p>
     * True iff the cache associated with this $BinaryMap is still active
     * (has not been destroyed).
     */
    public boolean isActive()
        {
        return getService().getBinaryMapArray().exists(getCacheId());
        }

    @Override
    public boolean isReady()
        {
        return isActive() &&
               getService().getPartitionOwner(0) != null &&
               getService().getOwnershipSenior() != null;
        }

    // Accessor for the property "AuthorizationEnabled"
    /**
     * Getter for property AuthorizationEnabled.<p>
     * Specifies whether or not the StorageAccessAuthorizer is configured
     * for this cache.
     */
    public boolean isAuthorizationEnabled()
        {
        return __m_AuthorizationEnabled;
        }

    // Accessor for the property "Confirmed"
    /**
     * Getter for property Confirmed.<p>
     * Flag indicating whether or not the existence of the cache associated
     * with this $BinaryMap has been confirmed with the partition owners.
     *
     * @volatile
     */
    public boolean isConfirmed()
        {
        return __m_Confirmed;
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Empty"
    /**
     * Getter for property Empty.<p>
     */
    public boolean isEmpty()
        {
        return size() == 0;
        }

    /**
     * Determine whether the specified version is compatible with partitioned
     * query changes.
     *
     * @return {@code true} if the specified version is compatible with partitioned
     *          query changes
     */
    protected static boolean isPartitionedQueryCompatible(int nVersion)
        {
        return (isVersionCompatible(VERSION_14_1_2_0, nVersion) && nVersion != VERSION_23_09)
               || isPatchCompatible(VERSION_23_09_1, nVersion)
               || isPatchCompatible(VERSION_14_1_1_2206_7, nVersion);
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet()
        {
        return (BinaryMap.KeySet) _findChild("KeySet");
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        return keySet(filter, makePartitionSet());
        }

    /**
     * Note: the passed-in PartitionSet will be changed.
     */
    public java.util.Set keySet(com.tangosol.util.Filter filter, com.tangosol.net.partition.PartitionSet partitions)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.filter.LimitFilter;

        try
            {
            return filter instanceof LimitFilter ?
                   limitQuerySequential(partitions, (LimitFilter) filter, true) :
                   mergeQueryResponse(query(partitions, filter, true));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e,
                                                   filter instanceof LimitFilter ? RESPONSE_VOID : RESPONSE_SET);
            }
        }

    /**
     * Obtain keys that belong to a subset of the specified partition set
     * using a paging algorithm. This method is expected to reduce the
     * passed in PartitionSet to reflect which parititons were fully
     * processed.
     */
    public java.util.Set keySetPage(com.tangosol.net.partition.PartitionSet partitions)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.NullImplementation;
        // import java.util.HashMap;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        BinaryMap.PartialRequestStatus status = null;
        try
            {
            PartitionedCache.KeyIteratorRequest msg =
                    (PartitionedCache.KeyIteratorRequest) service.instantiateMessage("KeyIteratorRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());

            status = ensureRequestTarget(partitions, status, msg);
            if (status.isTargetMissing())
                {
                partitions.clear();
                return NullImplementation.getSet();
                }

            Map mapByOwner = status.getPartitionsByOwner(); // Map<Member, PartitionSet>

            com.tangosol.coherence.component.net.Member       memberThis = service.getThisMember();
            PartitionSet partMember;
            com.tangosol.coherence.component.net.Member       member;
            if (mapByOwner.containsKey(memberThis))
                {
                // optimization - always start with the local node
                member     = memberThis;
                partMember = (PartitionSet) mapByOwner.get(member);
                }
            else
                {
                // randomize to prevent a resonance
                java.util.Map.Entry entry = (java.util.Map.Entry) Base.randomize(mapByOwner.entrySet()).get(0);

                member     = (com.tangosol.coherence.component.net.Member) entry.getKey();
                partMember = (PartitionSet) entry.getValue();
                }

            // since the poll will modify the passed-in partMask and partMember
            // we have to clone them to know what has been actually done
            PartitionSet partMask    = new PartitionSet(partMember);
            PartitionSet partRequest = new PartitionSet(partMember);

            msg.setToMemberSet(SingleMemberSet.instantiate(member));
            msg.setRequestMask(partMask);
            msg.setPartitions(partMember);

            PartitionedCache.QueryResponse msgResponse = (PartitionedCache.QueryResponse) service.poll(msg);

            if (msgResponse == null)
                {
                // the member is gone; let the caller ask again
                return NullImplementation.getSet();
                }

            if (msgResponse.getException() != null)
                {
                throw msgResponse.getException();
                }

            // partRequest contains all requested partitions;
            // at this time partMember contains only rejected paritions
            partRequest.remove(partMember);
            partitions.remove(partRequest);

            return new ImmutableArrayList(msgResponse.getResult(), 0, msgResponse.getSize());
            }
        finally
            {
            clearStatus(status);

            service.unregisterRequestContext(context);
            }
        }

    /**
     * Distributed limited query implementation. Note: we are allowed to
     * return more then asked; the client will do the final sort/extract.
     * Moreover, the filter's page size reflects bothe the page size and the
     * page number.
     */
    protected java.util.Set limitQueryDistributed(com.tangosol.net.partition.PartitionSet partitions, com.tangosol.util.filter.LimitFilter filterLimit)
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.filter.LimitFilter;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.List;

        _assert(filterLimit.getComparator() != null);

        PartitionedCache service = getService();

        // Assume close to even distribution and ask each member for 25% more than
        // we would need with an ideal distribution
        // TODO: this could be completely wrong for an unbalanced distribution
        int cPageSize   = filterLimit.getPageSize();
        Map mapByMember = service.splitByOwner(partitions, 0, service.getPartitionAssignments());

        if (mapByMember.size() == 0)
            {
            return NullImplementation.getSet();
            }

        int          cBatch     = filterLimit.getBatchSize();
        PartitionSet parts      = getPartitions(mapByMember, Math.max(1, cBatch));
        LimitFilter  filterPart = (LimitFilter) filterLimit.clone();
        PartitionSet partsQuery = new PartitionSet(parts);

        List listResponse = query(new PartitionSet(parts), filterPart, false);

        int cActual = 0;
        int cAvail  = 0;
        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            PartitionedCache.QueryResponse response = (PartitionedCache.QueryResponse) iter.next();
            if (response.getException() != null)
                {
                throw response.getException();
                }
            cActual += response.getSize();
            cAvail  += response.getAvailable();
            }

        if (cActual < cPageSize && cAvail > cActual)
            {
            // re-issue request for the members that have more
            // this would rarely happen: play it safe and ask for the maximum

            filterPart.setPageSize(cPageSize);

            for (Iterator iter = listResponse.iterator(); iter.hasNext();)
                {
                PartitionedCache.QueryResponse response = (PartitionedCache.QueryResponse) iter.next();

                int cPartActual = response.getSize();
                int cPartAvail  = response.getAvailable();
                if (cPartActual == cPartAvail)
                    {
                    // we received all we asked for; and there are no more
                    parts.remove(service.calculatePartitionSet(response.getFromMember(), 0));
                    }
                }

            if (!parts.isEmpty())
                {
                listResponse.addAll(query(parts, filterPart, false));
                }
            }

        partitions.remove(partsQuery);

        long cbScratch = 0;
        if (!partitions.isEmpty() && cBatch == 0)
            {
            // estimate batch size to query concurrently
            cbScratch = service.reserveScratchSpace();
            estimateBatchSize((PartitionedCache.QueryResponse) listResponse.iterator().next(), filterLimit, cbScratch);
            }

        if (partitions.isEmpty() && cbScratch > 0)
            {
            service.releaseScratchSpace(cbScratch);
            }

        return mergeQueryResponse(listResponse);
        }

    /**
     * Distributed limited query implementation. Note: we should return
     * exactly what asked; no further actions by a client are expected.
     */
    protected java.util.Set limitQuerySequential(com.tangosol.net.partition.PartitionSet partitions, com.tangosol.util.filter.LimitFilter filterLimit, boolean fKeysOnly)
        {
        // import Component.Net.Member;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.SimpleEnumerator;
        // import com.tangosol.util.filter.LimitFilter;
        // import java.util.Collections;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.Set;

        _assert(filterLimit.getComparator()   == null);
        _assert(filterLimit.getTopAnchor()    == null);
        _assert(filterLimit.getBottomAnchor() == null);

        PartitionedCache service  = getService();
        int    cPageSize = filterLimit.getPageSize();

        // in dedication to Jim Flowers[]
        Object[] aoCookie    = (Object[]) filterLimit.getCookie();
        List     listMember  = Collections.emptyList();
        int      cSkipMember = -1; // how many to skip for the current member
        int      cSkipGlobal = -1; // how many to skip altogether (including cSkipMember)
        int      cSkipActual = cPageSize*filterLimit.getPage();
        int      cAdjustment = 0;
        if (aoCookie != null)
            {
            listMember  = (List)     aoCookie[0];
            cSkipMember = ((Integer) aoCookie[1]).intValue();
            cSkipGlobal = ((Integer) aoCookie[2]).intValue();
            }

        // the number of total entries excluding the entries in the member
        int cAvail = cSkipGlobal - cSkipMember;
        if (cSkipGlobal == cSkipActual)
            {
            // the page in the limit filter points the next page we have previously
            // retrieved. no need to adjust cSkipGlobal and cSkipMember.
            cAdjustment = cAvail;
            }
        else if (cSkipGlobal != -1 && cSkipActual - cAvail >= 0)
            {
            // the page in the limit filter points a further page than the page
            // cSkipGlobal represents, or the page points a backward page that
            // is in a range retrievable from the member.
            cSkipMember += cSkipActual - cSkipGlobal;
            cSkipGlobal  = cSkipActual;
            cAdjustment  = cAvail;
            }
        else
            {
            // the limit filter has not been used before, or the page in it points
            // a backward page where we need to retrieve it from the beginning.

            // to avoid a situation when all clients go to the same server for a
            // page zero we will shuffle the members in a random but constant
            // for a given filter object order
            Object[] aMember   = service.getOwnershipMemberSet().toArray();
            int      cMembers  = aMember.length;
            int      ofShuffle = Base.mod(System.identityHashCode(filterLimit), cMembers);
            if (ofShuffle != 0)
                {
                Object[] ao = new Object[cMembers];
                System.arraycopy(aMember, ofShuffle, ao, 0, cMembers - ofShuffle);
                System.arraycopy(aMember, 0, ao, cMembers - ofShuffle, ofShuffle);
                aMember = ao;
                }
            listMember  = Collections.list(new SimpleEnumerator(aMember));
            cSkipMember = cSkipActual;
            cSkipGlobal = cSkipActual;
            }

        int cPartSize      = cPageSize;
        Set setResult      = new HashSet();
        int cEstimateAvail = 0; // An estimated number of available entries per member
        int cSample        = 0;
        while (!listMember.isEmpty())
            {
            // calculate the degree of parallelism
            int cParallelism = 1; // number of members to send the request to in parallel

            // do not parallelize the query if the estimated number of available entries
            // is greater than the remaining number of entries in the page, or if there
            // are entries to be skipped on the current member.
            if (cEstimateAvail < cPartSize && cEstimateAvail > 0 &&
                cSkipMember == 0)
                {
                // assume a balanced distribution, and derive the degree of parallelism
                // from the average available entries in members.
                cParallelism = cPartSize/cEstimateAvail + 1;
                }

            // the list of members we send the query to
            List listQueryMember = listMember.subList(0, Math.min(listMember.size(), cParallelism));
            int  cQueryMember    = listQueryMember.size();

            // calculate the partitions owned by the queried members
            PartitionSet partMember = new PartitionSet(service.getPartitionCount());
            for (Iterator iter = listQueryMember.iterator(); iter.hasNext(); )
                {
                partMember.add(service.calculatePartitionSet((Member) iter.next(), 0));
                }
            partMember.retain(partitions);

            if (partMember.isEmpty())
                {
                listQueryMember.clear(); // remove members from listMember
                continue;
                }

            LimitFilter filterPart = (LimitFilter) filterLimit.clone();
            filterPart.setPageSize(cPartSize);
            filterPart.setTopAnchor(Integer.valueOf(cSkipMember)); // start off where
            // the last query ended on this member

            List listPart = query(partMember, filterPart, fKeysOnly);

            int cPartActual = 0; // the number of entries returned by the query
            int cPartAvail  = 0; // the number of available entries in total including
            // entries not returned

            List listResponse = new LinkedList();
            for (Iterator iterResponse = listPart.iterator(); iterResponse.hasNext(); )
                {
                PartitionedCache.QueryResponse response = (PartitionedCache.QueryResponse) iterResponse.next();
                if (response.getException() != null)
                    {
                    throw response.getException();
                    }
                listResponse.add(response);

                // remove the read member from listMember
                Member member = (Member) response.getFromMember();
                listMember.remove(member);

                int cMemberAvail  = response.getAvailable(); // the number of entries returned by the member
                int cMemberActual = response.getSize();      // the number of available entries in total
                // including entries not returned
                cPartAvail  += cMemberAvail;
                cPartActual += cMemberActual;

                if (cPartActual >= cPartSize)
                    {
                    // we have a sufficient number of entries to fill the page.
                    // discard the remaining entries and break out from the loop.

                    // put the member at the head of the list so that we can retrieve
                    // entries from it first for the subsequent call.
                    listMember.add(0, member);
                    cSkipMember += (cMemberActual - (cPartActual - cPartSize));
                    break;
                    }

                // update the average number of available entries
                double dRatio = 1.0 / ++cSample;
                cEstimateAvail = (int) Math.round(cMemberAvail * dRatio + cEstimateAvail * (1.0 - dRatio));
                }

            if (cPartActual > 0)
                {
                Set setPart = mergeQueryResponse(listResponse, 0, cPartSize);

                int cMergeSize = setPart.size();
                setResult.addAll(setPart);
                cSkipGlobal += cMergeSize;

                if (listPart.size() != cQueryMember)
                    {
                    // There has been a redistribution. At this point there is no deterministic
                    // solution since the data was moved around; return the results we have and
                    // reset the query
                    listMember.clear();
                    break;
                    }

                if (cPartActual < cPartSize)
                    {
                    // reduce the number of entries we need to read by cMergeSize
                    cPartSize -= cMergeSize;
                    cSkipActual = 0; // the skip is already satisfied
                    cSkipMember = 0;
                    }
                else
                    {
                    // the page is filled
                    break;
                    }
                }
            else
                {
                cSkipActual -= (cAdjustment + cPartAvail);
                cSkipMember  = cSkipActual;
                cAdjustment  = 0;
                }
            }

        filterLimit.setCookie(listMember.isEmpty()
                              ? new Object[] {null, Integer.valueOf(-1), Integer.valueOf(-1)}
                              : new Object[] {listMember, Integer.valueOf(cSkipMember), Integer.valueOf(cSkipGlobal)});
        return setResult;
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey)
        {
        return lock(oKey, 0);
        }

    // From interface: com.tangosol.net.NamedCache
    /**
     * @param oKey   key being locked
     * @param cWait  the number of milliseconds to continue trying to obtain
     *  a lock; pass zero to return immediately; pass -1 to block the
     * calling thread until the lock could be obtained
     */
    public boolean lock(Object oKey, long cWaitMillis)
        {
        // import Component.Net.Lease;
        // import com.tangosol.util.Binary;

        BinaryMap.KeyRequestStatus status = null;
        try
            {
            PartitionedCache service      = getService();
            Binary  binKey       = (Binary) oKey;
            int     nMemberId    = service.getThisMember().getId();
            long    cLeaseMillis = service.getStandardLeaseMillis();
            long    lThreadId    = service.getLeaseGranularity() == Lease.BY_THREAD ?
                                   Lease.getCurrentThreadId() : 0L;

            PartitionedCache.LockRequest msg =
                    (PartitionedCache.LockRequest) service.instantiateMessage("LockRequest");
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);
            msg.setLeaseHolderId(nMemberId);
            msg.setLeaseThreadId(lThreadId);
            msg.setLeaseMillis(cLeaseMillis);
            msg.setLeaseWaitMillis(cWaitMillis);

            while (true)
                {
                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return false;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse;
                try
                    {
                    oResponse = service.poll(msg);
                    }
                catch (RuntimeException e)
                    {
                    // the most probable cause of the exception is the thread interrupt;
                    // since we cannot know whether or not the lock would have been acquired,
                    // request the unlock unconditionally
                    // Note: we have to clear the interrupt flag, in case the re-distribution
                    // is in progress and unlock() will have to wait a bit
                    boolean fInterrupt = Thread.interrupted();
                    try
                        {
                        unlock(oKey);
                        }
                    catch (RuntimeException x) {}

                    if (fInterrupt)
                        {
                        Thread.currentThread().interrupt();
                        }
                    throw e;
                    }

                if (checkResponse(msg, oResponse))
                    {
                    return ((Boolean) oResponse).booleanValue();
                    }

                msg = (PartitionedCache.LockRequest) msg.cloneMessage();
                }
            }
        finally
            {
            clearStatus(status);
            }
        }

    /**
     * Helper method to produce client-side logging of remote exceptions and
     * to wrap them to add the local thread's stack.
     */
    protected static RuntimeException logRemoteException(RuntimeException e)
        {
        // import Component.Util.Daemon.QueueProcessor.Logger;

        if (Logger.isDiagnosabilityEnabled())
            {
            _trace("Received a remote exception " + e +": " + getStackTrace(e), 2);
            }

        return e;
        }

    /**
     * Create a new Advancer for entry or value iteration.
     */
    public com.tangosol.util.PagedIterator.Advancer makeEntryAdvancer(boolean fValues, boolean fStrict)
        {
        BinaryMap.EntryAdvancer advancer = (BinaryMap.EntryAdvancer) _newChild("EntryAdvancer");
        advancer.setKeyAdvancer(makeKeyAdvancer());
        advancer.setValues(fValues);
        advancer.setStrict(fStrict);

        return advancer;
        }

    /**
     * Create a new Advancer for key iteration.
     */
    public com.tangosol.util.PagedIterator.Advancer makeKeyAdvancer()
        {
        return (BinaryMap.KeyAdvancer) _newChild("KeyAdvancer");
        }

    /**
     * Instantiate a new full partition set.
     */
    public com.tangosol.net.partition.PartitionSet makePartitionSet()
        {
        return getService().instantiatePartitionSet(/*fFill*/ true);
        }

    /**
     * Merge the results of PartialMapResponse or QueryResponse objects into
     * a map of results.
     */
    protected java.util.Map mergePartialMapResponse(java.util.List listResponse)
        {
        // import Component.Net.Message;
        // import com.tangosol.net.RequestIncompleteException;
        // import com.tangosol.util.Binary;
        // import java.util.Collection;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        Map              mapResult     = new HashMap();
        Set              setFailedKeys = null;
        RuntimeException exception     = null;

        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            Message msg = (Message) iter.next();
            if (msg instanceof PartitionedCache.PartialMapResponse)
                {
                PartitionedCache.PartialMapResponse msgResponse = (PartitionedCache.PartialMapResponse) msg;

                RuntimeException ePartial = msgResponse.getException();
                if (ePartial != null)
                    {
                    exception = ePartial;

                    Collection col = msgResponse.getFailedKeys();
                    if (col != null)
                        {
                        if (setFailedKeys == null)
                            {
                            setFailedKeys = new HashSet();
                            }
                        setFailedKeys.addAll(col);
                        }
                    }

                if (exception != null)
                    {
                    // if any member failed the partial result carries
                    // the failed keys only
                    continue;
                    }

                int      cSize = msgResponse.getSize();
                Object[] aoKey = msgResponse.getKey();
                Object[] aoVal = msgResponse.getValue();

                for (int i = 0; i < cSize; i++)
                    {
                    if (aoKey[i] != null && aoVal[i] != null)
                        {
                        mapResult.put(aoKey[i], aoVal[i]);
                        }
                    }
                }
            else
                {
                PartitionedCache.QueryResponse msgResponse = (PartitionedCache.QueryResponse) msg;

                RuntimeException ePartial = msgResponse.getException();
                if (ePartial != null)
                    {
                    throw ePartial;
                    }

                Object[] aoEntry  = msgResponse.getResult();
                int      cEntries = msgResponse.getSize();
                for (int i = 0; i < cEntries; i++)
                    {
                    java.util.Map.Entry  entry = (java.util.Map.Entry) aoEntry[i];
                    Object oKey  = PartitionedCache.QueryResponse.extractBinaryKey(entry);
                    Object oVal  = PartitionedCache.QueryResponse.extractBinaryValue(entry);

                    if (oKey != null && oVal != null)
                        {
                        mapResult.put(oKey, oVal);
                        }
                    }
                }
            }

        if (exception != null)
            {
            RequestIncompleteException e =
                    new RequestIncompleteException("Partial failure", exception);
            e.setPartialResult(setFailedKeys);

            throw e;
            }

        return mapResult;
        }

    /**
     * Merge the results of PartialValueResponse or Response objects into a
     * list of results.
     *
     * @see #validatePartialResponse
     */
    protected java.util.List mergePartialResponse(java.util.List listResponse)
        {
        // import Component.Net.Message;
        // import java.util.ArrayList;
        // import java.util.Collection;
        // import java.util.Iterator;
        // import java.util.List;

        List listResults = new ArrayList(listResponse.size());
        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            Message msg = (Message) iter.next();

            if (msg instanceof PartitionedCache.Response)
                {
                PartitionedCache.Response msgResponse = (PartitionedCache.Response) msg;

                switch (msgResponse.getResult())
                    {
                    case PartitionedCache.Response.RESULT_SUCCESS:
                        listResults.add(msgResponse.getValue());
                        break;

                    case PartitionedCache.Response.RESULT_FAILURE:
                        throw logRemoteException(msgResponse.getFailure());

                    default:
                        throw new IllegalStateException(
                                "Invalid partial response: " + msgResponse);
                    }
                }
            else
                {
                PartitionedCache.PartialValueResponse msgResponse = (PartitionedCache.PartialValueResponse) msg;

                RuntimeException exception = msgResponse.getException();
                if (exception != null)
                    {
                    throw logRemoteException(exception);
                    }

                Object oResult = msgResponse.getResult();
                if (oResult != null)
                    {
                    if (msgResponse.isCollection())
                        {
                        listResults.addAll((Collection) oResult);
                        }
                    else
                        {
                        listResults.add(oResult);
                        }
                    }
                }
            }
        return listResults;
        }

    /**
     * Merge the results of QueryResponse objects
     */
    protected java.util.Set mergeQueryResponse(java.util.List listResponse)
        {
        return mergeQueryResponse(listResponse, 0, 0);
        }

    /**
     * Merge the results of QueryResponse objects into a set of no more then
     * the specified number of items after skipping the specified number.
     */
    protected java.util.Set mergeQueryResponse(java.util.List listResponse, int cSkip, int cLimit)
        {
        // import com.tangosol.util.ImmutableArrayList;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map$Entry as java.util.Map.Entry;

        int cSize = 0;
        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            PartitionedCache.QueryResponse msgResponse = (PartitionedCache.QueryResponse) iter.next();
            cSize += msgResponse.getSize();
            }

        List listMerge = new ArrayList(cLimit > 0 ? cLimit : cSize);
        int  cMerge    = 0;

        all: for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            PartitionedCache.QueryResponse msgResponse = (PartitionedCache.QueryResponse) iter.next();

            if (msgResponse.getException() != null)
                {
                throw logRemoteException(msgResponse.getException());
                }

            Object[] aoEntry  = msgResponse.getResult();
            int      cEntries = msgResponse.getSize();

            if (cSkip >= cEntries)
                {
                cSkip -= cEntries;
                }
            else
                {
                for (int i = 0; i < cEntries; i++)
                    {
                    if (cSkip > 0)
                        {
                        cSkip--;
                        }
                    else
                        {
                        Object oValue = aoEntry[i]; // either a Binary key or Map.Entry
                        listMerge.add(oValue instanceof java.util.Map.Entry
                                      ? PartitionedCache.QueryResponse.ensureSimpleEntry((java.util.Map.Entry) oValue)
                                      : oValue);
                        if (++cMerge == cLimit)
                            {
                            break all;
                            }
                        }
                    }
                }
            }

        return new ImmutableArrayList(listMerge).getSet();
        }

    /**
     * Produce an exception indicating that a storage bound method is
     * called, but there are are no storage-enabled members of
     * PartitionedCache service (or this service has been terminated), or
     * the service is below restore-quorum.
     *
     * This method is called on a client thread.
     */
    protected RuntimeException onMissingStorage(com.tangosol.net.partition.PartitionSet setOrphan)
        {
        // import com.tangosol.net.RequestPolicyException;
        // import com.tangosol.net.ServiceStoppedException;

        PartitionedCache service = getService();
        if (service.isRunning())
            {
            String sMsg;

            if (service.getOwnershipMemberSet().isEmpty())
                {
                sMsg = "No storage-enabled nodes exist for service " + service.getServiceName();
                }
            else if (!isActive())
                {
                sMsg = "The reference to cache \"" + getCacheName() + "\" has been released";
                }
            else
                {
                // should be impossible; soft assert with a non-intimidating message
                sMsg = "The storage is not available to complete the request";
                }

            return new RequestPolicyException(sMsg);
            }
        else
            {
            return new ServiceStoppedException("Service " + service.getServiceName() + " has been terminated");
            }
        }

    /**
     * Post the specified partitioned keyset-based request to all the
     * storage enabled service members that own any of the specified keys.
     *
     * Note: the passed-in key set will not be changed.
     */
    protected void postPartitionedRequest(com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest msgRequest, java.util.Set setKeys)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Net.Message.RequestMessage.DistributedCacheRequest.KeySetRequest as com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        PartitionedCache service = getService();

        Map mapByOwner = service.splitKeysByOwner(setKeys.iterator(),
                                                  0, service.getPartitionAssignments());

        // allow the very first message to use the original message instance
        boolean  fClone = false;

        for (Iterator iter = mapByOwner.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry  entry     = (java.util.Map.Entry)  iter.next();
            com.tangosol.coherence.component.net.Member member    = (com.tangosol.coherence.component.net.Member) entry.getKey();
            Set    setMember = (Set)    entry.getValue(); // keys for that member

            if (fClone)
                {
                msgRequest = (com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest) msgRequest.cloneMessage();
                }
            else
                {
                fClone = true;
                }

            msgRequest.setToMemberSet(SingleMemberSet.instantiate(member));
            msgRequest.setKeySet(setMember);

            service.post(msgRequest);
            }
        }

    /**
     * Process a RequestTimeoutException for a sendPartitionedRequest() call.
     */
    protected com.tangosol.net.RequestTimeoutException processPartitionedRequestTimeout(com.tangosol.net.RequestTimeoutException eTimeout, int nResponseType)
        {
        // import java.util.List;

        Object oResult   = null;
        List   listParts = (List) eTimeout.getPartialResult();
        try
            {
            switch (nResponseType)
                {
                case RESPONSE_SIMPLE:
                    oResult = mergePartialResponse(listParts);
                    break;
                case RESPONSE_SET:
                    oResult = mergeQueryResponse(listParts);
                    break;
                case RESPONSE_MAP:
                    oResult = mergePartialMapResponse(listParts);
                    break;
                }
            }
        catch (Throwable ignored) {}

        eTimeout.setPartialResult(oResult);
        return eTimeout;
        }

    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue)
        {
        return put(oKey, oValue, 0L, true);
        }

    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return put(oKey, oValue, cMillis, true);
        }

    /**
     * Put implementation with conditional value return and expiration. This
     * method is "component private".
     *
     * @param cMillis  the number of milliseconds until the cache entry will
     * expire
     * @param fReturn  if true, the return value is required; otherwise it
     * will be ignored
     */
    public Object put(Object oKey, Object oValue, long cMillis, boolean fReturn)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.util.Binary;

        PartitionedCache           service = getService();
        BinaryMap.KeyRequestStatus status  = null;
        com.tangosol.coherence.component.net.RequestContext           context = registerRequestContext(true);

        try
            {
            Binary binKey   = (Binary) oKey;
            Binary binValue = (Binary) oValue;

            PartitionedCache.PutRequest msg =
                    (PartitionedCache.PutRequest) service.instantiateMessage("PutRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);
            msg.setValue(binValue);
            msg.setExpiryDelay(cMillis);
            msg.setReturnRequired(fReturn);

            while (true)
                {
                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return null;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse = service.poll(msg);

                if (checkResponse(msg, oResponse))
                    {
                    return (Binary) oResponse;
                    }

                context.setOldestPendingSUID(service.getOldestPendingRequestSUID());

                msg = (PartitionedCache.PutRequest) msg.cloneMessage();
                }
            }
        finally
            {
            service.unregisterRequestContext(context);
            clearStatus(status);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    /**
     * Optimized implementation.
     */
    public void putAll(java.util.Map map)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        int cItems = map.size();
        if (cItems <= 1)
            {
            if (cItems == 1)
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) map.entrySet().iterator().next();
                put(entry.getKey(), entry.getValue(), 0L, false);
                }
            return;
            }

        PartitionedCache           service = getService();
        BinaryMap.MapRequestStatus status  = null;
        com.tangosol.coherence.component.net.RequestContext           context = registerRequestContext(true);
        try
            {
            List listParts = new LinkedList();

            PartitionedCache.PutAllRequest msg = (PartitionedCache.PutAllRequest) service.instantiateMessage("PutAllRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());

            while (true)
                {
                status = ensureRequestTarget(map, status, msg);
                if (status.isTargetMissing())
                    {
                    return;
                    }

                // prepare and send a poll to each member
                Map  mapByPartition = status.getEntriesByPartition();
                List lstMsg         = new LinkedList();
                int  cTotal         = 0;

                for (Iterator iter = mapByPartition.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry   entry   = (java.util.Map.Entry) iter.next();
                    Integer nPart   = (Integer) entry.getKey();
                    Map     mapPart = (Map) entry.getValue();
                    com.tangosol.coherence.component.net.Member  owner   = service.getPrimaryOwner(nPart.intValue());

                    if (owner == null)
                        {
                        // partition is being transferred, add to orphaned entries to retry later
                        status.getOrphanedEntries().put(nPart, mapPart);
                        }
                    else
                        {
                        msg = (PartitionedCache.PutAllRequest) msg.cloneMessage();
                        msg.setToMemberSet(SingleMemberSet.instantiate(owner));
                        msg.setMap(mapPart);
                        msg.setPartResults(listParts);

                        lstMsg.add(msg);
                        }

                    cTotal += mapPart.size();
                    }

                service.poll(lstMsg, msg.checkTimeoutRemaining());

                Map mapResend = status.getOrphanedEntries();
                for (Iterator iter = lstMsg.iterator(); iter.hasNext(); )
                    {
                    msg = (PartitionedCache.PutAllRequest) iter.next();

                    Map mapPart = msg.getMap();
                    if (mapPart != null && !mapPart.isEmpty())
                        {
                        mapResend.put(Integer.valueOf(service.getKeyPartition((Binary) mapPart.keySet().iterator().next())), mapPart);
                        }
                    }

                int cResend = 0;
                for (Iterator iter = mapResend.values().iterator(); iter.hasNext(); )
                    {
                    cResend += ((Map) iter.next()).size();
                    }

                if (cResend == 0)
                    {
                    break;
                    }

                // update orphaned partition set
                PartitionSet partOrphaned = new PartitionSet(service.getPartitionCount());
                for (Iterator iter = mapResend.keySet().iterator(); iter.hasNext(); )
                    {
                    partOrphaned.add(((Integer) iter.next()).intValue());
                    }
                status.setOrphanedPartitions(partOrphaned);

                // fail if we have no storage enabled members
                if (service.getOwnershipMemberSet().isEmpty())
                    {
                    PartitionSet setOrphans = status.getOrphanedPartitions();
                    clearStatus(status);
                    throw onMissingStorage(setOrphans);
                    }

                reportRepeat("PutAllRequest", cResend, cTotal, partOrphaned);

                map = mapResend;
                context.setOldestPendingSUID(service.getOldestPendingRequestSUID());
                }

            validatePartialResponse(listParts);
            }
        finally
            {
            service.unregisterRequestContext(context);
            clearStatus(status);
            }
        }

    /**
     * @return a list of QueryReponse objects
     */
    protected java.util.List query(com.tangosol.net.partition.PartitionSet partitions, com.tangosol.util.Filter filter, boolean fKeysOnly)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.PriorityTask;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.QueryRequest msg = service.isVersionCompatible(BinaryMap::isPartitionedQueryCompatible)
                                                ? (PartitionedCache.QueryRequest) service.instantiateMessage("PartitionedQueryRequest")
                                                : (PartitionedCache.QueryRequest) service.instantiateMessage("QueryRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setFilter(filter);
            msg.setKeysOnly(fKeysOnly);

            if (filter instanceof PriorityTask)
                {
                msg.copyPriorityAttributes((PriorityTask) filter);
                }

            return sendPartitionedRequest(msg, partitions, false);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    /**
     * Create a filter id for the specified filter. Note that since the
     * filter ids are created by using Service.getSUID() method, zero is
     * guaranteed to be an invalid id.
     *
     * Note: all calls to this method should be synchronized using the
     * ListenerSupport object.
     */
    protected long registerFilter(com.tangosol.util.Filter filter)
        {
        long lFilterId = getService().getSUID(PartitionedCache.SUID_FILTER);

        getFilterArray().set(lFilterId, filter);

        return lFilterId;
        }

    /**
     * Register a new RequestContext with the service.
     *
     * @param fRequired  if true, the context must be created
     * unconditionally; otherwise its creation is optional
     */
    protected com.tangosol.coherence.component.net.RequestContext registerRequestContext(boolean fRequired)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.security.SecurityHelper;

        com.tangosol.coherence.component.net.RequestContext context = null;

        if (isAuthorizationEnabled())
            {
            context = getService().registerRequestContext(null);
            context.setSubject(SecurityHelper.getCurrentSubject());
            }
        else if (fRequired)
            {
            context = getService().registerRequestContext(null);
            }

        return context;
        }

    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        throw new UnsupportedOperationException();
        }

    // From interface: com.tangosol.net.NamedCache
    public Object remove(Object oKey)
        {
        return remove(oKey, true);
        }

    /**
     * Remove implementation with conditional value return. This method is
     * "component private".
     *
     * @param fReturn  if true, the return value is required; otherwise it
     * will be ignored
     */
    public Object remove(Object oKey, boolean fReturn)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.util.Binary;

        PartitionedCache           service = getService();
        BinaryMap.KeyRequestStatus status  = null;
        com.tangosol.coherence.component.net.RequestContext           context = registerRequestContext(true);
        try
            {
            Binary binKey = (Binary) oKey;

            PartitionedCache.RemoveRequest msg =
                    (PartitionedCache.RemoveRequest) service.instantiateMessage("RemoveRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);
            msg.setReturnRequired(fReturn);

            while (true)
                {
                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return null;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse = service.poll(msg);

                if (checkResponse(msg, oResponse))
                    {
                    return (Binary) oResponse;
                    }

                context.setOldestPendingSUID(service.getOldestPendingRequestSUID());

                msg = (PartitionedCache.RemoveRequest) msg.cloneMessage();
                }
            }
        finally
            {
            service.unregisterRequestContext(context);
            clearStatus(status);
            }
        }

    /**
     * Optimized implementation.
     */
    public boolean removeAll(java.util.Collection colKeys)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.HashSet;
        // import java.util.List;
        // import java.util.Set;

        Set setKeys = colKeys instanceof Set ? (Set) colKeys : new HashSet(colKeys);
        int cKeys   = setKeys.size();
        if (cKeys == 0)
            {
            return false;
            }
        if (cKeys == 1)
            {
            Binary binKey = (Binary) setKeys.iterator().next();
            return remove(binKey, false) != null;
            }

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(true);
        try
            {
            PartitionedCache.RemoveAllRequest msg =
                    (PartitionedCache.RemoveAllRequest) service.instantiateMessage("RemoveAllRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());

            RequestTimeoutException eTimeout = null;
            List listResponse;
            try
                {
                listResponse = sendPartitionedRequest(msg, setKeys);
                }
            catch (RequestTimeoutException e)
                {
                eTimeout     = e;
                listResponse = (List) e.getPartialResult();
                if (listResponse == null)
                    {
                    throw e;
                    }
                }

            for (Iterator iter = listResponse.iterator(); iter.hasNext();)
                {
                PartitionedCache.PartialValueResponse response = (PartitionedCache.PartialValueResponse) iter.next();

                RuntimeException exception = response.getException();
                if (exception != null)
                    {
                    throw exception;
                    }

                Object oResult = response.getResult();
                if (oResult != null) // Storage.BINARY_EXISTS
                    {
                    return true;
                    }
                }

            if (eTimeout != null)
                {
                // we don't really know
                eTimeout.setPartialResult(null);
                throw eTimeout;
                }

            return false;
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.IndexRequest msg =
                    (PartitionedCache.IndexRequest) service.instantiateMessage("IndexRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setAdd(false);
            msg.setExtractor(extractor);

            sendStorageRequest(msg);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;

        removeMapListener(listener, (Filter) null, null);
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        throw new UnsupportedOperationException();
        }

    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, com.tangosol.util.MapTrigger trigger)
        {
        // import Component.Net.MemberSet;
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;

        long lFilterId = 0L;

        if (trigger == null)
            {
            com.tangosol.util.MapListenerSupport support = getListenerSupport();
            synchronized (support)
                {
                if (support.removeListenerWithCheck(listener, filter))
                    {
                    lFilterId = getFilterId(filter);
                    getFilterArray().remove(lFilterId);
                    }
                else
                    {
                    return;
                    }
                }
            }

        PartitionedCache   service   = getService();
        MemberSet setOwners = service.getOwnershipMemberSet();

        if (setOwners.isEmpty())
            {
            return;
            }

        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);
        try
            {
            PartitionedCache.ListenerRequest msg =
                    (PartitionedCache.ListenerRequest) service.instantiateMessage("ListenerRequest");
            msg.setToMemberSet(setOwners);
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setAdd(false);
            msg.setFilter(filter);
            msg.setFilterId(lFilterId);
            msg.setTrigger(trigger);
            msg.setMemberId(service.getThisMember().getId());
            msg.setPartitions(makePartitionSet());

            service.checkQuorum(msg, msg.isReadOnly());

            if (listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener)
                {
                service.send(msg);
                }
            else
                {
                sendStorageRequest(msg);
                }
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        throw new UnsupportedOperationException();
        }

    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fPriming)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;

        Binary binKey = (Binary) oKey;

        if (!getListenerSupport().removeListenerWithCheck(listener, binKey))
            {
            return;
            }

        BinaryMap.KeyRequestStatus status = null;
        try
            {
            PartitionedCache service    = getService();
            int     iPartition = service.getKeyPartition(binKey);

            PartitionedCache.KeyListenerRequest msg =
                    (PartitionedCache.KeyListenerRequest) service.instantiateMessage("KeyListenerRequest");
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);
            msg.setAdd(false);
            msg.setPriming(fPriming);

            while (true)
                {
                // in-lined and slightly twisted "ensureRequestTarget" code
                if (status == null)
                    {
                    status = new BinaryMap.KeyRequestStatus();
                    status.setPartition(iPartition);
                    _linkChild(status);
                    }
                else
                    {
                    waitForRedistribution(status, msg.getRequestTimeout());
                    }

                service.checkQuorum(msg, msg.isReadOnly());

                com.tangosol.coherence.component.net.Member memberOwner = service.getPrimaryOwner(iPartition);
                if (memberOwner == null)
                    {
                    if (service.getOwnershipMemberSet().isEmpty())
                        {
                        // no reason to complain
                        return;
                        }
                    if (Thread.currentThread() == service.getThread())
                        {
                        // we can neither remove the listener, nor wait;
                        // leaking a listener is the lesser of two evils...
                        // we will remove it later (during dispatch())
                        // if an event comes, but there are no listeners for it
                        return;
                        }
                    if (listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener)
                        {
                        // compensation for COH-1386 until the real fix
                        // in the NearCache itself
                        return;
                        }
                    continue;
                    }
                else
                    {
                    status.setOwner(memberOwner);
                    }

                msg.setToMemberSet(status.getOwnerSet());

                if (listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener)
                    {
                    // similar to the comment above, there is a possibility
                    // of leaking the listener during re-distribution
                    service.send(msg);
                    break;
                    }
                else
                    {
                    Object oResponse = service.poll(msg);

                    if (checkResponse(msg, oResponse))
                        {
                        break;
                        }
                    }

                msg = (PartitionedCache.KeyListenerRequest) msg.cloneMessage();
                }
            }
        finally
            {
            clearStatus(status);
            }
        }

    public void removeMapListener(com.tangosol.util.MapListener listener, java.util.Set setBinKeys, boolean fPriming)
        {
        // import java.util.HashSet;

        // clone, since we might be removing some
        setBinKeys = new HashSet(setBinKeys);

        getListenerSupport().removeListenerWithCheck(listener, setBinKeys);

        if (!setBinKeys.isEmpty())
            {
            // the "fLite" flag is not used by the "removeMapListener" implementation
            sendMapListenerAllRequest(listener, setBinKeys, /*fLite*/ true, fPriming, /*fAdd*/ false, /*versions*/ null);
            }
        }

    public static void reportRepeat(String sRequest, int cItems, int cTotal, com.tangosol.net.partition.PartitionSet partitions)
        {
        StringBuilder sb = new StringBuilder();

        sb.append("Repeating ")
                .append(sRequest);

        if (cItems > 0)
            {
            sb.append(" for ")
                    .append(cItems);

            if (cTotal > 0)
                {
                sb.append(" out of ")
                        .append(cTotal);
                }
            sb.append(" items");
            }

        if (partitions != null)
            {
            sb.append(" due to the re-distribution of ")
                    .append(partitions);
            }

        _trace(sb.toString(), 3);
        }

    /**
     * Note: as of Coherence 12.2.1, this method is only called with fLite
     * == true and fPriming == true. Also, there is no corresponding
     * "remove" method.
     */
    protected void sendMapListenerAllRequest(com.tangosol.util.MapListener listener, java.util.Set setBinKeys, boolean fLite, boolean fPriming, boolean fAdd, com.tangosol.net.partition.VersionedPartitions versions)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);
        try
            {
            PartitionedCache.KeyListenerAllRequest msg =
                    (PartitionedCache.KeyListenerAllRequest) service.instantiateMessage("KeyListenerAllRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setAdd(fAdd);
            msg.setLite(fLite);
            msg.setPriming(fPriming);
            msg.setPartitionVersions(versions);

            if (fAdd || !(listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener))
                {
                mergePartialMapResponse(sendPartitionedRequest(msg, setBinKeys));
                }
            else
                {
                // removal of sync listeners must not be blocking
                postPartitionedRequest(msg, setBinKeys);
                }
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    protected void sendMapListenerRequest(long lFilterId, com.tangosol.util.Filter filter, com.tangosol.util.MapTrigger trigger, boolean fLite, com.tangosol.net.partition.VersionedPartitions versions)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(true);

        try
            {
            PartitionedCache.ListenerRequest msg =
                    (PartitionedCache.ListenerRequest) service.instantiateMessage("ListenerRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setAdd(true);
            msg.setFilter(filter);
            msg.setFilterId(lFilterId);
            msg.setLite(fLite);
            msg.setTrigger(trigger);
            msg.setPartitionVersions(versions);
            msg.setMemberId(service.getThisMember().getId());
            msg.setPartitions(makePartitionSet());

            sendStorageRequest(msg);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    protected void sendMapListenerRequest(com.tangosol.util.Binary binKey, boolean fLite, boolean fPriming, boolean fVersioned, long lVersion)
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        BinaryMap.KeyRequestStatus status = null;

        try
            {
            while (true)
                {
                PartitionedCache.KeyListenerRequest msg =
                        (PartitionedCache.KeyListenerRequest) service.instantiateMessage("KeyListenerRequest");
                msg.setRequestContext(context);
                msg.setCacheId(getCacheId());
                msg.setKey(binKey);
                msg.setAdd(true);
                msg.setLite(fLite);
                msg.setPriming(fPriming);

                if (fVersioned)
                    {
                    msg.setPartitionVersion(lVersion);
                    }

                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse = service.poll(msg);

                if (checkResponse(msg, oResponse))
                    {
                    return;
                    }
                }
            }
        finally
            {
            clearStatus(status);

            service.unregisterRequestContext(context);
            }
        }

    /**
     * Send the specified partitioned keyset-based request to all the
     * storage enabled service members that own any of the specified keys.
     *
     * Note: the passed-in key set will not be changed.
     */
    protected java.util.List sendPartitionedRequest(com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest msgRequest, java.util.Set setKeys)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Net.Message.RequestMessage.DistributedCacheRequest.KeySetRequest as com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest;
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        BinaryMap.KeySetRequestStatus status = null;
        try
            {
            PartitionedCache service   = getService();
            List    listParts = new LinkedList();
            Set     setResend = setKeys;
            boolean fClone    = false;

            while (true)
                {
                status = ensureRequestTarget(setResend, status, msgRequest);
                if (status.isTargetMissing())
                    {
                    return listParts;
                    }

                Map mapByOwner = status.getKeysByOwner();

                // prepare and send a poll to each member
                int       cMsgs      = mapByOwner.size();
                com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest[] aMsg       = new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest[cMsgs];
                com.tangosol.coherence.component.net.Member    memberThis = service.getThisMember();
                int       iMsgThis   = -1;

                Iterator iter = mapByOwner.entrySet().iterator();
                for (int iMsg = 0; iMsg < cMsgs; iMsg++)
                    {
                    java.util.Map.Entry  entry     = (java.util.Map.Entry)  iter.next();
                    com.tangosol.coherence.component.net.Member member    = (com.tangosol.coherence.component.net.Member) entry.getKey();
                    Set    setMember = (Set)    entry.getValue(); // keys for that member

                    if (member == memberThis)
                        {
                        iMsgThis = iMsg;
                        }

                    if (fClone)
                        {
                        msgRequest = (com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest) msgRequest.cloneMessage();
                        }
                    else
                        {
                        // allow the very first message to use the original message instance
                        fClone = true;
                        }
                    msgRequest.setToMemberSet(SingleMemberSet.instantiate(member));
                    msgRequest.setKeySet(setMember);
                    msgRequest.setPartResults(listParts);

                    aMsg[iMsg] = msgRequest;
                    }

                // post msg to ourself last to avoid processing said message (accelerated
                // path) prior to serializing messages to remote members; any
                // references to the same object across messages could be mutated
                // if message processing is accelerated (COH-18092)
                if (iMsgThis != -1)
                    {
                    com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest msgSwap = aMsg[cMsgs - 1];

                    aMsg[cMsgs - 1] = aMsg[iMsgThis];
                    aMsg[iMsgThis]  = msgSwap;
                    }

                try
                    {
                    // Note: checkTimeoutRemaining will throw if the request timeout has passed
                    service.poll(aMsg, msgRequest.checkTimeoutRemaining());
                    }
                catch (RequestTimeoutException e)
                    {
                    e.setPartialResult(listParts);
                    throw e;
                    }

                // collect all unprocessed keys (if any)
                setResend = status.getOrphanedKeys();
                for (int iMsg = 0; iMsg < cMsgs; iMsg++)
                    {
                    Set setReject = aMsg[iMsg].getKeySet();
                    if (setReject != null && !setReject.isEmpty())
                        {
                        if (setResend == null)
                            {
                            setResend = new HashSet();
                            }
                        setResend.addAll(setReject);
                        }
                    }

                if (setResend == null || setResend.isEmpty())
                    {
                    break;
                    }

                reportRepeat(msgRequest.get_Name(), setResend.size(), setKeys.size(),
                             service.calculatePartitionSet(setResend));

                com.tangosol.coherence.component.net.RequestContext context = msgRequest.getRequestContext();
                if (context != null)
                    {
                    context.setOldestPendingSUID(service.getOldestPendingRequestSUID());
                    }
                }
            return listParts;
            }
        finally
            {
            clearStatus(status);
            }
        }

    /**
     * Send the specified partitioned request to all the storage enabled
     * service members that own any of the specified partitions.
     *
     * @param msgRequest    the partitioned request to send
     * @param partitions         the set of partitions to send the request
     * to
     * @param fInternal           true iff the request is "internal"; false
     * if the request is a direct result of a client call to the
     *                                          NamedCache API
     *
     * Note: the passed-in PartitionSet will be changed.
     */
    protected java.util.List sendPartitionedRequest(com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest msgRequest, com.tangosol.net.partition.PartitionSet partitions, boolean fInternal)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Net.Message.RequestMessage.DistributedCacheRequest.PartialRequest as com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest;
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.LiteMap;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        List listParts = new LinkedList();
        if (partitions.isEmpty())
            {
            return listParts;
            }

        BinaryMap.PartialRequestStatus status = null;
        try
            {
            PartitionedCache service = getService();
            boolean fClone  = false;

            while (true)
                {
                status = ensureRequestTarget(partitions, status, msgRequest);
                if (status.isTargetMissing())
                    {
                    return listParts;
                    }

                Map mapByOwner = status.getPartitionsByOwner();
                if (mapByOwner.size() == 1)
                    {
                    if (fClone)
                        {
                        msgRequest = (com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest) msgRequest.cloneMessage();
                        }
                    else
                        {
                        // allow the very first message to use the original message instance
                        fClone = true;
                        }

                    com.tangosol.coherence.component.net.Member       member     = (com.tangosol.coherence.component.net.Member)       mapByOwner.keySet().iterator().next();
                    PartitionSet partMember = (PartitionSet) mapByOwner.get(member);

                    msgRequest.setToMemberSet(SingleMemberSet.instantiate(member));
                    msgRequest.setPartResults(listParts);
                    msgRequest.setRequestMask(partMember);
                    msgRequest.setRepliesMask(new PartitionSet(partMember));
                    msgRequest.setPartitions(partitions);

                    service.poll(msgRequest);
                    }
                else
                    {
                    // prepare and send a poll to each member
                    int       cMsgs      = mapByOwner.size();
                    com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest[] aMsg       = new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest[cMsgs];
                    com.tangosol.coherence.component.net.Member    memberThis = service.getThisMember();
                    int       iMsgThis   = -1;

                    Iterator iter = mapByOwner.entrySet().iterator();
                    for (int iMsg = 0; iMsg < cMsgs; iMsg++)
                        {
                        java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

                        com.tangosol.coherence.component.net.Member       member     = (com.tangosol.coherence.component.net.Member)       entry.getKey();
                        PartitionSet partMember = (PartitionSet) entry.getValue(); // partitions for that member

                        if (member == memberThis)
                            {
                            iMsgThis = iMsg;
                            }

                        if (fClone)
                            {
                            msgRequest = (com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest) msgRequest.cloneMessage();
                            }
                        else
                            {
                            // allow the very first message to use the original message instance
                            fClone = true;
                            }

                        msgRequest.setToMemberSet(SingleMemberSet.instantiate(member));
                        msgRequest.setRequestMask(partMember);
                        msgRequest.setPartResults(listParts);
                        msgRequest.setRepliesMask(new PartitionSet(partMember));
                        msgRequest.setPartitions(partitions);

                        aMsg[iMsg] = msgRequest;
                        }

                    // post msg to ourself last to avoid processing said message (accelerated
                    // path) prior to serializing messages to remote members; any
                    // references to the same object across messages could be mutated
                    // if message processing is accelerated (COH-18092)
                    if (iMsgThis != -1)
                        {
                        com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest msgSwap = aMsg[cMsgs - 1];

                        aMsg[cMsgs - 1] = aMsg[iMsgThis];
                        aMsg[iMsgThis]  = msgSwap;
                        }

                    try
                        {
                        // Note: checkTimeoutRemaining will throw if the request timeout has passed
                        service.poll(aMsg, msgRequest.checkTimeoutRemaining());
                        }
                    catch (RequestTimeoutException e)
                        {
                        e.setPartialResult(listParts);
                        throw e;
                        }
                    }

                if (partitions.isEmpty())
                    {
                    break;
                    }

                if (!fInternal)
                    {
                    reportRepeat(msgRequest.get_Name(), 0, 0, partitions);
                    }

                com.tangosol.coherence.component.net.RequestContext context = msgRequest.getRequestContext();
                if (context != null)
                    {
                    context.setOldestPendingSUID(service.getOldestPendingRequestSUID());
                    }
                }

            return listParts;
            }
        finally
            {
            clearStatus(status);
            }
        }

    /**
     * Send the specified request to the storage-senior, to be relayed to
     * all the storage enabled service members.
     */
    protected Object sendStorageRequest(com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.StorageRequest msgRequest)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Net.Message.RequestMessage.DistributedCacheRequest.StorageRequest;
        // import com.tangosol.net.partition.PartitionSet;

        PartitionedCache      service = getService();
        PartitionSet parts   = msgRequest.getRequestPartitions(); // not null for ListenerRequest

        if (parts == null)
            {
            parts = makePartitionSet();
            }

        while (true)
            {
            com.tangosol.coherence.component.net.Member memberSenior = service.getOwnershipSenior(true);
            if (memberSenior == null ||  // no ownership-enabled members
                !isActive())             // cache was concurrently destroyed
                {
                throw onMissingStorage(parts);
                }

            // wait for all partitions to be owned
            ensureRequestTarget(parts, msgRequest);

            service.checkQuorum(msgRequest, msgRequest.isReadOnly());

            msgRequest.addToMember(memberSenior);

            Object oResult = service.poll(msgRequest);
            if (checkResponse(msgRequest, oResult))
                {
                parts.remove((PartitionSet) oResult);
                if (parts.isEmpty())
                    {
                    // request was successful
                    return oResult;
                    }
                }

            // retry the request
            //
            // Note: unlike other partitioned requests, StorageRequests must be
            //       addressed to the full partition set and may not be re-tried
            //       to only a subset of the partitions due to the "global" nature
            //       of the requests themselves (until COH-3435/COH-6520 are
            //       implemented) in order to prevent a "partial" state (they are
            //       idempotent though, so resending is safe).
            _trace("Repeating " + msgRequest.get_Name(), 5);
            msgRequest = (StorageRequest) msgRequest.cloneMessage();
            }
        }

    // Accessor for the property "AuthorizationEnabled"
    /**
     * Setter for property AuthorizationEnabled.<p>
     * Specifies whether or not the StorageAccessAuthorizer is configured
     * for this cache.
     */
    protected void setAuthorizationEnabled(boolean fEnabled)
        {
        __m_AuthorizationEnabled = fEnabled;
        }

    // Accessor for the property "CacheId"
    /**
     * Setter for property CacheId.<p>
     * The cache id this BinaryMap represents.
     *
     * @see PartitionedCache#StorageArray
     */
    public void setCacheId(long lCacheId)
        {
        __m_CacheId = lCacheId;
        }

    // Accessor for the property "CacheName"
    /**
     * Setter for property CacheName.<p>
     * The cache name this BinaryMap represents.
     */
    public void setCacheName(String sCacheName)
        {
        __m_CacheName = (sCacheName);

        try
            {
            setAuthorizationEnabled(getService().getBackingMapManager().getStorageAccessAuthorizer(sCacheName) != null);
            }
        catch (RuntimeException e)
            {
            _trace("BackingMapManager " + getService().getBackingMapManager().getClass().getName() +
                   ": disabling StorageAccessAuthorizer due to failure to access it for cache: " + sCacheName, 1);
            setAuthorizationEnabled(false);
            }
        }

    // Accessor for the property "Confirmed"
    /**
     * Setter for property Confirmed.<p>
     * Flag indicating whether or not the existence of the cache associated
     * with this $BinaryMap has been confirmed with the partition owners.
     *
     * @volatile
     */
    public void setConfirmed(boolean fConfirmed)
        {
        __m_Confirmed = fConfirmed;
        }

    // Accessor for the property "Dispatcher"
    /**
     * Setter for property Dispatcher.<p>
     * An EventDispatcher that can dispatch cache lifecycle events.
     *
     * This is null for storage-enabled members and non-null otherwise.
     */
    public void setDispatcher(com.tangosol.net.events.internal.StorageDispatcher dispatcher)
        {
        __m_Dispatcher = dispatcher;
        }

    // Accessor for the property "FilterArray"
    /**
     * Setter for property FilterArray.<p>
     * A LongArray of Filter objects indexed by the unique filter id. These
     * filter id values are used by the MapEvent message to specify what
     * filters caused a cache event.
     *
     * Note: all access (for update) to this array should be synchronized on
     * the ListenerSupport object.
     */
    protected void setFilterArray(com.tangosol.util.LongArray array)
        {
        __m_FilterArray = array;
        }

    // Accessor for the property "ListenerSupport"
    /**
     * Setter for property ListenerSupport.<p>
     */
    protected void setListenerSupport(com.tangosol.util.MapListenerSupport support)
        {
        __m_ListenerSupport = support;
        }

    // Accessor for the property "ReadLocator"
    /**
     * Setter for property ReadLocator.<p>
     * A BiFunction<Ownership,PartitionedService,Member> that returns the
     * Member to target read requests against.
     */
    public void setReadLocator(java.util.function.BiFunction function)
        {
        __m_ReadLocator = function;
        }

    // From interface: com.tangosol.net.NamedCache
    public int size()
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import java.util.Iterator;
        // import java.util.List;

        PartitionedCache.SizeRequest msg =
                (PartitionedCache.SizeRequest) getService().instantiateMessage("SizeRequest");
        msg.setCacheId(getCacheId());

        RequestTimeoutException eTimeout = null;
        List listResponse;
        try
            {
            listResponse = sendPartitionedRequest(msg, makePartitionSet(), false);
            }
        catch (RequestTimeoutException e)
            {
            eTimeout     = e;
            listResponse = (List) e.getPartialResult();
            if (listResponse == null)
                {
                throw e;
                }
            }

        int cSize = 0;
        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            PartitionedCache.PartialValueResponse response = (PartitionedCache.PartialValueResponse) iter.next();

            RuntimeException exception = response.getException();
            if (exception != null)
                {
                throw exception;
                }

            Object oResult = response.getResult();
            if (oResult instanceof Integer)
                {
                cSize += ((Integer) oResult).intValue();
                }
            }

        if (eTimeout != null)
            {
            eTimeout.setPartialResult(Integer.valueOf(cSize));
            throw eTimeout;
            }
        return cSize;
        }

    // From interface: com.tangosol.net.NamedCache
    public void truncate()
        {
        // import Component.Net.RequestContext as com.tangosol.coherence.component.net.RequestContext;
        // import com.tangosol.net.RequestTimeoutException;

        PartitionedCache service = getService();
        com.tangosol.coherence.component.net.RequestContext context = registerRequestContext(false);

        try
            {
            PartitionedCache.ClearRequest msg = (PartitionedCache.ClearRequest)
                    service.instantiateMessage("ClearRequest");
            msg.setRequestContext(context);
            msg.setCacheId(getCacheId());
            msg.setTruncate(true);

            mergePartialResponse(
                    sendPartitionedRequest(msg, makePartitionSet(), false));
            }
        catch (RequestTimeoutException e)
            {
            throw processPartitionedRequestTimeout(e, RESPONSE_VOID);
            }
        finally
            {
            service.unregisterRequestContext(context);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean unlock(Object oKey)
        {
        // import Component.Net.Lease;
        // import com.tangosol.util.Binary;

        PartitionedCache service   = getService();
        Binary  binKey    = (Binary) oKey;
        int     nMemberId = service.getThisMember().getId();
        long    lThreadId = service.getLeaseGranularity() == Lease.BY_THREAD ?
                            Lease.getCurrentThreadId() : 0L;

        BinaryMap.KeyRequestStatus status = null;
        try
            {
            PartitionedCache.UnlockRequest msg =
                    (PartitionedCache.UnlockRequest) service.instantiateMessage("UnlockRequest");
            msg.setCacheId(getCacheId());
            msg.setKey(binKey);
            msg.setLeaseHolderId(nMemberId);
            msg.setLeaseThreadId(lThreadId);

            while (true)
                {
                status = ensureRequestTarget(binKey, status, msg);
                if (status.isTargetMissing())
                    {
                    return false;
                    }

                msg.setToMemberSet(status.getOwnerSet());

                Object oResponse = service.poll(msg);

                if (checkResponse(msg, oResponse))
                    {
                    return ((Boolean) oResponse).booleanValue();
                    }

                msg = (PartitionedCache.UnlockRequest) msg.cloneMessage();
                }
            }
        finally
            {
            clearStatus(status);
            }
        }

    /**
     * Check whether or not the list of partial responses carries any
     * exceptions.
     *
     * @see #mergePartialResponse
     */
    protected void validatePartialResponse(java.util.List listResponse)
        {
        // import Component.Net.Message;
        // import com.tangosol.net.RequestIncompleteException;
        // import java.util.Iterator;
        // import java.util.Collection;
        // import java.util.HashSet;
        // import java.util.Set;

        RuntimeException exception     = null;
        Set              setFailedKeys = null;

        for (Iterator iter = listResponse.iterator(); iter.hasNext();)
            {
            Message msg = (Message) iter.next();

            if (msg instanceof PartitionedCache.Response)
                {
                PartitionedCache.Response msgResponse = (PartitionedCache.Response) msg;

                switch (msgResponse.getResult())
                    {
                    case PartitionedCache.Response.RESULT_SUCCESS:
                        break;

                    case PartitionedCache.Response.RESULT_FAILURE:
                        throw msgResponse.getFailure();

                    default:
                        throw new IllegalStateException(
                                "Invalid partial response: " + msgResponse);
                    }
                }
            else
                {
                PartitionedCache.PartialValueResponse msgResponse = (PartitionedCache.PartialValueResponse) msg;

                RuntimeException ePartial = msgResponse.getException();
                if (ePartial != null)
                    {
                    exception = ePartial;

                    Collection col = msgResponse.getFailedKeys();
                    if (col != null)
                        {
                        if (setFailedKeys == null)
                            {
                            setFailedKeys = new HashSet();
                            }
                        setFailedKeys.addAll(col);
                        }
                    }
                }
            }

        if (exception != null)
            {
            RequestIncompleteException e =
                    new RequestIncompleteException("Partial failure", exception);
            e.setPartialResult(setFailedKeys);

            throw e;
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Collection values()
        {
        return (BinaryMap.Values) _findChild("Values");
        }

    /**
     * This method is called when a specified owner rejected a request
     * related to the specified partitions.
     *
     * @param mapRejected  a Map<Integer, Member> that maps partition
     * numbers to Members that previously rejected a request related to that
     * partition; this map will be modified
     * @param ldtTimeout  the timestamp (safe) indicating the timeout value
     */
    protected void waitForPartitionRedistribution(java.util.Map mapRejected, long ldtTimeout)
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Base;
        // import Component.Net.Member;
        // import java.util.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache       service      = getService();
        PartitionedCache.Contention[] aContention  = (PartitionedCache.Contention[]) service.getPartitionContention();
        int           nBaseMonitor = System.identityHashCode(aContention);
        Thread        threadThis   = Thread.currentThread();
        final long    WAIT_MILLIS  = 200L;

        int cRejected = mapRejected.size();
        if (cRejected == 0)
            {
            return;
            }

        int cCleared = 0;
        int cChanged = 0;

        while (true)
            {
            List         listContentions = new LinkedList();
            PartitionSet partsProcessed  = new PartitionSet(aContention.length);

            for (Iterator iter = mapRejected.entrySet().iterator(); iter.hasNext();)
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

                int iPartition = ((Integer) entry.getKey()).intValue();
                if (partsProcessed.contains(iPartition))
                    {
                    continue;
                    }
                partsProcessed.add(iPartition);

                PartitionedCache.Contention contention;

                // this synchronization is used only to flush the contention array content
                synchronized (Base.getCommonMonitor(nBaseMonitor + iPartition))
                    {
                    contention = aContention[iPartition];
                    }

                if (contention == null)
                    {
                    cCleared++;
                    iter.remove();
                    continue;
                    }

                // check if the owner has changed since the last [failed] attempt
                Member memberOld = (Member) entry.getValue();
                Member memberNew = service.getPrimaryOwner(iPartition);
                if (memberNew != null && memberNew != memberOld)
                    {
                    cChanged++;
                    iter.remove();
                    continue;
                    }

                listContentions.add(contention);
                }

            if (2*(cCleared + cChanged) >= cRejected) // equivalent "cCleared + cChanged >= ceil(cRejected/2)"
                {
                // at least half of the partitions have been cleared or updated;
                // no need to wait any longer
                return;
                }

            for (Iterator iter = listContentions.iterator(); iter.hasNext();)
                {
                PartitionedCache.Contention contention = (PartitionedCache.Contention) iter.next();

                List    listThreads = contention.getWaitingThreads();
                boolean fTrickle    = false;

                synchronized (contention)
                    {
                    if (!listThreads.contains(threadThis))
                        {
                        listThreads.add(threadThis);
                        }

                    try
                        {
                        if (!contention.isCleared())
                            {
                            Blocking.wait(contention, WAIT_MILLIS);

                            if (Base.getSafeTimeMillis() >= ldtTimeout)
                                {
                                listThreads.remove(threadThis);

                                throw new RequestTimeoutException(
                                        "Request timed-out due to a redistribution delay");
                                }

                            // TODO: choose the "trickle" thread based on the
                            // "lightness" of the request (cRejects)
                            fTrickle = !listThreads.isEmpty() &&
                                       threadThis == listThreads.get(0);
                            }
                        }
                    catch (InterruptedException e)
                        {
                        threadThis.interrupt();
                        throw Base.ensureRuntimeException(e);
                        }
                    }

                if (contention.isCleared())
                    {
                    // the config change has been received or a tickle thread
                    // finally got through; re-evaluate whether a multi-partition
                    // request should proceed or wait for more partitions to clear
                    break;
                    }

                // unless this is a spurious wake-up, we've been waiting
                // for a configuration update for a long time, but nothing came;
                // need to force a request going back to the server;
                // we will trickle it, only one client thread per partition
                if (fTrickle)
                    {
                    return;
                    }
                }
            }
        }

    /**
     * This method is called when a partition set-related request could not
     * be completed due to a re-distribution.
     *
     * @param partsRejected  PartitionSet of rejected partitions
     * @param status               the RequestStatus
     * @param ldtTimeout       the request timeout (in safe-time), or
     * Long.MAX_VALUE for infinite
     */
    protected void waitForRedistribution(com.tangosol.net.partition.PartitionSet partsRejected, BinaryMap.PartialRequestStatus status, long ldtTimeout)
        {
        // import Component.Net.Member;
        // import com.tangosol.net.partition.PartitionSet;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        Map mapByOwner  = status.getPartitionsByOwner(); // Map<Member, PartitionSet> reflecting previous (rejected) attempt
        Map mapRejected = new HashMap();

        for (Iterator iter = mapByOwner.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

            Member       member      = (Member) entry.getKey();
            PartitionSet partsMember = (PartitionSet) entry.getValue();

            if (partsMember.intersects(partsRejected))
                {
                partsMember = new PartitionSet(partsMember); // clone
                partsMember.retain(partsRejected);

                for (int iPartition = partsMember.next(0); iPartition >= 0;
                     iPartition = partsMember.next(iPartition + 1))
                    {
                    mapRejected.put(Integer.valueOf(iPartition), member);
                    }
                }
            }

        // COH-10405: If all partitions are orphaned, make sure to
        //            collect them and wait for redistribution
        if (mapRejected.isEmpty())
            {
            PartitionedCache service = getService();
            for (int iPartition = partsRejected.next(0); iPartition >= 0;
                 iPartition = partsRejected.next(iPartition + 1))
                {
                mapRejected.put(Integer.valueOf(iPartition), service.getPrimaryOwner(iPartition));
                }
            }

        status.markInTransition(new PartitionSet(partsRejected));

        waitForPartitionRedistribution(mapRejected, ldtTimeout);

        status.setOrphanedPartitions(null);
        }

    /**
     * This method is called when a map-related request could not be
     * completed due to a re-distribution.
     *
     * @param mapRejectedEntries  Map of rejected entries
     * @param status                          the RequestStatus
     * @param ldtTimeout                  the request timeout (in
     * safe-time), or Long.MAX_VALUE for infinite
     */
    protected void waitForRedistribution(java.util.Map mapRejectedEntries, BinaryMap.MapRequestStatus status, long ldtTimeout)
        {
        // import java.util.Map;
        // import java.util.Set;

        Set setPartitions = mapRejectedEntries.keySet();

        status.markInTransition(status.getOrphanedPartitions());

        // as: waitForPartitionRedistribution expects a map with partition numbers as keys, and a Member
        //     that previously rejected that partition as the value; now that we are splitting request
        //     per partition, the latter is always null, so we can simply create the map of partitions
        //     to null members
        Map mapParts = new HashMap();
        for (Iterator it = setPartitions.iterator(); it.hasNext(); )
            {
            Integer nPart = (Integer) it.next();
            mapParts.put(nPart, null);
            }

        waitForPartitionRedistribution(mapParts, ldtTimeout);

        status.setOrphanedEntries(null);
        status.setOrphanedPartitions(null);
        }

    /**
     * This method is called when a key set-related request could not be
     * completed due to a re-distribution.
     *
     * @param setRejectedKeys  Set of rejected keys
     * @param status            the RequestStatus
     * @param ldtTimeout    the request timeout (in safe-time), or
     * Long.MAX_VALUE for infinite
     */
    protected void waitForRedistribution(java.util.Set setRejectedKeys, BinaryMap.KeySetRequestStatus status, long ldtTimeout)
        {
        // import java.util.Map;

        Map mapByOwner = status.getKeysByOwner(); // Map<Member, Object> reflecting previous (rejected) attempt

        status.markInTransition(getService().calculatePartitionSet(setRejectedKeys));

        waitForPartitionRedistribution(
                collectRejectedPartitions(setRejectedKeys, mapByOwner), ldtTimeout);

        status.setOrphanedKeys(null);
        status.setOrphanedPartitions(null);
        }

    /**
     * This method is called when a key-related request could not be
     * completed due to a re-distribution.
     *
     * @param status           the RequestStatus
     * @param ldtTimeout   the request timeout (in safe-time), or
     * Long.MAX_VALUE for infinite
     */
    protected void waitForRedistribution(BinaryMap.KeyRequestStatus status, long ldtTimeout)
        {
        // import com.tangosol.util.LiteMap;
        // import java.util.Map;

        Map mapRejected = new LiteMap();
        mapRejected.put(Integer.valueOf(status.getPartition()), status.getOwner());

        status.markInTransition();

        waitForPartitionRedistribution(mapRejected, ldtTimeout);
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$Entry

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Entry
            extends    com.tangosol.coherence.component.util.collections.WrapperEntry
        {
        // ---- Fields declarations ----

        // Default constructor
        public Entry()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public Entry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new Entry();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$Entry".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Accessor for the property "BinaryMap"
        /**
         * Getter for property BinaryMap.<p>
         */
        public BinaryMap getBinaryMap()
            {
            return (BinaryMap) get_Parent();
            }

        // Declared at the super level
        public Object setValue(Object oValue)
            {
            // import com.tangosol.util.Binary;

            getBinaryMap().put(getKey(), (Binary) oValue, 0L, false);

            return (Binary) super.setValue(oValue);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$EntryAdvancer

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EntryAdvancer
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.PagedIterator.Advancer
        {
        // ---- Fields declarations ----

        /**
         * Property EntrySetSize
         *
         * An estimate number of entries that would satisfy the size
         * threshold to be used by the entrySetPage() algorithm. Initially
         * we start with 0, indicating no up-front info. After the first
         * entry set page is retrieved, the EntrySetSize value is adjusted
         * accordingly to the response size.
         */
        private transient int __m_EntrySetSize;

        /**
         * Property KeyAdvancer
         *
         */
        private com.tangosol.util.PagedIterator.Advancer __m_KeyAdvancer;

        /**
         * Property KeySet
         *
         * A reminder of the key set returned by the Keydvancer that have
         * not beed processed yet.
         */
        private java.util.Set __m_KeySet;

        /**
         * Property Strict
         *
         * True iff the Entry objects returned by the EntrySet iterator
         * should be backed up by the cache itself.
         */
        private boolean __m_Strict;

        /**
         * Property Values
         *
         * True iff the EntryAdvancer is to return values rather than Entry
         * objects.
         */
        private boolean __m_Values;

        // Default constructor
        public EntryAdvancer()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public EntryAdvancer(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();

            // state initialization: public and protected properties
            try
                {
                setKeySet(new java.util.HashSet());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }

            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new EntryAdvancer();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$EntryAdvancer".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        /**
         * Retrieve a page of entries for a subset of the specified key set.
         * The passed in key set will be modified accordingly.
         */
        protected java.util.Map entrySetPage(java.util.Set setKeys)
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ImmutableArrayList;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            // import java.util.Set;

            BinaryMap mapBinary = getBinaryMap();

            Set setKeysPage;

            int cKeys  = setKeys.size();
            int cLimit = getEntrySetSize();
            if (cLimit > 0 && cKeys > cLimit)
                {
                // reduce the number of keys for the getAll() call
                Object[] aoKey = new Object[cLimit];
                Iterator iter  = setKeys.iterator();
                for (int i = 0; iter.hasNext() && i < cLimit; i++)
                    {
                    aoKey[i] = iter.next();
                    }
                setKeysPage = new ImmutableArrayList(aoKey);
                }
            else
                {
                setKeysPage = setKeys;
                }

            // limit the response size based on the TransferThreshold (default 0.5MB)
            Map mapPart = mapBinary.getAll(setKeysPage, mapBinary.getService().getTransferThreshold());
            int cActual = mapPart.size();

            if (cActual == cKeys    // common case - all keys were processed at once
                || cActual == 0)       // all entries are evicted (COH-2591)
                {
                setKeys.clear();
                }
            else // (cActual < cKeys)
                {
                for (Iterator iter = mapPart.entrySet().iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

                    Binary binKey   = (Binary) entry.getKey();
                    Binary binValue = (Binary) entry.getValue();

                    if (binValue == null)
                        {
                        // the value for this key does not exist [anymore]
                        iter.remove();
                        }

                    setKeys.remove(binKey);
                    }

                // adjust the EntrySetSize if necessary
                if (cLimit == 0)
                    {
                    // first adjustment
                    cLimit = cActual;
                    }
                else if (cActual < cLimit)
                    {
                    // the limit was too high; drop 50% the difference
                    cLimit -= (cLimit - cActual) / 2;
                    }
                else // (cActual >= cLimit)
                    {
                    // the limit was too low; increase 50% the difference
                    cLimit += (cActual - cLimit) / 2;
                    }
                setEntrySetSize(cLimit);
                }

            return mapPart;
            }

        public BinaryMap getBinaryMap()
            {
            return (BinaryMap) get_Parent();
            }

        // Accessor for the property "EntrySetSize"
        /**
         * Getter for property EntrySetSize.<p>
         * An estimate number of entries that would satisfy the size
         * threshold to be used by the entrySetPage() algorithm. Initially
         * we start with 0, indicating no up-front info. After the first
         * entry set page is retrieved, the EntrySetSize value is adjusted
         * accordingly to the response size.
         */
        public int getEntrySetSize()
            {
            return __m_EntrySetSize;
            }

        // Accessor for the property "KeyAdvancer"
        /**
         * Getter for property KeyAdvancer.<p>
         */
        public com.tangosol.util.PagedIterator.Advancer getKeyAdvancer()
            {
            return __m_KeyAdvancer;
            }

        // Accessor for the property "KeySet"
        /**
         * Getter for property KeySet.<p>
         * A reminder of the key set returned by the Keydvancer that have
         * not beed processed yet.
         */
        public java.util.Set getKeySet()
            {
            return __m_KeySet;
            }

        // Accessor for the property "Strict"
        /**
         * Getter for property Strict.<p>
         * True iff the Entry objects returned by the EntrySet iterator
         * should be backed up by the cache itself.
         */
        public boolean isStrict()
            {
            return __m_Strict;
            }

        // Accessor for the property "Values"
        /**
         * Getter for property Values.<p>
         * True iff the EntryAdvancer is to return values rather than Entry
         * objects.
         */
        public boolean isValues()
            {
            return __m_Values;
            }

        protected java.util.Map.Entry makeEntry(java.util.Map.Entry binEntry)
            {
            // $Entry objects could be created quite frequently for "strict" model,
            // so we manually link it instead of using _newChild() approach

            BinaryMap.Entry entry = new BinaryMap.Entry();
            entry.setEntry(binEntry);

            getBinaryMap()._linkChild(entry);
            return entry;
            }

        // From interface: com.tangosol.util.PagedIterator$Advancer
        public java.util.Collection nextPage()
            {
            // import java.util.HashSet;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            // import java.util.Set;

            Set setKeys = getKeySet();
            while (setKeys.isEmpty())
                {
                Set setPage = (Set) getKeyAdvancer().nextPage();
                if (setPage == null)
                    {
                    return null;
                    }
                setKeys.addAll(setPage);
                }

            Map mapEntries = entrySetPage(setKeys);
            if (isStrict())
                {
                HashSet setStrict = new HashSet();
                for (Iterator iter = mapEntries.entrySet().iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

                    setStrict.add(makeEntry(entry));
                    }
                return setStrict;
                }
            else
                {
                // while in the "strict" case the returned entries are backed up by the cache,
                // here we break this contract to avoid an excessive garbage generation;
                // another work around is to use cache.put() instead of entry.setValue()
                return isValues() ? mapEntries.values() : mapEntries.entrySet();
                }
            }

        // From interface: com.tangosol.util.PagedIterator$Advancer
        public void remove(Object oCurr)
            {
            // import java.util.Map$Entry as java.util.Map.Entry;

            if (isValues())
                {
                throw new UnsupportedOperationException();
                }

            getBinaryMap().remove(((java.util.Map.Entry) oCurr).getKey(), false);
            }

        // Accessor for the property "EntrySetSize"
        /**
         * Setter for property EntrySetSize.<p>
         * An estimate number of entries that would satisfy the size
         * threshold to be used by the entrySetPage() algorithm. Initially
         * we start with 0, indicating no up-front info. After the first
         * entry set page is retrieved, the EntrySetSize value is adjusted
         * accordingly to the response size.
         */
        protected void setEntrySetSize(int c)
            {
            __m_EntrySetSize = c;
            }

        // Accessor for the property "KeyAdvancer"
        /**
         * Setter for property KeyAdvancer.<p>
         */
        public void setKeyAdvancer(com.tangosol.util.PagedIterator.Advancer advancer)
            {
            __m_KeyAdvancer = advancer;
            }

        // Accessor for the property "KeySet"
        /**
         * Setter for property KeySet.<p>
         * A reminder of the key set returned by the Keydvancer that have
         * not beed processed yet.
         */
        protected void setKeySet(java.util.Set set)
            {
            __m_KeySet = set;
            }

        // Accessor for the property "Strict"
        /**
         * Setter for property Strict.<p>
         * True iff the Entry objects returned by the EntrySet iterator
         * should be backed up by the cache itself.
         */
        public void setStrict(boolean fStrict)
            {
            __m_Strict = fStrict;
            }

        // Accessor for the property "Values"
        /**
         * Setter for property Values.<p>
         * True iff the EntryAdvancer is to return values rather than Entry
         * objects.
         */
        public void setValues(boolean fValues)
            {
            __m_Values = fValues;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$EntrySet

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EntrySet
            extends    com.tangosol.coherence.component.util.Collections
            implements java.util.Set
        {
        // ---- Fields declarations ----

        /**
         * Property Strict
         *
         * True iff the Entry objects returned by the EntrySet iterator
         * should be backed up by the cache itself.
         */
        private boolean __m_Strict;

        // Default constructor
        public EntrySet()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public EntrySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new EntrySet();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$EntrySet".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: java.util.Set
        public boolean add(Object o)
            {
            throw new UnsupportedOperationException();
            }

        // From interface: java.util.Set
        public boolean addAll(java.util.Collection col)
            {
            return addAll(this, col);
            }

        // From interface: java.util.Set
        public void clear()
            {
            getBinaryMap().clear();
            }

        // From interface: java.util.Set
        public boolean contains(Object o)
            {
            // import com.tangosol.util.Base;
            // import com.tangosol.util.Binary;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;

            if (o instanceof java.util.Map.Entry)
                {
                java.util.Map.Entry  entryThat = (java.util.Map.Entry) o;
                Map    mapThis   = getBinaryMap();
                Binary binKey    = (Binary) entryThat.getKey();

                return mapThis.containsKey(binKey) &&
                       Base.equals(mapThis.get(binKey), entryThat.getValue());
                }
            else
                {
                return false;
                }
            }

        // From interface: java.util.Set
        public boolean containsAll(java.util.Collection col)
            {
            return containsAll(this, col);
            }

        // Accessor for the property "BinaryMap"
        /**
         * Getter for property BinaryMap.<p>
         */
        public BinaryMap getBinaryMap()
            {
            return (BinaryMap) get_Parent();
            }

        // From interface: java.util.Set
        public boolean isEmpty()
            {
            return getBinaryMap().isEmpty();
            }

        // Accessor for the property "Strict"
        /**
         * Getter for property Strict.<p>
         * True iff the Entry objects returned by the EntrySet iterator
         * should be backed up by the cache itself.
         */
        public boolean isStrict()
            {
            return __m_Strict;
            }

        // From interface: java.util.Set
        public java.util.Iterator iterator()
            {
            // import com.tangosol.util.PagedIterator;

            return new PagedIterator(getBinaryMap().makeEntryAdvancer(false, isStrict()));
            }

        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called
         * out of setConstructed() for the topmost component and that in
         * turn notifies all the children.
         *
         * This notification gets called before the control returns back to
         * this component instantiator (using <code>new Component.X()</code>
         * or <code>_newInstance(sName)</code>) and on the same thread. In
         * addition, visual components have a "posted" notification
         * <code>onInitUI</code> that is called after (or at the same time
         * as) the control returns back to the instantiator and possibly on
         * a different thread.
         */
        public void onInit()
            {
            // import com.tangosol.coherence.config.Config;

            try
                {
                // undocumented; for rare case of backward compatibility
                setStrict(Config.getBoolean("coherence.entryset.strict"));
                }
            catch (RuntimeException ignored) {}

            super.onInit();
            }

        // From interface: java.util.Set
        public boolean remove(Object o)
            {
            // import com.tangosol.util.Binary;
            // import java.util.Map$Entry as java.util.Map.Entry;

            if (o instanceof java.util.Map.Entry)
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) o;

                // null vs. Binary(null) serves as existence indicator
                return getBinaryMap().remove((Binary) entry.getKey(), false) != null;
                }
            else
                {
                return false;
                }
            }

        // From interface: java.util.Set
        public boolean removeAll(java.util.Collection col)
            {
            // import java.util.Iterator;
            // import java.util.HashSet;
            // import java.util.Map$Entry as java.util.Map.Entry;
            // import java.util.Set;

            Set setKeys = new HashSet();
            for (Iterator iter = col.iterator(); iter.hasNext();)
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

                setKeys.add(entry.getKey());
                }
            return getBinaryMap().removeAll(setKeys);
            }

        // From interface: java.util.Set
        public boolean retainAll(java.util.Collection col)
            {
            return retainAll(this, col);
            }

        // Accessor for the property "Strict"
        /**
         * Setter for property Strict.<p>
         * True iff the Entry objects returned by the EntrySet iterator
         * should be backed up by the cache itself.
         */
        protected void setStrict(boolean fStrict)
            {
            __m_Strict = fStrict;
            }

        // From interface: java.util.Set
        public int size()
            {
            return getBinaryMap().size();
            }

        // From interface: java.util.Set
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        // From interface: java.util.Set
        public Object[] toArray(Object[] ao)
            {
            // import java.util.ConcurrentModificationException;

            while (true)
                {
                try
                    {
                    return toArray(this, ao);
                    }
                catch (ConcurrentModificationException ignored) {}
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$KeyAdvancer

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class KeyAdvancer
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.PagedIterator.Advancer
        {
        // ---- Fields declarations ----

        /**
         * Property Partitions
         *
         * PartitionSet representing partitions that already processed by
         * this Advancer.
         */
        private com.tangosol.net.partition.PartitionSet __m_Partitions;

        // Default constructor
        public KeyAdvancer()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public KeyAdvancer(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new KeyAdvancer();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$KeyAdvancer".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        public BinaryMap getBinaryMap()
            {
            return (BinaryMap) get_Parent();
            }

        // Accessor for the property "Partitions"
        /**
         * Getter for property Partitions.<p>
         * PartitionSet representing partitions that already processed by
         * this Advancer.
         */
        public com.tangosol.net.partition.PartitionSet getPartitions()
            {
            return __m_Partitions;
            }

        // From interface: com.tangosol.util.PagedIterator$Advancer
        public java.util.Collection nextPage()
            {
            // import com.tangosol.net.partition.PartitionSet;

            PartitionSet parts = getPartitions();
            return parts.isEmpty() ? null : getBinaryMap().keySetPage(parts);
            }

        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called
         * out of setConstructed() for the topmost component and that in
         * turn notifies all the children.
         *
         * This notification gets called before the control returns back to
         * this component instantiator (using <code>new Component.X()</code>
         * or <code>_newInstance(sName)</code>) and on the same thread. In
         * addition, visual components have a "posted" notification
         * <code>onInitUI</code> that is called after (or at the same time
         * as) the control returns back to the instantiator and possibly on
         * a different thread.
         */
        public void onInit()
            {
            setPartitions(getBinaryMap().makePartitionSet());

            super.onInit();
            }

        // From interface: com.tangosol.util.PagedIterator$Advancer
        public void remove(Object binKey)
            {
            // import com.tangosol.util.Binary;

            getBinaryMap().remove((Binary) binKey, false);
            }

        // Accessor for the property "Partitions"
        /**
         * Setter for property Partitions.<p>
         * PartitionSet representing partitions that already processed by
         * this Advancer.
         */
        protected void setPartitions(com.tangosol.net.partition.PartitionSet parts)
            {
            __m_Partitions = parts;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$KeyRequestStatus

    /**
     * RequestStatus associated with a key-based request.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class KeyRequestStatus
            extends    com.tangosol.coherence.component.net.requestStatus.SinglePartStatus
        {
        // ---- Fields declarations ----

        // Default constructor
        public KeyRequestStatus()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public KeyRequestStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();

            // state initialization: public and protected properties
            try
                {
                setPartition(-1);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }

            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new KeyRequestStatus();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$KeyRequestStatus".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Declared at the super level
        /**
         * Getter for property Service.<p>
         * The DistributedCache service component associated with this
         * request status.
         */
        public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService getService()
            {
            return (PartitionedCache) get_Module();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$KeySet

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class KeySet
            extends    com.tangosol.coherence.component.util.Collections
            implements java.util.Set
        {
        // ---- Fields declarations ----

        // Default constructor
        public KeySet()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public KeySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new KeySet();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$KeySet".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: java.util.Set
        public boolean add(Object o)
            {
            throw new UnsupportedOperationException();
            }

        // From interface: java.util.Set
        public boolean addAll(java.util.Collection col)
            {
            return addAll(this, col);
            }

        // From interface: java.util.Set
        public void clear()
            {
            getBinaryMap().clear();
            }

        // From interface: java.util.Set
        public boolean contains(Object o)
            {
            // import com.tangosol.util.Binary;

            return getBinaryMap().containsKey((Binary) o);
            }

        // From interface: java.util.Set
        public boolean containsAll(java.util.Collection col)
            {
            return getBinaryMap().containsAll(col);
            }

        // Accessor for the property "BinaryMap"
        /**
         * Getter for property BinaryMap.<p>
         */
        public BinaryMap getBinaryMap()
            {
            return (BinaryMap) get_Parent();
            }

        // From interface: java.util.Set
        public boolean isEmpty()
            {
            return getBinaryMap().isEmpty();
            }

        // From interface: java.util.Set
        public java.util.Iterator iterator()
            {
            // import com.tangosol.util.PagedIterator;

            return new PagedIterator(getBinaryMap().makeKeyAdvancer());
            }

        // From interface: java.util.Set
        public boolean remove(Object o)
            {
            // import com.tangosol.util.Binary;

            // null vs. Binary(null) serves as existence indicator
            return getBinaryMap().remove((Binary) o, false) != null;
            }

        // From interface: java.util.Set
        public boolean removeAll(java.util.Collection col)
            {
            return getBinaryMap().removeAll(col);
            }

        // From interface: java.util.Set
        public boolean retainAll(java.util.Collection col)
            {
            return retainAll(this, col);
            }

        // From interface: java.util.Set
        public int size()
            {
            return getBinaryMap().size();
            }

        // From interface: java.util.Set
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        // From interface: java.util.Set
        public Object[] toArray(Object[] ao)
            {
            // import java.util.ConcurrentModificationException;

            while (true)
                {
                try
                    {
                    return toArray(this, ao);
                    }
                catch (ConcurrentModificationException ignored) {}
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$KeySetRequestStatus

    /**
     * RequestStatus associated with a key set-based request.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class KeySetRequestStatus
            extends    com.tangosol.coherence.component.net.requestStatus.MultiPartStatus
        {
        // ---- Fields declarations ----

        /**
         * Property KeysByOwner
         *
         * Map<Member, Set<Binary>>  that contains sets of Binary keys
         * split (keyed) by the owner Member.
         */
        private java.util.Map __m_KeysByOwner;

        /**
         * Property OrphanedKeys
         *
         * Set of keys that are currently orphaned (have no primary owner).
         * This property value is most commonly null (except during
         * redistribution).
         */
        private java.util.Set __m_OrphanedKeys;

        // Default constructor
        public KeySetRequestStatus()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public KeySetRequestStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new KeySetRequestStatus();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$KeySetRequestStatus".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Accessor for the property "KeysByOwner"
        /**
         * Getter for property KeysByOwner.<p>
         * Map<Member, Set<Binary>>  that contains sets of Binary keys split
         * (keyed) by the owner Member.
         */
        public java.util.Map getKeysByOwner()
            {
            return __m_KeysByOwner;
            }

        // Accessor for the property "OrphanedKeys"
        /**
         * Getter for property OrphanedKeys.<p>
         * Set of keys that are currently orphaned (have no primary owner).
         * This property value is most commonly null (except during
         * redistribution).
         */
        public java.util.Set getOrphanedKeys()
            {
            return __m_OrphanedKeys;
            }

        // Declared at the super level
        /**
         * Getter for property Service.<p>
         * The DistributedCache service component associated with this
         * request status.
         */
        public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService getService()
            {
            return (PartitionedCache) get_Module();
            }

        // Declared at the super level
        /**
         * Getter for property TargetMissing.<p>
         * (Calculated) Specifies whether the associated request is missing
         * any alive target (i.e. no storage enabled members).
         */
        public boolean isTargetMissing()
            {
            return getKeysByOwner() == null;
            }

        // Declared at the super level
        /**
         * Clear any state associated with this status.
         */
        public void reset()
            {
            setKeysByOwner(null);
            setOrphanedKeys(null);

            super.reset();
            }

        // Accessor for the property "KeysByOwner"
        /**
         * Setter for property KeysByOwner.<p>
         * Map<Member, Set<Binary>>  that contains sets of Binary keys split
         * (keyed) by the owner Member.
         */
        public void setKeysByOwner(java.util.Map map)
            {
            __m_KeysByOwner = map;
            }

        // Accessor for the property "OrphanedKeys"
        /**
         * Setter for property OrphanedKeys.<p>
         * Set of keys that are currently orphaned (have no primary owner).
         * This property value is most commonly null (except during
         * redistribution).
         */
        public void setOrphanedKeys(java.util.Set set)
            {
            __m_OrphanedKeys = set;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$MapRequestStatus

    /**
     * RequestStatus associated with a map-based request.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MapRequestStatus
            extends    com.tangosol.coherence.component.net.requestStatus.MultiPartStatus
        {
        // ---- Fields declarations ----

        /**
         * Property EntriesByPartition
         *
         * Map<Member, Map<Binary, Binary>>  that contains sets of Binary
         * entries split (keyed) by the owner Member.
         */
        private java.util.Map __m_EntriesByPartition;

        /**
         * Property OrphanedEntries
         *
         * Map of entries that are currently orphaned (have no primary
         * owner). This property value is most commonly null (except during
         * redistribution).
         */
        private java.util.Map __m_OrphanedEntries;

        // Default constructor
        public MapRequestStatus()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public MapRequestStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new MapRequestStatus();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$MapRequestStatus".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Accessor for the property "EntriesByPartition"
        /**
         * Getter for property EntriesByPartition.<p>
         * Map<Member, Map<Binary, Binary>>  that contains sets of Binary
         * entries split (keyed) by the owner Member.
         */
        public java.util.Map getEntriesByPartition()
            {
            return __m_EntriesByPartition;
            }

        // Accessor for the property "OrphanedEntries"
        /**
         * Getter for property OrphanedEntries.<p>
         * Map of entries that are currently orphaned (have no primary
         * owner). This property value is most commonly null (except during
         * redistribution).
         */
        public java.util.Map getOrphanedEntries()
            {
            return __m_OrphanedEntries;
            }

        // Declared at the super level
        /**
         * Getter for property Service.<p>
         * The DistributedCache service component associated with this
         * request status.
         */
        public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService getService()
            {
            return (PartitionedCache) get_Module();
            }

        // Declared at the super level
        /**
         * Getter for property TargetMissing.<p>
         * (Calculated) Specifies whether the associated request is missing
         * any alive target (i.e. no storage enabled members).
         */
        public boolean isTargetMissing()
            {
            return getEntriesByPartition() == null;
            }

        // Declared at the super level
        /**
         * Clear any state associated with this status.
         */
        public void reset()
            {
            // import java.util.HashMap;

            setEntriesByPartition(null);
            setOrphanedEntries(new HashMap());

            super.reset();
            }

        // Accessor for the property "EntriesByPartition"
        /**
         * Setter for property EntriesByPartition.<p>
         * Map<Member, Map<Binary, Binary>>  that contains sets of Binary
         * entries split (keyed) by the owner Member.
         */
        public void setEntriesByPartition(java.util.Map map)
            {
            __m_EntriesByPartition = map;
            }

        // Accessor for the property "OrphanedEntries"
        /**
         * Setter for property OrphanedEntries.<p>
         * Map of entries that are currently orphaned (have no primary
         * owner). This property value is most commonly null (except during
         * redistribution).
         */
        public void setOrphanedEntries(java.util.Map map)
            {
            __m_OrphanedEntries = map;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$PartialRequestStatus

    /**
     * RequestStatus associated with a PartitionSet-based request.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PartialRequestStatus
            extends    com.tangosol.coherence.component.net.requestStatus.MultiPartStatus
        {
        // ---- Fields declarations ----

        /**
         * Property PartitionsByOwner
         *
         * Map<Member, PartitionSet> that contains PartitionSets split
         * (keyed) by the owner Member.
         */
        private java.util.Map __m_PartitionsByOwner;

        // Default constructor
        public PartialRequestStatus()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public PartialRequestStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new PartialRequestStatus();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$PartialRequestStatus".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Accessor for the property "PartitionsByOwner"
        /**
         * Getter for property PartitionsByOwner.<p>
         * Map<Member, PartitionSet> that contains PartitionSets split
         * (keyed) by the owner Member.
         */
        public java.util.Map getPartitionsByOwner()
            {
            return __m_PartitionsByOwner;
            }

        // Declared at the super level
        /**
         * Getter for property Service.<p>
         * The DistributedCache service component associated with this
         * request status.
         */
        public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService getService()
            {
            return (PartitionedCache) get_Module();
            }

        // Declared at the super level
        /**
         * Getter for property TargetMissing.<p>
         * (Calculated) Specifies whether the associated request is missing
         * any alive target (i.e. no storage enabled members).
         */
        public boolean isTargetMissing()
            {
            return getPartitionsByOwner() == null;
            }

        // Declared at the super level
        /**
         * Clear any state associated with this status.
         */
        public void reset()
            {
            setPartitionsByOwner(null);

            super.reset();
            }

        // Accessor for the property "PartitionsByOwner"
        /**
         * Setter for property PartitionsByOwner.<p>
         * Map<Member, PartitionSet> that contains PartitionSets split
         * (keyed) by the owner Member.
         */
        public void setPartitionsByOwner(java.util.Map map)
            {
            __m_PartitionsByOwner = map;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$BinaryMap$Values

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Values
            extends    com.tangosol.coherence.component.util.Collections
            implements java.util.Collection
        {
        // ---- Fields declarations ----

        // Default constructor
        public Values()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public Values(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
         * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new Values();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/BinaryMap$Values".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: java.util.Collection
        public boolean add(Object o)
            {
            throw new UnsupportedOperationException();
            }

        // From interface: java.util.Collection
        public boolean addAll(java.util.Collection col)
            {
            return addAll(this, col);
            }

        // From interface: java.util.Collection
        public void clear()
            {
            getBinaryMap().clear();
            }

        // From interface: java.util.Collection
        public boolean contains(Object o)
            {
            // import com.tangosol.util.Binary;

            return getBinaryMap().containsValue((Binary) o);
            }

        // From interface: java.util.Collection
        public boolean containsAll(java.util.Collection col)
            {
            return containsAll(this, col);
            }

        // Accessor for the property "BinaryMap"
        /**
         * Getter for property BinaryMap.<p>
         */
        public BinaryMap getBinaryMap()
            {
            return (BinaryMap) get_Parent();
            }

        // From interface: java.util.Collection
        public boolean isEmpty()
            {
            return getBinaryMap().isEmpty();
            }

        // From interface: java.util.Collection
        public java.util.Iterator iterator()
            {
            // import com.tangosol.util.PagedIterator;

            return new PagedIterator(getBinaryMap().makeEntryAdvancer(true, false));
            }

        // From interface: java.util.Collection
        public boolean remove(Object o)
            {
            return remove(this, o);
            }

        // From interface: java.util.Collection
        public boolean removeAll(java.util.Collection col)
            {
            return removeAll(this, col);
            }

        // From interface: java.util.Collection
        public boolean retainAll(java.util.Collection col)
            {
            return retainAll(this, col);
            }

        // From interface: java.util.Collection
        public int size()
            {
            return getBinaryMap().size();
            }

        // From interface: java.util.Collection
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        // From interface: java.util.Collection
        public Object[] toArray(Object[] ao)
            {
            // import java.util.ConcurrentModificationException;

            while (true)
                {
                try
                    {
                    return toArray(this, ao);
                    }
                catch (ConcurrentModificationException ignored) {}
                }
            }
        }
    }
