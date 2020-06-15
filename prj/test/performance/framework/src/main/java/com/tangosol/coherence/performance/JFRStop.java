/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

/**
 * @author jk 2016.04.18
 */
public class JFRStop implements RemoteCallable<String>
    {
    /**
     * Stop a JFR recording.
     *
     * @param sRecordingName  the name of the JFR recording to stop.
     */
    public JFRStop(String sRecordingName)
        {
        m_sRecordingName = sRecordingName;
        }

    @Override
    public String call() throws Exception
        {
        return JavaFlightRecorder.stopRecording(m_sRecordingName);
        }

    /**
     * The name of the JFR recording to stop.
     */
    private String m_sRecordingName;
    }
