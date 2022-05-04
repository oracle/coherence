/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.application.LifecycleEvent.Type;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.annotation.Interceptor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Basic tests for events.
 */
public class LifecycleEventTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Default Constructor.
    */
    public LifecycleEventTests()
        {
        super(CFG_FILE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.management", "all");

        AbstractFunctionalTest._startup();
        }

    @Before
    public void testBefore()
        {
        }

    @After
    public void testAfter()
        {
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that we can modify an insert event.
     */
    @Test
    public void testEvents()
            throws InterruptedException
        {
        ConfigurableCacheFactory ccf         = getFactory();
        LifecycleInterceptor     interceptor = new LifecycleInterceptor();

        InterceptorRegistry registry = ccf.getInterceptorRegistry();
        registry.registerEventInterceptor("lifecycle", interceptor, null);

        //the service monitor is typically set by the ContainerAdapter but this test is running outside of that model
        ccf.activate();
        ccf.dispose();

        registry.unregisterEventInterceptor("lifecycle");

        assertEquals(7, interceptor.m_nState);
        }

    // ----- inner class: LifecycleInterceptor ------------------------------

    /**
     * Test LifeCycle Events
     */
    @Interceptor(identifier = "lifecycle")
    public static class LifecycleInterceptor
            implements EventInterceptor<LifecycleEvent>
        {
        @Override
        public void onEvent(LifecycleEvent event)
            {
            assertThat(event.getConfigurableCacheFactory(), notNullValue());

            m_nState |= event.getType() == Type.ACTIVATING ? 1 : 0;
            m_nState |= event.getType() == Type.ACTIVATED  ? 2 : 0;
            m_nState |= event.getType() == Type.DISPOSING  ? 4 : 0;
            }

        protected int m_nState;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static final String CFG_FILE = "basic-server-cache-config.xml";
    }