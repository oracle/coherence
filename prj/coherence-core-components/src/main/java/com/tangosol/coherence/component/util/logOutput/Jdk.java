
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.logOutput.Jdk

package com.tangosol.coherence.component.util.logOutput;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete LogOutput extension that logs messages using the JDK 1.4 logging
 * utility classes (under java.util.logging).
 * 
 * The Jdk LogOutput takes the following configuration parameters:
 * 
 * logger-name:
 *     -the name of the JDK Logger used to log all messages
 * 
 * See the coherence-operational-config.xsd for additional documentation for
 * each of these parameters.
 * 
 * The underlying JDK Logger is configured using the normal JDK logging
 * configuration mechanism.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Jdk
        extends    com.tangosol.coherence.component.util.LogOutput
    {
    // ---- Fields declarations ----
    
    /**
     * Property Level
     *
     * A cache of frequently used JDK Level objects.
     */
    private static java.util.logging.Level[] __s_Level;
    
    /**
     * Property Logger
     *
     * The underlying JDK Logger used to log all messages.
     */
    private java.util.logging.Logger __m_Logger;
    
    private static void _initStatic$Default()
        {
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import java.util.logging.Level;
        
        _initStatic$Default();
        
        Level[] al = new Level[]
            {
            Level.OFF,     // LEVEL_NONE
            Level.ALL,     // LEVEL_ALL
            Level.SEVERE,  // LEVEL_ERROR
            Level.WARNING, // LEVEL_WARNING
            Level.INFO,    // LEVEL_INFO
            Level.CONFIG,  // LEVEL_D4
            Level.FINE,    // LEVEL_D5
            Level.FINER,   // LEVEL_D6
            Level.FINEST,  // LEVEL_D7
            Level.FINEST,  // LEVEL_D8
            Level.FINEST,  // LEVEL_D9
            Level.ALL      // LEVEL_ALL
            };
        
        setLevel(al);
        }
    
    // Default constructor
    public Jdk()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Jdk(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.logOutput.Jdk();
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
            clz = Class.forName("com.tangosol.coherence/component/util/logOutput/Jdk".replace('/', '.'));
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
     * Close the LogOutput and release any resources held by the LogOutput. This
    * method has no effect if the LogOutput has already been closed. Closing a
    * LogOutput makes it unusable. Any attempt to use a closed LogOutput may
    * result in an exception.
     */
    public void close()
        {
        super.close();
        }
    
    // Declared at the super level
    /**
     * Configure a newly created LogOutput instance using the supplied
    * dependencies.
     */
    public void configure(com.tangosol.internal.net.logging.LoggingDependencies deps)
        {
        // import java.util.logging.Logger;
        
        super.configure(deps);
        
        setLogger(Logger.getLogger(deps.getLoggerName()));
        }
    
    // Accessor for the property "Level"
    /**
     * Getter for property Level.<p>
    * A cache of frequently used JDK Level objects.
     */
    protected static java.util.logging.Level[] getLevel()
        {
        return __s_Level;
        }
    
    // Accessor for the property "Level"
    /**
     * Getter for property Level.<p>
    * A cache of frequently used JDK Level objects.
     */
    protected static java.util.logging.Level getLevel(int nIndex)
        {
        return getLevel()[nIndex];
        }
    
    // Accessor for the property "Logger"
    /**
     * Getter for property Logger.<p>
    * The underlying JDK Logger used to log all messages.
     */
    protected java.util.logging.Logger getLogger()
        {
        return __m_Logger;
        }
    
    // Declared at the super level
    /**
     * Return true if this logger is configured to log messages at least of
    * nLevel, where nLevel is translated to an implementation specific level.
     */
    public boolean isEnabled(int nLevel)
        {
        // import java.util.logging.Level;
        
        return getLogger().isLoggable((Level) translateLevel(nLevel));
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, String sMessage)
        {
        // import java.util.logging.Level;
        
        getLogger().log((Level) oLevel, sMessage);
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable)
        {
        // import java.util.logging.Level;
        
        getLogger().log((Level) oLevel, "", throwable);
        }
    
    // Declared at the super level
    /**
     * Log the given Throwable and associated message with the specified log
    * level (specific to the underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable, String sMessage)
        {
        // import java.util.logging.Level;
        
        getLogger().log((Level) oLevel, sMessage, throwable);
        }
    
    // Declared at the super level
    /**
     * Override generic parent method to actually use record object for logging.
     */
    public void log(java.util.logging.LogRecord logRec)
        {
        // import java.util.logging.Level;
        // import java.util.logging.Logger;
        
        Logger logger = getLogger();
        
        // populate the log record with the logger name,
        logRec.setLoggerName(logger.getName());
        
        // the record's level is a Coherence LogLevel, and needs to be "translated"
        Level level = (Level) translateLevel(logRec.getLevel().intValue());
        logRec.setLevel(level);
        
        logger.log(logRec);
        }
    
    // Accessor for the property "Level"
    /**
     * Setter for property Level.<p>
    * A cache of frequently used JDK Level objects.
     */
    protected static void setLevel(java.util.logging.Level[] aLevel)
        {
        __s_Level = aLevel;
        }
    
    // Accessor for the property "Level"
    /**
     * Setter for property Level.<p>
    * A cache of frequently used JDK Level objects.
     */
    protected static void setLevel(int nIndex, java.util.logging.Level level)
        {
        getLevel()[nIndex] = level;
        }
    
    // Declared at the super level
    /**
     * Set the logging level.
     */
    protected void setLevel(Object oLevel)
        {
        // import java.util.logging.Level;
        
        getLogger().setLevel((Level) oLevel);
        }
    
    // Accessor for the property "Logger"
    /**
     * Setter for property Logger.<p>
    * The underlying JDK Logger used to log all messages.
     */
    protected void setLogger(java.util.logging.Logger logger)
        {
        __m_Logger = logger;
        }
    
    /**
     * Translate the given Logger level to an equivalent object appropriate for
    * the underlying logging mechanism.
     */
    protected Object translateLevel(int iLevel)
        {
        return getLevel(iLevel + 1);
        }
    
    // Declared at the super level
    /**
     * Translate the given Logger level to an equivalent object appropriate for
    * the underlying logging mechanism.
     */
    protected Object translateLevel(Integer ILevel)
        {
        return translateLevel(ILevel.intValue());
        }
    }
