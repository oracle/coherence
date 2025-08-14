/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.console.EventsApplicationConsole;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.CacheFactory;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * Functional test of the jdk logging functionality.
 *
 * @author jonathan knight  2021.12.7
 */
public class LogLevelTests extends AbstractLoggerTests
    {
    @Test
    public void testLogLevel() throws InterruptedException
        {
        String        sMessage_info_1 = "This is a INFO message";
        AtomicBoolean msgSeen1        = new AtomicBoolean(false);
        String        sMessage_info_2 = "This is a INFO message after Coherence level change";
        AtomicBoolean msgSeen2        = new AtomicBoolean(false);
        String        sMessage_finest = "This is a FINEST message";
        AtomicBoolean msgSeenFinest        = new AtomicBoolean(false);

        EventsApplicationConsole console = (EventsApplicationConsole) testLogs.builder()
                .build("storage");


        console.withStdErrListener(line ->
            {
            if (line.contains(sMessage_info_1))
                {
                msgSeen1.set(true);
                }
            if (line.contains(sMessage_info_2))
                {
                msgSeen2.set(true);
                }
            if (line.contains(sMessage_finest))
                {
                msgSeenFinest.set(true);
                }
            });

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class,
                    SystemProperty.of("test.log.level", CacheFactory.LOG_DEBUG),
                    SystemProperty.of("test.log", "stderr"),
                    Console.of(console)))
            {
            Eventually.assertDeferred(member::getClusterSize, is(1));
            
            // Default log level for Coherence logging is LOG_DEBUG
            assertThat(member.invoke(new IsLogLevelEnabled(CacheFactory.LOG_DEBUG)), is(true));
            assertThat(member.invoke(new IsLogLevelEnabled(CacheFactory.LOG_QUIET)), is(false));

            // FINEST level message should not be logged
            member.invoke(new WriteLogMessage(sMessage_finest, Logger.FINEST));

            // INFO level message 1 should be logged
            member.invoke(new WriteLogMessage(sMessage_info_1, Logger.INFO));

            // wait for info message 1 to be logged
            Eventually.assertDeferred(msgSeen1::get, is(true));
            // the finest message should not have been logged
            assertThat(msgSeenFinest.get(), is(false));

            // Change the Coherence log level
            member.invoke(() -> changeCoherenceLogLevel(CacheFactory.LOG_MAX));

            // FINEST level message should now be logged
            member.invoke(new WriteLogMessage(sMessage_finest, Logger.FINEST));

            for (int i = 0; !msgSeenFinest.get() && i < 10; i++)
                {
                // retry logic until observe transition to LOG_MAX enabled in logs, see COH-25184
                Thread.sleep(500L);
                member.invoke(new WriteLogMessage(sMessage_finest, Logger.FINEST));
                }

            // INFO level message 2 should be logged
            member.invoke(new WriteLogMessage(sMessage_info_2, Logger.INFO));

            // wait for info message 2 to be logged
            Eventually.assertDeferred(msgSeen2::get, is(true));
            }
        }


    public static class IsLogLevelEnabled
            implements RemoteCallable<Boolean>
        {
        public IsLogLevelEnabled(int nLevel)
            {
            m_nLevel = nLevel;
            }

        @Override
        public Boolean call() throws Exception
            {
            return Logger.isEnabled(m_nLevel);
            }

        // ----- data members -----------------------------------------------

        private final int m_nLevel;
        }


    public static class WriteLogMessage
            implements RemoteCallable<Void>
        {
        public WriteLogMessage(String sMessage, int nLevel)
            {
            m_sMessage = sMessage;
            m_nLevel   = nLevel;
            }

        @Override
        public Void call() throws Exception
            {
            Logger.log(m_sMessage, m_nLevel);
            return null;
            }

        // ----- data members -----------------------------------------------

        private final String m_sMessage;

        private final int m_nLevel;
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final TestLogs testLogs = new TestLogs(LogLevelTests.class);
    }
