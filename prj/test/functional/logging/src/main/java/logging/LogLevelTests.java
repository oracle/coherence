/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.CacheFactory;
import org.junit.Test;

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
        String sMessage_info_1 = "This is a INFO message";
        String sMessage_info_2 = "This is a INFO message after Coherence level change";
        String sMessage_finest = "This is a FINEST message";

        CapturingApplicationConsole console = new CapturingApplicationConsole();

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class,
                    SystemProperty.of("test.log.level", CacheFactory.LOG_DEBUG),
                    SystemProperty.of("test.log", "stderr"),
                    Console.of(console)))
            {
            Eventually.assertDeferred(member::getClusterSize, is(1));
            
            // Default log level for Coherence logging is LOG_DEBUG
            assertThat(member.invoke(() -> Logger.isEnabled(CacheFactory.LOG_DEBUG)), is(true));
            assertThat(member.invoke(() -> Logger.isEnabled(CacheFactory.LOG_QUIET)), is(false));

            // FINEST level message should not be logged
            member.invoke(() ->
                {
                Logger.finest(sMessage_finest);
                return null;
                });

            // INFO level message 1 should be logged
            member.invoke(() ->
                {
                Logger.info(sMessage_info_1);
                return null;
                });

            // wait for info message 1 to be logged
            Eventually.assertDeferred(() -> isLogged(console, sMessage_info_1), is(true));
            // the finest message should not have been logged
            assertThat(isLogged(console, sMessage_finest), is(false));

            // Change the Coherence log level
            member.invoke(() -> changeCoherenceLogLevel(CacheFactory.LOG_MAX));

            // FINEST level message should now be logged
            member.invoke(() ->
                {
                Logger.finest(sMessage_finest);
                return null;
                });

            for (int i = 0; !isLogged(console, sMessage_finest) && i < 10; i++)
                {
                // retry logic until observe transition to LOG_MAX enabled in logs, see COH-25184
                Thread.sleep(500L);
                member.invoke(() ->
                    {
                    Logger.finest(sMessage_finest);
                    return null;
                    });
                }

            // INFO level message 2 should be logged
            member.invoke(() ->
                {
                Logger.info(sMessage_info_2);
                return null;
                });

            // wait for info message 2 to be logged
            Eventually.assertDeferred(() -> isLogged(console, sMessage_info_1), is(true));
            }
        }

     private boolean isLogged(CapturingApplicationConsole console, String sMsg)
         {
         return console.getCapturedErrorLines()
                 .stream()
                 .anyMatch(s -> s.contains(sMsg));
         }
    }
