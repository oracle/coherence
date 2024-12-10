/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;

/**
 * An InputStream implementation which delegates to a SocketChannel.
 *
 * @author mf  2016.05.02
 */
public class SocketChannelInputStream
    extends InputStream
    {
    public SocketChannelInputStream(SocketChannel channel)
        {
        f_channel = channel;
        }

    @Override
    public int read(byte[] ab, int off, int len)
            throws IOException
        {
        synchronized (f_channel.blockingLock())
            {
            if (f_channel.isBlocking())
                {
                return f_channel.read(ByteBuffer.wrap(ab, off, len));
                }
            else
                {
                throw new IllegalBlockingModeException();
                }
            }
        }

    @Override
    public void close()
            throws IOException
        {
        f_channel.close();
        }

    @Override
    public int read()
            throws IOException
        {
        byte[] ab = new byte[1];
        return read(ab) < 0
            ? -1
            : ab[0];
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying channel
     */
    protected final SocketChannel f_channel;
    }
