
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.MetaData

package com.tangosol.coherence.component.connector;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MetaData
        extends    com.tangosol.coherence.component.Connector
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
    public MetaData()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MetaData(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.connector.MetaData();
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
            clz = Class.forName("com.tangosol.coherence/component/connector/MetaData".replace('/', '.'));
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
    }
