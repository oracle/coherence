
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.LeaseMessage

package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Lease;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.util.ExternalizableHelper;
import java.io.IOException;

/**
 * LeaseMessage is the base component for Lease related messages used by
 * ReplicatedCache service.
 * 
 * Attributes:
 *     Lease
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class LeaseMessage
        extends    com.tangosol.coherence.component.net.Message
    {
    // ---- Fields declarations ----
    
    /**
     * Property Lease
     *
     * Reference to a Lease object that this message carries an information
     * about. This object is always just a copy of an actual Lease.
     */
    private transient com.tangosol.coherence.component.net.Lease __m_Lease;
    
    // Default constructor
    public LeaseMessage()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public LeaseMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.LeaseMessage();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/LeaseMessage".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific Message data.
     */
    public String getDescription()
        {
        // import Component.Net.Lease;
        
        Lease lease = getLease();
        return lease == null ? "Lease: null" : lease.toString();
        }
    
    // Accessor for the property "Lease"
    /**
     * Getter for property Lease.<p>
    * Reference to a Lease object that this message carries an information
    * about. This object is always just a copy of an actual Lease.
     */
    public com.tangosol.coherence.component.net.Lease getLease()
        {
        return __m_Lease;
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
        // import Component.Net.Lease;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.io.IOException;
        
        super.read(input);
        
        int    iCache = ExternalizableHelper.readInt(input);
        Object oKey;
        try
            {
            oKey = readObject(input);
            }
        catch (IOException e)
            {
            _trace("Failed to deserialize a key for cache " +
                   Lease.formatCacheName(iCache, getService()), 1);
            throw e;
            }
        
        Lease lease = Lease.instantiate(iCache, oKey, getService());
        
        lease.read(input);
        
        setLease(lease);
        }
    
    // Accessor for the property "Lease"
    /**
     * Setter for property Lease.<p>
    * Reference to a Lease object that this message carries an information
    * about. This object is always just a copy of an actual Lease.
     */
    public void setLease(com.tangosol.coherence.component.net.Lease lease)
        {
        // import Component.Net.Lease;
        
        // clone original lease, so this lease doesn't change
        
        __m_Lease = ((Lease) lease.clone());
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Net.Lease;
        // import com.tangosol.util.ExternalizableHelper;
        
        super.write(output);
        
        Lease lease = getLease();
        _assert(lease != null);
        
        ExternalizableHelper.writeInt(output, lease.getCacheIndex());
        writeObject(output, lease.getResourceKey());
        
        lease.write(output);
        }
    }
