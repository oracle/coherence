/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging;

import logging.impl.DebugLoggerFactory;

import logging.impl.DebugMDCAdapter;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.LoggerFactory;

import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * The binding of {@link LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 *
 * @author Jason Howes
 */
public class DebugSLF4JServiceProvider
        implements SLF4JServiceProvider
    {
    /**
     * The unique instance of this class.
     *
     */
    private static final DebugSLF4JServiceProvider SINGLETON = new DebugSLF4JServiceProvider();

    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static final DebugSLF4JServiceProvider getSingleton()
        {
        return SINGLETON;
        }

    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is usually modified with each release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.6";  // !final

    private static final String loggerFactoryClassStr = DebugLoggerFactory.class.getName();

    /**
     * The ILoggerFactory instance returned by the {@link #getLoggerFactory} method
     * should always be the same object
     */
    private final ILoggerFactory loggerFactory;

    private final IMarkerFactory markerFactory;

    public DebugSLF4JServiceProvider()
        {
        loggerFactory = new DebugLoggerFactory();
        markerFactory = new BasicMarkerFactory();
        }

    @Override
    public ILoggerFactory getLoggerFactory()
        {
        return loggerFactory;
        }

    public String getLoggerFactoryClassStr()
        {
        return loggerFactoryClassStr;
        }

    @Override
    public IMarkerFactory getMarkerFactory()
        {
        return markerFactory;
        }

    @Override
    public MDCAdapter getMDCAdapter()
        {
        return new DebugMDCAdapter();
        }

    @Override
    public String getRequestedApiVersion()
        {
        return "2.0.0";
        }

    @Override
    public void initialize()
        {

        }
    }
