/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package guardian;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.util.Duration;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;


import com.tangosol.util.Base;

import com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.coherence.common.util.Duration.Magnitude.MILLI;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS;

/**
 * Test the DefaultServiceFailurePolicy configuring exit-process and timeout for gracefully exiting JVM process.
 *
 * @author jf 2022.12.12
 */
public class DefaultServiceFailurePolicyTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("test.log.level", "5");
        AbstractFunctionalTest._startup();
        }

    @AfterClass
    public static void _shutdown()
        {
        CacheFactory.shutdown();

        // don't use the out() method, as it will restart the cluster
        System.out.println(createMessageHeader() + " <<<<<<< Stopped cluster");
        }

    @Test
    public void shouldExitByShutdownTimeoutNoShutdownHookHaltNotRequired()
        {
        shouldExitByShutdownTimeout("none", false);
        }

    @Test
    public void shouldExitByShutdownTimeoutNoShutdownHook()
        {
        shouldExitByShutdownTimeout("none", true);
        }

    @Test
    public void shouldExitByShutdownTimeoutGracefulShutdownHook()
        {
        shouldExitByShutdownTimeout("graceful", true);
        }

    @Test
    public void shouldExitByShutdownTimeoutForceShutdownHook()
        {
        shouldExitByShutdownTimeout("force", true);
        }

    public void shouldExitByShutdownTimeout(String sShutdownHook, boolean fHang)
        {
        // Note: sTimeout can not exceed the max default duration of
        //       Eventually.assertDeferred of one minute.
        String     sTimeout       = "14s";
        String     SERVER_PREFIX  = "DefaultServiceFailurePolicyTests";
        Duration   TIMEOUT        = new Duration(sTimeout);
        Properties props          = new Properties();
        String     sTimeoutMillis = Long.toString(TIMEOUT.as(MILLI));
        String     sTestScenario  = "ShtDwnHook" + sShutdownHook + (fHang ? "NeedHalt" : "");
        String     sServer        = SERVER_PREFIX + sTestScenario;
        Cluster    cluster        = CacheFactory.getCluster();

        props.put("coherence.shutdown.timeout", sTimeout);
        props.put("coherence.shutdownhook", sShutdownHook);

        startCacheServer(sServer,
                         "guardian", "coherence-cache-config.xml",
                         props, true, null);
        try
            {
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(2));

            InvocationService svc = (InvocationService) getFactory().ensureService("InvocationService");

            Set<Member> setInvocation = new HashSet<>();

            for (Member member : cluster.getMemberSet())
                {
                if (member.getRoleName().startsWith(SERVER_PREFIX))
                    {
                    setInvocation.add(member);
                    break;
                    }
                }

            long ldtStartTimeMillis = Base.getSafeTimeMillis();

            if (fHang)
                {
                Thread thread = new Thread(new Runnable()
                    {
                    @Override
                    public void run()
                        {
                        System.err.println("Start HungInvocation");
                        svc.execute(new HungInvocation(), setInvocation, null);
                        }
                    });
                thread.start();
                }

            System.err.println("Invoke OnServiceFailedInvocation on member " + setInvocation.iterator().next());
            svc.query(new OnServiceFailedInvocation(fHang), setInvocation);

            Eventually.assertDeferred("confirmed member with failed service left cluster",
                                      () -> cluster.getMemberSet().size(), is(1), within(30, TimeUnit.SECONDS));

            if (fHang)
                {
                Eventually.assertDeferred("validate halting message in server log", () ->
                    containsLogMessage(System.getProperty("test.project.dir"),
                                       "target/test-output/functional/" + sServer + ".out",
                                       "Oracle Coherence <Error>:  Failed to exit gracefully after " + sTimeoutMillis + " millis, halting process"),
                                          is(true));
                }

            long duration = (Base.getSafeTimeMillis() - ldtStartTimeMillis);

            System.out.println("OnServiceFailed property coherence.shutdown.timeout: " +
                               TIMEOUT.as(MILLI) + "ms, actual time millis onServiceFailed exit-process took: " + duration);

            if (fHang)
                {
                assertThat("validate test condition that member was hung during shutdown and that halt was employed",
                               duration, greaterThanOrEqualTo(TIMEOUT.as(MILLI)));
                }
            assertThat("validate that sys property coherence.shutdown.timeout " + TIMEOUT + " was honored",
                       duration, lessThan(TIMEOUT.as(MILLI) + 5000L));
            }
        finally
            {
            if (cluster.getMemberSet().size() > 1)
                {
                // test failure, clean up server that was not stopped
                stopCacheServer(sServer, false);
                }
            }
        }

    // ----- inner class: OnServiceFailedInvocation ---------------------

    /**
     * Test case for POLICY_EXIT_PROCESS with non-zero configured sys property
     * coherence.shutdown.timeout and optionally registers a hanging shutdown handler.
     */
    public static class OnServiceFailedInvocation
            extends AbstractInvocable
            implements ExternalizableLite
        {
        // ----- constructor ------------------------------------------------

        public OnServiceFailedInvocation(boolean fHang)
            {
            m_fHang = fHang;
            }

        /**
         * Constructor for serialization.
         */
        public OnServiceFailedInvocation()
            {}

        // ----- AbstractInvocable methods ----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override public void run()
            {
            System.err.println(Base.formatDateTime(Base.getSafeTimeMillis()) + " Start OnServiceFailedInvocation hangingInvocation=" + m_fHang);
            System.err.flush();

            if (m_fHang)
                {
                // register a shutdown handler that simulates hanging during graceful exit
                Runtime.getRuntime().addShutdownHook(new Thread()
                    {
                    public void run()
                        {
                        System.err.println(Base.formatDateTime(Base.getSafeTimeMillis()) + " Test Added Shutdown Hook is simulating waiting forever !");
                        System.err.flush();
                        while (true) ;
                        }
                    });
                }
            // validate onServiceFailed with POLICY_EXIT_PROCESS relying on default from system property coherence.shutdown.timeout.
            new DefaultServiceFailurePolicy(POLICY_EXIT_PROCESS).onServiceFailed(CacheFactory.getCluster());
            }

        public void readExternal(DataInput in) throws IOException
            {
            m_fHang = in.readBoolean();
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeBoolean(m_fHang);
            }

        // ----- data members ------------------------------------------------

        private boolean m_fHang;
        }

    // ----- inner class: HungInvocation -------------------------------------

    public static class HungInvocation
        extends AbstractInvocable
        {
        // ----- AbstractInvocable methods ----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
            {
            System.err.println(Base.formatDateTime(Base.getSafeTimeMillis()) + " Start HungInvocation");
            try
                {
                while (true);
                }
            finally
                {
                System.err.println(Base.formatDateTime(Base.getSafeTimeMillis()) + " Exit HungInvocation");
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    public static boolean containsLogMessage(String sDir, String sServerLogFilename, String sLogContains)
        {
        File fileServerOut = new File(sDir, sServerLogFilename);
        int            i   = 0;

        try (FileReader fileReader = new FileReader(fileServerOut);
             BufferedReader reader = new BufferedReader(fileReader))
            {
            String sLine = reader.readLine();

            while (sLine != null)
                {
                if (sLine.contains(sLogContains))
                    {
                    return true;
                    }
                sLine = reader.readLine();
                }
            }
        catch (IOException ignore) {}
        return false;
        }
    }