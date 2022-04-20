/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
/**
 * 
 */
package com.tangosol.net;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * Test for the Custom implementation of
 * {@link ExtensibleConfigurableCacheFactory}
 *
 * @author chpatel
 */
public class CustomExtensibleConfigurableCacheFactoryTest
    {

    @Test
    public void testCustomExtensibleConfigurableCacheFactory()
        {
        ConfigurableCacheFactory ccf = getCustomCacheFactory();
        assertNotNull(ccf);
        assertTrue("Failed to get required Configured CacheFactory instance.",
                ccf instanceof CustomExtensibleConfigurableCacheFactory);

        try
            {
            NamedCache<String, String> cache = ccf.ensureCache("my-cache",
                    CustomExtensibleConfigurableCacheFactory.class.getClassLoader());

            assertNotNull(cache);
            ccf.destroyCache(cache);
            } finally
            {
            ccf.dispose();
            }
        }

    private ConfigurableCacheFactory getCustomCacheFactory()
        {
        XmlElement xmlConfig = XmlHelper.loadFileOrResource("net/coherence-cache-config.xml", "cache config");
        ConfigurableCacheFactory ccf = new CustomExtensibleConfigurableCacheFactory(
                DependenciesHelper.newInstance(xmlConfig, this.getClass().getClassLoader()));

        CacheFactory.getCacheFactoryBuilder().setConfigurableCacheFactory(ccf, "net/coherence-cache-config.xml",
                CustomExtensibleConfigurableCacheFactory.class.getClassLoader(), true);

        return CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory("net/coherence-cache-config.xml",
                CustomExtensibleConfigurableCacheFactory.class.getClassLoader());
        }

    public static class CustomExtensibleConfigurableCacheFactory extends ExtensibleConfigurableCacheFactory
        {
        // ----- constructors ---------------------------------------------------

        /**
         * @param dependencies
         */
        public CustomExtensibleConfigurableCacheFactory(Dependencies dependencies)
        {
            super(dependencies);
        }

        /**
         * @param path
         */
        public CustomExtensibleConfigurableCacheFactory(String path)
        {
            super(ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(path));
        }
        }
    }
