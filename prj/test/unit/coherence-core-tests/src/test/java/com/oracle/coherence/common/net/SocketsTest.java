/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;

import java.io.IOException;

import java.nio.channels.Pipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Sockets}.
 *
 * @author phf  2026.09.22
 * @since 26.10
 */
class SocketsTest
    {
    /**
     * Verify that removing the legacy interruptibility workaround does not change
     * the blocking-mode behavior or the method's non-interruptible return value.
     */
    @Test
    void shouldConfigureBlockingMode()
            throws IOException
        {
        Pipe pipe = Pipe.open();

        try (Pipe.SourceChannel source = pipe.source();
             Pipe.SinkChannel sink = pipe.sink())
            {
            assertTrue(source.isBlocking());

            assertFalse(Sockets.configureBlocking(source, false));
            assertFalse(source.isBlocking());

            assertFalse(Sockets.configureBlocking(source, true));
            assertTrue(source.isBlocking());
            }
        }
    }
