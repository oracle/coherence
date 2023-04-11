
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.StorageRequest

package com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.net.partition.PartitionSet;

/**
 * StorageRequest is a DistributedCacheRequest that is used to replicate
 * storage-related metadata (e.g. Indices, Listeners) service-wide.  A
 * StorageRequest is sent from the requestor to the (storage) senior, and from
 * the senior to all (existing and future) storage members.  StorageRequests
 * must be idempotent.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class StorageRequest
        extends    com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property Response
     *
     * Response message to be sent to the requestor upon poll completion.
     */
    private transient com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse __m_Response;
    
    /**
     * Property RESPONSE_RETRY
     *
     */
    public static final Object RESPONSE_RETRY;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        try
            {
            RESPONSE_RETRY = new java.lang.Object();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Poll", StorageRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public StorageRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public StorageRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.StorageRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/StorageRequest".replace('/', '.'));
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
     * Check whether or not the response value for this request is valid.
     */
    public boolean checkResponse(Object oResponse)
        {
        if (oResponse == RESPONSE_RETRY)
            {
            return false;
            }
        if (oResponse instanceof RuntimeException)
            {
            throw (RuntimeException) oResponse;
            }
        
        return true;
        }
    
    // Declared at the super level
    /**
     * Getter for property RequestPartitions.<p>
    * (Calculated) Set of partitions that need to be processed for this
    * request. This value is never null for asynchronous requests.
     */
    public com.tangosol.net.partition.PartitionSet getRequestPartitions()
        {
        return null;
        }
    
    // Accessor for the property "Response"
    /**
     * Getter for property Response.<p>
    * Response message to be sent to the requestor upon poll completion.
     */
    public com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse getResponse()
        {
        return __m_Response;
        }
    
    // Accessor for the property "RelayedRequest"
    /**
     * Getter for property RelayedRequest.<p>
    * True iff this request is being relayed by the storage senior to the
    * storage member
     */
    public boolean isRelayedRequest()
        {
        return getResponse() != null;
        }
    
    // Declared at the super level
    /**
     * Preprocess this message.
    * 
    * @return true iff this message has been fully processed (onReceived was
    * called)
     */
    public boolean preprocess()
        {
        // in a number of places we assume the StorageRequest executes on the service thread
        return false;
        }
    
    /**
     * Called by the storage-senior to relay this message to the remaining set
    * of service members (on behalf of the requestor) and to ensure that the
    * requestor is responded to upon completion.
    * 
    * @param msgResponse    the response to the requestor
     */
    public void relayRequest(com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse msgResponse)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService service    = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService) getService();
        Member  memberThis = service.getThisMember();
        
        // only the storage senior is allowed to relay storage requests
        _assert(service.getOwnershipSenior() == memberThis);
        
        // Note: the set of ownership members to relay the request to must include
        //       members still in the process of joining (in the OWNERSHIP_PENDING
        //       state), as we may have already welcomed the new member to the service.
        MemberSet setOwners = service.getOwnershipMemberSet(/*fIncludePending*/ true);
        setOwners.remove(memberThis);
        
        if (setOwners.isEmpty())
            {
            // no additional members to relay the message to; send the
            // response to the requestor immediately
            service.post(msgResponse);
            }
        else
            {
            // response to the original requestor is posted upon poll completion
            StorageRequest msgRelay = (StorageRequest) cloneMessage();
            msgRelay.setToMemberSet(setOwners);
            msgRelay.setResponse(msgResponse);
        
            service.post(msgRelay);
            }
        }
    
    // Accessor for the property "Response"
    /**
     * Setter for property Response.<p>
    * Response message to be sent to the requestor upon poll completion.
     */
    protected void setResponse(com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse msgResponse)
        {
        __m_Response = msgResponse;
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.StorageRequest$Poll
    
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
            extends    com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest.Poll
        {
        // ---- Fields declarations ----
        
        /**
         * Property ResponseList
         *
         */
        private java.util.List __m_ResponseList;
        
        /**
         * Property ResultType
         *
         * One of the SimpleResponse.RESPONSE_* constants
         */
        private int __m_ResultType;
        
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
            return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.StorageRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/StorageRequest$Poll".replace('/', '.'));
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
        
        // Accessor for the property "ResponseList"
        /**
         * Getter for property ResponseList.<p>
         */
        public java.util.List getResponseList()
            {
            return __m_ResponseList;
            }
        
        // Accessor for the property "ResultType"
        /**
         * Getter for property ResultType.<p>
        * One of the SimpleResponse.RESPONSE_* constants
         */
        public int getResultType()
            {
            return __m_ResultType;
            }
        
        /**
         * Merge the list of relayed responses into the single "merged" response.
         */
        protected void mergeResponse(com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse msgResponse)
            {
            // import Component.Net.Message.ResponseMessage.SimpleResponse as com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse;
            // import com.tangosol.net.partition.PartitionSet;
            
            // merge the specified response;
            // SUCCESS can be overwritten with RETRY, and both can be overwritten with FAILURE
            com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse msgMerged = (com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse) ((StorageRequest) get_Parent()).getResponse();
            switch (msgResponse.getResult())
                {
                case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_FAILURE:
                    msgMerged.setValue(msgResponse.getFailure());
                    msgMerged.setResult(com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_FAILURE);
                    break;
            
                case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_RETRY:
                    if (msgMerged.getResult() == com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_SUCCESS)
                        {
                        msgMerged.setValue(null);
                        msgMerged.setResult(com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_RETRY);
                        }
                    break;
                    
                case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_SUCCESS:
                    if (msgMerged.getResult() == com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_SUCCESS)
                        {
                        PartitionSet partsMerged = (PartitionSet) msgMerged.getValue();
                        Object       respValue   = msgResponse.getValue();
                        PartitionSet partsThis   = null;
            
                        if (respValue instanceof PartitionSet)
                            {
                            partsThis = (PartitionSet) respValue;
                            if (partsMerged == null)
                                {
                                msgMerged.setValue(partsThis);
                                }
                            else
                                {
                                partsMerged.add(partsThis);
                                }
                            }
                        else
                            {
                            _trace("StorageRequest$Poll.mergeResponse received msgResponse: " + msgResponse
                                   + "; the response value is not of type PartitionSet: " + respValue
                                   + "; it's of type: " + respValue.getClass(), 2);
                            }
                        }
                    break;
                }
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when all the Members that were
        * polled have responded or have left the Service.
         */
        protected void onCompletion()
            {
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
            // import Component.Net.Message.ResponseMessage.SimpleResponse as com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse;
            
            super.onCompletion();
            
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService service     = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService) getService();
            boolean fIncomplete = !getLeftMemberSet().isEmpty() || !getRemainingMemberSet().isEmpty();
            StorageRequest msgRequest  = (StorageRequest) get_Parent();
            if (msgRequest.isRelayedRequest())
                {
                // storage requests may only be relayed by the storage senior
                _assert(service.getOwnershipSenior() == service.getThisMember());
            
                // post the response to the original requestor
                com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse msgResponse = msgRequest.getResponse();
                if (fIncomplete)
                    {
                    // poll was closed before some members could response; force a retry
                    msgResponse.setValue(null);
                    msgResponse.setResult(com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_RETRY);
                    }
            
                service.post(msgResponse);
                }
            else
                {
                if (fIncomplete)
                    {
                    setResult(StorageRequest.RESPONSE_RETRY);
                    }
                else
                    {
                    com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse msgResponse = (com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse) getResult();
                    switch (msgResponse.getResult())
                        {
                        case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_SUCCESS:
                            setResult(msgResponse.getValue());
                            break;
            
                        case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_RETRY:
                            setResult(StorageRequest.RESPONSE_RETRY);
                            break;
            
                        case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_FAILURE:
                            setResult(msgResponse.getFailure());
                            break;
            
                        default:
                            throw new IllegalStateException();
                        }
                    }
                }
            }
        
        // Declared at the super level
        /**
         * This event occurs for each response Message from each polled Member.
         */
        public void onResponse(com.tangosol.coherence.component.net.Message msg)
            {
            // import Component.Net.Message.ResponseMessage.SimpleResponse as com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse;
            
            StorageRequest msgRequest = (StorageRequest) get_Parent();
            if (msgRequest.isRelayedRequest())
                {
                mergeResponse((com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse) msg);
                }
            
            super.onResponse(msg);
            }
        
        // Accessor for the property "ResponseList"
        /**
         * Setter for property ResponseList.<p>
         */
        protected void setResponseList(java.util.List list)
            {
            __m_ResponseList = list;
            }
        
        // Accessor for the property "ResultType"
        /**
         * Setter for property ResultType.<p>
        * One of the SimpleResponse.RESPONSE_* constants
         */
        public void setResultType(int pResultType)
            {
            __m_ResultType = pResultType;
            }
        }
    }
