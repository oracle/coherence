
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.IpMonitor

package com.tangosol.coherence.component.util.daemon;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.memberSet.ActualMemberSet;
import com.oracle.coherence.common.util.Duration;
import com.tangosol.util.Base;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The IpMonitor service monitors machine addresses of other Cluster members to
 * quickly detect loss of connectivity.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class IpMonitor
        extends    com.tangosol.coherence.component.util.Daemon
    {
    // ---- Fields declarations ----
    
    /**
     * Property AddressScanArray
     *
     * An array of the InetAddress objects to be scanned for reachability.
     * 
     * @volatile - set on the ClusterService thread, read on IpMonitor thread
     */
    private volatile transient java.net.InetAddress[] __m_AddressScanArray;
    
    /**
     * Property AddressTimeout
     *
     */
    private long __m_AddressTimeout;
    
    /**
     * Property CollocatedMembersMap
     *
     * Cached map of InetAddress to MemberSet located at that address; it
     * reflects current ClusterMemberSet.
     * 
     * @volatile
     */
    private volatile java.util.Map __m_CollocatedMembersMap;
    
    /**
     * Property CurrentTimeoutMillis
     *
     * The datetime after which the current address should be considered
     * timed-out
     */
    private long __m_CurrentTimeoutMillis;
    
    /**
     * Property LocalInterface
     *
     * The NetworkInterface to utilize, or null for default.
     */
    private transient java.net.NetworkInterface __m_LocalInterface;
    
    /**
     * Property MachineSeniorMember
     *
     * @volatile
     * 
     * Contains the current senior member on this machine. First initialized by
     * ClusterService but may change by onMemberLeft.
     */
    private volatile com.tangosol.coherence.component.net.Member __m_MachineSeniorMember;
    
    /**
     * Property PingTimeout
     *
     * The timeout for each "ping" attempt.
     * 
     * In the Windows TCP/IP implementation, SYN packets are retransmitted a
     * number of times in response to the RST packet by default so a slightly
     * longer wait cycle is required to detect the availabilty an IP address on
     * Windows platforms than on other platforms.
     * 
     * Two seconds appears to be sufficient.  
     */
    private int __m_PingTimeout;
    
    /**
     * Property Position
     *
     * The offset within the IpMonitorScanArray from which to start the next
     * sampling.
     */
    private transient int __m_Position;
    
    /**
     * Property Service
     *
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService __m_Service;
    
    /**
     * Property StatsTimeouts
     *
     * Statistics: total number of address timeouts.
     */
    private long __m_StatsTimeouts;
    
    /**
     * Property Suspect
     *
     * The InetAddress that is currently being monitored and is "suspect".
     */
    private java.net.InetAddress __m_Suspect;
    
    // Default constructor
    public IpMonitor()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public IpMonitor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        _addChild(new IpMonitor.Guard("Guard", this, true), "Guard");
        
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
        return new com.tangosol.coherence.component.util.daemon.IpMonitor();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/IpMonitor".replace('/', '.'));
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
    
    /**
     * Advance the Ip being monitored to the next address in the scan array.
     */
    protected void advanceIpMonitor()
        {
        // import java.net.InetAddress;
        
        setSuspect(null);
        setCurrentTimeoutMillis(0L);
        
        InetAddress[] aAddr = getAddressScanArray();
        if (aAddr.length == 0)
            {
            setPosition(0);
            }
        else
            {
            setPosition((getPosition() + 1) % aAddr.length);
            }
        }
    
    protected java.net.InetAddress[] calculateAddressScanArray(java.util.Set setAddress)
        {
        // import com.tangosol.util.Base;
        // import java.net.InetAddress;
        
        // no point in monitoring our machine
        setAddress.remove(getThisMember().getAddress());
        
        // randomize the order of addresses
        int cAddr = setAddress.size();
        return (InetAddress[]) Base.randomize(
            setAddress.toArray(new InetAddress[cAddr]));
        }
    
    /**
     * Configure the IpMonitor.
     */
    public void configure(com.tangosol.net.ClusterDependencies config, com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService service)
        {
        setService(service);
        setWaitMillis(config.getClusterHeartbeatDelayMillis());
        
        // sanitize the configured parameters
        int cPingTimeout      = (int) config.getIpMonitorTimeoutMillis();
        int cAttempts         = Math.max(config.getIpMonitorAttempts(), 1);
        int cIpMonitorDefault = 5000;
        
        setPingTimeout   (cPingTimeout);
        setAddressTimeout(cAttempts * cPingTimeout);
        
        if (cPingTimeout < cIpMonitorDefault)
            {
            _trace("The timeout value configured for IpMonitor pings is shorter than the"
                 + " value of 5 seconds. Short ping timeouts may cause an IP address to be"
                 + " wrongly reported as unreachable on some platforms.", 2);
            }
        }
    
    /**
     * Initializes the machine senior member. If then current member is
    * determined to be the machine senior member also initializes the address
    * scan array.
    * 
    * Called from the ClusterService when the service joined the cluster or by
    * onMemberLeft when the machine senior departs.
     */
    public void ensureSeniority()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Member    memberThis   = getThisMember();
        Member    memberSenior = memberThis; // local senior
        Map       mapAddress   = groupMembersByAddress(getServiceMemberSet());
        MemberSet setLocal     = (MemberSet) mapAddress.get(memberThis.getAddress());
        
        for (Iterator iter = setLocal.iterator(); iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (member.getTimestamp() < memberSenior.getTimestamp())
                {
                memberSenior = member;
                }
            }
        
        setMachineSeniorMember(memberSenior);
        if (memberSenior == memberThis)
            {
            // initialize the address scan array and address map for the new machine senior
            setCollocatedMembersMap(mapAddress);
            setAddressScanArray(calculateAddressScanArray(mapAddress.keySet()));
            }
        }
    
    public String formatStats()
        {
        return "Timeouts=" + getStatsTimeouts();
        }
    
    // Accessor for the property "AddressScanArray"
    /**
     * Getter for property AddressScanArray.<p>
    * An array of the InetAddress objects to be scanned for reachability.
    * 
    * @volatile - set on the ClusterService thread, read on IpMonitor thread
     */
    private java.net.InetAddress[] getAddressScanArray()
        {
        return __m_AddressScanArray;
        }
    
    // Accessor for the property "AddressTimeout"
    /**
     * Getter for property AddressTimeout.<p>
     */
    public long getAddressTimeout()
        {
        return __m_AddressTimeout;
        }
    
    // Accessor for the property "CollocatedMembersMap"
    /**
     * Getter for property CollocatedMembersMap.<p>
    * Cached map of InetAddress to MemberSet located at that address; it
    * reflects current ClusterMemberSet.
    * 
    * @volatile
     */
    public java.util.Map getCollocatedMembersMap()
        {
        return __m_CollocatedMembersMap;
        }
    
    // Accessor for the property "CurrentAddress"
    /**
     * Getter for property CurrentAddress.<p>
    * The current InetAddress to monitor.
     */
    public java.net.InetAddress getCurrentAddress()
        {
        // import com.tangosol.util.Base;
        // import java.net.InetAddress;
        
        InetAddress addr = getSuspect();
        if (addr == null)
            {
            InetAddress[] aAddr = getAddressScanArray();
            int           cAddr = aAddr.length;
            if (cAddr > 0)
                {
                // mod the position against the array length as the array
                // could shrink due to topology change (see onMemberLeft)
                addr = aAddr[getPosition() % cAddr];
                if (getCurrentTimeoutMillis() == 0L)
                    {
                    setCurrentTimeoutMillis(Base.getSafeTimeMillis() + getAddressTimeout());
                    }
                }
            }
        
        return addr;
        }
    
    // Accessor for the property "CurrentTimeoutMillis"
    /**
     * Getter for property CurrentTimeoutMillis.<p>
    * The datetime after which the current address should be considered
    * timed-out
     */
    public long getCurrentTimeoutMillis()
        {
        return __m_CurrentTimeoutMillis;
        }
    
    // Accessor for the property "LocalInterface"
    /**
     * Getter for property LocalInterface.<p>
    * The NetworkInterface to utilize, or null for default.
     */
    public java.net.NetworkInterface getLocalInterface()
        {
        return __m_LocalInterface;
        }
    
    // Accessor for the property "MachineSeniorMember"
    /**
     * Getter for property MachineSeniorMember.<p>
    * @volatile
    * 
    * Contains the current senior member on this machine. First initialized by
    * ClusterService but may change by onMemberLeft.
     */
    protected com.tangosol.coherence.component.net.Member getMachineSeniorMember()
        {
        return __m_MachineSeniorMember;
        }
    
    // Accessor for the property "PingTimeout"
    /**
     * Getter for property PingTimeout.<p>
    * The timeout for each "ping" attempt.
    * 
    * In the Windows TCP/IP implementation, SYN packets are retransmitted a
    * number of times in response to the RST packet by default so a slightly
    * longer wait cycle is required to detect the availabilty an IP address on
    * Windows platforms than on other platforms.
    * 
    * Two seconds appears to be sufficient.  
     */
    public int getPingTimeout()
        {
        return __m_PingTimeout;
        }
    
    // Accessor for the property "Position"
    /**
     * Getter for property Position.<p>
    * The offset within the IpMonitorScanArray from which to start the next
    * sampling.
     */
    private int getPosition()
        {
        return __m_Position;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService getService()
        {
        return __m_Service;
        }
    
    // Accessor for the property "ServiceMemberSet"
    /**
     * Getter for property ServiceMemberSet.<p>
    * Calculated helper
     */
    public com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet getServiceMemberSet()
        {
        return getService().getServiceMemberSet();
        }
    
    // Accessor for the property "StatsTimeouts"
    /**
     * Getter for property StatsTimeouts.<p>
    * Statistics: total number of address timeouts.
     */
    public long getStatsTimeouts()
        {
        return __m_StatsTimeouts;
        }
    
    // Accessor for the property "Suspect"
    /**
     * Getter for property Suspect.<p>
    * The InetAddress that is currently being monitored and is "suspect".
     */
    private java.net.InetAddress getSuspect()
        {
        return __m_Suspect;
        }
    
    // Accessor for the property "ThisMember"
    /**
     * Getter for property ThisMember.<p>
    * Calculated helper
     */
    public com.tangosol.coherence.component.net.Member getThisMember()
        {
        // import Component.Net.Member;
        
        Member member = getService().getThisMember();
        return member == null
            ? getService().getAnnounceMember() // not yet joined;
            : member;
        }
    
    /**
     * Return a map, keyed by InetAddress, of MembersSet of members that are on
    * that address.
     */
    protected java.util.Map groupMembersByAddress(com.tangosol.coherence.component.net.MemberSet setMembers)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet;
        // import java.net.InetAddress;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Map mapByAddress = new HashMap();
        for (Iterator iter = setMembers.iterator(); iter.hasNext(); )
            {
            Member      member    = (Member) iter.next();
            InetAddress addr      = member.getAddress();
            MemberSet   setByAddr = (MemberSet) mapByAddress.get(addr);
            if (setByAddr == null)
                {
                setByAddr = new ActualMemberSet();
                mapByAddress.put(addr, setByAddr);
                }
        
            setByAddr.add(member);
            }
        
        return mapByAddress;
        }
    
    // Declared at the super level
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable, long cMillis, float flPctRecover)
        {
        // "inner-circle" guardables are all logically associated with ClusterService:
        // (ClusterService->TcpRingListener->IpMonitor)
        guardable = getService().instantiateWrapperGuardable(guardable);
        
        return super.guard(guardable, cMillis, flPctRecover);
        }
    
    // Accessor for the property "MachineSenior"
    /**
     * Getter for property MachineSenior.<p>
    * Determines if the current member is the machine senior member.
    * 
    * @returns true iff this member is the senior member on the current machine
     */
    public boolean isMachineSenior()
        {
        // import com.tangosol.util.Base;
        
        return Base.equals(getMachineSeniorMember(), getThisMember());
        }
    
    /**
     * Test if the specified address can be reached within the ping timeout.
    * 
    * @param addr                   the address to test
    * @param cMillisTimeout  the ping timeout
     */
    protected boolean isReachable(java.net.InetAddress addr, int cMillisTimeout)
        {
        // import java.io.IOException;
        // import java.net.NetworkInterface;
        
        try
            {
            NetworkInterface nicSrc = getLocalInterface();
            if (addr.isReachable(nicSrc, /*ttl*/ 0, cMillisTimeout))
                {
                return true;
                }
        
            // after each ping attempt failure we switch whether or not we will specify a source interface
            // for future pings.  The reason is that there are cases where either approach may incorrectly
            // identify the destination as "unreachable".
            // For instance when no source interface is specified and there are potentially
            // many routes to the destination the routing table could choose a different route then TCMP is
            // using.  Alternatlivey when a source interface is specified but there are multiple IPs associated
            // with that interface the JVM may choose an IP which can't route to the destination.
        
            setLocalInterface(nicSrc == null ? NetworkInterface.getByInetAddress(getThisMember().getAddress()) : null);
            }
        catch (IOException e)
            {
            // an exception from isReachable() is treated as a local failure
            _trace("Network failure encountered during InetAddress.isReachable(): " +
                   getStackTrace(e), 3);
            }
        
        return false;
        }
    
    // Declared at the super level
    /**
     * Event notification called once the daemon's thread starts and before the
    * daemon thread goes into the "wait - perform" loop. Unlike the
    * <code>onInit()</code> event, this method executes on the daemon's thread.
    * 
    * Note1: this method is called while the caller's thread is still waiting
    * for a notification to  "unblock" itself.
    * Note2: any exception thrown by this method will terminate the thread
    * immediately
     */
    protected void onEnter()
        {
        resetStats();
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import java.net.InetAddress;
        
        super.onInit();
        
        setAddressScanArray(new InetAddress[0]);
        }
    
    /**
     * Called by the ClusterService when member has joined the cluster.
     */
    public void onMemberJoined(com.tangosol.coherence.component.net.Member memberJoin)
        {
        // import Component.Net.MemberSet;
        // import java.util.Map;
        
        if (isMachineSenior())
            {
            Map       mapByAddress  = groupMembersByAddress(getServiceMemberSet());
            MemberSet setCollocated = (MemberSet) mapByAddress.get(memberJoin.getAddress());
        
            setCollocatedMembersMap(mapByAddress);
            
            if (setCollocated != null && setCollocated.size() == 1)
                {
                // the newly joined member is the first member on that machine;
                // update the address scan list
                setAddressScanArray(calculateAddressScanArray(mapByAddress.keySet()));
                }
            }
        }
    
    /**
     * Called by the ClusterService when member has left the cluster.
     */
    public void onMemberLeft(com.tangosol.coherence.component.net.Member memberLeft)
        {
        // import Component.Net.MemberSet;
        // import java.util.Map;
        
        if (memberLeft.equals(getMachineSeniorMember()))
            {
            ensureSeniority();
            }
        else if (isMachineSenior())
            {
            Map       mapByAddress  = groupMembersByAddress(getServiceMemberSet());
            MemberSet setCollocated = (MemberSet) mapByAddress.get(memberLeft.getAddress());
        
            setCollocatedMembersMap(mapByAddress);
              
            if (setCollocated == null || setCollocated.isEmpty())
                {
                // the departed member was the last member on that machine;
                // update the address scan list
                setAddressScanArray(calculateAddressScanArray(mapByAddress.keySet()));
                }
            }
        }
    
    // Declared at the super level
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        // import com.tangosol.util.Base;
        // import java.net.InetAddress;
        
        InetAddress addr;
        int         cIter = (int) Math.ceil(getAddressTimeout() / getPingTimeout());
        while ((addr = getCurrentAddress()) != null && --cIter > 0)
            {
            if (verifyReachable(addr))
                {
                // the address was reachable; advance to the next
                // address in the monitor list and go into a wait.
                advanceIpMonitor();
                return;
                }
            else
                {
                // the address was not reachable.  Unreachability is
                // determined by a timeout during connect, so there is
                // no need to wait before the next ping attempt.
                if (Base.getSafeTimeMillis() > getCurrentTimeoutMillis())
                    {
                    // the machine has crossed the allowable tolerance; notify
                    // the cluster service of the ip timeout, and move on to
                    // the next address in the list
                    getService().doNotifyIpTimeout(addr);
                    setStatsTimeouts(getStatsTimeouts() + 1);
        
                    advanceIpMonitor();
                    }
                else
                    {
                    setSuspect(addr);
                    }
        
                // heartbeat the guardian between each 'ping' attempt
                heartbeat();
                }
            }
        }
    
    // Declared at the super level
    /**
     * Event notification called when  the daemon's Thread is waiting for work.
    * 
    * @see #run
     */
    protected void onWait()
            throws java.lang.InterruptedException
        {
        // heartbeat the guardian before going into a wait
        heartbeat();
        
        super.onWait();
        }
    
    public void resetStats()
        {
        setStatsTimeouts(0L);
        }
    
    // Accessor for the property "AddressScanArray"
    /**
     * Setter for property AddressScanArray.<p>
    * An array of the InetAddress objects to be scanned for reachability.
    * 
    * @volatile - set on the ClusterService thread, read on IpMonitor thread
     */
    private void setAddressScanArray(java.net.InetAddress[] pAddressScanArray)
        {
        __m_AddressScanArray = pAddressScanArray;
        }
    
    // Accessor for the property "AddressTimeout"
    /**
     * Setter for property AddressTimeout.<p>
     */
    public void setAddressTimeout(long pAddressTimeout)
        {
        __m_AddressTimeout = pAddressTimeout;
        }
    
    // Accessor for the property "CollocatedMembersMap"
    /**
     * Setter for property CollocatedMembersMap.<p>
    * Cached map of InetAddress to MemberSet located at that address; it
    * reflects current ClusterMemberSet.
    * 
    * @volatile
     */
    protected void setCollocatedMembersMap(java.util.Map mapMembers)
        {
        __m_CollocatedMembersMap = mapMembers;
        }
    
    // Accessor for the property "CurrentTimeoutMillis"
    /**
     * Setter for property CurrentTimeoutMillis.<p>
    * The datetime after which the current address should be considered
    * timed-out
     */
    public void setCurrentTimeoutMillis(long pCurrentTimeoutMillis)
        {
        __m_CurrentTimeoutMillis = pCurrentTimeoutMillis;
        }
    
    // Accessor for the property "LocalInterface"
    /**
     * Setter for property LocalInterface.<p>
    * The NetworkInterface to utilize, or null for default.
     */
    public void setLocalInterface(java.net.NetworkInterface interfaceLocal)
        {
        __m_LocalInterface = interfaceLocal;
        }
    
    // Accessor for the property "MachineSeniorMember"
    /**
     * Setter for property MachineSeniorMember.<p>
    * @volatile
    * 
    * Contains the current senior member on this machine. First initialized by
    * ClusterService but may change by onMemberLeft.
     */
    protected void setMachineSeniorMember(com.tangosol.coherence.component.net.Member member)
        {
        __m_MachineSeniorMember = member;
        }
    
    // Accessor for the property "PingTimeout"
    /**
     * Setter for property PingTimeout.<p>
    * The timeout for each "ping" attempt.
    * 
    * In the Windows TCP/IP implementation, SYN packets are retransmitted a
    * number of times in response to the RST packet by default so a slightly
    * longer wait cycle is required to detect the availabilty an IP address on
    * Windows platforms than on other platforms.
    * 
    * Two seconds appears to be sufficient.  
     */
    public void setPingTimeout(int pPingTimeout)
        {
        __m_PingTimeout = pPingTimeout;
        }
    
    // Accessor for the property "Position"
    /**
     * Setter for property Position.<p>
    * The offset within the IpMonitorScanArray from which to start the next
    * sampling.
     */
    private void setPosition(int pPosition)
        {
        __m_Position = pPosition;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
     */
    public void setService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService pService)
        {
        __m_Service = pService;
        }
    
    // Accessor for the property "StatsTimeouts"
    /**
     * Setter for property StatsTimeouts.<p>
    * Statistics: total number of address timeouts.
     */
    public void setStatsTimeouts(long pStatsTimeouts)
        {
        __m_StatsTimeouts = pStatsTimeouts;
        }
    
    // Accessor for the property "Suspect"
    /**
     * Setter for property Suspect.<p>
    * The InetAddress that is currently being monitored and is "suspect".
     */
    private void setSuspect(java.net.InetAddress pSuspect)
        {
        __m_Suspect = pSuspect;
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.oracle.coherence.common.util.Duration;
        // import com.oracle.coherence.common.util.Duration$Magnitude as com.oracle.coherence.common.util.Duration.Magnitude;
        
        return isStarted()
            ? "IpMonitor{Addresses=" + getAddressScanArray().length + ", Timeout=" + new Duration(getAddressTimeout(), com.oracle.coherence.common.util.Duration.Magnitude.MILLI) + '}'
            : "IpMonitor is disabled";
        }
    
    /**
     * Verify that IpMonitor considers the specified address reachable.
     */
    public boolean verifyReachable(java.net.InetAddress addr)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import java.util.Map;
        
        // compute cutoff time which is just before the start of when we started to monitor this address
        long      ldtCutoff     = getCurrentTimeoutMillis() - (getAddressTimeout() + getWaitMillis());
        Map       mapByAddress  = getCollocatedMembersMap();
        MemberSet setCollocated = mapByAddress == null ? null : (MemberSet) mapByAddress.get(addr);
        
        if (wasReachable(setCollocated, ldtCutoff) || // "free" historical check
            isReachable(addr, getPingTimeout())    || // blocking active check
            wasReachable(setCollocated, ldtCutoff))   // double check after active check delay
            {
            return true;
            }
        else
            {
            getService().heartbeat(setCollocated); // prime for next historical check
            return false;
            }
        }
    
    /**
     * Verify that the IpMonitor daemon considers the specified member
    * reachable.
    * This method may be called on other threads.
    * 
    * @param member    the member to test connectivity
     */
    public boolean verifyReachable(com.tangosol.coherence.component.net.Member member)
        {
        return verifyReachable(member.getAddress());
        }
    
    /**
     * Return true if any of the specified members has been successfully
    * contacted since the specified time.
     */
    protected boolean wasReachable(com.tangosol.coherence.component.net.MemberSet setMember, long ldt)
        {
        // import Component.Net.Member;
        // import java.util.Iterator;
        
        if (setMember != null)
            {
            for (Iterator iter = setMember.iterator(); iter.hasNext(); )
                {
                if (((Member) iter.next()).getLastIncomingMillis() > ldt)
                    {
                    return true;
                    }
                }
            }
        
        return false;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.IpMonitor$Guard
    
    /**
     * Guard provides the Guardable interface implementation for the Daemon.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Guard
            extends    com.tangosol.coherence.component.util.Daemon.Guard
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public Guard()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Guard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            _addChild(new IpMonitor.Guard.StopIpMonitor("StopIpMonitor", this, true), "StopIpMonitor");
            
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
            return new com.tangosol.coherence.component.util.daemon.IpMonitor.Guard();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/IpMonitor$Guard".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        public void terminate()
            {
            // import com.tangosol.util.Base;
            
            // try to stop the IpMonitr
            final Thread t = Base.makeThread(
                null, (Runnable) _newChild("StopIpMonitor"), "StopIpMonitor");
            t.setDaemon(true);
            t.start();
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.IpMonitor$Guard$StopIpMonitor
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class StopIpMonitor
                extends    com.tangosol.coherence.Component
                implements Runnable
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public StopIpMonitor()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public StopIpMonitor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.IpMonitor.Guard.StopIpMonitor();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/IpMonitor$Guard$StopIpMonitor".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // From interface: java.lang.Runnable
            public void run()
                {
                ((IpMonitor) get_Module()).stop();
                }
            }
        }
    }
