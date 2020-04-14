/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.discovery;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.concurrent.atomic.AtomicReference;

import javax.management.remote.JMXServiceURL;

/**
 * NSLookup is a helper class for doing lookup from the Coherence NameService.
 * <p>
 * This helper class has no dependency on any other Coherence class and can be
 * used to do NameService lookup without requiring coherence jar.
 *
 * <ul>
 * <li>Using the NSLookup class for JMX query
 *    <pre>
 *    JMXServiceURL         jmxServiceURL = NSLookup.lookupJMXServiceURL(new InetSocketAddress("127.0.0.1", 8888), 0);
 *    JMXConnector          jmxConnector  = JMXConnectorFactory.connect(jmxServiceURL, null);
 *    MBeanServerConnection conn          = jmxConnector.getMBeanServerConnection();
 *    System.out.println(conn.queryNames(new javax.management.ObjectName("Coherence:type=Cluster,*"), null));
 *    </pre>
 * </li>
 * </ul>
 *
 * @author bb  2014.04.24
 */
public class NSLookup
    {
    /**
     * Lookup the current management node mbean connector url.
     *
     * @param socketAddr  Unicast socket address of the coherence cluster node
     *
     * @return JMXServiceURL for the management node
     *
     * @throws IOException if an I/O error occurs while doing the JMXServiceURL lookup
     */
    public static JMXServiceURL lookupJMXServiceURL(SocketAddress socketAddr)
            throws IOException
        {
        return lookupJMXServiceURL(null, socketAddr);
        }

    /**
     * Lookup the current management node mbean connector url.
     *
     * @param sCluster    the target cluster
     * @param socketAddr  Unicast socket address of the coherence cluster node
     *
     * @return JMXServiceURL for the management node
     *
     * @throws IOException if an I/O error occurs while doing the JMXServiceURL lookup
     */
    public static JMXServiceURL lookupJMXServiceURL(String sCluster, SocketAddress socketAddr)
            throws IOException
        {
        String sURL = lookup(sCluster, JMX_CONNECTOR_URL, socketAddr, DEFAULT_TIMEOUT);
        return sURL == null ? null : new JMXServiceURL(sURL);
        }

    /**
     * Lookup the current management HTTP connector URL.
     *
     * @param socketAddr  Unicast socket address of the coherence cluster node
     *
     * @return a collection of URLs which can be used to access an HTTP management service
     *
     * @throws IOException if an I/O error occurs while doing the URL lookup
     *
     * @since 12.2.1.4.0
     */
    public static Collection<URL> lookupHTTPManagementURL(SocketAddress socketAddr)
        throws IOException
        {
        return lookupHTTPManagementURL(null, socketAddr);
        }

    /**
     * Lookup the current management HTTP connector URL.
     *
     * @param sCluster    the target cluster
     * @param socketAddr  Unicast socket address of the coherence cluster node
     *
     * @return a collection of URLs which can be used to access an HTTP management service
     *
     * @throws IOException if an I/O error occurs while doing the URL lookup
     *
     * @since 12.2.1.4.0
     */
    public static Collection<URL> lookupHTTPManagementURL(String sCluster, SocketAddress socketAddr)
            throws IOException
        {
        // NSLookup only knows how to deserialize strings, so must request the string form
        // and then reverse engineer it to the collection of URLs

        Collection<URL> colUrl = new ArrayList<>();
        String          sURL   = lookup(sCluster, NS_STRING_PREFIX + HTTP_MANAGEMENT_URL, socketAddr, DEFAULT_TIMEOUT);

        // do not use streams as this class needs to be buildable on Java 7
        // format is "[URL1, URL2, URL3, ...]"
        String []       asURL  = sURL == null ? null : sURL.split("[\\[,\\] ]+");

        if (asURL != null)
            {
            // skip element 0 which will be an empty string
            for (int i = 1; i < asURL.length; ++i)
                {
                colUrl.add(new URL(asURL[i]));
                }
            }
        return colUrl;
        }

    /**
     * Lookup the current metrics HTTP connector URLs for current cluster.
     *
     * @param socketAddr  Unicast socket address of the coherence cluster node
     *
     * @return a collection of URLs which can be used to access a HTTP metrics endpoints
     *
     * @throws IOException if an I/O error occurs while doing the URL lookup
     *
     * @since 12.2.1.4.0
     */
    public static Collection<URL> lookupHTTPMetricsURL(SocketAddress socketAddr)
        throws IOException
        {
        return lookupHTTPMetricsURL(null, socketAddr);
        }

    /**
     * Lookup the current metrics HTTP connector URLs for a specified cluster.
     *
     * @param sCluster    the target cluster
     * @param socketAddr  Unicast socket address of the coherence cluster node
     *
     * @return a collection of URLs which can be used to access a HTTP metrics endpoint
     *
     * @throws IOException if an I/O error occurs while doing the URL lookup
     *
     * @since 12.2.1.4.0
     */
    public static Collection<URL> lookupHTTPMetricsURL(String sCluster, SocketAddress socketAddr)
        throws IOException
        {
        // NSLookup only knows how to deserialize strings, so must request the string form
        // and then reverse engineer it to the collection of URLs

        Collection<URL> colUrl = new ArrayList<>();
        String          sURL   = lookup(sCluster, NS_STRING_PREFIX + HTTP_METRICS_URL, socketAddr, DEFAULT_TIMEOUT);

        // do not use streams as this class needs to be buildable on Java 7
        // format is "[URL1, URL2, URL3, ...]"
        String []       asURL  = sURL == null ? null : sURL.split("[\\[,\\] ]+");

        if (asURL != null)
            {
            // skip element 0 which will be an empty string
            for (int i = 1; i < asURL.length; ++i)
                {
                colUrl.add(new URL(asURL[i]));
                }
            }
        return colUrl;
        }

    public static class Connection
        implements Closeable
        {
        protected Connection(SocketAddress socketAddr, int cTimeOutMillis)
                throws IOException
            {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(cTimeOutMillis);
            socket.connect(socketAddr, cTimeOutMillis);

            outStream = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));

            inStream = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));

            // connect to NameService sub-port
            outStream.writeInt(MULTIPLEXED_SOCKET);
            outStream.writeInt(NAMESERVICE_SUBPORT);
            write(outStream, CONN_OPEN);    // write open connection request
            write(outStream, CHANNEL_OPEN); // write open channel request
            outStream.flush();              // send open requests

            read(inStream);                        // wait for and skip over response to connect request
            byte[] aChanResponse = read(inStream); // wait for open channel response

            // extract the channel id from the response, this appears to start at offset 8, but not including
            // the last byte, the chan id is variable length, so we can't just walk back from the tail.
            // the header is also technically variable length as it contains packed ints, but unlike the channel id they
            // are constants and thus appear to be non-variable for this specific request.  This is only true
            // because we limit ourselves to a single request per connection.
            // TODO: properly parse the header
            int cbChanId = aChanResponse.length - 9;
            abChan = Arrays.copyOfRange(aChanResponse, 8, 8 + cbChanId);
            }

        public static Connection open(String sCluster, SocketAddress socketAddr, int cTimeOutMillis)
                throws IOException
            {
            Connection connection = null;
            do
                {
                connection = new Connection(socketAddr, cTimeOutMillis);
                if (sCluster != null && !sCluster.equals(connection.lookup("Cluster/name")))
                    {
                    String sPort = connection.lookup(NS_STRING_PREFIX + "Cluster/foreign/" + sCluster + "/NameService/localPort");
                    connection.close();
                    connection = null;
                    if (sPort == null)
                        {
                        throw new IOException(sCluster == null
                                ? "no cluster could be located"
                                : "cluster '" + sCluster + "' could not be located");
                        }

                    socketAddr = new InetSocketAddress(((InetSocketAddress) socketAddr).getAddress(), Integer.valueOf(sPort));
                    }
                }
            while (connection == null);

            return connection;
            }

        public String lookup(String sName)
                throws IOException
            {
            DataInputStream stream = lookupRaw(sName);
            return stream == null ? null : readString(stream);
            }

        public DataInputStream lookupRaw(String sName)
                throws IOException
            {
            // build the lookup request.
            ByteArrayOutputStream baos      = new ByteArrayOutputStream();
            DataOutputStream      reqStream = new DataOutputStream(baos);

            reqStream.write(abChan);

            // write request id
            reqStream.write(NS_LOOKUP_REQ_ID, 0, NS_LOOKUP_REQ_ID.length);

            byte[] abName = sName.getBytes("utf-8");

            // write length of the lookup string
            writePackedInt(reqStream, abName.length);

            // write lookup string
            reqStream.write(abName, 0, abName.length);

            // write terminating byte
            reqStream.write(REQ_END_MARKER);
            reqStream.flush();

            // write lookup request
            write(outStream, baos.toByteArray());
            outStream.flush(); // send request

            // wait for the request response
            byte[] aResponse = read(inStream);
            int    nLen      = aResponse.length;
            int    nMinLen   = abChan.length + 1;

            if (nLen <= nMinLen)
                {
                throw new EOFException("protocol error");
                }
            else if (nLen == nMinLen + 7)
                {
                // just has the terminating byte
                return null;
                }

            // strip channel id and request id from the response
            return new DataInputStream(
                    new ByteArrayInputStream(aResponse, nMinLen, nLen - nMinLen - 1));
            }

        public void close()
                throws IOException
            {
            socket.close();
            }

        final Socket socket;
        final DataOutputStream outStream;
        final DataInputStream  inStream;
        final byte[] abChan;
        }

    /**
     * Lookup the given name from the NameService. The object bound to the NameService
     * should be a String.
     *
     * @param sName           Name to lookup
     * @param socketAddr      Unicast socket address of the coherence cluster node
     * @param cTimeOutMillis  timeout in millis
     *
     * @return String bound to the NameService with the given name
     *
     * @throws IOException if an I/O error occurs while doing the lookup
     */
    public static String lookup(String sName, SocketAddress socketAddr, int cTimeOutMillis)
            throws IOException
        {
        try (Connection connection = new Connection(socketAddr, cTimeOutMillis))
            {
            return connection.lookup(sName);
            }
        }

    /**
     * Lookup the given name from the NameService. The object bound to the NameService
     * should be a String.
     *
     * @param sName           Name to lookup
     * @param socketAddr      Unicast socket address of the coherence cluster node
     * @param cTimeOutMillis  timeout in millis
     *
     * @return String bound to the NameService with the given name
     *
     * @throws IOException if an I/O error occurs while doing the lookup
     */
    public static String lookup(String sCluster, String sName, SocketAddress socketAddr, int cTimeOutMillis)
            throws IOException
        {
        try (Connection conn = Connection.open(sCluster, socketAddr, cTimeOutMillis))
            {
            return conn.lookup(sName);
            }
        }

    /**
     * Write length encoded message.
     *
     * @param outStream  socket output stream
     * @param ab         message byte[] to write
     *
     * @throws IOException if an I/O error occurs while writing to the socket stream
     */
    private static void write(DataOutputStream outStream, byte[] ab)
            throws IOException
        {
        int cb = ab.length;
        writePackedInt(outStream, cb);

        // Message contents
        outStream.write(ab, 0, cb);
        }

    /**
     * Read length encoded message.
     *
     * @param inStream  socket input stream
     *
     * @return message byte[]
     *
     * @throws IOException if an I/O error occurs while reading from the socket stream
     */
    protected static byte[] read(DataInputStream inStream)
               throws IOException
        {
        int cb = readPackedInt(inStream);
        if (cb < 0)
            {
            throw new IOException("Received a message with a negative length");
            }
        else if (cb == 0)
            {
            throw new IOException("Received a message with a length of zero");
            }
        else
            {
            byte[] ab = new byte[cb];
            inStream.readFully(ab);
            return ab;
            }
        }

    /**
     * Write packed int.
     *
     * @param outStream  socket output stream
     * @param n          int to write
     *
     * @throws IOException if an I/O error occurs while writing to the socket stream
     */
    private static void writePackedInt(DataOutputStream outStream, int n)
            throws IOException
        {
        // first byte contains sign bit (bit 7 set if neg)
        int b = 0;
        if (n < 0)
            {
            b = 0x40;
            n = ~n;
            }

        // first byte contains only 6 data bits
        b |= (byte) (n & 0x3F);
        n >>>= 6;

        while (n != 0)
            {
            b |= 0x80; // bit 8 is a continuation bit
            outStream.writeByte(b);

            b = (n & 0x7F);
            n >>>= 7;
            }

        // remaining byte
        outStream.writeByte(b);
        }

    /**
     * Read packed int.
     *
     * @param inStream  socket input stream
     *
     * @return int read
     *
     * @throws IOException if an I/O error occurs while reading from the socket stream
     */
    protected static int readPackedInt(DataInputStream inStream)
                throws IOException
        {
        int     b     = inStream.readUnsignedByte();
        int     n     = b & 0x3F;       // 6 bits of data in first byte
        int     cBits = 6;
        boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit

        while ((b & 0x80) != 0)     // eighth bit is the continuation bit
            {
            b      = inStream.readUnsignedByte();
            n     |= ((b & 0x7F) << cBits);
            cBits += 7;
            }

        if (fNeg)
            {
            n = ~n;
            }

        return n;
        }

    /**
     * Validate a command parameter.
     *
     * @param sCommand   the parameter to be validated
     * @param asCommand  valid command parameter array
     * @param fCaseSens  a flag to indicate if the parameter is case sensitive
     */
    protected static String validateCommand(String sCommand, String[] asCommand, boolean fCaseSens)
        {
        if (!fCaseSens)
            {
            sCommand = sCommand.toLowerCase();
            }

        if (asCommand == null)
            {
            return sCommand;
            }

        for (int i = 0; i < asCommand.length; i++)
            {
            if (asCommand[i].equals(sCommand))
                {
                return sCommand;
                }
            }
        throw new IllegalArgumentException("Illegal command: -" + sCommand);
        }

    /**
     * Parses the array of arguments into a map.
     *
     * Assume that a java tool starts by command line having
     * the following syntax:
     *   cmd-line  ::== (command space)* (argument space)*
     *   command   ::== "-" cmd-name ("=" | ":" | space) (cmd-value)?
     *   cmd-name  ::== word
     *   cmd-value ::== word ("," word)*
     *   argument  ::== word ("," word)*
     *   space     ::== (" ")+
     *
     * When java starts an application the arguments in the command line
     * are placed into a string array by breaking at spaces.
     * The purpose of this method is to place the command line
     * into a LinkedHashMap where each <command> would represent
     * an entry in this map with values equal to <cmd-value> (null if not
     * present) and each <argument> represented with an entry that has
     * the key equal to an Integer object holding on the 0-based argument number
     * and the value equal to the argument itself.
     *
     * @param asArg      an array of arguments from "public static main(String[])"
     * @param asCommand  an array of valid commands (if null, anything is allowed)
     * @param fCaseSens  if true, uses the commands the way they are typed in;
     *                   if false, converts all the commands to lowercase.
     *
     * @throws IllegalArgumentException if the syntax is unexpected or an invalid
     *         command has been encountered;  a caller is supposed to output the
     *         "Usage: ... " message if this exception is thrown.
     *
     * @return a map of arguments with their corresponding values
     */
    public static LinkedHashMap parseArguments(String[] asArg, String[] asCommand, boolean fCaseSens)
        {
        LinkedHashMap map = new LinkedHashMap();

        String sCommand = null;
        int    iArg    = -1;

        for (int i = 0; i < asArg.length; i++)
            {
            String sArg = asArg[i];

            if (sArg.charAt(0) == '-')
                {
                // encountered a new command
                if (sCommand != null)
                    {
                    // the previous command had no value
                    map.put(sCommand, null);
                    }

                sCommand = sArg.substring(1);
                if (sCommand.length() == 0)
                    {
                    throw new IllegalArgumentException("An empty command");
                    }

                int of = sCommand.indexOf('=');
                if (of < 0)
                    {
                    of = sCommand.indexOf(':');
                    }

                if (of > 0)
                    {
                    String sValue = sCommand.substring(of + 1);

                    sCommand = validateCommand(sCommand.substring(0, of),
                            asCommand, fCaseSens);
                    map.put(sCommand, sValue);
                    sCommand = null;
                    }
                else
                    {
                    sCommand = validateCommand(sCommand, asCommand, fCaseSens);
                    }
                }
            else
                {
                if (sCommand == null)
                    {
                    // encountered an argument
                    map.put(Integer.valueOf(++iArg), sArg);
                    }
                else
                    {
                    // encountered an cmd-value
                    map.put(sCommand, sArg);
                    sCommand = null;
                    }
                }
            }

        if (sCommand != null)
            {
            // the last arg was an command without a value
            map.put(sCommand, null);
            }
        return map;
        }

    /**
     * Lookup a name via UDP (generally multicast).
     *
     * @param sCluster        the cluster of address
     * @param sName           the name to lookup
     * @param addrGroup       the cluster address
     * @param addrLocal       the NIC to use
     * @param cTimeoutMillis  the discovery timeout
     * @param nTTL            the multicast TTL
     * @param abMemberClient  optional serialized Member object representing the client
     * @param consumerResult  the consumer for results
     */
    public static void datagramLookupRaw(String sCluster, String sName, InetSocketAddress addrGroup,
            InetAddress addrLocal, int cTimeoutMillis, int nTTL, byte[] abMemberClient,
            BiConsumer<String, DataInputStream> consumerResult)
        throws IOException
        {
        Set<String> setCluster = new HashSet<>();

        try (MulticastSocket socket = new MulticastSocket())
            {
            byte[]                abAddrThis = addrLocal == null || addrLocal.isAnyLocalAddress() ? null : addrLocal.getAddress();
            ByteArrayOutputStream outBytes   = new ByteArrayOutputStream();
            DataOutputStream      out        = new DataOutputStream(outBytes);

            int cDelayMillis = 200;
            int cAttempts;

            if (cTimeoutMillis == 0)
                {
                cAttempts = Integer.MAX_VALUE;
                }
            else
                {
                cDelayMillis = Math.min(cTimeoutMillis, cDelayMillis);
                cAttempts    = cTimeoutMillis / cDelayMillis;
                }

            out.writeInt(0x0DDF00DA);                         // packet type - TCMP NS packet
            out.writeUTF(sCluster == null ? "" : sCluster);   // target cluster name

            out.flush();
            int ofAttempt = outBytes.size();

            out.writeByte(0);                                // attempt count
            out.writeByte(cAttempts);                        // attempt limit
            if (abAddrThis == null)
                {
                // let routing table choose the IP, NS will then respond to the packet's src address
                out.writeByte(0);
                out.writeInt(0);
                }
            else
                {
                out.writeByte(abAddrThis.length);
                out.write(abAddrThis);
                out.writeInt(socket.getLocalPort());
                socket.setInterface(addrLocal);
                }
            out.writeUTF(sName);

            if (abMemberClient == null)
                {
                out.writeInt(0);
                }
            else
                {
                out.writeInt(abMemberClient.length);
                out.write(abMemberClient);
                }

            out.flush();

            byte[]         abReq     = outBytes.toByteArray();
            DatagramPacket packetReq = new DatagramPacket(abReq, 0, abReq.length);
            packetReq.setSocketAddress(addrGroup);

            socket.setSoTimeout(cDelayMillis);
            socket.setTimeToLive(nTTL);

            byte[]         abResp = new byte[65535];
            DatagramPacket packetResp = new DatagramPacket(abResp, 0, abResp.length);

            // send the request until response or timeout
            abReq[ofAttempt]++; // requests are never 0
            socket.send(packetReq);
            do
                {
                try
                    {
                    if (Thread.interrupted())
                        {
                        throw new InterruptedIOException();
                        }

                    socket.receive(packetResp);

                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(abResp));

                    if (in.readInt() == 0x0DDF00DA)
                        {
                        String sClusterResult = in.readUTF();
                        if (sCluster == null || sCluster.equals(sClusterResult))
                            {
                            in.read(); // attempt count
                            in.read(); // attempt limit

                            int    cbAddr = in.readByte();
                            byte[] abAddr = new byte[cbAddr];
                            in.readFully(abAddr);
                            in.readInt(); // port

                            if (sName.equals(in.readUTF()) && setCluster.add(sClusterResult))
                                {
                                in.mark(4);
                                int cbResult = in.readInt();
                                in.reset();

                                if (cbResult == -1)
                                    {
                                    // result is too large to ensure delivery over UDP, do TCP lookup
                                    InetAddress addrTcp = InetAddress.getByAddress(abAddr);
                                    try (Connection conn = Connection.open(sClusterResult,
                                               new InetSocketAddress(addrTcp, addrGroup.getPort()), cTimeoutMillis))
                                        {
                                        consumerResult.accept(sClusterResult, conn.lookupRaw(sName));
                                        }
                                    }
                                else
                                    {
                                    consumerResult.accept(sClusterResult, in);
                                    }

                                if (sCluster != null)
                                    {
                                    return;
                                    }
                                }
                            }
                        }
                    }
                catch (SocketTimeoutException e)
                    {
                    --cAttempts;
                    abReq[ofAttempt]++;

                    if (abReq[ofAttempt] == 0)
                        {
                        abReq[ofAttempt] = 1; // requests are never zero
                        }

                    socket.send(packetReq);
                    }
                }
            while (cAttempts > 0);
            }

        if (setCluster.isEmpty())
            {
            throw new IOException(sCluster == null
                    ? "no cluster could be located"
                    : "cluster '" + sCluster + "' could not be located");
            }
        }

    /**
     * Lookup a name via UDP (generally multicast).
     *
     * @param sCluster        the cluster of address
     * @param sName           the name to lookup
     * @param addrGroup       the cluster address
     * @param addrLocal       the NIC to use
     * @param cTimeoutMillis  the discovery timeout
     * @param nTTL            the multicast TTL
     * @param abMemberClient  optional serialized Member object representing the client
     */
    public static DataInputStream datagramLookupRaw(String sCluster, String sName, InetSocketAddress addrGroup,
            InetAddress addrLocal, int cTimeoutMillis, int nTTL, byte[] abMemberClient)
            throws IOException
        {
        final AtomicReference<DataInputStream> refResult = new AtomicReference<>();
        datagramLookupRaw(sCluster, sName, addrGroup, addrLocal, cTimeoutMillis, nTTL, abMemberClient,
                new BiConsumer<String, DataInputStream>()
                    {
                    public void accept(String t, DataInputStream in) 
                        {
                        refResult.set(in);
                        }
                    }
                    );
        return refResult.get();
        }

    /**
     * Lookup a name via UDP (generally multicast) and consume the result as a string.
     *
     * @param sCluster        the cluster of address
     * @param sName           the name to lookup
     * @param addrGroup       the cluster address
     * @param addrLocal       the NIC to use
     * @param cTimeoutMillis  the discovery timeout
     * @param nTTL            the multicast TTL
     * @param abMemberClient  optional serialized Member object representing the client
     * @param consumerResult  the consumer for results
     */
    public static void datagramLookup(String sCluster, String sName, InetSocketAddress addrGroup,
            InetAddress addrLocal, int cTimeoutMillis, int nTTL, byte[] abMemberClient, final BiConsumer<String, String> consumerResult)
        throws IOException
        {
        datagramLookupRaw(sCluster, sName, addrGroup, addrLocal, cTimeoutMillis, nTTL, abMemberClient,
                new BiConsumer<String, DataInputStream>()
                    {
                    public void accept(String sCluster1, DataInputStream in) 
                        {
                        consumerResult.accept(sCluster1, readString(in));
                        }
                    }
                    );
        }

    /**
     * Read a String from a stream
     *
     * @param in a DataInputStream containing the result
     *
     * @return the String
     */
    public static String readString(DataInputStream in)
        {
        try
            {
            int cbResult = in.readInt();
            if (cbResult == 0)
                {
                return null;
                }
            else
                {
                in.readShort(); // pof header we know it can only be a string;

                byte[] abResult = new byte[readPackedInt(in)];

                in.readFully(abResult);
                return new String(abResult);
                }
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Parse and validate the command-line parameters and do the lookup.
     *
     * @param asArg  an array of command line parameters
     *
     * @throws IOException if an I/O error occurs while doing the lookup
     */
    public static void main(String[] asArg)
            throws IOException
        {
        Map    mapArgs;
        String sHost;
        String sLocal;
        String sCluster;
        String sName;
        int    nPort;
        int    nTTL;
        int    cTimeoutMillis;

        try
            {
            mapArgs  = parseArguments(asArg, VALID_COMMANDS, true /*case sensitive*/);
            sHost    = (String) mapArgs.remove(COMMAND_HOST); // host default expands below to all local addresses
            sLocal   = (String) mapArgs.remove(COMMAND_LOCAL);
            sCluster = (String) mapArgs.remove(COMMAND_CLUSTER);
            sName = (String) mapArgs.remove(COMMAND_NAME);
            if (sName == null)
                {
                sName = DEFAULT_NAME;
                }

            if (sHost == null)
                {
                sHost = DEFAULT_HOST;
                }

            String sPort = (String) mapArgs.remove(COMMAND_PORT);
            nPort        = sPort == null ? DEFAULT_CLUSTERPORT : Integer.parseInt(sPort);

            String sTTL = (String) mapArgs.remove(COMMAND_TTL);
            nTTL        = sTTL == null ? DEFAULT_TTL : Integer.parseInt(sTTL);

            String sTimeoutSec = (String) mapArgs.remove(COMMAND_TIMEOUT);
            cTimeoutMillis     = sTimeoutSec == null ? DEFAULT_TIMEOUT : Integer.parseInt(sTimeoutSec) * 1000;

            if (!mapArgs.isEmpty())
                {
                showInstructions();
                System.exit(1);
                }
            }
        catch (Throwable e)
            {
            System.err.println(e);
            System.err.println();
            showInstructions();
            System.exit(1);
            return;
            }

        IOException   ex = null;
        Set<InetAddress> setAddr = new HashSet<>();

        if (sHost.equals("localhost"))
            {
            for (Enumeration<NetworkInterface> iterNics = NetworkInterface.getNetworkInterfaces(); iterNics.hasMoreElements(); )
                {
                for (Enumeration<InetAddress> iterAddr = iterNics.nextElement().getInetAddresses(); iterAddr.hasMoreElements(); )
                    {
                    setAddr.add(iterAddr.nextElement());
                    }
                }
            }
        else
            {
            Collections.addAll(setAddr, InetAddress.getAllByName(sHost));
            }

        for (InetAddress address : setAddr)
            {
            try
                {
                InetSocketAddress socketAddr = new InetSocketAddress(address, nPort);
                if (address.isMulticastAddress())
                    {
                    final String sClusterSearch = sCluster;
                    datagramLookup(sClusterSearch, NS_STRING_PREFIX + sName, socketAddr,
                            sLocal == null ? null : InetAddress.getByName(sLocal), cTimeoutMillis, nTTL, null, 
                                    new BiConsumer<String, String>()
                                        {
                                        public void accept(String sClusterFound, String sResult) 
                                            {
                                            System.out.println(sClusterSearch == null ? ("Cluster " + sClusterFound + ":\t" + sResult) : sResult);
                                            } 
                                        }
                                        );
                    }
                else
                    {
                    try (Connection conn = Connection.open(sCluster, socketAddr, cTimeoutMillis))
                        {
                        if (sCluster == null)
                            {
                            StringTokenizer sTok = new StringTokenizer(
                                    conn.lookup("Cluster/name") + "," +
                                    conn.lookup(NS_STRING_PREFIX + "Cluster/foreign"), "[,]");

                            while (sTok.hasMoreElements())
                                {
                                sCluster = sTok.nextToken().trim();
                                try (Connection conn2 = Connection.open(sCluster, socketAddr, cTimeoutMillis))
                                    {
                                    System.out.println("Cluster " + sCluster + ":\t" + conn2.lookup(NS_STRING_PREFIX + sName));
                                    }
                                }
                            }
                        else
                            {
                            System.out.println(conn.lookup(NS_STRING_PREFIX + sName));
                            }
                        }
                    ex = null;
                    break;
                    }
                }
            catch (IOException ioe)
                {
                ex = ioe;
                }
            }

        if (ex != null)
            {
            String sMsg = ex.getMessage();
            System.err.println("Error: " + (sMsg == null ? ex.getClass().getSimpleName() : sMsg) + "; while querying " + sHost + ":" + nPort + " for " + sName);
            System.exit(1);
            }
        }

    /**
     * Display the command-line instructions.
     */
    protected static void showInstructions()
        {
        String sClass =  NSLookup.class.getCanonicalName();
        System.out.println();
        System.out.println("java " + sClass + " <commands ...>");
        System.out.println();
        System.out.println("command options:");
        System.out.println("\t-" + COMMAND_HOST + "    the cluster address (unicast or multicast); default " + DEFAULT_HOST);
        System.out.println("\t-" + COMMAND_LOCAL + "   the local IP to issue the request on when using multicast");
        System.out.println("\t-" + COMMAND_TTL + "     the TTL for multicast; default " + DEFAULT_TTL);
        System.out.println("\t-" + COMMAND_CLUSTER + " the cluster; optional");
        System.out.println("\t-" + COMMAND_NAME + "    the name to lookup from the NameService; default " + DEFAULT_NAME);
        System.out.println("\t-" + COMMAND_PORT + "    the cluster port; default "  + DEFAULT_CLUSTERPORT);
        System.out.println("\t-" + COMMAND_TIMEOUT + " the timeout (in seconds) of the lookup request; default " + (DEFAULT_TIMEOUT/1000));
        System.out.println();
        System.out.println("Example:");
        System.out.println("\tjava " + sClass + " -" + COMMAND_HOST + " host.mycompany.com -" + COMMAND_NAME + " " + JMX_CONNECTOR_URL);
        System.out.println();
        }

    // ----- static data members -------------------------------------------

    public static final String COMMAND_HELP    = "?";
    public static final String COMMAND_HOST    = "host";
    public static final String COMMAND_LOCAL   = "local";
    public static final String COMMAND_TTL     = "ttl";
    public static final String COMMAND_PORT    = "port";
    public static final String COMMAND_TIMEOUT = "timeout";
    public static final String COMMAND_CLUSTER = "cluster";
    public static final String COMMAND_NAME    = "name";

    public static final String[] VALID_COMMANDS =
        {
        COMMAND_HELP,
        COMMAND_HOST,
        COMMAND_LOCAL,
        COMMAND_TTL,
        COMMAND_CLUSTER,
        COMMAND_PORT,
        COMMAND_TIMEOUT,
        COMMAND_NAME
        };

    /**
     * Prefix for NS lookups which ensure result will be in string format
     */
    public static final String NS_STRING_PREFIX = "NameService/string/";

    /**
     * Management Node JMX Connector URL lookup name.
     */
    public static final String JMX_CONNECTOR_URL = "management/JMXServiceURL";

    /**
     * Management over HTTP URL lookup name.
     *
     * @since 12.2.1.3.2
     */
    public static final String HTTP_MANAGEMENT_URL = "management/HTTPManagementURL";

    /**
     * HTTP Metrics URL lookup name.
     *
     * @since 12.2.1.4.0
     */
    public static final String HTTP_METRICS_URL = "metrics/HTTPMetricsURL";

    /**
     * Default timeout in milliseconds
     */
    public static final int DEFAULT_TIMEOUT = 5000;

    /**
     * Default cluster port
     */
    public static final int DEFAULT_CLUSTERPORT = 7574;

    /**
     * Default host.
     */
    public static final String DEFAULT_HOST = "239.192.0.0";

    /**
     * Default TTL.
     */
    public static final int DEFAULT_TTL = 4;

    /**
     * Default name.
     */
    public static final String DEFAULT_NAME = "Cluster/info";

    /**
     * Multiplexed Socket ID. See com.oracle.coherence.common.internal.net.ProtocolIdentifiers.
     */
    private static final int MULTIPLEXED_SOCKET = 0x05AC1E000;

    /**
     * NameService Sub port. See com.oracle.coherence.common.internal.net.MultiplexedSocketProvider.WellKnownSubPorts.
     */
    private static final int NAMESERVICE_SUBPORT = 3;

    /**
     * End of request marker. This marks the end of complex value. See WritingPofHandler.endComplexValue
     */
    private static final byte REQ_END_MARKER = 64; // This is -1 in packed int format.

    /**
     * byte[] for open connection request. Sniffed from
     * Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.TcpInitiator$TcpConnection.send(WriteBuffer).
     */
    private static final byte[] CONN_OPEN = new byte[]
        {
        0, 1, 2, 0, 66, 0, 1, 14, 0, 0, 66, -90, -74, -97, -34, -78, 81,
        1, 65, -29, -13, -28, -35, 15, 2, 65, -113, -10, -70, -103, 1, 3,
        65, -8, -76, -27, -14, 4, 4, 65, -60, -2, -36, -11, 5, 5, 65, -41,
        -50, -61, -115, 7, 6, 65, -37, -119, -36, -43, 10, 64, 2, 110, 3,
        93, 78, 87, 2, 17, 77, 101, 115, 115, 97, 103, 105, 110, 103, 80,
        114, 111, 116, 111, 99, 111, 108, 2, 65, 2, 65, 2, 19, 78, 97, 109,
        101, 83, 101, 114, 118, 105, 99, 101, 80, 114, 111, 116, 111, 99,
        111, 108, 2, 65, 1, 65, 1, 5, -96, 2, 0, 0, 14, 0, 0, 66, -82, -119,
        -98, -34, -78, 81, 1, 65, -127, -128, -128, -16, 15, 5, 65, -104, -97,
        -127, -128, 8, 6, 65, -109, -98, 1, 64, 1, 106, 2, 110, 3, 106, 4, 113,
        5, 113, 6, 78, 8, 67, 108, 117, 115, 116, 101, 114, 66, 9, 78, 9, 108,
        111, 99, 97, 108, 104, 111, 115, 116, 10, 78, 5, 50, 48, 50, 51, 51, 12,
        78, 16, 67, 111, 104, 101, 114, 101, 110, 99, 101, 67, 111, 110, 115,
        111, 108, 101, 64, 64
        };

    /**
     * byte[] for open channel request. Sniffed from
     * Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.TcpInitiator$TcpConnection.send(WriteBuffer).
     */
    private static final byte[] CHANNEL_OPEN = new byte[]
        {
        0, 11, 2, 0, 66, 1, 1, 78, 19, 78, 97, 109, 101, 83, 101, 114, 118,
        105, 99, 101, 80, 114, 111, 116, 111, 99, 111, 108, 2, 78, 11, 78,
        97, 109, 101, 83, 101, 114, 118, 105, 99, 101, 64
        };

    /**
     * byte[] for lookup request id. Sniffed from
     * Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.TcpInitiator$TcpConnection.send(WriteBuffer).
     */
    private static final byte[] NS_LOOKUP_REQ_ID = new byte[]
        {
        1, 1, 0, 66, 0, 1, 78
        };
    
    // ----- inner classes -------------------------------------------

    /**
     * This utility must support running on JDK 7 platform. Hence using our own 
     * BiConsumer API rather than the JDK 8 API.
     */
    public interface BiConsumer<T, U> 
        {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         */
        void accept(T t, U u);
        }
    }
