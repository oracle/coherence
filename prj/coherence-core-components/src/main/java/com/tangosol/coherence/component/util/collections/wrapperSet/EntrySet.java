
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet

package com.tangosol.coherence.component.util.collections.wrapperSet;

import com.tangosol.coherence.Component;
import java.util.Map;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class EntrySet
        extends    com.tangosol.coherence.component.util.collections.WrapperSet
    {
    // ---- Fields declarations ----
    
    /**
     * Property Map
     *
     * The Map object this EntrySet belongs to.
     */
    private transient java.util.Map __m_Map;
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
        __mapChildren.put("Entry", EntrySet.Entry.class);
        __mapChildren.put("Iterator", EntrySet.Iterator.class);
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
        return new com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return EntrySet.class;
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
    public boolean add(Object o)
        {
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        Map    map   = getMap();
        java.util.Map.Entry  entry = (java.util.Map.Entry) o;
        Object oKey  = entry == null ? null : entry.getKey();
        Object oVal  = entry == null ? null : entry.getValue();
        
        return map.put(oKey, oVal) == null;
        }
    
    // Accessor for the property "Map"
    /**
     * Getter for property Map.<p>
    * The Map object this EntrySet belongs to.
     */
    public java.util.Map getMap()
        {
        return __m_Map;
        }
    
    public static EntrySet instantiate(java.util.Set set, java.util.Map map)
        {
        EntrySet setWrapper = new EntrySet();
        setWrapper.setSet(set);
        setWrapper.setMap(map);
        return setWrapper;
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
        // import Component;
        // import java.util.Map;
        
        Component parent = get_Parent();
        if (parent instanceof Map)
            {
            setMap((Map) parent);
            }
        
        super.onInit();
        }
    
    // Declared at the super level
    public boolean remove(Object o)
        {
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        if (getSet().remove(o))
            {
            Object oKey = o == null ? null : ((java.util.Map.Entry) o).getKey();
            
            getMap().remove(oKey);
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Accessor for the property "Map"
    /**
     * Setter for property Map.<p>
    * The Map object this EntrySet belongs to.
     */
    public void setMap(java.util.Map pMap)
        {
        __m_Map = pMap;
        }
    
    // Declared at the super level
    public Object[] toArray()
        {
        return toArray(new Object[0]);
        }
    
    // Declared at the super level
    public Object[] toArray(Object[] ao)
        {
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        ao = super.toArray(ao);
        for (int i = 0, c = ao.length; i < c; i++)
            {
            EntrySet.Entry entry = (EntrySet.Entry) _newChild("Entry");
            entry.setEntry((java.util.Map.Entry) ao[i]);
            ao[i] = entry;
            }
        return ao;
        }

    // ---- class: com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet$Entry
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Entry
            extends    com.tangosol.coherence.component.util.collections.WrapperEntry
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
            return new com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet.Entry();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            return EntrySet.Entry.class;
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
        public Object setValue(Object oValue)
            {
            return ((EntrySet) get_Module()).getMap().put(getEntry().getKey(), oValue);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet$Iterator
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Iterator
            extends    com.tangosol.coherence.component.util.collections.WrapperSet.Iterator
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
            return new com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet.Iterator();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            return EntrySet.Iterator.class;
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
        public Object next()
            {
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            EntrySet.Entry entry = (EntrySet.Entry) get_Module()._newChild("Entry");
            entry.setEntry((java.util.Map.Entry) super.next());
            return entry;
            }
        }
    }
