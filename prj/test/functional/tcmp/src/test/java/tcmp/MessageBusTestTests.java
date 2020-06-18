/*
 * Copyright (c) 2000, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package tcmp;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Capture;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.coherence.common.net.exabus.util.MessageBusTest;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.delayedBy;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertFalse;
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
        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), Matchers.hasItem(containsString("OPEN event for")));

        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        Queue<String>               output2      = new LinkedList<>();
        Application                 application2 = startMessageBusTest(options, asArg2, console2);

        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), Matchers.hasItem(containsString("accepted connection migration")),
                delayedBy(20, TimeUnit.SECONDS));
        Eventually.assertThat(invoking(console2).getCapturedErrorLines(), Matchers.hasItem(containsString("accepting connection migration")));

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
        Eventually.assertThat(invoking(console2).getCapturedErrorLines(), Matchers.hasItem(containsString("OPEN event for")));

        CapturingApplicationConsole console3 = new CapturingApplicationConsole();
        Queue<String> output3 = new LinkedList<>();

        asArg[1] = "tmb://" + m_hostAddress + ":" + port3;
        asArg[2] = "-peer";
        asArg[3] = "tmb://" + m_hostAddress + ":" + port1;
        asArg[4] = "tmb://" + m_hostAddress + ":" + port2;
        asArg[5] = null;
        Application application3 = startMessageBusTest(options, asArg, console3);

        Eventually.assertThat(invoking(console1).getCapturedOutputLines(), Matchers.hasItem(containsString("connections 1, errors 0")),
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
        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), Matchers.hasItem(containsString("OPEN event for")));

        CapturingApplicationConsole console2     = new CapturingApplicationConsole();
        Queue<String>               output2      = new LinkedList<>();
        Application                 application2 = startMessageBusTest(options, asArg2, console2);

        Eventually.assertThat(invoking(console1).getCapturedErrorLines(), Matchers.hasItem(containsString("java.lang.OutOfMemoryError:")),
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

    /**
     * Start a MessageBusTest Java process with the specified {@link OptionsByType}
     * and arguments.
     *
     * @param options  the options used to start the MessageBusTest processes
     * @param asArg    the arguments to use
     * @param console  the console to receive the output
     *
     * @return the Java process Application
     */
    protected Application startMessageBusTest(OptionsByType options, String[] asArg, CapturingApplicationConsole console)
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

        Application application = m_platform.launch(JavaApplication.class, options.asArray());

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
        OptionsByType options = OptionsByType.of();

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

        Eventually.assertThat(invoking(console1).getCapturedOutputLines(), Matchers.hasItem(containsString("connections 1, errors 0")),
                delayedBy(cMillis, TimeUnit.MILLISECONDS));
        Eventually.assertThat(invoking(console2).getCapturedOutputLines(), Matchers.hasItem(containsString("connections 1, errors 0")));
        assertFalse(hasItem(containsString("WARNING:")).matches(console1.getCapturedErrorLines()));
        assertFalse(hasItem(containsString("WARNING:")).matches(console2.getCapturedErrorLines()));

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

    private LocalPlatform m_platform;
    private String        m_hostAddress;
    }
