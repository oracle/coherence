/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * Names the server-side operation category for a remote executable invocation.
 * Consumed by RemoteExecutablePolicy for logging, telemetry, and error
 * attribution. Additive enum: new values may be added in future Coherence
 * releases; consumers must not assume a closed set.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public enum OperationReason
    {
    PROCESS_ENTRY,
    SCRIPT_EVAL,
    AGGREGATE,
    INVOKE,
    EVALUATE_FILTER,
    EXTRACT,
    COMPARE,
    TRIGGER,
    EVENT_INTERCEPTOR,
    CONCURRENT_TASK
    }
