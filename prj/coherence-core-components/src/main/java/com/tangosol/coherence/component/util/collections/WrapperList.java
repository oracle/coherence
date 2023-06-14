
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.WrapperList

package com.tangosol.coherence.component.util.collections;

/*
* Integrates
*     java.util.List
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperList
        extends    com.tangosol.coherence.component.util.Collections
        implements java.util.List
    {
    // ---- Fields declarations ----
    
    /**
     * Property List
     *
     */
    private transient java.util.List __m_List;
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
        __mapChildren.put("Iterator", WrapperList.Iterator.class);
        }
    
    // Default constructor
    public WrapperList()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperList(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.collections.WrapperList();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return WrapperList.class;
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
    
    //++ java.util.List integration
    // Access optimization
    // properties integration
    // methods integration
    public void add(int i, Object o)
        {
        getList().add(i, o);
        }
    public boolean add(Object o)
        {
        return getList().add(o);
        }
    private boolean addAll$Router(int i, java.util.Collection col)
        {
        return getList().addAll(i, col);
        }
    public boolean addAll(int i, java.util.Collection col)
        {
        boolean fModified = false;
        for (java.util.Iterator iter = col.iterator(); iter.hasNext();)
            {
            add(i++, iter.next());
            fModified = true;
            }
        
        return fModified;
        }
    private boolean addAll$Router(java.util.Collection col)
        {
        return getList().addAll(col);
        }
    public boolean addAll(java.util.Collection col)
        {
        return addAll(this, col);
        }
    public void clear()
        {
        getList().clear();
        }
    public boolean contains(Object o)
        {
        return getList().contains(o);
        }
    public boolean containsAll(java.util.Collection col)
        {
        return getList().containsAll(col);
        }
    public Object get(int i)
        {
        return getList().get(i);
        }
    public int indexOf(Object o)
        {
        return getList().indexOf(o);
        }
    public boolean isEmpty()
        {
        return getList().isEmpty();
        }
    private java.util.Iterator iterator$Router()
        {
        return getList().iterator();
        }
    public java.util.Iterator iterator()
        {
        WrapperList.Iterator iter = (WrapperList.Iterator) _newChild("Iterator");
        iter.setIterator(iterator$Router());
        return iter;
        }
    public int lastIndexOf(Object o)
        {
        return getList().lastIndexOf(o);
        }
    private java.util.ListIterator listIterator$Router()
        {
        return getList().listIterator();
        }
    public java.util.ListIterator listIterator()
        {
        WrapperList.Iterator iter = (WrapperList.Iterator) _newChild("Iterator");
        iter.setIterator(listIterator$Router());
        return iter;
        }
    private java.util.ListIterator listIterator$Router(int i)
        {
        return getList().listIterator(i);
        }
    public java.util.ListIterator listIterator(int i)
        {
        WrapperList.Iterator iter = (WrapperList.Iterator) _newChild("Iterator");
        iter.setIterator(listIterator$Router(i));
        return iter;
        }
    public Object remove(int i)
        {
        return getList().remove(i);
        }
    public boolean remove(Object o)
        {
        return getList().remove(o);
        }
    private boolean removeAll$Router(java.util.Collection col)
        {
        return getList().removeAll(col);
        }
    public boolean removeAll(java.util.Collection col)
        {
        return removeAll(this, col);
        }
    private boolean retainAll$Router(java.util.Collection col)
        {
        return getList().retainAll(col);
        }
    public boolean retainAll(java.util.Collection col)
        {
        return retainAll(this, col);
        }
    public Object set(int i, Object o)
        {
        return getList().set(i, o);
        }
    public int size()
        {
        return getList().size();
        }
    private java.util.List subList$Router(int iFrom, int iTo)
        {
        return getList().subList(iFrom, iTo);
        }
    public java.util.List subList(int iFrom, int iTo)
        {
        return instantiate(subList$Router(iFrom, iTo));
        }
    public Object[] toArray()
        {
        return getList().toArray();
        }
    public Object[] toArray(Object[] ao)
        {
        return getList().toArray(ao);
        }
    //-- java.util.List integration
    
    // From interface: java.util.List
    // Declared at the super level
    public boolean equals(Object obj)
        {
        if (obj instanceof WrapperList)
            {
            return getList().equals(
                ((WrapperList) obj).getList());
            }
        return false;
        }
    
    // Accessor for the property "List"
    /**
     * Getter for property List.<p>
     */
    public java.util.List getList()
        {
        return __m_List;
        }
    
    // From interface: java.util.List
    // Declared at the super level
    public int hashCode()
        {
        return getList().hashCode();
        }
    
    public static WrapperList instantiate(java.util.List list)
        {
        WrapperList listWrapper = new WrapperList();
        listWrapper.setList(list);
        return listWrapper;
        }
    
    // Accessor for the property "List"
    /**
     * Setter for property List.<p>
     */
    public void setList(java.util.List list)
        {
        _assert(list != null && getList() == null, "List is not resettable");
        __m_List = (list);
        }
    
    // Declared at the super level
    public String toString()
        {
        return super.toString() + ":\n" + getList();
        }

    // ---- class: com.tangosol.coherence.component.util.collections.WrapperList$Iterator
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Iterator
            extends    com.tangosol.coherence.component.util.collections.wrapperIterator.ListIterator
        {
        // ---- Fields declarations ----
        
        /**
         * Property LastIndex
         *
         */
        private transient int __m_LastIndex;
        
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
            return new com.tangosol.coherence.component.util.collections.WrapperList.Iterator();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            return WrapperList.Iterator.class;
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
        public void add(Object o)
            {
            int iLast = getLastIndex();
            ((WrapperList) get_Parent()).add(iLast++, o);
            setLastIndex(iLast);
            }
        
        // Accessor for the property "LastIndex"
        /**
         * Getter for property LastIndex.<p>
         */
        public int getLastIndex()
            {
            return __m_LastIndex;
            }
        
        // Declared at the super level
        public Object next()
            {
            Object oNext = super.next();
            
            setLastIndex(previousIndex());
            
            return oNext;
            }
        
        // Declared at the super level
        public Object previous()
            {
            Object oNext = super.previous();
            
            setLastIndex(nextIndex());
            
            return oNext;
            }
        
        // Declared at the super level
        public void remove()
            {
            ((WrapperList) get_Parent()).remove(getLastIndex());
            }
        
        // Accessor for the property "LastIndex"
        /**
         * Setter for property LastIndex.<p>
         */
        public void setLastIndex(int pLastIndex)
            {
            __m_LastIndex = pLastIndex;
            }
        }
    }
