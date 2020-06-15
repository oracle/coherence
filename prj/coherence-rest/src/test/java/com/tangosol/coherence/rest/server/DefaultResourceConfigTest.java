/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

import com.tangosol.coherence.rest.DefaultRootResource;
import com.tangosol.coherence.rest.TestResource;

import com.tangosol.coherence.rest.providers.JsonCollectionWriter;
import com.tangosol.coherence.rest.providers.ObjectWriter;
import com.tangosol.coherence.rest.providers.XmlCollectionWriter;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link DefaultResourceConfig}.
 *
 * @author ic  2011.07.07
 */
public class DefaultResourceConfigTest
    {
    @Test
    public void testCtorNoParams()
        {
        DefaultResourceConfig config = new DefaultResourceConfig();
        assertTrue(config.isRegistered(DefaultRootResource.class));
        assertTrue(config.isRegistered(JsonCollectionWriter.class));
        assertTrue(config.isRegistered(XmlCollectionWriter.class));
        assertTrue(config.isRegistered(ObjectWriter.class));
        }

    @Test
    public void testCtor()
        {
        DefaultResourceConfig config = new DefaultResourceConfig("com.tangosol.coherence.rest");
        assertTrue(config.isRegistered(DefaultRootResource.class));
        assertTrue(config.isRegistered(JsonCollectionWriter.class));
        assertTrue(config.isRegistered(XmlCollectionWriter.class));
        assertTrue(config.isRegistered(ObjectWriter.class));
        assertTrue(config.getClasses().contains(TestResource.class));
        }
    }
