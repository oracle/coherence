/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package logging;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import common.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;


/**
 * Functional test of the jdk logging functionality.
 *
 * @author si  2013.10.15
 */
public class LogLevelTests extends AbstractFunctionalTest
    {

    @BeforeClass
    public static void _statup()
        {
        System.setProperty("test.log.level", "9");
        System.setProperty("test.log", "jdk");

        Logger logger = m_logger = Logger.getLogger("Coherence");
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
        String sMessage_info   = "This is a INFO message";
        String sMessage_finest = "This is a TRACE message";

        // Default log level for JDK logging is INFO
        // with JDK logging configured, it should override
        // default coherence log level (LOG_DEBUG).
        assertFalse(CacheFactory.isLogEnabled(LOG_DEBUG));

        // FINEST level message should not be logged
        m_logger.log(Level.FINEST, sMessage_finest);

        // INFO level message is logged
        m_logger.log(Level.INFO, sMessage_info);

        // wait for the logger to wake
        Eventually.assertThat(invoking(this).isLogged(sMessage_info), is(true));
        assertFalse(isLogged(sMessage_finest));
        }

    @AfterClass
    public static void _shutdown()
        {
        System.clearProperty("test.log.level");
        System.clearProperty("test.log");
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
        public void publish(LogRecord lr)
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
        public void close() throws SecurityException
            {
            m_listMessages.clear();
            }

        /**
         * Returns a list of log messages collected.
         *
         * @return a list of log messages collected
         */
        public List<String> collect()
            {
            return Collections.unmodifiableList(m_listMessages);
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
