
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.memberSet.SingleMemberSet

package com.tangosol.coherence.component.net.memberSet;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;
import java.lang.reflect.Array;

/**
 * A MemberSet that contains one Member and may optionally be immutable.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SingleMemberSet
        extends    com.tangosol.coherence.component.net.MemberSet
    {
    // ---- Fields declarations ----
    
    /**
     * Property ReadOnly
     *
     */
    private boolean __m_ReadOnly;
    
    /**
     * Property TheMember
     *
     */
    private com.tangosol.coherence.component.net.Member __m_TheMember;
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
    public SingleMemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SingleMemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.memberSet.SingleMemberSet();
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
            clz = Class.forName("com.tangosol.coherence/component/net/memberSet/SingleMemberSet".replace('/', '.'));
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
        
        Member memberOld = getTheMember();
        Member memberNew = (Member) o;
        
        if (memberNew == null)
            {
            throw new IllegalArgumentException();
            }
        
        if (memberOld == null)
            {
            setTheMember(memberNew);
            return true;
            }
        
        if (memberOld == memberNew)
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
    
    public void check()
        {
        if (isReadOnly())
            {
            throw new UnsupportedOperationException();
            }
        }
    
    // Declared at the super level
    public synchronized void clear()
        {
        if (!isEmpty())
            {
            setTheMember(null);
            }
        }
    
    // Declared at the super level
    public synchronized boolean contains(int nId)
        {
        // import Component.Net.Member;
        
        Member member = getTheMember();
        return member != null && member.getId() == nId;
        }
    
    // Declared at the super level
    public synchronized boolean contains(Object o)
        {
        // import Component.Net.Member;
        
        if (!(o instanceof Member))
            {
            return false;
            }
        
        Member member = getTheMember();
        return member != null && member.getId() == ((Member) o).getId();
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
    
    // Declared at the super level
    /**
     * Getter for property BitSet.<p>
    * The array of 32-bit integer values that hold the bit-set information.
     */
    public int getBitSet(int i)
        {
        // import Component.Net.Member;
        
        Member member = getTheMember();
        return member != null && i == member.getByteOffset() ? member.getByteMask() : 0;
        }
    
    // Declared at the super level
    /**
     * Getter for property BitSetCount.<p>
    * The number of 32-bit integer values that hold the bit-set information.
     */
    public int getBitSetCount()
        {
        // import Component.Net.Member;
        
        Member member = getTheMember();
        return member == null ? 0 : member.getByteOffset() + 1;
        }
    
    // Declared at the super level
    /**
     * Return the lowest member id contained in this member set, or 0 if empty.
     */
    public int getFirstId()
        {
        // import Component.Net.Member;
        
        Member member = getTheMember();
        return member != null ? member.getId() : 0;
        }
    
    // Declared at the super level
    /**
     * Return the highest member id contained in this member set, or 0 if empty.
     */
    public int getLastId()
        {
        // there's only one id, so first=last
        return getFirstId();
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
        
        Member member = getTheMember();
        return member != null && member.getId() == i ? member : null;
        }
    
    // Accessor for the property "TheMember"
    /**
     * Getter for property TheMember.<p>
     */
    public com.tangosol.coherence.component.net.Member getTheMember()
        {
        return __m_TheMember;
        }
    
    // Declared at the super level
    /**
     * Instantiate a MemberSet containing the specified member.  The resulting
    * MemberSet may not be iteratable.
    * Instantiate a SingleMemberSet containing the specified member
     */
    public static com.tangosol.coherence.component.net.MemberSet instantiate(com.tangosol.coherence.component.net.Member member)
        {
        SingleMemberSet set = new SingleMemberSet();
        set.setTheMember(member);
        return set;
        }
    
    // Declared at the super level
    public synchronized boolean isEmpty()
        {
        return getTheMember() == null;
        }
    
    // Accessor for the property "ReadOnly"
    /**
     * Getter for property ReadOnly.<p>
     */
    public boolean isReadOnly()
        {
        return __m_ReadOnly;
        }
    
    // Declared at the super level
    public java.util.Iterator iterator()
        {
        // import Component.Net.Member;
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.SimpleEnumerator;
        
        // Note: this implementation returns an iterator that does
        //       not support modification (remove() operation)
        Member member = getTheMember();
        if (member == null)
            {
            return NullImplementation.getIterator();
            }
        else
            {
            return new SimpleEnumerator(new Object[] {member});
            }
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
        readBarrier(getTheMember());
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
        // import Component.Net.Member;
        
        Member member = getTheMember();
        if (member != null && member.getId() == nId)
            {
            setTheMember(null);
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
        if (contains(o))
            {
            setTheMember(null);
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
        // import Component.Net.Member;
        
        Member member = getTheMember();
        if (member == null)
            {
            return false;
            }
        
        if (collection.contains(member))
            {
            setTheMember(null);
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Declared at the super level
    public synchronized boolean retainAll(java.util.Collection collection)
        {
        // import Component.Net.Member;
        
        Member member = getTheMember();
        if (member == null)
            {
            return false;
            }
        
        if (collection.contains(member))
            {
            return false;
            }
        else
            {
            setTheMember(null);
            return true;
            }
        }
    
    // Accessor for the property "ReadOnly"
    /**
     * Setter for property ReadOnly.<p>
     */
    public void setReadOnly(boolean fReadOnly)
        {
        // don't allow read-only to be reset
        _assert(!isReadOnly() || fReadOnly);
        
        __m_ReadOnly = (fReadOnly);
        }
    
    // Accessor for the property "TheMember"
    /**
     * Setter for property TheMember.<p>
     */
    public void setTheMember(com.tangosol.coherence.component.net.Member member)
        {
        check();
        __m_TheMember = (member);
        }
    
    // Declared at the super level
    public synchronized int size()
        {
        return getTheMember() == null ? 0 : 1;
        }
    
    // Declared at the super level
    public synchronized Object[] toArray()
        {
        // import Component.Net.Member;
        
        Member member = getTheMember();
        return member == null ? new Object[0] : new Object[] {member};
        }
    
    // Declared at the super level
    public synchronized Object[] toArray(Object[] ao)
        {
        // import Component.Net.Member;
        // import java.lang.reflect.Array;
        
        Member member = getTheMember();
        
        int    cNew   = member == null ? 0 : 1;
        int    cOld   = ao.length;
        
        if (cNew > cOld)
            {
            ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), cNew);
            }
        
        if (member != null)
            {
            ao[0] = member;
            }
        
        if (cOld > cNew)
            {
            ao[cNew] = null;
            }
        
        return ao;
        }
    
    // Declared at the super level
    /**
     * Ensure all writes made prior to this call to be visible to any thread
    * which calls the corresponding readBarrier method for any of the members
    * in this MemberSet.
     */
    public void writeBarrier()
        {
        writeBarrier(getTheMember());
        }
    
    // Declared at the super level
    /**
     * Write the MemberSet into the specified stream as a length-encoded array
    * of Member mini-ids.
     */
    public void writeFew(java.io.DataOutput stream)
            throws java.io.IOException
        {
        int cMembers = size();
        _assert(cMembers >= 0 && cMembers <= 1);
        stream.writeByte(cMembers);
        
        if (cMembers > 0)
            {
            stream.writeShort(getFirstId());
            }
        }
    
    // Declared at the super level
    /**
     * Write the MemberSet into the specified stream as a bit-set.
     */
    public void writeMany(java.io.DataOutput stream)
            throws java.io.IOException
        {
        throw new UnsupportedOperationException();
        }
    }
