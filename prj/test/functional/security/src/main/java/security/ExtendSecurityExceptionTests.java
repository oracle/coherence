/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.io.pof.PortableException;

import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.coherence.component.net.extend.RemoteService;
import com.tangosol.coherence.component.util.SafeService;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.oracle.coherence.testing.util.GetExtendPort;

import java.security.PrivilegedExceptionAction;

import java.util.Properties;

import javax.security.auth.Subject;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* Simple test of SecurityException handling in Coherence*Extend.
*
* @author lh  2013.08.20
*/
public class ExtendSecurityExceptionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ExtendSecurityExceptionTests()
        {
        super("client-cache-config.xml");
        }


    // ----- ExtendSecurityTests methods ------------------------------------

    /**
     * Start the cache server for this test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.override", "security-coherence-override.xml");
        System.setProperty("java.security.auth.login.config", "login.config");
        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("test.server.classname",
                          "security.SubjectCacheServer");

        CoherenceClusterMember clusterMember = startCacheServer("ExtendSecurityExceptionTests", "security",
                                                       "noaccess-server-cache-config.xml", props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("noAccessTcpProxyService"), is(true));

        Integer extendPort = clusterMember.invoke(new GetExtendPort("noAccessTcpProxyService"));
        System.setProperty("test.extend.port", String.valueOf(extendPort));
        }

    /**
    * Stop the cache server for this test class.
    */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("ExtendSecurityExceptionTests");
        }

    /**
    * Test if the identity gets passed by Extend to the proxy.
    */
    @Test
    public void testIdentityPassing()
        {
        final Subject subject = ExtendSecurityTests.loginJAAS("manager", "private");

        assertTrue(subject != null);
        Exception e = null;

        try
            {
            Subject.doAs(subject, new PrivilegedExceptionAction()
                {
                public Object run()
                        throws Exception
                    {
                    SafeService   serviceSafe = (SafeService) getFactory().ensureService(CACHE_SERVICE_NAME);
                    RemoteService serviceReal = (RemoteService) serviceSafe.getService();

                    return null;
                    }
                });
            }
        catch (Exception ee)
            {
            if (ee instanceof ConnectionException
                    && ee.getCause() instanceof PortableException)
                {
                PortableException pe = (PortableException) ee.getCause();
                if (pe.getName().indexOf("SecurityException") > 0)
                    {
                    e = ee;
                    }
                }
            }
        if (e == null)
            {
            fail("expected security exception");
            }
        }

    // ----- constants ------------------------------------------------------

    /**
    * The name of the InvocationService used by all test methods.
    */
    public static String CACHE_SERVICE_NAME = "ExtendTcpCacheService";
    }
