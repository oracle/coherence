/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.logging;

/**
 * The LoggingDependencies interface provides a Logging object with its external dependencies.
 *
 * @author der  2011.11.22
 * @since Coherence 12.1.2
 */
public interface LoggingDependencies
    {
    /**
     * Return the logging character limit.
     *
     * @return the logging character limit.
     */
    public int getCharacterLimit();

    /**
     * Return the logging destination.
     *
     * @return the logging destination
     */
    public String getDestination();

    /**
     * Return the logger name.
     *
     * @return the logging name
     */
    public String getLoggerName();

    /**
     * Return the logging message format.
     *
     * @return the logging message format
     */
    public String getMessageFormat();

    /**
     * Return the logging severity level.
     *
     * @return the logging severity level
     */
    public int getSeverityLevel();
    }
