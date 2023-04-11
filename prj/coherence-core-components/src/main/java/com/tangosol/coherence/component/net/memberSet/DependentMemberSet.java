
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.memberSet.DependentMemberSet

package com.tangosol.coherence.component.net.memberSet;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Packet;
import com.tangosol.util.Base;
import java.lang.reflect.Array;

/**
 * Set of Member objects; must be thread safe.
 * 
 * Requires BaseSet to be configured before use.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DependentMemberSet
        extends    com.tangosol.coherence.component.net.MemberSet
        implements Cloneable
    {
    // ---- Fields declarations ----
    
    /**
     * Property BaseSet
     *
     * The underlying MemberSet upon which this MemberSet is based. This
     * MemberSet is a sub-set of the base MemberSet.
     */
    private com.tangosol.coherence.component.net.MemberSet __m_BaseSet;
    
    /**
     * Property DestinationMessageId
     *
     * (Ambient.) An array, indexed by Member mini-id, of point-to-point
     * Message ids. This property is used by Messages and Directed Packets to
     * map destination Members to their corresponding Message id that they
     * identify incoming Message Packets with.
     */
    private int[] __m_DestinationMessageId;
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
    public DependentMemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DependentMemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.memberSet.DependentMemberSet();
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
            clz = Class.forName("com.tangosol.coherence/component/net/memberSet/DependentMemberSet".replace('/', '.'));
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
     * Adds all of the BaseSet's Members to this MemberSet.
     */
    public synchronized boolean addAll()
        {
        return addAll(getBaseSet());
        }
    
    // Declared at the super level
    public Object clone()
        {
        // import Component.Net.Member;
        // import com.tangosol.util.Base;
        
        DependentMemberSet that;
        try
            {
            that = (DependentMemberSet) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        
        // deep copy the indexed properties
        Member[] aMember = getMember();
        if (aMember != null)
            {
            that.setMember((Member[]) aMember.clone());
            }
        
        int[] aBits = getBitSet();
        if (aBits != null)
            {
            that.setBitSet((int[]) aBits.clone());
            }
        
        return that;
        }
    
    // Declared at the super level
    public synchronized boolean contains(int nId)
        {
        return super.contains(nId) && getBaseSet().contains(nId);
        }
    
    // Declared at the super level
    public synchronized boolean contains(Object o)
        {
        return super.contains(o) && getBaseSet().contains(o);
        }
    
    // Declared at the super level
    public synchronized boolean containsAll(java.util.Collection collection)
        {
        sync();
        return super.containsAll(collection);
        }
    
    // Accessor for the property "BaseSet"
    /**
     * Getter for property BaseSet.<p>
    * The underlying MemberSet upon which this MemberSet is based. This
    * MemberSet is a sub-set of the base MemberSet.
     */
    public com.tangosol.coherence.component.net.MemberSet getBaseSet()
        {
        return __m_BaseSet;
        }
    
    // Accessor for the property "DestinationMessageId"
    /**
     * Getter for property DestinationMessageId.<p>
    * (Ambient.) An array, indexed by Member mini-id, of point-to-point Message
    * ids. This property is used by Messages and Directed Packets to map
    * destination Members to their corresponding Message id that they identify
    * incoming Message Packets with.
     */
    protected int[] getDestinationMessageId()
        {
        return __m_DestinationMessageId;
        }
    
    // Accessor for the property "DestinationMessageId"
    /**
     * Getter for property DestinationMessageId.<p>
    * (Ambient.) An array, indexed by Member mini-id, of point-to-point Message
    * ids. This property is used by Messages and Directed Packets to map
    * destination Members to their corresponding Message id that they identify
    * incoming Message Packets with.
     */
    public int getDestinationMessageId(int i)
        {
        int[] an = getDestinationMessageId();
        return an != null && i < an.length ? an[i] : 0;
        }
    
    // Declared at the super level
    /**
     * Getter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    public com.tangosol.coherence.component.net.Member getMember(int i)
        {
        return getBaseSet().getMember(i);
        }
    
    // Declared at the super level
    public synchronized boolean isEmpty()
        {
        if (getBaseSet().isEmpty())
            {
            clear();
            return true;
            }
        
        sync();
        return super.isEmpty();
        }
    
    // Accessor for the property "BaseSet"
    /**
     * Setter for property BaseSet.<p>
    * The underlying MemberSet upon which this MemberSet is based. This
    * MemberSet is a sub-set of the base MemberSet.
     */
    public void setBaseSet(com.tangosol.coherence.component.net.MemberSet set)
        {
        _assert(getBaseSet() == null && set != null);
        __m_BaseSet = (set);
        }
    
    // Accessor for the property "DestinationMessageId"
    /**
     * Setter for property DestinationMessageId.<p>
    * (Ambient.) An array, indexed by Member mini-id, of point-to-point Message
    * ids. This property is used by Messages and Directed Packets to map
    * destination Members to their corresponding Message id that they identify
    * incoming Message Packets with.
     */
    protected void setDestinationMessageId(int[] an)
        {
        __m_DestinationMessageId = an;
        }
    
    // Accessor for the property "DestinationMessageId"
    /**
     * Setter for property DestinationMessageId.<p>
    * (Ambient.) An array, indexed by Member mini-id, of point-to-point Message
    * ids. This property is used by Messages and Directed Packets to map
    * destination Members to their corresponding Message id that they identify
    * incoming Message Packets with.
     */
    public void setDestinationMessageId(int i, int nId)
        {
        int[] an = getDestinationMessageId();
        
        if (an == null || i >= an.length)
            {
            // resize, making the array bigger than necessary (avoid resizes)
            int   cNew  = Math.max(i + (i >>> 1), i + 4);
            int[] anNew = new int[cNew];
        
            // copy original data
            if (an != null)
                {
                System.arraycopy(an, 0, anNew, 0, an.length);
                }
        
            setDestinationMessageId(an = anNew);
            }
        
        an[i] = nId;
        }
    
    // Declared at the super level
    public synchronized int size()
        {
        sync();
        return super.size();
        }
    
    /**
     * Updates this MemberSet's information such that it is a subset of the
    * BaseSet.
     */
    public void sync()
        {
        retainAll(getBaseSet());
        }
    
    // Declared at the super level
    public synchronized Object[] toArray(Object[] ao)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import java.lang.reflect.Array;
        // import java.util.Iterator as java.util.Iterator;
        
        MemberSet setBase = getBaseSet();
        synchronized (setBase)
            {
            // create the array to store the set contents
            int c = size();
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
        
            // go through all the base members and accumulate those
            // that are also contained by this set
            int of = 0;
            for (java.util.Iterator iter = setBase.iterator(); iter.hasNext(); )
                {
                Member member = (Member) iter.next();
                if (contains(member))
                    {
                    ao[of++] = member;
                    }
                }
        
            return ao;
            }
        }
    
    /**
     * Write this MemberSet and the destination Message id for the several
    * Members  the specified BufferOutput.
    * 
    * @param output  the BufferOutput to write to
     */
    public void writeFewWithMessageId(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Net.Packet;
        
        // before the intrinsic bitset is serialized, ensure that it is
        // synchronized with any previous mutation. Size will as a side-effect
        // trim the bitset to the correct size.
        int cMembers = size();
        
        writeFew(output);
        
        _assert(cMembers <= 255);
        // [implicit] stream.writeByte(cMembers);
        
        if (cMembers > 0)
            {
            for (int i = 0, c = getBitSetCount(); i < c && cMembers > 0; ++i)
                {
                int n = getBitSet(i);
                if (n != 0)
                    {
                    int nBase = i << 5;
                    for (int of = 1, nMask = 1; of <= 32; ++of, nMask <<= 1)
                        {
                        if ((n & nMask) != 0)
                            {
                            int nMemberId  = nBase + of;
                            int nMessageTo = getDestinationMessageId(nMemberId);
                            // this assertion is only for short-test debugging;
                            // the trint value in production will roll over to
                            // zero eventually (~16 million messages)
                            // _assert(nMessageTo != 0);
                            Packet.writeTrint(output, nMessageTo);
                            --cMembers;
                            }
                        }
                    }
                }
            }
        }
    
    /**
     * Write this MemberSet and the destination Message id for the several
    * Members as a length-encoded array of trints to the specified
    * BufferOutput.
    * 
    * @param output  the BufferOutput to write to
     */
    public void writeManyWithMessageId(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Net.Packet;
        
        // before the intrinsic bitset is serialized, ensure that it is
        // synchronized with any previous mutation. Size will as a side-effect
        // trim the bitset to the correct size.
        int cMembers = size();
        
        writeMany(output);
        
        output.writeShort(cMembers);
        
        if (cMembers > 0)
            {
            for (int i = 0, c = getBitSetCount(); i < c && cMembers > 0; ++i)
                {
                int n = getBitSet(i);
                if (n != 0)
                    {
                    int nBase = i << 5;
                    for (int of = 1, nMask = 1; of <= 32; ++of, nMask <<= 1)
                        {
                        if ((n & nMask) != 0)
                            {
                            int nMemberId  = nBase + of;
                            int nMessageTo = getDestinationMessageId(nMemberId);
                            // this assertion is only for short-test debugging;
                            // the trint value in production will roll over to
                            // zero eventually (~16 million messages)
                            // _assert(nMessageTo != 0);
                            Packet.writeTrint(output, nMessageTo);
                            --cMembers;
                            }
                        }
                    }
                }
            }
        }
    
    /**
     * Write this MemberSet and the destination  Message id for the one Member
    * contained to the specified BufferOutput.
    * 
    * @param output  the BufferOutput to write to
     */
    public void writeOneWithMessageId(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Net.Packet;
        
        writeOne(output);
        
        int nMemberTo = getFirstId();
        _assert(nMemberTo != 0);
        int nMessageTo = getDestinationMessageId(nMemberTo);
        // this assertion is only for short-test debugging;
        // the trint value in production will roll over to
        // zero eventually (~16 million messages)
        // _assert(nMessageTo != 0);
        
        Packet.writeTrint(output, nMessageTo);
        }
    }
