
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.logOutput.Log4j2

package com.tangosol.coherence.component.util.logOutput;

import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Concrete LogOutput extension that logs messages using the Log4j logging
 * library.
 * 
 * The Log4j LogOutput takes the following configuration parameters:
 * 
 * logger-name:
 *     -the name of the Log4j Logger used to log all messages
 * 
 * See the coherence-operational-config.xsd for additional documentation for
 * each of these parameters.
 * 
 * The underlying Log4j Logger is configured using the normal Log4j logging
 * configuration mechanism.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Log4j2
        extends    com.tangosol.coherence.component.util.LogOutput
    {
    // ---- Fields declarations ----
    
    /**
     * Property Level
     *
     * A cache of frequently used Log4j Level objects.
     */
    private static org.apache.logging.log4j.Level[] __s_Level;
    
    /**
     * Property Logger
     *
     * The underlying Log4j2 Logger used to log all messages.
     */
    private org.apache.logging.log4j.Logger __m_Logger;
    
    private static void _initStatic$Default()
        {
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import org.apache.logging.log4j.Level;
        
        _initStatic$Default();
        
        Level[] aLevel = new Level[]
            {
            (Level) Level.OFF,   // LEVEL_NONE
            (Level) Level.ALL,   // LEVEL_ALL
            (Level) Level.ERROR, // LEVEL_ERROR
            (Level) Level.WARN,  // LEVEL_WARNING
            (Level) Level.INFO,  // LEVEL_INFO
            (Level) Level.INFO,  // LEVEL_D4
            (Level) Level.DEBUG, // LEVEL_D5
            (Level) Level.DEBUG, // LEVEL_D6
            (Level) Level.TRACE, // LEVEL_D7
            (Level) Level.TRACE, // LEVEL_D8
            (Level) Level.TRACE, // LEVEL_D9
            (Level) Level.ALL    // LEVEL_ALL
            };
        
        setLevel(aLevel);
        }
    
    // Default constructor
    public Log4j2()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Log4j2(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.logOutput.Log4j2();
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
            clz = Class.forName("com.tangosol.coherence/component/util/logOutput/Log4j2".replace('/', '.'));
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
        // import org.apache.logging.log4j.LogManager;
        
        super.configure(deps);
        
        setLogger(LogManager.getLogger(deps.getLoggerName()));
        }
    
    // Accessor for the property "Level"
    /**
     * Getter for property Level.<p>
    * A cache of frequently used Log4j Level objects.
     */
    protected static org.apache.logging.log4j.Level[] getLevel()
        {
        return __s_Level;
        }
    
    // Accessor for the property "Level"
    /**
     * Getter for property Level.<p>
    * A cache of frequently used Log4j Level objects.
     */
    protected static org.apache.logging.log4j.Level getLevel(int nIndex)
        {
        return getLevel()[nIndex];
        }
    
    // Accessor for the property "Logger"
    /**
     * Getter for property Logger.<p>
    * The underlying Log4j2 Logger used to log all messages.
     */
    protected org.apache.logging.log4j.Logger getLogger()
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
        // import org.apache.logging.log4j.Level;
        
        return getLogger().isEnabled((Level) translateLevel(Integer.valueOf(nLevel)));
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, String sMessage)
        {
        // import org.apache.logging.log4j.Level;
        
        getLogger().log((Level) oLevel, sMessage);
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable)
        {
        // import org.apache.logging.log4j.Level;
        
        getLogger().log((Level) oLevel, throwable);
        }
    
    // Declared at the super level
    /**
     * Log the given Throwable and associated message with the specified log
    * level (specific to the underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable, String sMessage)
        {
        // import org.apache.logging.log4j.Level;
        
        getLogger().log((Level) oLevel, sMessage, throwable);
        }
    
    // Accessor for the property "Level"
    /**
     * Setter for property Level.<p>
    * A cache of frequently used Log4j Level objects.
     */
    protected static void setLevel(org.apache.logging.log4j.Level[] aLevel)
        {
        __s_Level = aLevel;
        }
    
    // Accessor for the property "Level"
    /**
     * Setter for property Level.<p>
    * A cache of frequently used Log4j Level objects.
     */
    protected static void setLevel(int nIndex, org.apache.logging.log4j.Level level)
        {
        getLevel()[nIndex] = level;
        }
    
    // Declared at the super level
    /**
     * Set the logging level.
     */
    protected void setLevel(Object oLevel)
        {
        // import org.apache.logging.log4j.Level;
        // import org.apache.logging.log4j.core.Appender;
        // import org.apache.logging.log4j.core.LoggerContext;
        // import org.apache.logging.log4j.core.config.Configuration;
        // import org.apache.logging.log4j.core.config.LoggerConfig;
        // import java.util.Iterator;;
        // import java.util.Iterator;
        // import java.util.Map;
        
        LoggerContext context      = (LoggerContext) LogManager.getContext(false);
        Configuration config       = context.getConfiguration();
        String        name         = getLogger().getName();
        LoggerConfig  loggerConfig = config.getLoggerConfig(name);
        
        if (loggerConfig.getName().equalsIgnoreCase(name))
            {
            loggerConfig.setLevel((Level) oLevel);
            }
        else
            {
            // create a new config.
            loggerConfig = new LoggerConfig(name, (Level) oLevel, false);
            config.addLogger(name, loggerConfig);
        
            LoggerConfig parentConfig = loggerConfig.getParent();
            do
                {
                Map appenders = parentConfig.getAppenders();
                for (Iterator iter = appenders.keySet().iterator(); iter.hasNext();)
                    {
                    String sName = (String) iter.next();
                    loggerConfig.addAppender((Appender) appenders.get(sName), null, null);
                    }
                parentConfig = parentConfig.getParent();
                }
            while (null != parentConfig && parentConfig.isAdditive());
            }
        
        context.updateLoggers();
        }
    
    // Accessor for the property "Logger"
    /**
     * Setter for property Logger.<p>
    * The underlying Log4j2 Logger used to log all messages.
     */
    protected void setLogger(org.apache.logging.log4j.Logger logger)
        {
        __m_Logger = logger;
        }
    
    // Declared at the super level
    /**
     * Translate the given Logger level to an equivalent object appropriate for
    * the underlying logging mechanism.
     */
    protected Object translateLevel(Integer ILevel)
        {
        return getLevel(ILevel.intValue() + 1);
        }
    }
