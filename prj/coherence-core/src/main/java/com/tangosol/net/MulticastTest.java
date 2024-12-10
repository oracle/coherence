/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;



import com.oracle.coherence.common.base.Blocking;

import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Daemon;
import com.tangosol.util.UID;
import com.tangosol.util.ClassHelper;

import java.io.*;
import java.net.*;
import java.util.*;


/**
* Test multicast broadcast.
*
* @author cp  2002.09.30
*/
public class MulticastTest
        extends Base
    {
    /**
    * Parse and validate the command-line parameters and start the test.
    *
    * @param asArg  an array of command line parameters
    */
    public static void main(String[] asArg)
        {
        Map               mapArgs;
        InetAddress       addrIface;
        InetSocketAddress addrGroup;

        try
            {
            mapArgs = CommandLineTool.parseArguments(asArg, VALID_COMMANDS,
                   true /*case sensitive*/);
            if (mapArgs.containsKey(COMMAND_HELP) || mapArgs.get(Integer.valueOf(0)) != null)
                {
                showInstructions();
                return;
                }

            String sValue = (String) mapArgs.get(COMMAND_ADDR_LOCAL);
            addrIface = sValue == null ? null : InetAddressHelper.getLocalAddress(sValue);

            sValue = (String) mapArgs.get(COMMAND_ADDR_GROUP);
            if (sValue != null && sValue.startsWith(":"))
                {
                sValue = DEFAULT_IP_GROUP + sValue;
                }
            addrGroup  = InetAddressHelper.getSocketAddress(sValue == null ? DEFAULT_ADDR_GROUP : sValue, DEFAULT_PORT);
            }
        catch (Throwable e)
            {
            err();
            err(e);
            err();
            showInstructions();
            return;
            }


        int nTTL = DEFAULT_TTL;
        try
            {
            nTTL = Integer.parseInt((String) mapArgs.get(COMMAND_TTL));
            }
        catch (Exception e) {}

        int nMTU = addrIface == null ? 0 : InetAddressHelper.getLocalMTU(addrIface);
        if (nMTU == 0)
            {
            nMTU = 1500;
            }

        int cb = nMTU - 48; // 48 is suitable for both ipv4 and ipv6
        try
            {
            cb = (int) Base.parseMemorySize((String) mapArgs.get(COMMAND_PACKET));
            }
        catch (Exception e) {}

        int cSecsDelay = DEFAULT_DELAY;
        try
            {
            cSecsDelay = Integer.parseInt((String) mapArgs.get(COMMAND_DELAY));
            }
        catch (Exception e) {}

        int cbMax = DEFAULT_DISPLAY;
        try
            {
            cbMax = Integer.parseInt((String) mapArgs.get(COMMAND_DISPLAY));
            }
        catch (Exception e) {}

        boolean fTranslate = mapArgs.containsKey(COMMAND_TRANSLATE);

        if (addrIface != null && addrIface.isMulticastAddress())
            {
            out("Interface address " + addrIface + " is multi-cast; it must be an IP address bound to a physical interface");
            showInstructions();
            return;
            }

        if (!addrGroup.getAddress().isMulticastAddress())
            {
            out("Multicast address " + addrGroup + " is not multi-cast; it must be in the range 224.0.0.0 to 239.255.255.255");
            showInstructions();
            return;
            }


        if (nTTL < 0 || nTTL > 0xFF)
            {
            out("TTL " + nTTL + " is out of range; it must be in the range 1 to 255");
            showInstructions();
            return;
            }


        if (mapArgs.isEmpty())
            {
            showInstructions();
            out();
            out("running with all default values...");
            out();
            }

        try
            {
            new MulticastTest(addrIface, addrGroup, nTTL,
                    Math.max(cSecsDelay, 1), Math.max(cbMax, 0), fTranslate, cb)
                    .run();
            }
        catch (Exception e)
            {
            err("An exception occurred while executing the MulticastTest:");
            err(e);
            err();
            }
        finally
            {
            out();
            out("Exiting MulticastTest");
            }
        }

    /**
    * Display the command-line instructions.
    */
    protected static void showInstructions()
        {
        out();
        out("java com.tangosol.net.MulticastTest <commands ...>");
        out();
        out("command options:");
        out("\t-local      (optional) the address of the NIC to transmit on, specified as an ip address, default " + DEFAULT_ADDR_LOCAL);
        out("\t-group      (optional) the multicast address to use, specified as ip:port, default " + DEFAULT_ADDR_GROUP);
        out("\t-ttl        (optional) the time to live for multicast packets, default " + DEFAULT_TTL);
        out("\t-delay      (optional) the delay in seconds between sending packets, default " + DEFAULT_DELAY);
        out("\t-packetSize (optional) the size of packet to send, default based on local MTU");
        out("\t-display    (optional) the number of bytes to display from unexpected packets, default " + DEFAULT_DISPLAY);
        out("\t-translate  (optional) listen to cluster multicast traffic and translate packets");
        out();
        out("Example:");
        out("\tjava com.tangosol.net.MulticastTest -group 237.0.0.1:9000 -ttl 4");
        out();
        }


    // ----- MulticastTest methods ------------------------------------------

    /**
    * Construct the MulticastTest object.
    *
    * @param addrIface   address of the interface (NIC)
    * @param addrGroup   address of the multicast group
    * @param nTTL        TTL setting for multicast packets
    * @param cSecsDelay  number of seconds to delay between pings
    * @param cbMax       max number of bytes in the packet body to display
    * @param fTranslate  true to translate and display cluster traffic
    * @param cb          the packet size
    */
    public MulticastTest(InetAddress addrIface, InetSocketAddress addrGroup, int nTTL, int cSecsDelay, int cbMax, boolean fTranslate, int cb)
        {
        m_addrIface  = addrIface;
        m_addrGroup  = addrGroup;
        m_nTTL       = nTTL;
        m_cSecsDelay = fTranslate ? 60 * 60 : cSecsDelay;
        m_cbMax      = cbMax;
        m_sNode      = "ip=" + addrIface
                     + ", group=" + addrGroup
                     + ", ttl="   + nTTL;
        m_fTranslate = fTranslate;
        m_cb         = cb;
        }

    /**
    * Run the test.
    */
    public void run()
        {
        String sNode = m_sNode;
        out("Starting test on " + sNode);

        out("Configuring multicast socket...");
        initSocket();

        out("Starting listener...");
        Listener listener = new Listener(m_cb);
        listener.start();

        int cPackets = 0;
        try
            {
            while (true)
                {
                if (!m_fTranslate)
                    {
                    ByteArrayOutputStream streamRaw  = new ByteArrayOutputStream();
                    DataOutputStream      streamData = new DataOutputStream(streamRaw);
                    streamData.writeInt(MAGIC);
                    streamData.writeLong(System.currentTimeMillis());
                    streamData.writeInt(++cPackets);
                    m_uid.save(streamData);
                    streamData.writeUTF(sNode);
                    for (int i = m_cb - streamData.size(); i > 0; --i)
                        {
                        streamData.write(i);
                        }
                    streamData.close();
                    streamRaw.flush();
                    byte[] ab = streamRaw.toByteArray();

                    out(new Date() + ": Sent packet " + cPackets + " containing " + ab.length + " bytes.");

                    DatagramPacket packet = new DatagramPacket(ab, ab.length,
                            m_addrGroup.getAddress(), m_addrGroup.getPort());
                    m_socket.send(packet);
                    }

                Blocking.sleep(m_cSecsDelay * 1000);
                }
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Open the multicast socket.
    */
    protected void initSocket()
        {
        try
            {
            MulticastSocket socket = new MulticastSocket(m_addrGroup.getPort());
            if (m_addrIface != null)
                {
                socket.setInterface(m_addrIface);
                }

            socket.setTimeToLive(m_nTTL);
            socket.joinGroup(m_addrGroup.getAddress());
            m_socket = socket;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    public void displayPacket(ReadBuffer buf, InetSocketAddress addr)
            throws Exception
        {
        ReadBuffer.BufferInput streamData = buf.getBufferInput();
        int iFirst = buf.length() >= 4 ? streamData.readInt() : 0;
        if (iFirst == MAGIC)
            {
            long   cMillis  = streamData.readLong();
            int    cPackets = streamData.readInt();
            UID    uid      = new UID(streamData);
            String sNode    = streamData.readUTF();

            if (uid.equals(m_uid))
                {
                long cDif = System.currentTimeMillis() - cMillis;
                out(new Date() + ": Received test packet " + cPackets + " from self"
                        + (cDif <= 0 ? "" : " (sent " + cDif + "ms ago)."));
                }
            else
                {
                out(new Date() + ": Received test packet " + cPackets + " from " + addr + " containing " + buf.length() + " bytes.");
                }
            }
        else if (m_fTranslate && iFirst == BROADCAST)
            {
            Class clzCoherence = m_clzCoherence;
            if (clzCoherence == null)
                {
                clzCoherence = m_clzCoherence =
                    Class.forName("com.tangosol.coherence.component.application.console.Coherence");
                }
            m_msgPrevHeartbeat = ClassHelper.invokeStatic(clzCoherence,
                "displayMessage", new Object[] {buf, addr, m_msgPrevHeartbeat});
            }
        else
            {
            int cb = buf.length();
            StringBuffer sb = new StringBuffer();
            sb.append(cb)
              .append(" bytes from ");

            if ((iFirst & CLUSTER_MASK) == CLUSTER)
                {
                sb.append("a Coherence cluster node");
                }
            else
                {
                sb.append("an unknown multicast application");
                }

            if (addr != null)
                {
                sb.append(" at ")
                  .append(addr.getAddress().getHostAddress());
                }

            sb.append(": ");

            if (m_cbMax == 0)
                {
                sb.append("???");
                }
            else
                {
                sb.append(Base.toHexEscape(buf.toByteArray(), 0, Math.min(m_cbMax, cb)));
                }

            out(new Date() + ": Received " + sb);
            }
        }


    // ----- Listener inner class -------------------------------------------

    public class Listener
            extends Daemon
        {
        /**
        * Construct the Listener (not started).
        */
        public Listener(int cb)
            {
            super("listener", Thread.NORM_PRIORITY, false);
            m_cb = cb;
            }

        /**
        * The listener loop.
        */
        public void run()
            {
            try
                {
                while (true)
                    {
	                byte[] ab = new byte[m_cb];
                    DatagramPacket packet = new DatagramPacket(ab, ab.length);
                    m_socket.receive(packet);
                    displayPacket(new ByteArrayReadBuffer(ab, 0, packet.getLength()),
                                  new InetSocketAddress(packet.getAddress(), packet.getPort()));
                    }
                }
            catch (Exception e)
                {
                err("The listener encountered an exception: ");
                err(e);
                err();

                err("The listener is terminating.");
                err();
                return;
                }
            }

        private int m_cb;
        }


    // ----- data members ---------------------------------------------------

    private InetAddress       m_addrIface;
    private InetSocketAddress m_addrGroup;
    private int               m_nTTL;
    private String            m_sNode;
    private MulticastSocket   m_socket;
    private int               m_cSecsDelay;
    private int               m_cbMax;
    private UID               m_uid        = new UID();
    private boolean           m_fTranslate;
    private int               m_cb;
    private Class             m_clzCoherence;
    private Object            m_msgPrevHeartbeat;


    // ---- constants -------------------------------------------------------

    public static final String COMMAND_HELP       = "?";
    public static final String COMMAND_ADDR_LOCAL = "local";
    public static final String COMMAND_ADDR_GROUP = "group";
    public static final String COMMAND_TTL        = "ttl";
    public static final String COMMAND_DELAY      = "delay";
    public static final String COMMAND_DISPLAY    = "display";
    public static final String COMMAND_TRANSLATE  = "translate";
    public static final String COMMAND_PACKET     = "packetSize";

    public static final String DEFAULT_ADDR_LOCAL = "localhost";
    public static final int    DEFAULT_PORT       = 9000;
    public static final String DEFAULT_IP_GROUP   = "239.192.0.0";
    public static final String DEFAULT_ADDR_GROUP = DEFAULT_IP_GROUP + ":" + DEFAULT_PORT;
    public static final int    DEFAULT_TTL        = 4;
    public static final int    DEFAULT_DELAY      = 2;
    public static final int    DEFAULT_DISPLAY    = 0;
    public static final int    DEFAULT_PACKET     = 1468;

    static final int MAGIC = ('t' << 24)
                     | ('e' << 16)
                     | ('s' <<  8)
                     | ('t'      );

    static final int CLUSTER      = 0x0DDF00D0;
    static final int CLUSTER_MASK = 0xFFFFFFF0;
    static final int BROADCAST    = 0x0DDF00D2;

    public static final String[] VALID_COMMANDS = {
            COMMAND_HELP,
            COMMAND_ADDR_LOCAL,
            COMMAND_ADDR_GROUP,
            COMMAND_TTL,
            COMMAND_PACKET,
            COMMAND_DELAY,
            COMMAND_DISPLAY,
            COMMAND_TRANSLATE,
            };
    }
