/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A factory class to create an {@link ExecutorService}.
 */
public class CustomExecutorFactory
    {
    public static ExecutorService createExecutor()
        {
        return Executors.newSingleThreadExecutor();
        }
    }
