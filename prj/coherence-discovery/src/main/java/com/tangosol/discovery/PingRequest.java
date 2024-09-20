/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.discovery;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import java.util.zip.CRC32;

/**
 * Make a ping request to a Coherence proxy server.
 * <p>
 * command options:
 * <ul>
 *   <li>host     the host of the proxy server to send the ping request</li>
 *   <li>port     the listen port of the proxy server</li>
 *   <li>name     the proxy service name</li>
 *   <li>cluster  the cluster name</li>
 *   <li>timeout  (optional) the timeout (in seconds) of the ping request, default 30s</li>
 *   <li>list     (optional) return a list of the address:port of the members in the proxy service, seperated by ','</li>
 * </ul>
 * <br>
 * Example:
 *   java com.tangosol.discovery.PingRequest -host proxyhost.mycompany.com -port 9000 -list
 * <br>
 * If the <i>list</i> option is specified, the program returns a hash value
 * and a list of the IP socket addresses of the members of the proxy service
 * in the Coherence cluster.  For example:
 *
 * Addresses-hash: 1974466772
 * Address-list: [192.110.1.12:9910, 192:110:1:22:9990]
 * Coherence proxy service ping succeeded
 *
 * Otherwise, the program returns the following message, indicating the proxy
 * server is healthy:
 *
 * Coherence proxy service ping succeeded
 *
 * If the ping request fails, it returns error.  For example:
 *
 * An exception occurred while executing the PingRequest:
 * java.net.ConnectException: Connection refused
 * Coherence proxy service ping failed
 *
 * If the ping request is sent to an older version of Coherence proxy server
 * that does not support the returning of a list of IP socket addresses,
 * the ping request is successful, but there is no list returned:
 *
 * Coherence proxy service ping succeeded
 *
 * @author lh  2014.04.17
 *
 * @since  Coherence 12.2.1
 */
public class PingRequest
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Construct the PingRequest.
     *
     * @param sHost     host of the proxy server
     * @param nPort     listen port of the proxy service
     * @param sName     the proxy service name
     * @param sCluster  the cluster name
     * @param cTimeout  ping timeout, in seconds
     * @param fList     whether to return a list of the address:port of the proxy servers in the proxy service.
     */
    public PingRequest(String sHost, int nPort, String sName, String sCluster, int cTimeout, boolean fList)
        {
        m_sHost          = sHost;
        m_nPort          = nPort;
        m_sName          = sName;
        m_sCluster       = sCluster;
        m_cTimeoutMillis = cTimeout*1000;
        m_fList          = fList;
        }

    // ----- static methods ----------------------------------------------------

    /**
     * Issue a proxy server ping request.
     *
     * @param asArg  command line parameter array
     */
    public static void main(String[] asArg)
        {
        Map     mapArgs;
        String  sHost    = null;
        int     nPort    = 0;
        String  sName    = null;
        String  sCluster = null;
        int     nTimeout = 30;
        boolean fList    = false;

        try
            {
            mapArgs = NSLookup.parseArguments(asArg, VALID_COMMANDS,
                    true /*case sensitive*/);
            if (mapArgs.isEmpty())
                {
                missingArguments();
                }

            if (mapArgs.containsKey(COMMAND_HELP)
                    || mapArgs.get(Integer.valueOf(0)) != null)
                {
                missingArguments();
                }

            sHost = (String) mapArgs.get(COMMAND_HOST);
            String sPort = (String) mapArgs.get(COMMAND_PORT);
            if (sPort != null)
                {
                nPort = Integer.parseInt((String) mapArgs.get(COMMAND_PORT));
                }
            sName = (String) mapArgs.get(COMMAND_NAME);
            if (sName != null)
                {
                if (nPort == 0)
                    {
                    nPort = 7574;
                    }
                sCluster = (String) mapArgs.get(COMMAND_CLUSTER);
                }
            else
                {
                if (nPort == 0)
                    {
                    missingArguments();
                    }
                fList = mapArgs.containsKey(COMMAND_LIST);
                }

            String sTimeout = (String) mapArgs.get(COMMAND_TIMEOUT);
            nTimeout = sTimeout == null ? 30 : Integer.parseInt(sTimeout);
            }
        catch (Throwable e)
            {
            System.err.println();
            System.err.println(e);
            System.err.println();
            showInstructions();
            System.exit(1);
            }

        try
            {
            PingRequest request = new PingRequest(sHost, nPort, sName, sCluster, nTimeout, fList);
            boolean     result  = sName == null ? request.ping() : request.nsLookup();

            if (result)
                {
                System.out.println(PING_SUCCEEDED);
                }
            else
                {
                System.out.println(PING_FAILED);
                }
            }
        catch (Exception e)
            {
            System.err.println("An exception occurred while executing the PingRequest:");
            System.err.println(e);
            System.out.println(PING_FAILED);
            System.err.println();
            System.exit(1);
            }
        }

    /**
     * Report error and display the command-line instructions.
     */
    protected static void missingArguments()
        {
        System.out.println();
        System.out.println("Missing arguments.");
        System.out.println("Please either specify proxy server host and port, or proxy service name with optional cluster name, address, or port.");
        showInstructions();
        System.exit(1);
        }

    /**
     * Display the command-line instructions.
     */
    protected static void showInstructions()
        {
         String sClass = PingRequest.class.getCanonicalName();
         System.out.println();
         System.out.println("java " + sClass + " <commands ...>");
         System.out.println();
         System.out.println("command options:");
         System.out.println("\t-" + COMMAND_HOST    + "     (optional when -" + COMMAND_NAME + " is specified; required otherwise) the host of the proxy service or cluster to send the ping request");
         System.out.println("\t-" + COMMAND_PORT    + "     (optional when -" + COMMAND_NAME + " is specified; required otherwise) the listen port of the proxy service or the cluster port");
         System.out.println("\t-" + COMMAND_NAME    + "     (optional when -" + COMMAND_HOST + " and -" + COMMAND_PORT + " are specified; required otherwise) the proxy service name.  When using -" + COMMAND_NAME + ", the host is the cluster address");
         System.out.println("\t-" + COMMAND_CLUSTER + "  (optional) the cluster; used only with -" + COMMAND_NAME);
         System.out.println("\t-" + COMMAND_TIMEOUT + "  (optional) the timeout (in seconds) of the ping request, default " + DEFAULT_TIMEOUT + "s");
         System.out.println("\t-" + COMMAND_LIST    + "     (optional) returns a list of the address:port of the members in the proxy service, separated by \',\'");
         System.out.println();
         System.out.println("Example: obtain a list of proxy service listen addresses by pinging a proxy service directly...");
         System.out.println("\tjava " + sClass + " -host proxyhost.mycompany.com -port 9000 -list");
         System.out.println();
         System.out.println("Example: obtain a list of proxy service listen addresses for a proxy service by name...");
         System.out.println("\tjava " + sClass + " -name myproxyservice -cluster mycluster");
         System.out.println();
        }

    // ----- PingRequest methods --------------------------------------------

    /**
     * Run the PingRequest.
     *
     * @throws IOException if an I/O error occurs while reading from the socket stream
     *
     * @return true if the ping is successful; false otherwise.
     */
    public boolean ping()
        throws IOException
        {
        IOException   ex             = null;
        InetAddress[] aHostAddresses = InetAddress.getAllByName(m_sHost);
        for (InetAddress address : aHostAddresses)
            {
            Socket clientSocket = null;
            try
                {
                clientSocket = new Socket();
                clientSocket.setTcpNoDelay(true);
                clientSocket.connect(new InetSocketAddress(address, m_nPort), m_cTimeoutMillis);

                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
                if (this.m_fList)
                    {
                    outToServer.write(QUERY_PING);
                    }
                else
                    {
                    outToServer.write(BASIC_PING);
                    }
                outToServer.flush();

                byte[] resultReturned = NSLookup.read(inFromServer);
                int len = resultReturned.length;
                if (!m_fList)
                    {
                    if (Arrays.equals(BASIC_RESULT, resultReturned)
                            || Arrays.equals(BASIC_RESULT_NO_LEN, resultReturned)
                            || Arrays.equals(OLDER_VERSION_NO_LEN, resultReturned))
                        {
                        return true;
                        }
                    return false;
                    }
                if (len == 10)
                    {
                    if (Arrays.equals(OLDER_VERSION_RESULT, resultReturned))
                        {
                        return true;
                        }
                    return false;
                    }
                int i;
                for (i = 0; i < len; i++)
                    {
                    if (i > 6 && resultReturned[i] == 0x55)
                        {
                        break;
                        }
                    }
                DataInputStream resultStream = new DataInputStream(
                        new ByteArrayInputStream(resultReturned, i, len - i));
                Collection aAddresses = readCollection(resultStream);
                System.out.println("Addresses-hash: " + makeCheckSumHash(resultReturned));
                System.out.println("Address-list: " + aAddresses);
                return true;
                }
            catch (IOException ioe)
                {
                ex = ioe;
                }
            finally
                {
                if (clientSocket != null)
                    {
                    clientSocket.close();
                    }
                }
            }

        if (ex != null)
            {
            throw ex;
            }

        return false;
        }

    /**
     * Given a byte array, return its checksum hash.
     *
     * @param input  input byte array to make checksum from
     *
     * @return the checksum hash of the input data
     */
    static public long makeCheckSumHash(byte[] input)
        {
        CRC32 checksum = new CRC32();
        checksum.reset();
        checksum.update(input);
        return checksum.getValue();
        }

    /**
     * Read a POF Collection of strings.
     *
     * @param inputStream  socket input stream
     *
     * @throws IOException if an I/O error occurs while reading from the socket stream
     *
     * @return Collection read
     */
    static public Collection readCollection(DataInputStream inputStream)
            throws IOException
        {
        inputStream.skipBytes(1);                          // POF Collection type
        int size = NSLookup.readPackedInt(inputStream);    // collection size
        if (size < 0)
            {
            throw new IOException("Received a message with a negative array length");
            }

        Collection result = new ArrayList(size);
        int i = 0;
        while (i < size)
            {
            inputStream.skipBytes(1);                         // POF String type
            int len = NSLookup.readPackedInt(inputStream);    // String length
            byte[] ab = new byte[len];
            inputStream.readFully(ab, 0, len);
            result.add(new String(ab));
            i++;
            }
        return result;
        }

    public boolean nsLookup()
        throws IOException
        {
        InetAddress[] aHostAddresses = InetAddress.getAllByName(m_sHost);

        for (InetAddress address : aHostAddresses)
            {
            InetSocketAddress socketAddr = new InetSocketAddress(address, m_nPort);
            try (Connection conn = Connection.open(m_sCluster, socketAddr, m_cTimeoutMillis))
                {
                DataInputStream resultStream = conn.lookupRaw(NSLookup.NS_STRING_PREFIX + m_sName + "/addresses");
                byte[] resultReturned = new byte[256];
                int len = resultStream.read(resultReturned);
                resultStream.reset();
                if (resultStream != null)
                    {
                    System.out.println("Addresses-hash: " + makeCheckSumHash(resultReturned));
                    System.out.println("Address-list: " + NSLookup.readString(resultStream));
                    return true;
                    }
                }
            }

        return false;
        }

    // ---- constants -------------------------------------------------------

    public static final String COMMAND_HELP    = "?";
    public static final String COMMAND_HOST    = "host";
    public static final String COMMAND_PORT    = "port";
    public static final String COMMAND_NAME    = "name";
    public static final String COMMAND_CLUSTER = "cluster";
    public static final String COMMAND_TIMEOUT = "timeout";
    public static final String COMMAND_LIST    = "list";
    public static final int    DEFAULT_TIMEOUT = 30;

    public static final String PING_SUCCEEDED  = "Coherence proxy service ping succeeded";
    public static final String PING_FAILED     = "Coherence proxy service ping failed";

    public static final String[] VALID_COMMANDS =
        {
        COMMAND_HELP,
        COMMAND_HOST,
        COMMAND_PORT,
        COMMAND_NAME,
        COMMAND_CLUSTER,
        COMMAND_TIMEOUT,
        COMMAND_LIST
        };

    private static final byte[] BASIC_PING           = new byte[] {7, 0, 3, 0, 0, 66, 0, 64};
    private static final byte[] BASIC_RESULT         = new byte[] {9, 0, 4, 3, 0, 66, 0, 3, 100, 64};
    private static final byte[] BASIC_RESULT_NO_LEN  = new byte[] {0, 4, 3, 0, 66, 0, 3, 100, 64};
    private static final byte[] OLDER_VERSION_RESULT = new byte[] {9, 0, 4, 2, 0, 66, 1, 3, 100, 64};
    private static final byte[] OLDER_VERSION_NO_LEN = new byte[] {0, 4, 2, 0, 66, 0, 3, 100, 64};
    private static final byte[] QUERY_PING           = new byte[] {7, 0, 3, 0, 0, 66, 1, 64};

    // ----- data members ---------------------------------------------------

    private String  m_sHost;
    private int     m_nPort;
    private String  m_sName;
    private String  m_sCluster;
    private int     m_cTimeoutMillis;
    private boolean m_fList;
    }
