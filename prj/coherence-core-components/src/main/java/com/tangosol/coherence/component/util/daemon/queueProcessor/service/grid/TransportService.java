
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.Threads;
import com.tangosol.util.Base;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TransportService is a service which hosts a MessageBus based transport which
 * may be used by other grid services.
 * 
 * The message range from [33-64] is reserved for usage by the TransportService
 * component
 * 
 * Currently used MessageTypes:
 * [1-32]  Reserved by Grid
 * 33         Heartbeat
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class TransportService
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid
    {
    // ---- Fields declarations ----
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
        __mapChildren.put("Acknowledgement", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Acknowledgement.get_CLASS());
        __mapChildren.put("BusEventMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("ConfigRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("Heartbeat", TransportService.Heartbeat.get_CLASS());
        __mapChildren.put("MemberConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigUpdate.get_CLASS());
        __mapChildren.put("MemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberJoined.get_CLASS());
        __mapChildren.put("MemberWelcome", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcome.get_CLASS());
        __mapChildren.put("MemberWelcomeRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequest.get_CLASS());
        __mapChildren.put("MemberWelcomeRequestTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequestTask.get_CLASS());
        __mapChildren.put("MessageHandler", TransportService.MessageHandler.get_CLASS());
        __mapChildren.put("NotifyConnectionClose", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionClose.get_CLASS());
        __mapChildren.put("NotifyConnectionOpen", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionOpen.get_CLASS());
        __mapChildren.put("NotifyMemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined.get_CLASS());
        __mapChildren.put("NotifyMemberLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving.get_CLASS());
        __mapChildren.put("NotifyMemberLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft.get_CLASS());
        __mapChildren.put("NotifyMessageReceipt", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMessageReceipt.get_CLASS());
        __mapChildren.put("NotifyPollClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyPollClosed.get_CLASS());
        __mapChildren.put("NotifyResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyResponse.get_CLASS());
        __mapChildren.put("NotifyServiceAnnounced", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced.get_CLASS());
        __mapChildren.put("NotifyServiceJoining", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining.get_CLASS());
        __mapChildren.put("NotifyServiceLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving.get_CLASS());
        __mapChildren.put("NotifyServiceLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft.get_CLASS());
        __mapChildren.put("NotifyServiceQuiescence", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence.get_CLASS());
        __mapChildren.put("NotifyShutdown", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyShutdown.get_CLASS());
        __mapChildren.put("NotifyStartup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyStartup.get_CLASS());
        __mapChildren.put("PingRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PingRequest.get_CLASS());
        __mapChildren.put("ProtocolContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ProtocolContext.get_CLASS());
        __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Response.get_CLASS());
        __mapChildren.put("WrapperGuardable", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.WrapperGuardable.get_CLASS());
        }
    
    // Initializing constructor
    public TransportService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant ServiceType
    public String getServiceType()
        {
        return "Transport";
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/TransportService".replace('/', '.'));
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
     * Instantiate a MessageHandler.
     */
    protected com.tangosol.coherence.component.net.MessageHandler instantiateMessageHandler()
        {
        return (TransportService.MessageHandler) _newChild("MessageHandler");
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService$Heartbeat
    
    /**
     * The Heartbeat message is used by the TransportService to test the health
     * of its peers.
     * 
     * @since 12.2.1.2.1
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Heartbeat
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public Heartbeat()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Heartbeat(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(33);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService.Heartbeat();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/TransportService$Heartbeat".replace('/', '.'));
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
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService$MessageHandler
    
    /**
     * MessageHandler is an adapter between the Grid services and the
     * MessageBus.
     * 
     * The MessageHandler may be used in one of two modes:
     * 
     * Service dedicated mode.  In this mode the Handler resides within a
     * service and is used only for exchanging messages with other members of
     * the same service.
     * 
     * Shared mode.  In this mode the Handler resides on the TransportService
     * and other services make use of it via Service.getMessagePublisher().  In
     * this mode it becomes very important to differentiate between
     * handler.getService().getMemberSet() and msg.getService().getMemberSet(),
     * as the former indicates who you can send to on this transport, while the
     * later indicates who you can send to on any transport.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static abstract class MessageHandler
            extends    com.tangosol.coherence.component.net.MessageHandler
        {
        // ---- Fields declarations ----
        
        /**
         * Property PendingServiceFlush
         *
         * Set of services for which a flush is pending.
         */
        private java.util.Set __m_PendingServiceFlush;
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
            __mapChildren.put("Connection", TransportService.MessageHandler.Connection.get_CLASS());
            }
        
        // Initializing constructor
        public MessageHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/TransportService$MessageHandler".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Accessor for the property "PendingServiceFlush"
        /**
         * Getter for property PendingServiceFlush.<p>
        * Set of services for which a flush is pending.
         */
        public java.util.Set getPendingServiceFlush()
            {
            return __m_PendingServiceFlush;
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // import java.util.Collections;
            // import java.util.concurrent.ConcurrentHashMap;
            
            super.onInit();
            
            setPendingServiceFlush(Collections.newSetFromMap(new ConcurrentHashMap()));
            }
        
        // Declared at the super level
        /**
         * Process the Bus receipt. Called on a Bus thread.
         */
        public void processReceipt(com.oracle.coherence.common.net.exabus.EndPoint peer, com.tangosol.coherence.component.net.Message msg, boolean fSuspect)
            {
            // due an independant life cycle between services and the transport
            // we cannot guarantee that a message delivered to the transport will be
            // delivered to the service before it learns that the sender is gone
            
            super.processReceipt(peer, msg, true);
            }
        
        // Accessor for the property "PendingServiceFlush"
        /**
         * Setter for property PendingServiceFlush.<p>
        * Set of services for which a flush is pending.
         */
        protected void setPendingServiceFlush(java.util.Set setFlush)
            {
            __m_PendingServiceFlush = setFlush;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService$MessageHandler$Connection
        
        /**
         * Information about a connection to a peer.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Connection
                extends    com.tangosol.coherence.component.net.MessageHandler.Connection
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Connection()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Connection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService.MessageHandler.Connection();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/TransportService$MessageHandler$Connection".replace('/', '.'));
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
            
            // Declared at the super level
            /**
             * Invoked once it has been identified that the we've failed to
            * deliver a message after an extended period.  Once this state
            * occurs, this method will be called once per onInterval cycle
            * until the connection is terminated or delivery succeeds.
             */
            public void onDeliveryTimeout()
                {
                // import Component.Net.Member;
                // import Component.Net.MemberSet.SingleMemberSet;
                // import com.oracle.coherence.common.util.Duration;
                // import com.oracle.coherence.common.util.Threads;
                
                Member member = getMember();
                if (member == null)
                    {
                    super.onDeliveryTimeout();
                    }
                else
                    {
                    // don't call super.onDeliveryTimeout as it would simply disconnect, instead handle it just like a packet timeout
                    // this allows for the witness protocol and quorum to take part in the decision making process
                
                    if (!member.isDeaf())
                        {
                        _trace("Message delivery timeout to " + member.toString(Member.SHOW_STATS) + " after " + new Duration(getDeliveryTimeoutMillis() * 1000000L) +
                            " via " + this + " on\n" + ((TransportService.MessageHandler) get_Parent()).getMessageBus() + "\n" + Threads.getThreadDump(), 5);
                        }
                    // else; still awaiting termination
                
                    ((TransportService) get_Module()).getClusterService().doNotifyTcmpTimeout(/*packet*/ null, SingleMemberSet.instantiate(member));
                    }
                }
            
            // Declared at the super level
            /**
             * Invoked from onInterval when the connection is found to be idle.
             */
            public void onIdle()
                {
                // import Component.Net.Member;
                // import Component.Net.MemberSet.SingleMemberSet;
                // import com.tangosol.util.Base;
                // import java.util.concurrent.ThreadLocalRandom;
                
                TransportService svc    = (TransportService) get_Module();
                Member  member = getMember();
                
                if (svc.isVersionCompatible(member, 12,2,1,2,1))
                    {
                    long ldtNow          = Base.getLastSafeTimeMillis();
                    long cMillisInterval = svc.getIntervalNextMillis() - ldtNow;                            // aprox how often are we called
                    long cMillisWindow   = ((TransportService.MessageHandler) get_Parent()).getDeliveryTimeoutMillis() / 3; // must be less then the deliveryTimeout - heuristicTimeout
                    long cMillisIdle     = ldtNow - getLastHealthyTimestamp();
                    int  cIntervals      = (int) (cMillisWindow / Math.max(1, cMillisInterval));
                
                    if (cMillisIdle >= cMillisWindow ||                       // we've fallen outside of the window, force it
                        ThreadLocalRandom.current().nextInt(cIntervals) == 0) // randomize HBs over the window so all peers aren't HB'd at the same time
                        {
                        TransportService.Heartbeat msg = (TransportService.Heartbeat) svc.instantiateMessage("Heartbeat");
                        msg.setToMemberSet(SingleMemberSet.instantiate(member));
                        svc.post(msg);
                        }
                    }
                }
            
            // Declared at the super level
            /**
             * Construct and deserialize a Message based on the supplied
            * BufferSequence
            * 
            * @return the message
             */
            public com.tangosol.coherence.component.net.Message prepareMessage(com.oracle.coherence.common.io.BufferSequence bufseq)
                    throws java.io.IOException
                {
                // import Component.Net.Message;
                
                Message msg = super.prepareMessage(bufseq);
                
                if (msg != null)
                    {
                    ((TransportService.MessageHandler) get_Parent()).getPendingServiceFlush().add(msg.getService());
                    }
                
                return msg;
                }
            
            // Declared at the super level
            /**
             * Setter for property SuspectTimeoutTimestamp.<p>
            * The timestamp at which a suspect connection will be considered to
            * have timedout.
             */
            protected void setSuspectTimeoutTimestamp(long lTimestamp)
                {
                /*
                import com.tangosol.internal.tracing.Span$Type as SpanType;
                import com.tangosol.internal.tracing.TracingHelper;
                import com.tangosol.util.Base;
                import com.oracle.common.util.Duration;
                
                long ldtLast = getSuspectTimeoutTimestamp();
                super.setSuspectTimeoutTimestamp(lTimestamp);
                
                if (lTimestamp == 0L && ldtLast != 0L) // end of delivery delay
                    {
                    long ldtNow = Base.getSafeTimeMillis();
                    long lDelta = ldtNow - (ldtLast - getDeliveryTimeoutMillis());
                    int  iLogLevel = lDelta > 1000L ? 2
                               : lDelta > 100L  ? 6
                               : lDelta > 10L   ? 8
                               : 9;
                
                    if (lDelta > 0L)
                        {
                        if (_isTraceEnabled(iLogLevel))
                            {
                            _trace("Experienced a " + new Duration(lDelta*1000000) + " bus communication delay (probable remote GC) with "
                                 + "member " + getMember().getId(), iLogLevel);
                            }
                
                        //if (lDelta > 1000L && TracingHelper.isEnabled())
                        //    {
                        //    TracingHelper.newSpan("commdelay")
                        //                .withMetadata(SpanType.COMPONENT.key(), "transport")
                        //                .withMetadata("layer", "bus")
                        //               .withMetadata("member.destination", Long.valueOf(getMember().getId()).longValue())
                        //                .setStartTimestamp((System.currentTimeMillis() - lDelta) * 1000L)
                        //                .startSpan().end();
                            }
                        }
                    }
                */
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService$MessageHandler$EventCollector
        
        /**
         * The Collector implementation used by the MessageHandler.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class EventCollector
                extends    com.tangosol.coherence.component.net.MessageHandler.EventCollector
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public EventCollector()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public EventCollector(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService.MessageHandler.EventCollector();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/TransportService$MessageHandler$EventCollector".replace('/', '.'));
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
            
            // Declared at the super level
            public void flush()
                {
                // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
                // import java.util.Iterator;
                // import java.util.Set;
                
                // flush other services we've just forwarded messages to
                Set setService = ((TransportService.MessageHandler) get_Parent()).getPendingServiceFlush();
                for (Iterator iter = setService.iterator(); iter.hasNext(); )
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid) iter.next();
                    iter.remove();
                    service.flush();
                    }
                
                super.flush();
                }
            }
        }
    }
