
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter

package com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter;

import com.tangosol.coherence.component.connector.NamedCacheRecord;
import com.tangosol.coherence.component.connector.connectionInfo.CacheInfo;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.util.TransactionMap;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.spi.LocalTransactionException;
import java.util.Iterator;
import java.util.Map;

/**
 * This component is a factory of both ManagedConnection and EIS-specific
 * connection factory instances. Default implementation assumes existence of
 * the following static children components:
 * <ul>
 * <li>ConnectionFactory
 * <li>ManagedConnection
 * <li>DefaultConnectionManager
 * </ul>
 * 
 * @see jakarta.resource.spi.ManagedConnectionFactory
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheAdapter
        extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter
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
        __mapChildren.put("ConnectionFactory", CacheAdapter.ConnectionFactory.get_CLASS());
        __mapChildren.put("DefaultConnectionManager", com.tangosol.coherence.component.connector.ResourceAdapter.DefaultConnectionManager.get_CLASS());
        __mapChildren.put("ManagedConnection", CacheAdapter.ManagedConnection.get_CLASS());
        }
    
    // Default constructor
    public CacheAdapter()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheAdapter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        _addChild(new CacheAdapter.AdapterMetaData("AdapterMetaData", this, true), "AdapterMetaData");
        
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
        return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter();
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
            clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter".replace('/', '.'));
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
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        // Application server creates a ConnectionFactory by calling createConnectionFactory(...)
        // which creates a ConnectionFactory as a child component.
        // However,  WL 6.1 uses RMI (and therefore serialization) to pass that ConnectionFactory
        // instance around.
        
        super.onInit();
        
        if (!is_Deserialized())
            {
            log("\n" + getMetaData() + "\n", 3);
            }
        }

    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$AdapterMetaData
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class AdapterMetaData
            extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.AdapterMetaData
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public AdapterMetaData()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public AdapterMetaData(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setAdapterName("CoherenceTx");
                setAdapterShortDescription("Resource adapter for Coherence(tm) clustered cache");
                setAdapterVendorName("Oracle");
                setAdapterVersion("3.2 (build 40)");
                String[] a0 = new String[1];
                    {
                    a0[0] = "com.tangosol.run.xml.XmlSerializable";
                    }
                setInteractionSpecsSupported(a0);
                setSpecVersion("1.0");
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
        
        // Getter for virtual constant SupportsExecuteWithInputAndOutputRecord
        public boolean isSupportsExecuteWithInputAndOutputRecord()
            {
            return false;
            }
        
        // Getter for virtual constant SupportsExecuteWithInputRecordOnly
        public boolean isSupportsExecuteWithInputRecordOnly()
            {
            return true;
            }
        
        // Getter for virtual constant SupportsLocalTransactionDemarcation
        public boolean isSupportsLocalTransactionDemarcation()
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
            return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.AdapterMetaData();
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
                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$AdapterMetaData".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ConnectionFactory
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConnectionFactory
            extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConnectionFactory()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConnectionFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            _addChild(new CacheAdapter.ConnectionFactory.RecordFactory("RecordFactory", this, true), "RecordFactory");
            
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
            return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ConnectionFactory();
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
                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ConnectionFactory".replace('/', '.'));
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
        public String toString()
            {
            return get_Name() + "@" + hashCode();
            }
        
        // Declared at the super level
        public jakarta.resource.spi.ConnectionRequestInfo translateConnectionSpec(jakarta.resource.cci.ConnectionSpec properties)
            {
            // import Component.Connector.ConnectionInfo.CacheInfo;
            
            if (properties instanceof CacheInfo)
                {
                return (CacheInfo) properties;
                }
            else
                {
                CacheInfo info = new CacheInfo();
            
                if (properties != null)
                    {
                    info.fromConnectionSpec(properties);
                    }
            
                return info;
                }
            }

        // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ConnectionFactory$RecordFactory
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class RecordFactory
                extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory
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
                __mapChildren.put("MappedRecord", CacheAdapter.ConnectionFactory.RecordFactory.MappedRecord.get_CLASS());
                }
            
            // Default constructor
            public RecordFactory()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public RecordFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ConnectionFactory.RecordFactory();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ConnectionFactory$RecordFactory".replace('/', '.'));
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
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Declared at the super level
            public jakarta.resource.cci.IndexedRecord createIndexedRecord(String sRecordName)
                    throws jakarta.resource.ResourceException
                {
                // import jakarta.resource.NotSupportedException;
                
                throw new NotSupportedException("IndexedRecord is not supported, use MappedRecord instead");
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ConnectionFactory$RecordFactory$MappedRecord
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class MappedRecord
                    extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord
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
                    __mapChildren.put("EntrySet", com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.EntrySet.get_CLASS());
                    __mapChildren.put("KeySet", com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.KeySet.get_CLASS());
                    __mapChildren.put("Values", com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.Values.get_CLASS());
                    }
                
                // Default constructor
                public MappedRecord()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public MappedRecord(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ConnectionFactory.RecordFactory.MappedRecord();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ConnectionFactory$RecordFactory$MappedRecord".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ManagedConnection
    
    /**
     * ManagedConnection represents a physical connection to
     * an underlying EIS (Chapter 5.5.4 of JCA 1.0 specification)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ManagedConnection
            extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection
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
            __mapChildren.put("Connection", CacheAdapter.ManagedConnection.Connection.get_CLASS());
            __mapChildren.put("LocalTransaction", CacheAdapter.ManagedConnection.LocalTransaction.get_CLASS());
            }
        
        // Default constructor
        public ManagedConnection()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ManagedConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setClosed(false);
                setConnectionSet(new java.util.HashSet());
                setCurrentTransaction(new java.lang.ThreadLocal());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new CacheAdapter.ManagedConnection.ManagedConnectionMetaData("ManagedConnectionMetaData", this, true), "ManagedConnectionMetaData");
            
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
            return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ManagedConnection();
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
                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ManagedConnection".replace('/', '.'));
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
         * Check the connection subject and info.
        * 
        * @see ResourceAdapter#createManagedConnection
         */
        public void authenticate(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
                throws jakarta.resource.ResourceException
            {
            // anything goes
            
            super.authenticate(subject, cxInfo);
            }
        
        // Declared at the super level
        public Object getConnection(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
                throws jakarta.resource.ResourceException
            {
            // import Component.Connector.ConnectionInfo.CacheInfo;
            // import com.tangosol.net.CacheFactory;
            // import com.tangosol.net.CacheService;
            // import com.tangosol.net.Cluster;
            // import jakarta.resource.ResourceException;
            
            try
                {
                String sServiceName;
                String sServiceType;
                int    nConcur;
                int    nIsolation;
                int    nTimeout;
            
                if (cxInfo instanceof CacheInfo)
                    {
                    CacheInfo info = (CacheInfo) cxInfo;
                    
                    sServiceName = info.getServiceName();
                    sServiceType = info.getServiceType();
                    nConcur      = info.getConcurrency();
                    nIsolation   = info.getIsolation();
                    nTimeout     = info.getTimeout();
                    }
                else
                    {
                    throw new IllegalArgumentException("Invalid ConnectionInfo: " + cxInfo);
                    }
            
                Cluster      cluster = CacheFactory.ensureCluster();
                CacheService service = null;
            
                if (sServiceName == null && sServiceType == null)
                    {
                    // service agnostic; using ConfigurableCacheFactory
                    }
                else
                    {
                    try
                        {
                        service = (CacheService) cluster.getService(sServiceName);
                        }
                    catch (ClassCastException e)
                        {
                        throw new IllegalArgumentException("Not a CacheService: " +
                            sServiceName);
                        }
            
                    if (service == null)
                        {
                        throw new IllegalArgumentException("Failed to find service: " +
                            sServiceName);
                        }
                    else if (!service.getInfo().getServiceType().equals(sServiceType))
                        {
                        throw new IllegalArgumentException("Requested service type \"" +
                            sServiceType + "\", but the existing service has type \""  +
                            service.getInfo().getServiceType() + '"');
                        }
                    }
            
                CacheAdapter.ManagedConnection.Connection connect = (CacheAdapter.ManagedConnection.Connection) super.getConnection(subject, cxInfo);
            
                connect.setCacheService(service);
                connect.setConcurrency(nConcur);
                connect.setIsolation(nIsolation);
                connect.setTimeout(nTimeout);
            
                return connect;
                }
            catch (Exception e)
                {
                log("CoherenceRA: getConnection(): " + e, 4);
                throw ensureResourceException(e);
                }
            }
        
        // Declared at the super level
        public javax.transaction.xa.XAResource getXAResource()
                throws jakarta.resource.ResourceException
            {
            // import jakarta.resource.NotSupportedException;
            
            throw new NotSupportedException(getMetaData().getEISProductName());
            }
        
        // Declared at the super level
        /**
         * Setter for property CurrentTransaction.<p>
        * ThreadLocal object holding a LocalTransaction associated with current
        * thread.
         */
        public void setCurrentTransaction(ThreadLocal tlo)
            {
            super.setCurrentTransaction(tlo);
            }
        
        // Declared at the super level
        /**
         * Verify the connection subject and info against this MC subject and
        * info.
        * 
        * @see #getConnection
         */
        public void verifyAuthentication(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
                throws jakarta.resource.ResourceException
            {
            // anything goes
            
            super.verifyAuthentication(subject, cxInfo);
            }

        // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ManagedConnection$Connection
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Connection
                extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection
            {
            // ---- Fields declarations ----
            
            /**
             * Property CacheService
             *
             * CacheService assosiated with this Connection.
             */
            private transient com.tangosol.net.CacheService __m_CacheService;
            
            /**
             * Property Concurrency
             *
             * The default concurrency value used to create transactional
             * caches.
             */
            private transient int __m_Concurrency;
            
            /**
             * Property Isolation
             *
             * The default transaction isolation value used to create
             * transactional caches.
             */
            private transient int __m_Isolation;
            
            /**
             * Property Timeout
             *
             * The default transaction timeout value used to create
             * transactional caches.
             */
            private transient int __m_Timeout;
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
                __mapChildren.put("Interaction", CacheAdapter.ManagedConnection.Connection.Interaction.get_CLASS());
                __mapChildren.put("UserTransaction", com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.UserTransaction.get_CLASS());
                }
            
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
                
                
                // containment initialization: children
                _addChild(new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.ConnectionMetaData("ConnectionMetaData", this, true), "ConnectionMetaData");
                _addChild(new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.ResultSetInfo("ResultSetInfo", this, true), "ResultSetInfo");
                
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
                return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ManagedConnection.Connection();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ManagedConnection$Connection".replace('/', '.'));
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
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Declared at the super level
            public void close()
                    throws jakarta.resource.ResourceException
                {
                super.close();
                
                setCacheService(null);
                }
            
            // Accessor for the property "CacheService"
            /**
             * Getter for property CacheService.<p>
            * CacheService assosiated with this Connection.
             */
            public com.tangosol.net.CacheService getCacheService()
                {
                return __m_CacheService;
                }
            
            // Accessor for the property "Concurrency"
            /**
             * Getter for property Concurrency.<p>
            * The default concurrency value used to create transactional caches.
             */
            public int getConcurrency()
                {
                return __m_Concurrency;
                }
            
            // Accessor for the property "Isolation"
            /**
             * Getter for property Isolation.<p>
            * The default transaction isolation value used to create
            * transactional caches.
             */
            public int getIsolation()
                {
                return __m_Isolation;
                }
            
            // Accessor for the property "Timeout"
            /**
             * Getter for property Timeout.<p>
            * The default transaction timeout value used to create
            * transactional caches.
             */
            public int getTimeout()
                {
                return __m_Timeout;
                }
            
            // Accessor for the property "CacheService"
            /**
             * Setter for property CacheService.<p>
            * CacheService assosiated with this Connection.
             */
            public void setCacheService(com.tangosol.net.CacheService service)
                {
                __m_CacheService = service;
                }
            
            // Accessor for the property "Concurrency"
            /**
             * Setter for property Concurrency.<p>
            * The default concurrency value used to create transactional caches.
             */
            public void setConcurrency(int nConcurrency)
                {
                __m_Concurrency = nConcurrency;
                }
            
            // Accessor for the property "Isolation"
            /**
             * Setter for property Isolation.<p>
            * The default transaction isolation value used to create
            * transactional caches.
             */
            public void setIsolation(int nIsolation)
                {
                __m_Isolation = nIsolation;
                }
            
            // Accessor for the property "Timeout"
            /**
             * Setter for property Timeout.<p>
            * The default transaction timeout value used to create
            * transactional caches.
             */
            public void setTimeout(int pTimeout)
                {
                __m_Timeout = pTimeout;
                }
            
            // Declared at the super level
            public String toString()
                {
                // import com.tangosol.net.CacheService;
                
                CacheService service = getCacheService();
                
                return super.toString() + ": CacheService{"
                    + (service == null ? "none" :
                        "name="   + service.getInfo().getServiceName() +
                        ", type=" + service.getInfo().getServiceType())
                    + '}';
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ManagedConnection$Connection$Interaction
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Interaction
                    extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.Interaction
                {
                // ---- Fields declarations ----
                
                /**
                 * Property Compatible
                 *
                 * *** LICENSE ***
                 * WARNING: This property name is obfuscated.
                 * 
                 * If true, a successful license check was performed; false
                 * otherwise.
                 */
                private transient boolean __m_Compatible;
                
                // Default constructor
                public Interaction()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Interaction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ManagedConnection.Connection.Interaction();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ManagedConnection$Connection$Interaction".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                // Declared at the super level
                public jakarta.resource.cci.Record execute(jakarta.resource.cci.InteractionSpec ispec, jakarta.resource.cci.Record input)
                        throws jakarta.resource.ResourceException
                    {
                    // import Component.Connector.NamedCacheRecord;
                    // import com.tangosol.net.CacheFactory;
                    // import com.tangosol.net.CacheService;
                    // import com.tangosol.net.NamedCache;
                    // import com.tangosol.util.TransactionMap;
                    // import com.tangosol.util.TransactionMap$Validator as com.tangosol.util.TransactionMap.Validator;
                    // import jakarta.resource.cci.MappedRecord;
                    // import jakarta.resource.ResourceException;
                    
                    CacheAdapter.ManagedConnection.Connection        con = (CacheAdapter.ManagedConnection.Connection) getConnection();
                    CacheAdapter.ManagedConnection mc  = (CacheAdapter.ManagedConnection) con.getManagedConnection();
                    if (mc == null)
                        {
                        throw new ResourceException("Connection has been closed: " + con);
                        }
                    
                    MappedRecord mapInput   = (MappedRecord) input;
                    String       sCacheName = null;
                    ClassLoader  loader     = null;
                    com.tangosol.util.TransactionMap.Validator    validator  = null;
                    Boolean      FImmutable = null;
                    
                    if (mapInput != null)
                        {
                        sCacheName = (String)      mapInput.get("CacheName");
                        loader     = (ClassLoader) mapInput.get("ClassLoader");
                        validator  = (com.tangosol.util.TransactionMap.Validator)   mapInput.get("Validator");
                        FImmutable = (Boolean)     mapInput.get("Immutable");
                        }
                    
                    CacheAdapter.ManagedConnection.LocalTransaction tx      = (CacheAdapter.ManagedConnection.LocalTransaction) mc.getCurrentTransaction().get();
                    CacheService      service = con.getCacheService();
                    NamedCache        cache   = service == null ?
                        CacheFactory.getCache(sCacheName, loader) :
                        service.ensureCache(sCacheName, loader);
                    
                    // license check
                    if (!isCompatible())
                        {
                        try
                            {
                            new com.tangosol.license.CoherenceApplicationEdition();
                            }
                        catch (Throwable e)
                            {
                            throw new com.tangosol.license.LicenseException(e.getMessage());
                            }
                        
                        setCompatible(true);    
                        }
                    
                    if (tx != null)
                        {
                        TransactionMap mapTx = tx.getEnlistedMap(sCacheName);
                        if (mapTx == null)
                            {
                            mapTx = CacheFactory.getLocalTransaction(cache);
                    
                            mapTx.setConcurrency         (con.getConcurrency());
                            mapTx.setTransactionIsolation(con.getIsolation());
                            mapTx.setTransactionTimeout  (con.getTimeout());
                            mapTx.setValidator           (validator);
                            mapTx.setValuesImmutable     (FImmutable != null && FImmutable.booleanValue());
                    
                            tx.enlist(mapTx, sCacheName);
                            }
                    
                        cache = (NamedCache) mapTx;
                        }
                    else
                        {
                        mc.log("CoherenceRA: Execute is called without transaction: " + sCacheName, 4);
                        }
                    
                    NamedCacheRecord cacheRecord = new NamedCacheRecord();
                    cacheRecord.setNamedCache(cache);
                    
                    return cacheRecord;
                    }
                
                // Accessor for the property "Compatible"
                /**
                 * Getter for property Compatible.<p>
                * *** LICENSE ***
                * WARNING: This property name is obfuscated.
                * 
                * If true, a successful license check was performed; false
                * otherwise.
                 */
                private boolean isCompatible()
                    {
                    return __m_Compatible;
                    }
                
                // Accessor for the property "Compatible"
                /**
                 * Setter for property Compatible.<p>
                * *** LICENSE ***
                * WARNING: This property name is obfuscated.
                * 
                * If true, a successful license check was performed; false
                * otherwise.
                 */
                private void setCompatible(boolean fCompatible)
                    {
                    __m_Compatible = fCompatible;
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ManagedConnection$LocalTransaction
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class LocalTransaction
                extends    com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.LocalTransaction
            {
            // ---- Fields declarations ----
            
            /**
             * Property TxMaps
             *
             * Map of TransactionMap objects enlisted in this transaction keyed
             * by their names.
             */
            private transient java.util.Map __m_TxMaps;
            
            // Default constructor
            public LocalTransaction()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public LocalTransaction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setTxMaps(new java.util.HashMap());
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
                return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ManagedConnection.LocalTransaction();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ManagedConnection$LocalTransaction".replace('/', '.'));
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
            public void commit()
                    throws jakarta.resource.ResourceException
                {
                // import com.tangosol.util.TransactionMap;
                // import java.util.Iterator;
                // import java.util.Map;
                // import jakarta.resource.spi.LocalTransactionException;
                
                Map map = getTxMaps();
                try
                    {
                    // everything is happenning on the same thread
                    // so we are guaranteed that the two iterators
                    // yield the same results
                
                    for (Iterator iter = map.values().iterator(); iter.hasNext();)
                        {
                        TransactionMap mapTx = (TransactionMap) iter.next();
                
                        mapTx.prepare();
                        }
                
                    for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                        {
                        Object         oKey  = iter.next();
                        TransactionMap mapTx = (TransactionMap) map.get(oKey);
                
                        mapTx.commit();
                
                        iter.remove(); // delist the transactional map
                        }
                    }
                catch (RuntimeException e)
                    {
                    String sMsg = "CoherenceRA: Commit failed:\n" + getStackTrace(e);
                    ((CacheAdapter.ManagedConnection) getManagedConnection()).log(sMsg, 4);
                
                    rollback();
                
                    LocalTransactionException lte = new LocalTransactionException(sMsg);
                    lte.setLinkedException(e); // in JEE 5.0 this call is deprecated
                    throw lte;
                    }
                
                super.commit();
                }
            
            /**
             * Enlist the specified named cache into this LocalTransaction under
            * a given name
             */
            public void enlist(com.tangosol.util.TransactionMap mapTx, String sName)
                {
                getTxMaps().put(sName, mapTx);
                mapTx.begin();
                }
            
            /**
             * Return a TransactionMap enlisted under a given name
             */
            public com.tangosol.util.TransactionMap getEnlistedMap(String sName)
                {
                // import com.tangosol.util.TransactionMap;
                
                return (TransactionMap) getTxMaps().get(sName);
                }
            
            // Accessor for the property "TxMaps"
            /**
             * Getter for property TxMaps.<p>
            * Map of TransactionMap objects enlisted in this transaction keyed
            * by their names.
             */
            protected java.util.Map getTxMaps()
                {
                return __m_TxMaps;
                }
            
            // Declared at the super level
            public void rollback()
                    throws jakarta.resource.ResourceException
                {
                // import com.tangosol.util.TransactionMap;
                // import java.util.Iterator;
                // import java.util.Map;
                // import java.util.Map$Entry;
                
                Map map = getTxMaps();
                
                for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                    {
                    Object         oKey  = iter.next();
                    TransactionMap mapTx = (TransactionMap) map.get(oKey);
                
                    try
                        {
                        mapTx.rollback();
                        }
                    catch (Exception e)
                        {
                        // rollback should never fail
                        e.printStackTrace(System.err);
                        }
                    finally
                        {
                        iter.remove();
                        }
                    }
                
                super.rollback();
                }
            
            // Accessor for the property "TxMaps"
            /**
             * Setter for property TxMaps.<p>
            * Map of TransactionMap objects enlisted in this transaction keyed
            * by their names.
             */
            protected void setTxMaps(java.util.Map map)
                {
                __m_TxMaps = map;
                }
            }

        // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter$ManagedConnection$ManagedConnectionMetaData
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class ManagedConnectionMetaData
                extends    com.tangosol.coherence.component.connector.ResourceAdapter.ManagedConnection.ManagedConnectionMetaData
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public ManagedConnectionMetaData()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public ManagedConnectionMetaData(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setEISProductName("Coherence");
                    setEISProductVersion("3.0");
                    setMaxConnections(255);
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
                return new com.tangosol.coherence.component.connector.resourceAdapter.cciAdapter.CacheAdapter.ManagedConnection.ManagedConnectionMetaData();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/cciAdapter/CacheAdapter$ManagedConnection$ManagedConnectionMetaData".replace('/', '.'));
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
