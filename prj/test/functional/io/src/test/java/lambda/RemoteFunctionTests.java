/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package lambda;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import common.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URL;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * RemoteFunctionTests to ensure lambda serialization functions both with POF
 * enabled and disabled.
 *
 * @author hr  2015.06.09
 */
@RunWith(Parameterized.class)
public class RemoteFunctionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public RemoteFunctionTests(boolean fPOF)
        {
        m_fPOF = fPOF;
        }

    // ----- public constant helpers ----------------------------------------

    @BeforeClass
    public static void _startup()
        {
        // generate unique IP addresses and ports for this test
        Properties props = System.getProperties();

        props.setProperty("java.net.preferIPv4Stack", "true");
        props.setProperty("test.extend.port",         String.valueOf(getAvailablePorts().next()));
        props.setProperty("test.multicast.address",   generateUniqueAddress(true));

        // use INADDR_ANY available ports for multicast
        props.setProperty("test.multicast.port",
            String.valueOf(LocalPlatform.get().getAvailablePorts().next()));

        props.setProperty("tangosol.coherence.nameservice.address",
            LocalPlatform.get().getLoopbackAddress().getHostAddress());

        // assume that this process should be storage disabled
        props.setProperty("tangosol.coherence.distributed.localstorage",
                "false");
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testSimple()
        {
        Properties props = new Properties();
        if (m_fPOF)
            {
            props.setProperty("coherence.pof.enabled", "true");
            System.getProperties().putAll(props);
            }

        startCacheServer("RemoteFunctionTests-Simple-1", "io", null, props, true, createClassPath());
        try
            {
            NamedCache cache = CacheFactory.getCache("foo");

            cache.put(1, 1);

            cache.invoke(1, entry -> entry.setValue((Integer) entry.getValue() + 1));

            assertEquals(2, cache.get(1));
            }
        finally
            {
            stopCacheServer("RemoteFunctionTests-Simple-1");
            }
        }

    @Test
    public void testWithArgs()
        {
        Properties props = new Properties();
        if (m_fPOF)
            {
            props.setProperty("coherence.pof.enabled", "true");
            System.getProperties().putAll(props);
            }

        startCacheServer("RemoteFunctionTests-WithArgs-1", "io", null, props, true, createClassPath());
        try
            {
            NamedCache cache = CacheFactory.getCache("foo");
            Integer    NInc  = 2;

            cache.put(1, 1);

            cache.invoke(1, entry -> entry.setValue((Integer) entry.getValue() + NInc));

            assertEquals(3, cache.get(1));
            }
        finally
            {
            stopCacheServer("RemoteFunctionTests-WithArgs-1");
            }
        }

    // ----- helpers --------------------------------------------------------

    @Parameterized.Parameters(name = "pof={0}")
    public static Collection<Object[]> args()
        {
        return Arrays.asList(new Object[][]{{true}, {false}});
        }

    /**
     * Return a class path that does not include the code source of this class.
     *
     * @return a class path that does not include the code source of this class
     */
    protected static String createClassPath()
        {
        ProtectionDomain domain  = RemoteFunctionTests.class.getProtectionDomain();
        CodeSource       codeSrc = domain  == null ? null : domain.getCodeSource();
        URL              url     = codeSrc == null ? null : codeSrc.getLocation();

        String sClassPath = System.getProperty("java.class.path");
        String sPath      = url.getFile();
               sPath      = ':' + (sPath.charAt(sPath.length() - 1) == '/'
               ? sPath.substring(0, sPath.length() - 1) : sPath);

        return sClassPath.replace(sPath, "");
        }

    // ----- data members ---------------------------------------------------

    /**
     * Whether POF is enabled.
     */
    protected boolean m_fPOF;
    }
