/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.io.IOException;

import java.net.SocketException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketOptions;

import java.nio.channels.SelectableChannel;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Sockets provides static helper methods related to sockets.
 *
 * @author mf 2010.12.14
 */
public final class Sockets
    {
    /**
     * Apply the specified options to a socket.
     *
     * @param socket   the socket to configure
     * @param options  the options to apply, or null for none
     *
     * @throws SocketException if an I/O error occurs
     */
    public static void configure(ServerSocket socket, SocketOptions options)
            throws SocketException
        {
        if (options == null || socket.isClosed())
            {
            return;
            }

        try
            {
            Object oOption;
            if (!socket.isBound() &&
                (oOption = options.getOption(SocketOptions.SO_REUSEADDR)) != null)
                {
                socket.setReuseAddress((Boolean) oOption);
                }
            if ((oOption = options.getOption(SocketOptions.SO_RCVBUF)) != null)
                {
                int cb = (Integer) oOption;
                socket.setReceiveBufferSize(cb);
                int cbReal = socket.getReceiveBufferSize();
                if (cbReal < cb)
                    {
                    warnBufferSize(socket, "receive", cb, cbReal);
                    }
                }
            if ((oOption = options.getOption(SocketOptions.SO_TIMEOUT)) != null)
                {
                socket.setSoTimeout((Integer) oOption);
                }
            }
        catch (SocketException e)
            {
            if (socket.isClosed())
                {
                return;
                }
            throw e;
            }
        }

    /**
     * Apply the specified options to a socket.
     *
     * @param socket   the socket to configure
     * @param options  the options to apply, or null for none
     *
     * @throws SocketException if an I/O error occurs
     */
    public static void configure(Socket socket, SocketOptions options)
            throws SocketException
        {
        if (options == null || socket.isClosed())
            {
            return;
            }

        try
            {
            Object oOption;
            if (!socket.isBound() &&
                (oOption = options.getOption(SocketOptions.SO_REUSEADDR)) != null)
                {
                boolean fReuse = (Boolean) oOption;
                if (fReuse != socket.getReuseAddress())
                    {
                    warnReuseAddr(fReuse);
                    }
                socket.setReuseAddress((Boolean) oOption);
                }
            if ((oOption = options.getOption(SocketOptions.SO_RCVBUF)) != null)
                {
                int cb = (Integer) oOption;
                socket.setReceiveBufferSize(cb);
                int cbReal = socket.getReceiveBufferSize();
                if (cbReal < cb)
                    {
                    warnBufferSize(socket, "receive", cb, cbReal);
                    }
                }
            if ((oOption = options.getOption(SocketOptions.SO_SNDBUF)) != null)
                {
                int cb = (Integer) oOption;
                socket.setSendBufferSize(cb);
                int cbReal = socket.getSendBufferSize();
                if (cbReal < cb)
                    {
                    warnBufferSize(socket, "send", cb, cbReal);
                    }
                }
            if ((oOption = options.getOption(SocketOptions.SO_TIMEOUT)) != null)
                {
                socket.setSoTimeout((Integer) oOption);
                }
            if ((oOption = options.getOption(SocketOptions.SO_LINGER)) != null)
                {
                socket.setSoLinger(true, (Integer) oOption);
                }
            if ((oOption = options.getOption(SocketOptions.SO_KEEPALIVE)) != null)
                {
                socket.setKeepAlive((Boolean) oOption);
                }
            if ((oOption = options.getOption(SocketOptions.TCP_NODELAY)) != null)
                {
                socket.setTcpNoDelay((Boolean) oOption);
                }
            if ((oOption = options.getOption(SocketOptions.IP_TOS)) != null)
                {
                socket.setTrafficClass((Integer) oOption);
                }
            }
        catch (SocketException e)
            {
            if (socket.isClosed())
                {
                return;
                }

            try
                {
                // on some OSs (OSX) you can't set options if the remote end
                // has closed the socket.  This isn't an easily detectable state
                // but we try by attempting to simply set some random option to its
                // current value.
                socket.setKeepAlive(socket.getKeepAlive());
                }
            catch (SocketException e2)
                {
                return;
                }

            throw e;
            }
        }

    /**
     * Apply the specified options to a socket.
     *
     * @param socket   the socket to configure
     * @param options  the options to apply, or null for none
     *
     * @throws SocketException if an I/O error occurs
     */
    public static void configure(DatagramSocket socket, SocketOptions options)
            throws SocketException
        {
        if (options == null || socket.isClosed())
            {
            return;
            }

        try
            {
            Object oOption;
            if (!socket.isBound() &&
                (oOption = options.getOption(SocketOptions.SO_REUSEADDR)) != null)
                {
                boolean fReuse = (Boolean) oOption;
                if (fReuse != socket.getReuseAddress())
                    {
                    warnReuseAddr(fReuse);
                    }
                socket.setReuseAddress((Boolean) oOption);
                }
            if ((oOption = options.getOption(SocketOptions.SO_RCVBUF)) != null)
                {
                int cb = (Integer) oOption;
                socket.setReceiveBufferSize(cb);
                int cbReal = socket.getReceiveBufferSize();
                if (cbReal < cb)
                    {
                    warnBufferSize(socket, "receive", cb, cbReal);
                    }
                }
            if ((oOption = options.getOption(SocketOptions.SO_SNDBUF)) != null)
                {
                int cb = (Integer) oOption;
                socket.setSendBufferSize(cb);
                int cbReal = socket.getSendBufferSize();
                if (cbReal < cb)
                    {
                    warnBufferSize(socket, "send", cb, cbReal);
                    }
                }
            if ((oOption = options.getOption(SocketOptions.SO_TIMEOUT)) != null)
                {
                socket.setSoTimeout((Integer) oOption);
                }
            }
        catch (SocketException e)
            {
            if (socket.isClosed())
                {
                return;
                }
            throw e;
            }
        }

    /**
     * Apply the specified options to a socket.
     *
     * @param socket   the socket to configure
     * @param options  the options to apply, or null for none
     *
     * @throws SocketException if an I/O error occurs
     */
    public void configure(MulticastSocket socket, SocketOptions options)
            throws SocketException
        {
        configure((DatagramSocket) socket, options);
        }

    /**
     * Update the Socket's blocking mode.
     * <p>
     * Java no longer supports changing a channel's interruptibility, so this method only updates the
     * blocking mode and always returns {@code false}.
     *
     * @param chan       the channel
     * @param fBlocking  the blocking mode
     *
     * @return {@code false} to indicate that only the blocking mode was updated
     *
     * @throws java.io.IOException if an IO error occurs
     */
    public static boolean configureBlocking(SelectableChannel chan, boolean fBlocking)
            throws IOException
        {
        synchronized (chan.blockingLock())
            {
            chan.configureBlocking(fBlocking);
            return false;
            }
        }

    /**
     * Return the MTU for the local NIC associated this socket.
     *
     * @param socket  the socket
     * @return the MTU
     */
    public static int getMTU(Socket socket)
        {
        int nMtu = 0;
        try
            {
            nMtu = InetAddresses.getLocalMTU(socket.getLocalAddress());
            }
        catch (Throwable e)
            {
            // at least on OSX we've seen that calling getLocalAddress on a socket which is
            // concurrently being closed can yield this Error.  As getLocalAddress is not
            // allowed to throw SocketException, it is surfaced as an Error, which is in
            // and of itself wrong.

            // fall through
            }

        return nMtu == 0 ? InetAddresses.getLocalMTU() : nMtu;
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Issue a warning regarding an undersized socket buffer.
    *
    * @param socket   the socket
    * @param sBuffer  the buffer description
    * @param cb       the requested size
    * @param cbReal   the actual size
    */
    protected static void warnBufferSize(Object socket, String sBuffer,
            int cb, int cbReal)
        {
        LOGGER.log(Level.INFO,
                "Failed to set the " + sBuffer + " buffer size on " + socket +
                " to " + cb + " bytes; actual size is " + cbReal + " bytes." +
                " Consult your OS documentation regarding  increasing the" +
                " maximum socket buffer size. Proceeding with the actual value" +
                " may cause sub-optimal performance.");
        }

    /**
    * Issue a warning regarding overrideing SO_REUSEADDR
    *
    * @param fReuse  the specified setting
    */
    protected static void warnReuseAddr(boolean fReuse)
        {
        if (s_fWarnReuseAddr)
            {
            s_fWarnReuseAddr = false; // only log this once per JVM
            LOGGER.log(Level.WARNING,
                    "The value of SO_REUSEADDR is being overriden to " +
                    fReuse + " from the system default; this setting is not " +
                    "portable and may result in differeing behavior across " +
                    "environments.");
            }
        }


    // ----- static data members --------------------------------------------

    /**
     * Tracks if the JVM has already warned about explicitly setting re-use
     * addr.
     */
    private static boolean s_fWarnReuseAddr = true;


    // ----- constants ------------------------------------------------------

    /**
     * The Sockets class logger.
     */
    private static Logger LOGGER = Logger.getLogger(Sockets.class.getName());
    }
