
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.application.Library

package com.tangosol.coherence.component.application;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Library
        extends    com.tangosol.coherence.component.Application
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public Library(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _Instance
    /**
     * Instantiate an Application component or return the previously
    * instantiated singleton.
    * 
    * The implementation of the get_Instance accessor on the Application
    * component is more complex than that of most other singleton components. 
    * First, it must be able to determine what application to instantiate. 
    * Secondly, it works with the _Reference property of the root component to
    * maintain a reference to the resulting application component until the
    * very last component instance is garbage-collected.
    * 
    * 1)  The _Reference property is static and located on the root component.
    * 2)  The accessor and mutator for the _Reference property are protected
    * and thus the property value can be obtained or modified by any component.
    *  (Specifically, it is set initially by Component.<init>, obtained by
    * Application.get_Instance, and set by Application.onInit.)
    * 3)  Component.<init> (the constructor of the root component) sets the
    * _Reference property to the instance of the component being constructed if
    * and only if the _Reference property is null; this guarantees that a
    * reference to the very first component to be instantiated is initially
    * stored in the _Reference property.
    * 4)  When an application component is instantiated, the Application.onInit
    * method stores a reference to that application in the _Reference property;
    * this ensures that a reference to the application will exist as long as
    * any component instance exists (because the root component class will not
    * be garbage-collectable until all component instances are determined to be
    * garbage-collectable and until all of its sub-classes are determined to be
    * garbage-collectable).  Since Application is a singleton, it is not
    * possible for a second instance of Application to exist, so once an
    * Application component has been instantiated, the _Reference property's
    * value will refer to that application until the root component class is
    * garbage-collected.
    * 5)  When Application.get_Instance() is invoked, if no singleton
    * application (Application.__singleton) has been created, then
    * Application.get_Instance is responsible for instantiating an application,
    * which will result in _Reference being set to the application instance (by
    * way of Application.onInit).
    * 
    * The implementation of Application.get_Instance is expected to take the
    * the following steps in order to instantiate the correct application
    * component:
    * 1) If the value of the _Reference property is non-null, it cannot be an
    * instance of Component.Application, because the __init method of an
    * Application component would have set the Application.__singleton field to
    * reference itself.  Application thus assumes that a non-null _Reference
    * value refers to the first component to be instantiated, and invokes the
    * _makeApplication instance method of that component.  If the return value
    * from _makeApplication is non-null, then it is returned by
    * Application.get_Instance.
    * 2)  Application.get_Instance is now in a catch-22.  No instance of
    * Component.Application exists, which means that the entry point (the
    * initially instantiated) component was not an application component, and
    * the component that was the entry point has denied knowledge of what
    * application should be instantiated.  At this point, the
    * Application.get_Instance method must determine the name of the
    * application component to instantiate without any help.  So it drops back
    * and punts.  First, it checks for an environment setting, then it checks
    * for a properties file setting, and if either one exists, then it
    * instantiates the component specified by that setting and returns it.
    * 3)  Finally, without so much as a clue telling Application.get_Instance
    * what to instantiate, it instantiates the default application context.
    * 
    * Note that in any of the above scenarios, by the time the value is
    * returned by Application.get_Instance, the value of the _Reference
    * property would have been set to the application instance by
    * Application.onInit, thus fulfilling the goal of holding a reference to
    * the application.
    * 
    * Any other case in which a reference must be maintained should be done by
    * having the application hold that reference.  In other words, as long as
    * the application doesn't go away, any reference that it holds won't go
    * away either.
    * 
    * @see Component#_Reference
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        com.tangosol.coherence.Component singleton = __singleton;
        
        if (!(singleton instanceof com.tangosol.coherence.component.application.Library))
            {
            throw new IllegalStateException("A singleton for \"com.tangosol.coherence.component.application.Library\" has already been set to a different type");
            }
        return singleton;
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return Library.class;
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
    }
