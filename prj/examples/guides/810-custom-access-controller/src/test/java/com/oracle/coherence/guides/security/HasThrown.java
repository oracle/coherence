/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;

/**
 * A Bedrock {@link RemoteCallable} that returns true if the process has thrown
 * either a {@link SecurityException} or {@link InterruptedException}.
 */
public class HasThrown
        implements RemoteCallable<Boolean>
    {
    public Boolean call()
        {
        Throwable thrown = SecureCoherence.getThrown();
        Logger.err("Invoking GetThrown, thrown=" + thrown);
        return thrown instanceof SecurityException || thrown instanceof InterruptedException;
        }

    /**
     * A singleton {@link HasThrown}.
     */
    public static final HasThrown INSTANCE = new HasThrown();
    }
