/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package tcmp;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Capture;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.coherence.common.internal.net.socketbus.AbstractSocketBus;
import com.oracle.coherence.common.internal.net.socketbus.BufferedSocketBus;
import com.oracle.coherence.common.internal.net.socketbus.SocketMessageBus;
import com.oracle.coherence.common.net.exabus.util.MessageBusTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.oracle.bedrock.deferred.DeferredHelper.delayedBy;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertTrue;


/**
 * @author lh 2020.01.14
 *
 * See MessageBusTest
 *
 * -bind tmb://127.0.0.1:30000 -peer tmb://127.0.0.1:30001 -polite -msgSize 4096..65536 -txRate 4096 -manager direct
 */
public class MessageBusTestTests
    {
    @Before
    public void setupTest()
            throws Exception
        {
        m_platform = LocalPlatform.get();
        m_hostAddress = m_platform.getLoopbackAddress().getHostAddress();
        }

    /**
     * Test point to point Message Bus.
     *
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port1 -peer tmb://localhost:port2 -polite
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port2 -peer tmb://localhost:port1
     */
    @Test
    public void testBidirectional()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = new String[5];
        String[] asArg2 = new String[4];

        asArg1[0] = "-bind";
        asArg1[1] = "tmb://" + m_hostAddress + ":" + port1;
        asArg1[2] = "-peer";
        asArg1[3] = "tmb://" + m_hostAddress + ":" + port2;
        asArg1[4] = "-polite";

        asArg2[0] = "-bind";
        asArg2[1] = "tmb://" + m_hostAddress + ":" + port2;
        asArg2[2] = "-peer";
        asArg2[3] = "tmb://" + m_hostAddress + ":" + port1;
        twoMembersTest(asArg1, asArg2);
        }

    /**
     * Test connection migration.
     * The occurance of connection migration depends on the machine on which the test is run.
     * So to avoid causing false alarm, ignore the test.
     *
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port1 -peer tmb://localhost:port2 -txRate 10000 -polite
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port2 -peer tmb://localhost:port1 -rxRate 10
     */
    @Test
    @Ignore
    public void testConnectionMigration()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = new String[7];
        String[] asArg2 = new String[6];

        asArg1[0] = "-bind";
        asArg1[1] = "tmb://" + m_hostAddress + ":" + port1;
        asArg1[2] = "-peer";
        asArg1[3] = "tmb://" + m_hostAddress + ":" + port2;
        asArg1[4] = "-txRate";
        asArg1[5] = "1000000000000";
        asArg1[6] = "-polite";

        asArg2[0] = "-bind";
        asArg2[1] = "tmb://" + m_hostAddress + ":" + port2;
        asArg2[2] = "-peer";
        asArg2[3] = "tmb://" + m_hostAddress + ":" + port1;
        asArg1[4] = "-rxRate";
        asArg1[5] = "10";

        OptionsByType               options      = OptionsByType.of();
        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        Queue<String>               output1      = new LinkedList<>();
        Application                 application1 = startMessageBusTest(options, asArg1, console1);
        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), hasItem(containsString("OPEN event for")));

        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        Queue<String>               output2      = new LinkedList<>();
        Application                 application2 = startMessageBusTest(options, asArg2, console2);

        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), hasItem(containsString("accepted connection migration")),
                delayedBy(20, TimeUnit.SECONDS));
        Eventually.assertThat(invoking(console2).getCapturedErrorLines(), hasItem(containsString("accepting connection migration")));

        output1.addAll(console1.getCapturedOutputLines());
        output1.addAll(console1.getCapturedErrorLines());
        output2.addAll(console2.getCapturedOutputLines());
        output2.addAll(console2.getCapturedErrorLines());
        System.out.println("Checking Status console1:");
        output1.forEach(System.out::println);
        System.out.println("Checking Status console2:");
        output2.forEach(System.out::println);

        application1.close();
        application2.close();
        }

    /**
     * Test Message Bus using direct and heap buffer managers.
     *
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port1 -peer tmb://localhost:port2 -manager direct -polite
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port2 -peer tmb://localhost:port1 -manager direct
     */
    @Test
    public void testBufferManager()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = new String[7];
        String[] asArg2 = new String[6];

        asArg1[0] = "-bind";
        asArg1[1] = "tmb://" + m_hostAddress + ":" + port1;
        asArg1[2] = "-peer";
        asArg1[3] = "tmb://" + m_hostAddress + ":" + port2;
        asArg1[4] = "-manager";
        asArg1[5] = "direct";
        asArg1[6] = "-polite";

        asArg2[0] = "-bind";
        asArg2[1] = "tmb://" + m_hostAddress + ":" + port2;
        asArg2[2] = "-peer";
        asArg2[3] = "tmb://" + m_hostAddress + ":" + port1;
        asArg2[4] = "-manager";
        asArg2[5] = "direct";
        twoMembersTest(asArg1, asArg2);

        asArg1[5] = "heap";
        asArg2[5] = "heap";
        twoMembersTest(asArg1, asArg2);
        }

    /**
     * Test point to point Message Bus.
     *
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port1
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port2 -peer tmb://localhost:port1
     */
    @Test
    public void testPointToPoint()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = new String[2];
        String[] asArg2 = new String[4];

        asArg1[0] = "-bind";
        asArg1[1] = "tmb://" + m_hostAddress + ":" + port1;

        asArg2[0] = "-bind";
        asArg2[1] = "tmb://" + m_hostAddress + ":" + port2;
        asArg2[2] = "-peer";
        asArg2[3] = "tmb://" + m_hostAddress + ":" + port1;
        twoMembersTest(asArg1, asArg2);
        }

    /**
     * Test distributed Message Bus.
     *
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port1 -peer tmb://localhost:port2 tmb://localhost:port3 -polite
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port2 -peer tmb://localhost:port1 tmb://localhost:port3 -polite
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port3 -peer tmb://localhost:port1 tmb://localhost:port2
     */
    @Test
    public void testDistributed()
            throws Exception
        {
        OptionsByType options = OptionsByType.of();

        String[]                    asArg    = new String[6];
        CapturingApplicationConsole console1 = new CapturingApplicationConsole();
        Queue<String>               output1  = new LinkedList<>();
        int                         port1    = new Capture<>(m_platform.getAvailablePorts()).get();
        int                         port2    = new Capture<>(m_platform.getAvailablePorts()).get();
        int                         port3    = new Capture<>(m_platform.getAvailablePorts()).get();

        asArg[0] = "-bind";
        asArg[1] = "tmb://" + m_hostAddress + ":" + port1;
        asArg[2] = "-peer";
        asArg[3] = "tmb://" + m_hostAddress + ":" + port2;
        asArg[4] = "tmb://" + m_hostAddress + ":" + port3;
        asArg[5] = "-polite";
        Application application1 = startMessageBusTest(options, asArg, console1);

        CapturingApplicationConsole console2 = new CapturingApplicationConsole();
        Queue<String> output2 = new LinkedList<>();

        asArg[1] = "tmb://" + m_hostAddress + ":" + port2;
        asArg[2] = "-peer";
        asArg[3] = "tmb://" + m_hostAddress + ":" + port1;
        asArg[4] = "tmb://" + m_hostAddress + ":" + port3;
        asArg[5] = "-polite";
        Application application2 = startMessageBusTest(options, asArg, console2);
        Eventually.assertThat(invoking(console2).getCapturedErrorLines(), hasItem(containsString("OPEN event for")));

        CapturingApplicationConsole console3 = new CapturingApplicationConsole();
        Queue<String> output3 = new LinkedList<>();

        asArg[1] = "tmb://" + m_hostAddress + ":" + port3;
        asArg[2] = "-peer";
        asArg[3] = "tmb://" + m_hostAddress + ":" + port1;
        asArg[4] = "tmb://" + m_hostAddress + ":" + port2;
        asArg[5] = null;
        Application application3 = startMessageBusTest(options, asArg, console3);

        Eventually.assertThat(invoking(console1).getCapturedOutputLines(), hasItem(containsString("connections 1, errors 0")),
                delayedBy(20, TimeUnit.SECONDS));
        assertTrue(hasItem(containsString("connections 1, errors 0")).matches(console2.getCapturedOutputLines()));
        assertTrue(hasItem(containsString("connections 2, errors 0")).matches(console3.getCapturedOutputLines()));

        output1.addAll(console1.getCapturedOutputLines());
        output1.addAll(console1.getCapturedErrorLines());
        output2.addAll(console2.getCapturedOutputLines());
        output2.addAll(console2.getCapturedErrorLines());
        output3.addAll(console3.getCapturedOutputLines());
        output3.addAll(console3.getCapturedErrorLines());

        System.out.println("Checking Status console1:");
        output1.forEach(System.out::println);

        System.out.println("Checking Status console2:");
        output2.forEach(System.out::println);

        System.out.println("Checking Status console3:");
        output3.forEach(System.out::println);

        application1.close();
        application2.close();
        application3.close();
        }

    /**
     * Test message size of MessageBusTest.
     */
    @Test
    public void testMessageSize()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = new String[7];
        String[] asArg2 = new String[6];

        asArg1[0] = "-bind";
        asArg1[1] = "tmb://" + m_hostAddress + ":" + port1;
        asArg1[2] = "-peer";
        asArg1[3] = "tmb://" + m_hostAddress + ":" + port2;
        asArg1[4] = "-msgSize";
        asArg1[5] = "65536..1000000";
        asArg1[6] = "-polite";

        asArg2[0] = "-bind";
        asArg2[1] = "tmb://" + m_hostAddress + ":" + port2;
        asArg2[2] = "-peer";
        asArg2[3] = "tmb://" + m_hostAddress + ":" + port1;
        asArg2[4] = "-msgSize";
        asArg2[5] = "1000000";
        twoMembersTest(asArg1, asArg2, 30000);
        }

    /**
     * 39634552 - test that a producer publishing the MPSC tail before linking the next entry does not
     * block the SelectionService thread long enough to trigger ack timeout connection migration.
     */
    @Test
    public void testMpscProducerPublicationWindow()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port1,
                "-peer",           "tmb://" + m_hostAddress + ":" + port2,
                "-txThreads",      "4",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "16m",
                "-reportInterval", "5s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port2,
                "-peer",           "tmb://" + m_hostAddress + ":" + port1,
                "-txThreads",      "4",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "16m",
                "-reportInterval", "5s"
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.ackTimeoutMillis", "2000"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.crc", "true"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "2"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketMessageBus.mpsc.enqueueLinkDelayMillis", "250"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketMessageBus.mpsc.pollMaxSpins", "16384"));

        twoMembersTest(options, asArg1, asArg2, 15000);
        }

    /**
     * Test that a partial gathering write retains a durable write-progress obligation when no subsequent
     * application send can nudge the connection.
     */
    @Test
    public void testPartialWriteProgressWithoutSubsequentSend()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port1,
                "-peer",           "tmb://" + m_hostAddress + ":" + port2,
                "-txThreads",      "1",
                "-msgSize",        "177302",
                "-cached",
                "-block",
                "-reportInterval", "1s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port2,
                "-peer",           "tmb://" + m_hostAddress + ":" + port1,
                "-txThreads",      "0",
                "-msgSize",        "177302",
                "-cached",
                "-reportInterval", "1s"
                };

        OptionsByType optionsSender = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.ackTimeoutMillis", "30000"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".partialWriteLimitBytes", "98304"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".partialWrites", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".zeroWritesAfterPartial", "1"),
                SystemProperty.of(BufferedSocketBus.class.getName() + ".dropWriteWakeupsAfterPartial", "1"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketMessageBus.mpsc.enqueueLinkDelayMillis", "500"),
                SystemProperty.of("coherence.socketbus.liveness.watchdogMillis", "200"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsSender, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(OptionsByType.of(), asArg2, console2);

            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getPartialWritesForTesting),
                    is(1L), within(10, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getZeroWritesAfterPartialForTesting),
                    is(1L), within(10, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(BufferedSocketBus::getDroppedWriteWakeupsForTesting),
                    is(1L), within(10, TimeUnit.SECONDS));

            assertThat(application1.invoke(BufferedSocketBus::getQueuedBytesAtDroppedWriteWakeupForTesting),
                    greaterThan(0L));
            assertThat(application1.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting), is(0L));
            assertNoMessageBusFailures(console1);
            assertNoMessageBusFailures(console2);

            int cLines1 = console1.getCapturedOutputLines().size();
            int cLines2 = console2.getCapturedOutputLines().size();
            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getBytesWrittenAfterPartialForTesting),
                    greaterThan(0L), within(10, TimeUnit.SECONDS));

            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);
            assertThat("the deliberately dropped wakeup was externally nudged",
                    application1.invoke(BufferedSocketBus::isDroppedWriteWakeupNudgePendingForTesting), is(true));
            assertThat(application1.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting), is(0L));
            assertThat(console1.getCapturedErrorLines(),
                    everyItem(not(containsString("LIVENESS[stalled-write]"))));
            assertNoMessageBusFailures(console1);
            assertNoMessageBusFailures(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that a producer-owned partial direct write returns queued send/resend progression to the transport
     * owner lane instead of continuing through the selector-only write loop on the application thread.
     */
    @Test
    public void testAdaptiveDirectPartialWriteOwnerHandoff()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port1,
                "-peer",           "tmb://" + m_hostAddress + ":" + port2,
                "-txThreads",      "1",
                "-msgSize",        "177302",
                "-cached",
                "-block",
                "-reportInterval", "1s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port2,
                "-peer",           "tmb://" + m_hostAddress + ":" + port1,
                "-txThreads",      "0",
                "-msgSize",        "177302",
                "-cached",
                "-reportInterval", "1s"
                };

        OptionsByType optionsSender = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.ackTimeoutMillis", "30000"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.autoFlushThreshold", "1KB"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".partialWriteLimitBytes", "98304"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".partialWrites", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".partialWritesOnNonOwnerOnly", "true"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".partialWriteRetryMillis", "5000"),
                SystemProperty.of(BufferedSocketBus.class.getName() + ".trackNonOwnerQueuedWriteHandoffs", "true"),
                SystemProperty.of(SocketMessageBus.class.getName() + ".blockAdaptiveDirectSend", "true"),
                SystemProperty.of(SocketMessageBus.class.getName() + ".blockAdaptiveDirectSendAfterPartial", "true"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsSender, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(OptionsByType.of(), asArg2, console2);

            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getPartialWritesForTesting),
                    is(1L), within(10, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(SocketMessageBus::getAdaptiveDirectSendBlocksForTesting),
                    is(1L), within(10, TimeUnit.SECONDS));
            assertThat(application1.invoke(SocketMessageBus::releaseAdaptiveDirectSendForTesting), is(true));
            Eventually.assertDeferred(
                    () -> application1.invoke(BufferedSocketBus::getNonOwnerQueuedWriteHandoffsForTesting),
                    greaterThan(0L), within(10, TimeUnit.SECONDS));

            assertThat(application1.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting), is(0L));
            assertNoMessageBusFailures(console1);
            assertNoMessageBusFailures(console2);
            }
        finally
            {
            try
                {
                application1.invoke(SocketMessageBus::releaseAdaptiveDirectSendForTesting);
                }
            catch (Throwable ignored)
                {
                }
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test the direct-to-MPSC ownership transition while receipts advance and the active transport starts migration.
     */
    @Test
    public void testAdaptiveDirectToMpscReceiptMigration()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port1,
                "-peer",           "tmb://" + m_hostAddress + ":" + port2,
                "-txThreads",      "2",
                "-msgSize",        "4096",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port2,
                "-peer",           "tmb://" + m_hostAddress + ":" + port1,
                "-txThreads",      "1",
                "-msgSize",        "4096",
                "-cached",
                "-txRate",         "64KBps",
                "-txMaxBacklog",   "16m",
                "-reportInterval", "1s"
                };

        OptionsByType optionsSender = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.ackTimeoutMillis", "30000"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.autoFlushThreshold", "1KB"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.maxReceiptDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "3"),
                SystemProperty.of(SocketMessageBus.class.getName() + ".blockAdaptiveDirectSend", "true"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".deferActiveReadFailures", "true"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"),
                SystemProperty.of(BufferedSocketBus.class.getName() + ".trackDeferredWriteProgression", "true"));
        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.maxReceiptDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "1000"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsSender, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(optionsPeer, asArg2, console2);

            Eventually.assertDeferred(
                    () -> application1.invoke(SocketMessageBus::getAdaptiveDirectSendBlocksForTesting),
                    is(1L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(SocketMessageBus::getAdaptiveMpscFallbacksDuringDirectForTesting),
                    greaterThan(0L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(SocketMessageBus::getAdaptiveReceiptsDuringDirectForTesting),
                    greaterThan(0L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(BufferedSocketBus::getDeferredWriteProgressionRequestsForTesting),
                    greaterThan(0L), within(30, TimeUnit.SECONDS));

            assertThat(application1.invoke(AbstractSocketBus::armActiveReadFailuresForTesting), is(true));
            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getActiveReadFailuresRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            assertThat(application1.invoke(SocketMessageBus::releaseAdaptiveDirectSendForTesting), is(true));

            Eventually.assertDeferred(
                    () -> application1.invoke(BufferedSocketBus::getDeferredWriteProgressionHandoffsForTesting),
                    greaterThan(0L), within(30, TimeUnit.SECONDS));

            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getCompletedMigrationCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));

            int cLines1 = console1.getCapturedOutputLines().size();
            int cLines2 = console2.getCapturedOutputLines().size();
            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);
            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            if (application1 != null)
                {
                try
                    {
                    application1.invoke(SocketMessageBus::releaseAdaptiveDirectSendForTesting);
                    }
                catch (Throwable ignored)
                    {
                    }
                application1.close();
                }
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that the receipt deadline flushes a small consumer-owned application batch instead of allowing the
     * application data to suppress the receipt indefinitely.
     */
    @Test
    public void testReceiptFlushPiggybacksDeferredApplicationData()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port1,
                "-peer",           "tmb://" + m_hostAddress + ":" + port2,
                "-txThreads",      "1",
                "-msgSize",        "65536",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + port2,
                "-peer",           "tmb://" + m_hostAddress + ":" + port1,
                "-reportInterval", "1s"
                };

        OptionsByType optionsSender = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.ackTimeoutMillis", "3000"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));
        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.maxReceiptDelayMillis", "100"),
                SystemProperty.of(BufferedSocketBus.class.getName() + ".trackReceiptFlushWithPendingData", "true"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsSender, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(optionsPeer, asArg2, console2);
            JavaApplication applicationPeer = application2;

            Eventually.assertDeferred(
                    () -> applicationPeer.invoke(BufferedSocketBus::getReceiptFlushesWithPendingDataForTesting),
                    greaterThan(0L), within(30, TimeUnit.SECONDS));

            long cReconnects = application1.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting);
            int  cLines1     = console1.getCapturedOutputLines().size();
            int  cLines2     = console2.getCapturedOutputLines().size();
            Eventually.assertDeferred(
                    () -> console1.getCapturedOutputLines().size(),
                    greaterThan(cLines1 + 5), within(15, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> console2.getCapturedOutputLines().size(),
                    greaterThan(cLines2 + 5), within(15, TimeUnit.SECONDS));
            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);
            assertThat(application1.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting), is(cReconnects));
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that producer write progression cannot cross a connection migration handshake.
     */
    @Test
    public void testMigrationHandshakeIsolation()
            throws Exception
        {
        int      port1         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      portMigration = Math.min(port1, port2);
        int      portPeer      = Math.max(port1, port2);
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-peer",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-txThreads",      "4",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-peer",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-txThreads",      "4",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s"
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.ackTimeoutMillis", "2000"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "10"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "5"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.producerBackpressureThresholdBytes", "64KB"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".concurrentReadMigrations", "3"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".reconnectSetupDelayMillis", "250"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "1000"));

        CapturingApplicationConsole console1 = new CapturingApplicationConsole();
        CapturingApplicationConsole console2 = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsPeer, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options, asArg2, console2);
            JavaApplication applicationMigration = application2;

            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getConcurrentReadMigrationsRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(3L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getCompletedOutboundMigrationCountForTesting),
                    greaterThanOrEqualTo(3L), within(30, TimeUnit.SECONDS));

            int cLines1 = console1.getCapturedOutputLines().size();
            int cLines2 = console2.getCapturedOutputLines().size();

            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);

            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that a routine connection reset is summarized without a warning-level stack trace.
     */
    @Test
    public void testRoutineConnectionResetLogging()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = createMigrationArguments(port1, port2, true);
        String[] asArg2 = createMigrationArguments(port2, port1, false);

        OptionsByType optionsReset = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "5"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));
        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "1000"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsPeer, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(optionsReset, asArg2, console2);
            JavaApplication applicationReset = application2;

            Eventually.assertDeferred(
                    () -> applicationReset.invoke(AbstractSocketBus::getActiveReadFailuresRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationReset.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(1L), within(10, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationReset.invoke(AbstractSocketBus::getCompletedMigrationCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));

            assertRoutineConnectionResetSummary(console1, console2);
            assertHealthyTrafficAfter(console1, console1.getCapturedOutputLines().size());
            assertHealthyTrafficAfter(console2, console2.getCapturedOutputLines().size());
            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that a superseded outbound handshake cannot disconnect the active replacement transport.
     */
    @Test
    public void testStaleOutboundHandshakeCannotDisconnectReplacement()
            throws Exception
        {
        int      port1         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      portMigration = Math.min(port1, port2);
        int      portPeer      = Math.max(port1, port2);
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-peer",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-txThreads",      "2",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-peer",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-txThreads",      "2",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s"
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "5"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".staleOutboundIntroductions", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "1000"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsPeer, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options, asArg2, console2);
            JavaApplication applicationMigration = application2;

            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getStaleOutboundIntroductionReplayCountForTesting),
                    is(1L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(2L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getCompletedOutboundMigrationCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));

            int cLines1 = console1.getCapturedOutputLines().size();
            int cLines2 = console2.getCapturedOutputLines().size();

            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);

            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that completing a TCP connect does not reset the migration retry count before the MessageBus handshake.
     */
    @Test
    public void testMigrationHandshakeRetryLimit()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 =
                {
                "-bind",   "tmb://" + m_hostAddress + ":" + port1,
                "-peer",   "tmb://" + m_hostAddress + ":" + port2,
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind", "tmb://" + m_hostAddress + ":" + port2,
                "-peer", "tmb://" + m_hostAddress + ":" + port1
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.dropRatio", "1"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "10"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "2"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".duplicateMigrationNotifications", "5"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        CapturingApplicationConsole console1 = new CapturingApplicationConsole();
        CapturingApplicationConsole console2 = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(OptionsByType.of(), asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options, asArg2, console2);
            JavaApplication applicationMigration = application2;

            Eventually.assertThat(invoking(console2).getCapturedErrorLines(),
                    hasItem(containsString("DISCONNECT event")), delayedBy(20, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    is(2L), within(20, TimeUnit.SECONDS));
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that a replacement socket open failure consumes one reconnect attempt and recovery continues.
     */
    @Test
    public void testMigrationSocketOpenFailureRetry()
            throws Exception
        {
        int      port1         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      portMigration = Math.min(port1, port2);
        int      portPeer      = Math.max(port1, port2);
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-peer",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-txThreads",      "2",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-peer",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-txThreads",      "2",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s"
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "3"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".duplicateMigrationNotifications", "5"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".reconnectOpenFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "1000"));

        CapturingApplicationConsole console1 = new CapturingApplicationConsole();
        CapturingApplicationConsole console2 = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsPeer, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options, asArg2, console2);
            JavaApplication applicationMigration = application2;

            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectOpenFailuresRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(2L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getCompletedOutboundMigrationCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));

            int cLines1 = console1.getCapturedOutputLines().size();
            int cLines2 = console2.getCapturedOutputLines().size();

            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);

            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that exhausting the retry budget after an initial socket-open failure disconnects cleanly.
     */
    @Test
    public void testInitialSocketOpenFailureDisconnect()
            throws Exception
        {
        int      port1 = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2 = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg =
                {
                "-bind", "tmb://" + m_hostAddress + ":" + port1,
                "-peer", "tmb://" + m_hostAddress + ":" + port2
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "0"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".initialOpenFailures", "1"));

        CapturingApplicationConsole console     = new CapturingApplicationConsole();
        JavaApplication             application = startMessageBusTest(options, asArg, console);

        try
            {
            Eventually.assertDeferred(
                    () -> application.invoke(AbstractSocketBus::getInitialOpenFailuresRemainingForTesting),
                    is(0), within(20, TimeUnit.SECONDS));
            Eventually.assertThat(invoking(console).getCapturedErrorLines(),
                    hasItem(containsString("DISCONNECT event")), within(20, TimeUnit.SECONDS));
            assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("NullPointerException"))));
            }
        finally
            {
            application.close();
            }
        }

    /**
     * Test that the fatal timeout bounds a replacement socket which does not complete its handshake.
     */
    @Test
    public void testMigrationHandshakeFatalTimeout()
            throws Exception
        {
        int      port1         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      portMigration = Math.min(port1, port2);
        int      portPeer      = Math.max(port1, port2);
        String[] asArg1 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-peer",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-txThreads",      "2",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s",
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portMigration,
                "-peer",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-txThreads",      "2",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s"
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.fatalTimeoutMillis", "200"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "3"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".reconnectHandshakeDelayMillis", "1000"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "2000"));

        CapturingApplicationConsole console1 = new CapturingApplicationConsole();
        CapturingApplicationConsole console2 = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsPeer, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options, asArg2, console2);
            JavaApplication applicationMigration = application2;

            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectHandshakeDelaysRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));
            Eventually.assertThat(invoking(console2).getCapturedErrorLines(),
                    hasItem(containsString("replacement handshake timeout")), within(30, TimeUnit.SECONDS));
            Eventually.assertThat(invoking(console2).getCapturedErrorLines(),
                    hasItem(containsString("DISCONNECT event")), within(30, TimeUnit.SECONDS));
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that the fatal timeout bounds an initial retry which does not complete its handshake.
     */
    @Test
    public void testInitialRetryHandshakeFatalTimeout()
            throws Exception
        {
        int      port1 = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2 = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 =
                {
                "-bind", "tmb://" + m_hostAddress + ":" + port1,
                "-peer", "tmb://" + m_hostAddress + ":" + port2,
                "-polite"
                };
        String[] asArg2 =
                {
                "-bind", "tmb://" + m_hostAddress + ":" + port2,
                "-peer", "tmb://" + m_hostAddress + ":" + port1
                };

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.fatalTimeoutMillis", "200"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "3"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".initialOpenFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".reconnectHandshakeDelayMillis", "1000"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(OptionsByType.of(), asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options, asArg2, console2);
            JavaApplication applicationRetry = application2;

            Eventually.assertDeferred(
                    () -> applicationRetry.invoke(AbstractSocketBus::getInitialOpenFailuresRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationRetry.invoke(AbstractSocketBus::getReconnectHandshakeDelaysRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationRetry.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));
            Eventually.assertThat(invoking(console2).getCapturedErrorLines(),
                    hasItem(containsString("replacement handshake timeout")), within(30, TimeUnit.SECONDS));
            Eventually.assertThat(invoking(console2).getCapturedErrorLines(),
                    hasItem(containsString("DISCONNECT event")), within(30, TimeUnit.SECONDS));
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that a blocking collector can reenter the bus without holding up the connection critical section.
     */
    @Test
    public void testBlockingReentrantCollectorOutsideConnectionLock()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = createMigrationArguments(port1, port2, true);
        String[] asArg2 = createMigrationArguments(port2, port1, false);

        OptionsByType options1 = OptionsByType.of(
                SystemProperty.of(MessageBusTest.class.getName() + ".collectorBlockMillis", "500"));
        OptionsByType options2 = OptionsByType.of(
                SystemProperty.of(MessageBusTest.class.getName() + ".collectorBlockMillis", "500"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(options1, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options2, asArg2, console2);
            JavaApplication applicationPeer = application2;

            Eventually.assertDeferred(
                    () -> application1.invoke(MessageBusTest::getBlockingCollectorInvocationCountForTesting),
                    is(1), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationPeer.invoke(MessageBusTest::getBlockingCollectorInvocationCountForTesting),
                    is(1), within(30, TimeUnit.SECONDS));

            assertThat(application1.invoke(MessageBusTest::getCollectorReentryTimeoutCountForTesting), is(0));
            assertThat(applicationPeer.invoke(MessageBusTest::getCollectorReentryTimeoutCountForTesting), is(0));
            assertThat(application1.invoke(AbstractSocketBus::getCollectorCallsUnderLockForTesting), is(0L));
            assertThat(applicationPeer.invoke(AbstractSocketBus::getCollectorCallsUnderLockForTesting), is(0L));

            assertHealthyTrafficAfter(console1, 0);
            assertHealthyTrafficAfter(console2, 0);
            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test simultaneous outbound failures and inbound replacement handshakes on both peers.
     */
    @Test
    public void testSimultaneousInboundOutboundMigration()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = createMigrationArguments(port1, port2, true);
        String[] asArg2 = createMigrationArguments(port2, port1, false);

        OptionsByType options1 = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".reconnectSetupDelayMillis", "250"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackTransportMetrics", "true"));
        OptionsByType options2 = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".reconnectSetupDelayMillis", "250"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackTransportMetrics", "true"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(options1, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options2, asArg2, console2);
            JavaApplication applicationPeer = application2;

            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getActiveReadFailuresRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationPeer.invoke(AbstractSocketBus::getActiveReadFailuresRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationPeer.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> application1.invoke(AbstractSocketBus::getCompletedMigrationCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationPeer.invoke(AbstractSocketBus::getCompletedMigrationCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));
            long cMigrationNanos1 = application1.invoke(AbstractSocketBus::getMigrationCompletionMaxNanosForTesting);
            long cMigrationNanos2 = applicationPeer.invoke(AbstractSocketBus::getMigrationCompletionMaxNanosForTesting);
            System.out.println("simultaneous migration completion nanos: peer1=" + cMigrationNanos1 +
                    ", peer2=" + cMigrationNanos2);
            assertThat(cMigrationNanos1, greaterThanOrEqualTo(1L));
            assertThat(cMigrationNanos2, greaterThanOrEqualTo(1L));

            int cLines1 = console1.getCapturedOutputLines().size();
            int cLines2 = console2.getCapturedOutputLines().size();
            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);
            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test that replacement handler registration failure consumes an attempt and recovery continues.
     */
    @Test
    public void testMigrationRegistrationFailureRetry()
            throws Exception
        {
        int      port1         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2         = new Capture<>(m_platform.getAvailablePorts()).get();
        int      portMigration = Math.min(port1, port2);
        int      portPeer      = Math.max(port1, port2);
        String[] asArg1        = createMigrationArguments(portPeer, portMigration, true);
        String[] asArg2        = createMigrationArguments(portMigration, portPeer, false);

        OptionsByType options = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "0"),
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectLimit", "3"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".activeReadFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".reconnectRegistrationFailures", "1"),
                SystemProperty.of(AbstractSocketBus.class.getName() + ".trackReconnectAttempts", "true"));
        OptionsByType optionsPeer = OptionsByType.of(
                SystemProperty.of("com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.reconnectDelayMillis", "1000"));

        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        JavaApplication             application1 = startMessageBusTest(optionsPeer, asArg1, console1);
        JavaApplication             application2 = null;

        try
            {
            application2 = startMessageBusTest(options, asArg2, console2);
            JavaApplication applicationMigration = application2;

            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectRegistrationFailuresRemainingForTesting),
                    is(0), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getReconnectAttemptCountForTesting),
                    greaterThanOrEqualTo(2L), within(30, TimeUnit.SECONDS));
            Eventually.assertDeferred(
                    () -> applicationMigration.invoke(AbstractSocketBus::getCompletedOutboundMigrationCountForTesting),
                    greaterThanOrEqualTo(1L), within(30, TimeUnit.SECONDS));

            int cLines1 = console1.getCapturedOutputLines().size();
            int cLines2 = console2.getCapturedOutputLines().size();
            assertHealthyTrafficAfter(console1, cLines1);
            assertHealthyTrafficAfter(console2, cLines2);
            assertMigrationHandshakeHealthy(console1);
            assertMigrationHandshakeHealthy(console2);
            }
        finally
            {
            application1.close();
            if (application2 != null)
                {
                application2.close();
                }
            }
        }

    /**
     * Test OOM error case.
     *
     * This test depends on the machine on which the test is run.
     * So to avoid causing false alarm, ignore the test.
     *
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port1 -peer tmb://localhost:port2 -msgSize 10000000 -txRate 10000000000 -txMaxBacklog 10m -polite
     * java -cp coherence.jar MessageBusTest -bind tmb://localhost:port2 -peer tmb://localhost:port1 -polite
     */
    @Test
    @Ignore
    public void testOOM()
            throws Exception
        {
        int      port1  = new Capture<>(m_platform.getAvailablePorts()).get();
        int      port2  = new Capture<>(m_platform.getAvailablePorts()).get();
        String[] asArg1 = new String[11];
        String[] asArg2 = new String[4];

        asArg1[0] = "-bind";
        asArg1[1] = "tmb://" + m_hostAddress + ":" + port1;
        asArg1[2] = "-peer";
        asArg1[3] = "tmb://" + m_hostAddress + ":" + port2;
        asArg1[4] = "-msgSize";
        asArg1[5] = "10000000";
        asArg1[6] = "-txRate";
        asArg1[7] = "10000000000";
        asArg1[8] = "-txMaxBacklog";
        asArg1[9] = "10m";
        asArg1[10] = "-polite";

        asArg2[0] = "-bind";
        asArg2[1] = "tmb://" + m_hostAddress + ":" + port2;
        asArg2[2] = "-peer";
        asArg2[3] = "tmb://" + m_hostAddress + ":" + port1;

        OptionsByType               options      = OptionsByType.of();
        CapturingApplicationConsole console1     = new CapturingApplicationConsole();
        Queue<String>               output1      = new LinkedList<>();
        Application                 application1 = startMessageBusTest(options, asArg1, console1);
        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), hasItem(containsString("OPEN event for")));

        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        Queue<String>               output2      = new LinkedList<>();
        Application                 application2 = startMessageBusTest(options, asArg2, console2);

        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), hasItem(containsString("java.lang.OutOfMemoryError:")),
                delayedBy(20, TimeUnit.SECONDS));

        output1.addAll(console1.getCapturedOutputLines());
        output1.addAll(console1.getCapturedErrorLines());
        output2.addAll(console2.getCapturedOutputLines());
        output2.addAll(console2.getCapturedErrorLines());
        System.out.println("Checking Status console1:");
        output1.forEach(System.out::println);
        System.out.println("Checking Status console2:");
        output2.forEach(System.out::println);

        application1.close();
        application2.close();
        }

    // ----- MessageBusTest test helper methods -----------------------------

    protected String[] createMigrationArguments(int portBind, int portPeer, boolean fPolite)
        {
        String[] asArg =
                {
                "-bind",           "tmb://" + m_hostAddress + ":" + portBind,
                "-peer",           "tmb://" + m_hostAddress + ":" + portPeer,
                "-txThreads",      "2",
                "-msgSize",        "1024",
                "-cached",
                "-txRate",         "1000000000",
                "-txMaxBacklog",   "1m",
                "-reportInterval", "1s"
                };

        if (!fPolite)
            {
            return asArg;
            }

        String[] asPolite = new String[asArg.length + 1];
        System.arraycopy(asArg, 0, asPolite, 0, asArg.length);
        asPolite[asArg.length] = "-polite";
        return asPolite;
        }

    /**
     * Start a MessageBusTest Java process with the specified {@link OptionsByType}
     * and arguments.
     *
     * @param options  the options used to start the MessageBusTest processes
     * @param asArg    the arguments to use
     * @param console  the console to receive the output
     *
     * @return the Java process application
     */
    protected JavaApplication startMessageBusTest(OptionsByType options, String[] asArg, CapturingApplicationConsole console)
            throws Exception
        {
        options.add(Console.of(console));
        options.add(ClassName.of(MessageBusTest.class));

        Arguments arguments = Arguments.empty();
        for (String sArg : asArg)
            {
            arguments = arguments.with(sArg);
            }
        options.add(arguments);

        JavaApplication application = m_platform.launch(JavaApplication.class, options.asArray());

        return application;
        }

    /*
     * Test MessageBusTest with 2 members.  Let the server run for
     * 20000 milliseconds before checking output.
     *
     * @param asArg1   arguments for member1
     * @param asArg2   arguments for member2
     */
    protected void twoMembersTest(String[] asArg1, String[] asArg2)
            throws Exception
        {
        twoMembersTest(asArg1, asArg2, 20000);
        }

    /*
     * Test MessageBusTest with 2 members.
     *
     * @param asArg1   arguments for member1
     * @param asArg2   arguments for member2
     * @param cMillis  time (in milliseconds) to run the servers before
     *                 checking the output
     */
    protected void twoMembersTest(String[] asArg1, String[] asArg2, long cMillis)
            throws Exception
        {
        twoMembersTest(OptionsByType.of(), asArg1, asArg2, cMillis);
        }

    /*
     * Test MessageBusTest with 2 members.
     *
     * @param options  the options used to start both members
     * @param asArg1   arguments for member1
     * @param asArg2   arguments for member2
     * @param cMillis  time (in milliseconds) to run the servers before
     *                 checking the output
     */
    protected void twoMembersTest(OptionsByType options, String[] asArg1, String[] asArg2, long cMillis)
            throws Exception
        {
        CapturingApplicationConsole console1 = new CapturingApplicationConsole();
        Queue<String> output1 = new LinkedList<>();

        Application application1 = startMessageBusTest(options, asArg1, console1);
        output1.addAll(console1.getCapturedOutputLines());
        output1.addAll(console1.getCapturedErrorLines());
        System.out.println("Checking Status:");
        output1.forEach(System.out::println);

        CapturingApplicationConsole console2 = new CapturingApplicationConsole();
        Queue<String> output2 = new LinkedList<>();

        Application application2 = startMessageBusTest(options, asArg2, console2);
        Thread.sleep(cMillis);

        Eventually.assertThat(invoking(console1).getCapturedOutputLines(), hasItem(containsString("connections 1, errors 0")),
                delayedBy(cMillis, TimeUnit.MILLISECONDS));
        Eventually.assertThat(invoking(console2).getCapturedOutputLines(), hasItem(containsString("connections 1, errors 0")));
        assertThat(console1.getCapturedErrorLines(), everyItem(not(containsString("WARNING: polling collector"))));
        assertThat(console2.getCapturedErrorLines(), everyItem(not(containsString("WARNING: polling collector"))));
        assertNoMessageBusFailures(console1);
        assertNoMessageBusFailures(console2);

        output1.addAll(console1.getCapturedOutputLines());
        output1.addAll(console1.getCapturedErrorLines());
        output2.addAll(console2.getCapturedOutputLines());
        output2.addAll(console2.getCapturedErrorLines());
        System.out.println("Checking Status console1:");
        output1.forEach(System.out::println);
        System.out.println("Checking Status console2:");
        output2.forEach(System.out::println);

        application1.close();
        application2.close();
        }

    protected boolean waitForHealthyTraffic(CapturingApplicationConsole console, long cMillis)
            throws InterruptedException
        {
        long ldtEnd = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(cMillis);
        do
            {
            if (console.getCapturedOutputLines().stream().anyMatch(PATTERN_HEALTHY_TRAFFIC.asPredicate()))
                {
                return true;
                }
            Thread.sleep(10L);
            }
        while (System.nanoTime() < ldtEnd);

        return false;
        }

    protected void assertNoMessageBusFailures(CapturingApplicationConsole console)
        {
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("PERF[ack-timeout]"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("initiating connection migration"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("incompatible protocol"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("protocol error"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("out of sync"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("corrupt"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("Exception"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("DISCONNECT"))));
        }

    protected void assertMigrationHandshakeHealthy(CapturingApplicationConsole console)
        {
        assertTrue(console.getCapturedOutputLines().stream().noneMatch(PATTERN_NONZERO_ERRORS.asPredicate()));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("IllegalBlockingModeException"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("incompatible protocol"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("protocol error"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("corrupt"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("DISCONNECT event"))));
        assertThat(console.getCapturedErrorLines(), everyItem(not(containsString("RELEASE event"))));
        }

    protected void assertRoutineConnectionResetSummary(CapturingApplicationConsole console1,
            CapturingApplicationConsole console2)
        {
        Eventually.assertDeferred(
                () -> console1.getCapturedErrorLines().stream().anyMatch(
                        line -> line.contains("cause=java.net.SocketException: Connection reset"))
                        || console2.getCapturedErrorLines().stream().anyMatch(
                                line -> line.contains("cause=java.net.SocketException: Connection reset")),
                is(true), within(30, TimeUnit.SECONDS));

        Queue<String> lines = new LinkedList<>();
        lines.addAll(console1.getCapturedErrorLines());
        lines.addAll(console2.getCapturedErrorLines());

        assertThat(lines, hasItem(containsString("cause=java.net.SocketException: Connection reset")));
        assertThat(lines, everyItem(not(containsString(
                "at com.oracle.coherence.common.internal.net.socketbus."))));
        }

    protected void assertHealthyTrafficAfter(CapturingApplicationConsole console, int cLines)
        {
        Eventually.assertDeferred(
                () -> console.getCapturedOutputLines().stream()
                        .skip(cLines)
                        .anyMatch(PATTERN_HEALTHY_TRAFFIC.asPredicate()),
                is(true), within(30, TimeUnit.SECONDS));
        }

    private static final Pattern PATTERN_HEALTHY_TRAFFIC = Pattern.compile(
            ".*throughput\\(out [1-9][0-9]*msg/s .*, in [1-9][0-9]*msg/s .*connections 1, errors 0.*");

    private static final Pattern PATTERN_NONZERO_ERRORS = Pattern.compile(".*errors [1-9][0-9]*.*");

    private LocalPlatform m_platform;
    private String        m_hostAddress;
    }
