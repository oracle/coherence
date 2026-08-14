/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import com.oracle.coherence.common.net.SelectionService;

import org.junit.Test;

import java.io.IOException;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link MultiProviderSelectionService}.
 */
public class MultiProviderSelectionServiceTest
    {
    @Test
    public void shouldAssociateChannelsWithEquivalentProviderWrappers()
            throws IOException
        {
        SocketChannel channelDelegateParent = SocketChannel.open();
        SocketChannel channelDelegateChild  = SocketChannel.open();
        SocketChannel channelParent         = new MultiplexedSocketProvider.MultiplexedSocketChannel(
                channelDelegateParent, null, null);
        SocketChannel channelChild          = new MultiplexedSocketProvider.MultiplexedSocketChannel(
                channelDelegateChild, null, null);

        try
            {
            assertThat(channelParent.provider(), is(not(sameInstance(channelChild.provider()))));
            assertThat(channelParent.provider(), is(channelChild.provider()));

            RecordingSelectionService service = new RecordingSelectionService();
            MultiProviderSelectionService multi = new MultiProviderSelectionService(() -> service);

            multi.associate(channelParent, channelChild);

            assertThat(service.m_channelParent, sameInstance((SelectableChannel) channelParent));
            assertThat(service.m_channelChild, sameInstance((SelectableChannel) channelChild));
            }
        finally
            {
            channelParent.close();
            channelChild.close();
            }
        }

    /**
     * SelectionService that records association arguments.
     */
    private static class RecordingSelectionService
            implements SelectionService
        {
        @Override
        public void register(SelectableChannel channel, Handler handler)
            {
            }

        @Override
        public void invoke(SelectableChannel channel, Runnable runnable, long cMillis)
            {
            }

        @Override
        public void associate(SelectableChannel channelParent, SelectableChannel channelChild)
            {
            m_channelParent = channelParent;
            m_channelChild  = channelChild;
            }

        @Override
        public void shutdown()
            {
            }

        private SelectableChannel m_channelParent;
        private SelectableChannel m_channelChild;
        }
    }
