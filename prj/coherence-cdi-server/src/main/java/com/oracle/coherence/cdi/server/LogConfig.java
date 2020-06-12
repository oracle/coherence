/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Logging configuration utility.
 * <p/>
 * Eliminates the need to explicitly configure Java Util logging as long as a
 * file {@code logging.properties} is on the classpath or in the current
 * directory, or you configure logging explicitly using System properties.
 * <p/>
 * Both {@value #SYS_PROP_LOGGING_CLASS} and {@value #SYS_PROP_LOGGING_FILE} are
 * honored. If you wish to configure the logging system differently, just do not
 * include the file and/or system properties.
 *
 * @since 20.06
 */
final class LogConfig
    {
    /**
     * Construct {@code LogConfig} instance.
     */
    private LogConfig()
        {
        }

    /**
     * Configure logging.
     */
    static void configureLogging()
        {
        try
            {
            doConfigureLogging();
            }
        catch (IOException e)
            {
            System.err.println("Failed to configure logging");
            e.printStackTrace();
            }
        }

    /**
     * Internal implementation of logging subsystem configuration.
     *
     * @throws IOException  if an error occurs
     */
    private static void doConfigureLogging() throws IOException
        {
        String sConfigClass = System.getProperty(SYS_PROP_LOGGING_CLASS);
        String sConfigPath = System.getProperty(SYS_PROP_LOGGING_FILE);
        String sSource;

        if (sConfigClass != null)
            {
            sSource = "class: " + sConfigClass;
            }
        else if (sConfigPath != null)
            {
            Path path = Paths.get(sConfigPath);
            sSource = path.toAbsolutePath().toString();
            }
        else
            {
            // we want to configure logging ourselves
            sSource = findAndConfigureLogging();
            }

        Logger.getLogger(LogConfig.class.getName()).info("Logging configured using " + sSource);
        }

    /**
     * Find the default configuration file on file system or in the class path
     * and use it to configure logging subsystem.
     *
     * @return the name of the configuration source used
     *
     * @throws IOException  if an error occurs
     */
    private static String findAndConfigureLogging() throws IOException
        {
        String sSource = "defaults";

        // Let's try to find a logging.properties
        // first as a file in the current working directory
        InputStream logConfigStream = null;

        Path path = Paths.get("").resolve(LOGGING_FILE);

        if (Files.exists(path))
            {
            logConfigStream = new BufferedInputStream(Files.newInputStream(path));
            sSource = "file: " + path.toAbsolutePath();
            }
        else
            {
            // second look for classpath (only the first one)
            InputStream resourceStream = LogConfig.class.getResourceAsStream("/" + LOGGING_FILE);
            if (null != resourceStream)
                {
                logConfigStream = new BufferedInputStream(resourceStream);
                sSource = "classpath: /" + LOGGING_FILE;
                }
            }
        if (null != logConfigStream)
            {
            try
                {
                LogManager.getLogManager().readConfiguration(logConfigStream);
                }
            finally
                {
                logConfigStream.close();
                }
            }

        return sSource;
        }

    // ---- constants -------------------------------------------------------

    /**
     * The default name of the logging configuration file.
     */
    private static final String LOGGING_FILE = "logging.properties";

    /**
     * The name of the system property that can be used to define
     * logging configuration class.
     */
    private static final String SYS_PROP_LOGGING_CLASS = "java.util.logging.config.class";

    /**
     * The name of the system property that can be used to define
     * logging configuration file.
     */
    private static final String SYS_PROP_LOGGING_FILE = "java.util.logging.config.file";
    }
