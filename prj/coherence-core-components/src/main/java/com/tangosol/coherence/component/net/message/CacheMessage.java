
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.CacheMessage

package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.run.component.EventDeathException;

/**
 * CacheMessage is the base component for Cache content related messages used
 * by ReplicatedCache service.
 * 
 * Attributes:
 *     CacheIndex
 *     LeaseCount
 *     CacheData
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheMessage
        extends    com.tangosol.coherence.component.net.Message
    {
    // ---- Fields declarations ----
    
    /**
     * Property CacheData
     *
     * Serialized cache data
     */
    private byte[] __m_CacheData;
    
    /**
     * Property CacheIndex
     *
     * Cache index
     */
    private int __m_CacheIndex;
    
    /**
     * Property LeaseCount
     *
     * The number of Lease/Resource pairs in the CacheData
     */
    private int __m_LeaseCount;
    
    // Default constructor
    public CacheMessage()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.CacheMessage();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/CacheMessage".replace('/', '.'));
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
    
    // Accessor for the property "CacheData"
    /**
     * Getter for property CacheData.<p>
    * Serialized cache data
     */
    public byte[] getCacheData()
        {
        return __m_CacheData;
        }
    
    // Accessor for the property "CacheIndex"
    /**
     * Getter for property CacheIndex.<p>
    * Cache index
     */
    public int getCacheIndex()
        {
        return __m_CacheIndex;
        }
    
    // Accessor for the property "LeaseCount"
    /**
     * Getter for property LeaseCount.<p>
    * The number of Lease/Resource pairs in the CacheData
     */
    public int getLeaseCount()
        {
        return __m_LeaseCount;
        }
    
    // Declared at the super level
    /**
     * This is the event that is executed when a Message is received.
    * <p>
    * It is the main processing event of the Message called by the
    * <code>Service.onMessage()</code> event. With regards to the use of
    * Message components within clustered Services, Services are designed by
    * dragging Message components into them as static children. These Messages
    * are the components that a Service can send to other running instances of
    * the same Service within a cluster. When the onReceived event is invoked
    * by a Service, it means that the Message has been received; the code in
    * the onReceived event is therefore the Message specific logic for
    * processing a received Message. For example, when onReceived is invoked on
    * a Message named FindData, the onReceived event should do the work to
    * "find the data", because it is being invoked by the Service that received
    * the "find the data" Message.
     */
    public void onReceived()
        {
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service;
        // import com.tangosol.run.component.EventDeathException;
        
        super.onReceived();
        
        // TODO: consider moving this into the Message driven by a VirtualConstant property
        
        Member memberFrom = getFromMember();
        if (memberFrom == null ||
            getService().getServiceState() >= Service.SERVICE_STOPPING)
            {
            // the sender is gone or the service is stopping
            throw new EventDeathException();
            }
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        super.read(input);
        
        setCacheIndex(input.readInt());
        setLeaseCount(input.readInt());
        
        int    cb = input.readInt();
        byte[] ab = new byte[cb];
        
        input.readFully(ab);
        
        setCacheData(ab);
        }
    
    // Accessor for the property "CacheData"
    /**
     * Setter for property CacheData.<p>
    * Serialized cache data
     */
    public void setCacheData(byte[] pCacheData)
        {
        __m_CacheData = pCacheData;
        }
    
    // Accessor for the property "CacheIndex"
    /**
     * Setter for property CacheIndex.<p>
    * Cache index
     */
    public void setCacheIndex(int lease)
        {
        __m_CacheIndex = lease;
        }
    
    // Accessor for the property "LeaseCount"
    /**
     * Setter for property LeaseCount.<p>
    * The number of Lease/Resource pairs in the CacheData
     */
    public void setLeaseCount(int pLeaseCount)
        {
        __m_LeaseCount = pLeaseCount;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        super.write(output);
        
        output.writeInt(getCacheIndex());
        output.writeInt(getLeaseCount());
        
        byte[] ab = getCacheData();
        int    cb = ab.length;
        
        output.writeInt(cb);
        output.write(ab, 0, cb);
        
        // cleanup no longer needed data as soon as we can
        setCacheData(null);
        }
    }
