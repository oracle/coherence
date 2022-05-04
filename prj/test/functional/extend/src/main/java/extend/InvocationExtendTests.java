/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.RequestTimeoutException;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.ConditionalPut;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.pof.PortablePerson;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* A collection of InvocationService functional tests for Coherence*Extend.
*
* @author jh  2005.11.29
*/
public class InvocationExtendTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public InvocationExtendTests()
        {
        super("client-cache-config-invocation.xml");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("InvocationExtendTests", "extend",
                                                "server-cache-config-invocation.xml");
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("InvocationExtendTests");
        }


    // ----- InvocationService tests ----------------------------------------

    /**
    * Test the behavior of
    * {@link InvocationService#execute(Invocable, Set, InvocationObserver)}.
    */
    @Test
    public void execute()
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(INVOCATION_SERVICE_NAME);
        OperationalContext ctx    = (OperationalContext)
                CacheFactory.getCluster();

        assertNotNull(ctx);
        assertEquals (ctx.getEdition(), 3);
        assertEquals (ctx.getEditionName(), "CE");
        assertEquals (ctx.getFilterMap().size(), 1);
        assertEquals (ctx.getSerializerMap().size(), 2);
        assertNotNull(ctx.getSocketProviderFactory());
        assertNotNull(ctx.getLocalMember());

        try
            {
            service.execute(new TestInvocable(), null, null);
            }
        catch (UnsupportedOperationException e)
            {
            // expected
            return;
            }
        finally
            {
            service.shutdown();
            }
        fail("expected exception");
        }

    /**
    * Test the behavior of {@link InvocationService#query(Invocable, Set)}.
    */
    @Test
    public void query()
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(INVOCATION_SERVICE_NAME);
        OperationalContext ctx    = (OperationalContext)
                CacheFactory.getCluster();

        assertNotNull(ctx);
        assertEquals (ctx.getEdition(), 3);
        assertEquals (ctx.getEditionName(), "CE");
        assertEquals (ctx.getFilterMap().size(), 1);
        assertEquals (ctx.getSerializerMap().size(), 2);
        assertNotNull(ctx.getSocketProviderFactory());
        assertNotNull(ctx.getLocalMember());

        try
            {
            TestInvocable task = new TestInvocable();
            task.setValue(6);
            Map map = service.query(task, null);

            assertTrue(map != null);
            assertTrue(map.size() == 1);

            Object oMember = map.keySet().iterator().next();
            assertTrue(equals(oMember, service.getCluster().getLocalMember()));

            Object oResult = map.values().iterator().next();
            assertTrue(oResult instanceof Integer);
            assertTrue(((Integer) oResult).intValue() == 7);
            }
        finally
            {
            service.shutdown();
            }
        }

    /**
    * Test the behavior of {@link InvocationService#query(Invocable, Set)}
    * with a PriorityTask Invocable that excutes for a period of time longer
    * than the configured task-timeout.
    */
    @Test
    public void queryTimeout()
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(INVOCATION_SERVICE_NAME);
        try
            {
            Invocable task = new TestInvocableTimeout();
            service.query(task, null);
            fail("Expected RequestTimeoutException");
            }
        catch (RequestTimeoutException e)
            {
            // success
            }
        finally
            {
            service.shutdown();
            }
        }

    /**
    * Test POF object with circular references.
    */
    @Test
    public void pofCircularReference()
        {
        CacheService service = (CacheService) getFactory().ensureService("ExtendTcpCacheService");
        NamedCache   cache   = service.ensureCache("dist-extend-direct", null);

        PortablePerson joe  = new PortablePerson("Joe Smith", new Date(78, 4, 25));
        PortablePerson jane = new PortablePerson("Jane Smith", new Date(80, 5, 22));
        joe.setSpouse(jane);
        jane.setSpouse(joe);

        cache.put(1, joe);
        cache.invoke(1, new ConditionalPut(AlwaysFilter.INSTANCE, joe, false));
        service.shutdown();
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


    // ----- inner class: TestInvocableTimeout-------------------------------

    /**
    * Invocable implementation that sleeps for 10 seconds.
    */
    public static class TestInvocableTimeout
            extends AbstractInvocable
            implements PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public TestInvocableTimeout()
            {
            }

        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            out("TestInvocableTimeout sleeping for 10 seconds...");
            long ldtEnd = getSafeTimeMillis() + 10000L;
            do
                {
                try
                    {
                    Thread.sleep(1000L);
                    }
                catch (InterruptedException e)
                    {
                    // ignore
                    }
                }
            while (getSafeTimeMillis() < ldtEnd);
            out("TestInvocableTimeout finished sleeping.");
            }

        // ----- PriorityTask interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public void runCanceled(boolean fAbandoned)
            {
            out("TestInvocableTimeout.runCanceled(" + fAbandoned + ").");
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of the InvocationService used by all test methods.
    */
    public static String INVOCATION_SERVICE_NAME = "ExtendTcpInvocationService";
    }
