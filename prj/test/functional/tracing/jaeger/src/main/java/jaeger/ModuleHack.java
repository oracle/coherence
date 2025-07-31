/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jaeger;

import org.slf4j.LoggerFactory;

/**
 * Workaround to ensure SLF4J is on the module path.
 */
@SuppressWarnings("unused")
public class ModuleHack
    {
    private static final Class<LoggerFactory> LOGGER_FACTORY_CLASS = LoggerFactory.class;
    }
