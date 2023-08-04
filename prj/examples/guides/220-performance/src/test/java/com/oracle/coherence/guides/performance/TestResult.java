/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.performance;

import java.io.Serializable;

/**
 * A class to represent a test result.
 *
 * @author Tim Middleton 2023.07.28
 */
public class TestResult
        implements Serializable {

    private final String type;
    private final long   putDuration;
    private final long   putAllDuration;
    private final long   getDuration;
    private final long   getAllDuration;
    private final long   invokeDuration;

    public TestResult(String type, long putDuration, long putAllDuration, long getDuration, long getAllDuration, long invokeDuration) {
        this.type = type;
        this.putDuration = putDuration;
        this.putAllDuration = putAllDuration;
        this.getDuration = getDuration;
        this.getAllDuration = getAllDuration;
        this.invokeDuration = invokeDuration;
    }

    public String getType() {
        return type;
    }

    public long getPutDuration() {
        return putDuration;
    }

    public long getPutAllDuration() {
        return putAllDuration;
    }

    public long getGetDuration() {
        return getDuration;
    }

    public long getGetAllDuration() {
        return getAllDuration;
    }

    public long getInvokeDuration() {
        return invokeDuration;
    }

    @Override
    public String toString() {
        return "TestResult{" +
               "type='" + type + '\'' +
               ", putDuration=" + putDuration +
               ", putAllDuration=" + putAllDuration +
               ", getDuration=" + getDuration +
               ", getAllDuration=" + getAllDuration +
               ", invokeDuration=" + invokeDuration +
               '}';
    }
}
