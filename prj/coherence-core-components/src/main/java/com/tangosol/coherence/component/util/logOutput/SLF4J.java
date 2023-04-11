
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.logOutput.SLF4J

package com.tangosol.coherence.component.util.logOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

/**
 * Concrete LogOutput extension that logs messages using the SLF4J logging
 * facility.
 * 
 * The SLF4J LogOutput takes the following configuration parameters:
 * 
 * logger-name:
 *     -the name of the SLF4J Logger used to log all messages
 * 
 * See the coherence-operational-config.xsd for additional documentation for
 * each of these parameters.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SLF4J
        extends    com.tangosol.coherence.component.util.LogOutput
    {
    // ---- Fields declarations ----
    
    /**
     * Property Logger
     *
     * The underlying SLF4J Logger used to log all messages.
     */
    private org.slf4j.Logger __m_Logger;
    
    /**
     * Property MarkerAll
     *
     * An SLF4J Marker that can be used to differentiate between the messages
     * that should always be logged and regular INFO messages.
     */
    private static org.slf4j.Marker __s_MarkerAll;
    
    private static void _initStatic$Default()
        {
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import org.slf4j.MarkerFactory;
        
        _initStatic$Default();
        
        setMarkerAll(MarkerFactory.getMarker("ALL"));
        }
    
    // Default constructor
    public SLF4J()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SLF4J(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.logOutput.SLF4J();
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
            clz = Class.forName("com.tangosol.coherence/component/util/logOutput/SLF4J".replace('/', '.'));
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
        // import org.slf4j.LoggerFactory;
        
        super.configure(deps);
        
        setLogger(LoggerFactory.getLogger(deps.getLoggerName()));
        }
    
    // Accessor for the property "Logger"
    /**
     * Getter for property Logger.<p>
    * The underlying SLF4J Logger used to log all messages.
     */
    protected org.slf4j.Logger getLogger()
        {
        return __m_Logger;
        }
    
    // Accessor for the property "MarkerAll"
    /**
     * Getter for property MarkerAll.<p>
    * An SLF4J Marker that can be used to differentiate between the messages
    * that should always be logged and regular INFO messages.
     */
    public static org.slf4j.Marker getMarkerAll()
        {
        return __s_MarkerAll;
        }
    
    // Declared at the super level
    /**
     * Return true if this logger is configured to log messages at least of
    * nLevel, where nLevel is translated to an implementation specific level.
     */
    public boolean isEnabled(int nLevel)
        {
        // import org.slf4j.Logger;
        
        Logger logger = getLogger();
        
        switch (nLevel)
            {
            case 0:
                return true;
                
            case 1:
                return logger.isErrorEnabled();
        
            case 2:
                return logger.isWarnEnabled();
        
            case 3:
            case 4:
                return logger.isInfoEnabled();
        
            case 5:
            case 6:
                return logger.isDebugEnabled();
        
            default:
                return logger.isTraceEnabled();
            }
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, String sMessage)
        {
        // import org.slf4j.Logger;
        
        Integer ILevel = (Integer) oLevel;
        Logger  logger = getLogger();
        
        switch (ILevel.intValue())
            {
            case 0:
                logger.info(getMarkerAll(), sMessage);
                break;
        
            case 1:
                logger.error(sMessage);
                break;
        
            case 2:
                logger.warn(sMessage);
                break;
        
            case 3:
            case 4:
                logger.info(sMessage);
                break;
        
            case 5:
            case 6:
                logger.debug(sMessage);
                break;
        
            default:
                logger.trace(sMessage);
            }
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable)
        {
        log(oLevel, throwable, "");
        }
    
    // Declared at the super level
    /**
     * Log the given Throwable and associated message with the specified log
    * level (specific to the underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable, String sMessage)
        {
        // import org.slf4j.Logger;
        
        Integer ILevel = (Integer) oLevel;
        Logger  logger = getLogger();
        
        switch (ILevel.intValue())
            {
            case 0:
                logger.info(getMarkerAll(), sMessage, throwable);
                break;
        
            case 1:
                logger.error(sMessage, throwable);
                break;
        
            case 2:
                logger.warn(sMessage, throwable);
                break;
        
            case 3:
            case 4:
                logger.info(sMessage, throwable);
                break;
        
            case 5:
            case 6:
                logger.debug(sMessage, throwable);
                break;
        
            default:
                logger.trace(sMessage, throwable);
            }
        }
    
    // Accessor for the property "Logger"
    /**
     * Setter for property Logger.<p>
    * The underlying SLF4J Logger used to log all messages.
     */
    protected void setLogger(org.slf4j.Logger logger)
        {
        __m_Logger = logger;
        }
    
    // Accessor for the property "MarkerAll"
    /**
     * Setter for property MarkerAll.<p>
    * An SLF4J Marker that can be used to differentiate between the messages
    * that should always be logged and regular INFO messages.
     */
    public static void setMarkerAll(org.slf4j.Marker markerAll)
        {
        __s_MarkerAll = markerAll;
        }
    
    // Declared at the super level
    /**
     * Translate the given Logger level to an equivalent object appropriate for
    * the underlying logging mechanism.
     */
    protected Object translateLevel(Integer ILevel)
        {
        return ILevel;
        }
    }
