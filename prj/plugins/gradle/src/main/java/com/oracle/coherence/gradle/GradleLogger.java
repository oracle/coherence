/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.tangosol.io.pof.generator.PortableTypeGenerator;

import org.gradle.api.logging.Logger;

/**
 * A {@link PortableTypeGenerator.Logger} to log output to the Gradle build output.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
public class GradleLogger
        implements PortableTypeGenerator.Logger
    {
    //----- constructors ----------------------------------------------------

    /**
     * Create a logger that wraps the specified Maven logger.
     *
     * @param logger  the Maven logger
     */
    public GradleLogger(Logger logger)
        {
        f_logger = logger;
        }

    //----- GradleLogger methods --------------------------------------------

    @Override
    public void debug(String sMessage)
        {
        f_logger.info(sMessage);
        }

    @Override
    public void info(String sMessage)
        {
        f_logger.lifecycle(sMessage);
        }

    // ----- data members -----------------------------------------------

    /**
     * The wrapped Maven logger.
     */
    private final Logger f_logger;
}
