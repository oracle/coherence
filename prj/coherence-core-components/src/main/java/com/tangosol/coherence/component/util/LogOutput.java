
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.LogOutput

package com.tangosol.coherence.component.util;

/**
 * Abstract component used to log formatted messages to an underlying logging
 * mechanism.
 * 
 * Concrete subclasses must implement the three abstract log methods:
 * 
 * log(Object oLevel, String sMessage, Throwable throwable)
 * log(Object oLevel, String sMessage)
 * log(Object oLevel, Throwable throwable)
 * 
 * Additionally, a concrete LogOutput must be able to translate between
 * internal Logger log levels (see the Logger component for details on the
 * various levels) and equivalent log level objects appropriate for the
 * underlying logging mechanism. See the translateLevel() method for additional
 * details.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class LogOutput
        extends    com.tangosol.coherence.component.Util
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public LogOutput(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant InheritLogLevel
    public boolean isInheritLogLevel()
        {
        return false;
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
            clz = Class.forName("com.tangosol.coherence/component/util/LogOutput".replace('/', '.'));
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
    
    /**
     * Close the LogOutput and release any resources held by the LogOutput. This
    * method has no effect if the LogOutput has already been closed. Closing a
    * LogOutput makes it unusable. Any attempt to use a closed LogOutput may
    * result in an exception.
     */
    public void close()
        {
        }
    
    /**
     * Configure a newly created LogOutput instance using the supplied
    * dependencies.
     */
    public void configure(com.tangosol.internal.net.logging.LoggingDependencies deps)
        {
        }
    
    /**
     * Return true if this logger is configured to log messages at least of
    * nLevel, where nLevel is translated to an implementation specific level.
     */
    public boolean isEnabled(int nLevel)
        {
        return false;
        }
    
    /**
     * Log the given message with the specified Logger log level (in Integer
    * form).
     */
    public void log(Integer ILevel, String sMessage)
        {
        log(translateLevel(ILevel), sMessage);
        }
    
    /**
     * Log the given Throwable with the specified Logger log level (in Integer
    * form).
     */
    public void log(Integer ILevel, Throwable throwable)
        {
        log(translateLevel(ILevel), throwable);
        }
    
    /**
     * Log the given Throwable and associated message with the specified Logger
    * log level (in Integer form).
     */
    public void log(Integer ILevel, Throwable throwable, String sMessage)
        {
        log(translateLevel(ILevel), throwable, sMessage);
        }
    
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, String sMessage)
        {
        }
    
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable)
        {
        }
    
    /**
     * Log the given Throwable and associated message with the specified log
    * level (specific to the underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable, String sMessage)
        {
        }
    
    /**
     * Log the LogRecord prepared by the caller. This implementation redirects
    * to the log method with separate arguments, but an extending logging
    * mechanism may overridde this method to log the record as a whole.
     */
    public void log(java.util.logging.LogRecord logRec)
        {
        Integer   ILevel = Integer.valueOf(logRec.getLevel().intValue());
        Throwable t      = logRec.getThrown();
        String    sMsg   = logRec.getMessage();
        
        if (t == null)
            {
            log(ILevel, sMsg);
            }
        else if (sMsg == null)
            {
            log(ILevel, t);
            }
        else
            {
            log(ILevel, t, sMsg);
            }
        }
    
    /**
     * Set the logging level.
     */
    public void setLevel(Integer ILevel)
        {
        setLevel(translateLevel(ILevel));
        }
    
    /**
     * Set the log level.
     */
    protected void setLevel(Object oLevel)
        {
        }
    
    /**
     * Translate the given Logger level to an equivalent object appropriate for
    * the underlying logging mechanism.
     */
    protected Object translateLevel(Integer ILevel)
        {
        return null;
        }
    }
