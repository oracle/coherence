
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.WrapperCollection

package com.tangosol.coherence.component.util.collections;

/*
* Integrates
*     java.util.Collection
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperCollection
        extends    com.tangosol.coherence.component.util.Collections
        implements java.util.Collection
    {
    // ---- Fields declarations ----
    
    /**
     * Property Collection
     *
     * Wrapped Collection
     */
    private transient java.util.Collection __m_Collection;
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
        __mapChildren.put("Iterator", WrapperCollection.Iterator.class);
        }
    
    // Default constructor
    public WrapperCollection()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperCollection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.collections.WrapperCollection();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return WrapperCollection.class;
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
    
    //++ java.util.Collection integration
    // Access optimization
    // properties integration
    // methods integration
    public boolean add(Object o)
        {
        return getCollection().add(o);
        }
    private boolean addAll$Router(java.util.Collection col)
        {
        return getCollection().addAll(col);
        }
    public boolean addAll(java.util.Collection col)
        {
        return addAll(this, col);
        }
    public void clear()
        {
        getCollection().clear();
        }
    public boolean contains(Object o)
        {
        return getCollection().contains(o);
        }
    public boolean containsAll(java.util.Collection col)
        {
        return getCollection().containsAll(col);
        }
    public boolean isEmpty()
        {
        return getCollection().isEmpty();
        }
    private java.util.Iterator iterator$Router()
        {
        return getCollection().iterator();
        }
    public java.util.Iterator iterator()
        {
        WrapperCollection.Iterator iter = (WrapperCollection.Iterator) _newChild("Iterator");
        iter.setIterator(iterator$Router());
        return iter;
        }
    public boolean remove(Object o)
        {
        return getCollection().remove(o);
        }
    private boolean removeAll$Router(java.util.Collection col)
        {
        return getCollection().removeAll(col);
        }
    public boolean removeAll(java.util.Collection col)
        {
        return removeAll(this, col);
        }
    private boolean retainAll$Router(java.util.Collection col)
        {
        return getCollection().retainAll(col);
        }
    public boolean retainAll(java.util.Collection col)
        {
        return retainAll(this, col);
        }
    public int size()
        {
        return getCollection().size();
        }
    public Object[] toArray()
        {
        return getCollection().toArray();
        }
    public Object[] toArray(Object[] ao)
        {
        return getCollection().toArray(ao);
        }
    //-- java.util.Collection integration
    
    // From interface: java.util.Collection
    // Declared at the super level
    public boolean equals(Object obj)
        {
        if (obj instanceof WrapperCollection)
            {
            return getCollection().equals(
                ((WrapperCollection) obj).getCollection());
            }
        return false;
        }
    
    // Accessor for the property "Collection"
    /**
     * Getter for property Collection.<p>
    * Wrapped Collection
     */
    public java.util.Collection getCollection()
        {
        return __m_Collection;
        }
    
    // From interface: java.util.Collection
    // Declared at the super level
    public int hashCode()
        {
        return getCollection().hashCode();
        }
    
    public static WrapperCollection instantiate(java.util.Collection col)
        {
        WrapperCollection colWrapper = new WrapperCollection();
        colWrapper.setCollection(col);
        return colWrapper;
        }
    
    // Accessor for the property "Collection"
    /**
     * Setter for property Collection.<p>
    * Wrapped Collection
     */
    public void setCollection(java.util.Collection col)
        {
        _assert(col != null && getCollection() == null, "Collection is not resettable");
        __m_Collection = (col);
        }
    
    // Declared at the super level
    public String toString()
        {
        return super.toString() + ":\n" + getCollection();
        }

    // ---- class: com.tangosol.coherence.component.util.collections.WrapperCollection$Iterator
    
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
            return new com.tangosol.coherence.component.util.collections.WrapperCollection.Iterator();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            return WrapperCollection.Iterator.class;
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
            ((WrapperCollection) get_Parent()).remove(getLast());
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
