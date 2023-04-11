
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.BusEventMessage

package com.tangosol.coherence.component.net.message;

import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.net.exabus.Event;
import java.io.IOException;

/**
 * BusEventMessage is an internal message used to pass Exabus events onto the
 * corresponding service thread.
 * 
 * Attributes:
 *     MessageHandler
 *     Event
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class BusEventMessage
        extends    com.tangosol.coherence.component.net.Message
    {
    // ---- Fields declarations ----
    
    /**
     * Property Event
     *
     * The Event object.
     */
    private com.oracle.coherence.common.net.exabus.Event __m_Event;
    
    /**
     * Property MessageHandler
     *
     * The MessageHandler that emitted this message.
     */
    private com.tangosol.coherence.component.net.MessageHandler __m_MessageHandler;
    
    // Default constructor
    public BusEventMessage()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public BusEventMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.BusEventMessage();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/BusEventMessage".replace('/', '.'));
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
        return "BusEvent=" + getEvent();
        }
    
    // Accessor for the property "Event"
    /**
     * Getter for property Event.<p>
    * The Event object.
     */
    public com.oracle.coherence.common.net.exabus.Event getEvent()
        {
        return __m_Event;
        }
    
    // Accessor for the property "MessageHandler"
    /**
     * Getter for property MessageHandler.<p>
    * The MessageHandler that emitted this message.
     */
    public com.tangosol.coherence.component.net.MessageHandler getMessageHandler()
        {
        return __m_MessageHandler;
        }
    
    // Declared at the super level
    /**
     * Getter for property Internal.<p>
    * True for "internal" notification Messages (those that do not come from
    * the network but come from the cluster Service).
     */
    public boolean isInternal()
        {
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import com.oracle.coherence.common.io.BufferSequence;
        // import com.oracle.coherence.common.net.exabus.Event;
        // import com.oracle.coherence.common.net.exabus.Event$Type as com.oracle.coherence.common.net.exabus.Event.Type;
        // import java.io.IOException;
        
        Event event = getEvent();
        if (event.getType() == com.oracle.coherence.common.net.exabus.Event.Type.MESSAGE)
            {
            // in the case of a event carrying a serialized message, the answer is
            // dependent on the type of the carried message. Peek at the type
            // to identify.
            try
                {
                com.tangosol.io.ReadBuffer.BufferInput in = getMessageHandler().createReadBuffer(
                    (BufferSequence) event.getContent()).getBufferInput();
                in.readShort(); // skip over service id
                return in.readShort() < 0;        
                }
            catch (IOException e)
                {
                // this should really not be possible unless this message came from a non-Coherence process
                return false;
                }
            }
        
        return super.isInternal();
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
        getMessageHandler().onBusEvent(getEvent());
        }
    
    // Accessor for the property "Event"
    /**
     * Setter for property Event.<p>
    * The Event object.
     */
    public void setEvent(com.oracle.coherence.common.net.exabus.Event event)
        {
        __m_Event = event;
        }
    
    // Accessor for the property "MessageHandler"
    /**
     * Setter for property MessageHandler.<p>
    * The MessageHandler that emitted this message.
     */
    public void setMessageHandler(com.tangosol.coherence.component.net.MessageHandler handler)
        {
        __m_MessageHandler = handler;
        }
    }
