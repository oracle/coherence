
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.cacheHandler.CatalogHandler

package com.tangosol.coherence.component.util.cacheHandler;

import com.tangosol.run.xml.XmlElement;

/**
 * CacheHandler represents a named Cache handled by the ReplicatedCache
 * service. During creation each handler is assigned an index that could be
 * used to obtain this handler out of the indexed property "CacheHandler"
 * maintained by the ReplicatedCache service. For the same index there could be
 * a list of handlers that differ only by the value of ClassLoader property.
 * The NextHandler property is used to maintain this list.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CatalogHandler
        extends    com.tangosol.coherence.component.util.CacheHandler
        implements com.tangosol.util.MapListener
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
        __mapChildren.put("BackingMapListener", com.tangosol.coherence.component.util.CacheHandler.BackingMapListener.get_CLASS());
        __mapChildren.put("EntrySet", com.tangosol.coherence.component.util.CacheHandler.EntrySet.get_CLASS());
        __mapChildren.put("KeySet", com.tangosol.coherence.component.util.CacheHandler.KeySet.get_CLASS());
        __mapChildren.put("Validator", com.tangosol.coherence.component.util.CacheHandler.Validator.get_CLASS());
        }
    
    // Default constructor
    public CatalogHandler()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CatalogHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setCacheIndex(0);
            setCacheName("$$$");
            setDeactivationListeners(new com.tangosol.util.Listeners());
            setIgnoreKey(new java.lang.Object());
            setLeaseMap(new com.tangosol.util.SafeHashMap());
            setPutExpiryWarned(false);
            setResourceMap(new com.tangosol.util.SafeHashMap());
            setStandardLeaseMillis(0L);
            setUseEventDaemon(false);
            setValid(true);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
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
        return new com.tangosol.coherence.component.util.cacheHandler.CatalogHandler();
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
            clz = Class.forName("com.tangosol.coherence/component/util/cacheHandler/CatalogHandler".replace('/', '.'));
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
    
    // From interface: com.tangosol.util.MapListener
    public void entryDeleted(com.tangosol.util.MapEvent e)
        {
        // import com.tangosol.run.xml.XmlElement;
        
        // the Catalog has been updated: a cache has to be destroyed
        XmlElement xml = (XmlElement) e.getOldValue();
        if (xml != null)
            {
            getService().onCatalogRemove(xml);
            }
        }
    
    // From interface: com.tangosol.util.MapListener
    public void entryInserted(com.tangosol.util.MapEvent e)
        {
        // import com.tangosol.run.xml.XmlElement;
        
        XmlElement xml = (XmlElement) e.getNewValue();
        if (xml != null)
            {
            getService().onCatalogUpdate(xml);
            }
        }
    
    // From interface: com.tangosol.util.MapListener
    public void entryUpdated(com.tangosol.util.MapEvent e)
        {
        // import com.tangosol.run.xml.XmlElement;
        
        XmlElement xml = (XmlElement) e.getNewValue();
        if (xml != null)
            {
            getService().onCatalogUpdate(xml);
            }
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
        super.onInit();
        
        addMapListener(this);
        }
    }
