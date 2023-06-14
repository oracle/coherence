
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.Application

package com.tangosol.coherence.component;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.application.DefaultApplication;
import com.tangosol.coherence.component.util.Config;
import com.tangosol.util.Base;
import com.tangosol.util.WrapperException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Application
        extends    com.tangosol.coherence.Component
        implements Runnable
    {
    // ---- Fields declarations ----
    
    /**
     * Property Argument
     *
     * Command-line arguments.  The property is static so that it can be
     * accessed before an application is instantiated.  One example is when the
     * application name that will be instantiated is an argument.
     */
    private static transient String[] __s_Argument;
    
    /**
     * Property Context
     *
     * The Java Naming and Directory Interface Context interface to use in this
     * run-time.  This field will auto-initialize to the
     * javax.naming.InitialContext.
     * 
     * This value is used by remote object initiailization to find the remote
     * object associated with the local object.
     * 
     * @see Component#_makeRemoteObject
     * @see Component#get_RemoteContext
     */
    private transient javax.naming.Context __m_Context;
    
    /**
     * Property FILE_CFG_INITIAL
     *
     * File location for initial configuration information.
     * 
     * Note that the forward slash is prescribed by the JAR format and does not
     * violate the 100% Pure Java standard.
     */
    public static final String FILE_CFG_INITIAL = "tangosol-application";
    
    /**
     * Property HostConfig
     *
     * The application configuration information that applies to the host that
     * the application is running on, and may be updated each time the
     * application runs or may just contain information necessary for the
     * application to be run on the host.  Note that this information is
     * host-specific, not user-specific.
     */
    private transient com.tangosol.coherence.component.util.Config __m_HostConfig;
    
    /**
     * Property InitialConfig
     *
     * The application configuration information that was deployed with the
     * application and is probably not ever updated after the application is
     * deployed.  (In other words, these settings may have come from the
     * developers of the application, or at the latest, may have been
     * configured when the application was deployed to a site.)
     * 
     * Examples include license information and names of database servers used
     * by the application.
     * 
     * The InitialConfig property must be static so that it can be obtained
     * before the application is instantiated.  One example is when the
     * application name that will be instantiated is part of the initial
     * configuration.
     */
    private static transient com.tangosol.coherence.component.util.Config __s_InitialConfig;
    
    /**
     * Property Locale
     *
     * The current user's Locale.
     */
    private java.util.Locale __m_Locale;
    
    /**
     * Property NamedReferences
     *
     */
    private transient com.tangosol.util.ListMap __m_NamedReferences;
    
    /**
     * Property Principal
     *
     * The current user's Principal information.
     */
    private java.security.Principal __m_Principal;
    
    /**
     * Property References
     *
     */
    private transient java.util.LinkedList __m_References;
    
    /**
     * Property TransientConfig
     *
     * The application configuration information that applies to the currently
     * running application and is probably not intended to be persisted.
     */
    private transient com.tangosol.coherence.component.util.Config __m_TransientConfig;
    
    /**
     * Property UserConfig
     *
     * The configuration information for the current user.  Unfortunately,
     * "user" means different things for different types of applications. 
     * There may be a single user for the duration of a desktop GUI
     * application, while there may be multitudes of dudes using a website,
     * many of which may be anonymous.  The implementation of obtaining
     * user-specific configuration information is left as an exercise for the
     * reader.
     */
    private transient com.tangosol.coherence.component.util.Config __m_UserConfig;
    protected static com.tangosol.coherence.Component __singleton;
    
    // Initializing constructor
    public Application(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_NamedReferences = new com.tangosol.util.ListMap();
            __m_References = new java.util.LinkedList();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    //++ getter for static property _Instance
    private static com.tangosol.coherence.Component get_Instance$Default()
        {
        com.tangosol.coherence.Component singleton = __singleton;
        
        return singleton;
        }
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
        // import Component;
        // import Component.Application.DefaultApplication;
        // import Component.Util.Config;
        
        Application app = (Application) get_Instance$Default();
        
        if (app == null)
            {
            synchronized (get_CLASS())
                {
                app = (Application) get_Instance$Default();
                if (app == null)
                    {
                    // _Reference value refers to the first component to be instantiated
                    // (the "entry point" for the application) so let that component
                    // instantiate an application if it knows how
                    Component component = get_Reference();
                    if (component != null)
                        {
                        app = component._makeApplication();
                        }
        
                    if (app == null)
                        {
                        // the next two steps will try to determine the application name
                        String sApp = null;
        
                        // check if the application name is a command-line parameter
                        String[] asArgs = getArgument();
                        if (asArgs != null && asArgs.length > 0)
                            {
                            for (int i = 0, c = asArgs.length; i < c; ++i)
                                {
                                String[] asPair = parseArgument(asArgs[i]);
                                if (asPair != null && asPair[0].compareToIgnoreCase("app") == 0)
                                    {
                                    sApp = asPair[1];
                                    if (sApp != null && sApp.length() == 0)
                                        {
                                        sApp = null;
                                        }
                                    }
                                }
                            }
        
                        // check if the application name is an application deployment property
                        if (sApp == null)
                            {
                            Config config = getInitialConfig();
                            if (config != null)
                                {
                                sApp = config.getString("Application");
                                if (sApp != null && sApp.length() == 0)
                                    {
                                    sApp = null;
                                    }
                                }
                            }
        
                        // if an application name was found, instantiate it
                        if (sApp != null)
                            {
                            if (!sApp.contains("Application."))
                                {
                                sApp = "Application." + sApp;
                                }
                            if (!sApp.contains("Component."))
                                {
                                sApp = "Component." + sApp;
                                }
                            app = (Application) _newInstance(sApp);
                            }
                        }
        
                    // no clue about what application component to instantiate so use
                    // the default application component
                    if (app == null)
                        {
                        System.out.println("Failed to find an application descriptor -- " +
                            "loading DefaultApplication");
                        app = (Application) DefaultApplication.get_Instance();
                        }
                    }
                }
            }
        
        return app;
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return Application.class;
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
    
    /**
     * Store a reference in the application so that the referenced object will
    * not be garbage-collected.  The application component is designed such
    * that it will not be garbage-collected for the duration of the
    * application.  (See the documentation for get_Instance on Application for
    * details.)  As a result, any object that is referenced from an application
    * will not be garbage-collected either.  This method is used to add an
    * anonymous reference from the application to an object, and is handy for
    * keeping singletons (components marked as static) alive.  For example:
    * 
    * // the get_Instance implementation on an abstract static component
    * Component.Some.Singleton
    * Singleton s = super.get_Instance();
    * if (s == null)
    *     {
    *     // use some factory method
    *     s = instantiateSingleton();
    *     Component.Application.get_Instance().addReference(s);
    *     }
    * return s;
    * 
    * The above code guarantees that for the life of the application, the
    * Singleton.get_Instance will return the same object, because it cannot be
    * garbage-collected (a reference is held by the application) and
    * get_Instance on a static component will prevent more than one from being
    * instantiated (commonly known as "the singleton pattern".)
    * 
    * To release the reference that the application holds, use removeReference
    * passing the same reference that was passed to addReference.
    * 
    * @param oRef  the reference for the application to hold
    * 
    * @see #putReference
    * @see #getReference
    * @see #removeReference
    * @see #addReference
    * @see #removeReference
     */
    public boolean addReference(Object oRef)
        {
        return getReferences().add(oRef);
        }
    
    /**
     * Prints out the specified message according to the Application context. 
    * Derived applications should provide an appropriate implementation for
    * this method if the output should not be sent to the standard output and
    * error output.  For example, if an application wanted to log output, this
    * would be the method to override.
    * 
    * @param message  the text message to display
    * @param severity  0 for informational, ascending for more serious output
    * (the default implementation assumes anything not 0 is an error and should
    * be printed to the system error stream)
     */
    public void debugOutput(String message, int severity)
        {
        // derived components must override this if either System.out or System.err
        // is not available
        if (severity == 0)
            {
            System.out.println(message);
            }
        else
            {
            System.err.println(message);
            debugSound();    
            }
        }
    
    /**
     * Prints out the specified exception according to the Application context. 
    * Derived applications should provide an appropriate implementation for
    * this method if the output should not be sent to the standard output and
    * error output. 
    * 
    * @param e  the exception to print
     */
    public void debugOutput(Throwable e)
        {
        debugOutput(getStackTrace(e), 1);
        }
    
    /**
     * Produces a sound. Derived components should provide an appropriate
    * implementation for this method.  For example, graphical applications can
    * use the AWT toolkit to produce a beep.  Server applications may not have
    * any way of beeping (or no one to hear it if they can).
     */
    public void debugSound()
        {
        // derived components must override this if System.out is not available
        System.out.println("Beep!" + (char) 7);
        }
    
    // Declared at the super level
    /**
     * Returns the remote [JNDI] name to use when finding the remote object
    * associated with this local component. Remotable sub-components may
    * override this method returning appropriate names.  If null is returned,
    * the component will only operate in the local mode.
     */
    public String get_RemoteName()
        {
        return super.get_RemoteName();
        }
    
    // Accessor for the property "Argument"
    /**
     * Getter for property Argument.<p>
    * Command-line arguments.  The property is static so that it can be
    * accessed before an application is instantiated.  One example is when the
    * application name that will be instantiated is an argument.
     */
    protected static String[] getArgument()
        {
        return __s_Argument;
        }
    
    // Accessor for the property "Argument"
    /**
     * Getter for property Argument.<p>
    * Command-line arguments.  The property is static so that it can be
    * accessed before an application is instantiated.  One example is when the
    * application name that will be instantiated is an argument.
     */
    public static String getArgument(int i)
        {
        return getArgument()[i];
        }
    
    // Accessor for the property "ArgumentCount"
    /**
     * @return the number of arguments passed to the application in the command
    * line
     */
    public static int getArgumentCount()
        {
        String[] asArgs = getArgument();
        return asArgs == null ? 0 : asArgs.length;
        }
    
    // Accessor for the property "Context"
    /**
     * Returns the current naming context. If not specified, initializes it with
    *  javax.naming.InitialContext object.
    * 
    * Derived application components may want to initialize the Context in
    * other ways, e.g., using an applicaiton-specific configuration file, or
    * application server context information.
     */
    public javax.naming.Context getContext()
        {
        // import javax.naming.Context;
        
        Context context = __m_Context;
        
        if (context == null)
            {
            setContext(context = instantiateInitialContext());
            }
        
        return context;
        }
    
    // Accessor for the property "FILE_CFG_HOST"
    /**
     * Getter for property FILE_CFG_HOST.<p>
     */
    protected String getFILE_CFG_HOST()
        {
        return get_Name();
        }
    
    // Accessor for the property "HostConfig"
    /**
     * Getter for property HostConfig.<p>
    * The application configuration information that applies to the host that
    * the application is running on, and may be updated each time the
    * application runs or may just contain information necessary for the
    * application to be run on the host.  Note that this information is
    * host-specific, not user-specific.
     */
    public com.tangosol.coherence.component.util.Config getHostConfig()
        {
        // import Component.Util.Config;
        
        Config config = __m_HostConfig;
        
        if (config == null)
            {
            config = instantiateHostConfig();
            setHostConfig(config);
            }
        
        return config;
        }
    
    // Accessor for the property "InitialConfig"
    /**
     * Getter for property InitialConfig.<p>
    * The application configuration information that was deployed with the
    * application and is probably not ever updated after the application is
    * deployed.  (In other words, these settings may have come from the
    * developers of the application, or at the latest, may have been configured
    * when the application was deployed to a site.)
    * 
    * Examples include license information and names of database servers used
    * by the application.
    * 
    * The InitialConfig property must be static so that it can be obtained
    * before the application is instantiated.  One example is when the
    * application name that will be instantiated is part of the initial
    * configuration.
     */
    public static com.tangosol.coherence.component.util.Config getInitialConfig()
        {
        // import Component.Util.Config;
        
        Config config = __s_InitialConfig;
        
        if (config == null)
            {
            config = instantiateInitialConfig();
            setInitialConfig(config);
            }
        
        return config;
        }
    
    // Accessor for the property "Locale"
    /**
     * Getter for property Locale.<p>
    * The current user's Locale.
     */
    public java.util.Locale getLocale()
        {
        // import java.util.Locale;
        
        Locale locale = __m_Locale;
        if (locale == null)
            {
            locale = Locale.getDefault();
            }
        
        return locale;
        }
    
    // Accessor for the property "NamedReferences"
    /**
     * Getter for property NamedReferences.<p>
     */
    private com.tangosol.util.ListMap getNamedReferences()
        {
        return __m_NamedReferences;
        }
    
    // Accessor for the property "Principal"
    /**
     * Getter for property Principal.<p>
    * The current user's Principal information.
     */
    public java.security.Principal getPrincipal()
        {
        return __m_Principal;
        }
    
    /**
     * Obtain a string property value by its name.  The default implementation
    * checks the application's runtime properties (including command line
    * parameters), then Java System properties, then user properties, then host
    * properties, then the application deployment properties.
    * 
    * Applications should typically not use System properties directly, but
    * rather use the getProperty(sName) and setProperty(sName, sValue) methods
    * on the Application component.  The implementation of these methods allows
    * information to be coallesced together, including System properties,
    * host-, user-, and application-specific information.  Furthermore,
    * potential security problems with System properties are hidden, and values
    * of System properties can be hidden by overriding them for the purposes of
    * this application without modifying them (or having to have security
    * rights to modify them) on the System component itself.  This can be very
    * important for hosted applications, such as web sites or EJB libraries.
    * 
    * @param sName the name of the property
    * 
    * @return the string value of the property or null if the property has not
    * been set
    * 
    * @see #setProperty
     */
    public String getProperty(String sName)
        {
        // import Component.Util.Config;
        
        // check runtime properties maintained by the application component
        Config config = getTransientConfig();
        if (config != null)
            {
            String sValue = config.getString(sName);
            if (sValue != null)
                {
                return sValue;
                }
            }
        
        // check optional user-specific properties
        config = getUserConfig();
        if (config != null)
            {
            String sValue = config.getString(sName);
            if (sValue != null)
                {
                return sValue;
                }
            }
        
        // check system properties (if the system permits it)
        try
            {
            String sValue = com.tangosol.coherence.config.Config.getProperty(sName);
            if (sValue != null)
                {
                return sValue;
                }
            }
        catch (SecurityException e)
            {
            // those grapes were probably sour anyway
            }
        
        // check optional host-specific properties
        config = getHostConfig();
        if (config != null)
            {
            String sValue = config.getString(sName);
            if (sValue != null)
                {
                return sValue;
                }
            }
        
        // check persistent application properties
        config = getInitialConfig();
        if (config != null)
            {
            String sValue = config.getString(sName);
            if (sValue != null)
                {
                return sValue;
                }
            }
        
        return null;
        }
    
    /**
     * Obtains the reference that was previously stored in the application
    * associated with the passed name.
    * 
    * @param sName  the name that was previouly supplied to putReference
    * 
    * @return the object that was previously supplied to putReference with the
    * specified name
    * 
    * @see #putReference
    * @see #getReference
    * @see #removeReference
    * @see #addReference
    * @see #removeReference
     */
    public Object getReference(String sName)
        {
        return getNamedReferences().get(sName);
        }
    
    // Accessor for the property "References"
    /**
     * Getter for property References.<p>
     */
    private java.util.LinkedList getReferences()
        {
        return __m_References;
        }
    
    /**
     * Finds an application resource with a given name for a specified class. 
    * This method returns null if no resource with this name is found.  The
    * rules for searching resources associated with the specified class are
    * implemented by the defining class loader of the class. If no class is
    * specified, the system class loader is asked.
    * 
    * Example of use:
    *     Application app =(Application) Application.get_Instance();
    *     URL url = app.getResource(sName);
    * 
    * @param sName  the name of the resource
    * 
    * @return URL for the specified resource of null if the resource cannot be
    * found
    * 
    * @see java.lang.Class#getResource(String)
     */
    public java.net.URL getResource(String sName)
        {
        return getClass().getResource(resolveResourceName(sName));
        }
    
    /**
     * Finds a resource with a given name for a specified class.  This method
    * returns null if no resource with this name is found.  The rules for
    * searching resources associated with the specified class are implemented
    * by the defining class loader of the class. If no class is specified, the
    * system class loader is asked.
    * 
    * Example of use:
    *     Application app =(Application) Application.get_Instance();
    *     InputStream stream = app.getResourceAsStream(sName, get_CLASS());
    * 
    * @param sName  the name of the resource
    * @param clazz  (optional) the class that is looking for the resource
    * 
    * @return InputStream for the specified resource of null if the resource
    * cannot be found
    * 
    * @see java.lang.Class#getResourceAsStream(String)
     */
    public java.io.InputStream getResourceAsStream(String sName)
        {
        return getClass().getResourceAsStream(resolveResourceName(sName));
        }
    
    // Accessor for the property "TransientConfig"
    /**
     * Getter for property TransientConfig.<p>
    * The application configuration information that applies to the currently
    * running application and is probably not intended to be persisted.
     */
    public com.tangosol.coherence.component.util.Config getTransientConfig()
        {
        // import Component.Util.Config;
        
        Config config = __m_TransientConfig;
        
        if (config == null)
            {
            config = instantiateTransientConfig();
            setTransientConfig(config);
            }
        
        return config;
        }
    
    // Accessor for the property "UserConfig"
    /**
     * Getter for property UserConfig.<p>
    * The configuration information for the current user.  Unfortunately,
    * "user" means different things for different types of applications.  There
    * may be a single user for the duration of a desktop GUI application, while
    * there may be multitudes of dudes using a website, many of which may be
    * anonymous.  The implementation of obtaining user-specific configuration
    * information is left as an exercise for the reader.
     */
    public com.tangosol.coherence.component.util.Config getUserConfig()
        {
        return __m_UserConfig;
        }
    
    protected com.tangosol.coherence.component.util.Config instantiateHostConfig()
        {
        // default implementation:  load from the <appname>.properties file
        return loadConfig(getFILE_CFG_HOST());
        }
    
    protected static com.tangosol.coherence.component.util.Config instantiateInitialConfig()
        {
        // import Component.Util.Config;
        // import java.io.InputStream;
        // import java.io.IOException;
        
        Config config = new Config();
        
        // load initial configuration from preset location
        Class       clz = Application.class;
        String      s   = Config.resolvePath(FILE_CFG_INITIAL);
        InputStream in  = clz.getResourceAsStream(s);
        if (in == null)
            {
            s  = Config.resolveName(FILE_CFG_INITIAL);
            in = clz.getResourceAsStream(s);
            }
        
        if (in != null)
            {
            try
                {
                config.load(in);
                }
            catch (IOException e)
                {
                // do not use _trace (Application may not yet be instantiated)
                System.err.println("Exception loading configuration:");
                e.printStackTrace(System.err);
                }
            finally
                {
                try
                    {
                    in.close();
                    }
                catch (IOException e) {}
                }
            }
        
        return config;
        }
    
    protected javax.naming.Context instantiateInitialContext()
        {
        // import com.tangosol.util.WrapperException;
        // import javax.naming.InitialContext;
        // import javax.naming.NamingException;
        
        try
            {
            return new InitialContext();
            }
        catch (NamingException e)
            {
            throw new WrapperException(e);
            }
        }
    
    protected com.tangosol.coherence.component.util.Config instantiateTransientConfig()
        {
        // import Component.Util.Config;
        
        // default implementation:  no default information in the transient config
        return new Config();
        }
    
    /**
     * Check whether or not the debug output for the specified severity level is
    * enabled according to the Application context.
     */
    public boolean isDebugOutputEnabled(int iSeverity)
        {
        return true;
        }
    
    /**
     * Loads a named configuration file from the application's JAR or from the
    * file-system.
    * 
    * Note that a non-null Config is always returned.  Use Config.isEmpty to
    * determine if no configuration data was loaded.
    * 
    * Example:
    * Application app = (Application) Application.get_Instance();
    * Config cfg = app.loadConfig("custom");
    * if (!cfg.isEmpty())
    *     {
    *     ...
    *     }
    * 
    * @param sName  the name of the information (wihout the configuration file
    * extension)
    * 
    * @return an instance of Component.Util.Config
     */
    public com.tangosol.coherence.component.util.Config loadConfig(String sName)
        {
        // import Component.Util.Config;
        
        Config config = new Config();
        config.load(sName);
        return config;
        }
    
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
        // store off the arguments
        setArgument(asArgs);
        
        ((Runnable) get_Instance()).run();
        }
    
    /**
     * Process an assertion failure according to the Application context.
    * 
    * @see Component#_assertFailed
     */
    public void onError(Throwable exception)
        {
        // import com.tangosol.util.Base;
        
        throw Base.ensureRuntimeException(exception);
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
        // import Component.Util.Config;
        
        // see the documentation/implementation for Application.get_Instance() for
        // an explanation of why this assertion must hold true
        _assert(get_Reference() == null
           ||   get_Reference() == this
           || !(get_Reference() instanceof Application));
        
        // store a reference to the application on the root component to prevent
        // garbage collection of the application instance
        set_Reference(this);
        
        super.onInit();
        
        // configure runtime properties from the command line arguments
        String[] asArgs = getArgument();
        if (asArgs != null && asArgs.length > 0)
            {
            Config config = getTransientConfig();
            for (int i = 0, c = asArgs.length; i < c; ++i)
                {
                String[] asPair = parseArgument(asArgs[i]);
                if (asPair != null)
                    {
                    config.putString(asPair[0], asPair[1]);
                    }
                }
            }
        
        // configure the application using properties from the deployment
        // configuration file
        Config config = getInitialConfig();
        if (config != null)
            {
            applyConfig(config, "Application");
            }
        }
    
    /**
     * Parses an argument into a name/value pair and returns the name and value
    * in a 2-element String array.
    * 
    * @param sArg  the argument value
    * 
    * @return a String array containing [0]=name and [1]=value, or null if the
    * argument is not a name/value pair
     */
    protected static String[] parseArgument(String sArg)
        {
        String[] asPair = null;
        
        if (sArg != null && sArg.length() > 0)
            {
            // look for optional "-"
            int ofStartName = sArg.charAt(0) == '-' ? 1 : 0;
            int ofDelim     = sArg.indexOf('=');
            if (ofDelim < 0)
                {
                ofDelim = sArg.indexOf(':');
                }
        
            if (ofDelim > ofStartName)
                {
                String sName  = sArg.substring(ofStartName, ofDelim).trim();
                if (sName.length() > 0)
                    {
                    String sValue = ofDelim == sArg.length() - 1 ? ""
                                  : sArg.substring(ofDelim + 1).trim();
        
                    asPair = new String[2];
                    asPair[0] = sName;
                    asPair[1] = sValue;
                    }
                }
            }
        
        return asPair;
        }
    
    /**
     * Store a reference from the application to an object, allowing it to be
    * later obtained by its name using getReference(sName), and preventing it
    * from being garbage-collected until it is discarded by
    * removeReference(sName).
    * 
    * @param sName  the name to associate with the specified reference
    * @param oRef  the object to store the specified name
    * 
    * @return the previous value, if any, associated with the specified name
    * 
    * @see #putReference
    * @see #getReference
    * @see #removeReference
    * @see #addReference
    * @see #removeReference
     */
    public Object putReference(String sName, Object oRef)
        {
        return getNamedReferences().put(sName, oRef);
        }
    
    /**
     * Releases an anonymous reference.  Corresponds to addReference(oRef).
    * 
    * @param  oRef  the reference previously passed to addReference(oRef)
    * 
    * @return  true if the reference was being held by the application, false
    * otherwise
    * 
    * @see #putReference
    * @see #getReference
    * @see #removeReference
    * @see #addReference
    * @see #removeReference
     */
    public boolean removeReference(Object oRef)
        {
        return getReferences().remove(oRef);
        }
    
    /**
     * Releases a named reference.  Corresponds to putReference(sName, oRef).
    * 
    * @param sName  the name passed to putReference(sName, oRef) when the
    * reference was stored
    * 
    * @return oRef  the reference held by the application and associated with
    * the specified name, or null if the application did not hold any reference
    * by that name
    * 
    * @see #putReference
    * @see #getReference
    * @see #removeReference
    * @see #addReference
    * @see #removeReference
     */
    public Object removeReference(String sName)
        {
        return getNamedReferences().remove(sName);
        }
    
    /**
     * Adds a "resource" prefix if the name is not absolute.
     */
    public String resolveResourceName(String sName)
        {
        if (!sName.startsWith("/"))
            {
            String sThisClass = get_CLASS().getName();
            int    ofPkg      = sThisClass.indexOf(".component.");
            String sPackage   = sThisClass.substring(0, ofPkg);
            sName = '/' + sPackage.replace('.', '/') + '/' + sName;
            }
        
        return sName;
        }
    
    // From interface: java.lang.Runnable
    public void run()
        {
        }
    
    // Accessor for the property "Argument"
    /**
     * Setter for property Argument.<p>
    * Command-line arguments.  The property is static so that it can be
    * accessed before an application is instantiated.  One example is when the
    * application name that will be instantiated is an argument.
     */
    protected static void setArgument(String[] asArgs)
        {
        __s_Argument = asArgs;
        }
    
    // Accessor for the property "Argument"
    /**
     * Setter for property Argument.<p>
    * Command-line arguments.  The property is static so that it can be
    * accessed before an application is instantiated.  One example is when the
    * application name that will be instantiated is an argument.
     */
    protected static void setArgument(int i, String sArg)
        {
        getArgument()[i] = sArg;
        }
    
    // Accessor for the property "Context"
    /**
     * Setter for property Context.<p>
    * The Java Naming and Directory Interface Context interface to use in this
    * run-time.  This field will auto-initialize to the
    * javax.naming.InitialContext.
    * 
    * This value is used by remote object initiailization to find the remote
    * object associated with the local object.
    * 
    * @see Component#_makeRemoteObject
    * @see Component#get_RemoteContext
     */
    public void setContext(javax.naming.Context ctx)
        {
        __m_Context = ctx;
        }
    
    // Accessor for the property "HostConfig"
    /**
     * Setter for property HostConfig.<p>
    * The application configuration information that applies to the host that
    * the application is running on, and may be updated each time the
    * application runs or may just contain information necessary for the
    * application to be run on the host.  Note that this information is
    * host-specific, not user-specific.
     */
    protected void setHostConfig(com.tangosol.coherence.component.util.Config config)
        {
        __m_HostConfig = config;
        }
    
    // Accessor for the property "InitialConfig"
    /**
     * Setter for property InitialConfig.<p>
    * The application configuration information that was deployed with the
    * application and is probably not ever updated after the application is
    * deployed.  (In other words, these settings may have come from the
    * developers of the application, or at the latest, may have been configured
    * when the application was deployed to a site.)
    * 
    * Examples include license information and names of database servers used
    * by the application.
    * 
    * The InitialConfig property must be static so that it can be obtained
    * before the application is instantiated.  One example is when the
    * application name that will be instantiated is part of the initial
    * configuration.
     */
    public static void setInitialConfig(com.tangosol.coherence.component.util.Config config)
        {
        __s_InitialConfig = config;
        }
    
    // Accessor for the property "Locale"
    /**
     * Setter for property Locale.<p>
    * The current user's Locale.
     */
    public void setLocale(java.util.Locale locale)
        {
        __m_Locale = locale;
        }
    
    // Accessor for the property "NamedReferences"
    /**
     * Setter for property NamedReferences.<p>
     */
    private void setNamedReferences(com.tangosol.util.ListMap map)
        {
        __m_NamedReferences = map;
        }
    
    // Accessor for the property "Principal"
    /**
     * Setter for property Principal.<p>
    * The current user's Principal information.
     */
    public void setPrincipal(java.security.Principal principal)
        {
        __m_Principal = principal;
        }
    
    /**
     * Store the specified name/value pair as part of the application's runtime
    * properties.  The property is later obtainable using getProperty(sName). 
    * The application's runtime properties over-ride all other properties in
    * the order the properties are searched by getProperty(sName).  See
    * getProperty(sName) for more details.
    * 
    * Applications should typically not use System properties directly, but
    * rather use the getProperty(sName) and setProperty(sName, sValue) methods
    * on the Application component.  The implementation of these methods allows
    * information to be coallesced together, including System properties,
    * host-, user-, and application-specific information.  Furthermore,
    * potential security problems with System properties are hidden, and values
    * of System properties can be hidden by overriding them for the purposes of
    * this application without modifying them (or having to have security
    * rights to modify them) on the System component itself.  This can be very
    * important for hosted applications, such as web sites or EJB libraries.
    * 
    * @param sName  the name of the property
    * @param sValue  the value of the property to store associated with the
    * specified name
    * 
    * @see #getProperty
     */
    public void setProperty(String sName, String sValue)
        {
        getTransientConfig().putString(sName, sValue);
        }
    
    // Accessor for the property "References"
    /**
     * Setter for property References.<p>
     */
    private void setReferences(java.util.LinkedList list)
        {
        __m_References = list;
        }
    
    // Accessor for the property "TransientConfig"
    /**
     * Setter for property TransientConfig.<p>
    * The application configuration information that applies to the currently
    * running application and is probably not intended to be persisted.
     */
    protected void setTransientConfig(com.tangosol.coherence.component.util.Config config)
        {
        __m_TransientConfig = config;
        }
    
    // Accessor for the property "UserConfig"
    /**
     * Setter for property UserConfig.<p>
    * The configuration information for the current user.  Unfortunately,
    * "user" means different things for different types of applications.  There
    * may be a single user for the duration of a desktop GUI application, while
    * there may be multitudes of dudes using a website, many of which may be
    * anonymous.  The implementation of obtaining user-specific configuration
    * information is left as an exercise for the reader.
     */
    protected void setUserConfig(com.tangosol.coherence.component.util.Config config)
        {
        __m_UserConfig = config;
        }
    }
