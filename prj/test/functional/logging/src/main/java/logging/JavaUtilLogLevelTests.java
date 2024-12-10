/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;


/**
 * Functional test of the jdk logging functionality.
 *
 * @author si  2013.10.15
 */
public class JavaUtilLogLevelTests
        extends AbstractLoggerTests
    {

    @BeforeClass
    public static void _statup()
        {
        System.setProperty("test.log.level", "9");
        System.setProperty("test.log", "jdk");

        System.setProperty("coherence.override", "logging-coherence-override.xml");
        System.setProperty("coherence.cacheconfig", "logging-cache-config.xml");

        Logger logger = m_logger = Logger.getLogger("Test");
        logger.addHandler(m_logHandler = new LogHandler());
        m_logHandler.m_enabled = true;

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Ensure
     *  1. messages are filtered based on destination log level.
     *  2. messages are logged based on the destination log level.
     */
    @Test
    public void testLogLevel()
        {
        String sMessage_info_1   = "This is a INFO message";
        String sMessage_info_2   = "This is a INFO message after Coherence level change";
        String sMessage_fine     = "This is a FINE message after logging level change";
        String sMessage_finest_1 = "This is a TRACE message";
        String sMessage_finest_2 = "This is a TRACE message after jdk level change";
        String sMessage_change   = "Change logging level from INFO to TRACE";

        // Default log level for JDK logging is INFO
        // with JDK logging configured, it should override
        // default coherence log level.
        assertFalse(com.oracle.coherence.common.base.Logger.isEnabled(5));

        // FINEST level message should not be logged
        com.oracle.coherence.common.base.Logger.finest(sMessage_finest_1);

        // INFO level message is logged
        com.oracle.coherence.common.base.Logger.info(sMessage_info_1);

        // wait for the logger to wake
        Eventually.assertDeferred(() -> isLogged(sMessage_info_1), is(true));
        assertFalse(isLogged(sMessage_finest_1));

        // Change the log level
        com.oracle.coherence.common.base.Logger.setLoggingLevel(5);

        // FINEST level message should not be logged
        com.oracle.coherence.common.base.Logger.finest(sMessage_finest_1);

        // FINE level message is logged
        com.oracle.coherence.common.base.Logger.fine(sMessage_fine);

        // wait for the logger to wake
        Eventually.assertDeferred(() -> isLogged(sMessage_fine), is(true));
        assertFalse(isLogged(sMessage_finest_1));

        // Change the Coherence log level - should have effect on Java logger as well
        com.oracle.coherence.common.base.Logger.info(sMessage_change);
        changeCoherenceLogLevel(9);

        // FINEST level message should be logged
        com.oracle.coherence.common.base.Logger.finest(sMessage_finest_1);

        Eventually.assertDeferred(() -> isLogged(sMessage_finest_1), is(true));

        // Change the Java logging level
        m_logger.setLevel(Level.INFO);

        // INFO level message is logged
        com.oracle.coherence.common.base.Logger.info(sMessage_info_2);

        // FINEST level message should not be logged
        com.oracle.coherence.common.base.Logger.finest(sMessage_finest_2);

        Eventually.assertDeferred(() -> isLogged(sMessage_info_2), is(true));
        assertFalse(isLogged(sMessage_finest_2));
        }

    @AfterClass
    public static void _shutdown()
        {
        System.clearProperty("test.log.level");
        System.clearProperty("test.log");
        System.clearProperty("coherence.override");
        System.clearProperty("coherence.cacheconfig");
        }

    /**
     * Helper method to check if a given string is logged.
     */
     public boolean isLogged(String sMsg)
         {
         boolean fMatch = false;
         for (String sLog : m_logHandler.collect())
             {
             fMatch |= sLog.contains(sMsg);
             }
         return fMatch;
         }

     // ----- inner class: LogHandler ----------------------------------------

     /**
      * A jdk logging handler to capture log messages when enabled.
      */
     public static class LogHandler
             extends Handler
         {

         // ----- Handler methods --------------------------------------------

         /**
          * {@inheritDoc}
          */
         @Override
         public synchronized void publish(LogRecord lr)
             {
             if (m_enabled)
                 {
                 m_listMessages.add(lr.getMessage());
                 }
             }

         /**
          * {@inheritDoc}
          */
         @Override
         public void flush()
             {
             }

         /**
          * {@inheritDoc}
          */
         @Override
         public synchronized void close() throws SecurityException
             {
             m_listMessages.clear();
             }

         /**
          * Returns a list of log messages collected.
          *
          * @return a list of log messages collected
          */
         public synchronized List<String> collect()
             {
             ArrayList<String> copy = new ArrayList<>(m_listMessages);
             return Collections.unmodifiableList(copy);
             }

         // ----- data members -----------------------------------------------

         /**
          * Whether to collect log messages.
          */
         protected volatile boolean m_enabled = false;

         /**
          * The log messages collected.
          */
         protected List<String> m_listMessages = new LinkedList<String>();
         }

     // ----- data members ---------------------------------------------------

    /**
    * The sniffing log handler that can be enabled / disabled.
    */
    private static LogHandler m_logHandler;

    /**
     * A reference to logger to ensure it is not gc'd as jdk only holds a
     * weak reference to the logger.
     */
     private static Logger m_logger;
    }
