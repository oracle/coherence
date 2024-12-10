
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.BackupRequest

package com.tangosol.coherence.component.net.message.requestMessage.chainedRequest;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.config.Config;
import java.util.List;

/**
 * A BackupRequest is used to backup some aspect of a partition(s) state.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class BackupRequest
        extends    com.tangosol.coherence.component.net.message.requestMessage.ChainedRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property Confirmed
     *
     * Transient property indicating whether or not the ownership has been
     * confirmed for this backup request.
     * 
     * See PartitionedService$PartitionControl.onBackupRequest.
     */
    private transient boolean __m_Confirmed;
    
    /**
     * Property DeliverySuspect
     *
     * True if message delivery to the service is suspect.
     * 
     * @volatile - releaseOutgoing can be called concurrently when message is
     * addressed to multiple peers.  We need it to be volatile to ensure
     * visibility if checkNotifySent is called on a different thread then one
     * which called releaseOutgoing(true)
     */
    private volatile boolean __m_DeliverySuspect;
    
    /**
     * Property Estimating
     *
     * True if message serialization is called to calculate the envelope size
     * of the backup message.
     */
    private boolean __m_Estimating;
    
    /**
     * Property SyncMsg
     *
     * Flag to indicate if the backup msg was sent synchronously.
     */
    private transient boolean __m_SyncMsg;
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
        __mapChildren.put("Poll", BackupRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public BackupRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public BackupRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.BackupRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/chainedRequest/BackupRequest".replace('/', '.'));
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
    
    // Accessor for the property "Confirmed"
    /**
     * Getter for property Confirmed.<p>
    * Transient property indicating whether or not the ownership has been
    * confirmed for this backup request.
    * 
    * See PartitionedService$PartitionControl.onBackupRequest.
     */
    public boolean isConfirmed()
        {
        return __m_Confirmed;
        }
    
    // Accessor for the property "DeliverySuspect"
    /**
     * Getter for property DeliverySuspect.<p>
    * True if message delivery to the service is suspect.
    * 
    * @volatile - releaseOutgoing can be called concurrently when message is
    * addressed to multiple peers.  We need it to be volatile to ensure
    * visibility if checkNotifySent is called on a different thread then one
    * which called releaseOutgoing(true)
     */
    public boolean isDeliverySuspect()
        {
        return __m_DeliverySuspect;
        }
    
    // Accessor for the property "Estimating"
    /**
     * Getter for property Estimating.<p>
    * True if message serialization is called to calculate the envelope size of
    * the backup message.
     */
    public boolean isEstimating()
        {
        return __m_Estimating;
        }
    
    // Accessor for the property "SyncMsg"
    /**
     * Getter for property SyncMsg.<p>
    * Flag to indicate if the backup msg was sent synchronously.
     */
    public boolean isSyncMsg()
        {
        return __m_SyncMsg;
        }
    
    // Declared at the super level
    /**
     * Preprocess the sent notification of this message.
    * 
    * @return true iff this notification has been fully processed (onSent was
    * called)
     */
    public boolean preprocessSentNotification()
        {
        // see BackupRequest.Poll.onDelivery()
        onDelivery();
        return true;
        }
    
    // Declared at the super level
    /**
     * Release the buffer(s) that held serialized form of this outgoing message.
    * This method must be called once per recipient.
    * 
    * @param fSuspect true if it is unclear if the message will be delivered
    * @param fOrdered true if messages are released in deliverey order, false
    * otherwise, a value of false can be specified at most once and must be
    * ultimately followed by a call to releaseOutgoingComplete(), the use of
    * false is reserved for TCMP/datagram
     */
    public void releaseOutgoing(boolean fSuspect, boolean fOrdered)
        {
        if (fSuspect)
            {
            setDeliverySuspect(true);
            }
        
        super.releaseOutgoing(fSuspect, fOrdered);
        }
    
    // Accessor for the property "Confirmed"
    /**
     * Setter for property Confirmed.<p>
    * Transient property indicating whether or not the ownership has been
    * confirmed for this backup request.
    * 
    * See PartitionedService$PartitionControl.onBackupRequest.
     */
    public void setConfirmed(boolean fConfirmed)
        {
        __m_Confirmed = fConfirmed;
        }
    
    // Accessor for the property "DeliverySuspect"
    /**
     * Setter for property DeliverySuspect.<p>
    * True if message delivery to the service is suspect.
    * 
    * @volatile - releaseOutgoing can be called concurrently when message is
    * addressed to multiple peers.  We need it to be volatile to ensure
    * visibility if checkNotifySent is called on a different thread then one
    * which called releaseOutgoing(true)
     */
    public void setDeliverySuspect(boolean fSuspect)
        {
        __m_DeliverySuspect = fSuspect;
        }
    
    // Accessor for the property "Estimating"
    /**
     * Setter for property Estimating.<p>
    * True if message serialization is called to calculate the envelope size of
    * the backup message.
     */
    public void setEstimating(boolean fEstimating)
        {
        __m_Estimating = fEstimating;
        }
    
    // Declared at the super level
    /**
     * Setter for property NotifyDelivery.<p>
    * Set to true to get a "return receipt" notification when the Message has
    * been delivered (or when Message is determined to be undeliverable).
    * 
    * As of Coherence 3.2, this provides a stronger guarantee than an ack of
    * all the message's packets.  This notification will not be delivered until
    * all living recipients have ack'd all older packets from us.  This ensures
    * that nothing will stop [the living] recipients from processing the
    * message.
    * 
    * This notification generally has a very high latency (compared to a poll),
    * and is meant mostly for cleanup tasks.
    * 
    * @functional
     */
    public void setNotifyDelivery(boolean fNotify)
        {
        super.setNotifyDelivery(fNotify);
        }
    
    // Accessor for the property "SyncMsg"
    /**
     * Setter for property SyncMsg.<p>
    * Flag to indicate if the backup msg was sent synchronously.
     */
    public void setSyncMsg(boolean fMsg)
        {
        __m_SyncMsg = fMsg;
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.BackupRequest$Poll
    
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
            extends    com.tangosol.coherence.component.net.message.requestMessage.ChainedRequest.Poll
        {
        // ---- Fields declarations ----
        
        /**
         * Property BusDeliveryOptimized
         *
         * Controlled by the "coherence.distributed.busOptimizedBackup" system
         * property.  If set to false it will disable the backup optimization
         * based on bus delivery confirmations.
         */
        private static transient boolean __s_BusDeliveryOptimized;
        
        private static void _initStatic$Default()
            {
            }
        
        // Static initializer (from _initStatic)
        static
            {
            // import com.tangosol.coherence.config.Config;
            
            _initStatic$Default();
            
            setBusDeliveryOptimized(Boolean.parseBoolean(Config.getProperty("coherence.distributed.busOptimizedBackup", "true")));
            }
        
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
            return new com.tangosol.coherence.component.net.message.requestMessage.chainedRequest.BackupRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/chainedRequest/BackupRequest$Poll".replace('/', '.'));
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
        
        // Accessor for the property "BusDeliveryOptimized"
        /**
         * Getter for property BusDeliveryOptimized.<p>
        * Controlled by the "coherence.distributed.busOptimizedBackup" system
        * property.  If set to false it will disable the backup optimization
        * based on bus delivery confirmations.
         */
        public static boolean isBusDeliveryOptimized()
            {
            return __s_BusDeliveryOptimized;
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
            return super.isCloseableOnDelivery() && !((BackupRequest) get_Module()).isDeliverySuspect();
            }
        
        // Declared at the super level
        /**
         * This method is called just before the parent RequestMessage is
        * dispatched.
         */
        public void prepareDispatch(com.tangosol.coherence.component.net.message.RequestMessage msg)
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet.SingleMemberSet;
            // import java.util.List;
            
            super.prepareDispatch(msg);
            
            // If we are using a MessageBus transport and this is the next to last server in the
            // backup chain, we can utilize the Bus's delivery guarantees to accelerate the completion
            // of the request to not wait for the response message, but only wait for the delivery
            // notification. (see ChainedRequest.post)
            BackupRequest msgBackup   = (BackupRequest) msg;
            List    listMembers = msgBackup.getMemberList();
            if (isBusDeliveryOptimized() && listMembers != null && listMembers.isEmpty())
                {
                // ensure the only receipient is using a bus
                Member member = ((SingleMemberSet) msg.getToMemberSet()).getTheMember();
                if (msgBackup.getService().getServiceMemberSet().getServiceEndPoint(member.getId()) != null)
                    {
                    msgBackup.setNotifyDelivery(true);
                    setCloseableOnDelivery(true);
                    }
                }
            }
        
        // Declared at the super level
        /**
         * Preprocess the response to this Poll.
        * 
        * @return true iff the response message has been fully processed
        * (onMessage was called)
         */
        public boolean preprocessResponse(com.tangosol.coherence.component.net.Message msgResponse)
            {
            // backup protocol maintains its own message ordering, thus it is always safe to preprocess backup
            // responses.  This is similar to backup acceleration.
            
            if (isPreprocessable())
                {
                getService().onMessage(msgResponse);
                return true;
                }
            return false;
            }
        
        // Accessor for the property "BusDeliveryOptimized"
        /**
         * Setter for property BusDeliveryOptimized.<p>
        * Controlled by the "coherence.distributed.busOptimizedBackup" system
        * property.  If set to false it will disable the backup optimization
        * based on bus delivery confirmations.
         */
        private static void setBusDeliveryOptimized(boolean fOptimized)
            {
            __s_BusDeliveryOptimized = fOptimized;
            }
        }
    }
