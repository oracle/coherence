/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.DocumentProcessor.Dependencies;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The unit tests for using cookies with an {@link ProcessingContext}.
 *
 * @author bo
 * @author dr
 */
public class ProcessingContextCookieTest
    {
    /**
     * Ensures that cookies defined in a context exist.
     *
     * @throws ConfigurationException Should the configuration be invalid
     */
    @Test
    public void testSimpleCookies()
            throws ConfigurationException
        {
        Dependencies             dependencies = new DocumentProcessor.DefaultDependencies();
        DefaultProcessingContext ctx          = new DefaultProcessingContext(dependencies);

        ctx.addCookie(String.class, "message", "hello world");
        
        assertEquals(ctx.getCookie(String.class, "message"), "hello world");
        assertTrue(ctx.getCookie(String.class, "other-message") == null);
        }

    /**
     * Ensures that cookies defined in a context exist.
     *
     * @throws ConfigurationException Should the configuration be invalid
     */
    @Test
    public void testNestedCookies()
            throws ConfigurationException
        {
        Dependencies             dependencies = new DocumentProcessor.DefaultDependencies();
        DefaultProcessingContext ctxOuter     = new DefaultProcessingContext(dependencies);
        DefaultProcessingContext ctxInner     = new DefaultProcessingContext(ctxOuter, null);

        ctxOuter.addCookie(String.class, "message", "hello world");
        ctxOuter.addCookie(String.class, "super-message", "bonjour world");

        assertEquals(ctxOuter.getCookie(String.class, "message"), "hello world");
        assertTrue(ctxOuter.getCookie(String.class, "other-message") == null);

        ctxInner.addCookie(String.class, "message", "gudday world");
        ctxInner.addCookie(String.class, "other-message", "awesomeness");

        assertEquals(ctxInner.getCookie(String.class, "message"), "gudday world");
        assertEquals(ctxInner.getCookie(String.class, "other-message"), "awesomeness");
        assertEquals(ctxInner.getCookie(String.class, "super-message"), "bonjour world");
        assertEquals(ctxOuter.getCookie(String.class, "message"), "hello world");
        assertTrue(ctxOuter.getCookie(String.class, "other-message") == null);
        }
    }
