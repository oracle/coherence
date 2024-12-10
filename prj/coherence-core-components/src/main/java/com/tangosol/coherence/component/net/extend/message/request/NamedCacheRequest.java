
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest

package com.tangosol.coherence.component.net.extend.message.request;

import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import java.io.IOException;
import java.util.Iterator;

/**
 * Base component for all NamedCache Protocol Request messages.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class NamedCacheRequest
        extends    com.tangosol.coherence.component.net.extend.message.Request
    {
    // ---- Fields declarations ----
    
    /**
     * Property NamedCache
     *
     * The target of this NamedCacheRequest. This property must be set by the
     * Receiver before the run() method is called.
     */
    private transient com.tangosol.net.NamedCache __m_NamedCache;
    
    /**
     * Property TransferThreshold
     *
     * The approximate maximum number of bytes transfered by a partial
     * response.
     * 
     * See CacheServiceProxy#TransferThreshold.
     */
    private transient long __m_TransferThreshold;
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
        __mapChildren.put("Status", com.tangosol.coherence.component.net.extend.message.Request.Status.get_CLASS());
        }
    
    // Initializing constructor
    public NamedCacheRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/request/NamedCacheRequest".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    /**
     * Calculate the number of partitions that can be included in a partial
    * response given the size of a single partition's worth of results.
    * 
    * @param cPart  the total number of partitions
    * @param cb  the number of bytes contained in a single partition's worth of
    * results
    * 
    * @return the number of partitions that can be included in a partial
    * response
    * 
    * @see TransferThreshold
     */
    protected int calculateBatchSize(int cPart, int cb)
        {
        // COH-2139: It is assumed that the size of each partition will be roughly equal.
        // Thus if the sampled partition yielded no results, then it is likely that the
        // overall result set will be small, and thus all partitions should be queried
        // in a single batch.
        int cBatch = cb == 0
                ? (int) cPart
                : (int) (getTransferThreshold() / cb);
        
        cBatch = Math.max(cBatch, 1);
        cBatch = Math.min(cBatch, cPart);
        
        return cBatch;
        }
    
    /**
     * Return the physical size of the given Collection of Binary instances.
    * 
    * @param col  the Collection
    * @param fEntries  if true, the given Collection contains Map.Entry
    * instances with Binary keys and values
    * 
    * @return the number of bytes contained in the given Collection
     */
    protected int calculateBinarySize(java.util.Collection col, boolean fEntries)
        {
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        int cb = 0;
        if (col != null)
            {
            for (Iterator iter = col.iterator(); iter.hasNext(); )
                {
                if (fEntries)
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                    cb += ((Binary) entry.getKey())  .length();
                    cb += ((Binary) entry.getValue()).length();
                    }
                else
                    {
                    cb += ((Binary) iter.next()).length();
                    }
                }
            }
        
        return cb;
        }
    
    /**
     * Create a newly filled PartitionSet appropriate for the NamedCache
    * associated with this Request.
    * 
    * @return a new PartitionSet
     */
    protected com.tangosol.net.partition.PartitionSet createPartitionSet()
        {
        // import Component.Util.SafeService.SafeCacheService;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.PartitionedService;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.run.xml.XmlConfigurable as com.tangosol.run.xml.XmlConfigurable;
        // import com.tangosol.run.xml.XmlElement;
        
        int cPart;
        
        CacheService service = getNamedCache().getCacheService();
        if (service instanceof PartitionedService)
            {
            cPart = ((PartitionedService) service).getPartitionCount();
            }
        else
            {
            XmlElement xml = null;
        
            if (service instanceof SafeCacheService)
                {
                xml = ((SafeCacheService) service).getConfig();
                }
            else if (service instanceof com.tangosol.run.xml.XmlConfigurable)
                {
                xml = ((com.tangosol.run.xml.XmlConfigurable) service).getConfig();
                }
        
            // default to 17 partitions for non-partitioned services
            cPart = 17;
        
            if (xml != null)
                {
                cPart = xml.getSafeElement("partition-count").getInt(cPart);
                }
            }
        
        PartitionSet parts = new PartitionSet(cPart);
        parts.fill();
        
        return parts;
        }
    
    /**
     * Decode the given Binary that contains a PartitionSet followed by a packed
    * integer value.
    * 
    * @param bin  the Binary to decode
    * 
    * @return an Object array containing a PartitionSet followed by an Integer
     */
    protected Object[] decodeCookie(com.tangosol.util.Binary bin)
        {
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Base;
        // import java.io.IOException;
        
        PartitionSet parts;
        int          n;
        
        if (bin == null)
            {
            parts = createPartitionSet();
            n     = 0;
            }
        else
            {
            com.tangosol.io.ReadBuffer.BufferInput bi = bin.getBufferInput();
            try
                {
                parts = new PartitionSet();
                parts.readExternal(bi);
                n     = bi.readPackedInt();
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e, "error decoding cookie");
                }
            }
        
        return new Object[] {parts, Base.makeInteger(n)};
        }
    
    /**
     * Encode the given PartitionSet and integer value into a new Binary.
    * 
    * @param parts  the PartitionSet
    * @param n  the integer value
    * 
    * @return a new Binary containing the encoded PartitionSet and integer value
     */
    protected com.tangosol.util.Binary encodeCookie(com.tangosol.net.partition.PartitionSet parts, int n)
        {
        // import com.tangosol.io.WriteBuffer$BufferOutput as com.tangosol.io.WriteBuffer.BufferOutput;
        // import com.tangosol.util.BinaryWriteBuffer;
        // import java.io.IOException;
        
        if (parts.isEmpty())
            {
            return null;
            }
        
        com.tangosol.io.WriteBuffer.BufferOutput bo = new BinaryWriteBuffer(64).getBufferOutput();
        try
            {
            parts.writeExternal(bo);
            bo.writePackedInt(n);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e, "error encoding cookie");
            }
        
        return bo.getBuffer().toBinary();
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Getter for property NamedCache.<p>
    * The target of this NamedCacheRequest. This property must be set by the
    * Receiver before the run() method is called.
     */
    public com.tangosol.net.NamedCache getNamedCache()
        {
        return __m_NamedCache;
        }
    
    // Accessor for the property "TransferThreshold"
    /**
     * Getter for property TransferThreshold.<p>
    * The approximate maximum number of bytes transfered by a partial response.
    * 
    * See CacheServiceProxy#TransferThreshold.
     */
    public long getTransferThreshold()
        {
        return __m_TransferThreshold;
        }
    
    /**
     * Remove a subset of partitions of no more than the given cardinality from
    * the specified PartitionSet. For partitioned service try to minimize the
    * number of members the partitions in the resulting PartitionSet are owned
    * by.
    * 
    * @param partsRemain the original ParitionSet;will be modified by removing
    * up to cBatch partitions
    * @param cBatch  the maximum subset size
    * 
    * @return a new PartitionSet containing up to cBatch partitions that were
    * removed from the original set
     */
    protected com.tangosol.net.partition.PartitionSet removePartitionBatch(com.tangosol.net.partition.PartitionSet partsRemain, int cBatch)
        {
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.PartitionedService;
        // import com.tangosol.net.partition.PartitionSet;
        
        PartitionSet partsBatch;
        
        int cPartsAll    = partsRemain.getPartitionCount();
        int cPartsRemain = partsRemain.cardinality();
        
        if (cPartsRemain <= cBatch)
            {
            partsBatch = new PartitionSet(partsRemain); // copy
            partsRemain.clear();
            }
        else
            {
            partsBatch = new PartitionSet(cPartsAll);
        
            CacheService service = getNamedCache().getCacheService();
            if (service instanceof PartitionedService)
                {
                PartitionedService svcPartitioned = (PartitionedService) service;
                int                cBatchLeft     = cBatch;
                while (!partsRemain.isEmpty() && cBatchLeft > 0)
                    {
                    // choose the first partition randomly
                    int nPart = partsRemain.rnd();
        
                    // the loop below should normally execute just once;
                    // during distribution we limit it to cPartsRemain attempts
                    Member member = null;
                    for (int i = 0; i < cPartsRemain; i++)
                        {
                        member = svcPartitioned.getPartitionOwner(nPart);
                        if (member != null)
                            {
                            break;
                            }
                        nPart = partsRemain.next(nPart);
                        }
                    if (member == null)
                    	{
                    	// every partition is in re-distribution; fall back on the default algorithm
                    	break;
                    	}
        
                    // add more partitions for the same member
                    PartitionSet parts = svcPartitioned.getOwnedPartitions(member);
                    parts.retain(partsRemain);
        
                    int c = parts.cardinality();
                    while (c > cBatchLeft)
                        {
                        parts.removeNext(0);
                        c--;
                        }
                    partsBatch.add(parts);
                    partsRemain.remove(parts);
                    cBatchLeft -= c;
                    }
                }
        
            if (partsBatch.isEmpty())
                {
                // service is not partitioned or the optimized algorithm failed
                // calculate the next batch of partitions randomly
                for (int nPart = partsRemain.rnd();
                     --cBatch >=0 && (nPart = partsRemain.removeNext(nPart)) >= 0; )
                    {
                    partsBatch.add(nPart);
                    }
                }
            }
        return partsBatch;
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Setter for property NamedCache.<p>
    * The target of this NamedCacheRequest. This property must be set by the
    * Receiver before the run() method is called.
     */
    public void setNamedCache(com.tangosol.net.NamedCache cache)
        {
        __m_NamedCache = cache;
        }
    
    // Accessor for the property "TransferThreshold"
    /**
     * Setter for property TransferThreshold.<p>
    * The approximate maximum number of bytes transfered by a partial response.
    * 
    * See CacheServiceProxy#TransferThreshold.
     */
    public void setTransferThreshold(long cb)
        {
        __m_TransferThreshold = cb;
        }
    }
