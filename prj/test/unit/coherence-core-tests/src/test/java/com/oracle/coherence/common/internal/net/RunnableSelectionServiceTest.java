/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import org.junit.Test;

import java.io.IOException;

import java.nio.channels.Pipe;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link RunnableSelectionService}.
 *
 * @author Aleks Seovic  2026.08.13
 * @since 26.07
 */
public class RunnableSelectionServiceTest
    {
    @Test
    public void shouldPublishWakeupForEveryImmediateTask()
            throws IOException
        {
        Pipe                      pipe    = Pipe.open();
        CountingSelectionService service = new CountingSelectionService();

        try
            {
            pipe.source().configureBlocking(false);

            service.invoke(pipe.source(), () -> {}, 0);
            service.invoke(pipe.source(), () -> {}, 0);

            assertThat(service.m_cWakeups, is(2));
            }
        finally
            {
            service.shutdown();
            pipe.source().close();
            pipe.sink().close();
            }
        }

    /**
     * Selection service that counts published wakeups.
     */
    private static class CountingSelectionService
            extends RunnableSelectionService
        {
        @Override
        protected void wakeup()
            {
            ++m_cWakeups;
            }

        private int m_cWakeups;
        }
    }
