
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.MessageFactory

package com.tangosol.coherence.component.net.extend;

import java.util.Iterator;
import java.util.Map;

/**
 * Base definition of a MessageFactory component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MessageFactory
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.net.messaging.Protocol.MessageFactory
    {
    // ---- Fields declarations ----
    
    /**
     * Property MessageClass
     *
     * An array of static child component classes that are subclasses of the
     * Message component.
     */
    private transient Class[] __m_MessageClass;
    
    /**
     * Property Protocol
     *
     * The Protocol for which this MessageFactory creates Message objects.
     * 
     * @see com.tangosol.net.messaging.Protocol$MessageFactory#getProtocol
     */
    private com.tangosol.net.messaging.Protocol __m_Protocol;
    
    /**
     * Property Version
     *
     * The Protocol version supported by this MessageFactory.
     * 
     * @see com.tangosol.net.messaging.Protocol$MessageFactory#getVersion
     */
    private int __m_Version;
    
    // Default constructor
    public MessageFactory()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MessageFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.MessageFactory();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/MessageFactory".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.messaging.Protocol$MessageFactory
    public com.tangosol.net.messaging.Message createMessage(int nType)
        {
        Class clz = getMessageClass(nType);
        if (clz == null)
            {
            throw new IllegalArgumentException(
                    "Unable to instantiate a Message of type: " + nType);
            }
        
        try
            {    
            Message message = (Message) clz.newInstance();
        
            // set the Message version
            message.setImplVersion(getVersion());
        
            return message;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error instantiating a message of type: "
                    + clz.getName());
            }
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return "Protocol=" + getProtocol()+ ", Version=" + getVersion();
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Getter for property MessageClass.<p>
    * An array of static child component classes that are subclasses of the
    * Message component.
     */
    protected Class[] getMessageClass()
        {
        return __m_MessageClass;
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Getter for property MessageClass.<p>
    * An array of static child component classes that are subclasses of the
    * Message component.
     */
    protected Class getMessageClass(int i)
        {
        Class[] aClz = getMessageClass();
        return aClz != null && i < aClz.length ? aClz[i] : null;
        }
    
    // From interface: com.tangosol.net.messaging.Protocol$MessageFactory
    // Accessor for the property "Protocol"
    /**
     * Getter for property Protocol.<p>
    * The Protocol for which this MessageFactory creates Message objects.
    * 
    * @see com.tangosol.net.messaging.Protocol$MessageFactory#getProtocol
     */
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        return __m_Protocol;
        }
    
    // From interface: com.tangosol.net.messaging.Protocol$MessageFactory
    // Accessor for the property "Version"
    /**
     * Getter for property Version.<p>
    * The Protocol version supported by this MessageFactory.
    * 
    * @see com.tangosol.net.messaging.Protocol$MessageFactory#getVersion
     */
    public int getVersion()
        {
        return __m_Version;
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import java.util.Iterator;
        // import java.util.Map;
        
        // go through all static children and register all message classes
        Class clzMessage = Message.class;
        Map   mapClz     = get_ChildClasses();
        if (mapClz != null)
            {
            for (Iterator iter = mapClz.values().iterator(); iter.hasNext(); )
                {
                Class clz = (Class) iter.next();
                if (clzMessage.isAssignableFrom(clz))
                    {
                    try
                        {
                        Message message = (Message) clz.newInstance();
                        int     nType   = message.getTypeId();
                        if (getMessageClass(nType) != null)
                            {
                            throw new IllegalStateException("duplicate message type "
                                    + nType + ": "
                                    + clz + ", "
                                    + getMessageClass(nType));
                            }
                        setMessageClass(nType, clz);
                        }
                    catch (Exception e)
                        {
                        _trace(e, "Unable to instantiate a message of type \"" + clz + '"');
                        }
                    }
                }
            }
        
        super.onInit();
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Setter for property MessageClass.<p>
    * An array of static child component classes that are subclasses of the
    * Message component.
     */
    protected void setMessageClass(Class[] aClz)
        {
        __m_MessageClass = aClz;
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Setter for property MessageClass.<p>
    * An array of static child component classes that are subclasses of the
    * Message component.
     */
    protected void setMessageClass(int i, Class clz)
        {
        _assert(clz != null);
        
        Class[] aClz = getMessageClass();
        if (aClz == null || i >= aClz.length)
            {
            // resize, making the array bigger than necessary (avoid resizes)
            Class[] aClzNew = new Class[Math.max(i + (i >>> 1), i + 4)];
        
            // copy original data
            if (aClz != null)
                {
                System.arraycopy(aClz, 0, aClzNew, 0, aClz.length);
                }
        
            setMessageClass(aClz = aClzNew);
            }
        
        aClz[i] = clz;
        }
    
    // Accessor for the property "Protocol"
    /**
     * Setter for property Protocol.<p>
    * The Protocol for which this MessageFactory creates Message objects.
    * 
    * @see com.tangosol.net.messaging.Protocol$MessageFactory#getProtocol
     */
    public void setProtocol(com.tangosol.net.messaging.Protocol protocol)
        {
        __m_Protocol = protocol;
        }
    
    // Accessor for the property "Version"
    /**
     * Setter for property Version.<p>
    * The Protocol version supported by this MessageFactory.
    * 
    * @see com.tangosol.net.messaging.Protocol$MessageFactory#getVersion
     */
    public void setVersion(int nVersion)
        {
        __m_Version = nVersion;
        }
    }
