
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.ChainedRequest

package com.tangosol.coherence.component.net.message.requestMessage;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.memberSet.EmptyMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A ChainedRequest is a RequestMessage which is logically sent (by sender S)
 * to a series of members
 * 
 * S->M1->M2->M3...
 * 
 * Each member (M1) in the delivery chain is responsible for forwarding the
 * message to the next member (M2) in the chain, and upon successful
 * completion, responding back to the previous member in the chain.  If the
 * member (M2) leaves before responding to the previous member in the chain
 * (M1), that member (M1) is responsible for resending the request to the next
 * member in the chain (M3), preserving the original order.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ChainedRequest
        extends    com.tangosol.coherence.component.net.message.RequestMessage
    {
    // ---- Fields declarations ----
    
    /**
     * Property IncomingRequest
     *
     * The request logically preceding this one in the delivery chain.
     * 
     * This property is transient, and only held by the message sender.
     */
    private transient ChainedRequest __m_IncomingRequest;
    
    /**
     * Property MemberList
     *
     * The List of members that this chained request message should be
     * delivered to, or null for all members. 
     */
    private java.util.List __m_MemberList;
    
    /**
     * Property OriginatingMemberId
     *
     * The id of the Member which this chained request originated from.
     */
    private int __m_OriginatingMemberId;
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
        __mapChildren.put("Poll", ChainedRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public ChainedRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ChainedRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.ChainedRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/ChainedRequest".replace('/', '.'));
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
        // import java.util.ArrayList;
        
        ChainedRequest msg = (ChainedRequest) super.cloneMessage();
        
        msg.setMemberList(new ArrayList(getMemberList()));
        msg.setOriginatingMemberId(getOriginatingMemberId());
        
        return msg;
        }
    
    // Declared at the super level
    /**
     * Route the message towards the proper service.  For local services (within
    * this member) the message is simply be placed on service's queue.  For
    * messages sent to services running on other members the message will be
    * forwarded to the publisher. The actual transmission of the message may be
    * deferred due to the send queue batching.
    * 
    * @param msg  the message to dispatch
     */
    protected void dispatch(com.tangosol.coherence.component.net.Message msg)
        {
        ChainedRequest req = (ChainedRequest) msg;
        if (req.getOriginatingMemberId() == 0)
            {
            req.setOriginatingMemberId(req.getService().getThisMember().getId());
            }
        
        super.dispatch(req);
        }
    
    /**
     * Forward this message to the next member in the delivery chain.
    * 
    * @return true iff the message was sent to the next member
     */
    protected boolean forwardMessage(ChainedRequest msgIncoming)
        {
        if (isTerminal())
            {
            return false;
            }
        else
            {
            // prepare the message to be forwarded to the next member
            ChainedRequest msgNext = (ChainedRequest) cloneMessage();
            msgNext.setIncomingRequest(msgIncoming);
        
            getService().post(msgNext);
            return true;
            }
        }
    
    /**
     * Helper method used to foward this request to the next recipient in the
    * chain, or to reply if there are no more recipients.
     */
    public void forwardOrReply(ChainedRequest msgIncoming)
        {
        // import Component.Net.Message;
        
        if (forwardMessage(msgIncoming))
            {
            // this message will be responded to when the poll closes
            }
        else
            {
            // no more members in the delivery chain
            if (msgIncoming == null)
                {
                // no incoming request to reply to so we must be the request originator
                // and thus the request is completely delivered
                onRequestCompleted();
                }
            else
                {
                onRequestDelivered();
        
                Message msgResponse = msgIncoming.instantiateResponse();
                msgResponse.respondTo(msgIncoming);
        
                getService().post(msgResponse);
                }
            }
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific Message data.
     */
    public String getDescription()
        {
        // import Component.Net.Member;
        // import java.util.Iterator;
        // import java.util.List;
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        
        List listMembers = getMemberList();
        if (listMembers == null)
            {
            sb.append("all");
            }
        else
            {
            for (Iterator iter = listMembers.iterator(); iter.hasNext(); )
                {
                Member member = (Member) iter.next();
                sb.append(member.getId());
                if (iter.hasNext())
                    {
                    sb.append(", ");
                    }
                }
            }
        sb.append(']');
        
        return "MemberList=" + sb.toString();
        }
    
    // Accessor for the property "IncomingRequest"
    /**
     * Getter for property IncomingRequest.<p>
    * The request logically preceding this one in the delivery chain.
    * 
    * This property is transient, and only held by the message sender.
     */
    public ChainedRequest getIncomingRequest()
        {
        return __m_IncomingRequest;
        }
    
    // Accessor for the property "MemberList"
    /**
     * Getter for property MemberList.<p>
    * The List of members that this chained request message should be delivered
    * to, or null for all members. 
     */
    public java.util.List getMemberList()
        {
        return __m_MemberList;
        }
    
    // Accessor for the property "OriginatingMember"
    /**
     * Getter for property OriginatingMember.<p>
    * The Member which this chained request originated from, or null if the
    * member is no longer in the service.
     */
    public com.tangosol.coherence.component.net.Member getOriginatingMember()
        {
        return getService().getServiceMemberSet().getMember(getOriginatingMemberId());
        }
    
    // Accessor for the property "OriginatingMemberId"
    /**
     * Getter for property OriginatingMemberId.<p>
    * The id of the Member which this chained request originated from.
     */
    public int getOriginatingMemberId()
        {
        return __m_OriginatingMemberId;
        }
    
    // Declared at the super level
    protected com.tangosol.coherence.component.net.Poll instantiatePoll()
        {
        return (ChainedRequest.Poll) _newChild("Poll");
        }
    
    /**
     * Instantiate a Response message to be sent to the prior member in the
    * delivery chain.
     */
    public com.tangosol.coherence.component.net.Message instantiateResponse()
        {
        return getService().instantiateMessage("Response");
        }
    
    // Accessor for the property "Terminal"
    /**
     * Getter for property Terminal.<p>
    * True iff this is the end of the chain.
     */
    public boolean isTerminal()
        {
        // import java.util.List;
        
        List listMembers = getMemberList();
        if (listMembers == null)
            {
            return getIncomingRequest() != null && getService().getServiceMemberSet().getSuccessorMember() == null;
            }
        return listMembers.isEmpty();
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
        forwardOrReply(this);
        }
    
    /**
     * This method is called on the originating member of the request when the
    * request has been fully delivered.
     */
    public void onRequestCompleted()
        {
        }
    
    /**
     * This method is called on by member when the request has been fully
    * delivered to all members after it on the delivery chain (or upon
    * receiving the request if this is the last member in the chain).
    * 
    * This event could be used to perform logic in "reverse" chain order, as it
    * guarantees that all successors in the delivery chain have received (and
    * processed) the message.
    * 
    * @param msgResponse    the response
     */
    public void onRequestDelivered()
        {
        }
    
    // Declared at the super level
    /**
     * Asynchronously send this message.  The actual transmission of the message
    * may be deferred due to the send queue batching.
    * This method should not be called directly; see Grid#post(Message).
     */
    public void post()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.EmptyMemberSet;
        // import Component.Net.MemberSet;
        // import java.util.Collections;
        // import java.util.List;
        
        List listMembers = getMemberList();
        if (listMembers == null)
            {
            setToMemberSet(MemberSet.instantiate(getIncomingRequest() == null
                ? getService().getServiceOldestMember()                   // origination message, start chain at senior 
                : getService().getServiceMemberSet().getSuccessorMember() // else, continue chain at sucessor
                ));
            }
        else if (listMembers.isEmpty())
            {
            setToMemberSet((EmptyMemberSet) EmptyMemberSet.get_Instance());
            }
        else
            {
            setToMemberSet(MemberSet.instantiate((Member) listMembers.remove(0)));
        
            if (listMembers.isEmpty())
                {
                // most common path: allow GC to collect the ArrayList right away
                setMemberList(Collections.emptyList());
                }
            }
        
        super.post();
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import java.util.ArrayList;
        // import java.util.Collections;
        // import java.util.List;
        
        super.read(input);
        
        setOriginatingMemberId(input.readInt());
        
        List listMembers;
        int  cMembers = input.readShort();
        if (cMembers == -1)
            {
            listMembers = null;
            }
        else if (cMembers == 0)
            {
            // common case
            listMembers = Collections.emptyList();
            }
        else
            {
            MemberSet setMembers = getService().getServiceMemberSet();
        
            listMembers = new ArrayList(cMembers);
            for (int i = 0; i < cMembers; i++)
                {
                int    nMember = input.readInt();
                Member member  = setMembers.getMember(nMember);
                if (member != null)
                    {
                    listMembers.add(member);
                    }
                }
            }
        
        setMemberList(listMembers);
        }
    
    // Accessor for the property "IncomingRequest"
    /**
     * Setter for property IncomingRequest.<p>
    * The request logically preceding this one in the delivery chain.
    * 
    * This property is transient, and only held by the message sender.
     */
    protected void setIncomingRequest(ChainedRequest msg)
        {
        __m_IncomingRequest = msg;
        }
    
    // Accessor for the property "MemberList"
    /**
     * Setter for property MemberList.<p>
    * The List of members that this chained request message should be delivered
    * to, or null for all members. 
     */
    public void setMemberList(java.util.List listMember)
        {
        __m_MemberList = listMember;
        }
    
    // Accessor for the property "OriginatingMemberId"
    /**
     * Setter for property OriginatingMemberId.<p>
    * The id of the Member which this chained request originated from.
     */
    public void setOriginatingMemberId(int nId)
        {
        __m_OriginatingMemberId = nId;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Net.Member;
        // import java.util.List;
        
        super.write(output);
        
        // write the originating member-id
        output.writeInt(getOriginatingMemberId());
        
        // write the list of member-ids
        List listMembers = getMemberList();
        int  cMembers    = listMembers == null ? -1 : listMembers.size();
        
        output.writeShort(cMembers);
        if (cMembers > 0)
            {
            // utilize the fact that listMembers is an ArrayList
            for (int i = 0; i < cMembers; i++)
                {
                Member member = (Member) listMembers.get(i);
                output.writeInt(member.getId());
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.ChainedRequest$Poll
    
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
            extends    com.tangosol.coherence.component.net.Poll
        {
        // ---- Fields declarations ----
        
        /**
         * Property Response
         *
         * A holder for a response message to be used by the run() method.
         * 
         * @see #preprocessResponse
         */
        private transient com.tangosol.coherence.component.net.Message __m_Response;
        
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
        
        // Getter for virtual constant Preprocessable
        public boolean isPreprocessable()
            {
            return true;
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.net.message.requestMessage.ChainedRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/ChainedRequest$Poll".replace('/', '.'));
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
        
        // Accessor for the property "Response"
        /**
         * Getter for property Response.<p>
        * A holder for a response message to be used by the run() method.
        * 
        * @see #preprocessResponse
         */
        protected com.tangosol.coherence.component.net.Message getResponse()
            {
            return __m_Response;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when all the Members that were
        * polled have responded or have left the Service.
         */
        protected void onCompletion()
            {
            // import Component.Net.Message;
            // import Component.Util.Daemon.QueueProcessor.Service;
            
            ChainedRequest msgRequest  = (ChainedRequest) get_Module();
            ChainedRequest msgIn       = msgRequest.getIncomingRequest();
            Message msgResponse = (Message) getResult();
            
            if (msgResponse == null)
                {
                if (getService().getServiceState() == Service.SERVICE_STOPPED)
                    {
                    // the poll was closed due to service exit; we should not continue
                    // to forward the message, and we must not consider the request
                    // "completed" since the delivery has not been guaranteed
                    }
                else
                    {
                    // the poll closed without the member responding.  Either the
                    // member left, or the poll was configured to close upon receiving
                    // sent notification.  All pending polls for preceding ChainedRequests
                    // (of the same type following the same path) have been closed and the
                    // corresponding messages forwarded.
                    //
                    // forward to the next recipient, or if the chain is exhausted, reply
                    // back to the previous member in the chain
                    msgRequest.forwardOrReply(msgIn);
                    }
                }
            else
                {
                // the message delivery has been completed and a response was received
                if (msgIn == null)
                    {
                    // no incoming request; this is the request originator
                    msgRequest.onRequestCompleted();
                    }
                else
                    {
                    msgRequest.onRequestDelivered();
            
                    msgResponse = msgResponse.cloneMessage();
                    msgResponse.respondTo(msgIn);
                    getService().post(msgResponse);
                    }
                }
            
            // no need to call super
            // super.onCompletion();
            }
        
        // Declared at the super level
        /**
         * This event occurs for each response Message from each polled Member.
         */
        public void onResponse(com.tangosol.coherence.component.net.Message msg)
            {
            setResult(msg);
            
            super.onResponse(msg);
            }
        
        // Accessor for the property "Response"
        /**
         * Setter for property Response.<p>
        * A holder for a response message to be used by the run() method.
        * 
        * @see #preprocessResponse
         */
        protected void setResponse(com.tangosol.coherence.component.net.Message msgResponse)
            {
            __m_Response = msgResponse;
            }
        }
    }
