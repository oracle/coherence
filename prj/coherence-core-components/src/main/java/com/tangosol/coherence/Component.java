
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.Component

package com.tangosol.coherence;

import com.tangosol.coherence.component.Application;
import com.tangosol.run.component.CallbackSink;
import com.tangosol.run.component.ComponentPeer;
import com.tangosol.run.component.RemoteSink;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlSerializable;
import com.tangosol.util.AssertionException;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.WrapperException;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Component
        implements java.io.ObjectInputValidation,
                   java.io.Serializable,
                   Comparable
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Children
     *
     */
    private java.util.Hashtable __m__Children;
    
    /**
     * Property _Constructed
     *
     * Specifies whether the component design state has been reconstructed.
     * This property is not meant to be designed directly.
     * 
     * @functional
     */
    
    /**
     * Property _ConstructorCache
     *
     * A lazily-filled map keyed by class with a corresponding value being the
     * 3-parameter constructor.
     * 
     * @see #_newChild
     */
    private static com.tangosol.util.SafeHashMap __s__ConstructorCache;
    
    /**
     * Property _ConstructorParams
     *
     */
    private static Object[] __s__ConstructorParams;
    
    /**
     * Property _ConstructorParamTypes
     *
     */
    private static Class[] __s__ConstructorParamTypes;
    
    /**
     * Property _Feed
     *
     * Property used by Component Integration. This property is not meant to be
     * designed directly.
     */
    private Object __m__Feed;
    
    /**
     * Property _Order
     *
     * Property used to control the order of children components within their
     * parent. The type of this property is "float" which allows insertion a
     * new child at derivation or modification level before, after or between
     * any sibling components regardless of the values of their _Order
     * properties. Components with _Order property equal to 0.0 are considered
     * to be "order tolerant".
     */
    private float __m__Order;
    
    /**
     * Property _Parent
     *
     * Parent component for this component. This property is not meant to be
     * designed directly.
     */
    private Component __m__Parent;
    
    /**
     * Property _Reference
     *
     * The _Reference property is designed to maintain a reference to an
     * application component until the very last component is
     * garbage-collected. The _Reference property is static and located on the
     * root component.  Any other case in which a reference must be maintained
     * should be done by having the singleton Application hold that reference. 
     * In other words, as long as the Application instance doesn't go away, any
     * reference that it holds won't go away either.
     * 
     * @see Application#get_Instance
     */
    private static Component __s__Reference;
    
    /**
     * Property _Sink
     *
     * Property used by Component Integration. This property is not meant to be
     * designed directly.
     */
    private Object __m__Sink;
    
    /**
     * Property _State
     *
     * Property with private accessors used by _Constructed and _Deserialized
     * properties. This property is not meant to be designed.
     */
    private transient int __m__State;
    
    /**
     * Property _STATE_AUX_MASK
     *
     * The mask for _STATE_AUX_* values.
     * 
     * 0x3FFF_FFFF
     */
    private static final int _STATE_AUX_MASK = 1073741823;
    
    /**
     * Property _STATE_CONSTRUCTED
     *
     * The value, representing the "_Constructed" state.
     * 
     * 0x4000_0000
     */
    private static final int _STATE_CONSTRUCTED = 1073741824;
    
    /**
     * Property _STATE_DESERIALIZED
     *
     * The value, representing the "_Deserialized" state.
     * 
     * 0x8000_0000
     */
    private static final int _STATE_DESERIALIZED = -2147483648;
    
    /**
     * Property _StateAux
     *
     * An auxiliary functional property that allows subcomponents to use the
     * non-utilized portion of the _State property. The root Component uses
     * only the two high bits, leaving low 30 bits for subcomponents' needs.
     * 
     * This property is not meant to be designed.
     * 
     * @functional
     */
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        _initStatic$Default();
        
        set_ConstructorParamTypes(new Class[] {String.class, Component.class, boolean.class});
        set_ConstructorParams(new Object[] {null, null, Boolean.FALSE});
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // state initialization: static properties
        try
            {
            __s__ConstructorCache = new com.tangosol.util.SafeHashMap();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Initializing constructor
    public Component(String sName, Component compParent, boolean fInit)
        {
        super();
        
        if (__s__Reference == null)
            {
            __s__Reference = this;
            }
        
        if (compParent != null)
            {
            compParent._registerChild(this, sName);
            }
        }
    
    // Main initializer's proxy
    /**
     * This method is a very special one. If implemented (even with a trivial
    * implementation "super._init()"), the class generation will make sure this
    * script is called right after the component allocation has been made but
    * prior to any state initialization (except the constant properties that
    * are "inlined"). This gives the component a chance to stop or defer
    * initialization by not calling the super.
    * Very important note: this method is very similar to a Java constructor:
    * during construction it's only called at the level where it's declared.
    * However, this method could be called directly to reset the state of the
    * component (given the component itself or any of its super components do
    * have an implementation).
    * 
    * There is an analogous initialization methods that you can declare for any
    * integrating component:
    * 
    *     void _initFeed(<parameters>)
    * 
    * where <parameters> could be empty.
    * 
    * If such a method is declared, the class generation doesn't initialize the
    * component's state during _init()'s call to it's super.  Instead, state
    * initialization is performed when the _initFeed(<parameters>) script calls
    * its super.
    * 
    * Currently, there are two ways this is being used.  The first is when the
    * integrated class doesn't have a default constructor, but could be
    * constructed with a constant (virtual or java).  This is done by
    * implementing _init() to call the appropriate _initFeed.  For example,
    * _init() would look like:
    * 
    *     _initFeed(getVirtualConstantValue(), OTHER_CONSTANT);
    * 
    * The other use is to allow the script allocating the component to create
    * to corresponding feed directly with an explicit call.   For example:
    * 
    *     MyComponent myc = new MyComponent();
    *     myc._initFeed(aValue);
    * 
    * See generated java listings for implementation details.
     */
    protected void _init()
        {
        __init();
        }
    
    // Main initializer
    public void __init()
        {
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        }
    
    // Getter for virtual constant _DefaultRemoteName
    protected String get_DefaultRemoteName()
        {
        return null;
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return Component.class;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private Component get_Module()
        {
        return this;
        }
    
    /**
     * Add an instance child component with the specified name to this
    * component.
    * 
    * @param child  a component to add to this component as an instance child
    * @param name  a [unique] name to identify this child. If the name is not
    * specified (null is passed) then a unique child name will be automatically
    * assigned
    * 
    * Note: this method fires onAdd() notification only if the parent (this
    * component) has already been fully constructed.
    * Note2: component containment/aggregation produces children initialization
    * code (see __init()) that is executed while the parent is not flagged as
    * _Constructed yet
     */
    public void _addChild(Component child, String name)
        {
        if (is_Constructed())
            {
            _registerChild(child, name);
            child.onAdd();
            }
        }
    
    /**
     * This method is used for debugging. Default implementation throws an
    * exception if the condition is "false".
     */
    public static void _assert(boolean fCondition)
        {
        if (!fCondition)
            {
            _assertFailed("");
            }
        }
    
    /**
     * This method is used for debugging. Default implementation throws an
    * exception if the condition is "false".
     */
    public static void _assert(boolean fCondition, String sInfo)
        {
        if (!fCondition)
            {
            _assertFailed(sInfo);
            }
        }
    
    /**
     * This method is used for debugging. Throw an exception with the specified
    * message.
     */
    public static void _assertFailed(String sInfo)
        {
        // import Component.Application;
        // import com.tangosol.util.AssertionException;
        
        String sStack = get_StackTrace();
        
        int iPos;
        iPos = sStack.lastIndexOf("._assert"); // clean itself from the trace
        iPos = sStack.indexOf('\n', iPos);
        
        _trace("Assertion failed: " + sInfo + sStack.substring(iPos), 1);
        
        ((Application) Application.get_Instance()).onError(new AssertionException(sInfo));
        }
    
    /**
     * This method is used for debugging. Default implementation delegates to
    * the Application component method "debugSound".
     */
    public static void _beep()
        {
        // import Component.Application;
        
        Application app = (Application) Application.get_Instance();
        _assert(app != null);
        app.debugSound();
        }
    
    /**
     * Returns an enumeration of names for design-time children of this
    * component.
     */
    public java.util.Enumeration _enumChildNames()
        {
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.SimpleEnumerator;
        // import java.util.Enumeration;
        // import java.util.Map;
        
        Map mapChildren = get_ChildClasses();
        
        return mapChildren == null
            ? NullImplementation.getEnumeration()
            : new SimpleEnumerator(mapChildren.keySet().iterator());
        }
    
    /**
     * Returns an enumeration of children components for this component.
     */
    public java.util.Enumeration _enumChildren()
        {
        // import com.tangosol.util.NullImplementation;
        // import java.util.Hashtable;
        
        Hashtable tblChildren = get_Children();
        
        return tblChildren == null ?
            NullImplementation.getEnumeration() : tblChildren.elements() ;
        }
    
    /**
     * Returns an enumeration of children components for this component ordered
    * by the values of their _Order property.
     */
    public java.util.Enumeration _enumChildrenInOrder()
        {
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.SimpleEnumerator;
        // import java.util.Arrays;
        // import java.util.Enumeration;
        // import java.util.Hashtable;
        
        Hashtable tblChildren = get_Children();
        if (tblChildren != null)
            {
            Component[] acChildren  = new Component[tblChildren.size()];
        
            int ix = 0;
            for (Enumeration e = tblChildren.elements(); e.hasMoreElements(); ix++)
                {
                acChildren[ix] = (Component) e.nextElement();
                }
        
            Arrays.sort(acChildren);
            return new SimpleEnumerator(acChildren);
            }
        else
            {
            return NullImplementation.getEnumeration();
            }
        }
    
    /**
     * Find an ancestor of the component that is or derives from the specified
    * class
    * 
    * @return the ancestor of the specified class or null if not found
     */
    public Component _findAncestor(Class clz)
        {
        Component parent = get_Parent();
        
        while (parent != null && !clz.isAssignableFrom(parent.getClass()))
            {
            parent = parent.get_Parent();
            }
        
        return parent;
        }
    
    /**
     * Find a child component by local name.
    * 
    * @param sLocalId  child's name (local identity)
    * 
    * @return  child component or null if not found
    * 
    * Note: Local Identity could be composed or simple. Composed Identity is a
    * $-delimited list of simple identities.
     */
    public Component _findChild(String sLocalId)
        {
        return _findChild(sLocalId, false, null);
        }
    
    /**
     * Find a child component by name.
    * 
    * @param sLocalId  child's name (local identity)
    * @param fFullSearch allow recursive lookup
    * 
    * @return  child component (null if not found)
    * 
    * Note: Local Identity could be composed or simple. Composed Identity is a
    * $-delimited list of simple identities.
     */
    public Component _findChild(String sLocalId, boolean fFullSearch)
        {
        return _findChild(sLocalId, fFullSearch, null);
        }
    
    /**
     * Find a child component by name.
    * 
    * @param sLocalId  child's name (local identity)
    * @param fFullSearch  allow recursive lookup
    * @param compSkip  component that has already been looked up
    * 
    * @return  child component (null if not found)
    * 
    * Note: Local Identity could be composed or simple. Composed Identity is a
    * $-delimited list of simple identities.
     */
    private Component _findChild(String sLocalId, boolean fFullSearch, Component compSkip)
        {
        // import java.util.Enumeration;
        // import java.util.Hashtable;
        
        Hashtable tblChildren = get_Children();
        if (tblChildren == null)
            {
            return null;
            }
        
        int       ofNext = sLocalId.indexOf('$');
        String    sName  = ofNext < 0 ? sLocalId : sLocalId.substring(0, ofNext);
        Component child  = (Component) tblChildren.get(sName);
        
        if (child != null && ofNext >= 0)
            {
            // composed identity -- go down without recursion
            child = child._findChild(sLocalId.substring(ofNext + 1), false, null);
            }
        
        if (child != null)
            {
            return child;
            }
        
        if (fFullSearch)
            {
            // lookup recursively in all the children except the one
            // that has already been looked up
            Enumeration e = tblChildren.elements();
        
            while (e.hasMoreElements())
                {
                Component compNext = (Component) e.nextElement();
                if (compNext != compSkip)
                    {
                    child = compNext._findChild(sLocalId, true, compSkip);
                    if (child != null)
                        {
                        return child;
                        }
                    }
                }
            }
        
        return null;
        }
    
    /**
     * Find a class of a static child component by local name.
    * 
    * @param name  child's name (simple identity)
    * 
    * @return  child component's class or null if not found
    * 
    * Note: this method works only for static children. Use _findChild for
    * instance children.
     */
    public Class _findChildClass(String name)
        {
        // import java.util.Map;
        
        Class clz = null;
        
        Map mapClasses = get_ChildClasses();
        if (mapClasses != null)
            {
            clz = (Class) mapClasses.get(name);
            if (clz == null && name.indexOf('$') >= 0)
                {
                throw new IllegalArgumentException("Invalid simple name " + name);
                }
            }
        
        return clz;
        }
    
    /**
     * Find a Component with the specified Feed object.
    * 
    * @param pFeed  feed object to look for
    * 
    * @return  Component with the specified feed or null if not found
    * 
    * Note: First we check if the Feed implements ComponentPeer (auto generated
    * by the JavaBean integration model) and return the Component peer
    * immediately; otherwise we look up all the children using the
    * breadth-first search (could be a relatively expensive operation).
     */
    public Component _findFeed(Object pFeed)
        {
        // import com.tangosol.run.component.ComponentPeer;
        
        if (pFeed == null)
            {
            return null;
            }
        
        if (pFeed instanceof ComponentPeer)
            {
            return (Component) ((ComponentPeer) pFeed).get_ComponentPeer();
            }
        
        if (pFeed == get_Feed())
            {
            return this;
            }
        
        return _findFeed(pFeed, true);
        }
    
    /**
     * Find a child component with the specified feed (using breadth-first
    * search)
    * 
    * @param pFeed  feed object to look for
    * @param fFullSearch  set to true to allow recursive lookup
    * 
    * @return  child component with the specified feed or null if not found
     */
    private Component _findFeed(Object pFeed, boolean fFullSearch)
        {
        // import java.util.Enumeration;
        
        for (Enumeration e = _enumChildren(); e.hasMoreElements();)
            {
            Component child = (Component) e.nextElement();
            if (child.get_Feed() == pFeed)
                {
                return child;
                }
            }
        
        if (fFullSearch)
            {
            // lookup recursively in all the children 
            for (Enumeration e = _enumChildren(); e.hasMoreElements();)
                {
                Component compNext = (Component) e.nextElement();
                Component child    = compNext._findFeed(pFeed, true);
                if (child != null)
                    {
                    return child;
                    }
                }
            }
        
        return null;
        }
    
    /**
     * Find a child, parent or sibling component with the specified name.
    * 
    * @param sLocalId  components name (local identity)
    * 
    * @return a component with the specified name or null if not found.
    * 
    * Note: Local Identity could be composed or simple (i.e. a component
    * "A$B$C$D" could be found by "D" as well as by "C$D").
    * Note2: The algorithm looks up the component's own children first, then
    * ask the parent to do the same (skipping itself).
     */
    public Component _findName(String sLocalId)
        {
        Component comp = this;
        Component skip = null;
        
        do
            {
            Component sibling = comp._findChild(sLocalId, true, skip);
            if (sibling != null)
                {
                return sibling;
                }
        
            skip = comp;
            comp = comp.get_Parent();
            }
        while (comp != null);
        
        return null;
        }
    
    /**
     * Find a sibling component with the value of the "_Order" property being
    * set (not equal to 0.0) to the smallest value bigger than ours or the
    * smallest value at all (in the case of the circular search)
    * 
    * @param fCircularSearch  in the case when there is no component with
    * bigger value: if set to true, returns the component with the smallest
    * value; otherwise returns null
    *  
    * @return the next ordered sibling Component or null
     */
    public Component _findNextPosition(boolean fCircularSearch)
        {
        // import java.util.Enumeration;
        
        Component next   = null;
        Component first  = null;
        Component parent = get_Parent();
        if (parent != null)
            {
            float flMinDelta  = Float.MAX_VALUE;
            float flMinValue  = Float.MAX_VALUE;
            float flThisOrder = get_Order();
        
            for (Enumeration e = parent._enumChildren(); e.hasMoreElements();)
                {
                Component   that = (Component) e.nextElement();
                float flThatOrder = that.get_Order();
                if (flThatOrder != 0.0f)
                    {
                    float flDelta = flThatOrder - flThisOrder;
                    if (flDelta > 0 && flDelta < flMinDelta)
                        {
                        flMinDelta = flDelta;
                        next = that;
                        }
                    else if (fCircularSearch && flThatOrder < flMinValue)
                        {
                        first = that;
                        }
                    }
                }
            }
        
        return next != null    ? next  :
               fCircularSearch ? first :
                                 null;
        }
    
    /**
     * Find a sibling component with the value of the "_Order" property being
    * set (not equal to 0.0) to the biggest value smaller than ours or the
    * biggest value at all (in the case of the circular search)
    * 
    * @param fCircularSearch  in the case when there is no component with
    * smaller value: if set to true, returns the component with the biggest
    * value; otherwise returns null
    * 
    * @return the previous ordered sibling Component or null
     */
    public Component _findPreviousPosition(boolean fCircularSearch)
        {
        // import java.util.Enumeration;
        
        Component prev   = null;
        Component last   = null;
        Component parent = get_Parent();
        if (parent != null)
            {
            float flMinDelta  = Float.MAX_VALUE;
            float flMaxValue  = 0.0f;
            float flThisOrder = get_Order();
            if (flThisOrder == 0.0f)
                {
                flThisOrder = Float.MAX_VALUE;
                }
        
            for (Enumeration e = parent._enumChildren(); e.hasMoreElements();)
                {
                Component   that = (Component) e.nextElement();
                float flThatOrder = that.get_Order();
                if (flThatOrder != 0.0f)
                    {
                    float flDelta = flThisOrder - flThatOrder;
                    if (flDelta > 0 && flDelta < flMinDelta)
                        {
                        flMinDelta = flDelta;
                        prev = that;
                        }
                    else if (fCircularSearch && flThatOrder > flMaxValue)
                        {
                        last = that;
                        }
                    }
                }
            }
        
        return prev != null    ? prev  :
               fCircularSearch ? last :
                                 null;
        }
    
    public void _imports()
        {
        // TODO remove this method
        }
    
    /**
     * Returns true if this component is ancestor of the specified component;
    * false otherwise.
     */
    public boolean _isAncestorOf(Component that)
        {
        while (that != null)
            {
            that = that.get_Parent();
            if (that == this)
                {
                return true;
                }
            }
        return false;
        }
    
    /**
     * Check whether or not the tracing of the specified severity level is
    * enabled.
     */
    public static boolean _isTraceEnabled(int iSeverity)
        {
        // import Component.Application;
        
        return ((Application) Application.get_Instance()).isDebugOutputEnabled(iSeverity);
        }
    
    /**
     * Link a child component to this parent component.
    * 
    * @param child  a component to link as a child
     */
    public void _linkChild(Component child)
        {
        Component parent = child.get_Parent();
        if (parent != null)
            {
            throw new IllegalStateException(
                "Attempt to re-register the child: " + child + " for " + this);
            }
        child.set_Parent(this);
        }
    
    /**
     * Used by Application.get_Instance to create an instance of the correct
    * application.  This method is only called against the very first component
    * to be instantiated (an "entry point"), and only if it is not an
    * Application component. An entry point component can implement this method
    * if it knows what application should be instantiated.
    * 
    * @return an appropriate Application component, or null if this component
    * does not know what Application component should be instantiated
    * 
    * @see #_Reference
     */
    public com.tangosol.coherence.component.Application _makeApplication()
        {
        return null;
        }
    
    /**
     * Used by auto-generated remote stubs and feeds to create a Component from
    * a remote object.
    * 
    * @param remoteObject the remote object to use as the sink
     */
    public static Component _makeRemoteInstance(Object remoteObject)
        {
        throw new com.tangosol.run.component.IntegrationException("Unexpected call to _makeRemoteInstance");
        }
    
    /**
     * Create and set the remote object associated with this client based
    * component.  This method is used by the class generation code during the
    * initialization of the object.
     */
    protected void _makeRemoteObject()
        {
        // import com.tangosol.util.WrapperException;
        // import javax.naming.Context;
        // import javax.naming.NamingException;
        
        // Get the context of this run-time.
        Context context = get_RemoteContext();
        if (context == null)
            {
            // No remote context, operate in local mode.
            set_Sink(null);
            return;
            }
        
        // Get this object's remote name.
        String name = get_RemoteName();
        if (name == null)
            {
            // No remote name, operate in local mode.
            set_Sink(null);
            return;
            }
        
        // Create the remote object.
        Object remoteObject;
        try
            {
            remoteObject = context.lookup(name);
            }
        catch (NamingException e)
            {
            throw new WrapperException(e);
            }
        
        // Set the remote object as the sink.
        set_Sink(remoteObject);
        }
    
    /**
     * Returns the name of the specified child component as known by this
    * component as the parent.
     */
    private String _nameOf(Component child)
        {
        // import java.util.Enumeration;
        // import java.util.Hashtable;
        
        Hashtable tblChildren = get_Children();
        if (tblChildren == null)
            {
            return null;
            }
        
        // first, try to guess the local name out of class name
        String sName = getClass().getName();
        sName = sName.substring(sName.lastIndexOf('$') + 1);
        
        if (tblChildren.get(sName) == child)
            {
            return sName;
            }
        
        Enumeration enumNames;
        Enumeration enumChildren;
        
        synchronized (tblChildren)
            {
            enumNames    = tblChildren.keys();
            enumChildren = tblChildren.elements();
            }
        
        while (enumNames.hasMoreElements())
            {
            sName = (String) enumNames.nextElement();
            if (child == enumChildren.nextElement())
                {
                return sName;
                }
            }
        
        return null;
        }
    
    /**
     * Create a new instance of a [static] child component with the specified
    * local name. For example, if a component A has a [static] child A$C, the
    * call to <code>_newChild("C")</code> will instantiate the child component.
    * The advantage of using virtual _newChild call versus static
    * A$C.get_Instance() is that if the component A gets derived by A.B (which
    * can make some modifications to A.B$C) then the same _newChild call will
    * actually instantiate A.B$C.
    * 
    * @param name  static child's name (local identity)
    * 
    * @return  a newly allocated instance of the child component with the given
    * local name.  Returns null  if the child component could not be found or
    * if an application tries to instantiate an abstract component, or if the
    * instantiation fails for some other reason.
    * 
    * Note: Local Identity could be composed or simple. Composed Identity is a
    * $-delimited list of simple identities.
     */
    public Component _newChild(String name)
        {
        // import com.tangosol.util.WrapperException;
        // import java.lang.reflect.Constructor;
        // import java.util.Map;
        
        int    ofSimple = name.indexOf('$');
        String sChild   = ofSimple < 0 ? name : name.substring(0, ofSimple);
        
        Class clzChild = _findChildClass(sChild);
        if (clzChild == null)
            {
            return null;
            }
        
        try
            {
            /*
            Component child = (Component) clzChild.newInstance();
            child.set_Parent(this);
            */
        
            // we need the static child remain un-initialized until after
            // the parent is set and only then proceed with the initialization
            /*
            Component child = (Component) ClassHelper.newInstance(clzChild,
                    new Object[] {null, null, Boolean.FALSE});
            */
            Map         cache       = get_ConstructorCache();
            Constructor constructor = (Constructor) cache.get(clzChild);
            if (constructor == null)
                {
                constructor = clzChild.getConstructor(get_ConstructorParamTypes());
                cache.put(clzChild, constructor);
                }
            Component child = (Component) constructor.newInstance(get_ConstructorParams());
        
            // we want to setup the parent before going into the state initialization;
            // however, the side effect of this is that "onInit" will not be called
            // by "set_Constructed" (called by "_init").
            child.set_Parent(this);
            child._init();
            child.onInit();
            return ofSimple < 0 ? child : child._newChild(name.substring(ofSimple + 1));
            }
        // InstantiationException, reflect.InvocationException
        catch (Exception e)
            {
            throw new WrapperException(e);
            }
        }
    
    /**
     * Create a new instance of a component with the specified fully qualified
    * name  (i.e. "Component.GUI.Color.Red").
    * 
    * The algorithm works out of the assumption that the specified component
    * belongs to the same root package that the root Component.
    * 
    * @param name  fully qualified component name
    * 
    * @return  newly allocated instance of a component with the given fully
    * qualified name or null if the component could not be found. 
    * 
    * @throws WrapperException if the specified component is abstract or if the
    * instantiation fails for any other reason.
     */
    public static Component _newInstance(String name)
        {
        // import com.tangosol.util.WrapperException;
        
        String        sThisClz = Component.class.getName();
        String        sPackage = sThisClz.substring(0, sThisClz.indexOf("Component"));
        StringBuilder sbName   = new StringBuilder(sPackage.length() + name.length());
        int           ofStart  = 0;
        
        sbName.append(sPackage);
        while (true)
            {
            int ofEnd = name.indexOf('.', ofStart);
            if (ofEnd > 0)
                {
                sbName.append(Character.toLowerCase(name.charAt(ofStart)))
                      .append(name, ofStart + 1, ofEnd + 1);
                ofStart = ofEnd + 1;
                }
            else
                {
                sbName.append(name.substring(ofStart));
                break;
                }
            }
        
        try
            {
            Class clz = Class.forName(sbName.toString());
            return (Component) clz.newInstance();
            }
        // we don't want to catch RuntimeException that could come from
        // component initialization code
        // TODO: consider wrapping them all into one CHECKED exception
        catch (ClassNotFoundException ignored) {}
        catch (NoClassDefFoundError e)   {throw new WrapperException(e);}
        catch (IllegalAccessException e) {throw new WrapperException(e);}
        catch (InstantiationException e) {throw new WrapperException(e);}
        
        return null;
        }
    
    /**
     * Find a position of the specified child component relative to other
    * children of this component by calculating the number of the "ordered"
    * children that have the lesser _Order property value
    * 
    * Note: a child with the _Order equal to 0.0 is considered to be not
    * "ordered".
    * 
    * @return position among the "ordered" children, where 0 means there is no
    * other child with a lower _Order value and -1 means that this child is not
    * ordered.
    * 
     */
    public int _positionOf(Component child)
        {
        // import java.util.Enumeration;
        
        int iPos = 0;
        
        float fThis = child.get_Order();
        if (fThis == 0.0f)
            {
            return -1;
            }
        
        for (Enumeration e = _enumChildren(); e.hasMoreElements();)
            {
            float fThat = ((Component) e.nextElement()).get_Order();
            if (fThat != 0.0f && fThat < fThis)
                {
                iPos++;
                }
            }
        
        return iPos;
        }
    
    private void _registerChild(Component child, String name)
        {
        // import java.util.Hashtable;
        
        Hashtable tblChildren = get_Children();
        if (tblChildren == null)
            {
            set_Children(tblChildren = new Hashtable(17));
            }
        
        if (name == null)
            {
            // create local identity out of child's composite identity (class name)
            String sLocalId = child.get_Name();
        
            // make sure it's not duplicate
            name = sLocalId;
            for (int i = 0; tblChildren.get(name) != null; i++)
                {
                name = sLocalId + "_" + i;
                }
            }
        
        if (tblChildren.get(name) == null)
            {
            _linkChild(child);
            tblChildren.put(name, child);
            }
        else
            {
            throw new IllegalStateException("Duplicated local identity: " + name + " for " + this);
            }
        }
    
    /**
     * Remove all the children of this component.
     */
    public void _removeAllChildren()
        {
        // import java.util.Enumeration;
        
        for (Enumeration e = _enumChildren(); e.hasMoreElements();)
            {
            _removeChild((Component) e.nextElement());
            }
        }
    
    /**
     * Remove the specified instance child component.
     */
    public void _removeChild(Component child)
        {
        child.onRemove();
        
        _unregisterChild(child);
        }
    
    /**
     * This method is used for debugging.
     */
    public static void _trace(String sMessage)
        {
        _trace(sMessage, 0);
        }
    
    /**
     * This method is used for debugging.
     */
    public static void _trace(String sMessage, int iSeverity)
        {
        // import Component.Application;
        
        ((Application) Application.get_Instance()).debugOutput(sMessage, iSeverity);
        }
    
    /**
     * This method is used for debugging.
     */
    public static void _trace(Throwable e)
        {
        _trace(e, null);
        }
    
    /**
     * This method is used for debugging.
     */
    public static void _trace(Throwable e, String sMessage)
        {
        // import Component.Application;
        
        if (sMessage == null)
            {
            ((Application) Application.get_Instance()).
                debugOutput(e);
            }
        else
            {
            ((Application) Application.get_Instance()).
                debugOutput(sMessage + ": " + getStackTrace(e), 1);
            }
        }
    
    /**
     * Unink a child component from this parent component.
    * 
    * @param child  a component to unlink
     */
    public void _unlinkChild(Component child)
        {
        Component parent = child.get_Parent();
        if (parent == this)
            {
            child.set_Parent(null);
            }
        else
            {
            throw new IllegalStateException(
                "Attempt to unregister the child: " + child + " for " + this);
            }
        }
    
    private void _unregisterChild(Component child)
        {
        String sName = _nameOf(child);
        
        if (sName != null)
            {
            child.set_Parent(null);
            get_Children().remove(sName);
            }
        else
            {
            throw new IllegalArgumentException("Unregistered child: " + child + " for " + this);
            }
        }
    
    /**
     * Apply configuration information about this component from the specified
    * property table using the specified string to prefix the property names.
    * 
    * For example, to retrieve a value of Enabled property it's recommended to
    * write:
    *     boolean fEnabled = config.getBoolean(sPrefix + ".Enabled");
    * or
    *     boolean fEnabled = config.getBoolean(sPrefix + ".Enabled",
    * isEnabled());
    * or
    *     if (config.containsKey(sPrefix + ".Enabled"))
    *         {
    *         boolean fEnabled = config.getBoolean(sPrefix + ".Enabled");
    *         }
    * 
    * Note: this method's access is declared as protected at the root Component
    * level. Any component that wants to expose the "configurable"
    * functionality should implement this method and/or change the access to
    * public.
    * 
    * @param config  a Config component used to retrieve the configuration info
    * for this component
    * @param sPrefix  a prefix used by this component for properties
    * identification
    * 
    * @see #saveConfig
    * @see Component.Util.Config
     */
    public void applyConfig(com.tangosol.coherence.component.util.Config config, String sPrefix)
        {
        // import java.util.Enumeration;
        // import java.util.Hashtable;
        
        Hashtable tblChildren = get_Children();
        if (tblChildren != null)
            {
            Enumeration enumNames;
            Enumeration enumChildren;
        
            synchronized (tblChildren)
                {
                enumNames    = tblChildren.keys();
                enumChildren = tblChildren.elements();
                }
        
            while (enumNames.hasMoreElements())
                {
                String    sChild = (String)    enumNames   .nextElement();
                Component child  = (Component) enumChildren.nextElement();
        
                child.applyConfig(config, sPrefix + '$' + sChild);
                }
            }
        }
    
    // From interface: java.lang.Comparable
    /**
     * Compares this object with the specified object for order.  Returns a
    * negative integer, zero, or a positive integer as this object is less
    * than, equal to, or greater than the specified object.
    * 
    * @param o  the Object to be compared.
    * @return  a negative integer, zero, or a positive integer as this object
    * is less than, equal to, or greater than the specified object.
    * 
    * @throws ClassCastException if the specified object's type prevents it
    * from being compared to this Object.
     */
    public int compareTo(Object o)
        {
        if (o == this)
            {
            return 0;
            }
        
        Component that       = (Component) o;
        Object    parentThis = this.get_Parent();
        if (parentThis != null &&
            parentThis == that.get_Parent())
            {
            float flOrderThis = this.get_Order();
            float flOrderThat = that.get_Order();
        
            return Float.compare(flOrderThis, flOrderThat);
            }
        
        throw new IllegalArgumentException(); // not comparable
        }
    
    // Accessor for the property "_ChildClasses"
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return null;
        }
    
    // Accessor for the property "_Children"
    /**
     * Getter for property _Children.<p>
     */
    private java.util.Hashtable get_Children()
        {
        return __m__Children;
        }
    
    // Accessor for the property "_ConstructorCache"
    /**
     * Getter for property _ConstructorCache.<p>
    * A lazily-filled map keyed by class with a corresponding value being the
    * 3-parameter constructor.
    * 
    * @see #_newChild
     */
    private static com.tangosol.util.SafeHashMap get_ConstructorCache()
        {
        return __s__ConstructorCache;
        }
    
    // Accessor for the property "_ConstructorParams"
    /**
     * Getter for property _ConstructorParams.<p>
     */
    private static Object[] get_ConstructorParams()
        {
        return __s__ConstructorParams;
        }
    
    // Accessor for the property "_ConstructorParamTypes"
    /**
     * Getter for property _ConstructorParamTypes.<p>
     */
    private static Class[] get_ConstructorParamTypes()
        {
        return __s__ConstructorParamTypes;
        }
    
    // Accessor for the property "_Feed"
    /**
     * Getter for property _Feed.<p>
    * Property used by Component Integration. This property is not meant to be
    * designed directly.
     */
    public Object get_Feed()
        {
        return __m__Feed;
        }
    
    // Accessor for the property "_Instance"
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static Component get_Instance()
        {
        return null;
        }
    
    // Accessor for the property "_Name"
    /**
     * Returns the name of this component. If component is a child, this returns
    * the name of this component as known by it's parent; otherwise the name is
    * constructed out of the class name.
     */
    public final String get_Name()
        {
        Component parent = get_Parent();
        String    sName  = null;
        
        if (parent != null)
            {
            // Note: static child may still yield "null"
            sName = parent._nameOf(this);
            }
        
        if (sName == null)
            {
            sName = getClass().getName();
            sName = sName.substring(sName.lastIndexOf('.') + 1);
            sName = sName.substring(sName.lastIndexOf('$') + 1);
            }
        
        return sName;
        }
    
    // Accessor for the property "_Order"
    /**
     * Getter for property _Order.<p>
    * Property used to control the order of children components within their
    * parent. The type of this property is "float" which allows insertion a new
    * child at derivation or modification level before, after or between any
    * sibling components regardless of the values of their _Order properties.
    * Components with _Order property equal to 0.0 are considered to be "order
    * tolerant".
     */
    public float get_Order()
        {
        return __m__Order;
        }
    
    // Accessor for the property "_Parent"
    /**
     * Getter for property _Parent.<p>
    * Parent component for this component. This property is not meant to be
    * designed directly.
     */
    public Component get_Parent()
        {
        return __m__Parent;
        }
    
    // Accessor for the property "_Position"
    /**
     * Returns a relative index of this component among its sibling components.
    * This calculation is based on the value of the _Order property for this
    * component and its sibling components.
     */
    public int get_Position()
        {
        Component parent = get_Parent();
        return parent == null ? 0 : parent._positionOf(this);
        }
    
    // Accessor for the property "_Reference"
    /**
     * Getter for property _Reference.<p>
    * The _Reference property is designed to maintain a reference to an
    * application component until the very last component is garbage-collected.
    * The _Reference property is static and located on the root component.  Any
    * other case in which a reference must be maintained should be done by
    * having the singleton Application hold that reference.  In other words, as
    * long as the Application instance doesn't go away, any reference that it
    * holds won't go away either.
    * 
    * @see Application#get_Instance
     */
    protected static Component get_Reference()
        {
        return __s__Reference;
        }
    
    // Accessor for the property "_RemoteContext"
    /**
     * Returns the remote context to use when finding the remote object
    * associated with this local component.  Remotable sub-components may
    * override this method returning appropriate naming contexts. If null is
    * returned, the component will only operate in the local mode.
     */
    protected javax.naming.Context get_RemoteContext()
        {
        // import Component.Application;
        
        return ((Application) Application.get_Instance()).getContext();
        }
    
    // Accessor for the property "_RemoteName"
    /**
     * Returns the remote [JNDI] name to use when finding the remote object
    * associated with this local component. Remotable sub-components may
    * override this method returning appropriate names.  If null is returned,
    * the component will only operate in the local mode.
     */
    protected String get_RemoteName()
        {
        String sName = get_DefaultRemoteName();
        
        return sName == null ? get_Name() : sName;
        }
    
    // Accessor for the property "_RemoteObject"
    /**
     * Returns the remote object used to communicate to the server or null if
    * running in local mode.
     */
    public Object get_RemoteObject()
        {
        // import com.tangosol.run.component.RemoteSink;
        
        return ((RemoteSink) get_Sink()).get_RemoteObject();
        }
    
    // Accessor for the property "_Sink"
    /**
     * Getter for property _Sink.<p>
    * Property used by Component Integration. This property is not meant to be
    * designed directly.
     */
    public Object get_Sink()
        {
        return __m__Sink;
        }
    
    // Accessor for the property "_StackTrace"
    /**
     * Returns a string with the stack trace for the caller method.
     */
    protected static String get_StackTrace()
        {
        String sStack = getStackTrace(new Exception());
        
        int iPos;
        iPos = sStack.lastIndexOf(".get_StackTrace("); // clean itself from the trace
        iPos = sStack.indexOf('\n', iPos);
        
        return sStack.substring(iPos + 1);
        }
    
    // Accessor for the property "_State"
    /**
     * Getter for property _State.<p>
    * Property with private accessors used by _Constructed and _Deserialized
    * properties. This property is not meant to be designed.
     */
    private int get_State()
        {
        return __m__State;
        }
    
    // Accessor for the property "_StateAux"
    /**
     * Getter for property _StateAux.<p>
    * An auxiliary functional property that allows subcomponents to use the
    * non-utilized portion of the _State property. The root Component uses only
    * the two high bits, leaving low 30 bits for subcomponents' needs.
    * 
    * This property is not meant to be designed.
    * 
    * @functional
     */
    protected int get_StateAux()
        {
        return get_State() & _STATE_AUX_MASK;
        }
    
    /**
     * Returns a string with the stack trace for the specified exception.
     */
    protected static String getStackTrace(Throwable exception)
        {
        // import java.io.ByteArrayOutputStream;
        // import java.io.PrintWriter;
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintWriter           writer = new PrintWriter(stream);
        
        exception.printStackTrace(writer);
        
        writer.close();
        
        return stream.toString();
        }
    
    // Accessor for the property "_Constructed"
    /**
     * Getter for property _Constructed.<p>
    * Specifies whether the component design state has been reconstructed. This
    * property is not meant to be designed directly.
    * 
    * @functional
     */
    public final boolean is_Constructed()
        {
        return (get_State() & _STATE_CONSTRUCTED) != 0;
        }
    
    // Accessor for the property "_Deserialized"
    /**
     * Getter for property _Deserialized.<p>
    * Specifies whether this component was "deserialized"
    * (as opposed to "newed")
     */
    public final boolean is_Deserialized()
        {
        return (get_State() & _STATE_DESERIALIZED) != 0;
        }
    
    /**
     * The "component has been added to a containing component"
    * method-notification.
    * 
    * Note: this notification is not sent during the component's state
    * initialization (deserialization)
    * 
    * @see #_addChild
     */
    public void onAdd()
        {
        }
    
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
        // import java.util.Enumeration;
        
        for (Enumeration e = _enumChildren(); e.hasMoreElements();)
            {
            ((Component) e.nextElement()).onInit();
            }
        }
    
    /**
     * The "component has been removed from the containing component"
    * method-notification.
     */
    public void onRemove()
        {
        }
    
    /**
     * This method is a part of "Serializable" pseudo-interface and is used to
    * hook-up the validation callback.
    * 
    * @see #validateObject
     */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException,
                   java.lang.ClassNotFoundException
        {
        try
            {
            in.registerValidation(this, 0);
            }
        catch (Throwable t)
            {
            System.err.println("Warning:  registerValidation() failed");
            validateObject();
            }
        
        in.defaultReadObject();
        }
    
    /**
     * Save configuration information about this component into the specified
    * Config component using the specified string to prefix the property names.
    * 
    * For example, to store values of Enabled and Text properties it's
    * recommended to write:
    *     config.putBoolean(sPrefix + ".Enabled", isEnabled());
    *     config.putString(sPrefix + ".Text", getText());
    * 
    * Note: this method's access is declared as "advanced" at the root
    * Component level. Any component that wants to expose the "configurable"
    * functionality should implement this method and/or change the "visibility"
    * to public.
    * 
    * @param config  a Config component used to store the configuration info
    * for this component
    * @param sPrefix  a prefix used by this component for properties
    * identification
    * 
    * @see #applyConfig
    * @see Component.Util.Config
     */
    public void saveConfig(com.tangosol.coherence.component.util.Config config, String sPrefix)
        {
        // import java.util.Enumeration;
        // import java.util.Hashtable;
        
        Hashtable tblChildren = get_Children();
        if (tblChildren != null)
            {
            Enumeration enumNames;
            Enumeration enumChildren;
        
            synchronized (tblChildren)
                {
                enumNames    = tblChildren.keys();
                enumChildren = tblChildren.elements();
                }
            
            while (enumNames.hasMoreElements())
                {
                String    sChild = (String)    enumNames   .nextElement();
                Component child  = (Component) enumChildren.nextElement();
            
                child.saveConfig(config, sPrefix + '$' + sChild);
                }
            }
        }
    
    // Accessor for the property "_Children"
    /**
     * Setter for property _Children.<p>
     */
    private void set_Children(java.util.Hashtable p_Children)
        {
        __m__Children = p_Children;
        }
    
    // Accessor for the property "_Constructed"
    /**
     * Setter for property _Constructed.<p>
    * Specifies whether the component design state has been reconstructed. This
    * property is not meant to be designed directly.
    * 
    * @functional
     */
    protected final void set_Constructed(boolean fConstructed)
        {
        if (fConstructed)
            {
            // set the state and call "create event" for the topmost component
            set_State(get_State() | _STATE_CONSTRUCTED);
            if (get_Parent() == null)
                {
                onInit();
                }
            }
        else
            {
            // TODO: should we allow de-constructing ??
            throw new IllegalArgumentException("Component cannot be de-constructed");
            }
        }
    
    // Accessor for the property "_ConstructorCache"
    /**
     * Setter for property _ConstructorCache.<p>
    * A lazily-filled map keyed by class with a corresponding value being the
    * 3-parameter constructor.
    * 
    * @see #_newChild
     */
    private static void set_ConstructorCache(com.tangosol.util.SafeHashMap map)
        {
        __s__ConstructorCache = map;
        }
    
    // Accessor for the property "_ConstructorParams"
    /**
     * Setter for property _ConstructorParams.<p>
     */
    private static void set_ConstructorParams(Object[] ao)
        {
        __s__ConstructorParams = ao;
        }
    
    // Accessor for the property "_ConstructorParamTypes"
    /**
     * Setter for property _ConstructorParamTypes.<p>
     */
    private static void set_ConstructorParamTypes(Class[] aclz)
        {
        __s__ConstructorParamTypes = aclz;
        }
    
    // Accessor for the property "_Feed"
    /**
     * Setter for property _Feed.<p>
    * Property used by Component Integration. This property is not meant to be
    * designed directly.
     */
    public void set_Feed(Object pFeed)
        {
        __m__Feed = pFeed;
        }
    
    // Accessor for the property "_Order"
    /**
     * Setter for property _Order.<p>
    * Property used to control the order of children components within their
    * parent. The type of this property is "float" which allows insertion a new
    * child at derivation or modification level before, after or between any
    * sibling components regardless of the values of their _Order properties.
    * Components with _Order property equal to 0.0 are considered to be "order
    * tolerant".
     */
    public void set_Order(float p_Order)
        {
        __m__Order = p_Order;
        }
    
    // Accessor for the property "_Parent"
    /**
     * Setter for property _Parent.<p>
    * Parent component for this component. This property is not meant to be
    * designed directly.
     */
    private void set_Parent(Component pParent)
        {
        __m__Parent = pParent;
        }
    
    // Accessor for the property "_Reference"
    /**
     * Setter for property _Reference.<p>
    * The _Reference property is designed to maintain a reference to an
    * application component until the very last component is garbage-collected.
    * The _Reference property is static and located on the root component.  Any
    * other case in which a reference must be maintained should be done by
    * having the singleton Application hold that reference.  In other words, as
    * long as the Application instance doesn't go away, any reference that it
    * holds won't go away either.
    * 
    * @see Application#get_Instance
     */
    protected static void set_Reference(Component ref)
        {
        __s__Reference = ref;
        }
    
    // Accessor for the property "_Sink"
    /**
     * Setter for property _Sink.<p>
    * Property used by Component Integration. This property is not meant to be
    * designed directly.
     */
    public void set_Sink(Object pSink)
        {
        // import com.tangosol.run.component.CallbackSink;
        
        __m__Sink = (pSink);
        
        // the following is necessary for the situation
        // when the component was created by the feed
        // (feed is being instantiated as a Javabean)
        if (pSink instanceof CallbackSink)
            {
            set_Feed(((CallbackSink) pSink).get_Feed());
            }
        }
    
    // Accessor for the property "_State"
    /**
     * Setter for property _State.<p>
    * Property with private accessors used by _Constructed and _Deserialized
    * properties. This property is not meant to be designed.
     */
    private void set_State(int pState)
        {
        __m__State = pState;
        }
    
    // Accessor for the property "_StateAux"
    /**
     * Setter for property _StateAux.<p>
    * An auxiliary functional property that allows subcomponents to use the
    * non-utilized portion of the _State property. The root Component uses only
    * the two high bits, leaving low 30 bits for subcomponents' needs.
    * 
    * This property is not meant to be designed.
    * 
    * @functional
     */
    protected void set_StateAux(int nState)
        {
        set_State((get_State() & ~_STATE_AUX_MASK) | (nState & _STATE_AUX_MASK));
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.run.xml.XmlSerializable;
        
        StringBuilder sb = new StringBuilder("Component: ");
        
        sb.append(getClass().getName())
          .append('(')
          .append(get_Name())
          .append(')');
          
        if (this instanceof XmlSerializable)
            {
            XmlElement xml = ((XmlSerializable) this).toXml();
            if (xml != null)
                {
                sb.append('\n')
                  .append(xml);
                }
            }
        else
            {
            sb.append('@')
              .append(System.identityHashCode(this));
            }
        return sb.toString();
        }
    
    // From interface: java.io.ObjectInputValidation
    /**
     * The "InputObjectValidation" interface is used to set the "_Constructed"
    * property, that in turn triggers "onInit()" notification.
     */
    public final void validateObject()
            throws java.io.InvalidObjectException
        {
        set_State(get_State() | _STATE_DESERIALIZED);
        set_Constructed(true);
        }
    
    /**
     * This method is a part of "Serializable" pseudo-interface and is used to
    * hook-up the validation callback.
    * 
    * @see #validateObject
     */
    private void writeObject(java.io.ObjectOutputStream out)
            throws java.io.IOException
        {
        out.defaultWriteObject();
        }
    }
