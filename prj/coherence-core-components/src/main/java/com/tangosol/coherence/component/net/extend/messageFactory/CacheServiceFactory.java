
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol;
import com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.messaging.ConnectionManager;
import java.net.URI;

/**
 * MessageFactory for version 1 of the CacheService Protocol.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheServiceFactory
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
        __mapChildren.put("DestroyCacheRequest", CacheServiceFactory.DestroyCacheRequest.get_CLASS());
        __mapChildren.put("EnsureCacheRequest", CacheServiceFactory.EnsureCacheRequest.get_CLASS());
        __mapChildren.put("Response", CacheServiceFactory.Response.get_CLASS());
        }
    
    // Default constructor
    public CacheServiceFactory()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheServiceFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/CacheServiceFactory".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory$DestroyCacheRequest
    
    /**
     * CacheService.destroyCache(NamedCache map) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DestroyCacheRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.CacheServiceRequest
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
            __mapChildren.put("Status", CacheServiceFactory.DestroyCacheRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public DestroyCacheRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DestroyCacheRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.DestroyCacheRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/CacheServiceFactory$DestroyCacheRequest".replace('/', '.'));
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
            // import com.tangosol.net.CacheService;
            
            CacheService service = getCacheService();
            _assert(service != null);
            
            service.destroyCache(service.ensureCache(getCacheName(), null));
            
            response.setResult(Boolean.TRUE);
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory$DestroyCacheRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.DestroyCacheRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/CacheServiceFactory$DestroyCacheRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory$EnsureCacheRequest
    
    /**
     * CacheService.ensureCache(String sName, ClassLoader loader) Request
     * message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnsureCacheRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.CacheServiceRequest
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
            __mapChildren.put("Status", CacheServiceFactory.EnsureCacheRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public EnsureCacheRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EnsureCacheRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.EnsureCacheRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/CacheServiceFactory$EnsureCacheRequest".replace('/', '.'));
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
            // import Component.Net.Extend.Protocol.NamedCacheProtocol;
            // import Component.Net.Extend.Proxy.NamedCacheProxy;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.ProxyService;
            // import Component.Util.Daemon.QueueProcessor.Service.Peer;
            // import com.tangosol.net.CacheService;
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.net.Service;
            // import com.tangosol.net.messaging.ConnectionManager;
            // import java.net.URI;
            
            CacheService service = getCacheService();
            _assert(service != null);
            
            String sName = getCacheName();
            _assert(sName != null);
            
            NamedCache cache = service.ensureCache(sName, null);
            _assert(cache != null); 
            
            NamedCacheProxy proxy = new NamedCacheProxy();
            proxy.setNamedCache(cache);
            proxy.setLockEnabled(isLockEnabled());
            proxy.setReadOnly(isReadOnly());
            proxy.setTransferThreshold(getTransferThreshold());
            
            URI uri = getChannel().getConnection().createChannel(
                    NamedCacheProtocol.getInstance(), null, proxy);
            
            ConnectionManager manager = getChannel().getConnection().getConnectionManager();
            if (manager instanceof Peer)
                {
                Service parentService = ((Peer) manager).getParentService();
                if (parentService instanceof ProxyService)
                    {
                    proxy.setDaemonPool(((ProxyService) parentService).getDaemonPool());
                    }
                }
            
            response.setResult(String.valueOf(uri));
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory$EnsureCacheRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.EnsureCacheRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/CacheServiceFactory$EnsureCacheRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory$Response
    
    /**
     * Generic Response component used for all CacheService Protocol Responses.
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.Response();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/CacheServiceFactory$Response".replace('/', '.'));
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
