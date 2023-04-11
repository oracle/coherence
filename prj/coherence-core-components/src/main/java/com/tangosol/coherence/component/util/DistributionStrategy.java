
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.DistributionStrategy

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
import com.tangosol.coherence.config.Config;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Base;

/**
 * Provides the strategy for distributing partitions among the
 * ownership-enabled members of a partitioned service.
 * 
 * @see #PartitionedService.transferPrimaries,
 * #PartitionedService.transferBackups
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class DistributionStrategy
        extends    com.tangosol.coherence.component.Util
    {
    // ---- Fields declarations ----
    
    /**
     * Property DistributionInProgress
     *
     * Indicates whether or not the service is in the progress of conducting
     * distribution.  Distribution is in progress if the service has a
     * currently active outgoing (sent but not yet answered) request. Not more
     * then one outgoing request could be active at any given time for the
     * service. 
     * 
     * Note: Only the service thread uses this property.
     */
    private boolean __m_DistributionInProgress;
    
    /**
     * Property DistributionsPendingStart
     *
     * The last time in milliseconds when the service started seeing pending
     * distributions which have not yet been completed, or zero if there are no
     * pending distributions.
     * 
     * Used only by the service thread.
     */
    private long __m_DistributionsPendingStart;
    
    /**
     * Property PENDING_WARNING_INTERVAL
     *
     */
    public static final long PENDING_WARNING_INTERVAL = 30000L;
    
    /**
     * Property REPORT_BRIEF
     *
     * The type of report produced by reportLocalDistributionState method; the
     * method breaks down the total number of pending distributions by member
     * id of the target or origin, and groups members  by machine.
     */
    public static final int REPORT_BRIEF = 1;
    
    /**
     * Property REPORT_VERBOSE
     *
     * The type of report produced by reportLocalDistributionState method; in
     * addition to information reported for REPORT_BRIEF, the method lists 
     * partition  numbers involved in pending distributions.
     */
    public static final int REPORT_VERBOSE = 2;
    
    /**
     * Property Service
     *
     * The partitioned service using this distribution strategy.
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService __m_Service;
    
    /**
     * Property StatsSampleNextMillis
     *
     * The StatsSampleNextMillis value is the time (in local clock) at which
     * the statistics sampling will be collected and sent to the distribution
     * coordinator.
     */
    private long __m_StatsSampleNextMillis;
    
    /**
     * Property StatsSamplingInterval
     *
     * The interval (in ms) with which to collect partition statistics samples.
     */
    private long __m_StatsSamplingInterval;
    
    /**
     * Property WarningNextMillis
     *
     * The time in milliseconds when a pending-distribution warning should next
     * be logged.  Used to avoid excessive logging.
     */
    private long __m_WarningNextMillis;
    
    // Initializing constructor
    public DistributionStrategy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
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
            clz = Class.forName("com.tangosol.coherence/component/util/DistributionStrategy".replace('/', '.'));
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
     * Perform the analysis of the partition distribution among the partitioned
    * service members and, if necessary, re-distribute the partitions. Called
    * on the service thread only.
    * 
    * @param setOwners  the set of ownership-enabled service members (including
    * any that might be leaving)
    * @param setLeaving  the set of ownership-enabled service members that are
    * in the process of leaving
     */
    public void checkDistribution(com.tangosol.coherence.component.net.MemberSet setOwners, java.util.Set setLeaving)
        {
        }
    
    /**
     * This method returns non-empty string iff the sevice member is in the
    * state of actively receiving or sending partitions (as opposed to just
    * having scheduled distributions). If this is the case, the method returns
    * details of the pending status, otherwise, it returns an empty string.
     */
    protected String displayPendingState(boolean fVerbose)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$DistributionRequest as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.DistributionRequest;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$TransferControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.TransferControl;
        
        StringBuilder      sb         = new StringBuilder("");
        PartitionedService service    = getService();
        String             sSeparator = "";
        
        if (isDistributionInProgress())
            {
            sb.append("The service currently has an active outgoing (sent but not yet completed) request" +
                " for a primary distribution.");
            sSeparator = "\n";
            }
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.TransferControl ctrlTransfer = service.getTransferControl();
        if (ctrlTransfer.isInProgress())
            {
            boolean fPrimary = ctrlTransfer.getDistributionRequest() != null;
        
            sb.append(sSeparator)
              .append("There are ")
              .append(ctrlTransfer.getTransferCount())
              .append(" outgoing ")
              .append(fPrimary ? "primary" : "backup")
              .append(" transfers in progress");
            sSeparator = "\n";
            }
        
        int cIncoming = ctrlTransfer.getIncomingTransfers().size();
        if (cIncoming > 0)
            {
            sb.append(sSeparator)
              .append("There are ")
              .append(cIncoming)
              .append(" incoming transfers in progress");
            }
        
        return sb.toString();
        }
    
    // Accessor for the property "BackupCount"
    /**
     * Getter for property BackupCount.<p>
    * The number of backups for each partition configured on the partitioned
    * service.
     */
    public int getBackupCount()
        {
        return getService().getBackupCount();
        }
    
    // Accessor for the property "DistributionsPendingStart"
    /**
     * Getter for property DistributionsPendingStart.<p>
    * The last time in milliseconds when the service started seeing pending
    * distributions which have not yet been completed, or zero if there are no
    * pending distributions.
    * 
    * Used only by the service thread.
     */
    public long getDistributionsPendingStart()
        {
        return __m_DistributionsPendingStart;
        }
    
    // Accessor for the property "PartitionAssignments"
    /**
     * Getter for property PartitionAssignments.<p>
    * The partition assignments array of the partitioned service.
    * 
    * @see PartitionedService.PartitionAssignments
     */
    public int[][] getPartitionAssignments()
        {
        return getService().getPartitionAssignments();
        }
    
    // Accessor for the property "PartitionCount"
    /**
     * Getter for property PartitionCount.<p>
    * The number of partitions configured on the partitioned service.
     */
    public int getPartitionCount()
        {
        return getService().getPartitionCount();
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * The partitioned service using this distribution strategy.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService getService()
        {
        return __m_Service;
        }
    
    // Accessor for the property "StatsSampleNextMillis"
    /**
     * Getter for property StatsSampleNextMillis.<p>
    * The StatsSampleNextMillis value is the time (in local clock) at which the
    * statistics sampling will be collected and sent to the distribution
    * coordinator.
     */
    public long getStatsSampleNextMillis()
        {
        return __m_StatsSampleNextMillis;
        }
    
    // Accessor for the property "StatsSamplingInterval"
    /**
     * Getter for property StatsSamplingInterval.<p>
    * The interval (in ms) with which to collect partition statistics samples.
     */
    public long getStatsSamplingInterval()
        {
        return __m_StatsSamplingInterval;
        }
    
    // Accessor for the property "ThisMember"
    /**
     * Getter for property ThisMember.<p>
    * The Member object for this Member.
     */
    public com.tangosol.coherence.component.net.Member getThisMember()
        {
        return getService().getThisMember();
        }
    
    // Accessor for the property "WarningNextMillis"
    /**
     * Getter for property WarningNextMillis.<p>
    * The time in milliseconds when a pending-distribution warning should next
    * be logged.  Used to avoid excessive logging.
     */
    public long getWarningNextMillis()
        {
        return __m_WarningNextMillis;
        }
    
    /**
     * Initialize this DistributionStrategy based on the Service attributes.
     */
    public void initialize()
        {
        }
    
    // Accessor for the property "DistributionInProgress"
    /**
     * Getter for property DistributionInProgress.<p>
    * Indicates whether or not the service is in the progress of conducting
    * distribution.  Distribution is in progress if the service has a currently
    * active outgoing (sent but not yet answered) request. Not more then one
    * outgoing request could be active at any given time for the service. 
    * 
    * Note: Only the service thread uses this property.
     */
    public boolean isDistributionInProgress()
        {
        return __m_DistributionInProgress;
        }
    
    /**
     * Called on the service thread when the distribution from the specified
    * member has completed (either by finished the receive successfully, or
    * departure of the source member).
    * 
    * @param member    the source member we are receiving distribution from
    * @param fSuccess   true iff the distribution was successfully received
     */
    public void onDistributionCompleted(com.tangosol.coherence.component.net.Member member, boolean fSuccess)
        {
        }
    
    /**
     * Called to process the a DistributionPlanUpdate.
     */
    public void onDistributionPlanUpdate(com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.DistributionPlanUpdate msgUpdate)
        {
        }
    
    /**
     * Called on the service thread to process the specified distribution
    * request.
    * 
    * @param msgRequest    the distribution request
     */
    public void onDistributionRequest(com.tangosol.coherence.component.net.message.RequestMessage msgRequest)
        {
        }
    
    /**
     * Called when the set of (ownership-enabled) members in the service has
    * changed (or is imminently changing).
    * 
    * @param iReason     one of the MemberEvent.MEMBER_* constants
     */
    public void onMembershipChanged(int iReason)
        {
        scheduleImmediate();
        }
    
    /**
     * Called to process the a PartitionStatsUpdate.
     */
    public void onPartitionStatsUpdate(com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionStatsUpdate msgUpdate)
        {
        }
    
    public void recoverOrphans(com.tangosol.net.partition.PartitionSet partsOrphan, com.tangosol.coherence.component.net.MemberSet setOwners, java.util.Set setLeaving, com.tangosol.persistence.GUIDHelper.GUIDResolver resolverGUID, String sSnapshot)
        {
        }
    
    /**
     * Periodically report distributions on this member which have been
    * outstanding for a long time, thus preventing a new round of distributions
    * from starting.
     */
    public void reportLateDistributions()
        {
        // import com.tangosol.coherence.config.Config;
        // import com.tangosol.util.Base;
        // import com.tangosol.net.management.MBeanServerProxy;
        // import com.tangosol.net.management.Registry;
        
        
        long ldtNow          = Base.getSafeTimeMillis();
        long ldtPendingStart = getDistributionsPendingStart();
        
        if (ldtPendingStart == 0L)
            {
            // record current time and let the first late distribution go unreported
            setDistributionsPendingStart(ldtNow);
            setWarningNextMillis(ldtNow + PENDING_WARNING_INTERVAL);  // only log periodically
            }
        else if (ldtNow > getWarningNextMillis())
            {
            long cLateSecs = (ldtNow - ldtPendingStart)/1000;
        
             _trace("Current partition distribution has been pending for over " + cLateSecs + " seconds;\n"
                 + reportLocalDistributionState(true), 2);
        
            setWarningNextMillis(ldtNow + PENDING_WARNING_INTERVAL);  // only log periodically
        
            if (cLateSecs >= 300 && Config.getBoolean("coherence.distributed.diagnose.stuck"))
                {
                // Something must have gone wrong, logClusterState and dumpClusterHeap for analysis
                Registry         registry      = getService().getCluster().getManagement();
                MBeanServerProxy proxy         = registry.getMBeanServerProxy();
                String           sClusterMBean = "Coherence:type=Cluster";
                String           sRoleName     = getService().getThisMember().getRoleName();
                try
                    {
                    proxy.invoke(sClusterMBean, "logClusterState",
                            new Object[]{sRoleName},
                            new String[]{String.class.getName()});
        
                    proxy.invoke(sClusterMBean, "dumpClusterHeap",
                            new Object[]{sRoleName},
                            new String[]{String.class.getName()});
                    }
                catch (Exception e)
                    {
                    _trace("Failed to logClusterState or dumpClusterHeap " + e.getMessage(), 2);
                    }
                }
            }
        }
    
    /**
     * Report current state of the distribution on this service member in
    * human-readable form. Pending distributions are grouped by machine and
    * further by target or origin member Id. If fVerbose parameter is true,
    * specific partition Ids are also reported.
    * 
    * @return  a user-readable report
     */
    public String reportLocalDistributionState(boolean fVerbose)
        {
        return "Operation is not implemented by this distribution strategy.";
        }
    
    /**
     * Schedule an immediate distribution check.
     */
    public void scheduleImmediate()
        {
        getService().setDistributionNextMillis(0L);
        }
    
    // Accessor for the property "DistributionInProgress"
    /**
     * Setter for property DistributionInProgress.<p>
    * Indicates whether or not the service is in the progress of conducting
    * distribution.  Distribution is in progress if the service has a currently
    * active outgoing (sent but not yet answered) request. Not more then one
    * outgoing request could be active at any given time for the service. 
    * 
    * Note: Only the service thread uses this property.
     */
    protected void setDistributionInProgress(boolean fDistribution)
        {
        __m_DistributionInProgress = fDistribution;
        }
    
    // Accessor for the property "DistributionsPendingStart"
    /**
     * Setter for property DistributionsPendingStart.<p>
    * The last time in milliseconds when the service started seeing pending
    * distributions which have not yet been completed, or zero if there are no
    * pending distributions.
    * 
    * Used only by the service thread.
     */
    public void setDistributionsPendingStart(long lStart)
        {
        __m_DistributionsPendingStart = lStart;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * The partitioned service using this distribution strategy.
     */
    public void setService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService service)
        {
        __m_Service = service;
        }
    
    // Accessor for the property "StatsSampleNextMillis"
    /**
     * Setter for property StatsSampleNextMillis.<p>
    * The StatsSampleNextMillis value is the time (in local clock) at which the
    * statistics sampling will be collected and sent to the distribution
    * coordinator.
     */
    public void setStatsSampleNextMillis(long lMillis)
        {
        __m_StatsSampleNextMillis = lMillis;
        }
    
    // Accessor for the property "StatsSamplingInterval"
    /**
     * Setter for property StatsSamplingInterval.<p>
    * The interval (in ms) with which to collect partition statistics samples.
     */
    public void setStatsSamplingInterval(long lInterval)
        {
        __m_StatsSamplingInterval = lInterval;
        }
    
    // Accessor for the property "WarningNextMillis"
    /**
     * Setter for property WarningNextMillis.<p>
    * The time in milliseconds when a pending-distribution warning should next
    * be logged.  Used to avoid excessive logging.
     */
    public void setWarningNextMillis(long lMillis)
        {
        __m_WarningNextMillis = lMillis;
        }
    }
