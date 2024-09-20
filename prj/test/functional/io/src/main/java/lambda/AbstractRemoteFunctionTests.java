/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.JavaModules;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.invoke.Lambdas.SerializationMode;

import com.tangosol.io.pof.PortableException;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.WrapperException;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.SystemPropertyIsolation;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * AbstractRemoteFunctionTests to ensure lambda serialization functions both with POF
 * enabled and disabled and with dynamic or static lambdas.
 * <p>
 * When dynamic lambdas are disabled, for static lambdas to work, the client
 * test classes must be included in server classpath for static lambdas to
 * deserialize properly.
 *
 * @author hr  2015.06.09
 * @author jf  2020.06.16
 */
abstract public class AbstractRemoteFunctionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@link AbstractRemoteFunctionTests} configured with various parameters.
     *
     * @param fPOF                           use POF serialization if true
     * @param fServerDisableDynamicLambdas  disable dynamic lambda functionality in cache server
     * @param fClientDisableDynamicLambdas  disable dynamic lambda functionality in client
     */
    public AbstractRemoteFunctionTests(boolean fPOF, boolean fServerDisableDynamicLambdas, boolean fClientDisableDynamicLambdas)
        {
        this(fPOF, fServerDisableDynamicLambdas, fClientDisableDynamicLambdas, false);
        }

    /**
     * Construct {@link AbstractRemoteFunctionTests} configured with various parameters.
     *
     * @param fPOF                                use POF serialization if true
     * @param fServerDynamicLambdasDisabled       disable dynamic lambda functionality in cache server
     * @param fClientDynamicLambdasDisabled       disable dynamic lambda functionality in client
     * @param fIncludeTestClassInServerClasspath  when {@code true}, allow static lambdas to work by including this class in server classpath.
     */
    public AbstractRemoteFunctionTests(boolean fPOF, boolean fServerDynamicLambdasDisabled, boolean fClientDynamicLambdasDisabled, boolean fIncludeTestClassInServerClasspath)
        {
        m_fPOF                               = fPOF;
        m_fServerDynamicLambdasDisabled      = fServerDynamicLambdasDisabled;
        m_fClientDynamicLambdaDisabled       = fClientDynamicLambdasDisabled;
        m_fIncludeTestClassInServerClasspath = fIncludeTestClassInServerClasspath;
        m_propsForServer                     = new Properties();

        if (m_fPOF)
            {
            m_propsForServer.setProperty("coherence.pof.enabled", "true");
            System.setProperty("coherence.pof.enabled", "true");
            }

        m_propsForServer.setProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                m_fServerDynamicLambdasDisabled ? SerializationMode.STATIC.name() : SerializationMode.DYNAMIC.name());

        System.setProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
            m_fClientDynamicLambdaDisabled ? SerializationMode.STATIC.name() : SerializationMode.DYNAMIC.name());


        System.out.println("RemoteFunctionTest serializer=" + (m_fPOF ? "pof" : "java") +
            " client " + Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY + "=" + System.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY) +
            " server "  + Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY + "=" + m_propsForServer.get(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY) +
            " allowStaticLambda=" + m_fIncludeTestClassInServerClasspath);
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

        props.setProperty("coherence.nameservice.address",
            LocalPlatform.get().getLoopbackAddress().getHostAddress());

        // assume that this process should be storage disabled
        props.setProperty("coherence.distributed.localstorage",
                "false");
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testSimple()
        {
        // strip out some characters since exceeded 66 character limit for role on some test
        String sServerName       = this.getClass().getName().replace("lambda.Remote", "Remote").replace("Tests", "") + "_Simple";
        String sClassPath        = m_fIncludeTestClassInServerClasspath ? null : createClassPathExcludingTestClasses();
        String sKindLambdaInvoke = Lambdas.isStaticLambdas() ? "static" : "dynamic";
        JavaModules module       = m_fIncludeTestClassInServerClasspath ? JavaModules.automatic() : JavaModules.automatic().excluding("io").adding("com.oracle.coherence.testing").withClassPath(new ClassPath(sClassPath));

        if (sClassPath != null)
            {
            System.setProperty("java.class.path", sClassPath);
            }
        startCacheServer(sServerName, "io", getCacheConfigPath(), m_propsForServer, true, sClassPath, module);

        assertEquals("assert client side " + Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
             Lambdas.isStaticLambdas(), m_fClientDynamicLambdaDisabled);
        try
            {
            NamedCache cache = CacheFactory.getCache("foo");

            cache.put(1, 1);

            cache.invoke(1, entry -> entry.setValue((Integer) entry.getValue() + 1));
            assertFalse(sKindLambdaInvoke + "lambda invocation must not work", m_fServerDynamicLambdasDisabled && (m_fServerDynamicLambdasDisabled != m_fIncludeTestClassInServerClasspath));

            assertEquals(2, cache.get(1));
            }
        catch (WrapperException e)
            {
            e.printStackTrace();
            assertTrue(sKindLambdaInvoke + " lamdba invocation failed unexpectedly: " + e, m_fServerDynamicLambdasDisabled && !m_fIncludeTestClassInServerClasspath);
            assertFalse(m_fIncludeTestClassInServerClasspath);
            }
        catch (PortableException e)
            {
            e.printStackTrace();
            assertTrue(m_fPOF == true && m_fServerDynamicLambdasDisabled && !m_fIncludeTestClassInServerClasspath);
            assertFalse(m_fIncludeTestClassInServerClasspath);
            }
        finally
            {
            stopCacheServer(sServerName);
            }
        }

    @Test
    public void testWithArgs()
        {
        // strip out some characters since exceeded 66 character limit for role on some test
        String sServerName       = this.getClass().getName().replace("lambda.Remote", "Remote").replace("Tests", "") +  "_WithArgs";
        String sClassPath        = m_fIncludeTestClassInServerClasspath ? null : createClassPathExcludingTestClasses();
        String sKindLambdaInvoke = Lambdas.isStaticLambdas()  ? "static" : "dynamic";
        JavaModules module       = m_fIncludeTestClassInServerClasspath ? JavaModules.automatic() : JavaModules.automatic().excluding("io").adding("com.oracle.coherence.testing").withClassPath(new ClassPath(sClassPath));

        if (sClassPath != null)
            {
            System.setProperty("java.class.path", sClassPath);
            }
        startCacheServer(sServerName, "io", getCacheConfigPath(), m_propsForServer, true, sClassPath, module);
        try
            {
            NamedCache cache = CacheFactory.getCache("foo");
            Integer    NInc  = 2;

            cache.put(1, 1);
            cache.invoke(1, entry -> entry.setValue((Integer) entry.getValue() + NInc));

            assertFalse(sKindLambdaInvoke + " lambda invocation must not work", m_fServerDynamicLambdasDisabled && (m_fServerDynamicLambdasDisabled != m_fIncludeTestClassInServerClasspath));
            assertEquals(3, cache.get(1));
            }
        catch (WrapperException e)
            {
            e.printStackTrace();
            assertTrue(sKindLambdaInvoke + " lamdba invocation failed unexpectedly: " + e, m_fServerDynamicLambdasDisabled && !m_fIncludeTestClassInServerClasspath);
            }
        catch (PortableException e)
            {
            e.printStackTrace();
            assertTrue(m_fPOF == true && m_fServerDynamicLambdasDisabled && !m_fIncludeTestClassInServerClasspath);
            }
        finally
            {
            stopCacheServer(sServerName);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a class path that does not include any of the test classes.
     *
     * @return a class path that does not include any of the test classes
     */
    protected static String createClassPathExcludingTestClasses()
        {
        String sSep;
        if ("\\".endsWith(File.separator))
            {
            sSep = "\\\\";
            }
        else
            {
            sSep = File.separator;
            }
        String sPattern = String.format(".*%sio%starget%s.*", sSep, sSep, sSep);
        return ClassPath.automatic().excluding(sPattern).toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Whether POF is enabled.
     */
    protected boolean m_fPOF;

    /**
     * {@code true} when dynamic lambdas are disabled on server side.
     */
    protected boolean m_fServerDynamicLambdasDisabled;

    /**
     * {@code true} when dynamic lambdas disabled on client side.
     */
    protected boolean m_fClientDynamicLambdaDisabled;

    /**
     * Cache server properties
     */
    protected Properties m_propsForServer;

    /**
     * If {@code true}, enables RemoteFunctionTest classes to be on server classpath and validate static lambdas are working.
     * If {@code false}, validates exception thrown when not able to find capturing class for static lambda.
     */
    protected final boolean m_fIncludeTestClassInServerClasspath;

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
