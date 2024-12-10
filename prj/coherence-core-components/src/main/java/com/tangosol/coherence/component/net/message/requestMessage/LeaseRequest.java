
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest

package com.tangosol.coherence.component.net.message.requestMessage;

import com.tangosol.coherence.component.net.Lease;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.util.ExternalizableHelper;

/**
 * The Message contains all of the information necessary to describe a message
 * to send or to describe a received message.
 * <p>
 * With regards to the use of Message components within clustered Services,
 * Services are designed by dragging Message components into them as static
 * children. These Messages are the components that a Service can send to other
 * running instances of the same Service within a cluster. To send a Message, a
 * Service calls <code>instantiateMessage(String sMsgName)</code> with the name
 * of the child, then configures the Message object and calls Service.send
 * passing the Message. An incoming Message is created by the Message Receiver
 * by calling the <code>Service.instantiateMessage(int nMsgType)</code> and the
 * configuring the Message using the Received data. The Message is then queued
 * in the Service's Queue. When the Service thread gets the Message out of the
 * Queue, it invokes onMessage passing the Message, and the default
 * implementation for onMessage in turn calls <code>onReceived()</code> on the
 * Message object.
 * <p>
 * A RequestMessage extends the generic Message and adds the capability to poll
 * one or more Members for responses. In the simplest case, the RequestMessage
 * with one destination Member implements the request/response model.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class LeaseRequest
        extends    com.tangosol.coherence.component.net.message.RequestMessage
        implements com.tangosol.net.PriorityTask
    {
    // ---- Fields declarations ----
    
    /**
     * Property Lease
     *
     * Reference to a Lease object that this request carries an information
     * about. This object is always just a copy of an actual Lease.
     */
    private transient com.tangosol.coherence.component.net.Lease __m_Lease;
    
    // Default constructor
    public LeaseRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public LeaseRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/LeaseRequest".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.PriorityTask
    public long getExecutionTimeoutMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "Lease"
    /**
     * Getter for property Lease.<p>
    * Reference to a Lease object that this request carries an information
    * about. This object is always just a copy of an actual Lease.
     */
    public com.tangosol.coherence.component.net.Lease getLease()
        {
        return __m_Lease;
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public long getRequestTimeoutMillis()
        {
        return 0L;
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public int getSchedulingPriority()
        {
        return 0;
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
        // import com.tangosol.run.component.EventDeathException;
        
        super.onReceived();
        
        Member memberFrom = getFromMember();
        if (memberFrom == null)
            {
            // the sender is gone
            throw new EventDeathException();
            }
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import Component.Net.Lease;
        // import com.tangosol.util.ExternalizableHelper;
        
        super.read(input);
        
        int    iCache = ExternalizableHelper.readInt(input);
        Object oKey   = readObject(input);
        Lease  lease  = Lease.instantiate(iCache, oKey, getService());
        
        lease.read(input);
        
        setLease(lease);
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public void runCanceled(boolean fAbandoned)
        {
        }
    
    // Accessor for the property "Lease"
    /**
     * Setter for property Lease.<p>
    * Reference to a Lease object that this request carries an information
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
