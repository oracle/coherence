/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package logging;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.util.Base;
import common.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.StringWriter;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.tangosol.util.Base.LOG_FINEST;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;


/**
 * Functional test of the Log4j logging functionality.
 *
 * @author shing.wai.chan  2018.08.10
 */
public class Log4jTests extends AbstractFunctionalTest
    {

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("test.log.level", "9");
        System.setProperty("test.log", "log4j");

        Logger logger = Logger.getRootLogger();
        logger.addAppender(new WriterAppender(new PatternLayout("log4j: %m%n"), stringWriter));

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

        // The log level for Log4j logging is INFO,
        // it should override default coherence log level (LOG_DEBUG).
        assertFalse(CacheFactory.isLogEnabled(LOG_DEBUG));

        // FINEST level message should not be logged
        CacheFactory.log(sMessage_finest, LOG_FINEST);

        // INFO level message is logged
        CacheFactory.log(sMessage_info, LOG_INFO);

        // wait for the logger to wake
        Eventually.assertThat(invoking(this).isLogged(sMessage_info), is(true));
        assertFalse(isLogged(sMessage_finest));
        }

    /**
     * Ensure "Started cluster" is in the log.
     */
    @Test
    public void testStartedCluster()
        {
        Eventually.assertThat(invoking(this).isLogged("log4j: Started cluster"), is(true));
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
        return stringWriter.getBuffer().indexOf(sMsg) != -1;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A StringWriter stores all the log string.
     */
    private static StringWriter stringWriter = new StringWriter();
    }
