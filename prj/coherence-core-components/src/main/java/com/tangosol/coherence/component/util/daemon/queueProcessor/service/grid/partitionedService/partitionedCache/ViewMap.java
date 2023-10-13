/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

import com.tangosol.application.ContainerHelper;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.license.LicenseException;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceStoppedException;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.partition.DefaultVersionedPartitions;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.partition.VersionedPartitions;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.aggregator.AbstractAsynchronousAggregator;
import com.tangosol.util.comparator.EntryComparator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.PartitionedFilter;
import com.tangosol.util.processor.AbstractAsynchronousProcessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The client view of the distributed cache.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ViewMap
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.io.ClassLoaderAware,
                   com.tangosol.net.NamedCache
    {
    // ---- Fields declarations ----

    /**
     * Property Active
     *
     */
    private boolean __m_Active;

    /**
     * Property BinaryMap
     *
     * Underyling $BinaryMap reference.
     *
     * @volatile
     */
    private volatile BinaryMap __m_BinaryMap;

    /**
     * Property CacheName
     *
     */
    private String __m_CacheName;

    /**
     * Property ClassLoader
     *
     */
    private ClassLoader __m_ClassLoader;

    /**
     * Property ConverterMap
     *
     * The converting map.
     *
     * @see ensureConverterMap()
     */
    private transient com.tangosol.net.NamedCache __m_ConverterMap;

    /**
     * Property DeactivationListeners
     *
     * Registered NamedCacheDeactivationListeners.
     */
    private com.tangosol.util.Listeners __m_DeactivationListeners;

    /**
     * Property Destroyed
     *
     * True if it has been destroyed.
     */
    private boolean __m_Destroyed;

    /**
     * Property FromBinaryConverter
     *
     * A converter that takes Binary keys and values (from "below" the
     * view) and converts them via deserialization (etc.) to the objects
     * expected by clients of the ViewMap.
     */
    private transient com.tangosol.util.Converter __m_FromBinaryConverter;

    /**
     * Property KeyToBinaryConverter
     *
     * A converter that takes keys (from the "outside" of the view) and
     * converts them via serialization (etc.) to Binary objects.
     */
    private transient com.tangosol.util.Converter __m_KeyToBinaryConverter;

    /**
     * Property LicenseMsgTimestamp
     *
     * Last time the license message was logged.
     *
     * @see #reportMissingLicense
     */
    private static transient long __s_LicenseMsgTimestamp;

    /**
     * Property PassThrough
     *
     * A boolean flag indicating that this ViewMap is used by the
     * pass-through optimization and all the incoming and outgoing keys and
     * values are Binary objects.
     */
    private boolean __m_PassThrough;

    /**
     * Property Released
     *
     * True if it has been released.
     */
    private boolean __m_Released;

    /**
     * Property ValueToBinaryConverter
     *
     * A converter that takes value objects (from the "outside" of the
     * view) and converts them via serialization (etc.) to Binary objects.
     */
    private transient com.tangosol.util.Converter __m_ValueToBinaryConverter;

    // Default constructor
    public ViewMap()
        {
        this(null, null, true);
        }

    // Initializing constructor
    public ViewMap(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setActive(true);
            setDeactivationListeners(new com.tangosol.util.Listeners());
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
        return new ViewMap();
        }

    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
     * Property with auto-generated accessor that returns the Class object
     * for a given component.
     */
    public static Class get_CLASS()
        {
        return ViewMap.class;
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

    // From interface: com.tangosol.net.NamedCache
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        ensureBinaryMap().addIndex(extractor, fOrdered, comparator);
        }

    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;

        addMapListener(listener, (Filter) null, false);
        }

    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        // import com.tangosol.internal.net.NamedCacheDeactivationListener;
        // import com.tangosol.net.partition.DefaultVersionedPartitions;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.net.partition.VersionAwareMapListener as com.tangosol.net.partition.VersionAwareMapListener;
        // import com.tangosol.net.partition.VersionedPartitions;
        // import com.tangosol.net.partition.VersionedPartitions$Iterator as com.tangosol.net.partition.VersionedPartitions.Iterator;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
        // import com.tangosol.util.MapTrigger;
        // import com.tangosol.util.MapTriggerListener;
        // import com.tangosol.util.filter.InKeySetFilter;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Set;

        if (listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener
            && Thread.currentThread() == getService().getThread())
            {
            _trace("SynchronousListener cannot be added on the service thread:\n"
                   + get_StackTrace(), 1);
            return;
            }

        if (listener instanceof NamedCacheDeactivationListener)
            {
            getDeactivationListeners().add(listener);
            }
        else if (listener instanceof MapTriggerListener)
            {
            if (filter == null)
                {
                MapTrigger trigger = ((MapTriggerListener) listener).getTrigger();

                ensureBinaryMap().addMapListener(null, null, trigger, fLite, null);
                }
            else
                {
                throw new UnsupportedOperationException(
                        "Filter-based MapTriggers are not supported");
                }
            }
        else if (filter instanceof InKeySetFilter)
            {
            Set setBinKeys = retrieveBinaryKeys((InKeySetFilter) filter);
            boolean fPriming   = com.tangosol.util.MapListenerSupport.isPrimingListener(listener);

            VersionedPartitions versions = listener.isVersionAware()
                                           ? ((com.tangosol.net.partition.VersionAwareMapListener) listener).getVersions() : null;

            if (fPriming && versions == null)
                {
                // make all relevant partition versions return PRIMING
                PartitionedCache service          = getService();
                Set     setKeysConverted = new HashSet(setBinKeys.size());

                DefaultVersionedPartitions versionsPrimed = new DefaultVersionedPartitions();

                for (Iterator iter = setBinKeys.iterator(); iter.hasNext(); )
                    {
                    Binary binKey = (Binary) iter.next();
                    setKeysConverted.add(binKey);

                    int iPart = service.getKeyPartition(binKey);

                    versionsPrimed.setPartitionVersion(iPart, com.tangosol.net.partition.VersionAwareMapListener.PRIMING);
                    }
                versions = versionsPrimed;
                }

            ensureBinaryMap().addMapListener(
                    instantiateProxyListener(listener), setBinKeys, fLite, fPriming, versions);
            }
        else if (com.tangosol.util.MapListenerSupport.isPrimingListener(listener))
            {
            throw new UnsupportedOperationException(
                    "Priming listeners are only supported with InKeySetFilter");
            }
        else
            {
            VersionedPartitions versions = listener.isVersionAware()
                                           ? ((com.tangosol.net.partition.VersionAwareMapListener) listener).getVersions() : null;

            ensureBinaryMap().addMapListener(
                    instantiateProxyListener(listener), filter, null, fLite, versions);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        // import com.tangosol.net.partition.VersionAwareMapListener as com.tangosol.net.partition.VersionAwareMapListener;
        // import com.tangosol.net.partition.VersionedPartitions;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
        // import com.tangosol.util.MapTriggerListener;
        // import com.tangosol.util.PrimitiveSparseArray as com.tangosol.util.PrimitiveSparseArray;

        if (listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener
            && Thread.currentThread() == getService().getThread())
            {
            _trace("SynchronousListener cannot be added on the service thread:\n"
                   + get_StackTrace(), 1);
            return;
            }

        if (listener instanceof MapTriggerListener)
            {
            throw new UnsupportedOperationException(
                    "Key-based MapTriggers are not supported");
            }

        VersionedPartitions versions = listener.isVersionAware()
                                       ? ((com.tangosol.net.partition.VersionAwareMapListener) listener).getVersions() : null;

        Binary  binKey   = (Binary) getKeyToBinaryConverter().convert(oKey);
        boolean fPriming = com.tangosol.util.MapListenerSupport.isPrimingListener(listener);
        long    lVersion = versions == null
                           ? fPriming ? com.tangosol.net.partition.VersionAwareMapListener.PRIMING : com.tangosol.net.partition.VersionAwareMapListener.HEAD
                           : versions.getVersion(getService().getKeyPartition(binKey));

        ensureBinaryMap().addMapListener(
                instantiateProxyListener(listener), binKey, fLite, fPriming, versions != null, lVersion);
        }

    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.PriorityTask;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        // import com.tangosol.util.InvocableMap$ParallelAwareAggregator as com.tangosol.util.InvocableMap.ParallelAwareAggregator;
        // import com.tangosol.util.InvocableMap$StreamingAggregator as com.tangosol.util.InvocableMap.StreamingAggregator;
        // import com.tangosol.util.filter.KeyAssociatedFilter;
        // import com.tangosol.util.filter.LimitFilter;
        // import com.tangosol.util.filter.PartitionedFilter;
        // import com.tangosol.util.aggregator.AbstractAsynchronousAggregator;

        AbstractAsynchronousAggregator asyncAggr = null;

        if (agent instanceof AbstractAsynchronousAggregator)
            {
            asyncAggr = (AbstractAsynchronousAggregator) agent;
            agent     = asyncAggr.getAggregator();
            if (!(agent instanceof com.tangosol.util.InvocableMap.StreamingAggregator))
                {
                throw new UnsupportedOperationException("StreamingAggregator required");
                }
            }

        if (agent == null)
            {
            throw new IllegalArgumentException("Aggregator must be specified");
            }

        PartitionSet partitions = null;
        if (filter instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter filterAssoc = (KeyAssociatedFilter) filter;

            partitions = makePartitionSet(filterAssoc.getHostKey());
            filter     = filterAssoc.getFilter();
            }
        else if (filter instanceof PartitionedFilter)
            {
            PartitionedFilter filterPart = (PartitionedFilter) filter;
            PartitionSet      parts      = filterPart.getPartitionSet();

            validatePartitionCount(parts);

            partitions = new PartitionSet(parts);
            filter     = filterPart.getFilter();
            }

        if (filter instanceof LimitFilter)
            {
            throw new UnsupportedOperationException(
                    "LimitFilter cannot be used with aggregate");
            }

        prepareParallelQuery(filter);

        PriorityTask taskHolder = agent instanceof PriorityTask ? (PriorityTask) agent : null;

        try
            {
            if (agent instanceof com.tangosol.util.InvocableMap.StreamingAggregator)
                {
                com.tangosol.util.InvocableMap.StreamingAggregator aggregator = (com.tangosol.util.InvocableMap.StreamingAggregator) agent;

                return asyncAggr == null ?
                       aggregateStreaming(filter, aggregator, partitions, taskHolder) :
                       aggregateAsync    (filter, aggregator, partitions, taskHolder, asyncAggr);
                }

            if (agent instanceof com.tangosol.util.InvocableMap.ParallelAwareAggregator)
                {
                com.tangosol.util.InvocableMap.ParallelAwareAggregator aggregator = (com.tangosol.util.InvocableMap.ParallelAwareAggregator) agent;

                return resultParallel(aggregator,
                                      aggregatePart(filter, aggregator.getParallelAggregator(), partitions, taskHolder));
                }

            return agent.aggregate(com.tangosol.util.InvocableMapHelper.makeEntrySet(entrySet(filter)));
            }
        catch (RequestTimeoutException e)
            {
            throw processAggregateTimeout(e, agent);
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Aggregation", e);
            return agent.aggregate(com.tangosol.util.InvocableMapHelper.makeEntrySet(localEntrySet(filter)));
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.PriorityTask;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.InvocableMap$ParallelAwareAggregator as com.tangosol.util.InvocableMap.ParallelAwareAggregator;
        // import com.tangosol.util.InvocableMap$StreamingAggregator as com.tangosol.util.InvocableMap.StreamingAggregator;
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        // import com.tangosol.util.aggregator.AbstractAsynchronousAggregator;

        AbstractAsynchronousAggregator asyncAggr = null;

        if (agent instanceof AbstractAsynchronousAggregator)
            {
            asyncAggr = (AbstractAsynchronousAggregator) agent;
            agent     = asyncAggr.getAggregator();
            if (!(agent instanceof com.tangosol.util.InvocableMap.StreamingAggregator))
                {
                throw new UnsupportedOperationException("StreamingAggregator required");
                }
            }

        if (agent == null)
            {
            throw new IllegalArgumentException("Aggregator must be specified");
            }

        PriorityTask taskHolder = agent instanceof PriorityTask ? (PriorityTask) agent : null;

        try
            {
            if (agent instanceof com.tangosol.util.InvocableMap.StreamingAggregator)
                {
                com.tangosol.util.InvocableMap.StreamingAggregator aggregator = (com.tangosol.util.InvocableMap.StreamingAggregator) agent;

                return asyncAggr == null ?
                       aggregateStreaming(colKeys, aggregator, taskHolder) :
                       aggregateAsync    (colKeys, aggregator, taskHolder, asyncAggr);
                }

            if (agent instanceof com.tangosol.util.InvocableMap.ParallelAwareAggregator)
                {
                com.tangosol.util.InvocableMap.ParallelAwareAggregator aggregator = (com.tangosol.util.InvocableMap.ParallelAwareAggregator) agent;

                return resultParallel(aggregator,
                                      aggregatePart(colKeys, aggregator.getParallelAggregator(), taskHolder));
                }
            }
        catch (RequestTimeoutException e)
            {
            throw processAggregateTimeout(e, agent);
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Aggregation", e);

            // fall through
            }

        return agent.aggregate(com.tangosol.util.InvocableMapHelper.makeEntrySet(getAll(colKeys), colKeys, true));
        }

    protected java.util.List aggregateAsync(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator aggregator, com.tangosol.net.partition.PartitionSet partitions, com.tangosol.net.PriorityTask taskHolder, com.tangosol.util.aggregator.AbstractAsynchronousAggregator asyncAggr)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;

        Binary    binAgent;
        Converter convUp;
        Converter convDown;

        if (isPassThrough())
            {
            // this is a pass through call; need to serialize the agent,
            // deserialize partial results and re-serialize the result
            // using the service's classloader
            PartitionedCache.BackingMapContext context = getService().getBackingMapContext();

            convUp   = context.getValueFromInternalConverter();
            convDown = context.getValueToInternalConverter();
            }
        else
            {
            convUp   = getFromBinaryConverter();
            convDown = getValueToBinaryConverter();
            }

        if (partitions == null)
            {
            partitions = getService().instantiatePartitionSet(/*fFill*/ true);
            }

        ensureBinaryMap().aggregateAsync(filter, (Binary) convDown.convert(aggregator),
                                         partitions, taskHolder, asyncAggr, convUp);

        return null;
        }

    protected java.util.List aggregateAsync(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryAggregator aggregator, com.tangosol.net.PriorityTask taskHolder, com.tangosol.util.aggregator.AbstractAsynchronousAggregator asyncAggr)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import java.util.Collection;
        // import java.util.List;
        // import java.util.Set;

        Converter convKeyDown = getKeyToBinaryConverter();
        Converter convValDown = getValueToBinaryConverter();
        Converter convUp      = getFromBinaryConverter();

        Collection colBinKeys = colKeys instanceof Set
                                ? ConverterCollections.getSet((Set) colKeys, convKeyDown, convUp)
                                : ConverterCollections.getCollection(colKeys, convKeyDown, convUp);

        if (isPassThrough())
            {
            // this is a pass through call; need to deserialize partial results
            // and re-serialize the result
            PartitionedCache.BackingMapContext context = getService().getBackingMapContext();

            convValDown = context.getValueToInternalConverter();
            convUp      = context.getValueFromInternalConverter();
            }

        Binary binAgent = (Binary) convValDown.convert(aggregator);

        ensureBinaryMap().aggregateAsync(colBinKeys, binAgent, taskHolder, asyncAggr, convUp);

        return null;
        }

    protected java.util.List aggregatePart(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator aggregator, com.tangosol.net.partition.PartitionSet partitions, com.tangosol.net.PriorityTask taskHolder)
        {
        // import com.tangosol.util.Binary;
        // import java.util.List;

        BinaryMap mapBinary = ensureBinaryMap();

        // if this is a pass through call, need to serialize the agent,
        // deserialize partial results and re-serialize the result using
        // the service's classloader
        Binary binAgent = isPassThrough()
                          ? (Binary) getService().getBackingMapContext().getValueToInternalConverter().convert(aggregator)
                          : (Binary) getValueToBinaryConverter().convert(aggregator);

        return partitions == null
               ? (List) mapBinary.aggregate(filter, binAgent, taskHolder)
               : (List) mapBinary.aggregate(filter, binAgent, partitions, taskHolder);
        }

    protected java.util.List aggregatePart(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryAggregator aggregator, com.tangosol.net.PriorityTask taskHolder)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import java.util.Collection;
        // import java.util.List;
        // import java.util.Set;

        Converter convKeyDown = getKeyToBinaryConverter();
        Converter convValDown = getValueToBinaryConverter();
        Converter convUp      = getFromBinaryConverter();

        Collection colBinKeys = colKeys instanceof Set
                                ? ConverterCollections.getSet((Set) colKeys, convKeyDown, convUp)
                                : ConverterCollections.getCollection(colKeys, convKeyDown, convUp);

        if (isPassThrough())
            {
            // this is a pass through call; need to deserialize partial results
            // and re-serialize the result
            PartitionedCache.BackingMapContext context = getService().getBackingMapContext();

            convValDown = context.getValueToInternalConverter();
            convUp      = context.getValueFromInternalConverter();
            }

        BinaryMap mapBinary = ensureBinaryMap();
        Binary     binAgent  = (Binary) convValDown.convert(aggregator);

        return (List) mapBinary.aggregate(colBinKeys, binAgent, taskHolder);
        }

    protected Object aggregateStreaming(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.StreamingAggregator aggregator, com.tangosol.net.partition.PartitionSet partitions, com.tangosol.net.PriorityTask taskHolder)
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.InvocableMap$StreamingAggregator as com.tangosol.util.InvocableMap.StreamingAggregator;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;

        if (partitions == null)
            {
            partitions = getService().instantiatePartitionSet(/*fFill*/ true);
            }

        switch (aggregator.characteristics()
                & (com.tangosol.util.InvocableMap.StreamingAggregator.PARALLEL | com.tangosol.util.InvocableMap.StreamingAggregator.SERIAL | com.tangosol.util.InvocableMap.StreamingAggregator.BY_MEMBER | com.tangosol.util.InvocableMap.StreamingAggregator.BY_PARTITION))
            {
            case (com.tangosol.util.InvocableMap.StreamingAggregator.SERIAL):
                {
                Map mapByOwner = getService().splitByOwner(partitions, 0, null);

                aggregator = aggregator.supply();

                for (Iterator iter = mapByOwner.values().iterator(); iter.hasNext();)
                    {
                    PartitionSet partMember = (PartitionSet) iter.next();

                    if (!streamingCombine(aggregator,
                                          aggregatePart(filter, aggregator, partMember, taskHolder)))
                        {
                        break;
                        }
                    }
                }
            break;

            case (com.tangosol.util.InvocableMap.StreamingAggregator.PARALLEL):
            default:
                {
                List listBinParts = aggregatePart(filter, aggregator, partitions, taskHolder);

                aggregator = aggregator.supply();

                streamingCombine(aggregator, listBinParts);
                }
            }

        return resultStreaming(aggregator);
        }

    protected Object aggregateStreaming(java.util.Collection colKeys, com.tangosol.util.InvocableMap.StreamingAggregator aggregator, com.tangosol.net.PriorityTask taskHolder)
        {
        // import com.tangosol.util.InvocableMap$StreamingAggregator as com.tangosol.util.InvocableMap.StreamingAggregator;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Set;

        switch (aggregator.characteristics()
                & (com.tangosol.util.InvocableMap.StreamingAggregator.PARALLEL | com.tangosol.util.InvocableMap.StreamingAggregator.SERIAL | com.tangosol.util.InvocableMap.StreamingAggregator.BY_MEMBER | com.tangosol.util.InvocableMap.StreamingAggregator.BY_PARTITION))
            {
            case (com.tangosol.util.InvocableMap.StreamingAggregator.SERIAL):
            case (com.tangosol.util.InvocableMap.StreamingAggregator.SERIAL | com.tangosol.util.InvocableMap.StreamingAggregator.BY_MEMBER):
            {
            Map mapByOwner = splitKeysByOwner(colKeys.iterator());

            aggregator = aggregator.supply();

            for (Iterator iter = mapByOwner.values().iterator(); iter.hasNext();)
                {
                Set setMember = (Set) iter.next();

                if (!streamingCombine(aggregator,
                                      aggregatePart(setMember, aggregator, taskHolder)))
                    {
                    break;
                    }
                }
            }
            break;

            case (com.tangosol.util.InvocableMap.StreamingAggregator.SERIAL | com.tangosol.util.InvocableMap.StreamingAggregator.BY_PARTITION):
            {
            Map mapByPartition = splitKeysByPartition(colKeys.iterator());

            aggregator = aggregator.supply();

            for (Iterator iter = mapByPartition.values().iterator(); iter.hasNext();)
                {
                Set setPart = (Set) iter.next();

                if (!streamingCombine(aggregator,
                                      aggregatePart(setPart, aggregator, taskHolder)))
                    {
                    break;
                    }
                }
            }
            break;

            case (com.tangosol.util.InvocableMap.StreamingAggregator.PARALLEL):
            default:
            {
            List listBinParts = aggregatePart(colKeys, aggregator, taskHolder);

            aggregator = aggregator.supply();

            streamingCombine(aggregator, listBinParts);
            }
            }

        return resultStreaming(aggregator);
        }

    // From interface: com.tangosol.net.NamedCache
    public void clear()
        {
        ensureBinaryMap().clear();
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean containsKey(Object oKey)
        {
        return ensureConverterMap().containsKey(oKey);
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean containsValue(Object oValue)
        {
        return ensureConverterMap().containsValue(oValue);
        }

    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        getCacheService().destroyCache(this);
        setDestroyed(true);
        }

    protected BinaryMap ensureBinaryMap()
        {
        BinaryMap map = getBinaryMap();
        if (map == null)
            {
            throw onInvalidAccess();
            }

        return map;
        }

    protected com.tangosol.net.NamedCache ensureConverterMap()
        {
        // import com.tangosol.util.ConverterCollections as com.tangosol.util.ConverterCollections;
        // import com.tangosol.net.NamedCache;

        NamedCache map = getConverterMap();
        if (map == null)
            {
            synchronized (this)
                {
                if ((map = getConverterMap()) == null)
                    {
                    map = com.tangosol.util.ConverterCollections.getNamedCache(ensureBinaryMap(),
                                                                               getFromBinaryConverter(), getKeyToBinaryConverter(),
                                                                               getFromBinaryConverter(), getValueToBinaryConverter());
                    setConverterMap(map);
                    }
                }
            }

        return map;
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet()
        {
        return ensureConverterMap().entrySet();
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.filter.KeyAssociatedFilter;
        // import com.tangosol.util.filter.LimitFilter;
        // import com.tangosol.util.filter.PartitionedFilter;

        if (filter == null)
            {
            return entrySet();
            }

        PartitionSet partitions = null;
        Filter       filterOrig = filter;
        if (filter instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter filterAssoc = (KeyAssociatedFilter) filter;

            partitions = makePartitionSet(filterAssoc.getHostKey());
            filter     = filterAssoc.getFilter();
            }
        else if (filter instanceof PartitionedFilter)
            {
            PartitionedFilter filterPart = (PartitionedFilter) filter;
            PartitionSet      parts      = filterPart.getPartitionSet();

            validatePartitionCount(parts);

            partitions = new PartitionSet(parts);
            filter     = filterPart.getFilter();
            }

        if (filter instanceof LimitFilter)
            {
            // LimitFilter for a non-sorted entrySet doesn't use anchors
            LimitFilter filterLimit = (LimitFilter) filter;

            filterLimit.setComparator  (null);
            filterLimit.setTopAnchor   (null);
            filterLimit.setBottomAnchor(null);
            }

        prepareParallelQuery(filter);

        try
            {
            if (partitions == null)
                {
                return ensureConverterMap().entrySet(filter);
                }
            else
                {
                Converter convUp = getFromBinaryConverter();

                return ConverterCollections.getEntrySet(
                        ensureBinaryMap().entrySet(filter, partitions),
                        convUp, getKeyToBinaryConverter(),
                        convUp, getValueToBinaryConverter());
                }
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Query", e);

            // parallel-query failed; revert to a client-driven "local"
            // (non-partition-masked) execution of the original filter
            return localEntrySet(filterOrig);
            }
        catch (RequestTimeoutException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_SET);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.comparator.EntryComparator;
        // import com.tangosol.util.comparator.SafeComparator;
        // import com.tangosol.util.filter.KeyAssociatedFilter;
        // import com.tangosol.util.filter.LimitFilter;
        // import com.tangosol.util.filter.PartitionedFilter;
        // import java.util.Arrays;
        // import java.util.Comparator;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        PartitionedCache      service    = getService();
        PartitionSet partitions = null;
        Filter       filterOrig = filter;
        if (filter instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter filterAssoc = (KeyAssociatedFilter) filter;

            partitions = makePartitionSet(filterAssoc.getHostKey());
            filter     = filterAssoc.getFilter();
            }
        else if (filter instanceof PartitionedFilter)
            {
            PartitionedFilter filterPart = (PartitionedFilter) filter;
            PartitionSet      parts      = filterPart.getPartitionSet();

            validatePartitionCount(parts);

            partitions = new PartitionSet(parts);
            filter     = filterPart.getFilter();
            }

        LimitFilter filterLimitOrig = null;
        int         cPageSize       = 0;
        int         cSkip           = 0;
        Object      oAnchorTop      = null;
        Object      oAnchorBottom   = null;
        int         nPage           = 0;
        if (filter instanceof LimitFilter)
            {
            filterLimitOrig = (LimitFilter) filter;
            if (comparator == null)
                {
                comparator = SafeComparator.INSTANCE;
                }
            filterLimitOrig.setComparator(comparator);

            cPageSize = filterLimitOrig.getPageSize();

            filter = (LimitFilter) filterLimitOrig.clone();
            nPage  = filterLimitOrig.getPage();

            if (nPage == 0)
                {
                filterLimitOrig.setTopAnchor   (null);
                filterLimitOrig.setBottomAnchor(null);
                }
            else
                {
                oAnchorTop    = filterLimitOrig.getTopAnchor();
                oAnchorBottom = filterLimitOrig.getBottomAnchor();

                // if there is no anchor we need to get all the pages
                // up to the specified one (inclusive)
                if (oAnchorTop == null && oAnchorBottom == null)
                    {
                    cSkip = cPageSize*nPage;
                    ((LimitFilter) filter).setPage(0);
                    ((LimitFilter) filter).setPageSize(cSkip + cPageSize);
                    }
                }
            }

        prepareParallelQuery(filter);

        Object[]        aEntryCur       = null;
        Object[]        aEntry          = null;
        int             cPageTotal      = cSkip + cPageSize;
        EntryComparator comparatorEntry = new EntryComparator(comparator);
        if (filter instanceof LimitFilter)
            {
            if (isPassThrough())
                {
                int cMembers = service.getOwnershipMemberSet().size();
                ((LimitFilter) filter).setBatchSize(cMembers);
                }
            else
                {
                partitions = service.instantiatePartitionSet(/*fFill*/ true);
                filterLimitOrig.setComparator(comparatorEntry);
                if (cSkip > 0)
                    {
                    filterLimitOrig.setPageSize(cPageTotal);
                    filterLimitOrig.setPage(0);
                    }
                }
            }

        Set setEntries;
        try
            {
            if (partitions == null)
                {
                setEntries = ensureConverterMap().entrySet(filter, comparator);
                aEntry     = setEntries.toArray();
                }
            else
                {
                while (!partitions.isEmpty())
                    {
                    Converter convUp = getFromBinaryConverter();

                    setEntries = ConverterCollections.getEntrySet(
                            ensureBinaryMap().entrySet(filter, comparator, partitions),
                            convUp, getKeyToBinaryConverter(),
                            convUp, getValueToBinaryConverter());

                    aEntry = setEntries.toArray();

                    int cEntries = setEntries.size();

                    if (filter instanceof LimitFilter && aEntryCur != null)
                        {
                        Object[] ao = new Object[aEntryCur.length + aEntry.length];
                        System.arraycopy(aEntry, 0, ao, 0, aEntry.length);
                        System.arraycopy(aEntryCur, 0, ao, aEntry.length, aEntryCur.length);
                        aEntry = ao;
                        }

                    if (partitions.isEmpty())
                        {
                        break;
                        }

                    // must be limitFilter
                    Arrays.sort(aEntry, comparatorEntry);

                    aEntry    = filterLimitOrig.extractPage(aEntry);
                    aEntryCur = aEntry;

                    if (cEntries >= cPageTotal && aEntry.length >= cPageTotal)
                        {
                        if (oAnchorTop != null)
                            {
                            ((LimitFilter) filter).setBottomAnchor(((java.util.Map.Entry) aEntry[aEntry.length -1]).getValue());
                            }
                        else if (oAnchorBottom != null)
                            {
                            ((LimitFilter) filter).setTopAnchor(((java.util.Map.Entry) aEntry[0]).getValue());
                            }
                        }
                    }
                }
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Query", e);

            // parallel-query failed; revert to a client-driven "local"
            // (non-partition-masked) execution of the original filter
            return localEntrySet(filterOrig, comparator);
            }
        catch (RequestTimeoutException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_VOID);
            }

        if (!isPassThrough()) // COH-2717
            {
            Arrays.sort(aEntry, comparatorEntry);

            // process the final query result for limitFilter
            if (filterLimitOrig != null)
                {
                filterLimitOrig.setPageSize(cPageSize);
                int cEntries;
                if (cSkip > 0)
                    {
                    // there were no anchors and the result is already sorted;
                    // just truncate the extras
                    cEntries = Math.min(Math.max(0, aEntry.length - cSkip), cPageSize);
                    if (cEntries > 0)
                        {
                        Object[] ao = new Object[cEntries];
                        System.arraycopy(aEntry, cSkip, ao, 0, cEntries);
                        aEntry = ao;
                        }
                    else
                        {
                        aEntry = new Object[0];
                        }
                    }
                else
                    {
                    aEntry   = filterLimitOrig.extractPage(aEntry);
                    cEntries = aEntry.length;
                    }

                if (cEntries > 0)
                    {
                    filterLimitOrig.setTopAnchor(((java.util.Map.Entry) aEntry[0]).getValue());
                    filterLimitOrig.setBottomAnchor(((java.util.Map.Entry) aEntry[cEntries-1]).getValue());
                    }

                // reset to original
                filterLimitOrig.setPage(nPage);
                filterLimitOrig.setPageSize(cPageSize);
                filterLimitOrig.setComparator(comparator);
                }
            }

        return new ImmutableArrayList(aEntry);
        }

    // From interface: com.tangosol.net.NamedCache
    public Object get(Object oKey)
        {
        return ensureConverterMap().get(oKey);
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        // import com.tangosol.net.RequestTimeoutException;

        try
            {
            return ensureConverterMap().getAll(colKeys);
            }
        catch (RequestTimeoutException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_MAP);
            }
        }

    // Accessor for the property "BinaryMap"
    /**
     * Getter for property BinaryMap.<p>
     * Underyling $BinaryMap reference.
     *
     * @volatile
     */
    public BinaryMap getBinaryMap()
        {
        return __m_BinaryMap;
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
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

    // Accessor for the property "ClassLoader"
    /**
     * Getter for property ClassLoader.<p>
     */
    public ClassLoader getClassLoader()
        {
        return __m_ClassLoader;
        }

    // From interface: com.tangosol.io.ClassLoaderAware
    public ClassLoader getContextClassLoader()
        {
        return getClassLoader();
        }

    // Accessor for the property "ConverterMap"
    /**
     * Getter for property ConverterMap.<p>
     * The converting map.
     *
     * @see ensureConverterMap()
     */
    protected com.tangosol.net.NamedCache getConverterMap()
        {
        return __m_ConverterMap;
        }

    // Accessor for the property "DeactivationListeners"
    /**
     * Getter for property DeactivationListeners.<p>
     * Registered NamedCacheDeactivationListeners.
     */
    public com.tangosol.util.Listeners getDeactivationListeners()
        {
        return __m_DeactivationListeners;
        }

    // Accessor for the property "FromBinaryConverter"
    /**
     * Getter for property FromBinaryConverter.<p>
     * A converter that takes Binary keys and values (from "below" the view)
     * and converts them via deserialization (etc.) to the objects expected
     * by clients of the ViewMap.
     */
    public com.tangosol.util.Converter getFromBinaryConverter()
        {
        return __m_FromBinaryConverter;
        }

    // Accessor for the property "KeyToBinaryConverter"
    /**
     * Getter for property KeyToBinaryConverter.<p>
     * A converter that takes keys (from the "outside" of the view) and
     * converts them via serialization (etc.) to Binary objects.
     */
    public com.tangosol.util.Converter getKeyToBinaryConverter()
        {
        return __m_KeyToBinaryConverter;
        }

    // Accessor for the property "LicenseMsgTimestamp"
    /**
     * Getter for property LicenseMsgTimestamp.<p>
     * Last time the license message was logged.
     *
     * @see #reportMissingLicense
     */
    public static long getLicenseMsgTimestamp()
        {
        return __s_LicenseMsgTimestamp;
        }

    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
     */
    public PartitionedCache getService()
        {
        return (PartitionedCache) get_Module();
        }

    // Accessor for the property "ValueToBinaryConverter"
    /**
     * Getter for property ValueToBinaryConverter.<p>
     * A converter that takes value objects (from the "outside" of the view)
     * and converts them via serialization (etc.) to Binary objects.
     */
    public com.tangosol.util.Converter getValueToBinaryConverter()
        {
        return __m_ValueToBinaryConverter;
        }

    protected com.tangosol.util.MapListener instantiateProxyListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.application.ContainerHelper;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;

        Converter conv = getFromBinaryConverter();

        return ContainerHelper.getContextAwareListener(getService(),
                                                       ConverterCollections.getMapListener(this, listener, conv, conv));
        }

    /**
     * Could be called on the service thread during service shutdown.
     */
    public void invalidate(boolean fDestroyed)
        {
        // import Component.Util.CacheEvent as com.tangosol.coherence.component.util.CacheEvent;
        // import com.tangosol.net.cache.CacheEvent;
        // import com.tangosol.util.Listeners;

        synchronized (this)
            {
            if (!isActive())
                {
                return;
                }

            setDestroyed(fDestroyed);
            setActive(false);
            setBinaryMap(null);
            setConverterMap(null);
            setClassLoader(null);
            setFromBinaryConverter(null);
            setKeyToBinaryConverter(null);
            setValueToBinaryConverter(null);
            }

        Listeners listeners = getDeactivationListeners();
        if (!listeners.isEmpty())
            {
            CacheEvent evt = new CacheEvent(this, CacheEvent.ENTRY_DELETED, null, null, null, true);
            // dispatch the event to the listeners, which are all synchronous (hence the null Queue)
            com.tangosol.coherence.component.util.CacheEvent.dispatchSafe(evt, listeners, null /*Queue*/);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.PriorityTask;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        // import com.tangosol.util.processor.AbstractAsynchronousProcessor;

        AbstractAsynchronousProcessor asyncProc = null;

        if (agent instanceof AbstractAsynchronousProcessor)
            {
            asyncProc = (AbstractAsynchronousProcessor) agent;
            agent     = asyncProc.getProcessor();
            }

        if (agent == null)
            {
            throw new IllegalArgumentException("Processor must be specified");
            }

        PriorityTask taskHolder = agent instanceof PriorityTask ? (PriorityTask) agent : null;

        try
            {
            Converter convKeyDown = getKeyToBinaryConverter();
            Converter convUp      = getFromBinaryConverter();

            // if any of the converters are null, the following line will throw
            BinaryMap mapBinary = ensureBinaryMap();

            Binary binAgent = toBinary(agent);
            Binary binKey   = (Binary) convKeyDown.convert(oKey);

            if (asyncProc == null)
                {
                return convUp.convert(mapBinary.invoke(binKey, binAgent, taskHolder));
                }
            else
                {
                mapBinary.invokeAsync(binKey, binAgent, taskHolder, asyncProc, convUp);
                return null;
                }
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Invocation", e);

            return com.tangosol.util.InvocableMapHelper.invokeLocked(this, com.tangosol.util.InvocableMapHelper.makeEntry(this, oKey), agent);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.PriorityTask;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.filter.KeyAssociatedFilter;
        // import com.tangosol.util.filter.LimitFilter;
        // import com.tangosol.util.filter.PartitionedFilter;
        // import com.tangosol.util.processor.AbstractAsynchronousProcessor;
        // import java.util.Set;

        AbstractAsynchronousProcessor asyncProc = null;

        if (agent instanceof AbstractAsynchronousProcessor)
            {
            asyncProc = (AbstractAsynchronousProcessor) agent;
            agent     = asyncProc.getProcessor();
            }

        if (agent == null)
            {
            throw new IllegalArgumentException("Processor must be specified");
            }

        PartitionSet partitions;
        if (filter instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter filterAssoc = (KeyAssociatedFilter) filter;

            partitions = makePartitionSet(filterAssoc.getHostKey());
            filter     = filterAssoc.getFilter();
            }
        else if (filter instanceof PartitionedFilter)
            {
            PartitionedFilter filterPart = (PartitionedFilter) filter;

            partitions = new PartitionSet(filterPart.getPartitionSet());
            filter     = filterPart.getFilter();
            }
        else
            {
            partitions = getService().instantiatePartitionSet(true);
            }

        if (filter instanceof LimitFilter)
            {
            throw new UnsupportedOperationException(
                    "LimitFilter cannot be used with invokeAll");
            }

        prepareParallelQuery(filter);

        Converter convUp      = getFromBinaryConverter();
        Converter convKeyDown = getKeyToBinaryConverter();

        PriorityTask taskHolder = agent instanceof PriorityTask ? (PriorityTask) agent : null;
        try
            {
            Binary binAgent = toBinary(agent);

            // if any of the converters are null, the following line will throw
            BinaryMap mapBinary = ensureBinaryMap();

            if (asyncProc == null)
                {
                return ConverterCollections.getMap(
                        mapBinary.invokeAll(filter, binAgent, partitions, taskHolder),
                        convUp, convKeyDown, convUp, getValueToBinaryConverter());
                }
            else
                {
                mapBinary.invokeAllAsync(filter, binAgent, partitions, taskHolder, asyncProc, convUp);
                return null;
                }
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Invocation", e);

            return com.tangosol.util.InvocableMapHelper.invokeAllLocked(this,
                                                                        com.tangosol.util.InvocableMapHelper.makeEntrySet(this, localKeySet(filter), false), agent);
            }
        catch (RequestTimeoutException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_MAP);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.PriorityTask;
        // import com.tangosol.net.RequestIncompleteException;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        // import com.tangosol.util.processor.AbstractAsynchronousProcessor;
        // import java.util.Collection;
        // import java.util.Set;

        AbstractAsynchronousProcessor asyncProc = null;

        if (agent instanceof AbstractAsynchronousProcessor)
            {
            asyncProc = (AbstractAsynchronousProcessor) agent;
            agent     = asyncProc.getProcessor();
            }

        if (agent == null)
            {
            throw new IllegalArgumentException("Processor must be specified");
            }

        PriorityTask taskHolder = agent instanceof PriorityTask ? (PriorityTask) agent : null;

        try
            {
            Binary     binAgent    = toBinary(agent);
            Converter  convKeyDown = getKeyToBinaryConverter();
            Converter  convUp      = getFromBinaryConverter();

            // if any of the converters are null, the following line will throw
            BinaryMap mapBinary = ensureBinaryMap();

            Collection colBinKeys  = colKeys instanceof Set ?
                                     ConverterCollections.getSet((Set) colKeys, convKeyDown, convUp) :
                                     ConverterCollections.getCollection(colKeys, convKeyDown, convUp);

            if (asyncProc == null)
                {
                return ConverterCollections.getMap(
                        mapBinary.invokeAll(colBinKeys, binAgent, taskHolder),
                        convUp, convKeyDown,
                        convUp, getValueToBinaryConverter());
                }
            else
                {
                mapBinary.invokeAllAsync(colBinKeys, binAgent, taskHolder, asyncProc, convUp);
                return null;
                }
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Invocation", e);

            return com.tangosol.util.InvocableMapHelper.invokeAllLocked(this,
                                                                        com.tangosol.util.InvocableMapHelper.makeEntrySet(this, colKeys, false), agent);
            }
        catch (RequestTimeoutException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_MAP);
            }
        catch (RequestIncompleteException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_SET);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Active"
    /**
     * Getter for property Active.<p>
     */
    public boolean isActive()
        {
        return __m_Active;
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Destroyed"
    /**
     * Getter for property Destroyed.<p>
     * True if it has been destroyed.
     */
    public boolean isDestroyed()
        {
        // the trivial script is needed to work around a TDE issue
        // regarding a default interface method with same name.
        return __m_Destroyed;
        }

    public boolean isReady()
        {
        if (isActive())
            {
            BinaryMap mapBinary = getBinaryMap();
            if (mapBinary != null)
                {
                return mapBinary.isReady();
                }
            }
        return false;
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean isEmpty()
        {
        return ensureConverterMap().isEmpty();
        }

    // Accessor for the property "PassThrough"
    /**
     * Getter for property PassThrough.<p>
     * A boolean flag indicating that this ViewMap is used by the
     * pass-through optimization and all the incoming and outgoing keys and
     * values are Binary objects.
     */
    public boolean isPassThrough()
        {
        return __m_PassThrough;
        }

    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Released"
    /**
     * Getter for property Released.<p>
     * True if it has been released.
     */
    public boolean isReleased()
        {
        // the trivial script is needed to work around a TDE issue
        // regarding a default interface method with same name.
        return __m_Released;
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet()
        {
        return ensureConverterMap().keySet();
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.filter.KeyAssociatedFilter;
        // import com.tangosol.util.filter.LimitFilter;
        // import com.tangosol.util.filter.PartitionedFilter;

        if (filter == null)
            {
            return keySet();
            }

        PartitionSet partitions = null;
        Filter       filterOrig = filter;
        if (filter instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter filterAssoc = (KeyAssociatedFilter) filter;

            partitions = makePartitionSet(filterAssoc.getHostKey());
            filter     = filterAssoc.getFilter();
            }

        if (filter instanceof LimitFilter)
            {
            // LimitFilter for a [non-sorted] keySet doesn't use anchors
            LimitFilter filterLimit = (LimitFilter) filter;

            filterLimit.setComparator  (null);
            filterLimit.setTopAnchor   (null);
            filterLimit.setBottomAnchor(null);
            }
        else if (filter instanceof PartitionedFilter)
            {
            PartitionedFilter filterPart = (PartitionedFilter) filter;
            PartitionSet      parts      = filterPart.getPartitionSet();

            validatePartitionCount(parts);

            partitions = new PartitionSet(parts);
            filter     = filterPart.getFilter();
            }

        prepareParallelQuery(filter);

        try
            {
            return partitions == null ? ensureConverterMap().keySet(filter) :
                   ConverterCollections.getSet(ensureBinaryMap().keySet(filter, partitions),
                                               getFromBinaryConverter(), getKeyToBinaryConverter());
            }
        catch (LicenseException e)
            {
            reportMissingLicense("Query", e);

            // parallel-query failed; revert to a client-driven "local"
            // (non-partition-masked) execution of the original filter
            return localKeySet(filterOrig);
            }
        catch (RequestTimeoutException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_SET);
            }
        }

    protected java.util.Set localEntrySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.InvocableMapHelper;

        return InvocableMapHelper.query(this, filter, true, false, null);
        }

    protected java.util.Set localEntrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        // import com.tangosol.util.InvocableMapHelper;

        return InvocableMapHelper.query(this, filter, true, true, comparator);
        }

    protected java.util.Set localKeySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.InvocableMapHelper;

        return InvocableMapHelper.query(this, filter, false, false, null);
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey)
        {
        return ensureConverterMap().lock(oKey);
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey, long lMillis)
        {
        return ensureConverterMap().lock(oKey, lMillis);
        }

    /**
     * Instantiate a new PartitionSet containing a partition that the
     * specified key belongs to.
     */
    protected com.tangosol.net.partition.PartitionSet makePartitionSet(Object oKey)
        {
        return getService().getKeyPartitioningStrategy().getAssociatedPartitions(oKey);
        }

    protected RuntimeException onInvalidAccess()
        {
        // import com.tangosol.net.ServiceStoppedException;

        PartitionedCache service = getService();
        if (service.isRunning())
            {
            return new IllegalStateException("The distributed cache reference \""
                                             + getCacheName() + "\" has been invalidated; no further operations are allowed.");
            }
        else
            {
            return new ServiceStoppedException("Service " + service.getServiceName() + " has been terminated");
            }
        }

    /**
     * Make sure that the specified filter is ready to be sent to the
     * parallel query execuion.
     */
    protected void prepareParallelQuery(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.filter.InKeySetFilter;
        // import com.tangosol.util.filter.LimitFilter;

        // currently the only filter that needs "massaging" is the InKeySetFilter,
        // since parallel queries use the keys in Binary form for both individual and index evaluation

        Filter filterTest = filter instanceof LimitFilter ?
                            ((LimitFilter) filter).getFilter() : filter;

        if (filterTest instanceof InKeySetFilter)
            {
            // regardless whether this is a pass-through or not, we need to use a "real" converter
            ((InKeySetFilter) filterTest).ensureConverted(
                    getService().getBackingMapContext().getKeyToInternalConverter());
            }
        }

    /**
     * Process a RequestTimeoutException for the aggregate() call.
     */
    protected com.tangosol.net.RequestTimeoutException processAggregateTimeout(com.tangosol.net.RequestTimeoutException eTimeout, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        // import com.tangosol.util.InvocableMap$ParallelAwareAggregator as com.tangosol.util.InvocableMap.ParallelAwareAggregator;
        // import com.tangosol.util.InvocableMap$StreamingAggregator as com.tangosol.util.InvocableMap.StreamingAggregator;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.NullImplementation;
        // import java.util.Iterator;
        // import java.util.List;

        if (isPassThrough())
            {
            eTimeout.setPartialResult(null);
            return eTimeout;
            }

        Object oResult = eTimeout.getPartialResult();
        if (oResult != null)
            {
            try
                {
                if (agent instanceof com.tangosol.util.InvocableMap.StreamingAggregator)
                    {
                    com.tangosol.util.InvocableMap.StreamingAggregator aggregator = (com.tangosol.util.InvocableMap.StreamingAggregator) agent;
                    Converter           convUp     = getFromBinaryConverter();

                    for (Iterator iter = ((List) oResult).iterator(); iter.hasNext();)
                        {
                        if (!aggregator.combine(convUp.convert(iter.next())))
                            {
                            break;
                            }
                        }
                    oResult = aggregator.finalizeResult();
                    }
                else
                    {
                    com.tangosol.util.InvocableMap.ParallelAwareAggregator aggregator = (com.tangosol.util.InvocableMap.ParallelAwareAggregator) agent;

                    oResult = aggregator.aggregateResults(
                            ConverterCollections.getCollection((List) oResult,
                                                               getFromBinaryConverter(), NullImplementation.getConverter()));
                    }
                }
            catch (Throwable e)
                {
                oResult = null;
                }
            }

        eTimeout.setPartialResult(oResult);
        return eTimeout;
        }

    /**
     * Process a RequestTimeoutException for a request.
     */
    protected com.tangosol.net.RequestIncompleteException processRequestIncomplete(com.tangosol.net.RequestIncompleteException exception, int nResponseType)
        {
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.NullImplementation;
        // import java.util.Map;
        // import java.util.Set;

        if (isPassThrough())
            {
            exception.setPartialResult(null);
            return exception;
            }

        Object oResult = exception.getPartialResult();
        if (oResult != null)
            {
            try
                {
                Converter convKeyDown = getKeyToBinaryConverter();
                Converter convUp      = getFromBinaryConverter();
                Converter convNotUsed = NullImplementation.getConverter();

                switch (nResponseType)
                    {
                    case BinaryMap.RESPONSE_SET:
                        oResult = ConverterCollections.getSet((Set) oResult,
                                                              convUp, convNotUsed);
                        break;

                    case BinaryMap.RESPONSE_MAP:
                        oResult = ConverterCollections.getMap((Map) oResult,
                                                              convUp, convKeyDown, convUp, convNotUsed);
                        break;

                    default:
                        oResult = null;
                        break;
                    }
                }
            catch (Throwable e)
                {
                oResult = null;
                }
            }

        exception.setPartialResult(oResult);
        return exception;
        }

    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue)
        {
        return ensureConverterMap().put(oKey, oValue);
        }

    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return ensureConverterMap().put(oKey, oValue, cMillis);
        }

    // From interface: com.tangosol.net.NamedCache
    public void putAll(java.util.Map map)
        {
        // import com.tangosol.net.RequestIncompleteException;

        try
            {
            ensureConverterMap().putAll(map);
            }
        catch (RequestIncompleteException e)
            {
            throw processRequestIncomplete(e, BinaryMap.RESPONSE_SET);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        getCacheService().releaseCache(this);
        setReleased(true);
        }

    // From interface: com.tangosol.net.NamedCache
    public Object remove(Object oKey)
        {
        return ensureConverterMap().remove(oKey);
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        ensureBinaryMap().removeIndex(extractor);
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;

        removeMapListener(listener, (Filter) null);
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.internal.net.NamedCacheDeactivationListener;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapTrigger;
        // import com.tangosol.util.MapTriggerListener;
        // import com.tangosol.util.filter.InKeySetFilter;
        // import java.util.Set;

        if (listener instanceof NamedCacheDeactivationListener)
            {
            getDeactivationListeners().remove(listener);
            }
        else if (listener instanceof MapTriggerListener)
            {
            if (filter == null)
                {
                MapTrigger trigger = ((MapTriggerListener) listener).getTrigger();

                ensureBinaryMap().removeMapListener(null, null, trigger);
                }
            else
                {
                throw new UnsupportedOperationException(
                        "Filter-based MapTriggers are not supported");
                }
            }
        else if (filter instanceof InKeySetFilter)
            {
            Set setBinKeys = retrieveBinaryKeys((InKeySetFilter) filter);

            ensureBinaryMap().removeMapListener(
                    instantiateProxyListener(listener), setBinKeys,
                    com.tangosol.util.MapListenerSupport.isPrimingListener(listener));
            }
        else
            {
            ensureBinaryMap().removeMapListener(
                    instantiateProxyListener(listener), filter, null);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        // import com.tangosol.util.MapTrigger;
        // import com.tangosol.util.MapTriggerListener;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;

        if (listener instanceof MapTriggerListener)
            {
            throw new UnsupportedOperationException(
                    "Key-based MapTriggers are not supported");
            }

        ensureBinaryMap().removeMapListener(
                instantiateProxyListener(listener), getKeyToBinaryConverter().convert(oKey),
                com.tangosol.util.MapListenerSupport.isPrimingListener(listener));
        }

    /**
     * For the debugging purposes only.
     */
    public String reportKey(Object oKey)
        {
        PartitionedCache service = getService();

        return service.reportPartitionOwnership(
                service.getKeyPartitioningStrategy().getKeyPartition(oKey));
        }

    /**
     * For the debugging purposes only.
     */
    public String reportKeyDistribution(boolean fVerbose)
        {
        // import com.tangosol.net.partition.KeyPartitioningStrategy as com.tangosol.net.partition.KeyPartitioningStrategy;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;

        PartitionedCache  service     = getService();
        com.tangosol.net.partition.KeyPartitioningStrategy strategy    = service.getKeyPartitioningStrategy();
        int      cPartitions = service.getPartitionCount();
        int[]    ac          = new int[cPartitions];

        for (Iterator iter = keySet().iterator(); iter.hasNext();)
            {
            ac[strategy.getKeyPartition(iter.next())]++;
            }

        StringBuilder sb = new StringBuilder("\n");
        double dAverage = ((double) size())/cPartitions;
        double dStdDev  = 0.0;
        int    cMax     = 0;
        int    cMin     = Integer.MAX_VALUE;

        for (int iPartition = 0; iPartition < cPartitions; iPartition++)
            {
            int c = ac[iPartition];
            cMax = Math.max(cMax, c);
            cMin = Math.min(cMin, c);

            if (fVerbose && c > 0)
                {
                sb.append('\n')
                        .append(iPartition)
                        .append(": ")
                        .append(c);
                }
            double dDiff = dAverage - c;
            dStdDev += dDiff*dDiff;
            }
        return "max=" + cMax + "; min=" + cMin
               + "; average=" + ((float) dAverage)
               + "; std dev=" + Math.sqrt((float) (dStdDev/cPartitions))
               + sb;
        }

    /**
     * Coherence ApplicationEdition+ license support on a client.
     */
    private static void reportMissingLicense(String sFeature, com.tangosol.license.LicenseException e)
        {
        // import com.tangosol.util.Base;

        long ldtMsg = getLicenseMsgTimestamp();
        if (ldtMsg == 0L)
            {
            setLicenseMsgTimestamp(Base.getSafeTimeMillis());

            String sMsg = "Parallel " + sFeature
                          + " is a feature of Coherence Enterprise Edition and Coherence Grid Edition,"
                          + " and is not available in this Coherence Edition; selecting the default single-threaded Local "
                          + sFeature
                          + " implementation instead. The single-threaded Local "
                          + sFeature
                          + " implementation uses significantly more resources (CPU, memory and network) than the Parallel "
                          + sFeature
                          + " implementation"
                          + (sFeature.equals("Query") ? ", and should not be used for large queries" : "")
                          + ".";
            _trace(sMsg, 2);
            }
        }

    /**
     * For the debugging purposes only.
     */
    public String reportStorage(boolean fPrimary)
        {
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache       service   = getService();
        BinaryMap    mapBinary = (BinaryMap) service.getReferencesBinaryMap().get(getCacheName());
        long          lCacheId  = mapBinary.getCacheId();
        Storage      storage   = service.getStorage(lCacheId);
        Map           map       = fPrimary ? storage.getBackingMap() : storage.getBackupMap();
        StringBuilder sb        = new StringBuilder();

        sb.append("CacheId=")
                .append(lCacheId)
                .append(", Size=")
                .append(map.size());

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

            Binary binKey   = (Binary) entry.getKey();
            Binary binValue = (Binary) entry.getValue();

            sb.append('\n')
                    .append(getFromBinaryConverter().convert(binKey))
                    .append(" = ")
                    .append(getFromBinaryConverter().convert(binValue));

            if (fPrimary ? !service.isPrimaryOwner(binKey) :
                !service.isBackupOwner(binKey))
                {
                int   iPartition = service.getKeyPartition(binKey);
                int[] aiOwner    = service.getPartitionAssignments()[iPartition];
                int   nOwner     = fPrimary ? aiOwner[0] : aiOwner[1];

                sb.append(" ! owner=")
                        .append(nOwner);
                }
            }

        return sb.toString();
        }

    protected Object resultParallel(com.tangosol.util.InvocableMap.ParallelAwareAggregator aggregator, java.util.List listBinParts)
        {
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;

        boolean   fPassThrough = isPassThrough();
        Converter convDown;
        Converter convUp;

        if (fPassThrough)
            {
            // this is a pass through call; need to serialize the agent,
            // deserialize partial results and re-serialize the result
            // using the service's classloader
            PartitionedCache.BackingMapContext context = getService().getBackingMapContext();

            convDown = context.getValueToInternalConverter();
            convUp   = context.getValueFromInternalConverter();
            }
        else
            {
            convDown = getValueToBinaryConverter();
            convUp   = getFromBinaryConverter();
            }

        Object oResult = aggregator.aggregateResults(
                ConverterCollections.getCollection(listBinParts, convUp, convDown));

        return fPassThrough ? convDown.convert(oResult) : oResult;
        }

    protected Object resultStreaming(com.tangosol.util.InvocableMap.StreamingAggregator aggregator)
        {
        Object oResult = aggregator.finalizeResult();

        return isPassThrough() ?
               getService().getBackingMapContext().getValueToInternalConverter().convert(oResult) :
               oResult;
        }

    private java.util.Set retrieveBinaryKeys(com.tangosol.util.filter.InKeySetFilter filterKeys)
        {
        // import com.tangosol.util.ConverterCollections;

        return ConverterCollections.getSet(filterKeys.getKeys(),
                                           getKeyToBinaryConverter(), // decorate the binary if necessary
                                           getFromBinaryConverter()); // never used!
        }

    // Accessor for the property "Active"
    /**
     * Setter for property Active.<p>
     */
    protected void setActive(boolean fActive)
        {
        __m_Active = fActive;
        }

    // Accessor for the property "BinaryMap"
    /**
     * Setter for property BinaryMap.<p>
     * Underyling $BinaryMap reference.
     *
     * @volatile
     */
    public void setBinaryMap(BinaryMap map)
        {
        if (isActive())
            {
            // Map is immutable
            _assert(getBinaryMap() == null && map != null);

            setCacheName(map.getCacheName());
            }
        else
            {
            // allow the Map to be cleared
            _assert(map == null);
            }

        __m_BinaryMap = (map);
        }

    // Accessor for the property "CacheName"
    /**
     * Setter for property CacheName.<p>
     */
    protected void setCacheName(String sName)
        {
        __m_CacheName = sName;
        }

    // Accessor for the property "ClassLoader"
    /**
     * Setter for property ClassLoader.<p>
     */
    public void setClassLoader(ClassLoader loader)
        {
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.NullImplementation;

        if (isActive())
            {
            // ClassLoader is immutable
            _assert(getClassLoader() == null && loader != null);

            PartitionedCache service = getService();

            if (loader == NullImplementation.getClassLoader())
                {
                // under certain scenarios we will need to deserialize the key
                // in which case we will use the service's class loader
                setKeyToBinaryConverter(service.instantiateKeyToBinaryConverter(null, true));
                setValueToBinaryConverter(NullImplementation.getConverter());
                setFromBinaryConverter(ExternalizableHelper.CONVERTER_STRIP_INTDECO);
                setPassThrough(true);
                }
            else
                {
                setKeyToBinaryConverter(service.instantiateKeyToBinaryConverter(loader, false));
                setValueToBinaryConverter(service.instantiateValueToBinaryConverter(loader));
                setFromBinaryConverter(service.instantiateFromBinaryConverter(loader));
                }
            }
        else
            {
            // allow the ClassLoader to be cleared
            _assert(loader == null);
            }

        __m_ClassLoader = (loader);
        }

    // From interface: com.tangosol.io.ClassLoaderAware
    public void setContextClassLoader(ClassLoader loader)
        {
        throw new UnsupportedOperationException();
        }

    // Accessor for the property "ConverterMap"
    /**
     * Setter for property ConverterMap.<p>
     * The converting map.
     *
     * @see ensureConverterMap()
     */
    protected void setConverterMap(com.tangosol.net.NamedCache map)
        {
        if (isActive())
            {
            // Map is immutable
            _assert(getConverterMap() == null && map != null);
            }
        else
            {
            // allow the Map to be cleared
            _assert(map == null);
            }

        __m_ConverterMap = (map);
        }

    // Accessor for the property "DeactivationListeners"
    /**
     * Setter for property DeactivationListeners.<p>
     * Registered NamedCacheDeactivationListeners.
     */
    protected void setDeactivationListeners(com.tangosol.util.Listeners listeners)
        {
        __m_DeactivationListeners = listeners;
        }

    // Accessor for the property "Destroyed"
    /**
     * Setter for property Destroyed.<p>
     * True if it has been destroyed.
     */
    protected void setDestroyed(boolean fDestroyed)
        {
        __m_Destroyed = fDestroyed;
        }

    // Accessor for the property "FromBinaryConverter"
    /**
     * Setter for property FromBinaryConverter.<p>
     * A converter that takes Binary keys and values (from "below" the view)
     * and converts them via deserialization (etc.) to the objects expected
     * by clients of the ViewMap.
     */
    protected void setFromBinaryConverter(com.tangosol.util.Converter conv)
        {
        __m_FromBinaryConverter = conv;
        }

    // Accessor for the property "KeyToBinaryConverter"
    /**
     * Setter for property KeyToBinaryConverter.<p>
     * A converter that takes keys (from the "outside" of the view) and
     * converts them via serialization (etc.) to Binary objects.
     */
    protected void setKeyToBinaryConverter(com.tangosol.util.Converter conv)
        {
        __m_KeyToBinaryConverter = conv;
        }

    // Accessor for the property "LicenseMsgTimestamp"
    /**
     * Setter for property LicenseMsgTimestamp.<p>
     * Last time the license message was logged.
     *
     * @see #reportMissingLicense
     */
    private static void setLicenseMsgTimestamp(long ldt)
        {
        __s_LicenseMsgTimestamp = ldt;
        }

    // Accessor for the property "PassThrough"
    /**
     * Setter for property PassThrough.<p>
     * A boolean flag indicating that this ViewMap is used by the
     * pass-through optimization and all the incoming and outgoing keys and
     * values are Binary objects.
     */
    protected void setPassThrough(boolean fPass)
        {
        __m_PassThrough = fPass;
        }

    // Accessor for the property "Released"
    /**
     * Setter for property Released.<p>
     * True if it has been released.
     */
    protected void setReleased(boolean fReleased)
        {
        __m_Released = fReleased;
        }

    // Accessor for the property "ValueToBinaryConverter"
    /**
     * Setter for property ValueToBinaryConverter.<p>
     * A converter that takes value objects (from the "outside" of the view)
     * and converts them via serialization (etc.) to Binary objects.
     */
    protected void setValueToBinaryConverter(com.tangosol.util.Converter conv)
        {
        __m_ValueToBinaryConverter = conv;
        }

    // From interface: com.tangosol.net.NamedCache
    public int size()
        {
        return ensureConverterMap().size();
        }

    /**
     * Split the keys from the given iterator into the a map of key sets by
     * owner for the specified store.
     *
     * Note: the set of orphaned keys is mapped to null in the resulting map
     *
     * @param iterKeys  the iterator of keys to split
     *
     * @return Map<Member, Set<Object>>
     */
    protected java.util.Map splitKeysByOwner(java.util.Iterator iterKeys)
        {
        // import com.tangosol.net.Member;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.Map;
        // import java.util.Set;

        PartitionedCache service    = getService();
        Map     mapByOwner = new HashMap();

        while (iterKeys.hasNext())
            {
            Object oKey = iterKeys.next();

            Member member = service.getKeyOwner(oKey);

            // member could be null here, indicating that the owning partition is orphaned
            Set setMember = (Set) mapByOwner.get(member);
            if (setMember == null)
                {
                setMember = new HashSet();
                mapByOwner.put(member, setMember);
                }
            setMember.add(oKey);
            }
        return mapByOwner;
        }

    /**
     * Split the keys from the given iterator into a map of key sets by
     * partition ID.
     *
     * @return Map<nPID, Set<oKey>>
     */
    protected java.util.Map splitKeysByPartition(java.util.Iterator iterKeys)
        {
        // import com.tangosol.net.partition.KeyPartitioningStrategy as com.tangosol.net.partition.KeyPartitioningStrategy;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.Map;
        // import java.util.Set;

        com.tangosol.net.partition.KeyPartitioningStrategy strategy = getService().getKeyPartitioningStrategy();
        Map mapByPID = new HashMap();

        while (iterKeys.hasNext())
            {
            Object  oKey = iterKeys.next();

            Integer IPartition = Integer.valueOf(strategy.getKeyPartition(oKey));

            Set setPart = (Set) mapByPID.get(IPartition);
            if (setPart == null)
                {
                setPart = new HashSet();
                mapByPID.put(IPartition, setPart);
                }
            setPart.add(oKey);
            }

        return mapByPID;
        }

    protected boolean streamingCombine(com.tangosol.util.InvocableMap.StreamingAggregator aggregator, java.util.List listBinParts)
        {
        // import com.tangosol.util.Converter;
        // import java.util.Iterator;

        Converter convUp = isPassThrough() ?
                           getService().getBackingMapContext().getValueFromInternalConverter() :
                           getFromBinaryConverter();

        for (Iterator iter = listBinParts.iterator(); iter.hasNext();)
            {
            if (!aggregator.combine(convUp.convert(iter.next())))
                {
                return false;
                }
            }

        return true;
        }

    /**
     * Serialize the specified user object (e.g. EntryProcessor, Extractor,
     * etc) into a Binary.
     */
    protected com.tangosol.util.Binary toBinary(Object o)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;

        if (isPassThrough())
            {
            return (Binary) getService().getBackingMapContext().getValueToInternalConverter().convert(o);
            }
        else
            {
            Converter conv = getValueToBinaryConverter();
            if (conv == null)
                {
                throw onInvalidAccess();
                }
            else
                {
                return (Binary) conv.convert(o);
                }
            }
        }

    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(get_Name())
                .append("{Name=")
                .append(getCacheName())
                .append(", ClassLoader=")
                .append(getClassLoader())
                .append(", ServiceName=")
                .append(getService().getServiceName())
                .append('}');

        return sb.toString();
        }

    // From interface: com.tangosol.net.NamedCache
    public void truncate()
        {
        ensureBinaryMap().truncate();
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean unlock(Object oKey)
        {
        return ensureConverterMap().unlock(oKey);
        }

    /**
     * Validate the partition count provided from user's PartitionFilter
     * against the partition count from cache configuration.
     */
    protected void validatePartitionCount(com.tangosol.net.partition.PartitionSet parts)
        {
        int cPartitionThat = parts.getPartitionCount();
        int cPartitionThis = getService().getPartitionCount();

        if (cPartitionThat != cPartitionThis)
            {
            throw new IllegalArgumentException("The specified " + parts
                                               + " uses a partition-count of " + cPartitionThat
                                               + " that does not match the service's partition-count "
                                               + cPartitionThis);
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public java.util.Collection values()
        {
        return ensureConverterMap().values();
        }
    }
