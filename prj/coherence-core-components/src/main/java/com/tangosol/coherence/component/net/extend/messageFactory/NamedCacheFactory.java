
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.tangosol.coherence.component.net.extend.RemoteNamedCache;
import com.tangosol.coherence.component.net.extend.proxy.MapListenerProxy;
import com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.ServiceInfo;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Base;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.PartitionedFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NamedCacheProtocol Version:
 * 
 * (2) MessageFactory
 * (3) COH-6337    Add cookie support for LimitFilter
 * (4) COH-8238    Add FilterIds to MapEvent
 * (5) COH-9355    Add TransformationState to MapEvent
 * (6) COH-4615    Add Priming events support
 * (6) COH-10216   Add cache truncate support
 * ...
 * (10) COH-25175  Add NoStorageMembers message support
 * (11) COH-24968  Add cache isReady support
 * 
 * The type identifiers of the Message classes instantiated by this
 * MessageFactory are organized as follows:
 * 
 * Map (1-10):
 * 
 * (1) SizeRequest
 * (2) ContainsKeyRequest
 * (3) ContainsValueRequest
 * (4) GetRequest 
 * (5) PutRequest
 * (6) RemoveRequest
 * (7) PutAllRequest
 * (8) ClearRequest
 * (9) ContainsAllRequest
 * (10) RemoveAllRequest
 * 
 * ObservableMap (11-20):
 * 
 * (11) ListenerKeyRequest
 * (12) ListenerFilterRequest
 * (13) MapEvent
 * 
 * CacheMap (21-30):
 * 
 * (21) GetAllRequest 
 * 
 * ConcurrentMap (31-40):
 * 
 * (31) LockRequest
 * 
 * QueryMap (41-50):
 * 
 * (41) QueryRequest
 * (42) IndexRequest
 * 
 * InvocableMap (51-60):
 * 
 * (51) AggregateAllRequest
 * (52) AggregateFilterRequest
 * (53) InvokeRequest
 * (54) InvokeAllRequest
 * (55) InvokeFilterRequest
 * (56) NoStorageMembers
 *
 * Other (61-70)
 *
 * (61) ReadyRequest
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class NamedCacheFactory
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
        __mapChildren.put("AggregateAllRequest", NamedCacheFactory.AggregateAllRequest.get_CLASS());
        __mapChildren.put("AggregateFilterRequest", NamedCacheFactory.AggregateFilterRequest.get_CLASS());
        __mapChildren.put("ClearRequest", NamedCacheFactory.ClearRequest.get_CLASS());
        __mapChildren.put("ContainsAllRequest", NamedCacheFactory.ContainsAllRequest.get_CLASS());
        __mapChildren.put("ContainsKeyRequest", NamedCacheFactory.ContainsKeyRequest.get_CLASS());
        __mapChildren.put("ContainsValueRequest", NamedCacheFactory.ContainsValueRequest.get_CLASS());
        __mapChildren.put("GetAllRequest", NamedCacheFactory.GetAllRequest.get_CLASS());
        __mapChildren.put("GetRequest", NamedCacheFactory.GetRequest.get_CLASS());
        __mapChildren.put("IndexRequest", NamedCacheFactory.IndexRequest.get_CLASS());
        __mapChildren.put("InvokeAllRequest", NamedCacheFactory.InvokeAllRequest.get_CLASS());
        __mapChildren.put("InvokeFilterRequest", NamedCacheFactory.InvokeFilterRequest.get_CLASS());
        __mapChildren.put("InvokeRequest", NamedCacheFactory.InvokeRequest.get_CLASS());
        __mapChildren.put("ListenerFilterRequest", NamedCacheFactory.ListenerFilterRequest.get_CLASS());
        __mapChildren.put("ListenerKeyRequest", NamedCacheFactory.ListenerKeyRequest.get_CLASS());
        __mapChildren.put("LockRequest", NamedCacheFactory.LockRequest.get_CLASS());
        __mapChildren.put("MapEvent", NamedCacheFactory.MapEvent.get_CLASS());
        __mapChildren.put("NoStorageMembers", NamedCacheFactory.NoStorageMembers.get_CLASS());
        __mapChildren.put("PartialResponse", NamedCacheFactory.PartialResponse.get_CLASS());
        __mapChildren.put("PutAllRequest", NamedCacheFactory.PutAllRequest.get_CLASS());
        __mapChildren.put("PutRequest", NamedCacheFactory.PutRequest.get_CLASS());
        __mapChildren.put("QueryRequest", NamedCacheFactory.QueryRequest.get_CLASS());
        __mapChildren.put("ReadyRequest", NamedCacheFactory.ReadyRequest.get_CLASS());
        __mapChildren.put("RemoveAllRequest", NamedCacheFactory.RemoveAllRequest.get_CLASS());
        __mapChildren.put("RemoveRequest", NamedCacheFactory.RemoveRequest.get_CLASS());
        __mapChildren.put("Response", NamedCacheFactory.Response.get_CLASS());
        __mapChildren.put("SizeRequest", NamedCacheFactory.SizeRequest.get_CLASS());
        __mapChildren.put("UnlockRequest", NamedCacheFactory.UnlockRequest.get_CLASS());
        }
    
    // Default constructor
    public NamedCacheFactory()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public NamedCacheFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$AggregateAllRequest
    
    /**
     * InvocableMap.aggregate(Collection collKeys, EntryAggregator agent)
     * Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class AggregateAllRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeySetRequest
            implements com.tangosol.net.PriorityTask
        {
        // ---- Fields declarations ----
        
        /**
         * Property Aggregator
         *
         * The EntryAggregator.
         */
        private com.tangosol.util.InvocableMap.EntryAggregator __m_Aggregator;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 51;
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
            __mapChildren.put("Status", NamedCacheFactory.AggregateAllRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public AggregateAllRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public AggregateAllRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateAllRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$AggregateAllRequest".replace('/', '.'));
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
        
        // Accessor for the property "Aggregator"
        /**
         * Getter for property Aggregator.<p>
        * The EntryAggregator.
         */
        public com.tangosol.util.InvocableMap.EntryAggregator getAggregator()
            {
            return __m_Aggregator;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription() + ", Aggregator=" + getAggregator();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            return (aggregator instanceof PriorityTask) 
                ? ((PriorityTask) aggregator).getExecutionTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            return (aggregator instanceof PriorityTask) 
                ? ((PriorityTask) aggregator).getRequestTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            return (aggregator instanceof PriorityTask) 
                ? ((PriorityTask) aggregator).getSchedulingPriority()
                : PriorityTask.SCHEDULE_STANDARD;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(cache.aggregate(getKeySet(), getAggregator()));
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            super.readExternal(in);
            
            setAggregator((com.tangosol.util.InvocableMap.EntryAggregator) in.readObject(2));
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            if (aggregator instanceof PriorityTask)
                {
                ((PriorityTask) aggregator).runCanceled(fAbandoned);
                }
            }
        
        // Accessor for the property "Aggregator"
        /**
         * Setter for property Aggregator.<p>
        * The EntryAggregator.
         */
        public void setAggregator(com.tangosol.util.InvocableMap.EntryAggregator agent)
            {
            __m_Aggregator = agent;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(2, getAggregator());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$AggregateAllRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateAllRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$AggregateAllRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$AggregateFilterRequest
    
    /**
     * InvocableMap.aggregate(Filter filter, EntryAggregator agent) Request
     * message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class AggregateFilterRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.FilterRequest
            implements com.tangosol.net.PriorityTask
        {
        // ---- Fields declarations ----
        
        /**
         * Property Aggregator
         *
         * The EntryAggregator.
         */
        private com.tangosol.util.InvocableMap.EntryAggregator __m_Aggregator;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 52;
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
            __mapChildren.put("Status", NamedCacheFactory.AggregateFilterRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public AggregateFilterRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public AggregateFilterRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateFilterRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$AggregateFilterRequest".replace('/', '.'));
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
        
        // Accessor for the property "Aggregator"
        /**
         * Getter for property Aggregator.<p>
        * The EntryAggregator.
         */
        public com.tangosol.util.InvocableMap.EntryAggregator getAggregator()
            {
            return __m_Aggregator;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription() + ", Aggregator=" + getAggregator();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            return (aggregator instanceof PriorityTask) 
                ? ((PriorityTask) aggregator).getExecutionTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            return (aggregator instanceof PriorityTask) 
                ? ((PriorityTask) aggregator).getRequestTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            return (aggregator instanceof PriorityTask) 
                ? ((PriorityTask) aggregator).getSchedulingPriority()
                : PriorityTask.SCHEDULE_STANDARD;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(cache.aggregate(getFilter(), getAggregator()));
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            super.readExternal(in);
            
            setAggregator((com.tangosol.util.InvocableMap.EntryAggregator) in.readObject(2));
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
            
            com.tangosol.util.InvocableMap.EntryAggregator aggregator = getAggregator();
            
            if (aggregator instanceof PriorityTask)
                {
                ((PriorityTask) aggregator).runCanceled(fAbandoned);
                }
            }
        
        // Accessor for the property "Aggregator"
        /**
         * Setter for property Aggregator.<p>
        * The EntryAggregator.
         */
        public void setAggregator(com.tangosol.util.InvocableMap.EntryAggregator agent)
            {
            __m_Aggregator = agent;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(2, getAggregator());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$AggregateFilterRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateFilterRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$AggregateFilterRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ClearRequest
    
    /**
     * Map.clear() Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ClearRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property Truncate
         *
         */
        private boolean __m_Truncate;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 8;
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
            __mapChildren.put("Status", NamedCacheFactory.ClearRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public ClearRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ClearRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ClearRequest".replace('/', '.'));
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
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Accessor for the property "Truncate"
        /**
         * Getter for property Truncate.<p>
         */
        public boolean isTruncate()
            {
            return __m_Truncate;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            if (isTruncate())
                {
                cache.truncate();
                }
            else
                {
                cache.clear();
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            int nVersion = getImplVersion();
            
            if (nVersion > 5)
                {
                setTruncate(in.readBoolean(1));
                }
            }
        
        // Accessor for the property "Truncate"
        /**
         * Setter for property Truncate.<p>
         */
        public void setTruncate(boolean fTruncate)
            {
            __m_Truncate = fTruncate;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            int nVersion = getImplVersion();
            
            if (nVersion > 5)
                {
                out.writeBoolean(1, isTruncate());
                }
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ClearRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ClearRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ContainsAllRequest
    
    /**
     * Map.keySet().containsAll(Collection colKeys) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ContainsAllRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeySetRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 9;
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
            __mapChildren.put("Status", NamedCacheFactory.ContainsAllRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public ContainsAllRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ContainsAllRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsAllRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ContainsAllRequest".replace('/', '.'));
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(Boolean.valueOf(cache.keySet().containsAll(getKeySet())));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ContainsAllRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsAllRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ContainsAllRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ContainsKeyRequest
    
    /**
     * Map.containsKey(Object oKey) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ContainsKeyRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 2;
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
            __mapChildren.put("Status", NamedCacheFactory.ContainsKeyRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public ContainsKeyRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ContainsKeyRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsKeyRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ContainsKeyRequest".replace('/', '.'));
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(Boolean.valueOf(cache.containsKey(getKey())));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ContainsKeyRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsKeyRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ContainsKeyRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ContainsValueRequest
    
    /**
     * Map.containsValue(Object oValue) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ContainsValueRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 3;
        
        /**
         * Property Value
         *
         * The entry value.
         */
        private Object __m_Value;
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
            __mapChildren.put("Status", NamedCacheFactory.ContainsValueRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public ContainsValueRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ContainsValueRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsValueRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ContainsValueRequest".replace('/', '.'));
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
            Object oValue = getValue();
            
            return super.getDescription() + ", Value=" + (oValue == null ? "null" : oValue.getClass().getSimpleName() + "(HashCode=" + oValue.hashCode() + ')');
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Accessor for the property "Value"
        /**
         * Getter for property Value.<p>
        * The entry value.
         */
        public Object getValue()
            {
            return __m_Value;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(Boolean.valueOf(cache.containsValue(getValue())));
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            setValue(in.readObject(1));
            }
        
        // Accessor for the property "Value"
        /**
         * Setter for property Value.<p>
        * The entry value.
         */
        public void setValue(Object oValue)
            {
            __m_Value = oValue;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(1, getValue());
            
            // release state
            setValue(null);
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ContainsValueRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsValueRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ContainsValueRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$GetAllRequest
    
    /**
     * CacheMap.getAll(Collection colKeys) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class GetAllRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeySetRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 21;
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
            __mapChildren.put("Status", NamedCacheFactory.GetAllRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public GetAllRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public GetAllRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetAllRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$GetAllRequest".replace('/', '.'));
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(cache.getAll(getKeySet()));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$GetAllRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetAllRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$GetAllRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$GetRequest
    
    /**
     * Map.get(Object oKey) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class GetRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 4;
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
            __mapChildren.put("Status", NamedCacheFactory.GetRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public GetRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public GetRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$GetRequest".replace('/', '.'));
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(cache.get(getKey()));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$GetRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$GetRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$IndexRequest
    
    /**
     * QueryMap.addIndex(ValueExtractor extractor, boolean fOrdered, Comparator
     * comparator) and removeIndex(ValueExtractor extractor) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class IndexRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property Add
         *
         * If true, add an index; otherwise remove it.
         */
        private boolean __m_Add;
        
        /**
         * Property Comparator
         *
         * The Comparator object which imposes an ordering on entries in the
         * indexed map; or null if the entries' values natural ordering should
         * be used.
         */
        private java.util.Comparator __m_Comparator;
        
        /**
         * Property Extractor
         *
         * The ValueExtractor object that is used to extract an indexable
         * Object from a value stored in the indexed Map.
         */
        private com.tangosol.util.ValueExtractor __m_Extractor;
        
        /**
         * Property Ordered
         *
         * If true, the contents of the indexed information should be ordered.
         */
        private boolean __m_Ordered;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 42;
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
            __mapChildren.put("Status", NamedCacheFactory.IndexRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public IndexRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public IndexRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$IndexRequest".replace('/', '.'));
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
        
        // Accessor for the property "Comparator"
        /**
         * Getter for property Comparator.<p>
        * The Comparator object which imposes an ordering on entries in the
        * indexed map; or null if the entries' values natural ordering should
        * be used.
         */
        public java.util.Comparator getComparator()
            {
            return __m_Comparator;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription()
                    + ", Add="        + isAdd()
                    + ", Comparator=" + getComparator()
                    + ", Extractor="  + getExtractor()
                    + ", Ordered="    + isOrdered();
            }
        
        // Accessor for the property "Extractor"
        /**
         * Getter for property Extractor.<p>
        * The ValueExtractor object that is used to extract an indexable Object
        * from a value stored in the indexed Map.
         */
        public com.tangosol.util.ValueExtractor getExtractor()
            {
            return __m_Extractor;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Accessor for the property "Add"
        /**
         * Getter for property Add.<p>
        * If true, add an index; otherwise remove it.
         */
        public boolean isAdd()
            {
            return __m_Add;
            }
        
        // Accessor for the property "Ordered"
        /**
         * Getter for property Ordered.<p>
        * If true, the contents of the indexed information should be ordered.
         */
        public boolean isOrdered()
            {
            return __m_Ordered;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            if (isAdd())
                {
                cache.addIndex(getExtractor(), isOrdered(), getComparator());
                }
            else
                {
                cache.removeIndex(getExtractor());
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ValueExtractor;
            // import java.util.Comparator;
            
            super.readExternal(in);
            
            setAdd(in.readBoolean(1));
            setExtractor((ValueExtractor) in.readObject(2));
            setOrdered(in.readBoolean(3));
            setComparator((Comparator) in.readObject(4));
            }
        
        // Accessor for the property "Add"
        /**
         * Setter for property Add.<p>
        * If true, add an index; otherwise remove it.
         */
        public void setAdd(boolean fAdd)
            {
            __m_Add = fAdd;
            }
        
        // Accessor for the property "Comparator"
        /**
         * Setter for property Comparator.<p>
        * The Comparator object which imposes an ordering on entries in the
        * indexed map; or null if the entries' values natural ordering should
        * be used.
         */
        public void setComparator(java.util.Comparator comparator)
            {
            __m_Comparator = comparator;
            }
        
        // Accessor for the property "Extractor"
        /**
         * Setter for property Extractor.<p>
        * The ValueExtractor object that is used to extract an indexable Object
        * from a value stored in the indexed Map.
         */
        public void setExtractor(com.tangosol.util.ValueExtractor extractor)
            {
            __m_Extractor = extractor;
            }
        
        // Accessor for the property "Ordered"
        /**
         * Setter for property Ordered.<p>
        * If true, the contents of the indexed information should be ordered.
         */
        public void setOrdered(boolean fOrdered)
            {
            __m_Ordered = fOrdered;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeBoolean(1, isAdd());
            out.writeObject(2, getExtractor());
            out.writeBoolean(3, isOrdered());
            out.writeObject(4, getComparator());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$IndexRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$IndexRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$InvokeAllRequest
    
    /**
     * InvocableMap.invokeAll(Collection collKeys, EntryProcessor agent)
     * Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvokeAllRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeySetRequest
            implements com.tangosol.net.PriorityTask
        {
        // ---- Fields declarations ----
        
        /**
         * Property Processor
         *
         * The EntryProcessor.
         */
        private com.tangosol.util.InvocableMap.EntryProcessor __m_Processor;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 54;
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
            __mapChildren.put("Status", NamedCacheFactory.InvokeAllRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public InvokeAllRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvokeAllRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeAllRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$InvokeAllRequest".replace('/', '.'));
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
            return super.getDescription() + ", Processor=" + getProcessor();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getExecutionTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // Accessor for the property "Processor"
        /**
         * Getter for property Processor.<p>
        * The EntryProcessor.
         */
        public com.tangosol.util.InvocableMap.EntryProcessor getProcessor()
            {
            return __m_Processor;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getRequestTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getSchedulingPriority()
                : PriorityTask.SCHEDULE_STANDARD;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(cache.invokeAll(getKeySet(), getProcessor()));
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            super.readExternal(in);
            
            setProcessor((com.tangosol.util.InvocableMap.EntryProcessor) in.readObject(2));
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            if (processor instanceof PriorityTask)
                {
                ((PriorityTask) processor).runCanceled(fAbandoned);
                }
            }
        
        // Accessor for the property "Processor"
        /**
         * Setter for property Processor.<p>
        * The EntryProcessor.
         */
        public void setProcessor(com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            __m_Processor = agent;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(2, getProcessor());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$InvokeAllRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeAllRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$InvokeAllRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$InvokeFilterRequest
    
    /**
     * InvocableMap.invokeAll(Filter filter, EntryProcessor agent) Request
     * message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvokeFilterRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.FilterRequest
            implements com.tangosol.net.PriorityTask
        {
        // ---- Fields declarations ----
        
        /**
         * Property Cookie
         *
         * Opaque cookie used to support streaming.
         * 
         * See PartialResponse#Cookie.
         */
        private com.tangosol.util.Binary __m_Cookie;
        
        /**
         * Property Processor
         *
         * The EntryProcessor.
         */
        private com.tangosol.util.InvocableMap.EntryProcessor __m_Processor;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 55;
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
            __mapChildren.put("Status", NamedCacheFactory.InvokeFilterRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public InvokeFilterRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvokeFilterRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeFilterRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$InvokeFilterRequest".replace('/', '.'));
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
        
        // Accessor for the property "Cookie"
        /**
         * Getter for property Cookie.<p>
        * Opaque cookie used to support streaming.
        * 
        * See PartialResponse#Cookie.
         */
        public com.tangosol.util.Binary getCookie()
            {
            return __m_Cookie;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription()
                    + ", Processor=" + getProcessor()
                    + ", Cookie="    + getCookie();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getExecutionTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // Accessor for the property "Processor"
        /**
         * Getter for property Processor.<p>
        * The EntryProcessor.
         */
        public com.tangosol.util.InvocableMap.EntryProcessor getProcessor()
            {
            return __m_Processor;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getRequestTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getSchedulingPriority()
                : PriorityTask.SCHEDULE_STANDARD;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Declared at the super level
        /**
         * Factory method: create a new Response instance.
        * 
        * @param  the MessageFactory used to create the new Response object
        * 
        * @return a new Response object
         */
        protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
            {
            // import Component.Net.Extend.Message.Response as com.tangosol.coherence.component.net.extend.message.Response;
            
            return (com.tangosol.coherence.component.net.extend.message.Response) factory.createMessage(NamedCacheFactory.PartialResponse.TYPE_ID);
            }
        
        /**
         * Helper method that runs the specified agent against the given
        * NamedCache.
        * 
        * @param cache  the NamedCache to process
        * @param filter  the Filter representing the keys to process
        * @param agent  the EntryProcessor to run
        * @param partsRemain  the remaining partitions that have yet to be
        * processed
        * @param cBatch  the maximum number of partitions to process
        * @param mapResult an optional Map containing the previous results; the
        * returned Map will contain both the previous results as well as the
        * results of the invocation
        * 
        * @return a Map containing the invocation results and any previous
        * results
         */
        protected static java.util.Map invoke(com.tangosol.net.NamedCache cache, com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent, com.tangosol.net.partition.PartitionSet partsRemain, int cBatch, java.util.Map mapResult)
            {
            // import com.tangosol.io.pof.PofHelper$WriteableEntrySetMap as com.tangosol.io.pof.PofHelper.WriteableEntrySetMap;
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.Base;
            // import com.tangosol.util.ImmutableArrayList;
            // import com.tangosol.util.filter.PartitionedFilter;
            // import java.util.Map;
            
            int cPart = partsRemain.getPartitionCount();
            
            // calculate the next batch of partitions
            PartitionSet partsProcess = new PartitionSet(cPart);
            for (int nPart = Base.getRandom().nextInt(cPart);
                 --cBatch >=0 && (nPart = partsRemain.removeNext(nPart)) >= 0; )
                {
                partsProcess.add(nPart);
                }
            
            // limit the invocation to the next batch of partitions
            filter = new PartitionedFilter(filter, partsProcess);
            
            // perform the invocation
            Map map = cache.invokeAll(filter, agent);
            if (mapResult == null || mapResult.isEmpty())
                {
                mapResult = map;
                }
            else if (map.isEmpty())
                {
                // nothing to do
                }
            else
                {
                Object[] aoOld = mapResult.entrySet().toArray();
                Object[] aoNew = map.entrySet().toArray();
                int      cOld  = aoOld.length;
                int      cNew  = aoNew.length;
                int      cAll  = cOld + cNew;
                Object[] aoAll = new Object[cAll];
            
                System.arraycopy(aoOld, 0, aoAll, 0, cOld);
                System.arraycopy(aoNew, 0, aoAll, cOld, cNew);
            
                mapResult = new com.tangosol.io.pof.PofHelper.WriteableEntrySetMap(new ImmutableArrayList(aoAll));
                }
            
            return mapResult;
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
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            // import com.tangosol.util.filter.AlwaysFilter;
            // import com.tangosol.util.filter.KeyAssociatedFilter;
            // import com.tangosol.util.filter.LimitFilter;
            // import com.tangosol.util.filter.PartitionedFilter;
            // import java.util.Map;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            Filter filter = getFilter();
            if (filter == null)
                {
                // see PartitionedFilter constructor
                filter = AlwaysFilter.INSTANCE;
                }
            else if (filter instanceof LimitFilter         ||
                     filter instanceof KeyAssociatedFilter ||
                     filter instanceof PartitionedFilter)
                {
                response.setResult(cache.invokeAll(filter, getProcessor()));
                return;
                }
            
            com.tangosol.util.InvocableMap.EntryProcessor agent    = getProcessor();
            Object[]       aoCookie = decodeCookie(getCookie());
            PartitionSet   parts    = (PartitionSet) aoCookie[0];
            int            cBatch   = ((Integer) aoCookie[1]).intValue();
            int            cPart    = parts.getPartitionCount();
            
            Map map;
            if (cBatch == 0)
                {
                // process an initial partition
                map = invoke(cache, filter, agent, parts, 1, null);
            
                // calculate the size of the first partition's worth of results
                int cb = calculateBinarySize(map == null ? null : map.entrySet(), true);
            
                // calculate the batch size
                cBatch = calculateBatchSize(cPart, cb);
            
                // process the remainder of the batch
                if (cBatch > 1 && cPart > 1)
                    {
                    map = invoke(cache, filter, agent, parts, cBatch - 1, map);
                    }
                }
            else
                {
                map = invoke(cache, filter, agent, parts, cBatch, null);
                }
            
            response.setResult(map);
            ((NamedCacheFactory.PartialResponse) response).setCookie(encodeCookie(parts, cBatch));
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            super.readExternal(in);
            
            setProcessor((com.tangosol.util.InvocableMap.EntryProcessor) in.readObject(2));
            setCookie(in.readBinary(3));
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            if (processor instanceof PriorityTask)
                {
                ((PriorityTask) processor).runCanceled(fAbandoned);
                }
            }
        
        // Accessor for the property "Cookie"
        /**
         * Setter for property Cookie.<p>
        * Opaque cookie used to support streaming.
        * 
        * See PartialResponse#Cookie.
         */
        public void setCookie(com.tangosol.util.Binary bin)
            {
            __m_Cookie = bin;
            }
        
        // Accessor for the property "Processor"
        /**
         * Setter for property Processor.<p>
        * The EntryProcessor.
         */
        public void setProcessor(com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            __m_Processor = agent;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(2, getProcessor());
            out.writeBinary(3, getCookie());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$InvokeFilterRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeFilterRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$InvokeFilterRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$InvokeRequest
    
    /**
     * InvocableMap.invoke(Object oKey, EntryProcessor agent) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvokeRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
            implements com.tangosol.net.PriorityTask
        {
        // ---- Fields declarations ----
        
        /**
         * Property Processor
         *
         * The EntryProcessor.
         */
        private com.tangosol.util.InvocableMap.EntryProcessor __m_Processor;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 53;
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
            __mapChildren.put("Status", NamedCacheFactory.InvokeRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public InvokeRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvokeRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$InvokeRequest".replace('/', '.'));
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
            return super.getDescription() + ", Processor=" + getProcessor();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getExecutionTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // Accessor for the property "Processor"
        /**
         * Getter for property Processor.<p>
        * The EntryProcessor.
         */
        public com.tangosol.util.InvocableMap.EntryProcessor getProcessor()
            {
            return __m_Processor;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getRequestTimeoutMillis()
                : PriorityTask.TIMEOUT_DEFAULT;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            return (processor instanceof PriorityTask) 
                ? ((PriorityTask) processor).getSchedulingPriority()
                : PriorityTask.SCHEDULE_STANDARD;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(cache.invoke(getKey(), getProcessor()));
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            super.readExternal(in);
            
            setProcessor((com.tangosol.util.InvocableMap.EntryProcessor) in.readObject(2));
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
            
            com.tangosol.util.InvocableMap.EntryProcessor processor = getProcessor();
            
            if (processor instanceof PriorityTask)
                {
                ((PriorityTask) processor).runCanceled(fAbandoned);
                }
            }
        
        // Accessor for the property "Processor"
        /**
         * Setter for property Processor.<p>
        * The EntryProcessor.
         */
        public void setProcessor(com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            __m_Processor = agent;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(2, getProcessor());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$InvokeRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$InvokeRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ListenerFilterRequest
    
    /**
     * ObservableMap.addMapListener(MapListener listener, Filter filter,
     * boolean fLite) and removeMapListener(MapListener listener, Filter
     * filter) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ListenerFilterRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.FilterRequest
            implements com.tangosol.net.cache.KeyAssociation
        {
        // ---- Fields declarations ----
        
        /**
         * Property Add
         *
         * True to add a MapListener, false to remove a MapListener.
         */
        private boolean __m_Add;
        
        /**
         * Property FilterId
         *
         * A unique identifier for the Filter associated with this
         * ListenerFilterRequest.
         */
        private long __m_FilterId;
        
        /**
         * Property Lite
         *
         * True if the MapListener is "lite", false if it is a standard
         * MapListener.
         */
        private boolean __m_Lite;
        
        /**
         * Property Priming
         *
         * Support for the NearCache priming listener. The value of true
         * indicates that the listener registration should force a synthetic
         * event containing the current value to the requesting client.
         * 
         * This property was added to Coherence 12.2.1 (protocol version 6) for
         * COH-4615 implementation.
         */
        private boolean __m_Priming;
        
        /**
         * Property Trigger
         *
         * An optional MapTrigger object associated with this request.
         */
        private com.tangosol.util.MapTrigger __m_Trigger;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 12;
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
            __mapChildren.put("Status", NamedCacheFactory.ListenerFilterRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public ListenerFilterRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ListenerFilterRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ListenerFilterRequest".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.cache.KeyAssociation
        /**
         * It may seem strange that a Request to add or remove a Filter-based
        * MapListener would implement KeyAssociation, but it is necessary to
        * ensure the in-order add and removal of SynchronousListeners, with
        * respect to similar requests involving the same Filter.
        * 
        * @return the Filter identifier
         */
        public Object getAssociatedKey()
            {
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.filter.AlwaysFilter;
            
            // see COH-950
            Filter filter = getFilter();
            return filter == null ? AlwaysFilter.INSTANCE : filter;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription()
                    + ", FilterId=" + getFilterId()
                    + ", Add="      + isAdd()
                    + ", Lite="     + isLite()
                    + ", Trigger="  + getTrigger();
            }
        
        // Accessor for the property "FilterId"
        /**
         * Getter for property FilterId.<p>
        * A unique identifier for the Filter associated with this
        * ListenerFilterRequest.
         */
        public long getFilterId()
            {
            return __m_FilterId;
            }
        
        // Accessor for the property "Trigger"
        /**
         * Getter for property Trigger.<p>
        * An optional MapTrigger object associated with this request.
         */
        public com.tangosol.util.MapTrigger getTrigger()
            {
            return __m_Trigger;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Accessor for the property "Add"
        /**
         * Getter for property Add.<p>
        * True to add a MapListener, false to remove a MapListener.
         */
        public boolean isAdd()
            {
            return __m_Add;
            }
        
        // Accessor for the property "Lite"
        /**
         * Getter for property Lite.<p>
        * True if the MapListener is "lite", false if it is a standard
        * MapListener.
         */
        public boolean isLite()
            {
            return __m_Lite;
            }
        
        // Accessor for the property "Priming"
        /**
         * Getter for property Priming.<p>
        * Support for the NearCache priming listener. The value of true
        * indicates that the listener registration should force a synthetic
        * event containing the current value to the requesting client.
        * 
        * This property was added to Coherence 12.2.1 (protocol version 6) for
        * COH-4615 implementation.
         */
        public boolean isPriming()
            {
            return __m_Priming;
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
            // import Component.Net.Extend.Proxy.MapListenerProxy;
            // import Component.Net.Extend.Proxy.NamedCacheProxy;
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.util.MapListener;
            // import com.tangosol.util.MapTrigger;
            // import com.tangosol.util.MapTriggerListener;
            
            Channel channel = getChannel();
            _assert(channel != null);
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            MapTrigger trigger = getTrigger();
            if (trigger == null)
                {
                MapListenerProxy proxy = (MapListenerProxy) channel.getAttribute(
                        NamedCacheProxy.ATTR_LISTENER);
                _assert(proxy != null);
            
                if (isAdd())
                    {
                    proxy.addListener(cache, getFilter(), getFilterId(), isLite(), isPriming());
                    }
                else
                    {
                    proxy.removeListener(cache, getFilter(), isPriming());
                    }
                }
            else
                {
                MapListener listener = new MapTriggerListener(trigger);
                if (isAdd())
                    {
                    cache.addMapListener(listener, getFilter(), isLite());
                    }
                else
                    {
                    cache.removeMapListener(listener, getFilter());
                    }
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.MapTrigger;
            
            super.readExternal(in);
            
            setFilterId(in.readLong(2));
            setAdd(in.readBoolean(3));
            setLite(in.readBoolean(4));
            setTrigger((MapTrigger) in.readObject(5));
            
            if (getImplVersion() > 5)
                {
                setPriming(in.readBoolean(6));
                }
            }
        
        // Accessor for the property "Add"
        /**
         * Setter for property Add.<p>
        * True to add a MapListener, false to remove a MapListener.
         */
        public void setAdd(boolean fAdd)
            {
            __m_Add = fAdd;
            }
        
        // Accessor for the property "FilterId"
        /**
         * Setter for property FilterId.<p>
        * A unique identifier for the Filter associated with this
        * ListenerFilterRequest.
         */
        public void setFilterId(long lId)
            {
            __m_FilterId = lId;
            }
        
        // Accessor for the property "Lite"
        /**
         * Setter for property Lite.<p>
        * True if the MapListener is "lite", false if it is a standard
        * MapListener.
         */
        public void setLite(boolean fLite)
            {
            __m_Lite = fLite;
            }
        
        // Accessor for the property "Priming"
        /**
         * Setter for property Priming.<p>
        * Support for the NearCache priming listener. The value of true
        * indicates that the listener registration should force a synthetic
        * event containing the current value to the requesting client.
        * 
        * This property was added to Coherence 12.2.1 (protocol version 6) for
        * COH-4615 implementation.
         */
        public void setPriming(boolean fPriming)
            {
            __m_Priming = fPriming;
            }
        
        // Accessor for the property "Trigger"
        /**
         * Setter for property Trigger.<p>
        * An optional MapTrigger object associated with this request.
         */
        public void setTrigger(com.tangosol.util.MapTrigger trigger)
            {
            __m_Trigger = trigger;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeLong(2, getFilterId());
            out.writeBoolean(3, isAdd());
            out.writeBoolean(4, isLite());
            out.writeObject(5, getTrigger());
            
            if (getImplVersion() > 5)
                {
                out.writeBoolean(6, isPriming());
                }
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ListenerFilterRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ListenerFilterRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ListenerKeyRequest
    
    /**
     * ObservableMap.addMapListener(MapListener listener, Object oKey, boolean
     * fLite) and removeMapListener(MapListener listener, Object oKey) Request
     * message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ListenerKeyRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property Add
         *
         * True to add a MapListener, false to remove a MapListener.
         */
        private boolean __m_Add;
        
        /**
         * Property Lite
         *
         * True if the MapListener is "lite", false if it is a standard
         * MapListener.
         */
        private boolean __m_Lite;
        
        /**
         * Property Priming
         *
         * Support for the NearCache priming listener. The value of true
         * indicates that the listener registration should force a synthetic
         * event containing the current value to the requesting client.
         * 
         * This property was added to Coherence 12.2.1 (protocol version 6) for
         * COH-4615 implementation.
         */
        private boolean __m_Priming;
        
        /**
         * Property Trigger
         *
         * An optional MapTrigger object associated with this request.
         */
        private com.tangosol.util.MapTrigger __m_Trigger;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 11;
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
            __mapChildren.put("Status", NamedCacheFactory.ListenerKeyRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public ListenerKeyRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ListenerKeyRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ListenerKeyRequest".replace('/', '.'));
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
        public boolean equals(Object obj)
            {
            // import com.tangosol.util.Base;
            
            // this implementation is used to associate non-commutative requests;
            // it is intentionally agnostic about the request specifics
            
            if (obj instanceof NamedCacheFactory.ListenerKeyRequest)
                {
                NamedCacheFactory.ListenerKeyRequest that = (NamedCacheFactory.ListenerKeyRequest) obj;
            
                return this.getChannel() == that.getChannel()
                    && Base.equals(this.getKey(), that.getKey());
                }
            return false;
            }
        
        // Declared at the super level
        public Object getAssociatedKey()
            {
            // use the same approach as used by DistributedCacheKeyRequest
            
            return this;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription()
                    + ", Add="     + isAdd()
                    + ", Lite="    + isLite()
                    + ", Trigger=" + getTrigger()
                    + ", Priming=" + isPriming();
            }
        
        // Accessor for the property "Trigger"
        /**
         * Getter for property Trigger.<p>
        * An optional MapTrigger object associated with this request.
         */
        public com.tangosol.util.MapTrigger getTrigger()
            {
            return __m_Trigger;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Declared at the super level
        public int hashCode()
            {
            // this implementation is used to associate non-commutative requests;
            // it is intentionally agnostic about the request specifics
            
            Object oKey = getKey();
            return oKey == null ? 0 : oKey.hashCode();
            }
        
        // Accessor for the property "Add"
        /**
         * Getter for property Add.<p>
        * True to add a MapListener, false to remove a MapListener.
         */
        public boolean isAdd()
            {
            return __m_Add;
            }
        
        // Accessor for the property "Lite"
        /**
         * Getter for property Lite.<p>
        * True if the MapListener is "lite", false if it is a standard
        * MapListener.
         */
        public boolean isLite()
            {
            return __m_Lite;
            }
        
        // Accessor for the property "Priming"
        /**
         * Getter for property Priming.<p>
        * Support for the NearCache priming listener. The value of true
        * indicates that the listener registration should force a synthetic
        * event containing the current value to the requesting client.
        * 
        * This property was added to Coherence 12.2.1 (protocol version 6) for
        * COH-4615 implementation.
         */
        public boolean isPriming()
            {
            return __m_Priming;
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
            // import Component.Net.Extend.Proxy.MapListenerProxy;
            // import Component.Net.Extend.Proxy.NamedCacheProxy;
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.util.MapListener;
            // import com.tangosol.util.MapTrigger;
            // import com.tangosol.util.MapTriggerListener;
            
            Channel channel = getChannel();
            _assert(channel != null);
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            MapTrigger trigger = getTrigger();
            if (trigger == null)
                {
                MapListenerProxy proxy = (MapListenerProxy) channel.getAttribute(
                        NamedCacheProxy.ATTR_LISTENER);
                _assert(proxy != null);
            
                if (isAdd())
                    {
                    proxy.addListener(cache, getKey(), isLite(), isPriming());
                    }
                else
                    {
                    proxy.removeListener(cache, getKey(), isPriming());
                    }
                }
            else
                {
                MapListener listener = new MapTriggerListener(trigger);
                if (isAdd())
                    {
                    cache.addMapListener(listener, getKey(), isLite());
                    }
                else
                    {
                    cache.removeMapListener(listener, getKey());
                    }
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.MapTrigger;
            
            super.readExternal(in);
            
            setAdd(in.readBoolean(2));
            setLite(in.readBoolean(3));
            setTrigger((MapTrigger) in.readObject(4));
            
            if (getImplVersion() > 5)
                {
                setPriming(in.readBoolean(5));
                }
            }
        
        // Accessor for the property "Add"
        /**
         * Setter for property Add.<p>
        * True to add a MapListener, false to remove a MapListener.
         */
        public void setAdd(boolean fAdd)
            {
            __m_Add = fAdd;
            }
        
        // Accessor for the property "Lite"
        /**
         * Setter for property Lite.<p>
        * True if the MapListener is "lite", false if it is a standard
        * MapListener.
         */
        public void setLite(boolean fLite)
            {
            __m_Lite = fLite;
            }
        
        // Accessor for the property "Priming"
        /**
         * Setter for property Priming.<p>
        * Support for the NearCache priming listener. The value of true
        * indicates that the listener registration should force a synthetic
        * event containing the current value to the requesting client.
        * 
        * This property was added to Coherence 12.2.1 (protocol version 6) for
        * COH-4615 implementation.
         */
        public void setPriming(boolean fPriming)
            {
            __m_Priming = fPriming;
            }
        
        // Accessor for the property "Trigger"
        /**
         * Setter for property Trigger.<p>
        * An optional MapTrigger object associated with this request.
         */
        public void setTrigger(com.tangosol.util.MapTrigger trigger)
            {
            __m_Trigger = trigger;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeBoolean(2, isAdd());
            out.writeBoolean(3, isLite());
            out.writeObject(4, getTrigger());
            
            if (getImplVersion() > 5)
                {
                out.writeBoolean(5, isPriming());
                }
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ListenerKeyRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ListenerKeyRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$LockRequest
    
    /**
     * NamedCache.lock(Object oKey, long cWait) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LockRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TimeoutMillis
         *
         * The number of milliseconds to wait to obtain the lock; 0 to return
         * immediately; -1 to wait indefinitely.
         */
        private long __m_TimeoutMillis;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 31;
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
            __mapChildren.put("Status", NamedCacheFactory.LockRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public LockRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LockRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.LockRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$LockRequest".replace('/', '.'));
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
            return super.getDescription() + ", TimeoutMillis=" + getTimeoutMillis();
            }
        
        // Accessor for the property "TimeoutMillis"
        /**
         * Getter for property TimeoutMillis.<p>
        * The number of milliseconds to wait to obtain the lock; 0 to return
        * immediately; -1 to wait indefinitely.
         */
        public long getTimeoutMillis()
            {
            return __m_TimeoutMillis;
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
            // import Component.Net.Extend.Proxy.NamedCacheProxy;
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.util.ConcurrentMap;
            
            Channel       channel = getChannel();
            Object        oKey    = getKey();
            long          cMillis = getTimeoutMillis();
            NamedCache    cache   = getNamedCache();
            ConcurrentMap map     = (ConcurrentMap) channel.getAttribute(
                    NamedCacheProxy.ATTR_LOCK_MAP);
            
            _assert(cache != null);
            _assert(map != null);
            
            boolean fLocked;
            if (cMillis == 0L)
                {
                if (fLocked = map.lock(oKey, 0L))
                    {
                    try
                        {
                        // check status
                        if (map.containsKey(oKey))
                            {
                            // key was already locked
                            fLocked = true;
                            }
                        else
                            {
                            // key is not locked; attempt to lock it
                            if (fLocked = cache.lock(oKey, 0L))
                                {
                                map.put(oKey, oKey);
                                }
                            }
                        }
                    finally
                        {
                        map.unlock(oKey);
                        }
                    }
                }
            else
                {
                if (cMillis < 0L)
                    {
                    cMillis = Long.MAX_VALUE;
                    }
            
                long ldtStart = System.currentTimeMillis();
                if (fLocked = map.lock(oKey, cMillis))
                    {
                    try
                        {
                        // check status
                        if (map.containsKey(oKey))
                            {
                            // key was already locked
                            fLocked = true;
                            }
                        else
                            {
                            // key is not locked; attempt to lock it
                            cMillis -= Math.max(System.currentTimeMillis() - ldtStart, 0L);
                            if (fLocked = cache.lock(oKey, cMillis))
                                {
                                map.put(oKey, oKey);
                                }
                            }
                        }
                    finally
                        {
                        map.unlock(oKey);
                        }
                    }
                }
            
            response.setResult(Boolean.valueOf(fLocked));
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            setTimeoutMillis(in.readLong(2));
            }
        
        // Accessor for the property "TimeoutMillis"
        /**
         * Setter for property TimeoutMillis.<p>
        * The number of milliseconds to wait to obtain the lock; 0 to return
        * immediately; -1 to wait indefinitely.
         */
        public void setTimeoutMillis(long cMillis)
            {
            __m_TimeoutMillis = cMillis;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeLong(2, getTimeoutMillis());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$LockRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.LockRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$LockRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$MapEvent
    
    /**
     * Message that encapsulates the information in a MapEvent. This Message is
     * sent by a MapListener registered on the remote NamedCache by the peer
     * NamedCacheProxy.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MapEvent
            extends    com.tangosol.coherence.component.net.extend.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Expired
         *
         * True if the MapEvent was caused by a time-based eviction.
         */
        private boolean __m_Expired;
        
        /**
         * Property FilterId
         *
         * If positive, identifies the Filter that caused the MapEvent to be
         * raised.
         * 
         * @deprecated Since NamedCacheProtocol version 4, replaced by FilterIds
         */
        private long __m_FilterId;
        
        /**
         * Property FilterIds
         *
         * If not null, identifies the Filter(s) that caused the FilterEvent to
         * be raised.
         * 
         * @since NamedCacheProtocol version 4
         */
        private long[] __m_FilterIds;
        
        /**
         * Property Id
         *
         * The MapEvent identifier, one of MapEvent.ENTRY_INSERTED,
         * ENTRY_UPDATED, ENTRY_DELETED.
         * 
         * @see MapEvent#getId
         */
        private int __m_Id;
        
        /**
         * Property Key
         *
         * The key associated with the MapEvent.
         * 
         * @see MapEvent#getKey
         */
        private Object __m_Key;
        
        /**
         * Property Priming
         *
         * True if the MapEvent was caused by a priming event (NearCache).
         */
        private boolean __m_Priming;
        
        /**
         * Property Synthetic
         *
         * True if the MapEvent was caused by the cache internal processing
         * such as eviction or loading.
         */
        private boolean __m_Synthetic;
        
        /**
         * Property TransformationState
         *
         * The TransformationState value of the event.
         * See the CacheEvent$TransformationState enum.
         */
        private int __m_TransformationState;
        
        /**
         * Property Truncate
         *
         * True if the MapEvent was caused by the cache truncate operation.
         */
        private boolean __m_Truncate;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 13;
        
        /**
         * Property ValueNew
         *
         * The new value (for insert and update events).
         * 
         * @see MapEvent#getNewValue
         */
        private Object __m_ValueNew;
        
        /**
         * Property ValueOld
         *
         * The old value (for update and delete events).
         * 
         * @see MapEvent#getOldValue
         */
        private Object __m_ValueOld;
        
        // Default constructor
        public MapEvent()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MapEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$MapEvent".replace('/', '.'));
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
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
            // import java.util.Arrays;
            
            Object oOldValue = getValueOld();
            Object oNewValue = getValueNew();
            
            return super.getDescription()
                    + ", Action="    + com.tangosol.util.MapEvent.getDescription(getId())
                    + ", FilterId="  + getFilterId()
                    + ", FilterIds=" + Arrays.toString(getFilterIds())
                    + ", Key="       + getKey()
                    + ", OldValue="  + (oOldValue == null ? "null" : oOldValue.getClass().getSimpleName() + "(HashCode=" + oOldValue.hashCode() + ')')
                    + ", NewValue="  + (oNewValue == null ? "null" : oNewValue.getClass().getSimpleName() + "(HashCode=" + oNewValue.hashCode() + ')')
                    + ", Synthetic=" + isSynthetic()
                    + ", Priming="   + isPriming();
            }
        
        // Accessor for the property "FilterId"
        /**
         * Getter for property FilterId.<p>
        * If positive, identifies the Filter that caused the MapEvent to be
        * raised.
        * 
        * @deprecated Since NamedCacheProtocol version 4, replaced by FilterIds
         */
        public long getFilterId()
            {
            return __m_FilterId;
            }
        
        // Accessor for the property "FilterIds"
        /**
         * Getter for property FilterIds.<p>
        * If not null, identifies the Filter(s) that caused the FilterEvent to
        * be raised.
        * 
        * @since NamedCacheProtocol version 4
         */
        public long[] getFilterIds()
            {
            return __m_FilterIds;
            }
        
        // Accessor for the property "Id"
        /**
         * Getter for property Id.<p>
        * The MapEvent identifier, one of MapEvent.ENTRY_INSERTED,
        * ENTRY_UPDATED, ENTRY_DELETED.
        * 
        * @see MapEvent#getId
         */
        public int getId()
            {
            return __m_Id;
            }
        
        // Accessor for the property "Key"
        /**
         * Getter for property Key.<p>
        * The key associated with the MapEvent.
        * 
        * @see MapEvent#getKey
         */
        public Object getKey()
            {
            return __m_Key;
            }
        
        // Accessor for the property "TransformationState"
        /**
         * Getter for property TransformationState.<p>
        * The TransformationState value of the event.
        * See the CacheEvent$TransformationState enum.
         */
        public int getTransformationState()
            {
            return __m_TransformationState;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Accessor for the property "ValueNew"
        /**
         * Getter for property ValueNew.<p>
        * The new value (for insert and update events).
        * 
        * @see MapEvent#getNewValue
         */
        public Object getValueNew()
            {
            return __m_ValueNew;
            }
        
        // Accessor for the property "ValueOld"
        /**
         * Getter for property ValueOld.<p>
        * The old value (for update and delete events).
        * 
        * @see MapEvent#getOldValue
         */
        public Object getValueOld()
            {
            return __m_ValueOld;
            }
        
        // Declared at the super level
        public boolean isExecuteInOrder()
            {
            return true;
            }
        
        // Accessor for the property "Expired"
        /**
         * Getter for property Expired.<p>
        * True if the MapEvent was caused by a time-based eviction.
         */
        public boolean isExpired()
            {
            return __m_Expired;
            }
        
        // Accessor for the property "Priming"
        /**
         * Getter for property Priming.<p>
        * True if the MapEvent was caused by a priming event (NearCache).
         */
        public boolean isPriming()
            {
            return __m_Priming;
            }
        
        // Accessor for the property "Synthetic"
        /**
         * Getter for property Synthetic.<p>
        * True if the MapEvent was caused by the cache internal processing such
        * as eviction or loading.
         */
        public boolean isSynthetic()
            {
            return __m_Synthetic;
            }
        
        // Accessor for the property "Truncate"
        /**
         * Getter for property Truncate.<p>
        * True if the MapEvent was caused by the cache truncate operation.
         */
        public boolean isTruncate()
            {
            return __m_Truncate;
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            int nImplVersion = getImplVersion();
            
            setId(in.readInt(0));
            // COH-8238
            if (nImplVersion > 3)
                {
                setFilterIds(in.readLongArray(1));
                }
            else
                {
                setFilterId(in.readLong(1));
                }
            setKey(in.readObject(2));
            setValueNew(in.readObject(3));
            setValueOld(in.readObject(4));
            setSynthetic(in.readBoolean(5));
            
            // COH-9355
            if (nImplVersion > 4)
                {
                setTransformationState(in.readInt(6));
                }
            
            // COH-10216
            if (nImplVersion > 5)
                {
                setTruncate(in.readBoolean(7));
                }
            
            // COH-18376
            if (nImplVersion > 6)
                {
                setPriming(in.readBoolean(8));
                }
            
            // COH-24927
            if (nImplVersion > 8)
                {
                setExpired(in.readBoolean(9));
                }
            }
        
        // Declared at the super level
        public void run()
            {
            // import Component.Net.Extend.RemoteNamedCache;
            // import Component.Util.CacheEvent as com.tangosol.coherence.component.util.CacheEvent;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.util.Listeners;
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
            
            Channel channel = getChannel();
            _assert(channel != null);
            
            RemoteNamedCache cache = (RemoteNamedCache) channel.getReceiver();
            _assert(cache != null);
            
            if (isTruncate())
                {
                Listeners listeners = cache.getDeactivationListeners();
                if (!listeners.isEmpty())
                    {
                    com.tangosol.util.MapEvent evtTruncated = new com.tangosol.util.MapEvent(cache, com.tangosol.util.MapEvent.ENTRY_UPDATED, null, null, null);
                    // dispatch the event to the listeners, which are all synchronous (hence the null Queue)
                    com.tangosol.coherence.component.util.CacheEvent.dispatchSafe(evtTruncated, listeners, null /*Queue*/);    
                    }
                }
            else
                {
                cache.getBinaryCache().dispatch(getId(), getFilterIds(), getKey(),
                        getValueOld(), getValueNew(), isSynthetic(), getTransformationState(), isPriming(), isExpired());
                }
            }
        
        // Accessor for the property "Expired"
        /**
         * Setter for property Expired.<p>
        * True if the MapEvent was caused by a time-based eviction.
         */
        public void setExpired(boolean fExpired)
            {
            __m_Expired = fExpired;
            }
        
        // Accessor for the property "FilterId"
        /**
         * Setter for property FilterId.<p>
        * If positive, identifies the Filter that caused the MapEvent to be
        * raised.
        * 
        * @deprecated Since NamedCacheProtocol version 4, replaced by FilterIds
         */
        public void setFilterId(long lFilterId)
            {
            __m_FilterId = lFilterId;
            }
        
        // Accessor for the property "FilterIds"
        /**
         * Setter for property FilterIds.<p>
        * If not null, identifies the Filter(s) that caused the FilterEvent to
        * be raised.
        * 
        * @since NamedCacheProtocol version 4
         */
        public void setFilterIds(long[] alFilterIds)
            {
            __m_FilterIds = alFilterIds;
            }
        
        // Accessor for the property "Id"
        /**
         * Setter for property Id.<p>
        * The MapEvent identifier, one of MapEvent.ENTRY_INSERTED,
        * ENTRY_UPDATED, ENTRY_DELETED.
        * 
        * @see MapEvent#getId
         */
        public void setId(int nId)
            {
            __m_Id = nId;
            }
        
        // Accessor for the property "Key"
        /**
         * Setter for property Key.<p>
        * The key associated with the MapEvent.
        * 
        * @see MapEvent#getKey
         */
        public void setKey(Object oKey)
            {
            __m_Key = oKey;
            }
        
        // Accessor for the property "Priming"
        /**
         * Setter for property Priming.<p>
        * True if the MapEvent was caused by a priming event (NearCache).
         */
        public void setPriming(boolean fPriming)
            {
            __m_Priming = fPriming;
            }
        
        // Accessor for the property "Synthetic"
        /**
         * Setter for property Synthetic.<p>
        * True if the MapEvent was caused by the cache internal processing such
        * as eviction or loading.
         */
        public void setSynthetic(boolean fSynthetic)
            {
            __m_Synthetic = fSynthetic;
            }
        
        // Accessor for the property "TransformationState"
        /**
         * Setter for property TransformationState.<p>
        * The TransformationState value of the event.
        * See the CacheEvent$TransformationState enum.
         */
        public void setTransformationState(int nState)
            {
            __m_TransformationState = nState;
            }
        
        // Accessor for the property "Truncate"
        /**
         * Setter for property Truncate.<p>
        * True if the MapEvent was caused by the cache truncate operation.
         */
        public void setTruncate(boolean fTruncate)
            {
            __m_Truncate = fTruncate;
            }
        
        // Accessor for the property "ValueNew"
        /**
         * Setter for property ValueNew.<p>
        * The new value (for insert and update events).
        * 
        * @see MapEvent#getNewValue
         */
        public void setValueNew(Object oValue)
            {
            __m_ValueNew = oValue;
            }
        
        // Accessor for the property "ValueOld"
        /**
         * Setter for property ValueOld.<p>
        * The old value (for update and delete events).
        * 
        * @see MapEvent#getOldValue
         */
        public void setValueOld(Object oValue)
            {
            __m_ValueOld = oValue;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            int nImplVersion = getImplVersion();
            
            out.writeInt(0, getId());
            // COH-8238
            if (nImplVersion > 3)
                {
                out.writeLongArray(1, getFilterIds());
                }
            else
                {
                out.writeLong(1, getFilterId());
                }
            out.writeObject(2, getKey());
            out.writeObject(3, getValueNew());
            out.writeObject(4, getValueOld());
            out.writeBoolean(5, isSynthetic());
            
            // COH-9355
            if (nImplVersion > 4)
                {
                out.writeInt(6, getTransformationState());
                }
            
            // COH-10216
            if (nImplVersion > 5)
                {
                out.writeBoolean(7, isTruncate());
                }
            
            // COH-18376
            if (nImplVersion > 6)
                {
                out.writeBoolean(8, isPriming());
                }
            
            // COH-24927
            if (nImplVersion > 8)
                {
                out.writeBoolean(9, isExpired());
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$NoStorageMembers
    
    /**
     * Message indicating no storage members are available to service a request.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NoStorageMembers
            extends    com.tangosol.coherence.component.net.extend.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 56;
        
        // Default constructor
        public NoStorageMembers()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NoStorageMembers(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$NoStorageMembers".replace('/', '.'));
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
            // import Component.Net.Extend.RemoteNamedCache;
            // import Component.Util.CacheEvent as com.tangosol.coherence.component.util.CacheEvent;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.util.Listeners;
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
            
            Channel channel = getChannel();
            _assert(channel != null);
            
            RemoteNamedCache cache = (RemoteNamedCache) channel.getReceiver();
            _assert(cache != null);
            
            Listeners listeners = cache.getDeactivationListeners();
            
            if (!listeners.isEmpty())
                {
                com.tangosol.util.MapEvent evtDeleted = new com.tangosol.util.MapEvent(cache, com.tangosol.util.MapEvent.ENTRY_DELETED, null, null, null);
                // dispatch the event to the listeners, which are all synchronous (hence the null Queue)
                com.tangosol.coherence.component.util.CacheEvent.dispatchSafe(evtDeleted, listeners, null /*Queue*/);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$PartialResponse
    
    /**
     * Generic Response component used for partial NamedCache Protocol
     * Responses.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PartialResponse
            extends    com.tangosol.coherence.component.net.extend.message.response.PartialResponse
        {
        // ---- Fields declarations ----
        
        /**
         * Property Filter
         *
         * Filter returned to the caller. This is only used for QueryRequests
         * that carry a LimitFilter.
         */
        private com.tangosol.util.Filter __m_Filter;
        
        /**
         * Property FilterCookie
         *
         * Cookie used by a returned LimitFilter (see Filter property).
         */
        private Object __m_FilterCookie;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 1000;
        
        // Default constructor
        public PartialResponse()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public PartialResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PartialResponse();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$PartialResponse".replace('/', '.'));
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
        
        // Accessor for the property "Filter"
        /**
         * Getter for property Filter.<p>
        * Filter returned to the caller. This is only used for QueryRequests
        * that carry a LimitFilter.
         */
        public com.tangosol.util.Filter getFilter()
            {
            return __m_Filter;
            }
        
        // Accessor for the property "FilterCookie"
        /**
         * Getter for property FilterCookie.<p>
        * Cookie used by a returned LimitFilter (see Filter property).
         */
        public Object getFilterCookie()
            {
            return __m_FilterCookie;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import com.tangosol.util.Filter;
            
            super.readExternal(in);
            
            // COH-6337
            if (getImplVersion() > 2)
                {
                setFilter((Filter) in.readObject(7));
                setFilterCookie(in.readObject(8));
                }
            }
        
        // Declared at the super level
        public void run()
            {
            // no-op
            }
        
        // Accessor for the property "Filter"
        /**
         * Setter for property Filter.<p>
        * Filter returned to the caller. This is only used for QueryRequests
        * that carry a LimitFilter.
         */
        public void setFilter(com.tangosol.util.Filter filter)
            {
            __m_Filter = filter;
            }
        
        // Accessor for the property "FilterCookie"
        /**
         * Setter for property FilterCookie.<p>
        * Cookie used by a returned LimitFilter (see Filter property).
         */
        public void setFilterCookie(Object oCookie)
            {
            __m_FilterCookie = oCookie;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            // COH-6337
            if (getImplVersion() > 2)
                {
                out.writeObject(7, getFilter());
                out.writeObject(8, getFilterCookie());
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$PutAllRequest
    
    /**
     * Map.putAll(Map mapEntries) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PutAllRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property Map
         *
         * The map of entries to be updated when this message is processed.
         */
        private java.util.Map __m_Map;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 7;
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
            __mapChildren.put("Status", NamedCacheFactory.PutAllRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public PutAllRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public PutAllRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutAllRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$PutAllRequest".replace('/', '.'));
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
            // import java.util.Map;
            
            Map map = getMap();
            
            String sMapDesc = map == null ? "null" : "Size=" + map.size() + ", HashCode=" + map.hashCode();
            
            return super.getDescription() + ", Map=(" + sMapDesc + ')';
            }
        
        // Accessor for the property "Map"
        /**
         * Getter for property Map.<p>
        * The map of entries to be updated when this message is processed.
         */
        public java.util.Map getMap()
            {
            return __m_Map;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            cache.putAll(getMap());
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            // import java.util.HashMap;
            
            super.readExternal(in);
            
            setMap(in.readMap(1, new HashMap()));
            }
        
        // Accessor for the property "Map"
        /**
         * Setter for property Map.<p>
        * The map of entries to be updated when this message is processed.
         */
        public void setMap(java.util.Map mapEntries)
            {
            __m_Map = mapEntries;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeMap(1, getMap());
            
            // release state
            setMap(null);
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$PutAllRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutAllRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$PutAllRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$ReadyRequest

    /**
     * Map.isReady() Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ReadyRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
        {
        // ---- Fields declarations ----

        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 61;
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
            __mapChildren.put("Status", NamedCacheFactory.ReadyRequest.Status.get_CLASS());
            }

        // Default constructor
        public ReadyRequest()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public ReadyRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ReadyRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$ReadyRequest".replace('/', '.'));
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
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.util.Base;

            NamedCache cache = getNamedCache();
            _assert(cache != null);

            response.setResult(cache.isReady());
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$PutRequest
    
    /**
     * Map.put(Object oKey, Object oValue, long cMillis) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PutRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property ExpiryDelay
         *
         * The entry expiration value.
         */
        private long __m_ExpiryDelay;
        
        /**
         * Property ReturnRequired
         *
         * If true, this PutRequest should return the old value back to the
         * caller; otherwise the return value will be ignored.
         */
        private boolean __m_ReturnRequired;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 5;
        
        /**
         * Property Value
         *
         * The new entry value.
         */
        private Object __m_Value;
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
            __mapChildren.put("Status", NamedCacheFactory.PutRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public PutRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public PutRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$PutRequest".replace('/', '.'));
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
            Object oValue = getValue();
            
            return super.getDescription()
                    + ", Value="  + (oValue == null ? "null" : oValue.getClass().getSimpleName() + "(HashCode=" + oValue.hashCode() + ')')
                    + ", Expiry=" + getExpiryDelay();
            }
        
        // Accessor for the property "ExpiryDelay"
        /**
         * Getter for property ExpiryDelay.<p>
        * The entry expiration value.
         */
        public long getExpiryDelay()
            {
            return __m_ExpiryDelay;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Accessor for the property "Value"
        /**
         * Getter for property Value.<p>
        * The new entry value.
         */
        public Object getValue()
            {
            return __m_Value;
            }
        
        // Accessor for the property "ReturnRequired"
        /**
         * Getter for property ReturnRequired.<p>
        * If true, this PutRequest should return the old value back to the
        * caller; otherwise the return value will be ignored.
         */
        public boolean isReturnRequired()
            {
            return __m_ReturnRequired;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            Object oValue = cache.put(getKey(), getValue(), getExpiryDelay());
            if (isReturnRequired())
                {
                response.setResult(oValue);
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            setValue(in.readObject(2));
            setExpiryDelay(in.readLong(3));
            setReturnRequired(in.readBoolean(4));
            }
        
        // Accessor for the property "ExpiryDelay"
        /**
         * Setter for property ExpiryDelay.<p>
        * The entry expiration value.
         */
        public void setExpiryDelay(long cMillis)
            {
            __m_ExpiryDelay = cMillis;
            }
        
        // Accessor for the property "ReturnRequired"
        /**
         * Setter for property ReturnRequired.<p>
        * If true, this PutRequest should return the old value back to the
        * caller; otherwise the return value will be ignored.
         */
        public void setReturnRequired(boolean fReturn)
            {
            __m_ReturnRequired = fReturn;
            }
        
        // Accessor for the property "Value"
        /**
         * Setter for property Value.<p>
        * The new entry value.
         */
        public void setValue(Object oValue)
            {
            __m_Value = oValue;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeObject(2, getValue());
            out.writeLong(3, getExpiryDelay());
            out.writeBoolean(4, isReturnRequired());
            
            // release state
            setValue(null);
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$PutRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$PutRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$QueryRequest
    
    /**
     * QueryMap.entrySet(Filter filter) and keySet(Filter filter) Request
     * message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class QueryRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.FilterRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property Cookie
         *
         * Opaque cookie used to support streaming.
         * 
         * See PartialResponse#Cookie.
         */
        private com.tangosol.util.Binary __m_Cookie;
        
        /**
         * Property FilterCookie
         *
         * Cookie used by LimitFilter.
         * 
         * See $PartialResponse#FilterCookie.
         */
        private Object __m_FilterCookie;
        
        /**
         * Property KeysOnly
         *
         * Specifies what kind of response is required: if true, a key Set is
         * sent back; otherwise an entry Set.
         */
        private boolean __m_KeysOnly;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 41;
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
            __mapChildren.put("Status", NamedCacheFactory.QueryRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public QueryRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public QueryRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$QueryRequest".replace('/', '.'));
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
        
        // Accessor for the property "Cookie"
        /**
         * Getter for property Cookie.<p>
        * Opaque cookie used to support streaming.
        * 
        * See PartialResponse#Cookie.
         */
        public com.tangosol.util.Binary getCookie()
            {
            return __m_Cookie;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return super.getDescription()
                    + ", KeysOnly=" + isKeysOnly()
                    + ", Cookie="   + getCookie();
            }
        
        // Accessor for the property "FilterCookie"
        /**
         * Getter for property FilterCookie.<p>
        * Cookie used by LimitFilter.
        * 
        * See $PartialResponse#FilterCookie.
         */
        public Object getFilterCookie()
            {
            return __m_FilterCookie;
            }
        
        // Declared at the super level
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Declared at the super level
        /**
         * Factory method: create a new Response instance.
        * 
        * @param  the MessageFactory used to create the new Response object
        * 
        * @return a new Response object
         */
        protected com.tangosol.coherence.component.net.extend.message.Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
            {
            // import Component.Net.Extend.Message.Response as com.tangosol.coherence.component.net.extend.message.Response;
            
            return (com.tangosol.coherence.component.net.extend.message.Response) factory.createMessage(NamedCacheFactory.PartialResponse.TYPE_ID);
            }
        
        // Accessor for the property "KeysOnly"
        /**
         * Getter for property KeysOnly.<p>
        * Specifies what kind of response is required: if true, a key Set is
        * sent back; otherwise an entry Set.
         */
        public boolean isKeysOnly()
            {
            return __m_KeysOnly;
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
            // import com.tangosol.net.CacheService;
            // import com.tangosol.net.Member;
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.net.ServiceInfo;
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.filter.AlwaysFilter;
            // import com.tangosol.util.filter.KeyAssociatedFilter;
            // import com.tangosol.util.filter.LimitFilter;
            // import com.tangosol.util.filter.PartitionedFilter;
            // import java.util.ArrayList;
            // import java.util.Collections;
            // import java.util.Comparator;
            // import java.util.Iterator;
            // import java.util.List;
            // import java.util.Set;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            CacheService service = cache.getCacheService();
            _assert(service != null);
            
            ServiceInfo info = service.getInfo();
            _assert(info != null);
            
            Filter filter = getFilter();
            if (filter == null)
                {
                // see COH-1183
                filter = AlwaysFilter.INSTANCE;
                }
            else if (filter instanceof LimitFilter         ||
                     filter instanceof KeyAssociatedFilter ||
                     filter instanceof PartitionedFilter)
                {
                if (filter instanceof LimitFilter)
                    {
                    Object oCookie = getFilterCookie();
            
                    // transform cookie array from its serializable representation
                    if (oCookie instanceof Object[])
                        {
                        Object[] ao = (Object[]) oCookie;
                        Object   o  = ao[0];
                        if (o instanceof Object[])
                            {
                            Object[] aoMemberIds = (Object[]) o;
                            int      cMembers    = aoMemberIds.length;
                            List     listMembers = new ArrayList(cMembers);
            
                            for (int i = 0; i < cMembers; ++i)
                                {
                                int    nId    = ((Integer) aoMemberIds[i]).intValue();
                                Member member = info.getServiceMember(nId);
                                if (member == null)
                                    {
                                    listMembers = Collections.emptyList();
                                    break;
                                    }
                                else
                                    {
                                    listMembers.add(member);
                                    }
                                }
            
                            ao[0] = listMembers;
                            }
                        }
            
                    ((LimitFilter) filter).setCookie(oCookie);
                    }
            
                if (isKeysOnly())
                    {
                    response.setResultAsCollection(cache.keySet(filter));
                    }
                else
                    {
                    // COH-2717
                    Comparator comparator = filter instanceof LimitFilter
                            ? ((LimitFilter) filter).getComparator()
                            : null;
                    response.setResultAsEntrySet(comparator == null
                            ? cache.entrySet(filter)
                            : cache.entrySet(filter, comparator));
                    }
            
                 if (filter instanceof LimitFilter)
                    {
                    Object oCookie = ((LimitFilter) filter).getCookie();
            
                    // transform cookie array into a serializable representation
                    if (oCookie instanceof Object[])
                        {
                        Object[] ao = (Object[]) oCookie;
                        Object   o  = ao[0];
                        if (o instanceof List)
                            {
                            List list = new ArrayList();
                            for (Iterator iter = ((List) o).iterator(); iter.hasNext(); )
                                {
                                Member member = (Member) iter.next();
                                list.add(Integer.valueOf(member.getId()));
                                }
                            ao[0] = list.toArray();
                            }
                        }
            
                    // send the LimitFilter/Cookie back to the client
                    NamedCacheFactory.PartialResponse responsePartial = (NamedCacheFactory.PartialResponse) response;
                    responsePartial.setFilter(filter);
                    responsePartial.setFilterCookie(oCookie);
                    }
            
                return;
                }
            
            boolean      fKeysOnly = isKeysOnly();
            Object[]     aoCookie  = decodeCookie(getCookie());
            PartitionSet parts     = (PartitionSet) aoCookie[0];
            int          cBatch    = ((Integer) aoCookie[1]).intValue();
            int          cPart     = parts.getPartitionCount();
            
            Set set;
            if (cBatch == 0)
                {
                // query an initial partition
                set = query(cache, filter, fKeysOnly, parts, 1, null);
            
                // calculate the size of the first partition's worth of results
                int cb = calculateBinarySize(set, !fKeysOnly);
            
                // calculate the batch size
                cBatch = calculateBatchSize(cPart, cb);
            
                // query the remainder of the batch
                if (cBatch > 1 && cPart > 1)
                    {
                    set = query(cache, filter, fKeysOnly, parts, cBatch - 1, set);
                    }
                }
            else
                {
                set = query(cache, filter, fKeysOnly, parts, cBatch, null);
                }
            
            if (fKeysOnly)
                {
                response.setResultAsCollection(set);
                }
            else
                {
                response.setResultAsEntrySet(set);
                }
            
            ((NamedCacheFactory.PartialResponse) response).setCookie(encodeCookie(parts, cBatch));
            }
        
        /**
         * Helper method that runs the specified query against the given
        * NamedCache.
        * 
        * @param cache  the NamedCache to query
        * @param filter  the Filter representing the query
        * @param fKeysOnly  if true, return just the keys of the matching
        * entries; otherwise, return the matching entries
        * @param partsRemain  the remaining partitions that have yet to be
        * queried
        * @param cBatch  the maximum number of partitions to query
        * @param setResult an optional Set containing the previous query
        * results; the returned Set will contain both the previous results as
        * well as the results of the query
        * 
        * @return a Set containing the query results and any previous results
         */
        protected java.util.Set query(com.tangosol.net.NamedCache cache, com.tangosol.util.Filter filter, boolean fKeysOnly, com.tangosol.net.partition.PartitionSet partsRemain, int cBatch, java.util.Set setResult)
            {
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.ImmutableArrayList;
            // import com.tangosol.util.filter.PartitionedFilter;
            // import java.util.Set;
            
            // calculate the next batch of partitions
            PartitionSet partsBatch = removePartitionBatch(partsRemain, cBatch);
            
            // limit the query to the next batch of partitions
            filter = new PartitionedFilter(filter, partsBatch);
            
            // perform the query
            Set set = fKeysOnly ? cache.keySet(filter) : cache.entrySet(filter);
            if (setResult == null || setResult.isEmpty())
                {
                setResult = set;
                }
            else if (set.isEmpty())
                {
                // nothing to do
                }
            else
                {
                Object[] aoOld = setResult.toArray();
                Object[] aoNew = set.toArray();
                int      cOld  = aoOld.length;
                int      cNew  = aoNew.length;
                int      cAll  = cOld + cNew;
                Object[] aoAll = new Object[cAll];
            
                System.arraycopy(aoOld, 0, aoAll, 0, cOld);
                System.arraycopy(aoNew, 0, aoAll, cOld, cNew);
            
                setResult = new ImmutableArrayList(aoAll);
                }
            
            return setResult;
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            setKeysOnly(in.readBoolean(2));
            setCookie(in.readBinary(3));
            setFilterCookie(in.readObject(4));
            }
        
        // Accessor for the property "Cookie"
        /**
         * Setter for property Cookie.<p>
        * Opaque cookie used to support streaming.
        * 
        * See PartialResponse#Cookie.
         */
        public void setCookie(com.tangosol.util.Binary bin)
            {
            __m_Cookie = bin;
            }
        
        // Accessor for the property "FilterCookie"
        /**
         * Setter for property FilterCookie.<p>
        * Cookie used by LimitFilter.
        * 
        * See $PartialResponse#FilterCookie.
         */
        public void setFilterCookie(Object oCookie)
            {
            __m_FilterCookie = oCookie;
            }
        
        // Accessor for the property "KeysOnly"
        /**
         * Setter for property KeysOnly.<p>
        * Specifies what kind of response is required: if true, a key Set is
        * sent back; otherwise an entry Set.
         */
        public void setKeysOnly(boolean fKeysOnly)
            {
            __m_KeysOnly = fKeysOnly;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeBoolean(2, isKeysOnly());
            out.writeBinary(3, getCookie());
            out.writeObject(4, getFilterCookie());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$QueryRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$QueryRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$RemoveAllRequest
    
    /**
     * Map.keySet().removeAll(Collection colKeys) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class RemoveAllRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeySetRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 10;
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
            __mapChildren.put("Status", NamedCacheFactory.RemoveAllRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public RemoveAllRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public RemoveAllRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveAllRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$RemoveAllRequest".replace('/', '.'));
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(Boolean.valueOf(cache.keySet().removeAll(getKeySet())));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$RemoveAllRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveAllRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$RemoveAllRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$RemoveRequest
    
    /**
     * Map.remove(Object oKey) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class RemoveRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property ReturnRequired
         *
         * If true, this RemoveRequest should return the old value back to the
         * caller; otherwise the return value will be ignored.
         */
        private transient boolean __m_ReturnRequired;
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 6;
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
            __mapChildren.put("Status", NamedCacheFactory.RemoveRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public RemoveRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public RemoveRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$RemoveRequest".replace('/', '.'));
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
        public int getTypeId()
            {
            return TYPE_ID;
            }
        
        // Accessor for the property "ReturnRequired"
        /**
         * Getter for property ReturnRequired.<p>
        * If true, this RemoveRequest should return the old value back to the
        * caller; otherwise the return value will be ignored.
         */
        public boolean isReturnRequired()
            {
            return __m_ReturnRequired;
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
            // import com.tangosol.net.NamedCache;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            if (isReturnRequired())
                {
                response.setResult(cache.remove(getKey()));
                }
            else
                {
                cache.keySet().remove(getKey());
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            setReturnRequired(in.readBoolean(2));
            }
        
        // Accessor for the property "ReturnRequired"
        /**
         * Setter for property ReturnRequired.<p>
        * If true, this RemoveRequest should return the old value back to the
        * caller; otherwise the return value will be ignored.
         */
        public void setReturnRequired(boolean fReturn)
            {
            __m_ReturnRequired = fReturn;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeBoolean(2, isReturnRequired());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$RemoveRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$RemoveRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$Response
    
    /**
     * Generic Response component used for basic NamedCache Protocol Responses.
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.Response();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$Response".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$SizeRequest
    
    /**
     * Map.size() Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SizeRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
        {
        // ---- Fields declarations ----
        
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
            __mapChildren.put("Status", NamedCacheFactory.SizeRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public SizeRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SizeRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.SizeRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$SizeRequest".replace('/', '.'));
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
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.util.Base;
            
            NamedCache cache = getNamedCache();
            _assert(cache != null);
            
            response.setResult(Base.makeInteger(cache.size()));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$SizeRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.SizeRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$SizeRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$UnlockRequest
    
    /**
     * NamedCache.unlock(Object oKey) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class UnlockRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
         * The type identifier for this Message component class.
         */
        public static final int TYPE_ID = 32;
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
            __mapChildren.put("Status", NamedCacheFactory.UnlockRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public UnlockRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public UnlockRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.UnlockRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$UnlockRequest".replace('/', '.'));
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
            // import Component.Net.Extend.Proxy.NamedCacheProxy;
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.util.ConcurrentMap;
            
            Channel       channel = getChannel();
            Object        oKey    = getKey();
            NamedCache    cache   = getNamedCache();
            ConcurrentMap map     = (ConcurrentMap)
                    channel.getAttribute(NamedCacheProxy.ATTR_LOCK_MAP);
            
            _assert(cache != null);
            _assert(map != null);
            
            boolean fUnlocked;
            if (fUnlocked = map.lock(oKey, -1L))
                {
                try
                    {
                    // check status
                    if (map.containsKey(oKey))
                        {
                        // key is locked; attempt to unlock it
                        if (fUnlocked = cache.unlock(oKey))
                            {
                            map.remove(oKey);
                            }
                        }
                    else
                        {
                        fUnlocked = cache.unlock(oKey);
                        }
                    }
                finally
                    {
                    map.unlock(oKey);
                    }
                }
            
            response.setResult(Boolean.valueOf(fUnlocked));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory$UnlockRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.UnlockRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NamedCacheFactory$UnlockRequest$Status".replace('/', '.'));
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
    }
