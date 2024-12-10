/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.run.xml.SimpleDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import static com.tangosol.util.Base.ensureRuntimeException;
import static com.tangosol.util.Base.err;
import static com.tangosol.util.Base.formatDateTime;
import static com.tangosol.util.Base.makeThread;
import static com.tangosol.util.Base.out;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;

import java.lang.reflect.Method;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSocketFactory;


/**
* Test for datagram communication.
*
* @author cp, mf 2005.10.24
*/
public class DatagramTest
    {
    /**
    * Parse and validate the command-line parameters and run the test.
    *
    * @param asArg  an array of command line parameters
    *
    * @see #showInstructions
    */
    public static void main(String[] asArg)
            throws Exception
        {
        // extract switches from the command line
        List lArg      = new ArrayList(Arrays.asList(asArg));
        List lSwitches = extractSwitches(lArg, VALID_SWITCHES);
        asArg = (String[]) lArg.toArray(new String[lArg.size()]);

        if (lSwitches.contains(SWITCH_HELP))
            {
            showInstructions();
            return;
            }

        // parse remainder of command line options
        Map mapCmd = CommandLineTool.parseArguments(asArg, VALID_COMMANDS,
                        true /*case sensitive*/);

        List<InetSocketAddress> listBind = parseAddresses((String) processCommand(mapCmd,
                COMMAND_ADDR_LOCAL, DEFAULT_ADDR_LOCAL));
        for (InetSocketAddress addrLocal : listBind)
            {
            StartFlag         startFlag = null;
            PublisherConfig   pConfig   = null;
            ListenerConfig    lConfig   = new ListenerConfig();
            int               cTimeoutMs;

            DatagramSocketProvider datagramSocketProvider;
            XmlElement xml;
            try
                {
                String sProvider = (String) processCommand(mapCmd, COMMAND_PROVIDER,
                                        DEFAULT_PROVIDER);
                xml = new SimpleDocument("socket-provider");
                if (sProvider.equals("ssl"))
                    {
                    // configure to allow for non-standard anonymous authentication
                    // by allowing all supported ciphers
                    XmlElement xmlCiphers = xml.addElement("ssl").addElement("cipher-suites");
                    String[]   asCiphers  = ((SSLSocketFactory) SSLSocketFactory.getDefault()).getSupportedCipherSuites();

                    for (String s : asCiphers)
                        {
                        xmlCiphers.addElement("name").setString(s);
                        }
                    }
                else if (sProvider.startsWith("file:"))
                    {
                    xml = XmlHelper.loadXml(new URL(sProvider));
                    }
                else
                    {
                    xml.addElement(sProvider);
                    }

                int nMTU = InetAddressHelper.getLocalMTU(addrLocal.getAddress());
                if (nMTU == 0)
                    {
                    nMTU = 1500;
                    }
                datagramSocketProvider = new SocketProviderFactory().getDatagramSocketProvider(xml, 1);

                // determine header sizes; since a cluster can be ipv4 and ipv6 we always choose ipv6 safe values.
                // Same as Cluster.configureSockets
                int cbPacket = processIntCommand(mapCmd, COMMAND_PACKET_SIZE,
                        nMTU - (datagramSocketProvider instanceof SystemDatagramSocketProvider ? 48 : 68));

                lConfig.setPacketSize(cbPacket);
                lConfig.setPayload(processIntCommand(mapCmd, COMMAND_PAYLOAD, DEFAULT_PAYLOAD));
                lConfig.setProcessPacketBytes(processIntCommand(mapCmd, COMMAND_PROCESS_BYTES, DEFAULT_PROCESS_BYTES));
                lConfig.setReportInterval(processIntCommand(mapCmd, COMMAND_REPORT_INTERVAL, DEFAULT_REPORT_INTERVAL));
                lConfig.setTickInterval(processIntCommand(mapCmd, COMMAND_TICK_INTERVAL, DEFAULT_TICK_INTERVAL));
                lConfig.setBufferPackets(processIntCommand(mapCmd, COMMAND_RX_PACKET_BUFFER_SIZE, DEFAULT_RX_PACKET_BUFFER_SIZE));
                String sLog = (String) processCommand(mapCmd, COMMAND_LOG, DEFAULT_LOG);
                if (sLog != null)
                    {
                    if (listBind.size() > 1)
                        {
                        lConfig.setLog(addrLocal.getPort() + "-" + sLog);
                        }
                    else
                        {
                        lConfig.setLog(sLog);
                        }
                    }
                lConfig.setLogInterval(processIntCommand(mapCmd, COMMAND_LOG_INTERVAL, DEFAULT_LOG_INTERVAL));
                cTimeoutMs = processIntCommand(mapCmd, COMMAND_RX_TIMEOUT_MS, DEFAULT_RX_TIMEOUT_MS);
                // process args, if specified this contains a list of peers to send to
                List<InetSocketAddress> lAddrPeer = null;

                for (int i = 0; ;++i)
                    {
                    String sAddrValue = (String) mapCmd.get(Integer.valueOf(i));

                    if (sAddrValue == null)
                        {
                        break;
                        }

                    if (lAddrPeer == null)
                        {
                        // lazy allocate peer list
                        lAddrPeer = new ArrayList();
                        }

                    lAddrPeer.addAll(parseAddresses(sAddrValue));
                    }

                Map<InetSocketAddress, AtomicLong> mapAcks = new HashMap<InetSocketAddress, AtomicLong>();
                lConfig.setAckMap(mapAcks);
                if (lAddrPeer != null)
                    {
                    for (InetSocketAddress addr : lAddrPeer)
                        {
                        mapAcks.put(addr, new AtomicLong(-1));
                        }
                    }

                // process any tx related settings if peers have been specified
                if (lAddrPeer != null)
                    {
                    pConfig = new PublisherConfig();

                    pConfig.setAckMap(mapAcks);
                    pConfig.setReturnPort(addrLocal.getPort());
                    pConfig.setAddrPeers(lAddrPeer.
                            toArray(new InetSocketAddress[lAddrPeer.size()]));
                    pConfig.setPacketSize(cbPacket);
                    pConfig.setPayload(processIntCommand(mapCmd, COMMAND_PAYLOAD, DEFAULT_PAYLOAD));
                    pConfig.setProcessPacketBytes(processIntCommand(mapCmd, COMMAND_PROCESS_BYTES, DEFAULT_PROCESS_BYTES));
                    pConfig.setReportInterval(processIntCommand(mapCmd, COMMAND_REPORT_INTERVAL, DEFAULT_REPORT_INTERVAL));
                    pConfig.setTickInterval(processIntCommand(mapCmd, COMMAND_TICK_INTERVAL, DEFAULT_TICK_INTERVAL));
                    pConfig.setBufferPackets(processIntCommand(mapCmd, COMMAND_TX_PACKET_BUFFER_SIZE, DEFAULT_TX_PACKET_BUFFER_SIZE));
                    int cmbs = processIntCommand(mapCmd, COMMAND_TX_RATE, DEFAULT_TX_RATE);
                    pConfig.setRate(cmbs <= 0 ? 0 : Math.max(1, cmbs / listBind.size()));
                    pConfig.setIterationLimit(processIntCommand(mapCmd, COMMAND_TX_ITERATIONS, DEFAULT_TX_ITERATIONS));
                    pConfig.setDurationLimitMs(processLongCommand(mapCmd, COMMAND_TX_DURATION_MS, DEFAULT_TX_DURATION_MS));

                    if (lSwitches.contains(SWITCH_POLITE))
                        {
                        startFlag = new StartFlag();
                        }
                    }

                if (lSwitches.contains(SWITCH_RAND))
                    {
                    lConfig.setPayload(-lConfig.getPayload());
                    pConfig.setPayload(-pConfig.getPayload());
                    }

                if (mapCmd.isEmpty())
                    {
                    showInstructions();
                    out();
                    out("running with all default values...");
                    out();
                    }
                }
            catch (Throwable e)
                {
                err();
                err(e);
                err();
                showInstructions();
                return;
                }

            // validate supplied values
            if (!checkUnicast(addrLocal)   ||
                !lConfig.check() ||
                (pConfig != null && !pConfig.check())
                )
            {
            showInstructions();
            return;
            }

            // start the test
            try
                {
                out("creating datagram socket using provider: " + datagramSocketProvider);
                final DatagramSocket socket = datagramSocketProvider.openDatagramSocket();
                if (socket instanceof TcpDatagramSocket)
                    {
                    ((TcpDatagramSocket) socket).setPacketMagic(MAGIC, MAGIC_MASK);
                    }

                String sOptions = (String) processCommand(mapCmd, COMMAND_OPTIONS,
                                        DEFAULT_OPTIONS);
                SocketOptions options = sOptions == null
                        ? null : SocketOptions.load(XmlHelper.loadXml(new URL(sOptions)));
                if (options != null)
                    {
                    out("using socket options: " + options);
                    SocketOptions.apply(options, socket);
                    }

                socket.bind(addrLocal);
                if (cTimeoutMs != 0)
                    {
                    socket.setSoTimeout(cTimeoutMs);
                    }

                // force an newline upon termination to clean up shell
                Runtime.getRuntime().addShutdownHook(new Thread()
                    {
                    public void run()
                        {
                        socket.close();
                        System.out.println();
                        }
                    });

                // always start listener
                DatagramListener listener    = new DatagramListener(socket, startFlag, lConfig);
                Thread           thListener  = makeThread(null, listener,  "TestListener:" + addrLocal);
                thListener.setDaemon(pConfig != null);
                thListener.start();

                // optionally start a publisher
                if (pConfig != null)
                    {
                    DatagramSocket sockSend;
                    if (s_fSplitSocket)
                        {
                        sockSend = datagramSocketProvider.openDatagramSocket();
                        if (sockSend instanceof TcpDatagramSocket)
                            {
                            ((TcpDatagramSocket) sockSend).setPacketMagic(MAGIC, MAGIC_MASK);
                            }
                        SocketOptions.apply(options, socket);

                        sockSend.bind(new InetSocketAddress(addrLocal.getAddress(), 0)); // ephemeral
                        }
                    else
                        {
                        sockSend = socket;
                        }
                    DatagramPublisher publisher = new DatagramPublisher(
                            sockSend, startFlag, pConfig);

                    // start publisher threads
                    Thread thPublisher = makeThread(null, publisher, "TestPublisher:" + addrLocal);
                    long cDurationLimitMs = pConfig.getDurationLimitMs();
                    thPublisher.setDaemon(cDurationLimitMs > 0);
                    thPublisher.start();

                    // wait for the publisher to finish
                    if(cDurationLimitMs > 0)
                        {
                        // if the publisher is polite, then don't start the
                        // timer untill the publisher gets to start.
                        if (startFlag != null)
                            {
                            startFlag.waitForGo();
                            }
                        thPublisher.join(cDurationLimitMs);
                        }
                    }
                }
            catch (Exception e)
                {
                err('[' + formatDateTime(System.currentTimeMillis()) +
                        "] An exception occurred while executing the DatagramTest:");
                err(e);
                }
            }
        }


    /**
     * Parse a string containing a space separated list of inet addresses.
     * <p>
     * This method supports range based addresses the range can be specified
     * using <tt>..port</tt>, allowing the specification of a range such as:
     * <tt>http://localhost:80..89</tt> to result in ten addresses.
     *
     * @param sAddrs   the addresses string
     *
     * @return a List of addresses
     */
    public static List<InetSocketAddress> parseAddresses(String sAddrs)
            throws UnknownHostException
        {
        List<InetSocketAddress> listAddr = new ArrayList<InetSocketAddress>();
        for (StringTokenizer tok = new StringTokenizer(sAddrs); tok.hasMoreElements(); )
            {
            String sTok = tok.nextToken();
            int    of   = sTok.indexOf("..");

            if (of == -1)
                {
                listAddr.add(InetAddressHelper.getSocketAddress(sTok, DEFAULT_PORT));
                }
            else // range based address
                {
                String sName    = sTok.substring(0, of);
                int    ofPort   = Math.max(sName.lastIndexOf('.'), sName.lastIndexOf(':'));
                String sPrefix  = sName.substring(0, ofPort + 1);
                int    nPort    = Integer.parseInt(sName.substring(ofPort + 1));
                int    nPortEnd = Integer.parseInt(sTok.substring(of + 2));

                if (nPort < nPortEnd)
                    {
                    for (; nPort <= nPortEnd; ++nPort)
                        {
                        listAddr.add(InetAddressHelper.getSocketAddress(sPrefix + nPort, DEFAULT_PORT));
                        }
                    }
                else
                    {
                    for (; nPort >= nPortEnd; --nPort)
                        {
                        listAddr.add(InetAddressHelper.getSocketAddress(sPrefix + nPort, DEFAULT_PORT));
                        }
                    }
                }
            }
        return listAddr;
        }

    public static long nanoTime()
        {
        Method methodNano = s_methodNano;

        if (methodNano == null)
            {
            return (System.currentTimeMillis() - s_ldtStart) * 1000000L;
            }

        try
            {
            return ((Long) methodNano.invoke(null)).longValue();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Search the supplied argument set for known switches, and extract them.
    * Each switch which is found will be placed in the returned List and
    * removed from the argument collection.
    *
    * @param colArg         argument collection to parse, and remove switch from
    * @param asValidSwitch  switches to look for
    *
    * @return list of found switches
    */
    protected static List extractSwitches(Collection colArg,
                                          String[] asValidSwitch)
        {
        List lResult = new LinkedList();
        for (int i = 0, c = asValidSwitch.length; i < c; ++i)
            {
            String sSwitch = "-" + asValidSwitch[i];
            for (Iterator iter = colArg.iterator(); iter.hasNext(); )
                {
                String sArg = (String) iter.next();
                if (sArg.equals(sSwitch))
                    {
                    lResult.add(asValidSwitch[i]);
                    iter.remove();
                    }
                }
            }
        return lResult;
        }

    /**
    * Process a command from the command line arguments.
    * This method is used to process required commands, and will throw
    * an exception if the command was not specified.
    *
    * @param mapCommands the map of command line arguments
    * @param sName       the name of the command to process
    *
    * @return the value
    *
    * @throws UnsupportedOperationException if a no value is available
    */
    protected static Object processCommand(Map mapCommands, String sName)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(sName);
        if (value == null)
            {
            throw new UnsupportedOperationException("-" + sName + " must be specified.");
            }
        return value;
        }

   /**
   * Process a command from the command line arguments.
   * This method is used to process optional commands, and the
   * default will be returned if command was not explicitly specified.
   *
   * @param mapCommands the map of command line arguments
   * @param sName       the name of the command to process
   * @param oDefault    Specifies a default value
   *
   * @return the value, or <tt>oDefault</tt> if unspecified
   */
    protected static Object processCommand(Map mapCommands, String sName, Object oDefault)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(sName);
        return (value == null ? oDefault : value);
        }

    /**
    * Process an optional command from the command line arguments, where the
    * value is to be interpreted as an integer.
    *
    * @param mapCommands the map of command line arguments
    * @param sName       the name of the argument to process
    * @param iDefault    Specifies an default value
    *
    * @return the value, or <tt>iDefault</tt> if unspecified
    */
    protected static int processIntCommand(Map mapCommands, String sName, int iDefault)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(sName);
        return (value == null ? iDefault : Integer.parseInt((String) value));
        }

    /**
    * Process a required command from the command line arguments, where the
    * value is to be interpreted as an integer.
    *
    * @param mapCommands the map of command line arguments
    * @param sName       the name of the argument to process
    *
    * @return the value
    * @throws UnsupportedOperationException if a no value is available
    */
    protected static int processIntCommand(Map mapCommands, String sName)
            throws UnsupportedOperationException
        {
        Object value = processCommand(mapCommands, sName);
        return Integer.parseInt((String) value);
        }

    /**
    * Process an optional command from the command line arguments, where the
    * value is to be interpreted as an long.
    *
    * @param mapCommands the map of command line arguments
    * @param sName       the name of the argument to process
    * @param lDefault    Specifies an default value
    *
    * @return the value, or <tt>iDefault</tt> if unspecified
    */
    protected static long processLongCommand(Map mapCommands, String sName, long lDefault)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(sName);
        return (value == null ? lDefault : Long.parseLong((String) value));
        }

    /**
    * Check that the value for processPacketBytes is acceptable.
    * To be acceptable it must be a positive multiple of 4, and less then the
    * packet size.
    *
    * @param cbPacket the packet size
    * @param cProcessPacketBytes the number of bytes to process from each packet
    *
    * @return true if the value is acceptable, false otherwise
    */
    public static boolean checkProcessPacketBytes(int cbPacket, int cProcessPacketBytes)
        {
        if ((cProcessPacketBytes < 20) ||
                ((cProcessPacketBytes % 4) != 0) ||
                (cProcessPacketBytes > cbPacket))
            {
            err("processPacketBytes must be between 20 and the packet size, in multiples of 4.");
            return false;
            }
        return true;
        }

    /**
    * Check if the address is a unicast address.
    *
    * @param addr the address to verify
    *
    * @return true if the value is unicast, false otherwise
    */
    private static boolean checkUnicast(InetSocketAddress addr)
        {
        InetAddress iAddr = addr.getAddress();
        if (iAddr != null && iAddr.isMulticastAddress())
            {
            err("Interface address " + addr + " is multi-cast; it must be an IP address bound to a physical interface");
            return false;
            }
        return true;
        }

    /**
    * Check that each address is a unicast address.
    *
    * @param aAddr an array of addresses to check
    *
    * @return true if the values are unicast, false otherwise
    */
    private static boolean checkUnicast(InetSocketAddress[] aAddr)
        {
        for (int i = 0, c = aAddr.length; i < c ; ++i)
            {
            if (!checkUnicast(aAddr[i]))
                {
                return false;
                }
            }
        return true;
        }

    /**
     * compute throughput as megabytes per second.
     *
     * @param cBytes       the number of bytes transferred
     * @param lDurationMs  the time in milliseconds the transfer took
     *
     * @return throughput  a human readable string for the throughput expressed
     *                      in megabytes per second
     */
    public static String computeThroughputMBPerSec(long cBytes, long lDurationMs)
        {
        if(lDurationMs == 0)
            {
            return "NaN";
            }

        float flMBs = (float) cBytes / (float) MB;
        int iThpt = Math.round((flMBs / lDurationMs) * 1000F);
        return Integer.toString(iThpt) + " MB/sec";
        }

    /**
     * compute throughput as packets per second.
     *
     * @param cPackets     the number of packets transferred
     * @param lDurationMs  the time in milliseconds the transfer took
     *
     * @return throughput  a human readable string for the throughput expressed
     *                      in packets per second
     */
    public static String computeThroughputPacketsPerSec(long cPackets, long lDurationMs)
        {
        if(lDurationMs == 0)
            {
            return "NaN";
            }

        int iThpt = Math.round(((float) cPackets / (float) lDurationMs) * 1000F);
        return Integer.toString(iThpt) + " packets/sec";
        }



    /**
    * Display the command-line instructions.
    */
    protected static void showInstructions()
        {
        out();
        out("java com.tangosol.net.DatagramTest <-local addr:port> [commands ...] [addr:port ...]");
        out();
        out("command option descriptions:");
        out("\t-local          (optional) the local address to bind to, specified as addr:port[..port], default " + DEFAULT_ADDR_LOCAL);
        out("\t-packetSize     (optional) the size of packet to work with, specified in bytes, default based on local MTU and provider");
        out("\t-payload        (optional) the amount of data to include in each packet, 0 to match packet size, default " + DEFAULT_PAYLOAD);
        out("\t-processBytes   (optional) the number of bytes (in multiples of 4) of each packet to process, default " + DEFAULT_PROCESS_BYTES);
        out("\t-rxBufferSize   (optional) the size of the receive buffer, specified in packets, default " + DEFAULT_RX_PACKET_BUFFER_SIZE);
        out("\t-rxTimeoutMs    (optional) the duration of inactivity before a connection is closed, default " + DEFAULT_RX_TIMEOUT_MS);
        out("\t-txBufferSize   (optional) the size of the transmit buffer, specified in packets, default " + DEFAULT_TX_PACKET_BUFFER_SIZE);
        out("\t-txRate         (optional) the rate at which to transmit data, specified in megabytes, default unlimited");
        out("\t-txIterations   (optional) specifies the number of packets to publish before exiting, default unlimited");
        out("\t-txDurationMs   (optional) specifies how long to publish before exiting, default unlimited");
        out("\t-reportInterval (optional) the interval at which to output a report, specified in packets, default " + DEFAULT_REPORT_INTERVAL);
        out("\t-tickInterval   (optional) the interval at which to output tick marks, default " + DEFAULT_TICK_INTERVAL);
        out("\t-log            (optional) the name of a file to save a tabular report of measured performance, default none");
        out("\t-logInterval    (optional) the interval at which to output a measurement to the log, default " + DEFAULT_LOG_INTERVAL);
        out("\t-polite         (optional) switch indicating if the publisher should wait for the listener to be contacted before publishing.");
        out("\t-provider       (optional) the socket provider to use (system, tcp, ssl, file:xxx.xml), default " + DEFAULT_PROVIDER);
        out("\targuments       (optional) space separated list of addresses to publish to, specified as addr:port[..port]");
        out();
        out("examples:");
        out("java com.tangosol.net.DatagramTest -local box1:9999 -polite box2:9999");
        out("java com.tangosol.net.DatagramTest -local box2:9999 box1:9000..9004");
        }

    // constants
    public static final int MB         = (1024 * 1024);

    // command line options
    public static final String  COMMAND_ADDR_LOCAL            = "local";
    public static final int     DEFAULT_PORT                  = 9999;
    public static final String  DEFAULT_IP_LOCAL              = "localhost";
    public static final String  DEFAULT_ADDR_LOCAL            = DEFAULT_IP_LOCAL + ":" + DEFAULT_PORT;
    public static final String  COMMAND_PACKET_SIZE           = "packetSize";
    public static final int     DEFAULT_PACKET_SIZE           = 1468; // 1448 for TCP
    public static final String  COMMAND_PAYLOAD               = "payload";
    public static final int     DEFAULT_PAYLOAD               = 0; // match packetsize
    public static final String  COMMAND_TX_RATE               = "txRate";
    public static final int     DEFAULT_TX_RATE               = -1; // unlimited
    public static final String  COMMAND_PROCESS_BYTES         = "processBytes";
    public static final int     DEFAULT_PROCESS_BYTES         = 20; // header only
    public static final String  COMMAND_TX_PACKET_BUFFER_SIZE = "txBufferSize";
    public static final int     DEFAULT_TX_PACKET_BUFFER_SIZE = 32;
    public static final String  COMMAND_RX_PACKET_BUFFER_SIZE = "rxBufferSize";
    public static final int     DEFAULT_RX_PACKET_BUFFER_SIZE = 1428;
    public static final String  COMMAND_LOG                   = "log";
    public static final String  DEFAULT_LOG                   = null;
    public static final String  COMMAND_REPORT_INTERVAL       = "reportInterval";
    public static final int     DEFAULT_REPORT_INTERVAL       = 100000;
    public static final String  COMMAND_LOG_INTERVAL          = "logInterval";
    public static final int     DEFAULT_LOG_INTERVAL          = 100000;
    public static final String  COMMAND_TICK_INTERVAL         = "tickInterval";
    public static final int     DEFAULT_TICK_INTERVAL         = 1000;
    public static final String  COMMAND_TX_ITERATIONS         = "txIterations";
    public static final int     DEFAULT_TX_ITERATIONS         = -1; // unlimited
    public static final String  COMMAND_TX_DURATION_MS        = "txDurationMs";
    public static final long    DEFAULT_TX_DURATION_MS        = -1; // unlimited
    public static final String  COMMAND_RX_TIMEOUT_MS         = "rxTimeoutMs";
    public static final int     DEFAULT_RX_TIMEOUT_MS         = 1000;
    public static final String  COMMAND_PROVIDER              = "provider";
    public static final String  DEFAULT_PROVIDER              = "system";
    public static final String  COMMAND_OPTIONS               = "options";
    public static final String  DEFAULT_OPTIONS               = null;

    public static final String[] VALID_COMMANDS = {
            COMMAND_ADDR_LOCAL,
            COMMAND_PACKET_SIZE,
            COMMAND_PAYLOAD,
            COMMAND_TX_RATE,
            COMMAND_PROCESS_BYTES,
            COMMAND_TX_PACKET_BUFFER_SIZE,
            COMMAND_RX_PACKET_BUFFER_SIZE,
            COMMAND_REPORT_INTERVAL,
            COMMAND_LOG,
            COMMAND_LOG_INTERVAL,
            COMMAND_TICK_INTERVAL,
            COMMAND_TX_ITERATIONS,
            COMMAND_TX_DURATION_MS,
            COMMAND_RX_TIMEOUT_MS,
            COMMAND_PROVIDER,
            COMMAND_OPTIONS
    };

    // switches are command options without values
    public static final String SWITCH_HELP   = "?";
    public static final String SWITCH_POLITE = "polite";
    public static final String SWITCH_RAND   = "rand";

    public static final String[] VALID_SWITCHES = {
            SWITCH_HELP,
            SWITCH_POLITE,
            SWITCH_RAND
    };



    // ----- inner class: TestConfig ----------------------------------------

    /**
     * Holder for Test configuration.
     */
    public static class TestConfiguration
        {
        /**
        * Return a human readable description of the configuration.
        *
        * @return a human readable description of the configuration
        */
        public String toString()
            {
            return new StringBuffer()
                    .append("packet size: ").append(m_cbPacket).append(" bytes")
                    .append('\n')
                    .append("buffer size: ").append(m_cBufferPackets).append(" packets")
                    .append('\n')
                    .append("  report on: ").append(m_cReportInterval).append(" packets, ")
                    .append((m_cReportInterval * (long) m_cbPacket) / MB).append(" MBs")
                    .append('\n')
                    .append("    process: ").append(m_cProcessPacketBytes).append(" bytes/packet")
                    .toString();
            }

        /**
        * Check that the contained configuration is valid.
        * @return true if valid false otherwise
        */
        public boolean check()
            {
            return checkProcessPacketBytes(m_cbPacket, m_cProcessPacketBytes);
            }

        /**
        * Accessor for ReportInterval.
        */
        public int getReportInterval()
            {
            return m_cReportInterval;
            }

        /**
         * Mutator for ReportInterval.
         */
        public void setReportInterval(int cReportInterval)
            {
            m_cReportInterval = cReportInterval;
            }

        /**
        * Accessor for TickInterval.
        */
        public int getTickInterval()
            {
            return m_cTickInterval;
            }

        /**
         * Mutator for TickInterval.
         */
        public void setTickInterval(int cTickInterval)
            {
            m_cTickInterval = cTickInterval;
            }

        /**
        * Accessor for PacketSize.
        */
        public int getPacketSize()
            {
            return m_cbPacket;
            }

        /**
         * Mutator for PacketSize.
         */
        public void setPacketSize(int cbPacket)
            {
            m_cbPacket = cbPacket;
            }

        /**
        * Accessor for Payload.
        */
        public int getPayload()
            {
            return m_cbPayload;
            }

        /**
         * Mutator for Payload.
         */
        public void setPayload(int cbPayload)
            {
            if (cbPayload == 0)
                {
                cbPayload = m_cbPacket;
                }
            else if (cbPayload > m_cbPacket)
                {
                throw new IllegalArgumentException("Payload cannot exceed packet size.");
                }
            m_cbPayload = cbPayload;
            }

        /**
        * Accessor for  ProcessPacketBytes.
        */
        public int getProcessPacketBytes()
            {
            return m_cProcessPacketBytes;
            }

        /**
         * Mutator for ProcessPacketBytes.
         */
        public void setProcessPacketBytes(int cProcessPacketBytes)
            {
            m_cProcessPacketBytes = cProcessPacketBytes;
            }

        /**
        * Accessor for BufferPackets.
        */
        public int getBufferPackets()
            {
            return m_cBufferPackets;
            }

        /**
         * Mutator for BufferPackets
         */
        public void setBufferPackets(int cBufferPackets)
            {
            m_cBufferPackets = cBufferPackets;
            }

        public void setAckMap(Map<InetSocketAddress, AtomicLong> mapAcks)
            {
            m_mapAcks = mapAcks;
            }

        // ----- data members -------------------------------------------

        protected int m_cReportInterval;
        protected int m_cTickInterval;
        protected int m_cbPacket;
        protected int m_cbPayload;
        protected int m_cProcessPacketBytes;
        protected int m_cBufferPackets;
        protected Map<InetSocketAddress, AtomicLong> m_mapAcks;
        }


    // ----- inner class: PublisherConfig -----------------------------------

    /**
     * Holder for Publisher configuration.
     */
    public static class PublisherConfig
           extends TestConfiguration
        {
        /**
        * Return a human readable description of the configuration.
        *
        * @return a human readable description of the configuration
        */
        public String toString()
            {
            return new StringBuffer(super.toString())
                    .append('\n')
                    .append("      peers: ").append(m_aAddrPeer.length)
                    .append('\n')
                    .append("       rate: ")
                    .append((m_cRate > 0 ? Integer.toString(m_cRate) : "no limit"))
                    .toString();
            }

        /**
        * Check that the contained configuration is valid.
        * @return true if valid false otherwise
        */
        public boolean check()
            {
            return super.check() & checkUnicast(m_aAddrPeer);
            }

        public int getReturnPort()
            {
            return m_nPortReturn;
            }

        public void setReturnPort(int nPort)
            {
            m_nPortReturn = nPort;
            }

        /**
        * Accessor for AddrPeers.
        */
        public InetSocketAddress[] getAddrPeers()
            {
            return m_aAddrPeer;
            }

        /**
        * Mutator for AddrPeers.
        */
        public void setAddrPeers(InetSocketAddress[] aAddrPeer)
            {
            m_aAddrPeer = aAddrPeer;
            }


        public Map<InetSocketAddress, AtomicLong> getAckMap()
            {
            return m_mapAcks;
            }

        public void setAckMap(Map<InetSocketAddress, AtomicLong> mapAcks)
            {
            m_mapAcks = mapAcks;
            }

        /**
        * Accessor for rate
        */
        public int getRate()
            {
            return m_cRate;
            }

        /**
        * Mutator for rate
        */
        public void setRate(int cRate)
            {
            m_cRate = cRate;
            }

        /**
        * Accessor for Iterations
        */
        public int getIterationLimit()
            {
            return m_cIterationLimit;
            }

        /**
        * Mutator for Iterations
        */
        public void setIterationLimit(int cIterations)
            {
            m_cIterationLimit = cIterations;
            }

        /**
        * Accessor for DurationLimitMs
        */
        public long getDurationLimitMs()
            {
            return m_cDurationLimitMs;
            }

        /**
        * Mutator for DurationLimitMs
        */
        public void setDurationLimitMs(long cDurationMs)
            {
            m_cDurationLimitMs = cDurationMs;
            }

        // ----- data members ------------------------------------------

        protected int                 m_nPortReturn;
        protected InetSocketAddress[] m_aAddrPeer;
        protected Map<InetSocketAddress, AtomicLong> m_mapAcks;
        protected int                 m_cRate;
        protected int                 m_cIterationLimit;
        protected long                m_cDurationLimitMs;
        }

    // ----- inner class: ListenerConfig ------------------------------------

    /**
     * Holder for Listener configuration.
     */
    public static class ListenerConfig
           extends TestConfiguration
       {
        /**
        * Return a human readable description of the configuration.
        *
        * @return a human readable description of the configuration
        */
        public String toString()
            {
            return new StringBuffer(super.toString())
                    .append('\n')
                    .append("        log: ").append(m_sLog)
                    .append('\n')
                    .append("     log on: ")
                    .append((m_cLogInterval * (long) m_cbPacket) / MB).append(" MBs")
                    .toString();
            }

        /**
        * Accessor for log.
        */
        public String getLog()
            {
            return m_sLog;
            }

        /**
        * Mutator for log.
        */
        public void setLog(String sLog)
            {
            m_sLog = sLog;
            }

        /**
        * Accessor for LogInterval.
        */
        public int getLogInterval()
            {
            return m_cLogInterval;
            }

        /**
        * Mutator for LogInterval.
        */
        public void setLogInterval(int cLogInterval)
            {
            m_cLogInterval = cLogInterval;
            }

        protected String m_sLog;
        protected int    m_cLogInterval;
        }


    // ----- inner class: DatagramPublisher ---------------------------------

    /**
    * The publisher test.
    */
    public static class DatagramPublisher
            implements Runnable
        {
        /**
        * Construct the publisher test object.
        *
        * @param socket     socket to use
        * @param startFlag  a flag which will delay the start of a publisher
        * @param config     structure holding publisher configuration
        */
        public DatagramPublisher(DatagramSocket socket,
                        StartFlag startFlag, PublisherConfig config)
                throws IOException
            {
            socket.setSendBufferSize(config.getPacketSize() * config.getBufferPackets());

            m_config    = config;
            m_socket    = socket;
            m_startFlag = startFlag;
            }

        /**
        * Write a long into a byte array
        * @param aBytes  the byte array to write into
        * @param lValue  the long to write
        * @param i       the index to write at
        */
        public static void writeLong(byte[] aBytes, long lValue, int i)
            {
            int n = (int) (lValue >>> 32);
            aBytes[i++] = (byte)(n >>> 24);
            aBytes[i++] = (byte)(n >>> 16);
            aBytes[i++] = (byte)(n >>>  8);
            aBytes[i++] = (byte)(n);

            // lo word
            n = (int) lValue;
            aBytes[i++] = (byte)(n >>> 24);
            aBytes[i++] = (byte)(n >>> 16);
            aBytes[i++] = (byte)(n >>>  8);
            aBytes[i++] = (byte)(n);
            }

        /**
        * Run the test.
        */
        public void run()
            {
            InetSocketAddress[] aAddrPeer           = m_config.getAddrPeers();
            AtomicLong[]        aAtomicAcks         = new AtomicLong[aAddrPeer.length];
            int                 cPeer               = aAddrPeer.length;
            int                 cbPacket            = m_config.getPacketSize();
            int                 cbPayload           = m_config.getPayload();
            int                 cReportInterval     = m_config.getReportInterval();
            int                 cProcessPacketBytes = m_config.getProcessPacketBytes();
            int                 cRate               = m_config.getRate();
            StartFlag           startFlag           = m_startFlag;
            DatagramSocket      socket              = m_socket;
            int                 nReturnPort         = m_config.getReturnPort();

            StringBuffer sbAddrs = new StringBuffer();
            for (int i = 0; i < cPeer; ++i )
                {
                if (i > 0)
                    {
                    sbAddrs.append(", ");
                    }
                sbAddrs.append(aAddrPeer[i].toString());
                aAtomicAcks[i] = m_config.getAckMap().get(aAddrPeer[i]);
                }

            out("starting publisher: at " + socket.getLocalSocketAddress() + " sending to " + sbAddrs);
            out(m_config);
            out();

            // calculate how often I need to take a break to maintain my rate
            int cRateBytes = cRate * MB;
            int cAvgPacket = cbPayload < 0 ? cbPacket + (cbPayload / 2) : cbPayload;
            int iPktsPerSecond = Math.round((float) cRateBytes / (float) cAvgPacket);
            int iBurstPackets =  iPktsPerSecond / 100; // assumes 10ms delay

            if (cRateBytes > 0)
                {
                out("setting packet burst to " + iBurstPackets);
                }
            else
                {
                out("no packet burst limit");
                }

            byte[]                     aBytes       = new byte[cbPacket];
            DatagramPacket             packet       = new DatagramPacket(aBytes, 0, cbPacket);
            DatagramPacketOutputStream streamPacket = new DatagramPacketOutputStream(packet);

            try (DataOutputStream stream = new DataOutputStream(streamPacket))
                {
                long   cTxPackets           = 0L;
                long   cTxBytes             = 0L;
                long   cTickInterval        = m_config.getTickInterval();
                long   cBigTickInterval     = cTickInterval * 10;
                long   lStart               = System.currentTimeMillis();
                long   lLastRateCheckTime   = lStart;
                long   lLastReportTime      = lStart;
                long   cThisReportTxBytes   = 0L;
                long   cThisReportTxPackets = 0L;
                int    cIterationLimit      = m_config.getIterationLimit();
                Random random               = new Random();

                // fill packet with random data
                int cInts = cbPacket / 4;
                for (int i = 0; i < cInts; ++i)
                    {
                    stream.writeInt(i);
                    }
                stream.flush();
                streamPacket.reset();

                for (int iIter = 1; ; ++iIter)
                    {
                    if (startFlag != null && startFlag.isStopped())
                        {
                        out("waiting for listener to be contacted before publishing");
                        try
                            {
                            m_startFlag.waitForGo();
                            }
                        catch (InterruptedException e)
                            {
                            err("Interrupted while waiting to start publishing");
                            return;
                            }
                        }

                    if (cIterationLimit > 0 && (iIter % cIterationLimit) == 0)
                        {
                        out("iteration limit reached");
                        return;
                        }

                    // fill in part of the packet
                    stream.writeInt(MAGIC);
                    stream.writeInt(nReturnPort);

                    cInts = cProcessPacketBytes / 4;
                    for (int i = 0; i < cInts; ++i)
                        {
                        stream.writeInt(iIter);
                        }
                    stream.flush();

                    int nBytes;
                    if (cbPayload < 0)
                        {
                        // random size packets
                        nBytes = cbPacket - random.nextInt(-cbPayload);
                        packet.setLength(nBytes);
                        }
                    else
                        {
                        nBytes = cbPayload;
                        // pad up to full packet size
                        packet.setLength(cbPacket);
                        }

                    // send the packet to each peer
                    for (int i = 0 ; i < cPeer ; ++i)
                        {
                        // address packet
                        packet.setAddress(aAddrPeer[i].getAddress());
                        packet.setPort(aAddrPeer[i].getPort());

                        // encode the current send time
                        writeLong(aBytes, nanoTime(), 8);

                        // encode peer specific ack
                        // reset value to -1 so that it will not be retransmitted
                        writeLong(aBytes, aAtomicAcks[i].getAndSet(-1), 16);

                        socket.send(packet);
                        ++cTxPackets;
                        ++cThisReportTxPackets;

                        cTxBytes += nBytes;
                        cThisReportTxBytes += nBytes;

                        // periodically output ticks
                        if (cTickInterval != 0 && (cTxPackets % cTickInterval) == 0)
                            {
                            System.out.print((cTxPackets % cBigTickInterval) == 0 ? 'O' : 'o'); System.out.flush();
                            }

                        if (cReportInterval != 0 && (cTxPackets % cReportInterval) == 0)
                            {
                            long lNow = System.currentTimeMillis();
                            long lDuration  = lNow - lStart;

                            long lLastDuration  = lNow - lLastReportTime;

                            StringBuffer sbReport = new StringBuffer();
                            sbReport.append("\nTx summary for ").append(m_socket.getLocalAddress()).append(":")
                                    .append(m_config.getReturnPort()).append(" to ").append(cPeer).append(" peers:")
                                    .append("\n   life: ")
                                    .append(computeThroughputMBPerSec(cTxBytes, lDuration))
                                    .append(", ")
                                    .append(computeThroughputPacketsPerSec(cTxPackets, lDuration))
                                    .append("\n    now: ")
                                    .append(computeThroughputMBPerSec(cThisReportTxBytes, lLastDuration))
                                    .append(", ")
                                    .append(computeThroughputPacketsPerSec(cThisReportTxPackets, lLastDuration))
                                    .append("\n     duration: ").append(lDuration).append("ms")
                                    .append("\n    completed: ").append(formatDateTime(lNow));

                            if (cRateBytes > 0)
                                {
                                    sbReport.append(", packets/burst: ").append(iBurstPackets)
                                            .append(", bursts/second: ").append((float) iPktsPerSecond / (float) iBurstPackets);
                                }

                            out(sbReport.toString());

                            lLastReportTime      = lNow;
                            cThisReportTxPackets = 0;
                            cThisReportTxBytes   = 0;
                            }

                        // periodically adjust tx rate
                        if (cRateBytes > 0)
                            {
                            if ((cTxPackets % iBurstPackets) == 0)
                                {
                                try
                                    {
                                    Blocking.sleep(1);
                                    // don't send ack for anything which came in while we were sleeping, this
                                    // would inflate the RTT calculation
                                    for (int j = 0, c = aAtomicAcks.length; j < c; ++j)
                                        {
                                        aAtomicAcks[j].set(-1);
                                        }
                                    }
                                catch (InterruptedException x) {}
                                }

                            if ((cTxPackets % iPktsPerSecond) == 0)
                                {
                                // check and see if this amount of work took us
                                // one second, and adjust the delays accordingly
                                long lNow   = System.currentTimeMillis();
                                long lDelta = lNow - lLastRateCheckTime;

                                lLastRateCheckTime = lNow;

                                float flPct = lDelta / 1000F;

                                iBurstPackets = Math.round(iBurstPackets * flPct);
                                }
                            }
                        }
                    streamPacket.reset();
                    }
                }
            catch (Exception e)
                {
                out('[' + formatDateTime(System.currentTimeMillis()) + "] test encountered exception:");
                out(e);
                }
            }


        // ----- data members -------------------------------------------

        protected DatagramSocket      m_socket;
        protected PublisherConfig     m_config;
        protected StartFlag           m_startFlag;
        }


    // ----- inner class: DatagramListener ----------------------------------

    public static class DatagramListener
            implements Runnable
        {
        /**
        * Construct the listener test object.
        *
        * @param socket     socket to use
        * @param startFlag  a flag which will inform a polite publisher
        *                    that it is ok to start
        * @param config     structure holding listener configuration
        */
        public DatagramListener(DatagramSocket socket, StartFlag startFlag,
                                ListenerConfig config)
                throws IOException
            {
            int cbBufferSize = config.m_cbPacket * config.m_cBufferPackets;

            socket.setReceiveBufferSize(cbBufferSize);

            // check if receive size setting was accepted
            // the setting is a "hint" so we want to validate
            // that we are testing with the expected value
            int iRcvSize = socket.getReceiveBufferSize();
            if (iRcvSize < cbBufferSize)
                {
                throw new IllegalArgumentException("Receive buffer size setting was not" +
                        " accepted by the OS, the buffer is only " + iRcvSize +
                        " bytes, or " + (iRcvSize / config.m_cbPacket) + " packets, " +
                        " please increase your OS socket buffer limits or use the " +
                        " -" + COMMAND_RX_PACKET_BUFFER_SIZE +
                        " test parameter to request a smaller buffer.");
                }

            // open a log stream if needed
            String sLog = config.m_sLog;
            if (sLog != null)
                {
                if (sLog.equals("stdout"))
                    {
                    m_pstreamLog = System.out;
                    logHeader();
                    }
                else if (sLog.equals("stderr"))
                    {
                    m_pstreamLog = System.err;
                    logHeader();
                    }
                else
                    {
                    File fLog = new File(sLog);
                    boolean fNewFile = !fLog.exists();
                    m_pstreamLog = new PrintStream(new FileOutputStream(fLog, true /*append*/));
                    if (fNewFile)
                        {
                        logHeader();
                        }
                    }
                }

            m_config    = config;
            m_startFlag = startFlag;
            m_socket    = socket;
            }

        /**
        * Run the test.
        */
        public void run()
            {
            DatagramSocket socket              = m_socket;
            int            cProcessPacketBytes = m_config.m_cProcessPacketBytes;
            int            cReportInterval     = m_config.m_cReportInterval;
            int            cbPacket            = m_config.m_cbPacket;
            int            cbPayload           = m_config.m_cbPayload;
            Map<InetSocketAddress, AtomicLong> mapAcks = m_config.m_mapAcks;
            boolean        fLifetime           = System.getProperty("tangosol.datagramtest.lifetime", "true").equals("true");
            out("starting listener: at " + socket.getLocalSocketAddress());
            out(m_config);
            out();

            try
                {
                int                  cTickInterval    = m_config.m_cTickInterval;
                int                  cBigTickInterval = cTickInterval * 10;
                DatagramPacket       packet           = new DatagramPacket(new byte[cbPacket], 0, cbPacket);
                StartFlag            startFlag        = m_startFlag;
                ByteArrayInputStream bStream          = new ByteArrayInputStream(packet.getData());
                DataInputStream      stream           = new DataInputStream(bStream);
                int                  cRxPackets       = 0;
                Map                  mapLifeTracker   = new HashMap();
                Map                  mapNowTracker    = new HashMap();
                int                  cLogInterval      = m_config.getLogInterval();

                while (true)
                    {
                    try
                        {
                        socket.receive(packet);
                        }
                    catch (InterruptedIOException e)
                        {
                        // all publishers have stopped
                        if (mapLifeTracker.size() > 0)
                            {
                            out("\nClients have stopped.");

                            PacketTracker.generateReport("Lifetime:", mapLifeTracker);
                            log(mapLifeTracker);

                            mapLifeTracker.clear();
                            mapNowTracker.clear();
                            cRxPackets = 0;

                            startFlag = m_startFlag;

                            if (startFlag != null)
                                {
                                startFlag.stop();
                                }
                            }
                        continue;
                        }

                    ++cRxPackets;
                    bStream.reset();

                    if (startFlag != null)
                        {
                        // instruct any polite publisher to start
                        startFlag.go();
                        startFlag = null;
                        }


                    InetSocketAddress addrSender = (InetSocketAddress) packet.getSocketAddress();

                    int nMagic = stream.readInt();
                    if (nMagic != MAGIC)
                        {
                        out("the packet contains a corrupted header: " + nMagic);
                        continue;
                        }
                    int nReturnPort = stream.readInt();

                    PacketTracker lifeTracker = (PacketTracker) mapLifeTracker.get(addrSender);
                    PacketTracker nowTracker  = (PacketTracker) mapNowTracker.get(addrSender);

                    if (lifeTracker == null)
                        {
                        // create tracker for new client
                        InetSocketAddress addrReturn = new InetSocketAddress(addrSender.getAddress(), nReturnPort);
                        InetSocketAddress addrBind   = (InetSocketAddress) socket.getLocalSocketAddress();
                        lifeTracker = new PacketTracker(addrBind, addrReturn, mapAcks.get(addrReturn));
                        nowTracker  = new PacketTracker(addrBind, addrReturn, mapAcks.get(addrReturn));

                        mapLifeTracker.put(addrSender, lifeTracker);
                        mapNowTracker.put(addrSender, nowTracker);

                        out("\n" + addrBind + " receiving data from " + mapLifeTracker.size() + " publisher(s).");
                        }


                    long ldtSentNanos = stream.readLong();
                    long ldtAckNanos  = stream.readLong();

                    long nCurrent = stream.readLong();
                    int  nBytes   = cbPayload < 0 ? packet.getLength() : Math.min(cbPayload, packet.getLength());

                    lifeTracker.trackArrival(nCurrent, ldtSentNanos, ldtAckNanos, nBytes);
                    nowTracker.trackArrival(nCurrent, ldtSentNanos, ldtAckNanos, nBytes);


                    int nProcess = (cProcessPacketBytes < nBytes ? cProcessPacketBytes : nBytes);
                    for (int i = 7/*header length in ints*/, c = nProcess / 4; i < c; ++i)
                        {
                        // check for corrupted packet
                        long n = stream.readLong();
                        if (n != nCurrent)
                            {
                            if (n == 0)
                                {
                                // publisher is not configured to fill in the entire packet
                                if ((cRxPackets % 10000) == 0)
                                    {
                                    // only output this message once in awhile
                                    out("the packet is not full, configure publisher to process the same number of bytes");
                                    }
                                }
                            else
                                {
                                err("corrupted packet from " + addrSender + " at i=" + i + ", n=" + n + ", nCurrent=" + nCurrent);
                                }
                            break; // don't check the remainder of the packet
                            }
                        }

                    if (cTickInterval != 0 && (cRxPackets % cTickInterval) == 0)
                        {
                        System.out.print((cRxPackets % cBigTickInterval) == 0 ? 'I' : 'i'); System.out.flush();
                        }

                    if (cLogInterval != 0 && (cRxPackets % cLogInterval) == 0)
                        {
                        log(mapNowTracker);
                        }

                    if (cReportInterval != 0 && (cRxPackets % cReportInterval) == 0)
                        {
                        if (fLifetime)
                            {
                            PacketTracker.generateReport("Lifetime:", mapLifeTracker);
                            }
                        PacketTracker.generateReport("Now:", mapNowTracker);

                        // reset the "now" trackers
                        for (Iterator iter = mapNowTracker.values().iterator(); iter.hasNext(); )
                            {
                            ((PacketTracker) iter.next()).reset(System.currentTimeMillis());
                            }
                        }

                    }
                }
            catch (Exception e)
                {
                err('[' + formatDateTime(System.currentTimeMillis()) + "] test encountered exception:");
                err(e);
                }
            }

        /**
        * Output a header to the tabular log.
        */
        protected void logHeader()
            {
            m_pstreamLog.println(PacketTracker.getTabularReportHeader());
            }

        /**
        * Log the performance data for trackers to the log.
        *
        * @param mapTracker a map of lifetime trackers keyed on sender address
        */
        protected void log(Map mapTracker)
            {
            for (Iterator iter = mapTracker.values().iterator() ; iter.hasNext() ; )
                {
                log((PacketTracker) iter.next());
                }
            }

        /**
        * Log the performance data from a single tracker to the log.
        *
        * @param tracker the tracker data to append to the log
        */
        protected void log(PacketTracker tracker)
            {
            if (m_pstreamLog != null)
                {
                m_pstreamLog.println(tracker.getTabularReport());
                }
            }


        // ---- data members -------------------------------------------

        protected DatagramSocket m_socket;
        protected StartFlag      m_startFlag;
        protected ListenerConfig m_config;
        protected PrintStream    m_pstreamLog;
        }


    // ----- inner class: PacketTracker ------------------------------------

    /**
    * Tracker of packet statistics for a single client.
    */
    protected static class PacketTracker
        {
        /**
        * Construct a PacketTracker for the given client.
        *
        * @param addrSender the address of the client
        */
        public PacketTracker(InetSocketAddress addrBind, InetSocketAddress addrSender, AtomicLong atomicAckOut)
            {
            m_addrBind      = addrBind;
            m_addrSender    = addrSender;
            m_atomicAckOut  = atomicAckOut;
            reset(System.currentTimeMillis());
            }

        /**
        * Track the arrival of a new packet.
        * @param nCurrent packet number
        * @param cBytes   packet size in bytes
        */
        public void trackArrival(long nCurrent, long ldtSentNanos, long ldtAckNanos, int cBytes)
            {
            long ldtNow = System.currentTimeMillis();

            AtomicLong atomicAckOut = m_atomicAckOut;
            if (atomicAckOut != null)
                {
                // record send time reported by peer to send back in next ack
                atomicAckOut.set(ldtSentNanos);

                // use ack from peer to update our RTT
                if (ldtAckNanos != -1)
                    {
                    m_lDeltaRttNanos += nanoTime() - ldtAckNanos;
                    ++m_cAcksIn;
                    }
                }

            ++m_cPacketsRcvd;

            if (m_cPacketsRcvd == 1)
                {
                // first packet received, setup min and max
                m_nMin = m_nMax = nCurrent;
                }
            else if (nCurrent > m_nMax)
                {
                m_nMax = nCurrent;
                }
            else if (nCurrent < m_nMin)
                {
                m_nMin = nCurrent;
                }

            // TODO: track duplicates

            if (m_cPacketsRcvd > 1)
                {
                // check for out of order packets
                if (nCurrent < m_nNext)
                    {
                    // out of order
                    // only late packets are considered to be out
                    // of order, i.e. (1, 4, 6) is still in order
                    // and (1, 4, 6, 2) has one packet out of order
                    // and two packets missing
                    ++m_cOutOfOrder;
                    m_cTotalOutOfOrderOffset += Math.abs(m_cPacketsRcvd - ((nCurrent - m_nMin) + 1));
                    }
                else if (nCurrent > m_nNext)
                    {
                    // a gap has been created, it may later be
                    // filled in by out of order packets
                    ++m_cGaps;
                    m_cGapPackets += (nCurrent - m_nNext);
                    m_cGapMillis  += (ldtNow - m_lLastPacketArrivalTime);
                    }
                }

            m_lLastPacketArrivalTime = ldtNow;
            m_nNext = m_nMax + 1;
            m_cBytesReceived += cBytes;
            }

        /**
        * Reset the counters.
        *
        * @param lTimeMs the current time
        */
        public void reset(long lTimeMs)
            {
            m_cPacketsRcvd           = 0;
            m_lStartTime             = lTimeMs;
            m_lLastPacketArrivalTime = lTimeMs;
            m_nMin                   = 0;
            m_nMax                   = -1;
            m_nNext                  = 0;
            m_cOutOfOrder            = 0;
            m_cTotalOutOfOrderOffset = 0;
            m_cBytesReceived         = 0;
            m_cGaps                  = 0;
            m_cGapPackets            = 0;
            m_cGapMillis             = 0;
            m_lDeltaRttNanos         = 0;
            m_cAcksIn                = 0;
            }

        /**
        * Return the duration this tracker has been accumulating data for.
        *
        * @return duration in milliseconds
        */
        public long computeDurationMillis()
            {
            return m_lLastPacketArrivalTime - m_lStartTime;
            }


        /**
        * Return the average round trip time of a packet in milliseconds.
        *
        * @return the average packet round trip time
        */
        public long computeRttNanos()
            {
            long cAcksIn = m_cAcksIn;

            return cAcksIn == 0 ? -1 : m_lDeltaRttNanos / cAcksIn;
            }

        /**
        * compute the number of packets sent by the publisher.
        *
        * @return the number of packets sent by the publisher
        */
        public long computeSentPackets()
            {
            return (m_nMax - m_nMin) + 1;
            }

        /**
        * compute the number of missing packets.
        *
        * @return the number of missing packets
        */
         public long computeMissingPackets()
            {
            return computeSentPackets() - m_cPacketsRcvd;
            }

        /**
        * compute the average out of order offset for packets which arrived
        * out of order.
        *
        * @return the average out of order offset
        */
        public long computeAverageOutOfOrderOffset()
            {
            return (m_cOutOfOrder == 0 ? 0 : m_cTotalOutOfOrderOffset / m_cOutOfOrder);
            }

        /**
        * compute the success rate for receiving packets.
        *
        * @return the success rate
        */
        public float computeSuccessRate()
            {
            long cSent = computeSentPackets();
            if (cSent == 0)
                {
                return 0;
                }
            return ((float) m_cPacketsRcvd) / ((float) computeSentPackets());
            }

        /**
        * compute the throughput in megabytes per second.
        *
        * @return return the computed throughput
        */
        public int computeThroughputMBPerSec()
            {
            long lDuration = computeDurationMillis();
            if (lDuration == 0)
                {
                return -1;
                }
            float flMBs = (float) m_cBytesReceived / (float) MB;
            return Math.round((flMBs / lDuration) * 1000F);
            }

        /**
        * compute the throughput in megabytes per second.
        *
        * @return return the computed throughput
        */
        public int computeThroughputPacketsPerSec()
            {
            long lDuration = computeDurationMillis();
            if (lDuration == 0)
                {
                return -1;
                }
            return Math.round(((float) m_cPacketsRcvd / (float) lDuration) * 1000F);
           }


        /**
        * compute average packet size.
        *
        * @return the average packet size received
        */
        public int computeAveragePacketSize()
            {
            if (m_cPacketsRcvd == 0)
                {
                return 0;
                }
            return (int) (m_cBytesReceived / m_cPacketsRcvd);
            }

        /**
        * Return a header for a tabular report.
        *
        * @return a tabular report header
        */
        public static String getTabularReportHeader()
            {
            return new StringBuffer()
                    .append("publisher\t")
                    .append("duration ms\t")
                    .append("packet size\t")
                    .append("throughput mb/sec\t")
                    .append("throughput packets/sec\t")
                    .append("sent packets\t")
                    .append("received packets\t")
                    .append("missing packets\t")
                    .append("success rate\t")
                    .append("out of order\t")
                    .append("avg out of order offset\t")
                    .append("gaps\t")
                    .append("avg gap size\t")
                    .append("avg gap time ms\t")
                    .append("avg ack ms")
                    .toString();
            }

        /**
        * Return a report in tabular form.
        *
        * @return a report in tabular form
        */
        public String getTabularReport()
            {
            return new StringBuffer()
                    .append(m_addrSender).append('\t')
                    .append(computeDurationMillis()).append('\t')
                    .append(computeAveragePacketSize()).append('\t')
                    .append(computeThroughputMBPerSec()).append('\t')
                    .append(computeThroughputPacketsPerSec()).append('\t')
                    .append(computeSentPackets()).append('\t')
                    .append(m_cPacketsRcvd).append('\t')
                    .append(computeMissingPackets()).append('\t')
                    .append(computeSuccessRate()).append('\t')
                    .append(m_cOutOfOrder).append('\t')
                    .append(computeAverageOutOfOrderOffset()).append('\t')
                    .append(m_cGaps).append('\t')
                    .append(m_cGapPackets / Math.max(1, m_cGaps)).append('\t')
                    .append(m_cGapMillis / Math.max(1, m_cGaps)).append('\t')
                    .append(computeRttNanos() / 1000000.0).append('\t')
                    .toString();
            }

        /**
        * Return a human readable report on the tracked data.
        *
        * @return a human readable report
        */
        public String toString()
            {
            long   lDurationMs = computeDurationMillis();
            long   cSent       = computeSentPackets();
            long   cRttNanos   = computeRttNanos();

            return new StringBuffer()
                    .append("Rx from publisher: ").append(m_addrSender)
                    .append("\n\t to listener: ").append(m_addrBind)
                    .append("\n\t     elapsed: ").append(lDurationMs).append("ms")
                    .append("\n\t packet size: ").append(computeAveragePacketSize())
                    .append("\n\t  throughput: ").append(DatagramTest.computeThroughputMBPerSec(m_cBytesReceived, lDurationMs))
                    .append("\n\t              ").append(DatagramTest.computeThroughputPacketsPerSec(m_cPacketsRcvd, lDurationMs))
                    .append("\n\t    received: ").append(m_cPacketsRcvd).append(" of ").append(cSent)
                    .append("\n\t     missing: ").append(computeMissingPackets())
                    .append("\n\tsuccess rate: ").append(computeSuccessRate())
                    .append("\n\tout of order: ").append(m_cOutOfOrder)
                    .append("\n\t  avg offset: ").append(computeAverageOutOfOrderOffset())
                    .append("\n\t        gaps: ").append(m_cGaps)
                    .append("\n\tavg gap size: ").append(m_cGapPackets / Math.max(1, m_cGaps))
                    .append("\n\tavg gap time: ").append(m_cGapMillis / Math.max(1, m_cGaps)).append("ms")
                    .append("\n\tavg ack time: ").append(cRttNanos / 1000000.0).append("ms; acks ").append(m_cAcksIn)
                    .toString();
            }

        /**
        * Return a summay report on multiple trackers.
        *
        * @param aTracker an array of trackers which ran concurrently
        */
        public static String toString(PacketTracker[] aTracker)
            {
            long lStartTime             = Long.MAX_VALUE;
            long lLastTime              = 0L;
            long cBytes                 = 0L;
            int  cOutOfOrder            = 0;
            int  cTotalOutOfOrderOffset = 0;
            long cRcvd                  = 0;
            long cSent                  = 0;
            long cGaps                  = 0;
            int  cGapPackets            = 0;
            int  cGapMillis             = 0;
            long cAcks                  = 0;
            long lDeltaRttNanos         = 0;

            for (int i = 0, c = aTracker.length; i < c; i++)
                {
                PacketTracker tracker = aTracker[i];

                // compute time as max last - min start
                if (tracker.m_lStartTime < lStartTime)
                    {
                    lStartTime = tracker.m_lStartTime;
                    }
                if (tracker.m_lLastPacketArrivalTime > lLastTime)
                    {
                    lLastTime = tracker.m_lLastPacketArrivalTime;
                    }

                cSent                  += ((tracker.m_nMax - tracker.m_nMin) + 1);
                cRcvd                  += tracker.m_cPacketsRcvd;
                cBytes                 += tracker.m_cBytesReceived;
                cOutOfOrder            += tracker.m_cOutOfOrder;
                cTotalOutOfOrderOffset += tracker.m_cTotalOutOfOrderOffset;
                cGaps                  += tracker.m_cGaps;
                cGapPackets            += tracker.m_cGapPackets;
                cGapMillis             += tracker.m_cGapMillis;
                cAcks                  += tracker.m_cAcksIn;
                lDeltaRttNanos         += tracker.m_lDeltaRttNanos;
                }

            long   lDurationMs = lLastTime - lStartTime;
            long   iAvgOffset  = cTotalOutOfOrderOffset / cRcvd;
            String sThptMB     = DatagramTest.computeThroughputMBPerSec(cBytes, lDurationMs);
            String sThptPk     = DatagramTest.computeThroughputPacketsPerSec(cRcvd, lDurationMs);
            long   cRttNanos   = cAcks == 0 ? -1 : lDeltaRttNanos / cAcks;

            return new StringBuffer()
                    .append("Rx Summary from " + aTracker.length + " publisher(s): ")
                    .append("\n\t     elapsed: ").append(lDurationMs).append("ms")
                    .append("\n\t  throughput: ").append(sThptMB)
                    .append("\n\t              ").append(sThptPk)
                    .append("\n\t    received: ").append(cRcvd).append(" of ").append(cSent)
                    .append("\n\t     missing: ").append(cSent - cRcvd)
                    .append("\n\tsuccess rate: ").append(((float) cRcvd) / ((float) cSent))
                    .append("\n\tout of order: ").append(cOutOfOrder)
                    .append("\n\t  avg offset: ").append(iAvgOffset)
                    .append("\n\t        gaps: ").append(cGaps)
                    .append("\n\tavg gap size: ").append(cGapPackets / Math.max(1, cGaps))
                    .append("\n\tavg gap time: ").append(cGapMillis / Math.max(1, cGaps)).append("ms")
                    .append("\n\tavg ack time: ").append(cRttNanos / 1000000.0).append("ms; acks ").append(cAcks)
                    .append("\n\t   completed: ").append(formatDateTime(System.currentTimeMillis()))
                    .toString();
            }

        /**
        * Generate a report for all PacketTrackers in the Map.
        *
        * @param sPeriod    description of the period the results cover
        * @param mapTracker the map of trackers
        */
        public static void generateReport(String sPeriod, Map mapTracker)
            {
            out();
            out(sPeriod);
            for (Iterator iter = mapTracker.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry = (Map.Entry) iter.next();
                out(entry.getValue() + "\n");
                }

            if (mapTracker.size() > 1)
                {
                out(PacketTracker.toString(
                        (PacketTracker[])mapTracker.values().toArray(new PacketTracker[0])));
                }
            out("\n completed: " + formatDateTime(System.currentTimeMillis()));
            }

        // ----- data members -----------------------------------------

        protected SocketAddress m_addrBind;
        protected SocketAddress m_addrSender;
        protected AtomicLong    m_atomicAckOut;
        protected long          m_cPacketsRcvd;
        protected long          m_lStartTime;
        protected long          m_lLastPacketArrivalTime;
        protected long          m_nMin;
        protected long          m_nMax;
        protected long          m_nNext;
        protected int           m_cOutOfOrder;
        protected int           m_cTotalOutOfOrderOffset;
        protected long          m_cBytesReceived;
        protected long          m_cGaps;
        protected long          m_cGapPackets;
        protected long          m_cGapMillis;
        protected long          m_lDeltaRttNanos;
        protected long          m_cAcksIn;
        }


    // ----- inner class: StartFlag -----------------------------------------

    /**
    * The start flag is used to allow a listener to control a inner process
    * publisher.  This is used when the publisher is run in "polite" mode
    * to cause it to only publish when the listener is receiving data.
    */
    protected static class StartFlag
        {
        /**
        * Instruct any publisher waiting for the go flag that it may proceed.
        */
        public void go()
            {
            synchronized (this)
                {
                m_fGo = true;
                this.notifyAll();
                }
            }

        /**
        * Instruct any publisher using this flag that it should stop.
        */
        public void stop()
            {
            m_fGo = false;
            }

        /**
        * Return true if the publisher should stop.
        *
        * @return true if the publisher should stop
        */
        public boolean isStopped()
            {
            return !m_fGo;
            }

        /**
        * Wait for the go signal.
        *
        * @throws InterruptedException if interrupted while waiting
        */
        public void waitForGo()
                throws InterruptedException
            {
            synchronized (this)
                {
                while (!m_fGo)
                    {
                    Blocking.wait(this);
                    }
                }

            }


        // ----- data members -------------------------------------------

        protected volatile boolean m_fGo;
        }


    /**
    * The application start time.
    */
    public static final long s_ldtStart = System.currentTimeMillis();

    /**
    * Java 1.5 System.nanoTime() if available.
    */
    public static Method s_methodNano;
    static
        {
        try
            {
            s_methodNano = System.class.getMethod("nanoTime");
            }
        catch (Exception e)
            {
            s_methodNano = null;
            }
        }

    /**
    * Header sent with every packet.
    */
    public static final int MAGIC =
                       ('t' << 24)
                     | ('e' << 16)
                     | ('s' <<  8)
                     | ('t'      );

    /**
    * The portion of the header that matters. Set to 3.5 bytes just like TCMP
    */
    public static final int MAGIC_MASK = 0xFFFFFFF0;

    /**
    * Indicates if split sockets should be used, the can be specified via
    * the tangosol.coherence.splitsocket system property.
    */
    public static final boolean s_fSplitSocket = Boolean.parseBoolean(
            System.getProperty("tangosol.coherence.datagram.splitsocket",
                    "true"));
    }
