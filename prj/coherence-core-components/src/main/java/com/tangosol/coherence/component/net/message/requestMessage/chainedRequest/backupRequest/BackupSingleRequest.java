
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.backupRequest.BackupSingleRequest

package com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.backupRequest;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

/**
 * A BackupSingleRequest is used to backup some aspect of a partition's state.
 * 
 * BackupSingleRequest is also used as a Continuation for deferring the
 * processing of a request for an unowned backup partition, pending a
 * confirmation of the ownership (see #onReceived).
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class BackupSingleRequest
        extends    com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.BackupRequest
        implements com.oracle.coherence.common.base.Continuation
    {
    // ---- Fields declarations ----
    
    /**
     * Property Partition
     *
     * The Partition-id that this backup message represents.
     */
    private int __m_Partition;
    
    /**
     * Property PartitionVersion
     *
     * The PartitionVersion that this backup message represents.
     */
    private long __m_PartitionVersion;
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
        __mapChildren.put("Poll", BackupSingleRequest.Poll.get_CLASS());
        }
    
    // Initializing constructor
    public BackupSingleRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/chainedRequest/backupRequest/BackupSingleRequest".replace('/', '.'));
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
    /**
     * Instantiate a copy of this message. This is quite different from the
    * standard "clone" since only the "transmittable" portion of the message
    * (and none of the internal) state should be cloned.
     */
    public com.tangosol.coherence.component.net.Message cloneMessage()
        {
        BackupSingleRequest msg = (BackupSingleRequest) super.cloneMessage();
        
        msg.setPartition(getPartition());
        msg.setPartitionVersion(getPartitionVersion());
        msg.setSyncMsg(isSyncMsg());
        return msg;
        }
    
    /**
     * Perform the backup operation.
     */
    public void doBackup()
        {
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific Message data.
     */
    public String getDescription()
        {
        return super.getDescription()
             + "\nPartition=" + getPartition()
             + "\nPartitionVersion=" + getPartitionVersion();
        }
    
    // Accessor for the property "Partition"
    /**
     * Getter for property Partition.<p>
    * The Partition-id that this backup message represents.
     */
    public int getPartition()
        {
        return __m_Partition;
        }
    
    // Accessor for the property "PartitionVersion"
    /**
     * Getter for property PartitionVersion.<p>
    * The PartitionVersion that this backup message represents.
     */
    public long getPartitionVersion()
        {
        return __m_PartitionVersion;
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
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$PartitionControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionControl;
        
        // Note: do not call super, as this message may be deferred.  See #doBackupRequest
        PartitionedService service    = (PartitionedService) getService();
        int                nPartition = getPartition();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionControl   control    = service.getPartitionControl(nPartition);
        
        if (control != null && (service.isBackupOwner(nPartition) || control.isTransferringOut()))
            {
            // process the backup request through the associated partition control
            // to enforce the request/version ordering
            service.processBackup(this);
            }
        else
            {
            // we received a backup request for a partition that we are not the backup
            // owner for.  This could happen as a result of updates during a backup transfer
            // arriving on the old backup (this member) after the backup was released.
        
            int    nMemberOriginator = getOriginatingMemberId();
            Member memberOriginator  = service.getServiceMemberSet().getMember(nMemberOriginator);
            if (isConfirmed() || memberOriginator == null)
                {
                _trace("Discarding " + get_Name() + " for unowned partition " + nPartition
                     + " originating from member " + nMemberOriginator, 5);
                forwardOrReply(this);
                }
            else
                {
                // issue a "ping" request to the primary owner that backup message
                // originated from in order to force any possible in-flight transfers
                // to arrive.  This is necessary to protect against the following sequence
                // of events:
                //  - primary owner 1 sends transfer to new backup 3  (version n)
                //  - primary owner 1 sends backup message to 2 -> 3  (version n+1)
                //  - the backup message at version n+1 arrives at member 3 before the
                //    transfer, so member 3 does not consider itself a backup owner
                //
                // Completion of a "ping" to member 1 allows the correct handling (or dropping)
                // of this backup request.
                //
                // Note: this will not be necessary once COH-5400 is implemented
        
                service.sendPingRequest(memberOriginator, this);
                }
            }
        }
    
    // From interface: com.oracle.coherence.common.base.Continuation
    public void proceed(Object oResult)
        {
        setConfirmed(true);
        
        onReceived();
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        
        super.read(input);
        
        setPartition(com.tangosol.util.ExternalizableHelper.readInt(input));
        setPartitionVersion(com.tangosol.util.ExternalizableHelper.readLong(input));
        }
    
    // Accessor for the property "Partition"
    /**
     * Setter for property Partition.<p>
    * The Partition-id that this backup message represents.
     */
    public void setPartition(int iPart)
        {
        __m_Partition = iPart;
        }
    
    // Accessor for the property "PartitionVersion"
    /**
     * Setter for property PartitionVersion.<p>
    * The PartitionVersion that this backup message represents.
     */
    public void setPartitionVersion(long lVersion)
        {
        __m_PartitionVersion = lVersion;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        
        super.write(output);
        
        com.tangosol.util.ExternalizableHelper.writeInt (output, getPartition());
        com.tangosol.util.ExternalizableHelper.writeLong(output, getPartitionVersion());
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.backupRequest.BackupSingleRequest$Poll
    
    /**
     * The Poll contains information regarding a request sent to one or more
     * Cluster Members that require responses. A Service may poll other Members
     * that are running the same Service, and the Poll is used to wait for and
     * assemble the responses from each of those Members. A client thread may
     * also use the Poll to block on a response or set of responses, thus
     * waiting for the completion of the Poll. In its simplest form, which is a
     * Poll that is sent to one Member of the Cluster, the Poll actually
     * represents the request/response model.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Poll
            extends    com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.BackupRequest.Poll
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public Poll()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.backupRequest.BackupSingleRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/chainedRequest/backupRequest/BackupSingleRequest$Poll".replace('/', '.'));
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
        /**
         * Getter for property CloseableOnDelivery.<p>
        * True if the poll is closeable once delivery notification is received
        * for the corresponding RequestMessage.
        * 
        * To optimize the memory usage, the value is stored in the _StateAux
        * property.
        * 
        * @functional
         */
        public boolean isCloseableOnDelivery()
            {
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$PartitionControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionControl;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
            
            if (super.isCloseableOnDelivery())
                {
                BackupSingleRequest          msgBackup = (BackupSingleRequest) get_Module();
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionControl control   = ((PartitionedService) msgBackup.getService())
                    .getPartitionControl(msgBackup.getPartition());
                
                if (control != null &&
                    control.getVersionBackup() == msgBackup.getPartitionVersion() - 1L)
                    {        
                    // this was the next completion we were waiting on, it is safe to close it now
                    return true;
                    }
                // else; we don't own the partition or the completions are out-of-order; unsafe to close now
                }
            
            return false;
            }
        }
    }
