/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting.common;

import org.junit.Test;

import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Class description
 *
 * @version        Enter version here..., 13/11/26
 * @author         Enter your name here...
 */
public class JCacheIdentifierTests
    {
    /**
     * Method description
     */
    @Test
    public void testInternalExternalForm()
        {
        JCacheIdentifier[] ids = {new JCacheIdentifier("cacheMgrURI", "cacheName"),
                                  new JCacheIdentifier("", "noCacheMgrURIJustCacheName"),
                                  new JCacheIdentifier("/dir/subdir/cache-config.xml", "validCacheName3")};

        for (JCacheIdentifier id : ids)
            {
            JCacheIdentifier fromExternal = new JCacheIdentifier(id.getCanonicalCacheName());

            assertEquals(id.getCanonicalCacheName(), fromExternal.getCanonicalCacheName());
            assertEquals(id.getCacheManagerURI(), fromExternal.getCacheManagerURI());
            assertEquals(id.getName(), fromExternal.getName());
            }
        }

    /**
     * Method description
     */
    @Test
    public void testInvalidExternalFormat()
        {
        String[] invalidExternalFormats = {"missingCacheMgrURICacheNameSeparator"};

        for (String invalidFormat : invalidExternalFormats)
            {
            try
                {
                new JCacheIdentifier(invalidFormat);
                assertTrue("expected an exception to be thrown for invalid format", false);
                }
            catch (Throwable e)
                {
                // expected path for invalid external format
                }
            }
        }
    }
