
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Logger

package com.tangosol.coherence.component.util.daemon.queueProcessor;

import com.tangosol.coherence.component.Application;
import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.util.LogOutput;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.internal.net.logging.DefaultLoggingDependencies;
import com.tangosol.util.Base;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A Logger component is used to asynchronously log messages for a specific
 * system or application component.
 * 
 * Each Logger instance has an associated logging level. Only log messages that
 * meet or exceed this level are logged. Currently, the Logger defines 10
 * logging levels (from highest to lowest level):
 * 
 * LEVEL_INTERNAL (All messages without a log level)
 * LEVEL_ERROR      (Error messages)
 * LEVEL_WARNING (Warning messages)
 * LEVEL_INFO          (Informational messages)
 * LEVEL_D4             (Debug messages)
 * LEVEL_D5
 * LEVEL_D6
 * LEVEL_D7
 * LEVEL_D8
 * LEVEL_D9
 * 
 * Additionally, the Logger defines two "psuedo" levels that instruct the
 * Logger to either log all messages or to not log any messages:
 * 
 * LEVEL_ALL
 * LEVEL_NONE
 * 
 * Log messages are logged using the log() method. There are several versions
 * of the log() method that allow both string messages and Throwable stack
 * traces to be logged. The Logger uses a string template to format the log
 * message before it is logged using the underlying logging mechanism. The
 * template may contain the following parameterizable strings:
 * 
 * {date}     -the date and time that the message was logged
 * {level}    -the level of the log message
 * {thread} -the thread that logged the message
 * {text}     -the text of the message
 * 
 * Subclasses of the Logger are free to define additional parameters.
 * 
 * The Logger component uses a LogOutput component to log messages to an
 * underlying logging mechanism, such as stdout, stderr, a file, a JDK Logger,
 * or a Log4j Logger. See configure() method for additional detail.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Logger
        extends    com.tangosol.coherence.component.util.daemon.QueueProcessor
    {
    // ---- Fields declarations ----
    
    /**
     * Property Dependencies
     *
     * The external dependencies needed by this component. The dependencies
     * object must be fully populated and validated before this property is
     * set.  See setDependencies.  
     * 
     * The mechanism for creating and populating dependencies is hidden from
     * this component. Typically, the dependencies object is populated using
     * data from some external configuration, such as XML, but this may not
     * always be the case
     */
    private com.tangosol.internal.net.logging.LoggingDependencies __m_Dependencies;
    
    /**
     * Property Destination
     *
     * The logging destination. Can be one of stderr, stdout, jdk, log4j2,
     * slf4j or a file name.
     */
    private String __m_Destination;
    
    /**
     * Property Format
     *
     * The log message format template.
     */
    private String __m_Format;
    
    /**
     * Property Integer
     *
     * A cache of frequently used Integer objects that represent logging levels.
     */
    private static Integer[] __s_Integer;
    
    /**
     * Property Level
     *
     * The configured maximum logging level.
     */
    private int __m_Level;
    
    /**
     * Property LEVEL_ALL
     *
     * Logging level that instructs the Logger to log all messages.
     */
    public static final int LEVEL_ALL = 10;
    
    /**
     * Property LEVEL_D4
     *
     * Logging level associated with debug messages.
     */
    public static final int LEVEL_D4 = 4;
    
    /**
     * Property LEVEL_D5
     *
     * Logging level associated with debug messages.
     */
    public static final int LEVEL_D5 = 5;
    
    /**
     * Property LEVEL_D6
     *
     * Logging level associated with debug messages.
     */
    public static final int LEVEL_D6 = 6;
    
    /**
     * Property LEVEL_D7
     *
     * Logging level associated with debug messages.
     */
    public static final int LEVEL_D7 = 7;
    
    /**
     * Property LEVEL_D8
     *
     * Logging level associated with debug messages.
     */
    public static final int LEVEL_D8 = 8;
    
    /**
     * Property LEVEL_D9
     *
     * Logging level associated with debug messages.
     */
    public static final int LEVEL_D9 = 9;
    
    /**
     * Property LEVEL_ERROR
     *
     * Logging level associated with error messages.
     */
    public static final int LEVEL_ERROR = 1;
    
    /**
     * Property LEVEL_INFO
     *
     * Logging level associated with informational messages.
     */
    public static final int LEVEL_INFO = 3;
    
    /**
     * Property LEVEL_INTERNAL
     *
     * Logging level associated with internal log messages.
     */
    public static final int LEVEL_INTERNAL = 0;
    
    /**
     * Property LEVEL_NONE
     *
     * Logging level that instructs the Logger to not log any messages.
     */
    public static final int LEVEL_NONE = -1;
    
    /**
     * Property LEVEL_TEXT
     *
     * A String array containing descriptions of each of the supported logging
     * levels indexed by the level.
     */
    public static final String[] LEVEL_TEXT;
    
    /**
     * Property LEVEL_WARNING
     *
     * Logging level associated with warning messages.
     */
    public static final int LEVEL_WARNING = 2;
    
    /**
     * Property Limit
     *
     * The logging character limit.
     */
    private int __m_Limit;
    
    /**
     * Property LogLevel
     *
     * An array of Level objects representing the various Coherence log levels.
     */
    private static java.util.logging.Level[] __s_LogLevel;
    
    /**
     * Property LogOutput
     *
     * The LogOutput used to log all formatted log messages.
     */
    private com.tangosol.coherence.component.util.LogOutput __m_LogOutput;
    
    /**
     * Property Overflowed
     *
     * Indicates that the Logger thread is stuck or too slow to process
     * incoming log messages.
     * 
     * @volatile
     */
    private volatile transient boolean __m_Overflowed;
    
    /**
     * Property OverflowedLevel
     *
     * The logging level while in Overflowed state.
     * 
     * @volatile
     */
    private volatile int __m_OverflowedLevel;
    
    /**
     * Property Parameters
     *
     * The set of parameterizable strings that may appear in formatted log
     * messages.
     */
    private String[] __m_Parameters;
    
    /**
     * Property QUEUE_DROP_SIZE
     *
     * The number of messages over the threshold that would cause the
     * overflowed log level to drop by 1.
     */
    public static final int QUEUE_DROP_SIZE = 500;
    
    /**
     * Property QUEUE_MAX_THRESHOLD
     *
     * The absolute maximum number of messages in the log queue before it is
     * considered "overflowed".  The actual threshold value could be further
     * reduced by the Limit setting.
     * 
     * @see #setLimit()
     */
    public static final int QUEUE_MAX_THRESHOLD = 8000;
    
    /**
     * Property QueueHalfThreshold
     *
     * The half of the maximum number of messages in the log queue before it is
     * considered "overflowed". 
     * Initialized based on the Limit property value.
     */
    private int __m_QueueHalfThreshold;
    
    /**
     * Property THREAD_NAME_DELIM
     *
     * A delimiter used to decorate service thread names with information
     * useful for a thread dump analysis.
     */
    public static final char THREAD_NAME_DELIM = '|';
    private static com.tangosol.util.ListMap __mapChildren;
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import com.tangosol.util.Base;
        // import java.util.logging.Level;
        
        _initStatic$Default();
        
        LEVEL_TEXT = new String[]
            {
            "Internal",
            "Error",
            "Warning",
            "Info",
            "D4",
            "D5",
            "D6",
            "D7",
            "D8",
            "D9"
            };
        
        Integer[] ai = new Integer[]
            {
            Integer.valueOf(LEVEL_NONE),
            Integer.valueOf(LEVEL_INTERNAL),
            Integer.valueOf(LEVEL_ERROR),
            Integer.valueOf(LEVEL_WARNING),
            Integer.valueOf(LEVEL_INFO),
            Integer.valueOf(LEVEL_D4),
            Integer.valueOf(LEVEL_D5),
            Integer.valueOf(LEVEL_D6),
            Integer.valueOf(LEVEL_D7),
            Integer.valueOf(LEVEL_D8),
            Integer.valueOf(LEVEL_D9),
            Integer.valueOf(LEVEL_ALL)
            };
        setInteger(ai);
        
        // COH-6582: use Level.parse() here as a workaround for JDK bug6543126
        Level[] aLogLevel = new Level[]
            {
            Level.parse("0"),
            Level.parse("1"),
            Level.parse("2"),
            Level.parse("3"),
            Level.parse("4"),
            Level.parse("5"),
            Level.parse("6"),
            Level.parse("7"),
            Level.parse("8"),
            Level.parse("9"),
            };
        
        setLogLevel(aLogLevel);
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Default constructor
    public Logger()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Logger(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        // state initialization: public and protected properties
        try
            {
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setLevel(10);
            setLimit(65536);
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setOverflowed(false);
            String[] a0 = new String[3];
                {
                a0[0] = "{logRecord}";
                a0[1] = "{uptime}";
                a0[2] = "{thread}";
                }
            setParameters(a0);
            setPriority(3);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");
        _addChild(new Logger.ShutdownHook("ShutdownHook", this, true), "ShutdownHook");
        
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.Logger();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Logger".replace('/', '.'));
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
    
    /**
     * Determine whether diagnosability logging (ODL/DMS) can be performed.
     */
    public boolean checkDiagnosability()
        {
        return false;
        }
    
    /**
     * Create a new Default dependencies object by cloning the input
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone.
    * 
    * @return DefaultLoggingDependencies  the cloned dependencies
    * 
    * 
    * 
     */
    protected com.tangosol.internal.net.logging.DefaultLoggingDependencies cloneDependencies(com.tangosol.internal.net.logging.LoggingDependencies deps)
        {
        // import com.tangosol.internal.net.logging.DefaultLoggingDependencies;
        
        return new DefaultLoggingDependencies(deps);
        }
    
    /**
     * Collects an array of parameters to be used to format the log message.
     */
    protected Object[] collectLogParameters()
        {
        return null;
        }
    
    /**
     * Create a new log message with the following structure:
    * 
    * new Object[]
    *     {
    *     [Timestamp],
    *     [Level],
    *     [Thread],
    *     [Throwable],
    *     [Message],
    *     [Parameter Value]*
    *     };
     */
    protected Object[] createMessage(int nLevel, Throwable throwable, String sMessage)
        {
        // import com.tangosol.util.Base;
        // import java.util.logging.LogRecord;
        
        Object[] aoParam   = collectLogParameters();
        int      cParams   = aoParam == null ? 0 : aoParam.length;
        Object[] aoMessage = new Object[3 + cParams];
        
        LogRecord logRecord = instantiateLogRecord(getLogLevel(nLevel), sMessage);
        logRecord.setMillis(System.currentTimeMillis());
        logRecord.setThrown(throwable);
        
        aoMessage[0] = logRecord;
        aoMessage[1] = Long.valueOf(Base.getUpTimeMillis());
        aoMessage[2] = Thread.currentThread();
        
        if (cParams > 0)
            {
            System.arraycopy(aoParam, 0, aoMessage, 3, cParams);
            }
        
        return aoMessage;
        }
    
    /**
     * Format the given log message by substituting parameter names in the
    * message format string with the values represented by the specified
    * LogRecord.
     */
    protected String formatLogRecord(String sFormat, java.util.logging.LogRecord logRecord)
        {
        // import com.tangosol.util.Base;
        // import java.sql.Timestamp;
        
        String sMessage = sFormat;
        
        sMessage = Base.replace(sMessage, "{text}",
                                formatParameter("{text}", logRecord.getMessage()));
        sMessage = Base.replace(sMessage, "{date}",
                                formatParameter("{date}", new Timestamp(logRecord.getMillis())));
        sMessage = Base.replace(sMessage, "{level}",
                                formatParameter("{level}", logRecord.getLevel()));
        
        return sMessage;
        }
    
    /**
     * Format the given log message by substituting parameter names in the
    * message format string with the values contained in the given message. Log
    * messages must be in the form of an Object array with the following
    * structure:
    * 
    * new Object[]
    *     {
    *     [LogRecord],
    *     [Uptime],
    *     [Thread],
    *     [Parameter Value]*
    *     };
     */
    public String formatMessage(Object[] aoParam)
        {
        // import com.tangosol.util.Base;
        // import java.sql.Timestamp;
        // import java.util.logging.LogRecord;
        
        LogRecord logRecord = (LogRecord) aoParam[0];
        String    sMessage  = logRecord.getMessage();
        
        // do not format messages with LEVEL_INTERNAL
        if (logRecord.getLevel().intValue() != LEVEL_INTERNAL)
            {    
            String   sFormat     = getFormat();
            String[] aoParamName = getParameters();
            int      cParamNames = aoParamName == null ? 0 : aoParamName.length;
            int      cParams     = aoParam.length;
        
            // replace any parameters
            if (sFormat != null)
                {
                // convert the fixed parameters captured in the LogRecord
                sMessage = formatLogRecord(sFormat, logRecord);
        
                // convert the rest of the parameters
                for (int i = 1; i < cParamNames; i++)
                    {
                    String sParamName  = aoParamName[i];
                    Object oParamValue = i < cParams ? aoParam[i] : null;
        
                    sMessage = Base.replace(sMessage, sParamName,
                        formatParameter(sParamName, oParamValue)); 
                    }
                }
        
            // replace the original message text with the formatted version
            logRecord.setMessage(sMessage);
            }
        
        return sMessage;
        }
    
    /**
     * Format the given parameter with the given name for output to the
    * underlying logger.
     */
    protected String formatParameter(String sParamName, Object oParamValue)
        {
        // import java.sql.Timestamp;
        // import java.util.logging.Level;
        
        String sParam = null;
        
        if (sParamName != null && sParamName.length() > 2)
            {
            switch (sParamName.charAt(1))
                {
                case 'd':
                    // {date}
                    if (sParamName.equals("{date}") && oParamValue instanceof Timestamp)
                        {
                        sParam = oParamValue.toString();
                        if (sParam.length() < 23)
                            {
                            sParam = (sParam + "000").substring(0, 23);
                            }
                        }
                    break;
        
                case 'u':
                    // {uptime}
                    if (sParamName.equals("{uptime}") && oParamValue instanceof Long)
                        {
                        long cMillisUp  = ((Long) oParamValue).longValue();
                        long cSec       = cMillisUp / 1000;
                        long cMillis    = cMillisUp % 1000;
                        
                        if (cMillis < 10)
                            {
                            sParam = cSec + ".00" + cMillis;
                            }
                        else if (cMillis < 100)
                            {
                            sParam = cSec + ".0" + cMillis;
                            }
                        else
                            {
                            sParam = cSec + "." + cMillis;
                            }
                        }
                    break;
        
                case 'l':
                    // {level}
                    if (sParamName.equals("{level}") && oParamValue instanceof Level)
                        {
                        sParam = LEVEL_TEXT[((Level) oParamValue).intValue()];
                        }
                    break;
        
                case 't':
                    // {thread}
                    if (sParamName.equals("{thread}") && oParamValue instanceof Thread)
                        {
                        sParam = ((Thread) oParamValue).getName();
        
                        // remove decoration added by Service threads
                        int ofDecor = sParam.indexOf(THREAD_NAME_DELIM);
                        if (ofDecor > 0)
                            {
                            sParam = sParam.substring(0, ofDecor);
                            }
                        }
                    // {text}
                    else if (sParamName.equals("{text}"))
                        {
                        sParam = oParamValue instanceof String ? (String) oParamValue : "";
                        }
                    break;
                }
            }
        
        if (sParam == null)
            {
            return oParamValue == null ? "n/a" : oParamValue.toString();
            }
        
        return sParam;
        }
    
    // Accessor for the property "Dependencies"
    /**
     * Getter for property Dependencies.<p>
    * The external dependencies needed by this component. The dependencies
    * object must be fully populated and validated before this property is set.
    *  See setDependencies.  
    * 
    * The mechanism for creating and populating dependencies is hidden from
    * this component. Typically, the dependencies object is populated using
    * data from some external configuration, such as XML, but this may not
    * always be the case
     */
    public com.tangosol.internal.net.logging.LoggingDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // Accessor for the property "Destination"
    /**
     * Getter for property Destination.<p>
    * The logging destination. Can be one of stderr, stdout, jdk, log4j2, slf4j
    * or a file name.
     */
    public String getDestination()
        {
        return __m_Destination;
        }
    
    // Accessor for the property "Format"
    /**
     * Getter for property Format.<p>
    * The log message format template.
     */
    public String getFormat()
        {
        return __m_Format;
        }
    
    // Accessor for the property "Integer"
    /**
     * Getter for property Integer.<p>
    * A cache of frequently used Integer objects that represent logging levels.
     */
    protected static Integer[] getInteger()
        {
        return __s_Integer;
        }
    
    // Accessor for the property "Integer"
    /**
     * Getter for property Integer.<p>
    * A cache of frequently used Integer objects that represent logging levels.
     */
    protected static Integer getInteger(int nIndex)
        {
        nIndex = Math.max(nIndex, LEVEL_INTERNAL);
        nIndex = Math.min(nIndex, LEVEL_D9);
        
        return getInteger()[nIndex + 1];
        }
    
    // Accessor for the property "Level"
    /**
     * Getter for property Level.<p>
    * The configured maximum logging level.
     */
    public int getLevel()
        {
        return __m_Level;
        }
    
    // Accessor for the property "Limit"
    /**
     * Getter for property Limit.<p>
    * The logging character limit.
     */
    public int getLimit()
        {
        return __m_Limit;
        }
    
    // Accessor for the property "LogLevel"
    /**
     * Getter for property LogLevel.<p>
    * An array of Level objects representing the various Coherence log levels.
     */
    public static java.util.logging.Level[] getLogLevel()
        {
        return __s_LogLevel;
        }
    
    // Accessor for the property "LogLevel"
    /**
     * Getter for property LogLevel.<p>
    * An array of Level objects representing the various Coherence log levels.
     */
    public static java.util.logging.Level getLogLevel(int nLevel)
        {
        nLevel = Math.max(nLevel, LEVEL_INTERNAL);
        nLevel = Math.min(nLevel, LEVEL_D9);
        
        return getLogLevel()[nLevel];
        }
    
    // Accessor for the property "LogOutput"
    /**
     * Getter for property LogOutput.<p>
    * The LogOutput used to log all formatted log messages.
     */
    protected com.tangosol.coherence.component.util.LogOutput getLogOutput()
        {
        return __m_LogOutput;
        }
    
    // Accessor for the property "OverflowedLevel"
    /**
     * Getter for property OverflowedLevel.<p>
    * The logging level while in Overflowed state.
    * 
    * @volatile
     */
    public int getOverflowedLevel()
        {
        return __m_OverflowedLevel;
        }
    
    // Accessor for the property "Parameters"
    /**
     * Getter for property Parameters.<p>
    * The set of parameterizable strings that may appear in formatted log
    * messages.
     */
    public String[] getParameters()
        {
        return __m_Parameters;
        }
    
    // Accessor for the property "QueueHalfThreshold"
    /**
     * Getter for property QueueHalfThreshold.<p>
    * The half of the maximum number of messages in the log queue before it is
    * considered "overflowed". 
    * Initialized based on the Limit property value.
     */
    public int getQueueHalfThreshold()
        {
        return __m_QueueHalfThreshold;
        }
    
    /**
     * Return a LogRecord that encapsulates the specified level and message.
     */
    protected java.util.logging.LogRecord instantiateLogRecord(java.util.logging.Level level, String sMessage)
        {
        // import java.util.logging.LogRecord;
        
        return new LogRecord(level, sMessage);
        }
    
    /**
     * Determine whether diagnosability logging (ODL/DMS) can be performed.
     */
    public static boolean isDiagnosabilityEnabled()
        {
        // import Component.Application;
        // import Component.Application.Console.Coherence;
        
        return ((Coherence) Application.get_Instance()).getLogger().checkDiagnosability();
        }
    
    /**
     * Return true if the Logger would log a message with the given log level.
     */
    public boolean isEnabled(int nLevel)
        {
        // import Component.Util.LogOutput;
        
        LogOutput logOutput = getLogOutput();
                  nLevel    = getInteger(nLevel).intValue();
        
        return logOutput == null || logOutput.isInheritLogLevel()
                ? nLevel <= getLevel()
                : logOutput.isEnabled(nLevel);
        }
    
    // Accessor for the property "Overflowed"
    /**
     * Getter for property Overflowed.<p>
    * Indicates that the Logger thread is stuck or too slow to process incoming
    * log messages.
    * 
    * @volatile
     */
    public boolean isOverflowed()
        {
        return __m_Overflowed;
        }
    
    /**
     * Log the given message with the specified log level.
     */
    public void log(int nLevel, String sMessage)
        {
        log(nLevel, null, sMessage);
        }
    
    /**
     * Log the given Throwable with the specified log level.
     */
    public void log(int nLevel, Throwable throwable)
        {
        log(nLevel, throwable, null);
        }
    
    /**
     * Log the given Throwable and associated message with the specified log
    * level.
     */
    public void log(int nLevel, Throwable throwable, String sMessage)
        {
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        if (isEnabled(nLevel))
            {
            nLevel = Math.max(nLevel, LEVEL_NONE);
            nLevel = Math.min(nLevel, LEVEL_ALL);
        
            com.tangosol.coherence.component.util.Queue queue = getQueue();
            int   cSize = queue.size();
            int   cHalf = getQueueHalfThreshold();
        
            if (cSize >= cHalf)
                {
                boolean fOverflow = isOverflowed();
                if (fOverflow)
                    {
                    // it was and still is overflowed
                    if (nLevel > getOverflowedLevel() &&
                        cSize >= 2 * cHalf)
                        {
                        // drop the high level messages due to overflow
                        return;
                        }
                    }
                else
                    {
                    // first time over the limit
                    fOverflow = cSize >= 2 * cHalf;
                    }
        
                if (fOverflow)
                    {
                    processOverflow(
                        createMessage(nLevel, throwable, sMessage), throwable);
                    return;
                    }
                }
        
            // create a new log message and add it to the queue
            queue.add(createMessage(nLevel, throwable, sMessage));
            }
        }
    
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies.  Typically, the  dependencies are copied into the
    * component's properties.  This technique isolates Dependency Injection
    * from the rest of the component code since components continue to access
    * properties just as they did before. 
    * 
    * However, for read-only dependency properties, the component can access
    * the dependencies directly as shown in the example below for Gateway
    * dependencies.  The advantage to this technique is that the property only
    * exists in the dependencies object, it is not duplicated in the component
    * properties.
    * 
    * LoggingDependencies deps = (LoggingrDependencies) getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.logging.LoggingDependencies deps)
        {
        // import Component.Util.LogOutput;
        // import Component.Util.LogOutput.Jdk as com.tangosol.coherence.component.util.logOutput.Jdk;
        // import Component.Util.LogOutput.Log4j2 as com.tangosol.coherence.component.util.logOutput.Log4j2;
        // import Component.Util.LogOutput.SLF4J as com.tangosol.coherence.component.util.logOutput.SLF4J;
        // import Component.Util.LogOutput.Standard as com.tangosol.coherence.component.util.logOutput.Standard;
        // import com.tangosol.internal.net.logging.DefaultLoggingDependencies;;
        // import com.tangosol.internal.net.logging.DefaultLoggingDependencies;
        
        String sDest = deps.getDestination();
        
        // create a LogOutput of the appropriate type
        LogOutput output;
        try
            {
            if ("jdk".equalsIgnoreCase(sDest))
                {
                output = new com.tangosol.coherence.component.util.logOutput.Jdk();
                }
            else if ("log4j2".equalsIgnoreCase(sDest))
                {
                output = new com.tangosol.coherence.component.util.logOutput.Log4j2();
                }
            else if ("slf4j".equalsIgnoreCase(sDest))
                {
                output = new com.tangosol.coherence.component.util.logOutput.SLF4J();
                }
            else
                {
                output = new com.tangosol.coherence.component.util.logOutput.Standard();
                }
            output.configure(deps);
            }
        catch (Throwable e)
            {
            // reset the destination in the dependencies.  The cast is safe as
            // the Logger controls the implementation (see #cloneDependencies)
            ((DefaultLoggingDependencies) deps).setDestination("stderr");
        
            output = new com.tangosol.coherence.component.util.logOutput.Standard();
            output.configure(deps);
        
            output.log(getInteger(LEVEL_ERROR), e,
                    "Error configuring logger '" + sDest + "'; using default settings instead.");
            }
        
        setDestination(deps.getDestination());
        setFormat(deps.getMessageFormat());
        setLevel(deps.getSeverityLevel());
        setLimit(deps.getCharacterLimit());
        setLogOutput(output);

        if (this.isEnabled(getInteger(LEVEL_D6)))
            {
            System.out.println(String.format("Logger configured with destination '%s', severity level '%s' and a character limit of '%s'.",
                    deps.getDestination(), deps.getSeverityLevel(), deps.getCharacterLimit()));
            }
        }
    
    // Declared at the super level
    /**
     * This event occurs when an exception is thrown from onEnter, onWait,
    * onNotify and onExit.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
    * 
    * @param e  the Throwable object (a RuntimeException or an Error)
    * 
    * @throws RuntimeException may be thrown; will terminate the daemon
    * @throws Error may be thrown; will terminate the daemon
     */
    protected void onException(Throwable e)
        {
        // log and continue
        System.err.println("Logger: " + e);
        e.printStackTrace(System.err);
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        // add a shutdown hook that will shutdown the Logger when the JVM exists
        try
            {
            Logger.ShutdownHook hook = (Logger.ShutdownHook) _findName("ShutdownHook");
            hook.register();
            }
        catch (Throwable ignored) {}
        
        super.onInit();
        }
    
    /**
     * Called immediately before a log message is logged to the underlying
    * LogOutput.
     */
    protected void onLog()
        {
        }
    
    // Declared at the super level
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        // import Component.Util.LogOutput;
        // import com.tangosol.util.Base;
        // import java.util.logging.LogRecord;
        
        final int  MAX_TOTAL      = getLimit();
        int        cchTotal       = 0;
        boolean    fTruncate      = false;
        int        cTruncate      = 0;
        int        cTruncateLines = 0;
        int        cchTruncate    = 0;
        boolean    fDone          = false;
        
        do
            {
            Object[] aoMessage = (Object[]) getQueue().removeNoWait();
        
            // check for end of queue; if any messages have been discarded, report the
            // number and size
            if (aoMessage == null)
                {
                if (fTruncate && cTruncate > 0)
                    {
                    aoMessage = createMessage(
                        LEVEL_WARNING,
                        null,
                        "Asynchronous logging character limit exceeded; discarding "
                            + cTruncate + " log messages "
                            + "(lines=" + cTruncateLines
                            + ", chars=" + cchTruncate + ")");
        
                    fDone = true;
                    }
                else
                    {
                    break;
                    }
                }
        
            if (aoMessage.length == 0)
                {
                // zero length message array serves as a shutdown marker
                setExiting(true);
                return;
                }
        
            LogRecord logRecord = (LogRecord) aoMessage[0];
            if (!isEnabled(logRecord.getLevel().intValue()))
                {
                // log level must have been changed after start
                continue;
                }
        
            String    sText      = formatMessage(aoMessage);
            String    sTextSafe  = sText == null ? "" : sText;
            Throwable throwable  = logRecord.getThrown();
            String    sThrowable = throwable == null ? "" : getStackTrace(throwable); 
        
            cchTotal += sTextSafe.length() + sThrowable.length();
            if (fTruncate && !fDone)
                {
                cTruncate      += 1;
                cTruncateLines += Base.parseDelimitedString(sTextSafe, '\n').length;
                cTruncateLines += Base.parseDelimitedString(sThrowable, '\n').length;
                cchTruncate    += sTextSafe.length();
                cchTruncate    += sThrowable.length();
                }
            else
                {
                if (cchTotal > MAX_TOTAL)
                    {
                    fTruncate = true;
                    }
        
                onLog();
        
                LogOutput out = getLogOutput();
                if (out == null)
                    {
                    try
                        {
                        System.err.println(sTextSafe);
                        if (throwable != null)
                            {
                            throwable.printStackTrace();
                            }
                        }
                    catch (Throwable ignored) {}
                    }
                else
                    {
                    out.log(logRecord);
                    }
                }
            }
        while (!fDone);
        }
    
    /**
     * Process a message when the logger's queue is overflowed.
     */
    protected void processOverflow(Object[] aoMessage, Throwable throwable)
        {
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        com.tangosol.coherence.component.util.Queue queue = getQueue();
        int   cHalf = getQueueHalfThreshold();
        int   cDrop = 0; // levels to drop
        
        synchronized (queue)
            {
            if (isOverflowed())
                {
                int cSize = queue.size();
                if (cSize <= cHalf + QUEUE_DROP_SIZE)
                    {
                    // the size dropped below the half threshold;
                    // resume the configured behavior
                    setOverflowed(false);
        
                    queue.add(aoMessage);
                    return;
                    }
        
                // drop the log level proportional to the number of messages over the threshold
                cDrop = (cSize - 2 * cHalf + QUEUE_DROP_SIZE) / QUEUE_DROP_SIZE;
                }
            else
                {
                // the size has just become over the full threshold (2 * half)
                setOverflowed(true);
                setOverflowedLevel(getLevel());
                cDrop = 1;
                }
            }
        
        int nLevelCurrent = getOverflowedLevel();
        if (nLevelCurrent > LEVEL_ERROR)
            {
            queue.add(aoMessage);
        
            if (cDrop > 0)
                {
                int nLevelTarget = Math.max(LEVEL_ERROR, getLevel() - cDrop);
                if (nLevelTarget < nLevelCurrent)
                    {
                    setOverflowedLevel(nLevelTarget);
        
                    String sMsg =
                        "The configured logger is stuck or too slow (backlog of " +
                        queue.size() + " messages) at threshold log level of \"" +
                        LEVEL_TEXT[nLevelCurrent] + "\"" +
                        (nLevelTarget == LEVEL_ERROR
                            ? "; switching to synchronous \"err\" logging"
                            : "; lowering the threshold to \"" + LEVEL_TEXT[nLevelTarget] + "\"");
        
                    queue.add(createMessage(nLevelCurrent, null, sMsg));
                    if (nLevelTarget == LEVEL_ERROR)
                        {
                        // formatMessage() mutates the LogRecord, so we need to
                        // create a copy of the same message (Object[])
                        System.err.println(formatMessage(createMessage(LEVEL_ERROR, null, sMsg)));
                        }
                    }
                }
            }
        else
            {       
            String sText = formatMessage(aoMessage);
        
            System.err.println(sText == null ? "" : sText);
            if (throwable != null)
                {
                throwable.printStackTrace();
                }
            }
        }
    
    // Accessor for the property "Dependencies"
    /**
     * Inject the Dependencies object into the component.  First clone the
    * dependencies, then validate the cloned copy.  Note that the validate
    * method may modify the cloned dependencies, so it is important to use the
    * cloned dependencies for all subsequent operations.  Once the dependencies
    * have been validated, call onDependencies so that each Componenet in the
    * class hierarchy can process the dependencies as needed.  
     */
    public void setDependencies(com.tangosol.internal.net.logging.LoggingDependencies deps)
        {
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        __m_Dependencies = (cloneDependencies(deps).validate());
        
        // use the cloned dependencies
        onDependencies(getDependencies());
        }
    
    // Accessor for the property "Destination"
    /**
     * Setter for property Destination.<p>
    * The logging destination. Can be one of stderr, stdout, jdk, log4j2, slf4j
    * or a file name.
     */
    protected void setDestination(String sDestination)
        {
        __m_Destination = sDestination;
        }
    
    // Accessor for the property "Format"
    /**
     * Setter for property Format.<p>
    * The log message format template.
     */
    public void setFormat(String sFormat)
        {
        __m_Format = sFormat;
        }
    
    // Accessor for the property "Integer"
    /**
     * Setter for property Integer.<p>
    * A cache of frequently used Integer objects that represent logging levels.
     */
    protected static void setInteger(Integer[] aInteger)
        {
        __s_Integer = aInteger;
        }
    
    // Accessor for the property "Integer"
    /**
     * Setter for property Integer.<p>
    * A cache of frequently used Integer objects that represent logging levels.
     */
    protected static void setInteger(int nIndex, Integer integer)
        {
        getInteger()[nIndex] = integer;
        }
    
    // Accessor for the property "Level"
    /**
     * Set the logging level.
     */
    public void setLevel(int nLevel)
        {
        // import Component.Util.LogOutput;
        
        __m_Level = (nLevel);
        
        LogOutput out = getLogOutput();
        if (out != null)
            {
            out.setLevel(Integer.valueOf(nLevel));
            }
        }
    
    // Accessor for the property "Level"
    /**
     * Set the logging level.
     */
    public void setLevel(Integer ILevel)
        {
        // import Component.Util.LogOutput;
        
        setLevel(ILevel.intValue());
        
        LogOutput out = getLogOutput();
        if (out != null)
            {
            out.setLevel(ILevel);
            }
        }
    
    // Accessor for the property "Limit"
    /**
     * Setter for property Limit.<p>
    * The logging character limit.
     */
    public void setLimit(int nLimit)
        {
        setQueueHalfThreshold(Math.min(QUEUE_MAX_THRESHOLD / 2 , nLimit / 256)); // assume 256 chars per line
        
        __m_Limit = (nLimit);
        }
    
    // Accessor for the property "LogLevel"
    /**
     * Setter for property LogLevel.<p>
    * An array of Level objects representing the various Coherence log levels.
     */
    protected static void setLogLevel(java.util.logging.Level[] aLevel)
        {
        __s_LogLevel = aLevel;
        }
    
    // Accessor for the property "LogLevel"
    /**
     * Setter for property LogLevel.<p>
    * An array of Level objects representing the various Coherence log levels.
     */
    protected static void setLogLevel(int i, java.util.logging.Level level)
        {
        getLogLevel()[i] = level;
        }
    
    // Accessor for the property "LogOutput"
    /**
     * Setter for property LogOutput.<p>
    * The LogOutput used to log all formatted log messages.
     */
    protected void setLogOutput(com.tangosol.coherence.component.util.LogOutput output)
        {
        __m_LogOutput = output;
        }
    
    // Accessor for the property "Overflowed"
    /**
     * Setter for property Overflowed.<p>
    * Indicates that the Logger thread is stuck or too slow to process incoming
    * log messages.
    * 
    * @volatile
     */
    protected void setOverflowed(boolean fOverflowed)
        {
        __m_Overflowed = fOverflowed;
        }
    
    // Accessor for the property "OverflowedLevel"
    /**
     * Setter for property OverflowedLevel.<p>
    * The logging level while in Overflowed state.
    * 
    * @volatile
     */
    protected void setOverflowedLevel(int nLevel)
        {
        __m_OverflowedLevel = nLevel;
        }
    
    // Accessor for the property "Parameters"
    /**
     * Setter for property Parameters.<p>
    * The set of parameterizable strings that may appear in formatted log
    * messages.
     */
    protected void setParameters(String[] asParams)
        {
        __m_Parameters = asParams;
        }
    
    // Accessor for the property "QueueHalfThreshold"
    /**
     * Setter for property QueueHalfThreshold.<p>
    * The half of the maximum number of messages in the log queue before it is
    * considered "overflowed". 
    * Initialized based on the Limit property value.
     */
    public void setQueueHalfThreshold(int nLimit)
        {
        __m_QueueHalfThreshold = nLimit;
        }
    
    /**
     * Stop the Logger and release any resources held by the Logger. This method
    * has no effect if the Logger has already been stopped. Stopping a Logger
    * makes it unusable. Any attempt to use a stopped Logger may result in an
    * exception.
     */
    public synchronized void shutdown()
        {
        // import com.oracle.coherence.common.base.Blocking;
        
        Logger.ShutdownHook hook = (Logger.ShutdownHook) _findName("ShutdownHook");
        hook.unregister();
        
        if (isStarted())
            {
            // zero length log info serves as a shutdown marker
            getQueue().add(new Object[0]);
        
            if (getThread() != Thread.currentThread())
                {
                try
                    {
                    Blocking.wait(this, 1000);
                    }
                catch (InterruptedException e)
                    {
                    stop();
                    }
                finally
                    {
                    getLogOutput().close();
                    }
                }
            }
        else
            {
            onNotify();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Logger$ShutdownHook
    
    /**
     * Abstract runnable component used as a virtual-machine shutdown hook.
     * Runnable component used to shutdown the Logger upon virtual-machine
     * shutdown.
     * 
     * @see Logger#onInit
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ShutdownHook
            extends    com.tangosol.coherence.component.util.ShutdownHook
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("UnregisterAction", Logger.ShutdownHook.UnregisterAction.get_CLASS());
            }
        
        // Default constructor
        public ShutdownHook()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ShutdownHook(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            
            
            // containment initialization: children
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.Logger.ShutdownHook();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Logger$ShutdownHook".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        public void run()
            {
            if (getThread() != null)
                {
                ((Logger) get_Module()).shutdown();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Logger$ShutdownHook$UnregisterAction
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class UnregisterAction
                extends    com.tangosol.coherence.component.util.ShutdownHook.UnregisterAction
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Logger.ShutdownHook.UnregisterAction();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Logger$ShutdownHook$UnregisterAction".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }
        }
    }
