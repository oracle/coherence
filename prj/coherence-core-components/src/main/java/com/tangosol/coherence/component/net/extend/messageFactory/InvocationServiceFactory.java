
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.messaging.ConnectionException;

/**
 * MessageFactory for version 1 of the InvocationService Protocol.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class InvocationServiceFactory
        extends    com.tangosol.coherence.component.net.extend.MessageFactory
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
        __mapChildren.put("InvocationRequest", InvocationServiceFactory.InvocationRequest.get_CLASS());
        __mapChildren.put("Response", InvocationServiceFactory.Response.get_CLASS());
        }
    
    // Default constructor
    public InvocationServiceFactory()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public InvocationServiceFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/InvocationServiceFactory".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory$InvocationRequest
    
    /**
     * InvocationService.query(Invocable task, Set setMembers) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvocationRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.InvocationServiceRequest
            implements com.tangosol.net.PriorityTask
        {
        // ---- Fields declarations ----
        
        /**
         * Property Task
         *
         * The Invocable task to execute.
         */
        private com.tangosol.net.Invocable __m_Task;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 1;
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
            __mapChildren.put("Status", InvocationServiceFactory.InvocationRequest.Status.get_CLASS());
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory.InvocationRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/InvocationServiceFactory$InvocationRequest".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription() + ", Task=" + getTask();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.Invocable;
            // import com.tangosol.net.PriorityTask;
            
            Invocable task = getTask();
            return task instanceof PriorityTask
                    ? ((PriorityTask) task).getExecutionTimeoutMillis()
                    : PriorityTask.TIMEOUT_NONE;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            // import com.tangosol.net.Invocable;
            // import com.tangosol.net.PriorityTask;
            
            Invocable task = getTask();
            return task instanceof PriorityTask
                    ? ((PriorityTask) task).getRequestTimeoutMillis()
                    : PriorityTask.TIMEOUT_NONE;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.Invocable;
            // import com.tangosol.net.PriorityTask;
            
            Invocable task = getTask();
            return task instanceof PriorityTask
                    ? ((PriorityTask) task).getSchedulingPriority()
                    : PriorityTask.SCHEDULE_STANDARD;
            }
        
        // Accessor for the property "Task"
        /**
         * Getter for property Task.<p>
        * The Invocable task to execute.
         */
        public com.tangosol.net.Invocable getTask()
            {
            return __m_Task;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Declared at the super level
        /**
         * Called when the Request is run.
        * 
        * @param response  the Response that should be populated with the
        * result of running the Request
         */
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            // import com.tangosol.net.Invocable;
            // import com.tangosol.net.InvocationService;
            // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
            
            com.tangosol.net.messaging.Channel   channel = getChannel();
            Invocable task    = getTask();
            _assert(task != null);
            
            InvocationService service = getInvocationService();
            _assert(service != null);
            
            response.setResult(service.query(task, null).values().iterator().next());
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.net.Invocable;
            
            super.readExternal(in);
            
            setTask((Invocable) in.readObject(1));
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import Component.Net.Extend.Channel;
            // import Component.Net.Extend.Message.Response as com.tangosol.coherence.component.net.extend.message.Response;
            // import com.tangosol.net.Invocable;
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.net.RequestTimeoutException;
            // import com.tangosol.net.messaging.ConnectionException;
            
            com.tangosol.coherence.component.net.extend.message.Response response = (com.tangosol.coherence.component.net.extend.message.Response) ensureResponse();
            try
                {
                Invocable task = getTask();
                if (task instanceof PriorityTask)
                    {
                    ((PriorityTask) task).runCanceled(fAbandoned);
                    }
            
                response.setFailure(true);        
                response.setResult(new RequestTimeoutException((fAbandoned ? "Abandoned " : "Canceled ") + this));
                }
            catch (Throwable e)
                {
                response.setFailure(true);
                response.setResult(e);
                }
            
            response.setRequestId(getId());
            
            // send the com.tangosol.coherence.component.net.extend.message.Response; since this method is invoked by a different thread than
            // the one that executed the Request, we must make sure to enter the Channel
            // before sending the com.tangosol.coherence.component.net.extend.message.Response (@see Channel#post)
            Channel channel = (Channel) getChannel();
            try
                {
                channel.gateEnter();
                }
            catch (ConnectionException e)
                {
                // ignore: the Channel or Connection is closed or closing
                return;
                }
            try
                {
                channel.send(response);
                }
            finally
                {
                channel.gateExit();
                }
            }
        
        // Accessor for the property "Task"
        /**
         * Setter for property Task.<p>
        * The Invocable task to execute.
         */
        public void setTask(com.tangosol.net.Invocable task)
            {
            __m_Task = task;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(1, getTask());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory$InvocationRequest$Status
        
        /**
         * Implementation of the Request$Status interface.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Status
                extends    com.tangosol.coherence.component.net.extend.message.Request.Status
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Status()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory.InvocationRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/InvocationServiceFactory$InvocationRequest$Status".replace('/', '.'));
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
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory$Response
    
    /**
     * Generic Response component used for basic InvocationService Protocol
     * Responses.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Response
            extends    com.tangosol.coherence.component.net.extend.message.Response
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 0;
        
        // Default constructor
        public Response()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Response(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory.Response();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/InvocationServiceFactory$Response".replace('/', '.'));
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
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Declared at the super level
        public void run()
            {
            // no-op
            }
        }
    }
