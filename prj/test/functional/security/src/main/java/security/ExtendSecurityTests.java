/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.messaging.Channel;

import com.tangosol.net.security.Security;
import com.tangosol.net.security.SimpleHandler;

import com.tangosol.coherence.component.net.extend.RemoteService;
import com.tangosol.coherence.component.util.SafeService;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.Properties;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.Subject;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* Simple test of authorizing a Coherence*Extend client.
*
* @author jh  2009.11.23
*/
public class ExtendSecurityTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ExtendSecurityTests()
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
        System.setProperty("coherence.security.log", "true");

        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("test.server.classname",
                "security.SubjectCacheServer");

        CoherenceClusterMember clusterMember = startCacheServer("ExtendSecurityTests", "security",
                "server-cache-config.xml", props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("TcpProxyService"), is(true));
        }

    /**
    * Stop the cache server for this test class.
    */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("ExtendSecurityTests");
        }

    /**
    * Test if the identity gets passed by Extend to the proxy.
    */
    @Test
    public void testIdentityPassing()
        {
        final Subject subject0 = loginJAAS("manager", "private");
        final Subject subject1 = loginJAAS("worker",  "private");

        assertTrue(subject0 != null);
        assertTrue(subject1 != null);
        try
            {
            Subject.doAs(subject0, createPrivilegedExceptionAction(CACHE_SERVICE_NAME, subject0));
            }
        catch (PrivilegedActionException e)
            {
            // failed if exception
            e.printStackTrace();
            fail("should not be an exception");
            }

        try
            {
            Subject.doAs(subject1, createPrivilegedExceptionAction(CACHE_SERVICE_NAME, subject1));
            }
        catch (PrivilegedActionException e)
            {
            // failed if exception
            e.printStackTrace();
            fail("should not be an exception");
            }

        try
            {
            Subject.doAs(subject0, createPrivilegedExceptionAction(INVOCATION_SERVICE_NAME, subject0));
            }
        catch (PrivilegedActionException e)
            {
            // failed if exception
            e.printStackTrace();
            fail("should not be an exception");
            }

        try
            {
            Subject.doAs(subject1, createPrivilegedExceptionAction(INVOCATION_SERVICE_NAME, subject1));
            }
        catch (PrivilegedActionException e)
            {
            // failed if exception
            e.printStackTrace();
            fail("should not be an exception");
            }

        try
            {
            Subject.doAs(subject0, (PrivilegedAction) () -> getNamedCache("dist-test").put("key", "value"));
            }
        catch (Exception e)
            {
            // failed if security exception
            e.printStackTrace();
            fail("should not be a security exception");
            }

        Exception e = null;
        try
            {
            Subject.doAs(subject1, (PrivilegedAction) () -> getNamedCache("dist-test").put("key", "value"));
            }
        catch (Exception ee)
            {
            // should get security exception
            e = ee;
            }
        if (e == null)
            {
            fail("expected security exception");
            }

        e = null;

       try
            {
            // Also try JAAS
            Subject.doAs(subject1, (PrivilegedAction) () -> getNamedCache("dist-test").put("key", "value"));
            }
        catch (Exception ee)
            {
            // should get security exception
            e = ee;
            }
        if (e == null)
            {
            fail("expected security exception");
            }

        try
            {
            Subject.doAs(subject0, (PrivilegedAction) () ->
                {
                InvocationService service = (InvocationService) getFactory()
                            .ensureService(INVOCATION_SERVICE_NAME);
                Map map = service.query(new TestInvocable(), null);
                assertEquals(1, map.size());

                return null;
                });
            }
        catch (Exception ee)
            {
            // failed if security exception
            ee.printStackTrace();
            fail("should not be a security exception");
            }

        e = null;
        try
            {
            Subject.doAs(subject1, (PrivilegedAction) () ->
                {
                InvocationService service = (InvocationService) getFactory()
                            .ensureService(INVOCATION_SERVICE_NAME);
                service.query(new TestInvocable(), null);

                return null;
                });
            }
        catch (Exception ee)
            {
            // should get security exception
            e = ee;
            }
        if (e == null)
            {
            fail("expected security exception");
            }

        e = null;
        try
            {
            Subject.doAs(null, (PrivilegedAction) () ->
                {
                InvocationService service = (InvocationService) getFactory()
                            .ensureService(INVOCATION_SERVICE_NAME);
                service.query(new TestInvocable(), null);

                return null;
                });
            }
        catch (Exception ee)
            {
            // should get security exception
            e = ee;
            }
        if (e == null)
            {
            fail("expected security exception");
            }
        }

    /**
    * Test the behavior of subject scoped RemoteInvocationService
    */
    @Test
    public void testSubjectServiceScope()
        {
        InvocationService service00 = (InvocationService) Subject.doAs(
                TestHelper.SUBJECT_ADMIN,
                (PrivilegedAction) () -> getFactory().ensureService(INVOCATION_SERVICE_NAME));

        InvocationService service01 = (InvocationService) Subject.doAs(
                TestHelper.SUBJECT_USER,
                (PrivilegedAction) () -> getFactory().ensureService(INVOCATION_SERVICE_NAME));

        // none of the service references should be the same (created with
        // different subjects)
        assertTrue(service00 != service01);
        }

    /**
    * Test if the remote cache reference is scoped by subject.
    */
    @Test
    public void testSubjectCacheScoping()
        {
        Subject subjectMgr = Security.login("manager", "private".toCharArray());

        NamedCache cache00 = (NamedCache) Subject.doAs(subjectMgr,
                (PrivilegedAction) () -> getFactory().ensureCache("dist-test", null));

        Subject subjectWkr = Security.login("worker", "private".toCharArray());

        NamedCache cache01 = (NamedCache) Subject.doAs(subjectWkr,
                (PrivilegedAction) () -> getFactory().ensureCache("dist-test", null));

        // none of the named caches should be the same (created with
        // different subjects)
        assertTrue(cache00 != cache01);
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Return a PrivilegedExceptionAction implementation with specified service name and Subject.
     *
     * @param sService  the service name
     * @param subject   the Subject
     *
     * @return a PrivilegedExceptionAction implementation
     */
    public PrivilegedExceptionAction createPrivilegedExceptionAction(String sService, Subject subject)
        {
        return (PrivilegedExceptionAction) () ->
            {
            SafeService serviceSafe = (SafeService) getFactory().ensureService(sService);
            RemoteService serviceRemote = (RemoteService) serviceSafe.getService();

            Channel channel = serviceRemote.getChannel();
            assertEquals(subject, channel.getSubject());

            channel = channel.getConnection().getChannel(0);
            assertEquals(subject, channel.getSubject());

            return null;
            };
        }

    /**
    * Login using JAAS.
    *
    * @param sUser      the user name
    * @param sPassword  the password
    *
    * @return the authenticated subject
    */
    public static Subject loginJAAS(String sUser, String sPassword)
        {
        LoginContext context = null;
        try
            {
            context = new LoginContext("Coherence",
                   new SimpleHandler(sUser, sPassword.toCharArray()));
            }
        catch (LoginException le)
            {
            fail("login context failed: " + le);
            }

        try
            {
            context.login();
            }
        catch (Exception ee)
            {
            fail("login failed: " + ee);
            }
        return context.getSubject();
        }


    // ----- inner class: TestInvocable -------------------------------------

    /**
    * Invocable implementation that increments and returns a given integer.
    */
    public static class TestInvocable
            implements Invocable, PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public TestInvocable()
            {
            }

        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void init(InvocationService service)
            {
            assertTrue(service.getInfo().getServiceType()
                    .equals(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            if (m_service != null)
                {
                m_nValue++;
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getResult()
            {
            return m_nValue;
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_nValue = in.readInt(0);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, m_nValue);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Set the integer value to increment.
        *
        * @param nValue  the value to increment
        */
        public void setValue(int nValue)
            {
            m_nValue = nValue;
            }

        // ----- data members ---------------------------------------------

        /**
        * The integer value to increment.
        */
        private int m_nValue;

        /**
        * The InvocationService that is executing this Invocable.
        */
        private transient InvocationService m_service;
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of the InvocationService used by all test methods.
    */
    public static String CACHE_SERVICE_NAME = "ExtendTcpCacheService";

    /**
    * The name of the InvocationService used by all test methods.
    */
    public static String INVOCATION_SERVICE_NAME = "ExtendTcpInvocationService";
    }
