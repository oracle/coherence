/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import java.util.Collections;

import com.tangosol.net.CacheService;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for a {@link AbstractServiceScheme}.
 *
 * @author pfm  2012.06.28
 */
public class ServiceSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ConcreteScheme scheme = new ConcreteScheme();

        assertFalse(scheme.isAutoStart());
        assertEquals(null, scheme.getScopeName());
        assertEquals(null, scheme.getXml());
        assertEquals(scheme.getServiceType(), scheme.getServiceName());
        assertEquals(scheme.getServiceType(), scheme.getScopedServiceName());
        assertEquals(scheme, scheme.getServiceBuilder());
        assertEquals(Collections.EMPTY_LIST, scheme.getEventInterceptorBuilders());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ConcreteScheme scheme = new ConcreteScheme();

        scheme.setAutoStart(true);
        assertTrue(scheme.isAutoStart());

        scheme.setSchemeName("foo");
        assertEquals("foo", scheme.getSchemeName());

        scheme.setScopeName("scope");
        assertEquals("scope", scheme.getScopeName());
        assertEquals("scope:"+ scheme.getServiceName(), scheme.getScopedServiceName());

        XmlElement xml = new SimpleElement();
        scheme.setXml(xml);
        assertEquals(xml, scheme.getXml());
        }

    // ----- inner class ConcreteClass --------------------------------------

    /**
     * Concrete class for testing.
     */
    public static class ConcreteScheme
            extends AbstractServiceScheme
        {
        @Override
        public String getServiceType()
            {
            return CacheService.TYPE_LOCAL;
            }

        @Override
        public boolean isRunningClusterNeeded()
            {
            return false;
            }
        }
    }
