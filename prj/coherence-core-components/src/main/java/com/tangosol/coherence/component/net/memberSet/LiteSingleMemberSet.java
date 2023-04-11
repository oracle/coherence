
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.memberSet.LiteSingleMemberSet

package com.tangosol.coherence.component.net.memberSet;

import com.tangosol.coherence.component.net.Member;

/**
 * A MemberSet that represents one Member.  This differs from SingleMemberSet
 * by holding the represented member in "lite" format (mini-id) only.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class LiteSingleMemberSet
        extends    com.tangosol.coherence.component.net.MemberSet
    {
    // ---- Fields declarations ----
    
    /**
     * Property MemberId
     *
     * The mini-id of the singleton member.
     */
    private int __m_MemberId;
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
        __mapChildren.put("Iterator", com.tangosol.coherence.component.net.MemberSet.Iterator.get_CLASS());
        }
    
    // Default constructor
    public LiteSingleMemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public LiteSingleMemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        
        // containment initialization: children
        
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
        return new com.tangosol.coherence.component.net.memberSet.LiteSingleMemberSet();
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
            clz = Class.forName("com.tangosol.coherence/component/net/memberSet/LiteSingleMemberSet".replace('/', '.'));
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
    
    // Declared at the super level
    public synchronized boolean add(Object o)
        {
        // import Component.Net.Member;
        
        Member memberNew = (Member) o;
        if (memberNew == null)
            {
            throw new IllegalArgumentException();
            }
        
        int nMemberOld = getMemberId();
        int nMemberNew = memberNew.getId();
        if (nMemberOld == 0)
            {
            setMemberId(nMemberNew);
            return true;
            }
        
        if (nMemberOld == nMemberNew)
            {
            return false;
            }
        
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    public synchronized boolean addAll(java.util.Collection collection)
        {
        synchronized (collection)
            {
            switch (collection.size())
                {
                case 0:
                    return false;
        
                case 1:
                    return add(collection.toArray()[0]);
        
                default:
                    throw new UnsupportedOperationException();
                }
            }
        }
    
    // Declared at the super level
    public synchronized void clear()
        {
        if (!isEmpty())
            {
            setMemberId(0);
            }
        }
    
    // Declared at the super level
    public synchronized boolean contains(int nId)
        {
        return getMemberId() == nId;
        }
    
    // Declared at the super level
    public synchronized boolean contains(Object o)
        {
        // import Component.Net.Member;
        
        return o instanceof Member && getMemberId() == ((Member) o).getId();
        }
    
    // Declared at the super level
    public synchronized boolean containsAll(java.util.Collection collection)
        {
        synchronized (collection)
            {
            switch (collection.size())
                {
                case 0:
                    return true;
        
                case 1:
                    return contains(collection.toArray()[0]);
        
                default:
                    return false;
                }
            }
        }
    
    public static LiteSingleMemberSet copyFrom(com.tangosol.coherence.component.net.MemberSet setMembers)
        {
        if (setMembers.size() > 1)
            {
            throw new IllegalArgumentException();
            }
        
        return instantiate(setMembers.getFirstId());
        }
    
    // Declared at the super level
    /**
     * Return the lowest member id contained in this member set, or 0 if empty.
     */
    public int getFirstId()
        {
        return getMemberId();
        }
    
    // Declared at the super level
    /**
     * Return the highest member id contained in this member set, or 0 if empty.
     */
    public int getLastId()
        {
        return getMemberId();
        }
    
    // Declared at the super level
    /**
     * Getter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    public com.tangosol.coherence.component.net.Member getMember(int i)
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "MemberId"
    /**
     * Getter for property MemberId.<p>
    * The mini-id of the singleton member.
     */
    public int getMemberId()
        {
        return __m_MemberId;
        }
    
    public static LiteSingleMemberSet instantiate(int nMember)
        {
        LiteSingleMemberSet set = new LiteSingleMemberSet();
        set.setMemberId(nMember);
        return set;
        }
    
    // Declared at the super level
    /**
     * Instantiate a MemberSet containing the specified member.  The resulting
    * MemberSet may not be iteratable.
     */
    public static com.tangosol.coherence.component.net.MemberSet instantiate(com.tangosol.coherence.component.net.Member member)
        {
        return instantiate(member == null ? 0 : member.getId());
        }
    
    // Declared at the super level
    public synchronized boolean isEmpty()
        {
        return getMemberId() == 0;
        }
    
    // Declared at the super level
    /**
     * Randomly select a Member id from the MemberSet
    * 
    * @return a Member id or 0 if no Members are available
     */
    public synchronized int random()
        {
        return getFirstId();
        }
    
    // Declared at the super level
    /**
     * Ensure all reads made after this call will have visibility to any writes
    * made prior to a corresponding call to writeBarrier for any member in this
    * set on another thread.
     */
    public void readBarrier()
        {
        readBarrier(getMemberId());
        }
    
    // Declared at the super level
    /**
     * Read a trivial (containing a single member) MemberSet from the specified
    * stream.
     */
    public void readFew(java.io.DataInput stream)
            throws java.io.IOException
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    /**
     * Read the MemberSet serialized as a bit-set from the specified stream.
     */
    public void readMany(java.io.DataInput stream)
            throws java.io.IOException
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    /**
     * Read a length-encoded array of Member mini-ids from the specified stream.
     */
    public void readOne(java.io.DataInput stream)
            throws java.io.IOException
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    public synchronized boolean remove(int nId)
        {
        if (nId == getMemberId())
            {
            setMemberId(0);
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Declared at the super level
    public synchronized boolean remove(Object o)
        {
        // import Component.Net.Member;
        
        return (o instanceof Member) ? remove(((Member) o).getId()) : false;
        }
    
    // Declared at the super level
    public synchronized boolean removeAll(java.util.Collection collection)
        {
        // import java.util.Iterator as java.util.Iterator;
        
        boolean fMod = false;
        for (java.util.Iterator iter = collection.iterator(); iter.hasNext(); )
            {
            fMod = fMod | remove(iter.next());
            }
        
        return fMod;
        }
    
    // Declared at the super level
    public synchronized boolean retainAll(java.util.Collection collection)
        {
        // import Component.Net.Member;
        // import java.util.Iterator as java.util.Iterator;
        
        int nMemberId = getMemberId();
        for (java.util.Iterator iter = collection.iterator(); iter.hasNext(); )
            {
            if (nMemberId == ((Member) iter.next()).getId())
                {
                // found the single member
                return false;
                }
            }
        
        setMemberId(0);
        return true;
        }
    
    // Accessor for the property "MemberId"
    /**
     * Setter for property MemberId.<p>
    * The mini-id of the singleton member.
     */
    public void setMemberId(int nMember)
        {
        // import Component.Net.Member;
        
        __m_MemberId = (nMember);
        
        if (nMember == 0)
            {
            setBitSet(null);
            }
        else
            {
            setBitSet(Member.calcByteOffset(nMember), Member.calcByteMask(nMember));
            }
        }
    
    // Declared at the super level
    public synchronized int size()
        {
        return getMemberId() == 0 ? 0 : 1;
        }
    
    // Declared at the super level
    /**
     * Ensure all writes made prior to this call to be visible to any thread
    * which calls the corresponding readBarrier method for any of the members
    * in this MemberSet.
     */
    public void writeBarrier()
        {
        writeBarrier(getMemberId());
        }
    }
