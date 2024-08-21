/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import java.util.Arrays;


/**
 * Provides Connection functionality to Coherence cluster service endpoints.
 * <p>
 * Note: This class is for internal use solely by other Coherence classes.
 */
class Connection
        implements Closeable
    {
    // ----- constructors ----------------------------------------------------

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
        NSLookup.write(outStream, CONN_OPEN);    // write open connection request
        NSLookup.write(outStream, CHANNEL_OPEN); // write open channel request
        outStream.flush();              // send open requests

        NSLookup.read(inStream);                        // wait for and skip over response to connect request
        byte[] aChanResponse = NSLookup.read(inStream); // wait for open channel response

        // extract the channel id from the response, this appears to start at offset 8, but not including
        // the last byte, the chan id is variable length, so we can't just walk back from the tail.
        // the header is also technically variable length as it contains packed ints, but unlike the channel id they
        // are constants and thus appear to be non-variable for this specific request.  This is only true
        // because we limit ourselves to a single request per connection.
        // TODO: properly parse the header
        int cbChanId = aChanResponse.length - 9;
        abChan = Arrays.copyOfRange(aChanResponse, 8, 8 + cbChanId);
        }

    // ----- Connection methods ----------------------------------------------

    public static Connection open(String sCluster, SocketAddress socketAddr, int cTimeOutMillis)
            throws IOException
        {
        Connection connection = null;
        do
            {
            connection = new Connection(socketAddr, cTimeOutMillis);
            if (sCluster != null && !sCluster.equals(connection.lookup("Cluster/name")))
                {
                String sPort = connection.lookup(NSLookup.NS_STRING_PREFIX + "Cluster/foreign/" + sCluster + "/NameService/localPort");
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
        return stream == null ? null : NSLookup.readString(stream);
        }

    public DataInputStream lookupRaw(String sName)
            throws IOException
        {
        // build the lookup request.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream reqStream = new DataOutputStream(baos);

        reqStream.write(abChan);

        // write request id
        reqStream.write(NS_LOOKUP_REQ_ID, 0, NS_LOOKUP_REQ_ID.length);

        byte[] abName = sName.getBytes("utf-8");

        // write length of the lookup string
        NSLookup.writePackedInt(reqStream, abName.length);

        // write lookup string
        reqStream.write(abName, 0, abName.length);

        // write terminating byte
        reqStream.write(REQ_END_MARKER);
        reqStream.flush();

        // write lookup request
        NSLookup.write(outStream, baos.toByteArray());
        outStream.flush(); // send request

        // wait for the request response
        byte[] aResponse = NSLookup.read(inStream);
        int nLen = aResponse.length;
        int nMinLen = abChan.length + 1;

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

    // ----- Closable nethods ------------------------------------------------

    @Override
    public void close()
            throws IOException
        {
        socket.close();
        }

    // ----- constants -------------------------------------------------------

    /**
     * Multiplexed Socket ID. See com.oracle.coherence.common.internal.net.ProtocolIdentifiers.
     */
    private static final int MULTIPLEXED_SOCKET = 0x05AC1E000;
    /**
     * NameService Sub port. See com.oracle.coherence.common.internal.net.MultiplexedSocketProvider.WellKnownSubPorts.
     */
    private static final int NAMESERVICE_SUBPORT = 3;

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

    /**
     * End of request marker. This marks the end of complex value. See WritingPofHandler.endComplexValue
     */
    private static final byte REQ_END_MARKER = 64; // This is -1 in packed int format.

    // ----- data members ----------------------------------------------------
    final Socket socket;

    final DataOutputStream outStream;

    final DataInputStream inStream;

    final byte[] abChan;
    }
