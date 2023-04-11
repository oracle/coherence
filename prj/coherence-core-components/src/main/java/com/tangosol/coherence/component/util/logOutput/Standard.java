
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.logOutput.Standard

package com.tangosol.coherence.component.util.logOutput;

import com.tangosol.coherence.component.util.FileHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Concrete LogOutput extension that logs messages to either stderr, stdout, or
 * a file via a PrintStream.
 * 
 * The Standard LogOutput takes the following configuration parameters:
 * 
 * destination
 *     -specifies the output device used by the logging system; can be one of
 * stderr, stdout, or a file name
 * 
 * See the coherence-operational-config.xsd for additional documentation for
 * each of these parameters.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Standard
        extends    com.tangosol.coherence.component.util.LogOutput
    {
    // ---- Fields declarations ----
    
    /**
     * Property LogLevel
     *
     * The maximum severity level that should be logged
     */
    private int __m_LogLevel;
    
    /**
     * Property PrintStream
     *
     * The PrintStream used to output log messages.
     */
    private java.io.PrintStream __m_PrintStream;
    
    // Default constructor
    public Standard()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Standard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
    
    // Getter for virtual constant InheritLogLevel
    public boolean isInheritLogLevel()
        {
        return true;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.logOutput.Standard();
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
            clz = Class.forName("com.tangosol.coherence/component/util/logOutput/Standard".replace('/', '.'));
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
        // import java.io.PrintStream;
        
        super.close();
        
        PrintStream stream = getPrintStream();
        stream.flush();
        if (stream != System.out && stream != System.err)
            {
            stream.close();
            }
        }
    
    // Declared at the super level
    /**
     * Configure a newly created DefaultLogOutput instance using the supplied
    * XML configuration:
    * 
    * destination
    *     -specifies the output device used by the logging system; can be one
    * of stderr, stdout, or a file name
    * 
    * See the coherence-operational-config.xsd for additional documentation for
    * each of these parameters.
     */
    public void configure(com.tangosol.internal.net.logging.LoggingDependencies deps)
        {
        // import Component.Util.FileHelper;
        // import java.io.File;
        // import java.io.FileOutputStream;
        // import java.io.PrintStream;
        
        super.configure(deps);
        
        PrintStream stream = null;
        String      sDest  = deps.getDestination();
        String      sErr   = null;
        
        if ("stdout".equalsIgnoreCase(sDest))
            {
            stream = System.out;
            }
        else if ("stderr".equalsIgnoreCase(sDest))
            {
            stream = System.err;
            }
        else
            {
            if (sDest != null)
                {
                try
                    {
                    File    file    = new File(sDest).getCanonicalFile();
                    boolean fExists = file.exists();
        
                    if (fExists && file.isDirectory())
                        {
                        sErr = "\nThe specified log file \""
                             + file
                             + "\" refers to a directory";
                        }
                    else
                        {
                        if (!fExists && !file.getParentFile().exists())
                            {
                            sErr = "\nThe parent directory of the specified log file \""
                                 + file
                                 + "\" does not exist";                    
                            }
                        else if (FileHelper.isFullyAccessible(file))
                            {
                            stream = new PrintStream(new FileOutputStream(file));
                            }
                        else
                            {
                            sErr = "\nThe specified log file \""
                                 + file
                                 + "\" appears to be locked by another process";          
                            }
                        }
                    }
                catch (Exception e)
                    {
                    sErr = "\nError opening the specified log file \""
                         + sDest
                         + "\" ("
                         + e.getMessage()
                         + ")";            
                    }
                }
            if (stream == null)
                {
                if (sErr != null)
                    {
                    sErr += "; using System.out for log output instead.\n";
                    System.err.println(sErr);
                    }
                 
                stream = System.out;
                }
            }
        
        setLogLevel(deps.getSeverityLevel());
        setPrintStream(stream);
        }
    
    // Accessor for the property "LogLevel"
    /**
     * Getter for property LogLevel.<p>
    * The maximum severity level that should be logged
     */
    public int getLogLevel()
        {
        return __m_LogLevel;
        }
    
    // Accessor for the property "PrintStream"
    /**
     * Getter for property PrintStream.<p>
    * The PrintStream used to output log messages.
     */
    protected java.io.PrintStream getPrintStream()
        {
        return __m_PrintStream;
        }
    
    // Declared at the super level
    /**
     * Return true if this logger is configured to log messages at least of
    * nLevel, where nLevel is translated to an implementation specific level.
     */
    public boolean isEnabled(int nLevel)
        {
        return nLevel <= getLogLevel();
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, String sMessage)
        {
        log(oLevel,  null, sMessage);
        }
    
    // Declared at the super level
    /**
     * Log the given message with the specified log level (specific to the
    * underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable)
        {
        log(oLevel, throwable, null);
        }
    
    // Declared at the super level
    /**
     * Log the given Throwable and associated message with the specified log
    * level (specific to the underlying logging mechanism).
     */
    protected void log(Object oLevel, Throwable throwable, String sMessage)
        {
        // import java.io.PrintStream;
        
        PrintStream stream = getPrintStream();
        
        synchronized (stream)
            {
            if (sMessage != null)
                {
                stream.println(sMessage);
                }
        
            if (throwable != null)
                {
                throwable.printStackTrace(stream);
                }
        
            stream.flush();
            }
        }
    
    // Declared at the super level
    /**
     * Set the logging level.
     */
    protected void setLevel(Object oLevel)
        {
        setLogLevel(((Integer) oLevel).intValue());
        }
    
    // Accessor for the property "LogLevel"
    /**
     * Setter for property LogLevel.<p>
    * The maximum severity level that should be logged
     */
    public void setLogLevel(int nLevel)
        {
        __m_LogLevel = nLevel;
        }
    
    // Accessor for the property "PrintStream"
    /**
     * Setter for property PrintStream.<p>
    * The PrintStream used to output log messages.
     */
    protected void setPrintStream(java.io.PrintStream stream)
        {
        __m_PrintStream = stream;
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
