/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;

/**
 * An OutputStream implementation which delegates to a SocketChannel.
 *
 * @author mf  2016.05.02
 */
public class SocketChannelOutputStream
    extends OutputStream
    {
    public SocketChannelOutputStream(SocketChannel channel)
        {
        f_channel = channel;
        }

    @Override
    public void write(int b)
            throws IOException
        {
        write(new byte[]{(byte) b});
        }

    @Override
    public void write(byte[] ab, int off, int len)
            throws IOException
        {
        synchronized (f_channel.blockingLock())
            {
            if (f_channel.isBlocking())
                {
                f_channel.write(ByteBuffer.wrap(ab, off, len));
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


    // ----- data members ---------------------------------------------------

    /**
     * The underlying channel.
     */
    protected final SocketChannel f_channel;
    }
