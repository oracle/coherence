/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import java.util.concurrent.TimeUnit;

/**
 * @author jk 2016.04.18
 */
public class JFRStart implements RemoteCallable<String>
    {

    /**
     * Create a new indefinite JFR recording.
     *
     * @param sRecordingName  the name of the recording
     */
    public JFRStart(String sRecordingName)
        {
        this.m_sRecordingName = sRecordingName;
        }

    /**
     * Create a new indefinite JFR recording.
     *
     * @param sRecordingName  the name of the recording
     * @param cDuration       the amount of time to record for
     * @param units           the {@link TimeUnit}s to apply to the duration
     */
    public JFRStart(String sRecordingName, int cDuration, TimeUnit units)
        {
        this.m_sRecordingName = sRecordingName;
        this.cSeconds         = (int) units.toSeconds(cDuration);
        }

    @Override
    public String call() throws Exception
        {
        return String.valueOf(JavaFlightRecorder.recordFor(m_sRecordingName, cSeconds));
        }

    /**
     * The unique name for the JFR recording
     */
    private String m_sRecordingName;

    /**
     * The number of seconds to record for, or zero to record indefinitely.
     */
    private int cSeconds;

    }
