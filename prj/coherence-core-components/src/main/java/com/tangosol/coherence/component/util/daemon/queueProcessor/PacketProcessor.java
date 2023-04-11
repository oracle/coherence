
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.PacketProcessor

package com.tangosol.coherence.component.util.daemon.queueProcessor;

import com.tangosol.coherence.component.net.Member;

/**
 * This is a Daemon component that waits for items to process from a Queue.
 * Whenever the Queue contains an item, the onNotify event occurs. It is
 * expected that sub-classes will process onNotify as follows:
 * <pre><code>
 * Object o;
 * while ((o = getQueue().removeNoWait()) != null)
 *     {
 *     // process the item
 *     // ...
 *     }
 * </code></pre>
 * <p>
 * The Queue is used as the synchronization point for the daemon.
 * 
 * <br>
 * A client of PacketProcessor must configure:<br>
 * <br><ul>
 * <li>MemberSet property</li>
 * </ul><br>
 * A client of PacketDispatcher may configure:<br>
 * <br><ul>
 * <li>Priority property</li>
 * <li>ThreadGroup property</li>
 * </ul><br>
 * See the associated documentation for each.<br>
 * <br>
 * Once the PacketProcessor is configured, the client can start the processor
 * using the start() method.<br>
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PacketProcessor
        extends    com.tangosol.coherence.component.util.daemon.QueueProcessor
    {
    // ---- Fields declarations ----
    
    /**
     * Property MemberSet
     *
     * The master Member set of information about each Member.
     */
    private com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet __m_MemberSet;
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
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Default constructor
    public PacketProcessor()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PacketProcessor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant NonBlocking
    public boolean isNonBlocking()
        {
        return true;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.PacketProcessor();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/PacketProcessor".replace('/', '.'));
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
    
    // Accessor for the property "Member"
    /**
     * Getter for property Member.<p>
    * Indexed property of Cluster Member objects based on the MemberSet
    * property.
     */
    public com.tangosol.coherence.component.net.Member getMember(int i)
        {
        return getMemberSet().getMember(i);
        }
    
    // Accessor for the property "MemberId"
    /**
     * Getter for property MemberId.<p>
    * Once a Member id has been assigned to this Member, this property provides
    * that Member id; previous to assignment, this property evaluates to zero.
     */
    public int getMemberId()
        {
        // import Component.Net.Member;
        
        Member member = getThisMember();
        return member == null ? 0 : member.getId();
        }
    
    // Accessor for the property "MemberSet"
    /**
     * Getter for property MemberSet.<p>
    * The master Member set of information about each Member.
     */
    public com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet getMemberSet()
        {
        return __m_MemberSet;
        }
    
    // Accessor for the property "ThisMember"
    /**
     * Getter for property ThisMember.<p>
    * Once a Member id has been assigned to this Member, this property provides
    * the Member object; previous to assignment, this property evaluates to
    * null.
     */
    public com.tangosol.coherence.component.net.Member getThisMember()
        {
        return getMemberSet().getThisMember();
        }
    
    // Accessor for the property "MemberSet"
    /**
     * Setter for property MemberSet.<p>
    * The master Member set of information about each Member.
     */
    public void setMemberSet(com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet set)
        {
        _assert(set != null);
        _assert(getMemberSet() == null);
        
        __m_MemberSet = (set);
        }
    
    // Declared at the super level
    /**
     * Starts the daemon thread associated with this component. If the thread is
    * already starting or has started, invoking this method has no effect.
    * 
    * Synchronization is used here to verify that the start of the thread
    * occurs; the lock is obtained before the thread is started, and the daemon
    * thread notifies back that it has started from the run() method.
     */
    public synchronized void start()
        {
        if (getMemberSet() == null)
            {
            throw new IllegalStateException("MemberSet is required!");
            }
        
        super.start();
        }
    }
