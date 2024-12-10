/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.util.Resources;
import com.tangosol.util.WrapperException;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.config.ConfigurationException;
import com.tangosol.dev.compiler.SyntaxException;

import com.tangosol.io.FileHelper;
import com.tangosol.net.CacheFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;
/**
 * <pre>
 * The {@link CacheConfigurationTests} class contains multiple tests that use
 * cache configurations files with different encodings such as:
 * 1) UTF-8 with BOM
 * 2) UTF-8 without BOM
 * 3) ANSI or ISO-8859-1
 * </pre>
 */
public class CacheConfigurationTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
     public CacheConfigurationTests()
         {
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
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void _shutdown()
        {
        // no-op since each test shutdowns the cluster in a finally block
        }

    // ----- test methods ---------------------------------------------------

    /**
     * This test utilizes the cache config xml which is encoded in UTF-8 and includes a BOM character
     */
    @Test
    public void testCacheConfigUTF8BOM()
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_UTF8_BOM);
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * This test utilizes the cache config xml which is encoded in UTF-8 and does not include a BOM character
     */
    @Test
    public void testCacheConfigUTF8NoBOM()
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_UTF8_NOBOM);
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * This test utilizes the cache config xml which is encoded in ANSI or ISO-8859-1
     */
    @Test
    public void testCacheConfigANSI()
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_ANSI);
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }


    /**
     * This test uses a non-existent cache config file.
     */
    @Test
    public void testCacheConfigNonExistent()
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_NON_EXISTENT);
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");
            }
        catch (Exception e)
            {
            assertTrue(e instanceof RuntimeException);
            assertTrue(getOriginalException((RuntimeException) e) instanceof IOException);

            String s_message = getOriginalException((RuntimeException) e).getMessage();

            assertNotNull(s_message);
            assertTrue(s_message.startsWith(
                         "Could not load cache configuration resource file"));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }


    /**
     * This test uses a illegal cache config file (typos).
     */
    @Test
    public void testCacheConfigIllegal()
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_ILLEGAL);
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");
            }
        catch (Exception e)
            {
            assertTrue(e instanceof RuntimeException);
            assertTrue(getOriginalException((RuntimeException) e) instanceof SyntaxException);

            String s_message = getOriginalException((RuntimeException) e).getMessage();

            assertNotNull(s_message);
            assertTrue(s_message.startsWith(
                         "looking for name token=cache-name, found token"));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * This test uses a cache config file with a # in file name.
     */
    @Test
    public void testCacheConfigWithHashInFileName()
        {
        File cacheConfigFile = new File(FILE_CFG_CACHE_HASH_CHAR_IN_FILE_NAME);
        try
            {
            URL url = Resources.findFileOrResource("server-cache-config.xml", null);
            FileHelper.copyFile(new File(url.toURI()), cacheConfigFile);
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_HASH_CHAR_IN_FILE_NAME);
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");
            }
        catch (Exception e)
            {
            fail(e.getMessage());
            }
        finally
            {
            cacheConfigFile.delete();
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * Test to verify exception is thrown when cache configuration
     * is invalid due to missing scheme that referenced thru scheme-ref
     */
    @Test
    public void testInvalidCacheConfigMissingSchemRef()
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_MISSING_SCHEME);
            AbstractFunctionalTest._startup();
            CacheFactory.getCache("dist-testCache");
            fail("Exception should have been thrown!");
            }
        catch (Exception e)
            {
            assertTrue(e instanceof WrapperException);
            assertTrue("Expected exception: " + ((WrapperException) e).getOriginalException().getMessage(),
                       ((WrapperException) e).getOriginalException() instanceof ConfigurationException);
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            AbstractFunctionalTest._shutdown();
            }
        }

    // ----- constants and data members -------------------------------------

    /**
     * Cache configuration file encoded as UTF8 with BOM character present.
     */
    public final static String FILE_CFG_CACHE_UTF8_BOM = "cache-config-UTF8-BOM.xml";

    /**
     * Cache configuration file encoded as UTF8 with BOM character absent.
     */
    public final static String FILE_CFG_CACHE_UTF8_NOBOM = "cache-config-UTF8-NoBOM.xml";

    /**
     * Cache configuration file encoded as ANSI or ISO-8859-1.
     */
    public final static String FILE_CFG_CACHE_ANSI = "cache-config-ANSI.xml";

    /**
     * Cache configuration file that does not exist.
     */
    public final static String FILE_CFG_CACHE_NON_EXISTENT = "cache-config-non-existent.xml";

    /**
     * Cache configuration file that syntactical errors.
     */
    public final static String FILE_CFG_CACHE_ILLEGAL = "cache-config-illegal.xml";

    /**
     * Cache configuration file that contains # character in the filename.
     */
    public final static String FILE_CFG_CACHE_HASH_CHAR_IN_FILE_NAME = "COH10679#-cache-config.xml";

    /**
     * Cache configuration file which has missing scheme that is reference using scheme-ref
     */
    public final static String FILE_CFG_CACHE_MISSING_SCHEME = "invalid-scheme-ref-cache-config.xml";
    }
