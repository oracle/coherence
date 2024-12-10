
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.WrapperSet

package com.tangosol.coherence.component.util.collections;

/*
* Integrates
*     java.util.Set
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperSet
        extends    com.tangosol.coherence.component.util.Collections
        implements java.util.Set
    {
    // ---- Fields declarations ----
    
    /**
     * Property Initialized
     *
     */
    private transient boolean __m_Initialized;
    
    /**
     * Property Set
     *
     * Wrapped Set
     */
    private transient java.util.Set __m_Set;
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
        __mapChildren.put("Iterator", WrapperSet.Iterator.class);
        }
    
    // Default constructor
    public WrapperSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.collections.WrapperSet();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return WrapperSet.class;
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
    
    //++ java.util.Set integration
    // Access optimization
    // properties integration
    // methods integration
    public boolean add(Object o)
        {
        return getSet().add(o);
        }
    private boolean addAll$Router(java.util.Collection col)
        {
        return getSet().addAll(col);
        }
    public boolean addAll(java.util.Collection col)
        {
        return addAll(this, col);
        }
    public void clear()
        {
        getSet().clear();
        }
    public boolean contains(Object o)
        {
        return getSet().contains(o);
        }
    public boolean containsAll(java.util.Collection col)
        {
        return getSet().containsAll(col);
        }
    public boolean isEmpty()
        {
        return getSet().isEmpty();
        }
    private java.util.Iterator iterator$Router()
        {
        return getSet().iterator();
        }
    public java.util.Iterator iterator()
        {
        WrapperSet.Iterator iter = (WrapperSet.Iterator) _newChild("Iterator");
        iter.setIterator(iterator$Router());
        return iter;
        }
    public boolean remove(Object o)
        {
        return getSet().remove(o);
        }
    private boolean removeAll$Router(java.util.Collection col)
        {
        return getSet().removeAll(col);
        }
    public boolean removeAll(java.util.Collection col)
        {
        return removeAll(this, col);
        }
    private boolean retainAll$Router(java.util.Collection col)
        {
        return getSet().retainAll(col);
        }
    public boolean retainAll(java.util.Collection col)
        {
        return retainAll(this, col);
        }
    public int size()
        {
        return getSet().size();
        }
    public Object[] toArray()
        {
        return getSet().toArray();
        }
    public Object[] toArray(Object[] ao)
        {
        return getSet().toArray(ao);
        }
    //-- java.util.Set integration
    
    // From interface: java.util.Set
    // Declared at the super level
    public boolean equals(Object obj)
        {
        if (obj instanceof WrapperSet)
            {
            return getSet().equals(
                ((WrapperSet) obj).getSet());
            }
        return false;
        }
    
    // Accessor for the property "Set"
    /**
     * Getter for property Set.<p>
    * Wrapped Set
     */
    public java.util.Set getSet()
        {
        return __m_Set;
        }
    
    // From interface: java.util.Set
    // Declared at the super level
    public int hashCode()
        {
        return getSet().hashCode();
        }
    
    public static WrapperSet instantiate(java.util.Set set)
        {
        WrapperSet setWrapper = new WrapperSet();
        setWrapper.setSet(set);
        return setWrapper;
        }
    
    // Accessor for the property "Initialized"
    /**
     * Getter for property Initialized.<p>
     */
    public boolean isInitialized()
        {
        return __m_Initialized;
        }
    
    // Accessor for the property "Initialized"
    /**
     * Setter for property Initialized.<p>
     */
    public void setInitialized(boolean fInitialized)
        {
        __m_Initialized = fInitialized;
        }
    
    // Accessor for the property "Set"
    /**
     * Setter for property Set.<p>
    * Wrapped Set
     */
    public void setSet(java.util.Set set)
        {
        _assert(set != null && !isInitialized());
        __m_Set = (set);
        setInitialized(true);
        }
    
    // Declared at the super level
    public String toString()
        {
        return super.toString() + ":\n" + String.valueOf(getSet());
        }

    // ---- class: com.tangosol.coherence.component.util.collections.WrapperSet$Iterator
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Iterator
            extends    com.tangosol.coherence.component.util.collections.WrapperIterator
        {
        // ---- Fields declarations ----
        
        /**
         * Property Last
         *
         * Last "next" object.
         */
        private transient Object __m_Last;
        
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
            return new com.tangosol.coherence.component.util.collections.WrapperSet.Iterator();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            return WrapperSet.Iterator.class;
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
        
        // Accessor for the property "Last"
        /**
         * Getter for property Last.<p>
        * Last "next" object.
         */
        protected Object getLast()
            {
            return __m_Last;
            }
        
        // Declared at the super level
        public Object next()
            {
            Object oNext = super.next();
            
            setLast(oNext);
            
            return oNext;
            }
        
        // Declared at the super level
        public void remove()
            {
            ((WrapperSet) get_Parent()).remove(getLast());
            }
        
        // Accessor for the property "Last"
        /**
         * Setter for property Last.<p>
        * Last "next" object.
         */
        protected void setLast(Object oLast)
            {
            __m_Last = oLast;
            }
        }
    }
