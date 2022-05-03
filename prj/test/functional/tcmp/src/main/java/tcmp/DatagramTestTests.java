/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package tcmp;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;

import com.oracle.bedrock.runtime.java.JavaApplication;

import com.oracle.bedrock.runtime.java.options.ClassName;

import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.util.Capture;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.DatagramTest;
import com.tangosol.net.InetAddressHelper;

import com.tangosol.util.Base;

import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

/**
 * See {@link DatagramTest}
 *
 * @author phf 2020.10.01
 */
public class DatagramTestTests
    {
    @Before
    public void setupTest()
        {
        LocalPlatform platform     = LocalPlatform.get();
        InetAddress   addrLoopback = platform.getLoopbackAddress();
        int           nMTU         = InetAddressHelper.getLocalMTU(addrLoopback);
        int           nRecvBufferSize;

        if (nMTU == 0)
            {
            nMTU = 1500;
            }

        // a large packet size is not required as this test is for functionality, not performance
        m_nPacketSize = Math.min(1400, nMTU);

        try (DatagramSocket socket = new DatagramSocket(0, addrLoopback))
            {
            nRecvBufferSize = socket.getReceiveBufferSize();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // calculate an rxBufferSize that will work on this machine
        m_nBufferSize = nRecvBufferSize / (m_nPacketSize + 1);

        Logger.info("Setting rxBufferSize to " + m_nBufferSize + " and packetSize to " + m_nPacketSize +
                    " based on MTU=" + nMTU + " and ReceiveBufferSize=" + nRecvBufferSize);

        m_platform    = platform;
        m_hostAddress = addrLoopback.getHostAddress();
        }

    /**
     * Test point to point Datagram.
     *
     * java -cp coherence.jar com.tangosol.net.DatagramTest -rxBufferSize 128 -local 127.0.0.1:8002 127.0.0.1:8004
     * java -cp coherence.jar com.tangosol.net.DatagramTest -rxBufferSize 128 -local 127.0.0.1:8002 127.0.0.1:8004
     */
    @Test
    public void testBidirectional()
        {
        final int    DURATION  = 20000;
        final String SDURATION = Integer.valueOf(DURATION).toString();

        String        sRxBufferSize = Integer.valueOf(m_nBufferSize).toString();
        String        sPacketSize   = Integer.valueOf(m_nPacketSize).toString();
        int           nPort1        = new Capture<>(m_platform.getAvailablePorts()).get();
        int           nPort2        = new Capture<>(m_platform.getAvailablePorts()).get();
        String[]      asArg1        = {"-txDurationMs", SDURATION, "-rxBufferSize", sRxBufferSize, "-packetSize", sPacketSize,
                                       "-local", m_hostAddress + ':' + nPort1, m_hostAddress + ':' + nPort2};
        String[]      asArg2        = {"-txDurationMs", SDURATION, "-rxBufferSize", sRxBufferSize, "-packetSize", sPacketSize,
                                       "-local", m_hostAddress + ':' + nPort2, m_hostAddress + ':' + nPort1};
        OptionsByType options       = OptionsByType.of();

        try (CapturingApplicationConsole console1     = new CapturingApplicationConsole();
             CapturingApplicationConsole console2     = new CapturingApplicationConsole();
             Application                 application1 = startDatagramTest(options, asArg1, console1);
             Application                 application2 = startDatagramTest(options, asArg2, console2))
            {
            try
                {
                Base.sleep(DURATION);

                // the "Rx" message will only get printed if packet transfers are successful
                Eventually.assertDeferred(console1::getCapturedOutputLines, hasItem(containsString("Rx from publisher:")));
                Eventually.assertDeferred(console2::getCapturedOutputLines, hasItem(containsString("Rx from publisher:")));
                }
            finally
                {
                // print the console output in the finally block so that they will be printed even if the assert fails
                if (application1 != null)
                    {
                    Queue<String> output1 = new LinkedList<>();
                    output1.addAll(console1.getCapturedOutputLines());
                    output1.addAll(console1.getCapturedErrorLines());
                    Logger.info("First 200 lines console1:");
                    output1.stream().limit(200).forEach(System.out::println);
                    }
                if (application2 != null)
                    {
                    Queue<String> output2 = new LinkedList<>();
                    output2.addAll(console2.getCapturedOutputLines());
                    output2.addAll(console2.getCapturedErrorLines());
                    Logger.info("First 200 lines console2:");
                    output2.stream().limit(200).forEach(System.out::println);
                    }
                }
            }
        }

    // ----- DatagramTestTests test helper methods --------------------------

    /**
     * Start a DatagramTest Java process with the specified {@link OptionsByType}
     * and arguments.
     *
     * @param options  the options used to start the DatagramTest process
     * @param asArg    the arguments to use
     * @param console  the console to receive the output
     *
     * @return the Java process Application
     */
    protected Application startDatagramTest(OptionsByType options, String[] asArg, CapturingApplicationConsole console)
        {
        options.add(Console.of(console));
        options.add(ClassName.of(DatagramTest.class));

        Arguments arguments = Arguments.empty();
        for (String sArg : asArg)
            {
            arguments = arguments.with(sArg);
            }
        options.add(arguments);

        Application application = m_platform.launch(JavaApplication.class, options.asArray());

        return application;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The local platform to use to launch DatagramTest processes.
     */
    private LocalPlatform m_platform;

    /**
     * The address for DatagramTest to send and receive packets.
     */
    private String m_hostAddress;

    /**
     * The "rxBufferSize" parameter value.
     */
    private int m_nBufferSize;

    /**
     * The "packetSize" parameter value.
     */
    private int m_nPacketSize;
    }
