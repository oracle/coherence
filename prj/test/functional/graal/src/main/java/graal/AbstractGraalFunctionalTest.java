/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package graal;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.NamedCache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import graal.pojo.LorCharacter;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

/**
* A base class for all Graal related functional tests.
*
* @author mk 2019.08.07
*/
@RunWith(Parameterized.class)
public abstract class AbstractGraalFunctionalTest
        extends AbstractFunctionalTest
    {
    // ----- constructors -----------------------------------------------------

    /**
    * Create a new AbstractGraalFunctionalTest that will use the specified
    * serializer in test methods.
    *
    * @param sSerializer the serializer name
    */
    public AbstractGraalFunctionalTest(String sSerializer)
        {
        super();

        m_sCache = CACHE_NAME;
        m_sSerializer = sSerializer;
        m_sTestClassName = getClass().getSimpleName();
        }

    // ----- helper methods ---------------------------------------------------

    @Parameterized.Parameters(name = "serializer={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]{ {"pof"}, {"java"} });
        }

    /**
     * Return the cache used in all test methods.
     *
     * @return the test cache
     */
    protected NamedCache<String, LorCharacter> getNamedCache()
        {
        return getNamedCache(getCacheName());
        }

    // ----- accessors --------------------------------------------------------

    /**
    * Return the name of the cache used in all test methods.
    *
    * @return the name of the cache used in all test methods
    */
    protected String getCacheName()
        {
        return m_sCache;
        }

    // ----- test lifecycle ---------------------------------------------------

    /**
     * Initialize the test class.
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @BeforeClass
    public static void _startup()
        {
        setupProps();
        }

    /**
     * Initialize the test class.
     */
    @Before
    public void beforeTest()
        {
        if (!m_fServerStarted)
            {
            Properties props = new Properties();
            if (m_sSerializer.equals("pof"))
                {
                props.put("coherence.pof.enabled", "true");
                props.put("coherence.pof.config", "pof-config.xml");

                System.setProperty("coherence.pof.enabled", "true");
                System.setProperty("coherence.pof.config", "pof-config.xml");
                }

            props.put("coherence.distributed.localstorage", "true");

            if (System.getenv(JVM_EXTRA_OPTS) != null)
                {
                System.setProperty("test.jvm.options", System.getenv(JVM_EXTRA_OPTS));
                }

            CoherenceClusterMember m1 = startCacheServer(
                    getServerName(1),"functional/graal",
                    "coherence-cache-config.xml", props);

            CoherenceClusterMember m2 = startCacheServer(
                    getServerName(2),"functional/graal",
                    "coherence-cache-config.xml", props);

            waitForServer(m1);
            waitForServer(m2);

            m_fServerStarted = true;
            }

        populateCache();
        }

    /**
     * Helper class to get the test server name.
     *
     * @param num  the server number
     *
     * @return the test server name
     */
    private static String getServerName(int num)
        {
        return "Graal-" + m_sTestClassName + "-" + m_sSerializer + "-Server-" + num;
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer(getServerName(1));
        stopCacheServer(getServerName(2));
        }

    public void populateCache()
        {
        clearAndPopulateCacheWithLorEntries();
        }

    // ----- helpers ----------------------------------------------------------

    protected void clearAndPopulateCacheWithLorEntries()
        {
        getNamedCache().clear();
        for (LorCharacter c : aLorChars)
            {
            getNamedCache().put(c.getName(), c);
            }
        }

    // ----- static members ---------------------------------------------------

    protected static final String CACHE_NAME = "script-test-cache";

    protected static LorCharacter[] aLorChars = new LorCharacter[] {
            new LorCharacter("Bilbo", 111, "male", new String[] {"burglaring", "pipe smoking"}),
            new LorCharacter("Frodo", 33, "male", new String[] {"long hikes"}),
            new LorCharacter("Aragon", 87, "male", new String[] {"pipe smoking", "hitting elvish woman"}),
            new LorCharacter("Galadriel", 8372, "female", new String[] {"dwarves"})
    };

    // ----- data members -----------------------------------------------------

    /**
     * The name of the cache used in all test methods.
     */
    protected final String m_sCache;

    /**
     * The serializer name.
     */
    private static String m_sSerializer;

    /**
     * The current test class name. Used for naming test log output files.
     */
    private static String m_sTestClassName;

    /**
     * Flag indicating if server has been started. Used to prevent server
     * starts between tests.
     */
    private boolean m_fServerStarted = false;
    }
