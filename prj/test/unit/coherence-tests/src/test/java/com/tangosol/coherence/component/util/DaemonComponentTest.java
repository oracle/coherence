/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test of the Daemon component.
 *
 * @author Aleks Seovic  2026.04.24
 *
 * @since 26.04
 */
public class DaemonComponentTest
    {
    @Test
    public void shouldSuppressRuntimeExceptionFromExitingDiagnostics()
        {
        TestDaemon daemon = new TestDaemon();

        daemon.markExiting();
        daemon.onException(new RuntimeException("test"));

        assertThat(daemon.wasTraceAttempted(), is(true));
        }

    // ----- inner class: TestDaemon ---------------------------------------

    public static class TestDaemon
            extends Daemon
        {
        public void markExiting()
            {
            setExiting(true);
            }

        public boolean wasTraceAttempted()
            {
            return m_fTraceAttempted;
            }

        @Override
        protected void traceExitingException(Throwable e)
            {
            m_fTraceAttempted = true;

            throw new RuntimeException("diagnostic failure");
            }

        private boolean m_fTraceAttempted;
        }
    }
