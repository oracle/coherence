/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;

/**
 * A {@link RemoteCallable} to log a message using the Coherence logger.
 */
public class LogMessage
        implements RemoteCallable<Void>
    {
    /**
     * Create a {@link LogMessage} task.
     *
     * @param sMsg  the message to log
     */
    public LogMessage(String sMsg)
        {
        this.sMsg = sMsg;
        }

    @Override
    public Void call() throws Exception
        {
        Logger.info(sMsg);
        return null;
        }

    // ----- data members ---------------------------------------------------

    private final String sMsg;
    }
