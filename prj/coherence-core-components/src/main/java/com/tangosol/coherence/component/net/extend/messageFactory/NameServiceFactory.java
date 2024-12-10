
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.tangosol.coherence.component.net.extend.connection.TcpConnection;
import com.tangosol.coherence.component.util.NameService;
import com.tangosol.net.InetAddressHelper;

/**
 * MessageFactory for version 1 of the NameService Protocol.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class NameServiceFactory
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
        __mapChildren.put("BindRequest", NameServiceFactory.BindRequest.get_CLASS());
        __mapChildren.put("LookupRequest", NameServiceFactory.LookupRequest.get_CLASS());
        __mapChildren.put("Response", NameServiceFactory.Response.get_CLASS());
        }
    
    // Default constructor
    public NameServiceFactory()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public NameServiceFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NameServiceFactory".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory$BindRequest
    
    /**
     * NameService.bind(String sName, Object) Request message.
     * 
     * The resource remains bound for the life of the channel which bound it,
     * and is automatically unbound when the channel closes.  Note that while
     * we could make it revocable, the intent is to mimic socket binding which
     * is not revocable either.  Instead of building in revocation logic which
     * could leave two processes believing they own the binding we rely on
     * higher level fault tolerance to fill in the gap, namely a client will
     * try NSs on other machines if it failes to connect.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BindRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NameServiceRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property Name
         *
         * The name to bind.
         */
        private String __m_Name;
        
        /**
         * Property Resource
         *
         * The resource associated with the name
         */
        private Object __m_Resource;
        
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
            __mapChildren.put("Status", NameServiceFactory.BindRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public BindRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BindRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.BindRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NameServiceFactory$BindRequest".replace('/', '.'));
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
        
        // Accessor for the property "Name"
        /**
         * Getter for property Name.<p>
        * The name to bind.
         */
        public String getName()
            {
            return __m_Name;
            }
        
        // Accessor for the property "Resource"
        /**
         * Getter for property Resource.<p>
        * The resource associated with the name
         */
        public Object getResource()
            {
            return __m_Resource;
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
            // import Component.Util.NameService;
            
            try
                {
                ((NameService) getNameService()).bind(getName(), getResource(), getChannel());
                }
            catch (Exception e)
                {
                response.setFailure(true);
                response.setResult(e);
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            setName(in.readString(1));
            setResource(in.readObject(2));
            }
        
        // Accessor for the property "Name"
        /**
         * Setter for property Name.<p>
        * The name to bind.
         */
        public void setName(String sName)
            {
            __m_Name = sName;
            }
        
        // Accessor for the property "Resource"
        /**
         * Setter for property Resource.<p>
        * The resource associated with the name
         */
        public void setResource(Object oResource)
            {
            __m_Resource = oResource;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeString(1, getName());
            out.writeObject(2, getResource());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory$BindRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.BindRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NameServiceFactory$BindRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory$LookupRequest
    
    /**
     * NameService.lookup(String sName) Request message.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LookupRequest
            extends    com.tangosol.coherence.component.net.extend.message.request.NameServiceRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property LookupName
         *
         * The name to lookup.
         */
        private String __m_LookupName;
        
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
            __mapChildren.put("Status", NameServiceFactory.LookupRequest.Status.get_CLASS());
            }
        
        // Default constructor
        public LookupRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LookupRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.LookupRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NameServiceFactory$LookupRequest".replace('/', '.'));
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
        
        // Accessor for the property "LookupName"
        /**
         * Getter for property LookupName.<p>
        * The name to lookup.
         */
        public String getLookupName()
            {
            return __m_LookupName;
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
            // import Component.Net.Extend.Connection.TcpConnection;
            // import Component.Util.NameService;
            // import Component.Util.NameService$RequestContext as com.tangosol.coherence.component.util.NameService.RequestContext;
            // import com.tangosol.net.InetAddressHelper;
            
            try
                {
                TcpConnection connection = (TcpConnection) getChannel().getConnection();
                
                com.tangosol.coherence.component.util.NameService.RequestContext context = new com.tangosol.coherence.component.util.NameService.RequestContext();
                context.setMember(connection.getMember());
                context.setAcceptAddress(connection.getSocket().getLocalAddress());
                context.setSourceAddress(InetAddressHelper
                    .getAddress(connection.getSocket().getRemoteSocketAddress()));
             
                response.setResult(((NameService) getNameService()).lookup(getLookupName(), context));
                }
            catch (Exception e)
                {
                response.setFailure(true);
                response.setResult(e);
                }
            }
        
        // Declared at the super level
        public void readExternal(com.tangosol.io.pof.PofReader in)
                throws java.io.IOException
            {
            super.readExternal(in);
            
            setLookupName(in.readString(1));
            }
        
        // Accessor for the property "LookupName"
        /**
         * Setter for property LookupName.<p>
        * The name to lookup.
         */
        public void setLookupName(String sName)
            {
            __m_LookupName = sName;
            }
        
        // Declared at the super level
        public void writeExternal(com.tangosol.io.pof.PofWriter out)
                throws java.io.IOException
            {
            super.writeExternal(out);
            
            out.writeString(1, getLookupName());
            }

        // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory$LookupRequest$Status
        
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
                return new com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.LookupRequest.Status();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NameServiceFactory$LookupRequest$Status".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory$Response
    
    /**
     * Generic Response component used for all NameService Protocol Responses.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Response
            extends    com.tangosol.coherence.component.net.extend.message.Response
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ID
         *
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
            return new com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.Response();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/messageFactory/NameServiceFactory$Response".replace('/', '.'));
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
