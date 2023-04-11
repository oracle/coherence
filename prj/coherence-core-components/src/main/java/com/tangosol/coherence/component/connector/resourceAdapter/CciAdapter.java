
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter

package com.tangosol.coherence.component.connector.resourceAdapter;

import com.tangosol.coherence.component.connector.ConnectionInfo;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.SimpleEnumerator;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransactionException;
import jakarta.resource.spi.ManagedConnectionFactory;
import java.util.Iterator;

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
public abstract class CciAdapter
        extends    com.tangosol.coherence.component.connector.ResourceAdapter
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
        __mapChildren.put("ConnectionFactory", CciAdapter.ConnectionFactory.get_CLASS());
        __mapChildren.put("DefaultConnectionManager", com.tangosol.coherence.component.connector.ResourceAdapter.DefaultConnectionManager.get_CLASS());
        __mapChildren.put("ManagedConnection", CciAdapter.ManagedConnection.get_CLASS());
        }
    
    // Initializing constructor
    public CciAdapter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter".replace('/', '.'));
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
    public boolean equals(Object obj)
        {
        if (obj instanceof CciAdapter)
            {
            CciAdapter that = (CciAdapter) obj;
        
            return this == that
                || this.getMetaData().equals(that.getMetaData());
            }
        return false;
        }
    
    // Accessor for the property "MetaData"
    /**
     * Getter for property MetaData.<p>
     */
    public jakarta.resource.cci.ResourceAdapterMetaData getMetaData()
        {
        return (CciAdapter.AdapterMetaData) _findChild("AdapterMetaData");
        }
    
    // Declared at the super level
    public int hashCode()
        {
        return getMetaData().hashCode();
        }

    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$AdapterMetaData
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class AdapterMetaData
            extends    com.tangosol.coherence.component.Data
            implements jakarta.resource.cci.ResourceAdapterMetaData
        {
        // ---- Fields declarations ----
        
        /**
         * Property AdapterName
         *
         */
        private String __m_AdapterName;
        
        /**
         * Property AdapterShortDescription
         *
         */
        private String __m_AdapterShortDescription;
        
        /**
         * Property AdapterVendorName
         *
         */
        private String __m_AdapterVendorName;
        
        /**
         * Property AdapterVersion
         *
         */
        private String __m_AdapterVersion;
        
        /**
         * Property InteractionSpecsSupported
         *
         */
        private String[] __m_InteractionSpecsSupported;
        
        /**
         * Property SpecVersion
         *
         */
        private String __m_SpecVersion;
        
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
            return false;
            }
        
        // Getter for virtual constant SupportsLocalTransactionDemarcation
        public boolean isSupportsLocalTransactionDemarcation()
            {
            return false;
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.AdapterMetaData();
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
                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$AdapterMetaData".replace('/', '.'));
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
        public boolean equals(Object obj)
            {
            if (obj instanceof CciAdapter.AdapterMetaData)
                {
                CciAdapter.AdapterMetaData that = (CciAdapter.AdapterMetaData) obj;
            
                return this == that
                    || this.getAdapterName()   .equals(that.getAdapterName())
                    && this.getAdapterVersion().equals(that.getAdapterVersion());
                }
            return false;
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        // Accessor for the property "AdapterName"
        /**
         * Getter for property AdapterName.<p>
         */
        public String getAdapterName()
            {
            return __m_AdapterName;
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        // Accessor for the property "AdapterShortDescription"
        /**
         * Getter for property AdapterShortDescription.<p>
         */
        public String getAdapterShortDescription()
            {
            return __m_AdapterShortDescription;
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        // Accessor for the property "AdapterVendorName"
        /**
         * Getter for property AdapterVendorName.<p>
         */
        public String getAdapterVendorName()
            {
            return __m_AdapterVendorName;
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        // Accessor for the property "AdapterVersion"
        /**
         * Getter for property AdapterVersion.<p>
         */
        public String getAdapterVersion()
            {
            return __m_AdapterVersion;
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        // Accessor for the property "InteractionSpecsSupported"
        /**
         * Getter for property InteractionSpecsSupported.<p>
         */
        public String[] getInteractionSpecsSupported()
            {
            return __m_InteractionSpecsSupported;
            }
        
        // Accessor for the property "InteractionSpecsSupported"
        /**
         * Getter for property InteractionSpecsSupported.<p>
         */
        public String getInteractionSpecsSupported(int pIndex)
            {
            return getInteractionSpecsSupported()[pIndex];
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        // Accessor for the property "SpecVersion"
        /**
         * Getter for property SpecVersion.<p>
         */
        public String getSpecVersion()
            {
            return __m_SpecVersion;
            }
        
        // Declared at the super level
        public int hashCode()
            {
            return getAdapterName().hashCode() + getAdapterVersion().hashCode();
            }
        
        // Accessor for the property "AdapterName"
        /**
         * Setter for property AdapterName.<p>
         */
        public void setAdapterName(String pAdapterName)
            {
            __m_AdapterName = pAdapterName;
            }
        
        // Accessor for the property "AdapterShortDescription"
        /**
         * Setter for property AdapterShortDescription.<p>
         */
        public void setAdapterShortDescription(String pAdapterShortDescription)
            {
            __m_AdapterShortDescription = pAdapterShortDescription;
            }
        
        // Accessor for the property "AdapterVendorName"
        /**
         * Setter for property AdapterVendorName.<p>
         */
        public void setAdapterVendorName(String pAdapterVendorName)
            {
            __m_AdapterVendorName = pAdapterVendorName;
            }
        
        // Accessor for the property "AdapterVersion"
        /**
         * Setter for property AdapterVersion.<p>
         */
        public void setAdapterVersion(String pAdapterVersion)
            {
            __m_AdapterVersion = pAdapterVersion;
            }
        
        // Accessor for the property "InteractionSpecsSupported"
        /**
         * Setter for property InteractionSpecsSupported.<p>
         */
        public void setInteractionSpecsSupported(String[] pInteractionSpecsSupported)
            {
            __m_InteractionSpecsSupported = pInteractionSpecsSupported;
            }
        
        // Accessor for the property "InteractionSpecsSupported"
        /**
         * Setter for property InteractionSpecsSupported.<p>
         */
        public void setInteractionSpecsSupported(int pIndex, String pInteractionSpecsSupported)
            {
            getInteractionSpecsSupported()[pIndex] = pInteractionSpecsSupported;
            }
        
        // Accessor for the property "SpecVersion"
        /**
         * Setter for property SpecVersion.<p>
         */
        public void setSpecVersion(String pSpecVersion)
            {
            __m_SpecVersion = pSpecVersion;
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        public boolean supportsExecuteWithInputAndOutputRecord()
            {
            return isSupportsExecuteWithInputAndOutputRecord();
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        public boolean supportsExecuteWithInputRecordOnly()
            {
            return isSupportsExecuteWithInputRecordOnly();
            }
        
        // From interface: jakarta.resource.cci.ResourceAdapterMetaData
        public boolean supportsLocalTransactionDemarcation()
            {
            return isSupportsLocalTransactionDemarcation();
            }
        
        // Declared at the super level
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            
            sb.append("ResourceAdapter=")
              .append(getAdapterName())
              .append(" (")
              .append(getAdapterShortDescription())
              .append("), version=")
              .append(getAdapterVersion())
              .append(", vendor=")
              .append(getAdapterVendorName())
              ;
            
            return sb.toString();
            }
        }

    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static abstract class ConnectionFactory
            extends    com.tangosol.coherence.component.connector.ResourceAdapter.ConnectionFactory
            implements jakarta.resource.cci.ConnectionFactory
        {
        // ---- Fields declarations ----
        
        /**
         * Property Reference
         *
         */
        private javax.naming.Reference __m_Reference;
        
        // Initializing constructor
        public ConnectionFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory".replace('/', '.'));
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
        
        // From interface: jakarta.resource.cci.ConnectionFactory
        public jakarta.resource.cci.Connection getConnection()
                throws jakarta.resource.ResourceException
            {
            return getConnection(null);
            }
        
        // From interface: jakarta.resource.cci.ConnectionFactory
        public jakarta.resource.cci.Connection getConnection(jakarta.resource.cci.ConnectionSpec properties)
                throws jakarta.resource.ResourceException
            {
            // import jakarta.resource.cci.Connection;
            // import jakarta.resource.spi.ConnectionManager;
            // import jakarta.resource.spi.ConnectionRequestInfo;
            // import jakarta.resource.spi.ManagedConnectionFactory;
            // import jakarta.resource.ResourceException;
            
            ManagedConnectionFactory mcf  = getManagedConnectionFactory();
            ConnectionRequestInfo    info = translateConnectionSpec(properties);
            ConnectionManager        mgr  = getConnectionManager();
            Object                   conn = mgr.allocateConnection(mcf, info);
            
            if (conn instanceof Connection)
                {
                return (Connection) conn;
                }
            
            // WL may return a proxy class "$ProxyN" causing the ClassCastException
            throw new ResourceException(
                "allocateConnection() returned an invalid class: " + conn.getClass().getName() +
                "\nConnectionManager=" + mgr +
                "\nManagedConnectionFactory=" + mcf +
                "\nConnectionRequestInfo=" + info +
                "\nconnection=" + conn);
            }
        
        // From interface: jakarta.resource.cci.ConnectionFactory
        public jakarta.resource.cci.ResourceAdapterMetaData getMetaData()
            {
            // import jakarta.resource.cci.ResourceAdapterMetaData;
            
            CciAdapter adapter = (CciAdapter) get_Module();
            return (ResourceAdapterMetaData) adapter.getMetaData();
            }
        
        // From interface: jakarta.resource.cci.ConnectionFactory
        public jakarta.resource.cci.RecordFactory getRecordFactory()
                throws jakarta.resource.ResourceException
            {
            return (CciAdapter.ConnectionFactory.RecordFactory) _findChild("RecordFactory");
            }
        
        // From interface: jakarta.resource.cci.ConnectionFactory
        // Accessor for the property "Reference"
        /**
         * Getter for property Reference.<p>
         */
        public javax.naming.Reference getReference()
                throws javax.naming.NamingException
            {
            return __m_Reference;
            }
        
        // From interface: jakarta.resource.cci.ConnectionFactory
        // Accessor for the property "Reference"
        /**
         * Setter for property Reference.<p>
         */
        public void setReference(javax.naming.Reference reference)
            {
            __m_Reference = reference;
            }
        
        public jakarta.resource.spi.ConnectionRequestInfo translateConnectionSpec(jakarta.resource.cci.ConnectionSpec properties)
            {
            return null;
            }

        // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class RecordFactory
                extends    com.tangosol.coherence.component.Util
                implements jakarta.resource.cci.RecordFactory
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
                __mapChildren.put("IndexedRecord", CciAdapter.ConnectionFactory.RecordFactory.IndexedRecord.get_CLASS());
                __mapChildren.put("MappedRecord", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.get_CLASS());
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
                return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory".replace('/', '.'));
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
            
            // From interface: jakarta.resource.cci.RecordFactory
            public jakarta.resource.cci.IndexedRecord createIndexedRecord(String sRecordName)
                    throws jakarta.resource.ResourceException
                {
                // import com.tangosol.util.SafeLinkedList;
                
                CciAdapter.ConnectionFactory.RecordFactory.IndexedRecord record = (CciAdapter.ConnectionFactory.RecordFactory.IndexedRecord) _newChild("IndexedRecord");
                record.setRecordName(sRecordName);
                record.setList(new SafeLinkedList());
                
                return record;
                }
            
            // From interface: jakarta.resource.cci.RecordFactory
            public jakarta.resource.cci.MappedRecord createMappedRecord(String sRecordName)
                    throws jakarta.resource.ResourceException
                {
                // import com.tangosol.util.SafeHashMap;
                
                CciAdapter.ConnectionFactory.RecordFactory.MappedRecord record = (CciAdapter.ConnectionFactory.RecordFactory.MappedRecord) _newChild("MappedRecord");
                record.setRecordName(sRecordName);
                record.setMap(new SafeHashMap());
                
                return record;
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$IndexedRecord
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class IndexedRecord
                    extends    com.tangosol.coherence.component.util.collections.WrapperList
                    implements jakarta.resource.cci.IndexedRecord
                {
                // ---- Fields declarations ----
                
                /**
                 * Property RecordName
                 *
                 */
                private String __m_RecordName;
                
                /**
                 * Property RecordShortDescription
                 *
                 */
                private String __m_RecordShortDescription;
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
                    __mapChildren.put("Iterator", CciAdapter.ConnectionFactory.RecordFactory.IndexedRecord.Iterator.get_CLASS());
                    }
                
                // Default constructor
                public IndexedRecord()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public IndexedRecord(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.IndexedRecord();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$IndexedRecord".replace('/', '.'));
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
                
                // From interface: jakarta.resource.cci.IndexedRecord
                // Declared at the super level
                public Object clone()
                        throws java.lang.CloneNotSupportedException
                    {
                    throw new CloneNotSupportedException();
                    }
                
                // From interface: jakarta.resource.cci.IndexedRecord
                // Accessor for the property "RecordName"
                /**
                 * Getter for property RecordName.<p>
                 */
                public String getRecordName()
                    {
                    return __m_RecordName;
                    }
                
                // From interface: jakarta.resource.cci.IndexedRecord
                // Accessor for the property "RecordShortDescription"
                /**
                 * Getter for property RecordShortDescription.<p>
                 */
                public String getRecordShortDescription()
                    {
                    return __m_RecordShortDescription;
                    }
                
                // From interface: jakarta.resource.cci.IndexedRecord
                // Accessor for the property "RecordName"
                /**
                 * Setter for property RecordName.<p>
                 */
                public void setRecordName(String sName)
                    {
                    __m_RecordName = sName;
                    }
                
                // From interface: jakarta.resource.cci.IndexedRecord
                // Accessor for the property "RecordShortDescription"
                /**
                 * Setter for property RecordShortDescription.<p>
                 */
                public void setRecordShortDescription(String sDescr)
                    {
                    __m_RecordShortDescription = sDescr;
                    }

                // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$IndexedRecord$Iterator
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Iterator
                        extends    com.tangosol.coherence.component.util.collections.WrapperList.Iterator
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Iterator()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.IndexedRecord.Iterator();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$IndexedRecord$Iterator".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class MappedRecord
                    extends    com.tangosol.coherence.component.util.collections.WrapperMap
                    implements jakarta.resource.cci.MappedRecord
                {
                // ---- Fields declarations ----
                
                /**
                 * Property RecordName
                 *
                 */
                private String __m_RecordName;
                
                /**
                 * Property RecordShortDescription
                 *
                 */
                private String __m_RecordShortDescription;
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
                    __mapChildren.put("EntrySet", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.EntrySet.get_CLASS());
                    __mapChildren.put("KeySet", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.KeySet.get_CLASS());
                    __mapChildren.put("Values", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.Values.get_CLASS());
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord".replace('/', '.'));
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
                
                // From interface: jakarta.resource.cci.MappedRecord
                // Declared at the super level
                public Object clone()
                        throws java.lang.CloneNotSupportedException
                    {
                    throw new CloneNotSupportedException();
                    }
                
                // From interface: jakarta.resource.cci.MappedRecord
                // Accessor for the property "RecordName"
                /**
                 * Getter for property RecordName.<p>
                 */
                public String getRecordName()
                    {
                    return __m_RecordName;
                    }
                
                // From interface: jakarta.resource.cci.MappedRecord
                // Accessor for the property "RecordShortDescription"
                /**
                 * Getter for property RecordShortDescription.<p>
                 */
                public String getRecordShortDescription()
                    {
                    return __m_RecordShortDescription;
                    }
                
                // From interface: jakarta.resource.cci.MappedRecord
                // Accessor for the property "RecordName"
                /**
                 * Setter for property RecordName.<p>
                 */
                public void setRecordName(String sName)
                    {
                    __m_RecordName = sName;
                    }
                
                // From interface: jakarta.resource.cci.MappedRecord
                // Accessor for the property "RecordShortDescription"
                /**
                 * Setter for property RecordShortDescription.<p>
                 */
                public void setRecordShortDescription(String sDescr)
                    {
                    __m_RecordShortDescription = sDescr;
                    }

                // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$EntrySet
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class EntrySet
                        extends    com.tangosol.coherence.component.util.collections.WrapperMap.EntrySet
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
                        __mapChildren.put("Entry", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.EntrySet.Entry.get_CLASS());
                        __mapChildren.put("Iterator", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.EntrySet.Iterator.get_CLASS());
                        }
                    
                    // Default constructor
                    public EntrySet()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public EntrySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.EntrySet();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$EntrySet".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
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

                    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$EntrySet$Entry
                    
                    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                    public static class Entry
                            extends    com.tangosol.coherence.component.util.collections.WrapperMap.EntrySet.Entry
                        {
                        // ---- Fields declarations ----
                        
                        // Default constructor
                        public Entry()
                            {
                            this(null, null, true);
                            }
                        
                        // Initializing constructor
                        public Entry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                            return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.EntrySet.Entry();
                            }
                        
                        //++ getter for static property _CLASS
                        /**
                         * Getter for property _CLASS.<p>
                        * Property with auto-generated accessor that returns
                        * the Class object for a given component.
                         */
                        public static Class get_CLASS()
                            {
                            Class clz;
                            try
                                {
                                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$EntrySet$Entry".replace('/', '.'));
                                }
                            catch (ClassNotFoundException e)
                                {
                                throw new NoClassDefFoundError(e.getMessage());
                                }
                            return clz;
                            }
                        
                        //++ getter for autogen property _Module
                        /**
                         * This is an auto-generated method that returns the
                        * global [design time] parent component.
                        * 
                        * Note: the class generator will ignore any custom
                        * implementation for this behavior.
                         */
                        private com.tangosol.coherence.Component get_Module()
                            {
                            return this.get_Parent().get_Parent().get_Parent().get_Parent().get_Parent();
                            }
                        }

                    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$EntrySet$Iterator
                    
                    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                    public static class Iterator
                            extends    com.tangosol.coherence.component.util.collections.WrapperMap.EntrySet.Iterator
                        {
                        // ---- Fields declarations ----
                        
                        // Default constructor
                        public Iterator()
                            {
                            this(null, null, true);
                            }
                        
                        // Initializing constructor
                        public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                            return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.EntrySet.Iterator();
                            }
                        
                        //++ getter for static property _CLASS
                        /**
                         * Getter for property _CLASS.<p>
                        * Property with auto-generated accessor that returns
                        * the Class object for a given component.
                         */
                        public static Class get_CLASS()
                            {
                            Class clz;
                            try
                                {
                                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$EntrySet$Iterator".replace('/', '.'));
                                }
                            catch (ClassNotFoundException e)
                                {
                                throw new NoClassDefFoundError(e.getMessage());
                                }
                            return clz;
                            }
                        
                        //++ getter for autogen property _Module
                        /**
                         * This is an auto-generated method that returns the
                        * global [design time] parent component.
                        * 
                        * Note: the class generator will ignore any custom
                        * implementation for this behavior.
                         */
                        private com.tangosol.coherence.Component get_Module()
                            {
                            return this.get_Parent().get_Parent().get_Parent().get_Parent().get_Parent();
                            }
                        }
                    }

                // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$KeySet
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class KeySet
                        extends    com.tangosol.coherence.component.util.collections.WrapperMap.KeySet
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
                        __mapChildren.put("Iterator", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.KeySet.Iterator.get_CLASS());
                        }
                    
                    // Default constructor
                    public KeySet()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public KeySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.KeySet();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$KeySet".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
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

                    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$KeySet$Iterator
                    
                    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                    public static class Iterator
                            extends    com.tangosol.coherence.component.util.collections.WrapperMap.KeySet.Iterator
                        {
                        // ---- Fields declarations ----
                        
                        // Default constructor
                        public Iterator()
                            {
                            this(null, null, true);
                            }
                        
                        // Initializing constructor
                        public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                            return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.KeySet.Iterator();
                            }
                        
                        //++ getter for static property _CLASS
                        /**
                         * Getter for property _CLASS.<p>
                        * Property with auto-generated accessor that returns
                        * the Class object for a given component.
                         */
                        public static Class get_CLASS()
                            {
                            Class clz;
                            try
                                {
                                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$KeySet$Iterator".replace('/', '.'));
                                }
                            catch (ClassNotFoundException e)
                                {
                                throw new NoClassDefFoundError(e.getMessage());
                                }
                            return clz;
                            }
                        
                        //++ getter for autogen property _Module
                        /**
                         * This is an auto-generated method that returns the
                        * global [design time] parent component.
                        * 
                        * Note: the class generator will ignore any custom
                        * implementation for this behavior.
                         */
                        private com.tangosol.coherence.Component get_Module()
                            {
                            return this.get_Parent().get_Parent().get_Parent().get_Parent().get_Parent();
                            }
                        }
                    }

                // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$Values
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Values
                        extends    com.tangosol.coherence.component.util.collections.WrapperMap.Values
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
                        __mapChildren.put("Iterator", CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.Values.Iterator.get_CLASS());
                        }
                    
                    // Default constructor
                    public Values()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Values(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.Values();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$Values".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
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

                    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$Values$Iterator
                    
                    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                    public static class Iterator
                            extends    com.tangosol.coherence.component.util.collections.WrapperMap.Values.Iterator
                        {
                        // ---- Fields declarations ----
                        
                        // Default constructor
                        public Iterator()
                            {
                            this(null, null, true);
                            }
                        
                        // Initializing constructor
                        public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                            return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ConnectionFactory.RecordFactory.MappedRecord.Values.Iterator();
                            }
                        
                        //++ getter for static property _CLASS
                        /**
                         * Getter for property _CLASS.<p>
                        * Property with auto-generated accessor that returns
                        * the Class object for a given component.
                         */
                        public static Class get_CLASS()
                            {
                            Class clz;
                            try
                                {
                                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ConnectionFactory$RecordFactory$MappedRecord$Values$Iterator".replace('/', '.'));
                                }
                            catch (ClassNotFoundException e)
                                {
                                throw new NoClassDefFoundError(e.getMessage());
                                }
                            return clz;
                            }
                        
                        //++ getter for autogen property _Module
                        /**
                         * This is an auto-generated method that returns the
                        * global [design time] parent component.
                        * 
                        * Note: the class generator will ignore any custom
                        * implementation for this behavior.
                         */
                        private com.tangosol.coherence.Component get_Module()
                            {
                            return this.get_Parent().get_Parent().get_Parent().get_Parent().get_Parent();
                            }
                        }
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ManagedConnection
    
    /**
     * ManagedConnection represents a physical connection to
     * an underlying EIS (Chapter 5.5.4 of JCA 1.0 specification)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static abstract class ManagedConnection
            extends    com.tangosol.coherence.component.connector.ResourceAdapter.ManagedConnection
        {
        // ---- Fields declarations ----
        
        /**
         * Property ConnectionSet
         *
         */
        private transient java.util.Set __m_ConnectionSet;
        
        /**
         * Property CurrentTransaction
         *
         * ThreadLocal object holding a LocalTransaction associated with
         * current thread.
         */
        private transient ThreadLocal __m_CurrentTransaction;
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
            __mapChildren.put("Connection", CciAdapter.ManagedConnection.Connection.get_CLASS());
            __mapChildren.put("LocalTransaction", CciAdapter.ManagedConnection.LocalTransaction.get_CLASS());
            __mapChildren.put("XAResource", com.tangosol.coherence.component.connector.ResourceAdapter.ManagedConnection.XAResource.get_CLASS());
            }
        
        // Initializing constructor
        public ManagedConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ManagedConnection".replace('/', '.'));
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
         * See J2CA spec chapter 6.11
        * 
        * "The associateConnection method implementation for a
        * ManagedConnection should dissociate the connection handle (passed as
        * a parameter) from its currently associated ManagedConnection and
        * associate the new connection handle with itself. "
         */
        public void associateConnection(Object connection)
                throws jakarta.resource.ResourceException
            {
            checkStatus();
            
            if (connection instanceof CciAdapter.ManagedConnection.Connection)
                {
                CciAdapter.ManagedConnection.Connection con = (CciAdapter.ManagedConnection.Connection) connection;
            
                CciAdapter.ManagedConnection mcCurrent = con.getManagedConnection();
                if (mcCurrent == null)
                    {
                    String  sMsg    = "CoherenceRA: associateConnection(): Missing associated ManagedConnection";
                    Boolean fStrict = ((CciAdapter) get_Module()).getStrict();
                    if (fStrict != null && fStrict.booleanValue())
                        {
                        throw new IllegalStateException(sMsg);
                        }
                    else
                        {
                        log(sMsg, 1);
                        }
                    }
                else
                    {
                    // dissociate connection with current managed connection
                    mcCurrent.unregisterConnection(con);
                    }
            
                // associate connection with new managed connection
                registerConnection(con);
                }
            else
                {
                throw new IllegalStateException("Invalid connection object: " + connection);
                }
            }
        
        // Declared at the super level
        public void cleanup()
                throws jakarta.resource.ResourceException
            {
            unregisterAllConnections();
            }
        
        // Declared at the super level
        public void destroy()
                throws jakarta.resource.ResourceException
            {
            unregisterAllConnections();
            }
        
        // Declared at the super level
        public Object getConnection(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
                throws jakarta.resource.ResourceException
            {
            // import jakarta.resource.cci.Connection;
            // import jakarta.resource.spi.ConnectionRequestInfo;
            
            checkStatus();
            
            verifyAuthentication(subject, cxInfo);
            
            CciAdapter.ManagedConnection.Connection con = (CciAdapter.ManagedConnection.Connection) _newChild("Connection");
            registerConnection(con);
            
            return con;
            }
        
        // Accessor for the property "ConnectionSet"
        /**
         * Getter for property ConnectionSet.<p>
         */
        protected java.util.Set getConnectionSet()
            {
            return __m_ConnectionSet;
            }
        
        // Accessor for the property "CurrentTransaction"
        /**
         * Getter for property CurrentTransaction.<p>
        * ThreadLocal object holding a LocalTransaction associated with current
        * thread.
         */
        public ThreadLocal getCurrentTransaction()
            {
            return __m_CurrentTransaction;
            }
        
        // Declared at the super level
        public jakarta.resource.spi.LocalTransaction getLocalTransaction()
                throws jakarta.resource.ResourceException
            {
            // import jakarta.resource.spi.LocalTransaction as jakarta.resource.spi.LocalTransaction;
            
            jakarta.resource.spi.LocalTransaction txCurrent = (jakarta.resource.spi.LocalTransaction) getCurrentTransaction().get();
            return txCurrent == null ? super.getLocalTransaction() : txCurrent;
            }
        
        // Declared at the super level
        /**
         * Checks whether or not this connection object matches to the specified
        * security info (subject) and connection request information (info)
         */
        public synchronized boolean matches(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
            {
            // Section 5.5.4 of JCA 1.0 specification states:
            // "To avoid any unexpected matching behavior, the application server
            // should not pass a ManagedConnection instance with existing connection
            // handles to the matchManagedConnections method as part of a candidate set"
            
            return getConnectionSet().isEmpty() && super.matches(subject, cxInfo);
            }
        
        public synchronized void registerConnection(jakarta.resource.cci.Connection con)
            {
            if (con instanceof CciAdapter.ManagedConnection.Connection)
                {
                ((CciAdapter.ManagedConnection.Connection) con).setManagedConnection(this);
                }
            
            getConnectionSet().add(con);
            }
        
        /**
         * Register the transaction assosiated with the calling thread
         */
        public void registerTransaction(jakarta.resource.spi.LocalTransaction tx)
                throws jakarta.resource.ResourceException
            {
            // import jakarta.resource.spi.LocalTransaction as jakarta.resource.spi.LocalTransaction;
            // import jakarta.resource.spi.LocalTransactionException;
            
            ThreadLocal      tlo       = getCurrentTransaction();
            jakarta.resource.spi.LocalTransaction txCurrent = (jakarta.resource.spi.LocalTransaction) tlo.get();
            
            if (txCurrent == null)
                {
                tlo.set(tx);
                }
            else if (tx != txCurrent)
                {
                throw new LocalTransactionException(
                    "Register called with invalid transaction context: " + tx +
                    "; current context: " + txCurrent);
                }
            }
        
        // Accessor for the property "ConnectionSet"
        /**
         * Setter for property ConnectionSet.<p>
         */
        protected void setConnectionSet(java.util.Set set)
            {
            __m_ConnectionSet = set;
            }
        
        // Accessor for the property "CurrentTransaction"
        /**
         * Setter for property CurrentTransaction.<p>
        * ThreadLocal object holding a LocalTransaction associated with current
        * thread.
         */
        protected void setCurrentTransaction(ThreadLocal tlo)
            {
            __m_CurrentTransaction = tlo;
            }
        
        protected void unregisterAllConnections()
                throws jakarta.resource.ResourceException
            {
            // import com.tangosol.util.SimpleEnumerator;
            // import java.util.Iterator;
            // import jakarta.resource.cci.Connection;
            // import jakarta.resource.spi.LocalTransaction as jakarta.resource.spi.LocalTransaction;
            
            ThreadLocal      tlo = getCurrentTransaction();
            jakarta.resource.spi.LocalTransaction tx  = (jakarta.resource.spi.LocalTransaction) tlo.get();
            
            if (tx != null)
                {
                log("CoherenceRA: jakarta.resource.spi.LocalTransaction is not completed: " + tx, 2);
                tlo.set(null);
                }
            
            for (Iterator iter = new SimpleEnumerator(
                    getConnectionSet().toArray()); iter.hasNext();)
                {
                unregisterConnection((Connection) iter.next());
                }
            }
        
        public synchronized void unregisterConnection(jakarta.resource.cci.Connection con)
            {
            getConnectionSet().remove(con);
            
            if (con instanceof CciAdapter.ManagedConnection.Connection)
                {
                ((CciAdapter.ManagedConnection.Connection) con).setManagedConnection(null);
                }
            }
        
        /**
         * Register the transaction assosiated with the calling thread
         */
        public void unregisterTransaction(jakarta.resource.spi.LocalTransaction tx)
                throws jakarta.resource.ResourceException
            {
            // import jakarta.resource.spi.LocalTransaction as jakarta.resource.spi.LocalTransaction;
            // import jakarta.resource.spi.LocalTransactionException;
            
            ThreadLocal      tlo       = getCurrentTransaction();
            jakarta.resource.spi.LocalTransaction txCurrent = (jakarta.resource.spi.LocalTransaction) tlo.get();
            if (txCurrent == tx)
                {
                tlo.set(null);
                }
            else if (txCurrent != null)
                {
                tlo.set(null);
                throw new LocalTransactionException(
                    "Unregister called with invalid transaction context: " + tx +
                    "; current context: " + txCurrent);
                }
            }
        
        /**
         * Verify the connection subject and info against this MC subject and
        * info.
        * 
        * @see #getConnection
         */
        public void verifyAuthentication(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
                throws jakarta.resource.ResourceException
            {
            }

        // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ManagedConnection$Connection
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Connection
                extends    com.tangosol.coherence.component.Connector
                implements jakarta.resource.cci.Connection
            {
            // ---- Fields declarations ----
            
            /**
             * Property ManagedConnection
             *
             */
            private CciAdapter.ManagedConnection __m_ManagedConnection;
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
                __mapChildren.put("Interaction", CciAdapter.ManagedConnection.Connection.Interaction.get_CLASS());
                __mapChildren.put("UserTransaction", CciAdapter.ManagedConnection.Connection.UserTransaction.get_CLASS());
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
                _addChild(new CciAdapter.ManagedConnection.Connection.ConnectionMetaData("ConnectionMetaData", this, true), "ConnectionMetaData");
                _addChild(new CciAdapter.ManagedConnection.Connection.ResultSetInfo("ResultSetInfo", this, true), "ResultSetInfo");
                
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
                return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ManagedConnection$Connection".replace('/', '.'));
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
            
            // From interface: jakarta.resource.cci.Connection
            public void close()
                    throws jakarta.resource.ResourceException
                {
                // import jakarta.resource.spi.ConnectionEvent;
                
                CciAdapter.ManagedConnection mc = getManagedConnection();
                if (mc != null)
                    {
                    mc.fireConnectionEvent(ConnectionEvent.CONNECTION_CLOSED, null, this);
                    mc.unregisterConnection(this);
                    }
                }
            
            // From interface: jakarta.resource.cci.Connection
            public jakarta.resource.cci.Interaction createInteraction()
                    throws jakarta.resource.ResourceException
                {
                return (CciAdapter.ManagedConnection.Connection.Interaction) _newChild("Interaction");
                }
            
            // From interface: jakarta.resource.cci.Connection
            public jakarta.resource.cci.LocalTransaction getLocalTransaction()
                    throws jakarta.resource.ResourceException
                {
                CciAdapter.ManagedConnection.Connection.UserTransaction tx = (CciAdapter.ManagedConnection.Connection.UserTransaction) _newChild("UserTransaction");
                
                tx.setManagedConnection(getManagedConnection());
                
                return tx;
                }
            
            // Accessor for the property "ManagedConnection"
            /**
             * Getter for property ManagedConnection.<p>
             */
            public CciAdapter.ManagedConnection getManagedConnection()
                {
                return __m_ManagedConnection;
                }
            
            // From interface: jakarta.resource.cci.Connection
            public jakarta.resource.cci.ConnectionMetaData getMetaData()
                {
                return (CciAdapter.ManagedConnection.Connection.ConnectionMetaData) _findChild("ConnectionMetaData");
                }
            
            // From interface: jakarta.resource.cci.Connection
            public jakarta.resource.cci.ResultSetInfo getResultSetInfo()
                    throws jakarta.resource.ResourceException
                {
                return (CciAdapter.ManagedConnection.Connection.ResultSetInfo) _findChild("ResultSetInfo");
                }
            
            // Accessor for the property "ManagedConnection"
            /**
             * Setter for property ManagedConnection.<p>
             */
            public void setManagedConnection(CciAdapter.ManagedConnection mc)
                {
                __m_ManagedConnection = mc;
                }
            
            // Declared at the super level
            public String toString()
                {
                return get_Name() + "@" + hashCode();
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ManagedConnection$Connection$ConnectionMetaData
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class ConnectionMetaData
                    extends    com.tangosol.coherence.component.Data
                    implements jakarta.resource.cci.ConnectionMetaData
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public ConnectionMetaData()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public ConnectionMetaData(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.ConnectionMetaData();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ManagedConnection$Connection$ConnectionMetaData".replace('/', '.'));
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
                public boolean equals(Object obj)
                    {
                    // import jakarta.resource.ResourceException;
                    
                    try
                        {
                        if (obj instanceof CciAdapter.ManagedConnection.Connection.ConnectionMetaData)
                            {
                            CciAdapter.ManagedConnection.Connection.ConnectionMetaData that = (CciAdapter.ManagedConnection.Connection.ConnectionMetaData) obj;
                    
                            return this == that
                                || this.getEISProductName()   .equals(that.getEISProductName())
                                && this.getEISProductVersion().equals(that.getEISProductVersion());
                            }
                        }
                    catch (ResourceException e) {}
                    
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ConnectionMetaData
                // Accessor for the property "EISProductName"
                /**
                 * Getter for property EISProductName.<p>
                 */
                public String getEISProductName()
                        throws jakarta.resource.ResourceException
                    {
                    return getManagedConnection().getMetaData().getEISProductName();
                    }
                
                // From interface: jakarta.resource.cci.ConnectionMetaData
                // Accessor for the property "EISProductVersion"
                /**
                 * Getter for property EISProductVersion.<p>
                 */
                public String getEISProductVersion()
                        throws jakarta.resource.ResourceException
                    {
                    return getManagedConnection().getMetaData().getEISProductVersion();
                    }
                
                // Accessor for the property "ManagedConnection"
                /**
                 * Getter for property ManagedConnection.<p>
                 */
                protected CciAdapter.ManagedConnection getManagedConnection()
                        throws jakarta.resource.ResourceException
                    {
                    // import jakarta.resource.ResourceException;
                    
                    CciAdapter.ManagedConnection.Connection        con = (CciAdapter.ManagedConnection.Connection) get_Parent();
                    CciAdapter.ManagedConnection mc  = (CciAdapter.ManagedConnection) con.getManagedConnection();
                    
                    if (mc == null)
                        {
                        mc = (CciAdapter.ManagedConnection) con.get_Parent();
                        }
                    
                    if (mc == null)
                        {
                        throw new ResourceException("Invalid ManagedConnection: " + con);
                        }
                    
                    return mc;
                    }
                
                // From interface: jakarta.resource.cci.ConnectionMetaData
                // Accessor for the property "UserName"
                /**
                 * Getter for property UserName.<p>
                 */
                public String getUserName()
                        throws jakarta.resource.ResourceException
                    {
                    // import Component.Connector.ConnectionInfo;
                    // import jakarta.resource.spi.ConnectionRequestInfo;
                    
                    CciAdapter.ManagedConnection mc = getManagedConnection();
                    
                    ConnectionRequestInfo cxInfo = mc.getConnectionInfo();
                    return cxInfo instanceof ConnectionInfo ?
                        ((ConnectionInfo) cxInfo).getUserName() : null;
                    }
                
                // Declared at the super level
                public int hashCode()
                    {
                    // import jakarta.resource.ResourceException;
                    
                    try
                        {
                        return getEISProductName().hashCode() + getEISProductVersion().hashCode();
                        }
                    catch (ResourceException e)
                        {
                        return 0;
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ManagedConnection$Connection$Interaction
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Interaction
                    extends    com.tangosol.coherence.component.connector.Interaction
                {
                // ---- Fields declarations ----
                
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.Interaction();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ManagedConnection$Connection$Interaction".replace('/', '.'));
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
                /**
                 * Getter for property Connection.<p>
                 */
                public jakarta.resource.cci.Connection getConnection()
                    {
                    // import jakarta.resource.cci.Connection;
                    
                    return (Connection) get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ManagedConnection$Connection$ResultSetInfo
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class ResultSetInfo
                    extends    com.tangosol.coherence.component.Data
                    implements jakarta.resource.cci.ResultSetInfo
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public ResultSetInfo()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public ResultSetInfo(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.ResultSetInfo();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ManagedConnection$Connection$ResultSetInfo".replace('/', '.'));
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
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean deletesAreDetected(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean insertsAreDetected(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean othersDeletesAreVisible(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean othersInsertsAreVisible(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean othersUpdatesAreVisible(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean ownDeletesAreVisible(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean ownInsertsAreVisible(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean ownUpdatesAreVisible(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean supportsResultSetType(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean supportsResultTypeConcurrency(int iType, int iConcurrency)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                
                // From interface: jakarta.resource.cci.ResultSetInfo
                public boolean updatesAreDetected(int iType)
                        throws jakarta.resource.ResourceException
                    {
                    return false;
                    }
                }

            // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ManagedConnection$Connection$UserTransaction
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class UserTransaction
                    extends    com.tangosol.coherence.component.Connector
                    implements jakarta.resource.cci.LocalTransaction
                {
                // ---- Fields declarations ----
                
                /**
                 * Property ManagedConnection
                 *
                 */
                private CciAdapter.ManagedConnection __m_ManagedConnection;
                
                // Default constructor
                public UserTransaction()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public UserTransaction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.Connection.UserTransaction();
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
                        clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ManagedConnection$Connection$UserTransaction".replace('/', '.'));
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
                
                // From interface: jakarta.resource.cci.LocalTransaction
                public void begin()
                        throws jakarta.resource.ResourceException
                    {
                    // import jakarta.resource.spi.ConnectionEvent;
                    
                    getManagedConnection().fireConnectionEvent(
                        ConnectionEvent.LOCAL_TRANSACTION_STARTED);
                    }
                
                // From interface: jakarta.resource.cci.LocalTransaction
                public void commit()
                        throws jakarta.resource.ResourceException
                    {
                    // import jakarta.resource.spi.ConnectionEvent;
                    
                    getManagedConnection().fireConnectionEvent(
                        ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
                    }
                
                // Accessor for the property "ManagedConnection"
                /**
                 * Getter for property ManagedConnection.<p>
                 */
                public CciAdapter.ManagedConnection getManagedConnection()
                    {
                    return __m_ManagedConnection;
                    }
                
                // From interface: jakarta.resource.cci.LocalTransaction
                public void rollback()
                        throws jakarta.resource.ResourceException
                    {
                    // import jakarta.resource.spi.ConnectionEvent;
                    
                    getManagedConnection().fireConnectionEvent(
                        ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
                    }
                
                // Accessor for the property "ManagedConnection"
                /**
                 * Setter for property ManagedConnection.<p>
                 */
                public void setManagedConnection(CciAdapter.ManagedConnection mc)
                    {
                    __m_ManagedConnection = mc;
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter$ManagedConnection$LocalTransaction
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class LocalTransaction
                extends    com.tangosol.coherence.component.connector.ResourceAdapter.ManagedConnection.LocalTransaction
            {
            // ---- Fields declarations ----
            
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
                return new com.tangosol.coherence.component.connector.resourceAdapter.CciAdapter.ManagedConnection.LocalTransaction();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/resourceAdapter/CciAdapter$ManagedConnection$LocalTransaction".replace('/', '.'));
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
            public void begin()
                    throws jakarta.resource.ResourceException
                {
                ((CciAdapter.ManagedConnection) getManagedConnection()).registerTransaction(this);
                }
            
            // Declared at the super level
            public void commit()
                    throws jakarta.resource.ResourceException
                {
                ((CciAdapter.ManagedConnection) getManagedConnection()).unregisterTransaction(this);
                }
            
            // Declared at the super level
            public void rollback()
                    throws jakarta.resource.ResourceException
                {
                ((CciAdapter.ManagedConnection) getManagedConnection()).unregisterTransaction(this);
                }
            
            // Declared at the super level
            public String toString()
                {
                return super.toString() + '@' + String.valueOf(hashCode());
                }
            }
        }
    }
