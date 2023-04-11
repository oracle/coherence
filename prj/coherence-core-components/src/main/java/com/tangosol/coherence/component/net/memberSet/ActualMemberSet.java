
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.memberSet.ActualMemberSet

package com.tangosol.coherence.component.net.memberSet;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.util.Collections;
import java.lang.reflect.Array;

/**
 * Set of Member objects; must be thread safe.
 * 
 * ActualMemberSet holds references to its members.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ActualMemberSet
        extends    com.tangosol.coherence.component.net.MemberSet
    {
    // ---- Fields declarations ----
    
    /**
     * Property Count
     *
     * The number of Members in the MemberSet.
     * 
     * @volatile for use in size()
     */
    private volatile int __m_Count;
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
    public ActualMemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ActualMemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.memberSet.ActualMemberSet();
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
            clz = Class.forName("com.tangosol.coherence/component/net/memberSet/ActualMemberSet".replace('/', '.'));
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
        
        if (super.add(o))
            {
            Member member = (Member) o;
            setMember(member.getId(), member);
            setCount(getCount() + 1);
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Declared at the super level
    public synchronized boolean addAll(java.util.Collection collection)
        {
        // import Component.Util.Collections;
        
        return Collections.addAll(this, collection);
        }
    
    // Declared at the super level
    public synchronized void clear()
        {
        super.clear();
        setCount(0);
        setMember(null);
        }
    
    // Accessor for the property "Count"
    /**
     * Getter for property Count.<p>
    * The number of Members in the MemberSet.
    * 
    * @volatile for use in size()
     */
    protected int getCount()
        {
        return __m_Count;
        }
    
    // Declared at the super level
    /**
     * Getter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    public com.tangosol.coherence.component.net.Member getMember(int i)
        {
        // import Component.Net.Member;
        
        Member[] aMember = getMember();
        return (aMember == null || i >= aMember.length) ? null : aMember[i];
        }
    
    // Declared at the super level
    public synchronized boolean isEmpty()
        {
        return getCount() == 0;
        }
    
    // Declared at the super level
    public synchronized boolean remove(int nId)
        {
        if (super.remove(nId))
            {
            setMember(nId, null);
            setCount(getCount() - 1);
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Declared at the super level
    public synchronized boolean removeAll(java.util.Collection collection)
        {
        // import Component.Util.Collections;
        
        return Collections.removeAll(this, collection);
        }
    
    // Declared at the super level
    public synchronized boolean retainAll(java.util.Collection collection)
        {
        // import Component.Util.Collections;
        
        return Collections.retainAll(this, collection);
        }
    
    // Accessor for the property "Count"
    /**
     * Setter for property Count.<p>
    * The number of Members in the MemberSet.
    * 
    * @volatile for use in size()
     */
    protected void setCount(int c)
        {
        __m_Count = c;
        }
    
    // Declared at the super level
    /**
     * Setter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    protected void setMember(int i, com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member;
        
        Member[] aMember = getMember();
        
        // resize if storing non-null beyond bounds
        boolean fBeyondBounds = (aMember == null || i >= aMember.length);
        if (member != null && fBeyondBounds)
            {
            // resize just a little bigger (not trying very hard to
            // avoid resizes)
            Member[] aMemberNew = new Member[i + 2];
        
            // copy original bits
            if (aMember != null)
                {
                System.arraycopy(aMember, 0, aMemberNew, 0, aMember.length);
                }
        
            // store members
            aMember = aMemberNew;
            setMember(aMember);
        
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            aMember[i] = member;
            }
        }
    
    // Declared at the super level
    public int size()
        {
        return getCount();
        }
    
    // Declared at the super level
    public synchronized Object[] toArray(Object[] ao)
        {
        // import Component.Net.Member;
        // import java.lang.reflect.Array;
        
        // create the array to store the set contents
        int c = getCount();
        if (ao == null)
            {
            ao = new Member[c];
            }
        else if (ao.length < c)
            {
            // if it is not big enough, a new array of the same runtime
            // type is allocated
            ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), c);
            }
        else if (ao.length > c)
            {
            // if the collection fits in the specified array with room to
            // spare, the element in the array immediately following the
            // end of the collection is set to null
            ao[c] = null;
            }
        
        if (c > 0)
            {
            Member[] aMember = getMember();
            for (int i = 0, cMember = aMember.length, of = 0; i < cMember; ++i)
                {
                Member member = aMember[i];
                if (member != null)
                    {
                    ao[of++] = member;
                    }
                }
            }
        
        return ao;
        }
    }
