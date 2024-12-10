
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.PointToPointModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * The LocalModel components operate in two distinct modes: live and snapshot.
 * In the live mode all model methods call corresponding methods on managed
 * objects. The snapshot mode uses the _SnapshotMap to keep the attribute
 * values.
 * 
 * Every time a remote invocation is used by the RemoteModel to do a
 * setAttribute or invoke call, the snapshot model is refreshed.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PointToPointModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _MemberSet
     *
     */
    private transient com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet __m__MemberSet;
    
    /**
     * Property _MemberSetRef
     *
     * WeakReference wrapping the cluster member set to avoid resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__MemberSetRef;
    
    /**
     * Property VIEW_WEAKEST
     *
     */
    public static final int VIEW_WEAKEST = 0;
    
    /**
     * Property ViewedMemberId
     *
     * The Id of the Member being viewed.  If set to 0 then view will be set to
     * the member identified by the WeakestChannel.
     */
    private int __m_ViewedMemberId;
    
    // Default constructor
    public PointToPointModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PointToPointModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            set_SnapshotMap(new java.util.HashMap());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
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
        return new com.tangosol.coherence.component.net.management.model.localModel.PointToPointModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/PointToPointModel".replace('/', '.'));
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
    
    // Accessor for the property "_MemberSet"
    /**
     * Getter for property _MemberSet.<p>
     */
    public com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet get_MemberSet()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_MemberSetRef();
        return wr == null ? null : (MasterMemberSet) wr.get();
        }
    
    // Accessor for the property "_MemberSetRef"
    /**
     * Getter for property _MemberSetRef.<p>
    * WeakReference wrapping the cluster member set to avoid resource leakage.
     */
    protected java.lang.ref.WeakReference get_MemberSetRef()
        {
        return __m__MemberSetRef;
        }
    
    // Accessor for the property "_ViewedFlowControl"
    /**
     * Helper method for accessing the viewed member's flowcontrol object.
     */
    public com.tangosol.coherence.component.net.Member.FlowControl get_ViewedFlowControl()
        {
        // import Component.Net.Member;
        
        Member member = get_ViewedMember();
        
        return member == null ? null : member.getFlowControl();
        }
    
    // Accessor for the property "_ViewedMember"
    /**
     * Helper method for accessing the viewed member.
     */
    public com.tangosol.coherence.component.net.Member get_ViewedMember()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        
        MasterMemberSet members = get_MemberSet();
        return members == null ? null : members.getMember(getViewedMemberId());
        }
    
    // Accessor for the property "DeferredPackets"
    /**
     * Getter for property DeferredPackets.<p>
    * The number of packets the vieweing member has batched up for the member
    * being viewed.  The viewing member has deferred sending these packets
    * while it waits for the viewing member to consume currently outstanding
    * packets.
     */
    public int getDeferredPackets()
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowcontrol = get_ViewedFlowControl();
        
        return flowcontrol == null ? -1 : flowcontrol.getDeferredPacketCount();
        }
    
    // Accessor for the property "LastIn"
    /**
     * Getter for property LastIn.<p>
    * The number of milliseconds since the viewing member last received a
    * packet from the viewed member.
     */
    public long getLastIn()
        {
        // import Component.Net.Member;
        // import com.tangosol.util.Base;
        
        Member member = get_ViewedMember();
        
        if (member == null)
            {
            return -1;
            }
        else
            {
            long ldtLast = member.getLastIncomingMillis();
            return ldtLast == 0L ? -1L : Base.getSafeTimeMillis() - ldtLast;
            }
        }
    
    // Accessor for the property "LastOut"
    /**
     * Getter for property LastOut.<p>
    * The number of milliseconds since the viewing member last sent a packet to
    * the viewed member.
     */
    public long getLastOut()
        {
        // import Component.Net.Member;
        // import com.tangosol.util.Base;
        
        Member member = get_ViewedMember();
        
        if (member == null)
            {
            return -1;
            }
        else
            {
            long ldtLast = member.getLastOutgoingMillis();
            return ldtLast == 0L ? -1L : Base.getSafeTimeMillis() - ldtLast;
            }
        }
    
    // Accessor for the property "LastSlow"
    /**
     * Getter for property LastSlow.<p>
    * The last the viewing member declared the viewing member as slow.
     */
    public long getLastSlow()
        {
        // import Component.Net.Member;
        // import com.tangosol.util.Base;
        
        Member member = get_ViewedMember();
        
        if (member == null)
            {
            return -1;
            }
        else
            {
            long ldtLast = member.getLastSlowMillis();
            return ldtLast == 0L ? -1L : Base.getSafeTimeMillis() - ldtLast;
            }
        }
    
    // Accessor for the property "OutstandingPackets"
    /**
     * Getter for property OutstandingPackets.<p>
    * The number of packets which the viewing member has sent to the viewed
    * member which have yet to be acknowledged.
     */
    public int getOutstandingPackets()
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowcontrol = get_ViewedFlowControl();
        
        return flowcontrol == null ? -1 : flowcontrol.getOutstandingPacketCount();
        }
    
    // Accessor for the property "PauseRate"
    /**
     * Getter for property PauseRate.<p>
    * The percentage of time, since last stats reset in which the viewing
    * member considered the viewed member to be unresponsive.
     */
    public float getPauseRate()
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowcontrol = get_ViewedFlowControl();
        
        return flowcontrol == null ? -1F : (float) flowcontrol.getStatsPauseRate();
        }
    
    // Accessor for the property "PublisherSuccessRate"
    /**
     * Getter for property PublisherSuccessRate.<p>
    * The publisher success rate from the viewing node to the viewed node since
    * the statistics were last reset.
     */
    public float getPublisherSuccessRate()
        {
        // import Component.Net.Member;
        
        Member member = get_ViewedMember();
        
        return member == null ? -1F : (float) member.getStatsPublisherSuccessRate();
        }
    
    // Accessor for the property "ReceiverSuccessRate"
    /**
     * Getter for property ReceiverSuccessRate.<p>
    * The receiver success rate from the viewing node to the viewed node since
    * the statistics were last reset.
     */
    public float getReceiverSuccessRate()
        {
        // import Component.Net.Member;
        
        Member member = get_ViewedMember();
        
        return member == null ? -1F : (float) member.getStatsReceiverSuccessRate();
        }
    
    // Accessor for the property "Threshold"
    /**
     * Getter for property Threshold.<p>
    * The packet threshold the viewing member is using when sending packets to
    * the viewed member.
     */
    public int getThreshold()
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowcontrol = get_ViewedFlowControl();
        
        return flowcontrol == null ? -1 : flowcontrol.getOutstandingPacketThreshold();
        }
    
    // Accessor for the property "ViewedMemberId"
    /**
     * Getter for property ViewedMemberId.<p>
    * The Id of the Member being viewed.  If set to 0 then view will be set to
    * the member identified by the WeakestChannel.
     */
    public int getViewedMemberId()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        
        MasterMemberSet setMembers = get_MemberSet();
        if (setMembers == null)
            {
            return -1;
            }
        
        int nId = __m_ViewedMemberId;
        if (nId == 0)
            {
            Member memberWorst = Member.findWeakestMember(setMembers);
            if (memberWorst != null)
                {
                nId = memberWorst.getId();
                }
            }
        return nId;
        }
    
    // Accessor for the property "ViewerStatistics"
    /**
     * Getter for property ViewerStatistics.<p>
    * Point-to-point statistics from a individual node`s point of view.  These
    * statistics are indented to aid in advanced diagnostics of network related
    * issues.
     */
    public String[] getViewerStatistics()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import java.util.Iterator;
        
        MasterMemberSet members = get_MemberSet();
        if (members == null)
            {
            return new String[0];
            }
        
        int      cMembers   = members.size() - 1;
        Member   memberThis = members.getThisMember();
        String[] asStats    = new String[cMembers];
        Iterator iter       = members.iterator();
        for (int i = 0; i < cMembers && iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (member != memberThis && member != null)
                {
                asStats[i++] = "Member=" + member.getId() +", " + member.formatStats();
                }
            }
        
        return asStats;
        }
    
    // Accessor for the property "Deferring"
    /**
     * Getter for property Deferring.<p>
    * Indicates if the viewing member is deferring packets to viewed member.
     */
    public boolean isDeferring()
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowcontrol = get_ViewedFlowControl();
        
        return flowcontrol == null ? false : flowcontrol.isDeferring();
        }
    
    // Accessor for the property "Paused"
    /**
     * Getter for property Paused.<p>
    * Indicates if the viewing member currently considers the viewed member to
    * be unresponsive.
     */
    public boolean isPaused()
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowcontrol = get_ViewedFlowControl();
        
        return flowcontrol == null ? false : flowcontrol.isPaused();
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("ViewedMemberId", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ViewerStatistics", ExternalizableHelper.readStringArray(in));
        mapSnapshot.put("DeferredPackets", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("Deferring", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("LastIn", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("LastOut", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("LastSlow", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OutstandingPackets", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("Paused", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("PauseRate", Float.valueOf(in.readFloat()));
        mapSnapshot.put("PublisherSuccessRate", Float.valueOf(in.readFloat()));
        mapSnapshot.put("ReceiverSuccessRate", Float.valueOf(in.readFloat()));
        mapSnapshot.put("Threshold", Base.makeInteger(ExternalizableHelper.readInt(in)));
        }
    
    /**
     * Reset the vieweing member's point-to-point statistics.
     */
    public void resetStatistics()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        
        MasterMemberSet setMembers = get_MemberSet();
        if (setMembers == null)
            {
            return;
            }
        
        try
            {
            for (Iterator iter = setMembers.iterator();
                     iter.hasNext(); )
                {
                Member member = (Member) iter.next();
                if (member != null)
                    {
                    member.resetStats();
                    }
                }
            }
        catch (ConcurrentModificationException e)
            {
            // just reporting stats, eat the exception
            }
        }
    
    // Accessor for the property "_MemberSet"
    /**
     * Setter for property _MemberSet.<p>
     */
    public void set_MemberSet(com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet p_MemberSet)
        {
        // import java.lang.ref.WeakReference;
        
        set_MemberSetRef(new WeakReference(p_MemberSet));
        }
    
    // Accessor for the property "_MemberSetRef"
    /**
     * Setter for property _MemberSetRef.<p>
    * WeakReference wrapping the cluster member set to avoid resource leakage.
     */
    protected void set_MemberSetRef(java.lang.ref.WeakReference refMemberSet)
        {
        __m__MemberSetRef = refMemberSet;
        }
    
    // Accessor for the property "ViewedMemberId"
    /**
     * Setter for property ViewedMemberId.<p>
    * The Id of the Member being viewed.  If set to 0 then view will be set to
    * the member identified by the WeakestChannel.
     */
    public void setViewedMemberId(int nMemberId)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        
        MasterMemberSet setMembers = get_MemberSet();
        if (setMembers == null)
            {
            return;
            }
        
        // test if a valid id was supplied
        if (nMemberId == 0 || setMembers.contains(nMemberId))
            {
            __m_ViewedMemberId = (nMemberId);
            }
        else
            {
            throw new IllegalArgumentException("There is currently no cluster member with id "
                 + nMemberId + ".");
            }
        }
    
    /**
     * Change the outstanding packet threshold which the vieweing member is
    * using for the viewed member.
     */
    public void specifyThreshold(int iThreshold)
        {
        }
    
    /**
     * Instruct the PointToPointMBean to set the view to track the weakest
    * member.
     */
    public void trackWeakest()
        {
        setViewedMemberId(VIEW_WEAKEST);
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        super.writeExternal(out);
        
        ExternalizableHelper.writeInt(out, getViewedMemberId());
        ExternalizableHelper.writeStringArray(out, getViewerStatistics());
        ExternalizableHelper.writeInt(out, getDeferredPackets());
        out.writeBoolean(isDeferring());
        ExternalizableHelper.writeLong(out, getLastIn());
        ExternalizableHelper.writeLong(out, getLastOut());
        ExternalizableHelper.writeLong(out, getLastSlow());
        ExternalizableHelper.writeInt(out, getOutstandingPackets());
        out.writeBoolean(isPaused());
        out.writeFloat(getPauseRate());
        out.writeFloat(getPublisherSuccessRate());
        out.writeFloat(getReceiverSuccessRate());
        ExternalizableHelper.writeInt(out, getThreshold());
        }
    }
