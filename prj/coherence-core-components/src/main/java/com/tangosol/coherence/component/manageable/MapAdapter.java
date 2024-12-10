
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.MapAdapter

package com.tangosol.coherence.component.manageable;

import java.util.Iterator;
import java.util.Map;
import javax.management.MBeanException;

/**
 * MapAdapter is a DynamicMBean implementation that is driven by two maps: 
 * - Map<String, String> keyed by the attribute names with values being
 * attribute descriptions;
 * - Map<String, Object> keyed by the attribute names with values being
 * attribute values.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MapAdapter
        extends    com.tangosol.coherence.component.Manageable
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Descriptions
     *
     * Map<String, String> keyed by the attribute names with values being
     * attribute descriptions
     */
    private java.util.Map __m__Descriptions;
    
    /**
     * Property _Values
     *
     * Map<String, Object> keyed by the attribute names with values being
     * attribute values
     */
    private java.util.Map __m__Values;
    
    // Default constructor
    public MapAdapter()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MapAdapter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.MapAdapter();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/MapAdapter".replace('/', '.'));
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
    private Object[] get_ComponentInfo$Default()
        {
        return new Object[]
            {
            "MapAdapter is a DynamicMBean implementation that is driven by two maps: \n- Map<String, String> keyed by the attribute names with values being attribute descriptions;\n- Map<String, Object> keyed by the attribute names with values being attribute values.",
            null,
            };
        }
    /**
     * Auto-generated for concrete Components.
     */
    protected Object[] get_ComponentInfo()
        {
        return new Object[] {"Map-driven MBean"};
        }
    private java.util.Map get_PropertyInfo$Default()
        {
        java.util.Map mapInfo = super.get_PropertyInfo();
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Properties that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - at least one public accessor
     */
    protected java.util.Map get_PropertyInfo()
        {
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        Map mapInfo  = get_PropertyInfo$Default();
        Map mapDescr = get_Descriptions();
        
        for (Iterator iter = mapDescr.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entryInfo = (java.util.Map.Entry) iter.next();
        
            String sName  = (String) entryInfo.getKey();
            String sDescr = (String) entryInfo.getValue();
        
            if (sDescr == null)
                {
                sDescr = "";
                }
        
            Object[] aoInfo = new Object[]
                {
                sDescr,
                "get" + sName,
                null,
                "Ljava/lang/String;",
                };
            mapInfo.put(sName, aoInfo);
            }
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Behaviors that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - public access
     */
    protected java.util.Map get_MethodInfo()
        {
        java.util.Map mapInfo = super.get_MethodInfo();
        
        return mapInfo;
        }
    
    /**
     * Initialize the MapAdapter with the corresponding maps.
    * 
    * @param mapDescr  Map<String, String> keyed by the attribute names with
    * values being attribute descriptions
    * @param mapValue  Map<String, String> keyed by the attribute names with
    * values being attribute values
     */
    public void _initialize(java.util.Map mapDescr, java.util.Map mapValue)
        {
        set_Descriptions(mapDescr);
        set_Values(mapValue);
        }
    
    // Accessor for the property "_Descriptions"
    /**
     * Getter for property _Descriptions.<p>
    * Map<String, String> keyed by the attribute names with values being
    * attribute descriptions
     */
    public java.util.Map get_Descriptions()
        {
        return __m__Descriptions;
        }
    
    // Accessor for the property "_Values"
    /**
     * Getter for property _Values.<p>
    * Map<String, Object> keyed by the attribute names with values being
    * attribute values
     */
    public java.util.Map get_Values()
        {
        return __m__Values;
        }
    
    // Declared at the super level
    public Object getAttribute(String sName)
            throws javax.management.AttributeNotFoundException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        return String.valueOf(get_Values().get(sName));
        }
    
    // Declared at the super level
    public Object invoke(String sName, Object[] aoParam, String[] asSignature)
            throws javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import javax.management.MBeanException;
        
        throw new MBeanException(null, "Unsupported operation");
        }
    
    // Accessor for the property "_Descriptions"
    /**
     * Setter for property _Descriptions.<p>
    * Map<String, String> keyed by the attribute names with values being
    * attribute descriptions
     */
    protected void set_Descriptions(java.util.Map mapDescriptions)
        {
        __m__Descriptions = mapDescriptions;
        }
    
    // Accessor for the property "_Values"
    /**
     * Setter for property _Values.<p>
    * Map<String, Object> keyed by the attribute names with values being
    * attribute values
     */
    protected void set_Values(java.util.Map mapDescriptions)
        {
        __m__Values = mapDescriptions;
        }
    
    // Declared at the super level
    public void setAttribute(javax.management.Attribute attribute)
            throws javax.management.AttributeNotFoundException,
                   javax.management.InvalidAttributeValueException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import javax.management.MBeanException;
        
        throw new MBeanException(null, "Unsupported operation");
        }
    }
