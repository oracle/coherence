
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.application.console.TestEnvironment

package com.tangosol.coherence.component.application.console;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Stand-alone application that prints JVM's environmental settings.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TestEnvironment
        extends    com.tangosol.coherence.component.application.Console
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public TestEnvironment()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TestEnvironment(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        // singleton initialization
        if (__singleton != null)
            {
            throw new IllegalStateException("A singleton for \"TestEnvironment\" has already been set");
            }
        __singleton = this;
        
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
        
        if (singleton == null)
            {
            singleton = new com.tangosol.coherence.component.application.console.TestEnvironment();
            }
        else if (!(singleton instanceof com.tangosol.coherence.component.application.console.TestEnvironment))
            {
            throw new IllegalStateException("A singleton for \"com.tangosol.coherence.component.application.console.TestEnvironment\" has already been set to a different type");
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
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/application/console/TestEnvironment".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * This method is the entry point for executable Java applications.
    * 
    * Certain types of Java applications are started by the JVM invoking the
    * main() method of the entry point class.  The Application component
    * assists in building these types of applications by providing a default
    * implementation for the main() method.  Unfortunately, main() is not
    * virtual (it must be static) so an application must either override main()
    * or provide configuration information so that the default main()
    * implementation can determine the identity of the entry point class.  For
    * example, the following is a script that an application
    * (Component.Application.Console.HelloWorld) could use to ensure that the
    * HelloWorld application is instantiated:
    * 
    *     // instantiate HelloWorld
    *     get_Instance();
    *     // use the default main() implementation provided by
    * Component.Application
    *     super.main(asArgs);
    * 
    * To avoid creating the script on HelloWorld.main(), and if the application
    * were jar'd, the META-INF directory in the .jar file would contain the
    * following:
    * 
    *     # -- contents of META-INF/MANIFEST.MF
    *     Main-Class:com.tangosol.coherence.Component.Application.Console.HelloWorld
    * 
    *     # -- contents of META-INF/application.properties --
    *     app=Console.HelloWorld
    * 
    * The application identity could alternatively be provided on the command
    * line, for example if the application has not been jar'd:
    * 
    *     java com.tangosol.coherence.Component.Application.Console.HelloWorld
    * -app=Console.HelloWorld
    * 
    * The default implementation (Application.main) stores the arguments for
    * later use in the indexed Argument property, instantiates the application
    * (if an instance does not already exist), and invokes the run() method of
    * the application instance.  It is expected that application implementors
    * will provide an implementation for run() and not for main().
    * 
    * Note that "com.tangosol.coherence." is a place-holder for a deployer-specified package
    * name, for example "com.mycompany.myapplication".  The Packaging Wizard
    * allows the deployer to specify the package into which the application
    * will be deployed.  The above examples would have to be changed
    * accordingly.
    * 
    * @param asArgs  an array of string arguments
    * 
    * @see #get_Instance
     */
    public static void main(String[] asArgs)
        {
        setArgument(asArgs);
        get_Instance();
        com.tangosol.coherence.component.Application.main(asArgs);
        }
    
    // Declared at the super level
    public void run()
        {
        traceArguments();
        traceSystemProperties();
        traceConfig();
        }
    
    public void traceArguments()
        {
        _trace("");
        _trace("*** Arguments:");
        String[] asArgs = getArgument();
        for (int i = 0, c = asArgs == null ? 0 : asArgs.length; i < c; ++i)
            {
            _trace("[" + i + "]=\"" + asArgs[i] + "\"");
            String[] a = parseArgument(asArgs[i]);
            if (a != null)
                {
                _trace(" --> \"" + a[0] + "\"=\"" + a[1] + "\"");
                }
            if (a[0].equals("resource"))
                {
                _trace("resource " + a[0] + " -> " + getResource(a[1]));
                }
            else if (a[0].equals("class"))
                {
                try
                    {
                    _trace("loader " + getClass().getClassLoader());
                    _trace("class " + a[1] + " -> " +
                        getClass().getClassLoader().loadClass(a[1]).getClassLoader());
                    }
                catch (Exception e)
                    {
                    _trace(e.toString());
                    }
                }
            }
        }
    
    public void traceConfig()
        {
        if (getTransientConfig() != null)
            {
            _trace("");
            _trace("*** Transient Configuration:");
            getTransientConfig().list();
            }
        
        if (getUserConfig() != null)
            {
            _trace("");
            _trace("*** User Configuration:");
            getUserConfig().list();
            }
        
        if (getHostConfig() != null)
            {
            _trace("");
            _trace("*** Host Configuration:");
            getHostConfig().list();
            }
        
        if (getInitialConfig() != null)
            {
            _trace("");
            _trace("*** Initial Configuration:");
            getInitialConfig().list();
            }
        }
    
    public static void traceSystemProperties()
        {
        // import java.awt.GraphicsEnvironment;
        // import java.awt.Font;
        // import java.util.Arrays;
        // import java.util.Enumeration;
        
        _trace("");
        _trace("*** Properties:");
        java.util.Properties props = System.getProperties();
        
        int      cProps = props.size();
        String[] asProp = new String[cProps];
        
        Enumeration e = props.propertyNames();
        for (int i = 0; e.hasMoreElements(); i++)
            {
            asProp[i] = (String) e.nextElement();
            }
        
        Arrays.sort(asProp);
        
        for (int i = 0; i < cProps; i++)
            {
            _trace(asProp[i] + " = " + props.getProperty(asProp[i]));
            }
        
        if (false)
            {
            _trace("*** FontsFamilyNames:");
            GraphicsEnvironment env =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        
            String[] asFont = env.getAvailableFontFamilyNames();
            for (int i = 0; i < asFont.length; i++)
                {
                _trace(asFont[i]);
                }
        
            if (false)
                {
                _trace("*** Fonts:");
        
                Font[] aFont = env.getAllFonts();
                for (int i = 0; i < aFont.length; i++)
                    {
                    _trace(aFont[i].toString());
                    }
                }
            }
        }
    }
