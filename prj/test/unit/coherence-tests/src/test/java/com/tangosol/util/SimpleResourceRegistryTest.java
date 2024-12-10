/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import org.junit.Assert;
import org.junit.Test;

import static com.tangosol.util.BuilderHelper.using;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

/**
 * Tests for {@link SimpleResourceRegistry}.
 *
 * @author pp  2012.02.29
 */
public class SimpleResourceRegistryTest
    {
    /**
     * Resource registration and unregistration test.
     */
    @Test
    public void testRegisterResource()
        {
        SimpleResourceRegistry registry   = new SimpleResourceRegistry();
        StringBuilder          sbResource = new StringBuilder("resource");

        String sResourceName = registry.registerResource(StringBuilder.class, "StringBuilderResource", sbResource);

        assertEquals("StringBuilderResource", sResourceName);
        
        assertEquals(sbResource, registry.getResource(StringBuilder.class, "StringBuilderResource"));

        // registering the same resource under the same name is allowed 
        //(it's ignored when the resources are the same)
        sResourceName = registry.registerResource(StringBuilder.class, "StringBuilderResource", sbResource);

        assertEquals("StringBuilderResource", sResourceName);
        
        // registering a different resource under the same name should fail
        boolean fException = false;

        try
            {
            registry.registerResource(StringBuilder.class, "StringBuilderResource", new StringBuilder());
            }
        catch (IllegalArgumentException e)
            {
            fException = true;
            }

        assertTrue("Expected IllegalArgumentException", fException);

        registry.unregisterResource(StringBuilder.class, "StringBuilderResource");

        assertNull(registry.getResource(StringBuilder.class, "StringBuilderResource"));
        }

    /**
     * Test of resource registration with lifecycle observer.
     */
    @Test
    public void testRegisterResourceWithLifecycle()
        {
        SimpleResourceRegistry registry   = new SimpleResourceRegistry();
        StringBuilder          sbResource = new StringBuilder("resource");

        ResourceRegistry.ResourceLifecycleObserver<StringBuilder> observer =
            new ResourceRegistry.ResourceLifecycleObserver<StringBuilder>()
            {
            @Override
            public void onRelease(StringBuilder stringBuilder)
                {
                stringBuilder.append("disposed");
                }
            };

        registry.registerResource(StringBuilder.class, "StringBuilderResource", using(sbResource),
                                  RegistrationBehavior.FAIL, observer);

        assertEquals(sbResource, registry.getResource(StringBuilder.class, "StringBuilderResource"));

        registry.dispose();

        assertNull(registry.getResource(StringBuilder.class, "StringBuilderResource"));

        assertTrue("Expected string 'disposed'", sbResource.toString().contains("disposed"));
        }

    /**
     * Ensure {@link RegistrationBehavior} behaves as expected.
     */
    @Test
    public void testRegisterResourceBehaviors()
    {
        SimpleResourceRegistry registry     = new SimpleResourceRegistry();
        String resource1 = "Hello World";
        String resource2 = "Gudday World";
        
        //test FAIL behavior
        String sResourceName = registry.registerResource(String.class, using(resource1), RegistrationBehavior.FAIL, null);
        assertEquals(String.class.getName(), sResourceName);
        assertEquals(resource1, registry.getResource(String.class));
        
        try 
        {
        	registry.registerResource(String.class, using(resource2), RegistrationBehavior.FAIL, null);
        	Assert.fail();
        	
        } catch (IllegalArgumentException e)
        {
        	//expected
        }
        
        //test IGNORE behavior
        sResourceName = registry.registerResource(String.class, using(resource2), RegistrationBehavior.IGNORE, null);
        assertEquals(String.class.getName(), sResourceName);
        assertEquals(resource1, registry.getResource(String.class));

        //test REPLACE behavior
        sResourceName = registry.registerResource(String.class, using(resource2), RegistrationBehavior.REPLACE, null);
        assertEquals(String.class.getName(), sResourceName);
        assertEquals(resource2, registry.getResource(String.class));
        
        //test ALWAYS behavior
        sResourceName = registry.registerResource(String.class, using(resource1), RegistrationBehavior.ALWAYS, null);
        assertEquals(String.class.getName() + "-1", sResourceName);
        assertEquals(resource1, registry.getResource(String.class, sResourceName));
    }
    
    
    /**
     * Test of resource unregistration where an exception is thrown.
     */
    @Test
    public void testUnregistrationWithException()
        {
        SimpleResourceRegistry registry     = new SimpleResourceRegistry();
        Combustible[]          aCombustible = new Combustible[50];
        Random                 random       = Base.getRandom();

        for (int i = 0; i < aCombustible.length; i++)
            {
            aCombustible[i] = new Combustible(random.nextBoolean());
            registry.registerResource(Combustible.class, "Combustible " + String.valueOf(i), aCombustible[i]);
            }

        registry.dispose();

        for (int i = 0; i < aCombustible.length; i++)
            {
            aCombustible[i].assertState();
            }
        }

    /**
     * {@link com.oracle.coherence.common.base.Disposable} implementation that may be
     * configured to throw a {@link RuntimeException} upon {@link #dispose()}
     * invocation.
     */
    public static final class Combustible
            implements com.oracle.coherence.common.base.Disposable
        {
        /**
         * Construct a Combustible.
         *
         * @param fBlowsUp if true, throws a {@link RuntimeException}
         *                 upon {@link #dispose()} invocation
         */
        public Combustible(boolean fBlowsUp)
            {
            m_fBlowsUp        = fBlowsUp;
            m_fDisposeInvoked = false;
            m_fDisposed       = false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose()
            {
            m_fDisposeInvoked = true;

            if (m_fBlowsUp)
                {
                throw new RuntimeException("kaboom");
                }

            m_fDisposed = true;
            }

        /**
         * Assert the state of this object. Assertion will fail if
         * <ul>
         *     <li>dispose() was not invoked</li>
         *     <li>dispose() was invoked but an exception was not thrown
         *     if constructed with true</li>
         *     <li>dispose() was invoked but an exception was thrown
         *     if constructed with false</li>
         * </ul>
         */
        public void assertState()
            {
            assertTrue("dispose() was not invoked", m_fDisposeInvoked);

            if (m_fBlowsUp)
                {
                assertFalse("dispose() did not throw expected exception", m_fDisposed);
                }
            else
                {
                assertTrue("dispose() threw unexpected exception", m_fDisposed);
                }
            }

        /**
         * If true, throws {@link RuntimeException} upon disposal.
         */
        private boolean m_fBlowsUp;

        /**
         * If true, dispose was invoked.
         */
        private boolean m_fDisposeInvoked;

        /**
         * If true, dispose was invoked and no exception was thrown.
         */
        private boolean m_fDisposed;
        }
    }
