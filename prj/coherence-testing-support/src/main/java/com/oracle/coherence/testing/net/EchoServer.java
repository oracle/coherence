/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.net;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.Daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.SSLException;


/**
* Simple server that echos back data sent by a client.
*
* @author jh  2010.04.27
*/
public class EchoServer
        extends Daemon
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new EchoServer.
    *
    * @param provider  the SocketProvider used by the new EchoServer to
    *                  create a ServerSocket
    * @param nPort     the port to listen on for client connections
    */
    public EchoServer(SocketProvider provider, int nPort)
        {
        if (provider == null || nPort <= 0)
            {
            throw new IllegalArgumentException();
            }
        m_provider = provider;
        m_nPort    = nPort;
        }


    // ----- EchoServer methods ---------------------------------------------

    /**
    * Return the number of active client connections.
    *
    * @return the number of active client connections
    */
    public int getConnectionCount()
        {
        int cConnection = 0;

        // scan for open sockets
        Set<Socket> setSocket = m_setSocket;
        synchronized (setSocket)
            {
            for (Iterator<Socket> iter = setSocket.iterator(); iter.hasNext(); )
                {
                Socket socket = iter.next();
                if (socket.isClosed())
                    {
                    iter.remove();
                    }
                else
                    {
                    ++cConnection;
                    }
                }
            }

        return cConnection;
        }

    /**
    * Release any resources created by the EchoServer.
    */
    protected synchronized void cleanup()
        {
        ServerSocket serverSocket = m_socket;
        if (serverSocket != null)
            {
            try
                {
                serverSocket.close();
                }
            catch (IOException e)
                {
                // ignore
                }
            m_socket = null;

            log(ClassHelper.getSimpleName(getClass()) + " stopped listening.");
            }

        Set<Socket> setSocket = m_setSocket;
        synchronized (setSocket)
            {
            for (Iterator<Socket> iter = setSocket.iterator(); iter.hasNext(); )
                {
                Socket socket = iter.next();
                iter.remove();
                try
                    {
                    socket.close();
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }
        }


    // ----- Daemon methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void run()
        {
        ServerSocket serverSocket = m_socket;
        Set<Socket>  setSocket    = m_setSocket;
        try
            {
            while (isRunning())
                {
                Socket clientSocket;
                try
                    {
                    clientSocket = serverSocket.accept();
                    }
                catch (SSLException e)
                    {
                    continue;
                    }

                synchronized (setSocket)
                    {
                    setSocket.add(clientSocket);

                    // scan for closed sockets
                    for (Iterator<Socket> iter = setSocket.iterator(); iter.hasNext(); )
                        {
                        Socket socket = iter.next();
                        if (socket.isClosed())
                            {
                            iter.remove();
                            }
                        }
                    }

                Thread thread = new Thread(new SocketProcessor(clientSocket));
                thread.setName("SocketProcessor");
                thread.setDaemon(true);
                thread.start();
                }
            }
        catch (IOException e)
            {
            if (!isStopping())
                {
                log(ClassHelper.getSimpleName(getClass()) +
                        " exiting due to unhandled exception: " +
                        printStackTrace(e));
                }
            }
        catch (Exception e)
            {
            log(ClassHelper.getSimpleName(getClass()) +
                    " exiting due to unhandled exception: " +
                    printStackTrace(e));
            }
        finally
            {
            cleanup();
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void start()
        {
        if (m_socket == null)
            {
            try
                {
                // create the ServerSocket, bind it to the loopback adapter,
                // and start listening for connections on the configured port
                ServerSocket socket = m_provider.openServerSocket();
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress("127.0.0.1", m_nPort));

                log(ClassHelper.getSimpleName(getClass()) +
                        " listening for connections on " +
                        socket.getInetAddress() + ":" +
                        socket.getLocalPort());

                m_socket = socket;
                }
            catch (IOException e)
                {
                cleanup();
                throw ensureRuntimeException(e);
                }
            }

        super.start();
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void stop()
        {
        super.stop();
        cleanup();
        }


    // ----- application entry point ----------------------------------------

    /**
    * Usage: java com.oracle.coherence.testing.net.EchoServer [port]
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        if (asArg.length != 1)
            {
            log("Usage: java com.oracle.coherence.testing.net.EchoServer [port]");
            return;
            }

        int nPort;
        try
            {
            nPort = Integer.valueOf(asArg[0]);
            }
        catch (NumberFormatException e)
            {
            log("Usage: java com.oracle.coherence.testing.net.EchoServer [port]");
            return;
            }

        EchoServer server = new EchoServer(
            new DemultiplexedSocketProvider(
                (MultiplexedSocketProvider)SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, 1), nPort);
        server.start();
        while (server.isRunning())
            {
            synchronized (server)
                {
                try
                    {
                    Blocking.wait(server);
                    }
                catch (InterruptedException e)
                    {
                    server.stop();
                    }
                }
            }
        }


    // ----- inner class: DaemonAcceptor ------------------------------------

    /**
    * Instantiate a DaemonWorker that will be used as a daemon.
    *
    * @return a new instance of DaemonWorker or a sub-class thereof
    */
    protected DaemonWorker instantiateWorker()
        {
        return new DaemonAcceptor();
        }

    /**
    * Main acceptor thread.
    */
    public class DaemonAcceptor
            extends DaemonWorker
        {
        /**
        * {@inheritDoc}
        */
        protected void notifyStopping()
            {
            super.notifyStopping();
            getThread().interrupt();
            }
        }


    // ----- inner class: SocketProcessor -----------------------------------

    /**
    * Runnable implementation that reads data from an associated socket and
    * echos it back until an I/O error occurs or the socket is closed.
    */
    public class SocketProcessor
            implements Runnable
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a new SocketProcessor that processes the given socket.
        *
        * @param socket  the socket to process
        */
        public SocketProcessor(Socket socket)
            {
            if (socket == null)
                {
                throw new IllegalArgumentException();
                }
            m_socket = socket;
            }

        // ----- Runnable interface -------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            Socket socket = m_socket;

            log(ClassHelper.getSimpleName(getClass()) +
                    " accepted connection from: " +
                    socket.getRemoteSocketAddress());

            try
                {
                DataInputStream  in  = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                byte[]           ab  = new byte[2048];

                while (!socket.isClosed())
                    {
                    if (!m_fEcho)
                        {
                        // just read and discard
                        in.readByte();
                        }

                    if (in.readInt() != EchoNIOClient.MAGIC)
                        {
                        throw new IOException();
                        }
                    int cb = in.readInt();
                    out.writeInt(EchoNIOClient.MAGIC);
                    out.writeInt(cb);
                    while (cb > 0)
                        {
                        int cbNew = in.read(ab);
                        if (cbNew < 0)
                            {
                            // EOF
                            return;
                            }
                        out.write(ab, 0, cbNew);
                        out.flush();
                        cb -= cbNew;
                        }
                    }
                }
            catch (IOException e)
                {
                // ignore
                }
            finally
                {
                try
                    {
                    Set<Socket> setSocket = EchoServer.this.m_setSocket;
                    synchronized (setSocket)
                        {
                        setSocket.remove(socket);
                        }
                    socket.close();
                    log(ClassHelper.getSimpleName(getClass()) +
                            " dropped connection from: " +
                            socket.getRemoteSocketAddress());
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }

        // ----- data members -------------------------------------------

        /**
        * The socket to process.
        */
        protected final Socket m_socket;
        }

    /**
     * Specify if the server is to echo or consume input from the client.
     *
     * @param fEcho true for echo, false for consume
     */
    public void setEcho(boolean fEcho)
        {
        m_fEcho = fEcho;
        }

    /**
     * Return true if the server is configured to echo input back to the client.
     *
     * @return true for echo, false for consume
     */
    public boolean getEcho()
        {
        return m_fEcho;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The SocketProvider used by the EchoServer to create a ServerSocket.
    */
    protected final SocketProvider m_provider;

    /**
    * The port to listen on for client connections.
    */
    protected final int m_nPort;

    /**
    * The set of open Socket connections.
    */
    protected final Set<Socket> m_setSocket = new HashSet<Socket>();

    /**
    * The ServerSocket used by the EchoServer.
    */
    protected ServerSocket m_socket;

    /**
     * Indicator if the server is to echo back clients data, or false to simply consume it.
     */
    protected boolean m_fEcho = true;
    }
