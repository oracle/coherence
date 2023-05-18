/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.util.Threads;

/**
 * A {@link RemoteCallable} that writes a thread dump to standard error.
 */
public class LogThreadDump
        implements RemoteCallable<Void>
    {
    @Override
    public Void call() throws Exception
        {
        System.err.println(Threads.getThreadDump());
        return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of {@link LogThreadDump}.
     */
    public static final LogThreadDump INSTANCE = new LogThreadDump();
    }
