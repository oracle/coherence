/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.logging;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;

/**
 * DefaultLoggingDependencies is a default implementation of LoggingDependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public class DefaultLoggingDependencies
        implements LoggingDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link DefaultLoggingDependencies} object.
     */
    public DefaultLoggingDependencies()
        {
        this(null);
        }

    /**
     * Construct a {@link DefaultLoggingDependencies} object, copying the values
     * from the specified DefaultLoggingDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultLoggingDependencies(LoggingDependencies deps)
        {
        if (deps != null)
            {
            m_cCharLimit     = deps.getCharacterLimit();
            m_sDestination   = deps.getDestination();
            m_sLoggerName    = deps.getLoggerName();
            m_sMessageFormat = deps.getMessageFormat();
            m_nSeverityLevel = deps.getSeverityLevel();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCharacterLimit()
        {
        return m_cCharLimit;
        }

    /**
     * Set the logging character limit.
     *
     * @param cCharLimit  the character limit
     *
     * @return this object
     */
    public DefaultLoggingDependencies setCharacterLimit(int cCharLimit)
        {
        m_cCharLimit = cCharLimit;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDestination()
        {
        return m_sDestination;
        }

    /**
     * Set the destination for the logging file.
     *
     * @param sDestination  the destination for the logging file
     *
     * @return this object
     */
    public DefaultLoggingDependencies setDestination(String sDestination)
        {
        m_sDestination = sDestination;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLoggerName()
        {
        return m_sLoggerName;
        }

    /**
     * Set the name of the logger.
     *
     * @param sLoggerName  the logger name.
     *
     * @return this object
     */
    public DefaultLoggingDependencies setLoggerName(String sLoggerName)
        {
        m_sLoggerName = sLoggerName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessageFormat()
        {
        return m_sMessageFormat;
        }

    /**
     * Set the message format.
     *
     * @param sMessageFormat  the message format
     *
     * @return this object
     */
    public DefaultLoggingDependencies setMessageFormat(String sMessageFormat)
        {
        m_sMessageFormat = sMessageFormat;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSeverityLevel()
        {
        return m_nSeverityLevel;
        }

    /**
     * Set the logging severity level of what is to be logged.
     *
     * @param nSeverityLevel  the severity level to log
     *
     * @return this object
     */
    protected DefaultLoggingDependencies setSeverityLevel(int nSeverityLevel)
        {
        m_nSeverityLevel = nSeverityLevel;
        return this;
        }

    // ----- DefaultLoggingDependencies methods -----------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @return this object
     */
    public DefaultLoggingDependencies validate()
        {
        // if a empty format string was specified then need
        // to reset it to the default.
        if (m_sMessageFormat.length() == 0)
            {
            m_sMessageFormat = DEFAULT_LOGGING_MESSAGE_FORMAT;
            }

        // log level
        if (m_nSeverityLevel < Base.LOG_MIN)
            {
            Logger.fine("Logging.Dependencies: Minimum severity is " + Logger.ALWAYS
                    + " (overriding setting of " + m_nSeverityLevel + ")");
            m_nSeverityLevel = Logger.ALWAYS;
            }
        else if (m_nSeverityLevel > Base.LOG_MAX)
            {
            Logger.fine("Logging.Dependencies: Maximum severity is " + Base.LOG_MAX
                    + " (overriding setting of " + m_nSeverityLevel + ")");
            m_nSeverityLevel = Base.LOG_MAX;
            }

        // char limit
        if (m_cCharLimit <= 0)
            {
            m_cCharLimit = Integer.MAX_VALUE;
            }

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Return a human-readable String representation of the DefaultLoggingDependencies.
     *
     * @return a String describing the DefaultLoggingDependencies
     */
    @Override
    public String toString()
        {
        return "DefaultLoggingDependencies{" + getDescription() + '}';
        }

    /**
     * Format the DefaultLoggingDependencies attributes into a String for inclusion in
     * the String returned from the {@link #toString} method.
     *
     * @return a String listing the attributes of the DefaultLoggingDependencies
     */
     protected String getDescription()
         {
         return "CharacterLimit=" + getCharacterLimit()
            + ", Destination="    + getDestination()
            + ", LoggerName="     + getLoggerName()
            + ", MessageFormat="  + getMessageFormat()
            + ", SeverityLevel="  + getSeverityLevel();
         }

    // ----- constants and data members -------------------------------------

    /**
     * The default logging character limit.
     */
    public static final int DEFAULT_LOGGING_CHAR_LIMIT = 65536;

    /**
     * The default logging destination.
     */
    public static final String DEFAULT_LOGGING_DESTINATION = "stderr";

    /**
     * The default logging level.
     */
    public static final int DEFAULT_LOGGING_LEVEL = Base.LOG_MAX;

    /**
     * The default logger name.
     */
    public static final String DEFAULT_LOGGING_LOGGER_NAME = "Coherence";

    /**
     * The default logging message format.
     */
    public static final String DEFAULT_LOGGING_MESSAGE_FORMAT =
        "{date} &lt;{level}&gt; (thread={thread}): {text}";

    /**
     * The logging character limit.
     */
    private int m_cCharLimit = DEFAULT_LOGGING_CHAR_LIMIT;

    /**
     * The logging destination. Can be one of stderr, stdout, jdk, log4j2, or a file name.
     */
    private String m_sDestination = DEFAULT_LOGGING_DESTINATION;

    /**
     * The name used to identify the logger.
     */
    private String m_sLoggerName = DEFAULT_LOGGING_LOGGER_NAME;

    /**
     * The log message format template.
     */
    private String m_sMessageFormat = DEFAULT_LOGGING_MESSAGE_FORMAT;

    /**
     * The logging level.
     */
    private int m_nSeverityLevel = DEFAULT_LOGGING_LEVEL;
    }
