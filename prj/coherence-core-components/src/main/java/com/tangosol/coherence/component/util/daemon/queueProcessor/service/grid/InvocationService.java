
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.util.WindowedArray;
import com.oracle.coherence.common.base.Associated;
import com.oracle.coherence.common.base.NonBlocking;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocableInOrder;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.NonBlockingInvocable;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.WrapperException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Invocation service is the base component for 
 * 
 * The message range from [33-64] is reserved for usage by the
 * InvocationService component
 * 
 * Currently used MessageTypes:
 * [1-32]  Reserved by Grid
 * 33         InvocationRequest
 * 34         InvocationResponse
 * 35         InvocationMessage
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class InvocationService
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid
        implements com.tangosol.net.InvocationService
    {
    // ---- Fields declarations ----
    
    /**
     * Property PendingProcess
     *
     * A map of pending processes that is used by the Service in case when
     * Invocable tasks from a given member have to be responded to in the order
     * of arrival. This map is keyed by the Member that sent the request and
     * the corresponding value is WindowedArray containing either
     * $InvocationRequest (if the request has not been procesed yet) or
     * $InvocationResponse message (if the response has been deferred).
     */
    private transient java.util.Map __m_PendingProcess;
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
        __mapChildren.put("InvocationMessage", InvocationService.InvocationMessage.get_CLASS());
        __mapChildren.put("InvocationRequest", InvocationService.InvocationRequest.get_CLASS());
        __mapChildren.put("InvocationResponse", InvocationService.InvocationResponse.get_CLASS());
        __mapChildren.put("MemberConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigUpdate.get_CLASS());
        __mapChildren.put("MemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberJoined.get_CLASS());
        __mapChildren.put("MemberWelcome", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcome.get_CLASS());
        __mapChildren.put("MemberWelcomeRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequest.get_CLASS());
        __mapChildren.put("MemberWelcomeRequestTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequestTask.get_CLASS());
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
    
    // Default constructor
    public InvocationService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public InvocationService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setMessageClassMap(new java.util.HashMap());
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setOldestPendingRequestSUIDCounter(new java.util.concurrent.atomic.AtomicLong());
            setPendingProcess(new com.tangosol.util.LiteMap());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSerializerMap(new java.util.WeakHashMap());
            setSuspendPollLimit(new java.util.concurrent.atomic.AtomicLong());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DaemonPool("DaemonPool", this, true), "DaemonPool");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.EventDispatcher("EventDispatcher", this, true), "EventDispatcher");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Guard("Guard", this, true), "Guard");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigListener("MemberConfigListener", this, true), "MemberConfigListener");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PollArray("PollArray", this, true), "PollArray");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ReceiveQueue("ReceiveQueue", this, true), "ReceiveQueue");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig("ServiceConfig", this, true), "ServiceConfig");
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant ServiceType
    public String getServiceType()
        {
        return "Invocation";
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/InvocationService".replace('/', '.'));
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
     * Wait for the service's associated backlog to drain.
    * 
    * @param setMembers  the members of interest
    * @param cMillis             the maximum amount of time to wait, or 0 for
    * infinite
    * 
    * @return the remaining timeout
    * 
    * @throw RequestTimeoutException on timeout
     */
    public long drainOverflow(com.tangosol.coherence.component.net.MemberSet setMembers, long cMillis)
            throws java.lang.InterruptedException
        {
        // Note: that we cannot hold the service thread itself because the Management
        // service utilizes the execute method from the service thread assuming it
        // will be non-blocking (COH-7084)
        
        return isServiceThread(/*fStrict*/ true)
            ? cMillis
            : super.drainOverflow(setMembers, cMillis);
        }
    
    // From interface: com.tangosol.net.InvocationService
    /**
     * Called on a client thread.
     */
    public void execute(com.tangosol.net.Invocable task, java.util.Set setMembers, com.tangosol.net.InvocationObserver observer)
        {
        // import Component.Net.Message;
        // import com.tangosol.net.InvocableInOrder;
        // import com.tangosol.util.WrapperException;
        // import com.oracle.coherence.common.base.NonBlocking;
        
        if (task == null)
            {
            throw new IllegalArgumentException("Task must be specified");
            }
        
        Message msgExecute;
        long    cTimeout;
        if (observer == null)
            {
            InvocationService.InvocationMessage msg = (InvocationService.InvocationMessage)
                instantiateMessage("InvocationMessage");
            msg.setTask(task);
        
            msgExecute = msg;
            cTimeout   = getRequestTimeout();
            }
        else
            {
            InvocationService.InvocationRequest msg = (InvocationService.InvocationRequest)
                instantiateMessage("InvocationRequest");
            msg.setTask(task);
            msg.setQuery(false);
            msg.setObserver(observer);
            msg.setRespondInOrder(task instanceof InvocableInOrder ?
                ((InvocableInOrder) task).isRespondInOrder() : false);
        
            msgExecute = msg;
            cTimeout   = calculateRequestTimeout(msg);
            }
        
        msgExecute.ensureToMemberSet().addAll(
            setMembers == null ? getServiceMemberSet() : setMembers);
        
        send(msgExecute);
        
        if (!NonBlocking.isNonBlockingCaller())
            {
            try
                {
                // keep this thread from producing any new work if the system is backlogged
                // despite execute being an "async" call we still want to protect from them
                // overwhelming the service
                drainOverflow(msgExecute.getToMemberSet(), cTimeout);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new WrapperException(e, "Request interrupted");
                }
            }
        }
    
    // Accessor for the property "PendingProcess"
    /**
     * Getter for property PendingProcess.<p>
    * A map of pending processes that is used by the Service in case when
    * Invocable tasks from a given member have to be responded to in the order
    * of arrival. This map is keyed by the Member that sent the request and the
    * corresponding value is WindowedArray containing either $InvocationRequest
    * (if the request has not been procesed yet) or $InvocationResponse message
    * (if the response has been deferred).
     */
    public java.util.Map getPendingProcess()
        {
        return __m_PendingProcess;
        }
    
    /**
     * Return the WindowedArray of pending processes for a given member.
     */
    public com.tangosol.coherence.component.util.WindowedArray getProcessArray(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Util.WindowedArray;
        // import java.util.Map;
        
        Map           mapPending = getPendingProcess();
        WindowedArray waProcess  = (WindowedArray) mapPending.get(member);
        if (waProcess == null)
            {
            synchronized (this)
                {
                waProcess = (WindowedArray) mapPending.get(member);
                if (waProcess == null)
                    {
                    waProcess = new WindowedArray();
                    mapPending.put(member, waProcess);
                    }
                }
            }
        return waProcess;
        }
    
    /**
     * Called on either service or worker thread.
     */
    public void onInvocationMessage(InvocationService.InvocationMessage msg)
        {
        // import com.tangosol.net.Invocable;
        
        Invocable task = msg.getTask();
        try
            {
            task.run();
            }
        catch (Throwable e)
            {
            _trace("Failure to execute an Invocable object: " + task
                 + "\n" + getStackTrace(e), 1);
            }
        }
    
    /**
     * Called on either service or worker thread.
     */
    public void onInvocationRequest(InvocationService.InvocationRequest msgRequest)
        {
        // import com.tangosol.net.Invocable;
        // import com.tangosol.net.NonBlockingInvocable;
        
        Invocable task = msgRequest.getTask();
        
        // by now the "task" is a valid Invocable - any deserialization or
        // initialiazation issues have been take care of at onReceived()
        try
            {
            if (task instanceof NonBlockingInvocable)
                {
                ((NonBlockingInvocable) task).run(msgRequest);
                }
            else
                {
                task.run();
                msgRequest.proceed(task.getResult());
                }
            }
        catch (Throwable e)
            {
            msgRequest.proceed(e);
            }
        }
    
    // Declared at the super level
    /**
     * Called to complete the "service-left" processing for the specified
    * member.  This notification is processed only after the associated
    * endpoint has been released by the message handler.  See
    * $NotifyServiceLeft#onReceived/#proceed.
    * Called on the service thread only.
     */
    public void onNotifyServiceLeft(com.tangosol.coherence.component.net.Member member)
        {
        super.onNotifyServiceLeft(member);
        
        getPendingProcess().remove(member);
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to true.
    * If the Service has not completed preparing at this point, then the
    * Service must override this implementation and only set AcceptingClients
    * to true when the Service has actually "finished starting".
     */
    public void onServiceStarted()
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
        if (pool.getDaemonCount() > 0)
            {
            pool.setThreadGroup(new ThreadGroup(getServiceName()));
            pool.start();
            }
        
        super.onServiceStarted();
        }
    
    // From interface: com.tangosol.net.InvocationService
    /**
     * Called on a client thread.
     */
    public java.util.Map query(com.tangosol.net.Invocable task, java.util.Set setMembers)
        {
        // import com.tangosol.net.InvocableInOrder;
        // import java.util.Map;
        
        if (task == null)
            {
            throw new IllegalArgumentException("Task must be specified");
            }
        
        InvocationService.InvocationRequest msg = (InvocationService.InvocationRequest) instantiateMessage("InvocationRequest");
        msg.setTask(task);
        msg.setQuery(true);
        msg.setRespondInOrder(task instanceof InvocableInOrder ?
            ((InvocableInOrder) task).isRespondInOrder() : false);
        msg.ensureToMemberSet().addAll(
            setMembers == null ? getServiceMemberSet() : setMembers);
        
        return (Map) poll(msg);
        }
    
    /**
     * Send the response in a serialization safe way (see COH-3227). Called on
    * either service or worker thread.
     */
    public void sendResponse(InvocationService.InvocationRequest msgRequest, InvocationService.InvocationResponse msgResponse)
        {
        try
            {
            msgResponse.respondTo(msgRequest);
            send(msgResponse);
            }
        catch (Exception e)
            {
            msgResponse = (InvocationService.InvocationResponse) msgResponse.cloneMessage();
            msgResponse.setException(e);
            msgResponse.respondTo(msgRequest);
            send(msgResponse);
            }
        }
    
    // Accessor for the property "PendingProcess"
    /**
     * Setter for property PendingProcess.<p>
    * A map of pending processes that is used by the Service in case when
    * Invocable tasks from a given member have to be responded to in the order
    * of arrival. This map is keyed by the Member that sent the request and the
    * corresponding value is WindowedArray containing either $InvocationRequest
    * (if the request has not been procesed yet) or $InvocationResponse message
    * (if the response has been deferred).
     */
    protected void setPendingProcess(java.util.Map map)
        {
        __m_PendingProcess = map;
        }
        
    /**
     * @return the description of the service
     */
    @Override
    public String getDescription()
        {
        return super.getDescription() + ", Serializer=" + getSerializer().getName();
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService$InvocationMessage
    
    /**
     * @see InvocationService#execute()
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvocationMessage
            extends    com.tangosol.coherence.component.net.Message
            implements com.oracle.coherence.common.base.Associated,
                       com.tangosol.net.PriorityTask,
                       Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Task
         *
         * The task.
         */
        private com.tangosol.net.Invocable __m_Task;
        
        // Default constructor
        public InvocationMessage()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvocationMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(35);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService.InvocationMessage();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/InvocationService$InvocationMessage".replace('/', '.'));
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
        
        // From interface: com.oracle.coherence.common.base.Associated
        public Object getAssociatedKey()
            {
            // import com.oracle.coherence.common.base.Associated;
            // import com.tangosol.net.Invocable;
            
            Invocable task = getTask();
            
            return task instanceof Associated ?
                ((Associated) task).getAssociatedKey() : null;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            StringBuffer sb = new StringBuffer("InvocationMessage{Task=");
            try
                {
                sb.append(getTask());
                }
            catch (Throwable e)
                {
                sb.append(getTask().getClass().getName());
                }
            sb.append('}');
            
            return sb.toString();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            return task instanceof PriorityTask ?
                ((PriorityTask) task).getExecutionTimeoutMillis() : PriorityTask.TIMEOUT_NONE;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            return 0L;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            return task instanceof PriorityTask ?
                ((PriorityTask) task).getSchedulingPriority() : PriorityTask.SCHEDULE_STANDARD;
            }
        
        // Accessor for the property "Task"
        /**
         * Getter for property Task.<p>
        * The task.
         */
        public com.tangosol.net.Invocable getTask()
            {
            return __m_Task;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
            // import com.tangosol.net.Invocable;
            
            super.onReceived();
            
            InvocationService   service = (InvocationService) getService();
            Invocable task    = getTask();
            
            // if there was a serialization failure, the task will be null
            if (task != null)
                {
                try
                    {
                    // initialization is done on the service thread, while
                    // the invocation itself could be executed by a daemon thread
                    task.init(service);
                    }
                catch (Throwable e)
                    {
                    _trace("Failure to initialize an Invocable object: " + e
                         + "\n" + getStackTrace(e), 1);
                    return;
                    }
            
                com.tangosol.coherence.component.util.DaemonPool pool = service.getDaemonPool();
                if (pool.isStarted())
                    {
                    pool.add(this);
                    }
                else
                    {
                    service.onInvocationMessage(this);
                    }
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.net.Invocable;
            // import java.io.IOException;
            
            super.read(input);
            
            try
                {
                setTask((Invocable) readObject(input));
                }
            catch (IOException e)
                {
                _trace("Failure to deserialize an Invocable object: " + e
                     + "\n" + getStackTrace(e) + " Message " + toString(false), 1);
                }
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            ((InvocationService) getService()).onInvocationMessage(this);
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.Invocable;
            // import com.tangosol.net.PriorityTask;
            
            try
                {
                Invocable task = getTask();
                if (task instanceof PriorityTask)
                    {
                    ((PriorityTask) task).runCanceled(fAbandoned);
                    }
                }
            catch (Throwable e) {}
            }
        
        // Accessor for the property "Task"
        /**
         * Setter for property Task.<p>
        * The task.
         */
        public void setTask(com.tangosol.net.Invocable pTask)
            {
            __m_Task = pTask;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            writeObject(output, getTask());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService$InvocationRequest
    
    /**
     * @see InvocationService#execute()
     * @see InvocationService#query()
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvocationRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
            implements com.oracle.coherence.common.base.Associated,
                       com.oracle.coherence.common.base.Continuation,
                       com.tangosol.net.PriorityTask,
                       Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Exception
         *
         * A deserialization exception caught by the read().
         */
        private transient Throwable __m_Exception;
        
        /**
         * Property Observer
         *
         * The observer (optional). Used only if Query is false.
         */
        private transient com.tangosol.net.InvocationObserver __m_Observer;
        
        /**
         * Property ProcessId
         *
         * Transient property used by the Service in case when Invocable tasks
         * from a given member have to be responded to in the order of arrival.
         *  Indicates this task's arrival id.
         */
        private transient long __m_ProcessId;
        
        /**
         * Property Query
         *
         * Set to true if this Request represents a query() call; false for
         * execute() call
         */
        private transient boolean __m_Query;
        
        /**
         * Property RespondInOrder
         *
         * Specifies whether or not this request has to be responded to in the
         * order of arrival (for a given Member).
         */
        private boolean __m_RespondInOrder;
        
        /**
         * Property Task
         *
         * The task.
         */
        private com.tangosol.net.Invocable __m_Task;
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
            __mapChildren.put("Poll", InvocationService.InvocationRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public InvocationRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvocationRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService.InvocationRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/InvocationService$InvocationRequest".replace('/', '.'));
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
        
        // From interface: com.oracle.coherence.common.base.Associated
        public Object getAssociatedKey()
            {
            // import com.oracle.coherence.common.base.Associated;
            // import com.tangosol.net.Invocable;
            
            Invocable task = getTask();
            
            return task instanceof Associated ?
                ((Associated) task).getAssociatedKey() : null;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            StringBuffer sb = new StringBuffer("InvocationRequest{");
            sb.append(isQuery() ? "Query" : "Execute")
              .append(", Task=");
            try
                {
                sb.append(getTask());
                }
            catch (Throwable e)
                {
                sb.append(getTask().getClass().getName());
                }
            sb.append('}');
            
            return sb.toString();
            }
        
        // Accessor for the property "Exception"
        /**
         * Getter for property Exception.<p>
        * A deserialization exception caught by the read().
         */
        protected Throwable getException()
            {
            return __m_Exception;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            return task instanceof PriorityTask ?
                ((PriorityTask) task).getExecutionTimeoutMillis() : PriorityTask.TIMEOUT_NONE;
            }
        
        // Accessor for the property "Observer"
        /**
         * Getter for property Observer.<p>
        * The observer (optional). Used only if Query is false.
         */
        public com.tangosol.net.InvocationObserver getObserver()
            {
            return __m_Observer;
            }
        
        // Accessor for the property "ProcessId"
        /**
         * Getter for property ProcessId.<p>
        * Transient property used by the Service in case when Invocable tasks
        * from a given member have to be responded to in the order of arrival. 
        * Indicates this task's arrival id.
         */
        public long getProcessId()
            {
            return __m_ProcessId;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            return task instanceof PriorityTask ?
                ((PriorityTask) task).getRequestTimeoutMillis() : PriorityTask.TIMEOUT_NONE;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            return task instanceof PriorityTask ?
                ((PriorityTask) task).getSchedulingPriority() : PriorityTask.SCHEDULE_STANDARD;
            }
        
        // Accessor for the property "Task"
        /**
         * Getter for property Task.<p>
        * The task.
         */
        public com.tangosol.net.Invocable getTask()
            {
            return __m_Task;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll as com.tangosol.coherence.component.net.Poll;
            
            // static child keeps a reference back to the parent
            return (com.tangosol.coherence.component.net.Poll) _newChild("Poll");
            }
        
        // Accessor for the property "Query"
        /**
         * Getter for property Query.<p>
        * Set to true if this Request represents a query() call; false for
        * execute() call
         */
        public boolean isQuery()
            {
            return __m_Query;
            }
        
        // Accessor for the property "RespondInOrder"
        /**
         * Getter for property RespondInOrder.<p>
        * Specifies whether or not this request has to be responded to in the
        * order of arrival (for a given Member).
         */
        public boolean isRespondInOrder()
            {
            return __m_RespondInOrder;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
            // import Component.Util.WindowedArray;
            // import com.tangosol.net.Invocable;
            
            super.onReceived();
            
            InvocationService service = (InvocationService) getService();
            
            Throwable exception = getException();
            if (exception == null)
                {
                Invocable task = getTask();
                try
                    {
                    // initialization is done on the service thread, while
                    // the invocation itself could be executed by a daemon thread
                    task.init(service);
                    }
                catch (Throwable e)
                    {
                    exception = e;
                    }
                }
            
            if (exception == null)
                {
                com.tangosol.coherence.component.util.DaemonPool pool = service.getDaemonPool();
                if (pool.isStarted())
                    {
                    if (isRespondInOrder())
                        {
                        WindowedArray waProcess = service.getProcessArray(getFromMember());
                        setProcessId(waProcess.add(this));
                        }
                    pool.add(this);
                    }
                else
                    {
                    setRespondInOrder(false);
                    service.onInvocationRequest(this);
                    }
                }
            else
                {
                InvocationService.InvocationResponse msgResponse = (InvocationService.InvocationResponse)
                    service.instantiateMessage("InvocationResponse");
                msgResponse.setException(exception);
                msgResponse.respondTo(this);
                service.send(msgResponse);
                }
            }
        
        // From interface: com.oracle.coherence.common.base.Continuation
        public void proceed(Object oResult)
            {
            // import Component.Net.Message;
            // import Component.Util.WindowedArray;
            
            InvocationService             service     = (InvocationService) getService();
            InvocationService.InvocationResponse msgResponse = (InvocationService.InvocationResponse)
                service.instantiateMessage("InvocationResponse");
            
            if (oResult instanceof Throwable)
                {
                msgResponse.setException((Throwable) oResult);
                }
            else
                {
                msgResponse.setResult(oResult);
                }
            
            if (isRespondInOrder())
                {
                WindowedArray waProcess = service.getProcessArray(this.getFromMember());
                synchronized (waProcess)
                    {
                    long lProcessId = getProcessId();
                    if (lProcessId == waProcess.getFirstIndex())
                        {
                        waProcess.remove(lProcessId);
                        service.sendResponse(this, msgResponse);
              
                        // check for pending responses
                        while (true)
                            {
                            Message msg = (Message) waProcess.get(++lProcessId);
                            if (msg instanceof InvocationService.InvocationResponse)
                                {
                                waProcess.remove(lProcessId);
                                service.sendResponse(this, (InvocationService.InvocationResponse) msg);
                                }
                            else
                                {
                                break;
                                }
                            }
                        }
                    else
                        {
                        // defer sending ...
                        _assert(lProcessId > waProcess.getFirstIndex());
                        waProcess.set(lProcessId, msgResponse);
                        }
                    }
                }
            else
                {
                service.sendResponse(this, msgResponse);
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.net.Invocable;
            // import java.io.IOException;
            
            super.read(input);
            
            try
                {
                setTask((Invocable) readObject(input));
                setRespondInOrder(input.readBoolean());
                }
            catch (IOException e)
                {
                setException(e);
                _trace("Failure to deserialize an Invocable object: " + e
                     + "\n" + getStackTrace(e), 1);
                }
            }
        
        // From interface: java.lang.Runnable
        /**
         * Executes on a [daemon pool] worker thread.
         */
        public void run()
            {
            ((InvocationService) getService()).onInvocationRequest(this);
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.Invocable;
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.net.RequestTimeoutException;
            
            InvocationService             service     = (InvocationService) getService();
            InvocationService.InvocationResponse msgResponse = (InvocationService.InvocationResponse)
                service.instantiateMessage("InvocationResponse");
            msgResponse.respondTo(this);
            
            try
                {
                Invocable task = getTask();
                if (task instanceof PriorityTask)
                    {
                    ((PriorityTask) task).runCanceled(fAbandoned);
                    }
            
                String sMsg = (fAbandoned ? "Abandoned " : "Canceled ") +
                    getDescription() + "; Service=" + service.getServiceName();
                msgResponse.setException(new RequestTimeoutException(sMsg));
                }
            catch (Throwable e)
                {
                msgResponse.setException(e);
                }
            
            service.send(msgResponse);
            }
        
        // Accessor for the property "Exception"
        /**
         * Setter for property Exception.<p>
        * A deserialization exception caught by the read().
         */
        protected void setException(Throwable e)
            {
            __m_Exception = e;
            }
        
        // Accessor for the property "Observer"
        /**
         * Setter for property Observer.<p>
        * The observer (optional). Used only if Query is false.
         */
        public void setObserver(com.tangosol.net.InvocationObserver observer)
            {
            __m_Observer = observer;
            }
        
        // Accessor for the property "ProcessId"
        /**
         * Setter for property ProcessId.<p>
        * Transient property used by the Service in case when Invocable tasks
        * from a given member have to be responded to in the order of arrival. 
        * Indicates this task's arrival id.
         */
        public void setProcessId(long lProcessId)
            {
            __m_ProcessId = lProcessId;
            }
        
        // Accessor for the property "Query"
        /**
         * Setter for property Query.<p>
        * Set to true if this Request represents a query() call; false for
        * execute() call
         */
        public void setQuery(boolean fQuery)
            {
            __m_Query = fQuery;
            }
        
        // Accessor for the property "RespondInOrder"
        /**
         * Setter for property RespondInOrder.<p>
        * Specifies whether or not this request has to be responded to in the
        * order of arrival (for a given Member).
         */
        public void setRespondInOrder(boolean fDone)
            {
            __m_RespondInOrder = fDone;
            }
        
        // Accessor for the property "Task"
        /**
         * Setter for property Task.<p>
        * The task.
         */
        public void setTask(com.tangosol.net.Invocable task)
            {
            __m_Task = task;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            writeObject(output, getTask());
            output.writeBoolean(isRespondInOrder());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService$InvocationRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            /**
             * Property Observer
             *
             * @see $InvocationRequest#Observer property
             */
            private transient com.tangosol.net.InvocationObserver __m_Observer;
            
            /**
             * Property Query
             *
             * @see $InvocationRequest#Query property
             */
            private transient boolean __m_Query;
            
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService.InvocationRequest.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/InvocationService$InvocationRequest$Poll".replace('/', '.'));
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
            
            // Accessor for the property "Observer"
            /**
             * Getter for property Observer.<p>
            * @see $InvocationRequest#Observer property
             */
            public com.tangosol.net.InvocationObserver getObserver()
                {
                return __m_Observer;
                }
            
            // Accessor for the property "Query"
            /**
             * Getter for property Query.<p>
            * @see $InvocationRequest#Query property
             */
            public boolean isQuery()
                {
                return __m_Query;
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                // import com.tangosol.net.InvocationObserver;
                
                InvocationObserver observer = getObserver();
                
                if (observer != null)
                    {
                    try
                        {
                        observer.invocationCompleted();
                        }
                    catch (Throwable e)
                        {
                        _trace(e);
                        }
                    }
                
                super.onCompletion();
                }
            
            // Declared at the super level
            /**
             * The "component has been initialized" method-notification called
            * out of setConstructed() for the topmost component and that in
            * turn notifies all the children.
            * 
            * This notification gets called before the control returns back to
            * this component instantiator (using <code>new Component.X()</code>
            * or <code>_newInstance(sName)</code>) and on the same thread. In
            * addition, visual components have a "posted" notification
            * <code>onInitUI</code> that is called after (or at the same time
            * as) the control returns back to the instantiator and possibly on
            * a different thread.
             */
            public void onInit()
                {
                // import java.util.HashMap;
                
                super.onInit();
                
                InvocationService.InvocationRequest msg = (InvocationService.InvocationRequest) get_Parent();
                
                if (msg.isQuery())
                    {
                    setQuery(true);
                    setResult(new HashMap());
                    }
                else
                    {
                    setObserver(msg.getObserver());
                    }
                }
            
            // Declared at the super level
            /**
             * This event occurs when a Member has left the Service (or died). 
            * Note: as the service does not assume internal knowledge of the
            * Poll this method will be called for any Member's which leave the
            * service, include ones which were never part of the Poll.
             */
            public void onLeft(com.tangosol.coherence.component.net.Member member)
                {
                // import com.tangosol.net.InvocationObserver;
                
                InvocationObserver observer = getObserver();
                if (observer != null)
                    {
                    try
                        {
                        // filter out events for memebers which we don't care about
                        if (getRemainingMemberSet().contains(member))
                            {
                            observer.memberLeft(member);
                            }
                        }
                    catch (Throwable e)
                        {
                        _trace(e);
                        }
                    }
                
                super.onLeft(member);
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                // import Component.Net.Member;
                // import com.tangosol.net.InvocationObserver;
                // import com.tangosol.net.NonBlockingInvocable;
                // import java.util.Map;
                
                Member member = msg.getFromMember();
                
                InvocationService.InvocationResponse msgResponse = (InvocationService.InvocationResponse) msg;
                
                Throwable exception = msgResponse.getException();
                Object    oResult   = msgResponse.getResult();
                
                if (isQuery())
                    {
                    // for NonBlockingInvocable, we need to return an exception as a result
                    InvocationService.InvocationRequest msgRequest = (InvocationService.InvocationRequest) get_Parent();
                    if (exception != null &&
                            msgRequest.getTask() instanceof NonBlockingInvocable)
                        {
                        oResult   = exception;
                        exception = null; // no reason to report anymore
                        }
                    ((Map) getResult()).put(member, oResult);
                    }
                else
                    {
                    InvocationObserver observer = getObserver();
                    if (observer != null)
                        {
                        try
                            {
                            if (exception == null)
                                {
                                observer.memberCompleted(member, oResult);
                                }
                            else
                                {
                                observer.memberFailed(member, exception);
                                exception = null; // no reason to report anymore
                                }
                            }
                        catch (Throwable e)
                            {
                            _trace(e);
                            }
                        }
                    }
                
                if (exception != null)
                    {
                    _trace("Invocation request to " + member + " threw an exception \n" +
                        getStackTrace(exception), 1);
                    }
                
                super.onResponse(msg);
                }
            
            // Accessor for the property "Observer"
            /**
             * Setter for property Observer.<p>
            * @see $InvocationRequest#Observer property
             */
            public void setObserver(com.tangosol.net.InvocationObserver observer)
                {
                __m_Observer = observer;
                }
            
            // Accessor for the property "Query"
            /**
             * Setter for property Query.<p>
            * @see $InvocationRequest#Query property
             */
            public void setQuery(boolean fQuery)
                {
                __m_Query = fQuery;
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService$InvocationResponse
    
    /**
     * Response message to an InvocationRequest
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvocationResponse
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Exception
         *
         * An exception that occured while running the invocation.
         */
        private Throwable __m_Exception;
        
        /**
         * Property Result
         *
         * The result, if any, of the invocation.
         */
        private Object __m_Result;
        
        // Default constructor
        public InvocationResponse()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvocationResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(34);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.InvocationService.InvocationResponse();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/InvocationService$InvocationResponse".replace('/', '.'));
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
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "InvocationResponse{" +
                   (getException() == null ?
                       "Exception=" + getException() : "Result=" + getResult()) +
                   '}';
            }
        
        // Accessor for the property "Exception"
        /**
         * Getter for property Exception.<p>
        * An exception that occured while running the invocation.
         */
        public Throwable getException()
            {
            return __m_Exception;
            }
        
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * The result, if any, of the invocation.
         */
        public Object getResult()
            {
            return __m_Result;
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);
            
            try
                {
                setException((Throwable) readObject(input));
                setResult(readObject(input));
                }
            catch (Exception e)
                {
                setException(e);
                }
            }
        
        // Accessor for the property "Exception"
        /**
         * Setter for property Exception.<p>
        * An exception that occured while running the invocation.
         */
        public void setException(Throwable exception)
            {
            __m_Exception = exception;
            }
        
        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * The result, if any, of the invocation.
         */
        public void setResult(Object oResult)
            {
            __m_Result = oResult;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            writeObject(output, getException());
            writeObject(output, getResult());
            }
        }
    }
