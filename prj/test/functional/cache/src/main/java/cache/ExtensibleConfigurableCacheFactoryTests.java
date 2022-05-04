/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.options.Timeout;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.TypeAssertion;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * The {@link ExtensibleConfigurableCacheFactoryTests} functional tests.
 *
 * Only testing ensureTypedCache() functionality.
 *
 * @author jf 2105.08.07
 */
public class ExtensibleConfigurableCacheFactoryTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ExtensibleConfigurableCacheFactoryTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("test.log", "jdk");

        Logger logger = Logger.getLogger("Coherence");
        logger.addHandler(s_logHandler = new LogHandler());
        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Validate ensureTypedCache works with built-in and user types, local and distributed caches.
     */
    @Test
    public void testEnsureTypedCache()
        {
        NamedCache<Integer, String> cache1 = getNamedCache("local-1", Integer.class, String.class);
        NamedCache<MyKey, MyValue>  cache2 = getNamedCache("local-3", MyKey.class, MyValue.class);
        NamedCache<Integer, String> cache3 = getNamedCache("dist-1", Integer.class, String.class);

        assertNotNull(cache1);
        assertNotNull(cache2);
        assertNotNull(cache3);
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSecondGetOfEnsureTypedCache()
        {
        NamedCache<Integer, String> cache1 = getNamedCache("local-1", Integer.class, String.class);
        assertNotNull(cache1);
        NamedCache<String, String> cache2 = getNamedCache("local-1", String.class, String.class);
        assertNull(cache2);
        }

    @Test
    public void testWarnRawEnsureWithTypedCacheMapping()
        {
        NamedCache cache1 = getNamedCache("dist-1");
        assertNotNull(cache1);
        }

    @Test
    public void testOneWarnRawEnsureWithTypedCacheMapping()
            throws InterruptedException
        {
        for (int i = 0; i < 100; i++)
            {
            NamedCache cache1 = getNamedCache("dist-1234");
            assertNotNull(cache1);
            }
        Eventually.assertThat(invoking(s_logHandler).matchCount("The cache \"dist-1234\" has been configured as NamedCache"),
                is(1), Timeout.after("30s"));
        }

    @Test
    public void testNoWarnEnsureWithoutTypedCacheMapping()
        {
        for (int i = 0; i < 10; i++ )
            {
            NamedCache cache1 = getFactory().ensureTypedCache("dist-1", null, TypeAssertion.withoutTypeChecking());
            assertNotNull(cache1);
            NamedCache cache2 = getFactory().ensureTypedCache("local-1", null, TypeAssertion.withoutTypeChecking());
            assertNotNull(cache2);
            NamedCache cache3 = getFactory().ensureTypedCache("local-3", null, TypeAssertion.withoutTypeChecking());
            assertNotNull(cache3);
            }
        }

    @Test
    public void testWarnTypedAccessToRawMapping()
        {
        NamedCache<Integer, String> cache1 = getNamedCache("local-raw", Integer.class, String.class);
        assertNotNull(cache1);
        }

    @Test
    public void testOneWarnTypedAccessToRawMapping()
        {
        for (int i = 0; i < 100; i++)
            {
            NamedCache<Integer, String> cache1 = getNamedCache("local-raw-42", Integer.class, String.class);
            assertNotNull(cache1);
            }
        Eventually.assertThat(invoking(s_logHandler).matchCount("The cache \"local-raw-42\" is configured"), is(1),
                Timeout.after("30s"));
        }

    // ----- nested classes -------------------------------------------------

    public static class MyKey
        {
        }

    public static class MyValue
        {
        }

    // ----- inner class: LogHandler ----------------------------------------

    /**
     * A jdk logging handler to capture log messages when enabled.
     */
    public static class LogHandler extends Handler
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
         * Return true if any of the accumulated messages contain the given
         * {@code sLine}.
         *
         * @return true if any of the accumulated messages contain the given
         *         {@code sLine}
         */
        public boolean contains(String sLine)
            {
            boolean[] af = new boolean[1];
            m_listMessages.forEach(s ->
            {
            if (s.contains(sLine))
                {
                af[0] = true;
                }
            });
            return af[0];
            }

        /**
         * Return count of messages matching {@code sLine}.
         *
         * @return count of messages matching {@code sLine}
         */
        public int matchCount(String sLine)
            {
            int[] nCount = new int[1];
            m_listMessages.forEach(s ->
            {
            if (s.contains(sLine))
                {
                nCount[0]++;
                }
            });
            return nCount[0];
            }

        // ----- data members -----------------------------------------------

        /**
         * Whether to collect log messages.
         */
        public volatile boolean m_enabled = true;

        /**
         * The log messages collected.
         */
        protected List<String> m_listMessages = Collections.synchronizedList(new LinkedList<>());
        }

    // ----- constants and data members -------------------------------------

    /**
     * The path to the cache configuration.
     */
    public final static String FILE_CFG_CACHE = "ecf-cache-config.xml";

    /**
     * LogHandler to enable test to verify log messages.
     */
    public static LogHandler s_logHandler = null;
    }
