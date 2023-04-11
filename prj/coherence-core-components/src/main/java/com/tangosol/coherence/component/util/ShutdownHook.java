
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.ShutdownHook

package com.tangosol.coherence.component.util;

import com.tangosol.net.security.DoAsAction;
import com.tangosol.util.Base;
import java.security.AccessController;

/**
 * Abstract runnable component used as a virtual-machine shutdown hook.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class ShutdownHook
        extends    com.tangosol.coherence.component.Util
        implements Runnable
    {
    // ---- Fields declarations ----
    
    /**
     * Property Graceful
     *
     * Specifies whether or not to perform a graceful shutdown.
     */
    private transient boolean __m_Graceful;
    
    /**
     * Property Thread
     *
     * A thread this ShutdownHook is associated with. Note, that this could be
     * null if the shutdown hook's thread was started by the unregister() call
     * to compensate for a JDK 1.4 bug.
     */
    private transient Thread __m_Thread;
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
        __mapChildren.put("UnregisterAction", ShutdownHook.UnregisterAction.get_CLASS());
        }
    
    // Initializing constructor
    public ShutdownHook(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/ShutdownHook".replace('/', '.'));
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
    
    // Accessor for the property "Thread"
    /**
     * Getter for property Thread.<p>
    * A thread this ShutdownHook is associated with. Note, that this could be
    * null if the shutdown hook's thread was started by the unregister() call
    * to compensate for a JDK 1.4 bug.
     */
    public Thread getThread()
        {
        return __m_Thread;
        }
    
    // Accessor for the property "Graceful"
    /**
     * Getter for property Graceful.<p>
    * Specifies whether or not to perform a graceful shutdown.
     */
    public boolean isGraceful()
        {
        return __m_Graceful;
        }
    
    /**
     * Register itself with Java runtime.
     */
    public void register()
        {
        // import com.tangosol.util.Base;
        
        Thread thread = Base.makeThread(null, this, get_Name());
        
        Runtime.getRuntime().addShutdownHook(thread);
        
        setThread(thread);
        }
    
    // From interface: java.lang.Runnable
    public void run()
        {
        }
    
    // Accessor for the property "Graceful"
    /**
     * Setter for property Graceful.<p>
    * Specifies whether or not to perform a graceful shutdown.
     */
    public void setGraceful(boolean fGraceful)
        {
        __m_Graceful = fGraceful;
        }
    
    // Accessor for the property "Thread"
    /**
     * Setter for property Thread.<p>
    * A thread this ShutdownHook is associated with. Note, that this could be
    * null if the shutdown hook's thread was started by the unregister() call
    * to compensate for a JDK 1.4 bug.
     */
    public void setThread(Thread thread)
        {
        __m_Thread = thread;
        }
    
    /**
     * Unegister itself from Java runtime.
     */
    public void unregister()
        {
        // import com.tangosol.net.security.DoAsAction;
        // import java.security.AccessController;
        
        if (System.getSecurityManager() == null)
            {
            unregisterInternal();
            }
        else
            {
            AccessController.doPrivileged(
                new DoAsAction((ShutdownHook.UnregisterAction) _newChild("UnregisterAction")));
            }
        }
    
    /**
     * Unegister itself from Java runtime.
     */
    public void unregisterInternal()
        {
        Thread thread = getThread();
        if (thread != null)
            {
            setThread(null);
            try
                {
                if (Thread.currentThread() != thread &&
                        Runtime.getRuntime().removeShutdownHook(thread))
                    {
                    // we can only get here if the thread has been successfully unregistered
                    // which means it has not ever been started
                    // in JDK 1.4.x this causes a memory leak (bug id #4533087)
                    thread.start();
                    }
                }
            catch (Throwable e) {}
            }
        }

    // ---- class: com.tangosol.coherence.component.util.ShutdownHook$UnregisterAction
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class UnregisterAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public UnregisterAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public UnregisterAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.ShutdownHook.UnregisterAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/ShutdownHook$UnregisterAction".replace('/', '.'));
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
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            ((ShutdownHook) get_Module()).unregisterInternal();
            return null;
            }
        }
    }
