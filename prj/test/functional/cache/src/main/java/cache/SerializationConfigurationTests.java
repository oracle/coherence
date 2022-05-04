/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import static org.junit.Assert.*;

public class SerializationConfigurationTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SerializationConfigurationTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test methods ---------------------------------------------------


    /**
    * Test the ability to configure an in-line ConfigurablePofContext Serializer.
    */
    @Test
    public void inlinePofContextSerializer() throws Exception
        {
        NamedCache cache = getNamedCache("dist-inline-pofcontext");
        assertTrue(cache.getCacheService().getSerializer() instanceof ConfigurablePofContext);
        }

    /**
    * Test the ability to configure an in-line Java SerializationCache subclass.
    */
    @Test
    public void inlineJavaSerializer() throws Exception
        {
        NamedCache cache = getNamedCache("dist-inline-java");
        assertTrue(cache.getCacheService().getSerializer() instanceof DefaultSerializer);
        }

    /**
     * Test the ability to configure an in-line POF Serializer.
     */
     @Test
     public void inlinePofSerializer() throws Exception
         {
         NamedCache cache = getNamedCache("dist-inline-pof");
         assertTrue(cache.getCacheService().getSerializer() instanceof ConfigurablePofContext);
         }

     /**
      * Test the ability to configure a default POF Serializer.
      */
      @Test
      public void defaultPofSerializer() throws Exception
          {
          NamedCache cache = getNamedCache("dist-default-pof");
          assertTrue(cache.getCacheService().getSerializer() instanceof ConfigurablePofContext);
          }

    /**
     * Test the ability to configure a default Java Serializer.
     */
     @Test
     public void defaultJavaSerializer() throws Exception
         {
         ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
             .getConfigurableCacheFactory(FILE_CFG_DEFAULT_JAVA_CACHE, null);
         NamedCache cache = factory.ensureCache("default-java-serializer", null);
         assertTrue(cache.getCacheService().getSerializer() instanceof DefaultSerializer);
         }

    /**
     * Test the ability to configure a default custom Serializer.
     */
     @Test
     public void defaultCustomSerializer() throws Exception
         {
         ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
             .getConfigurableCacheFactory(FILE_CFG_DEFAULT_CUSTOM_CACHE, null);
         NamedCache cache = factory.ensureCache("default-custom-serializer", null);
         assertTrue(cache.getCacheService().getSerializer() instanceof PofSerializer);
         }

    // ----- inner class ----------------------------------------------------

    public static class PofSerializer
            extends ConfigurablePofContext
            {
            /**
             * Construct a PofSerialier.
             *
             * @param sPofFile  the Pof file name
             */
            public PofSerializer(String sPofFile)
                {
                super (sPofFile);
                }
            }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "serializer-cache-config.xml";

    /**
     * The file name of the cache configuration file to test default java serializer.
     */
    public static String FILE_CFG_DEFAULT_JAVA_CACHE = "serializer-java-cache-config.xml";

    /**
     * The file name of the cache configuration file to test default custom class serializer.
     */
    public static String FILE_CFG_DEFAULT_CUSTOM_CACHE = "serializer-custom-cache-config.xml";
    }
