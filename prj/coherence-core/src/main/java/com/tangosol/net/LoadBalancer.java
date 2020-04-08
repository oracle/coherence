/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.util.Base;
import com.tangosol.util.Daemon;
import com.tangosol.util.RecyclingLinkedList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;


/**
* A non-sticky HTTP load-balancer.
*
* @author Cameron Purdy
* @version 1.0, 2002-07-29
*/
public class LoadBalancer
        extends Base
        implements Runnable
    {
    // ----- command line ---------------------------------------------------

    /**
    * Command-line capability to start the load balancer.
    */
    public static void main(String[] asArgs)
        {
        AddressPort addrportHost = null;
        List        listAddrPort = new ArrayList();
        Properties  propOptions  = new Properties();

        for (int iArg = 0, cArgs = asArgs == null ? 0 : asArgs.length; iArg < cArgs; ++iArg)
            {
            String sArg = asArgs[iArg];
            if (sArg != null & sArg.length() > 0)
                {
                if (sArg.charAt(0) == '-')
                    {
                    int ofEq = sArg.indexOf('=');
                    if (ofEq < 0)
                        {
                        propOptions.put(sArg.substring(1), "true");
                        }
                    else
                        {
                        propOptions.put(sArg.substring(1, ofEq), sArg.substring(ofEq + 1));
                        }
                    }
                else
                    {
                    // assume it is an IP address
                    int ofColon = sArg.indexOf(':');
                    if (ofColon < 0)
                        {
                        showInstructions();
                        return;
                        }

                    String sAddr   = sArg.substring(0, ofColon);
                    String sPort   = sArg.substring(ofColon + 1);

                    if (sAddr.length() == 0 || sPort.length() == 0)
                        {
                        out("IP and port are required for both the host and all destination addresses");
                        showInstructions();
                        return;
                        }

                    InetAddress addr;
                    int         nPort;
                    try
                        {
                        addr  = InetAddress.getByName(sAddr);
                        nPort = Integer.parseInt(sPort);
                        }
                    catch (Exception e)
                        {
                        out(e);
                        showInstructions();
                        return;
                        }

                    if (nPort < 1 || nPort > 0xFFFF)
                        {
                        out("Illegal port value: " + nPort);
                        showInstructions();
                        return;
                        }

                    AddressPort addrport = new AddressPort(addr, nPort);
                    if (addrportHost == null)
                        {
                        addrportHost = addrport;
                        }
                    else
                        {
                        listAddrPort.add(addrport);
                        }
                    }
                }
            }

        if (addrportHost == null || listAddrPort.isEmpty())
            {
            showInstructions();
            return;
            }

        LoadBalancer lb;
        try
            {
            lb = new LoadBalancer(addrportHost, (AddressPort[])
                    listAddrPort.toArray(new AddressPort[listAddrPort.size()]), propOptions);
            }
        catch (Exception e)
            {
            out(e);
            showInstructions();
            return;
            }

        lb.run();
        }

    /**
    * Display the instructions for the command-line utility.
    */
    public static void showInstructions()
        {
        out();
        out("java LoadBalancer <host-ip>:<host-port> <ip>:<port> <ip>:<port> ...");
        out();
        }


    // ----- constructors ---------------------------------------------------

    /**
    * Instantiate a LoadBalancer object that will listen on a host
    * address/port and redirect requests to destination addresses/ports.
    *
    * @param addrportHost   the AddressPort combination for this host
    * @param aAddrPortDest  the array of AddressPort combinations that
    *                       requests will be sent to
    */
    public LoadBalancer(AddressPort addrportHost, AddressPort[] aAddrPortDest, Properties propOptions)
        {
        // check params
        azzert(addrportHost != null);
        azzert(aAddrPortDest != null);
        for (int i = 0, c = aAddrPortDest.length; i < c; ++i)
            {
            azzert(aAddrPortDest[i] != null);
            }
        azzert(propOptions != null);

        // store addresses & options
        m_addrportHost  = addrportHost;
        m_aAddrPortDest = aAddrPortDest;
        m_propOptions   = propOptions;
        }


    // ----- Runnable interface ---------------------------------------------

    /**
    * Start the LoadBalancer.
    */
    public void run()
        {
        synchronized (this)
            {
            if (m_threadRunning == null)
                {
                m_threadRunning = Thread.currentThread();
                }
            else
                {
                throw new IllegalStateException("Load balancer cannot be started"
                        + " on more than one thread or restarted once it stops");
                }
            }

        int     cBacklog   = getProperty("backlog", 64);
        int     cThreads   = getProperty("threads", 64);
        boolean fKeepAlive = getProperty("keepalive", true);

        // create conection queue (for pending requests)
        Queue queue;
        m_queue = queue = instantiateQueue();

        // create worker threads
        List listHandler = m_listHandler;
        for (int i = 0; i < cThreads; ++i)
            {
            listHandler.add(instantiateRequestHandler(queue));
            }

        // determine load-balancing approach
        m_fRoundRobin = getProperty("roundrobin", false);

        AddressPort  addrportHost = getHost();
        ServerSocket socketAccept = null;
        try
            {
            // open socket to listen for connections
            try
                {
                socketAccept = new ServerSocket(addrportHost.nPort, cBacklog, addrportHost.address);
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            // listen for connections (requests)
            while (true)
                {
                Socket socket = null;
                try
                    {
                    socket = socketAccept.accept();
                    socket.setKeepAlive(fKeepAlive);

                    if (!fKeepAlive)
                        {
                        socket.setSoTimeout(1000);
                        }
                    }
                catch (IOException e) {}

                if (socket != null)
                    {
                    queue.add(socket);
                    }
                }
            }
        finally
            {
            // close server socket
            if (socketAccept != null)
                {
                try
                    {
                    socketAccept.close();
                    }
                catch (Exception e) {}
                }

            // close all socket connections (requests)
            while (!queue.isEmpty())
                {
                Socket socket = (Socket) queue.removeNoWait();
                if (socket != null)
                    {
                    try
                        {
                        socket.close();
                        }
                    catch (Exception e) {}
                    }
                }
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the AddressPort that the load balancer listens on.
    *
    * @return the AddressPort that the load balancer listens on
    */
    public AddressPort getHost()
        {
        return m_addrportHost;
        }

    /**
    * Determine the number of AddressPort combinations that the load balancer
    * balances requests to.
    */
    public int getDestinationCount()
        {
        AddressPort[] aAddrPort = m_aAddrPortDest;
        return aAddrPort == null ? 0 : aAddrPort.length;
        }

    /**
    * Determine one of the AddressPort combinations that the load balancer
    * balances requests to. (Indexed property "Destination".)
    *
    * @param i  an index in the range <tt>0 &lt; i &lt; getDesinationCount()</tt>
    *
    * @return the AddressPort that the load balancer listens on
    */
    public AddressPort getDestination(int i)
        {
        return m_aAddrPortDest[i];
        }

    /**
    * Open a socket to route to.
    *
    * @return the next available Socket to route a request to
    */
    public Socket getNextDestinationSocket()
        {
        for (int i = 0; i < 128; ++i)
            {
            AddressPort addrport = getNextDestination();
            if (addrport == null)
                {
                return null;
                }

            try
                {
                Socket socket = new Socket(addrport.address, addrport.nPort);

                // TODO remove
                out("routing to " + addrport);

                return socket;
                }
            catch (Exception e)
                {
                // TODO remove
                out("*** could not connect to " + addrport + "; " + e);
                }
            }

        return null;
        }

    /**
    * Determine the next AddressPort combination to route to.
    *
    * @return the next AddressPort combination to route a request to
    */
    protected AddressPort getNextDestination()
        {
        return m_fRoundRobin ? getRoundRobinDestination()
                             : getRandomDestination();
        }

    /**
    * Determine a random AddressPort combination to route to.
    *
    * @return a random AddressPort combination to route a request to
    */
    protected AddressPort getRandomDestination()
        {
        int c = getDestinationCount();
        if (c == 0)
            {
            return null;
            }

        int i = m_random.nextInt(c);
        return getDestination(i);
        }

    /**
    * Using a round-robin algorithm, determine the next AddressPort
    * combination to route to.
    *
    * @return the next AddressPort combination to route a request to
    */
    protected AddressPort getRoundRobinDestination()
        {
        int c = getDestinationCount();
        if (c == 0)
            {
            return null;
            }

        int i;
        synchronized (this)
            {
            i = m_iNextDestination;
            m_iNextDestination = (i + 1 >= c) ? 0 : i + 1;
            }

        return getDestination(i);
        }

    /**
    * Determine the Queue that Socket connections are placed into.
    *
    * @return the Queue of Socket objects that have been accepted
    */
    protected Queue getQueue()
        {
        return m_queue;
        }

    /**
    * Determine the value of a String option.
    *
    * @param sName     the property name that specifies the option to look up
    * @param sDefault  the default option value to use if the option is not
    *                  set
    *
    * @return the value of the specified option, or the passed default value
    *         if the option is not set
    */
    public String getProperty(String sName, String sDefault)
        {
        return m_propOptions.getProperty(sName, sDefault);
        }

    /**
    * Determine the value of an integer option.
    *
    * @param sName     the property name that specifies the option to look up
    * @param nDefault  the default option value to use if the option is not
    *                  set
    *
    * @return the value of the specified option, or the passed default value
    *         if the option is not set
    */
    public int getProperty(String sName, int nDefault)
        {
        String sValue = m_propOptions.getProperty(sName);
        if (sValue != null)
            {
            try
                {
                return Integer.parseInt(sValue);
                }
            catch (Exception e) {}
            }
        return nDefault;
        }

    /**
    * Determine the value of a boolean option.
    *
    * @param sName     the property name that specifies the option to look up
    * @param fDefault  the default option value to use if the option is not
    *                  set
    *
    * @return the value of the specified option, or the passed default value
    *         if the option is not set
    */
    public boolean getProperty(String sName, boolean fDefault)
        {
        String sValue = m_propOptions.getProperty(sName);
        if (sValue != null)
            {
            try
                {
                switch (sValue.charAt(0))
                    {
                    case '0':   // 0 (binary false)
                    case 'N':   // NO
                    case 'n':   // no
                    case 'F':   // FALSE
                    case 'f':   // false
                        return false;

                    case '1':   // 1 (binary true)
                    case 'Y':   // YES
                    case 'y':   // yes
                    case 'T':   // TRUE
                    case 't':   // true
                        return true;
                    }
                }
            catch (Exception e) {}
            }
        return fDefault;
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Parse the extension glued onto the end of a daemon's thread's name.
    *
    * @param daemon  a Daemon object
    *
    * @return the extension glued onto the end of a daemon's thread's name
    */
    protected static String parseThreadExtension(Daemon daemon)
        {
        Thread thread = daemon.getThread();
        if (thread == null)
            {
            return "";
            }
        String sName    = thread.getName();
        int    ofHyphen = sName.indexOf('-');
        return ofHyphen < 0 ? "" : sName.substring(ofHyphen);
        }


    // ----- inner class: AdressPort ----------------------------------------

    /**
    * An AddressPort is an immutable combination of an IP address and a port
    * number.
    */
    public static class AddressPort
            extends Base
        {
        /**
        * Construct an AddressPort object.
        */
        public AddressPort(InetAddress address, int nPort)
            {
            azzert(address != null);
            azzert(nPort > 0 && nPort <= 0xFFFF);

            this.address = address;
            this.nPort   = nPort;
            }

        /**
        * Return a human readable description of this AddressPort object.
        *
        * @return a human readable description of this AddressPort object
        */
        public String toString()
            {
            return address.getHostAddress() + ":" + nPort;
            }

        /**
        * The address.
        */
        public final InetAddress address;

        /**
        * The port.
        */
        public final int nPort;
        }


    // ----- inner class: Queue ---------------------------------------------

    /**
    * Factory method: Create a queue.
    *
    * @return a Queue instance
    */
    protected Queue instantiateQueue()
        {
        return new Queue();
        }

    /**
    * A Queue is used to effeciently queue up items for daemon threads to
    * work on.
    */
    public static class Queue
            extends Base
        {
        /**
        * Construct a queue.
        */
        public Queue()
            {
            m_list = new RecyclingLinkedList();
            }

        /**
        * Determine the number of items in the queue.
        *
        * @return the number of items in the queue
        */
        public int size()
            {
            return m_list.size();
            }

        /**
        * Determine if the queue is empty.
        *
        * @return true if the queue currently has no items in it
        */
        public boolean isEmpty()
            {
            return m_list.isEmpty();
            }

        /**
        * Add an object to the end of the queue.
        *
        * @param o  the item to add to the end of the queue
        */
        public synchronized void add(Object o)
            {
            m_list.add(o);
            notify();
            }

        /**
        * Wait for and remove an item from the from of the queue.
        *
        * @return an item from the queue
        *
        * @throws InterruptedException  if the thread is interrupted while
        *         waiting for something to be added to the queue
        */
        public synchronized Object remove()
                throws InterruptedException
            {
            List list = m_list;
            while (list.isEmpty())
                {
                Blocking.wait(this);
                }

            return list.remove(0);
            }

        /**
        * Remove an item from the queue if the queue is not empty.
        *
        * @return an item if the queue is not empty, otherwise null
        */
        public synchronized Object removeNoWait()
            {
            List list = m_list;
            if (list.isEmpty())
                {
                return null;
                }
            else
                {
                return list.remove(0);
                }
            }

        /**
        * A list of items that have been queued.
        */
        protected List m_list;
        }


    // ----- inner class: SocketHandler -------------------------------------

    /**
    * A SocketHandler is an abstract daemon thread.
    */
    public abstract static class SocketHandler
            extends Daemon
        {
        /**
        * Construct a SocketHandler with a given daemon thread name.
        */
        public SocketHandler(String sName)
            {
            super(sName, Thread.NORM_PRIORITY, false);
            }

        /**
        * Processing loop for the SocketHandler.
        */
        public abstract void run();

        /**
        * Process the transfer of data from one socket to another.
        *
        * @param socketIn   the socket to read from
        * @param socketOut  the socket to write to
        */
        protected void process(Socket socketIn, Socket socketOut)
                throws IOException
            {
            try
                {
                InputStream  streamIn  = socketIn.getInputStream();
                OutputStream streamOut = socketOut.getOutputStream();
                copy(streamIn, streamOut, ensureBuffer(socketIn));
                }
            catch (SocketException e)
                {
                try
                    {
                    socketIn.close();
                    }
                catch (Exception eIgnore) {}

                try
                    {
                    socketOut.close();
                    }
                catch (Exception eIgnore) {}

                throw e;
                }

            }

        /**
        * Process the transfer of data from one stream to another.
        *
        * @param streamIn   the stream to read from
        * @param streamOut  the stream to write to
        * @param abBuf      the byte array to use as a buffer to read into
        *                   and write from
        */
        protected void copy(InputStream streamIn, OutputStream streamOut, byte[] abBuf)
                throws IOException
            {
            int cb;
            while ((cb = streamIn.read(abBuf)) != -1)
                {
                if (cb > 0)
                    {
                    streamOut.write(abBuf, 0, cb);
                    }
                }
            }

        /**
        * Return the existing buffer, if there is one, or create one to use
        * for reading from the passed socket.
        *
        * @param socket  the socket that the buffer will be used to read from
        *
        * @return a byte array to use as a read buffer
        */
        protected byte[] ensureBuffer(Socket socket)
            {
            byte[] abBuf = m_abBuf;
            if (abBuf == null)
                {
                int cb = 0;
                try
                    {
                    cb = socket.getReceiveBufferSize();
                    }
                catch (SocketException e) {}

                if (cb < 1)
                    {
                    cb = 1024;
                    }

                if (cb > 16384)
                    {
                    cb = 16384;
                    }

                m_abBuf = abBuf = new byte[cb];
                }

            return abBuf;
            }

        /**
        * The buffer to use for reading from a stream.
        */
        protected byte[] m_abBuf;
        }


    // ----- inner class: RequestHandler ------------------------------------

    /**
    * Factory method: Create a RequestHandler.
    *
    * @param queue  a Queue of Socket objects
    *
    * @return a RequestHandler instance
    */
    protected RequestHandler instantiateRequestHandler(Queue queue)
        {
        return new RequestHandler(queue);
        }

    /**
    * A RequestHandler is a daemon thread that processes a request from a
    * queue.
    */
    public class RequestHandler
            extends SocketHandler
        {
        /**
        * Constructs a RequestHandler that will pull request connections
        * (Socket objects) from a Queue.
        *
        * @param queue  a Queue of Socket objects
        */
        public RequestHandler(Queue queue)
            {
            super("RequestHandler-" + m_listHandler.size());
            m_queue = queue;
            m_daemonResponse = instantiateResponseHandler(this);
            start();
            }

        /**
        * Processing loop for the RequestHandler daemon.
        */
        public void run()
            {
            Queue queue = m_queue;
            azzert(queue != null);

            ResponseHandler daemonResponse = m_daemonResponse;
            azzert(daemonResponse != null);

            while (true)
                {
                Socket socketClient = null;
                Socket socketServer = null;
                try
                    {
                    socketClient = (Socket) queue.remove();
                    socketServer = LoadBalancer.this.getNextDestinationSocket();

                    daemonResponse.relayResponse(socketServer, socketClient);
                    process(socketClient, socketServer);
                    }
                catch (InterruptedException e)
                    {
                    break;
                    }
                catch (Exception e)
                    {
                    // TODO remove
                    // err(e);
                    }
                finally
                    {
                    try
                        {
                        socketClient.close();
                        }
                    catch (Exception e) {}

                    try
                        {
                        socketServer.close();
                        }
                    catch (Exception e) {}
                    }
                }
            }

        /**
        * The Queue that this RequestHandler listens to connections (Socket)
        * on.
        */
        protected Queue m_queue;

        /**
        * The ResponseHandler that handles the server-to-client response
        * routing corresponding to requests routed client-to-server by this
        * daemon.
        */
        protected ResponseHandler m_daemonResponse;
        }


    // ----- inner class: ResponseHandler -----------------------------------

    /**
    * Factory method: Create a ResponseHandler.
    *
    * @param daemonRequest  the RequestHandler that the ResponseHandler will
    *                       belong to
    *
    * @return a ResponseHandler instance
    */
    protected ResponseHandler instantiateResponseHandler(RequestHandler daemonRequest)
        {
        return new ResponseHandler(daemonRequest);
        }

    /**
    * A ResponseHandler is a daemon thread that processes an outgoing
    * response from a destination server.
    */
    public class ResponseHandler
            extends SocketHandler
        {
        /**
        * Construct a ResponseHandler that belongs to the specified
        * RequestHandler.
        *
        * @param daemonRequest  the RequestHandler that this ResponseHandler
        *                       belongs to
        */
        public ResponseHandler(RequestHandler daemonRequest)
            {
            super("ResponseHandler" + parseThreadExtension(daemonRequest));
            m_daemonRequest = daemonRequest;
            start();
            }

        /**
        * Processing loop for the ResponseHandler daemon.
        */
        public synchronized void run()
            {
            Socket socketServer = null;
            Socket socketClient = null;
            while (true)
                {
                try
                    {
                    Blocking.wait(this);

                    socketServer = m_socketServer;
                    socketClient = m_socketClient;

                    if (socketServer == null || socketClient == null)
                        {
                        continue;
                        }

                    process(socketServer, socketClient);
                    }
                catch (InterruptedException e)
                    {
                    break;
                    }
                catch (Exception e)
                    {
                    // TODO remove
                    // err(e);
                    }
                finally
                    {
                    try
                        {
                        socketClient.close();
                        }
                    catch (Exception e) {}

                    try
                        {
                        socketServer.close();
                        }
                    catch (Exception e) {}
                    }
                }
            }

        /**
        * This method is used to assign a task to the ResponseHandler thread
        * from the RequestHandler thread.
        *
        * @param socketServer  the socket to copy from
        * @param socketClient  the socket to copy to
        */
        public synchronized void relayResponse(Socket socketServer, Socket socketClient)
            {
            m_socketClient = socketClient;
            m_socketServer = socketServer;
            notify();
            }

        /**
        * The RequestHandler that this ResponseHandler belongs to.
        */
        protected RequestHandler m_daemonRequest;

        /**
        * The Socket to read the response from.
        */
        protected Socket m_socketServer;

        /**
        * The Socket to write the response to.
        */
        protected Socket m_socketClient;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The AddressPort combination that the load balancer will listen on.
    */
    protected AddressPort   m_addrportHost;

    /**
    * The AddressPort combinations that the load balancer will balance to.
    */
    protected AddressPort[] m_aAddrPortDest;

    /**
    * The optional settings that the load balancer will use.
    */
    protected Properties    m_propOptions;

    /**
    * The Thread that the load balancer is started on.
    */
    protected Thread        m_threadRunning;

    /**
    * The queue of pending requests.
    */
    protected Queue         m_queue;

    /**
    * The list of RequestHandler daemons.
    */
    protected List          m_listHandler = new ArrayList();

    /**
    * Toggles between random and round-robin load balancing.
    */
    protected boolean       m_fRoundRobin;

    /**
    * Random number generator.
    */
    protected Random        m_random = getRandom();

    /**
    * Round-robin index.
    */
    protected int           m_iNextDestination;
    }