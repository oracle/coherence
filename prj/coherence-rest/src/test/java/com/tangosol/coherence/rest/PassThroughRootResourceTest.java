/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.rest.config.DirectQuery;
import com.tangosol.coherence.rest.config.ResourceConfig;
import com.tangosol.coherence.rest.config.RestConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link PassThroughRootResource}.
 * <p>
 * Prompt 02 at
 * design/features/security-bugs/plans/rest-01/prompts/02-slice-b-auth-passthrough-implementation.md
 * requires DEV/PROD allowlist enforcement and LEGACY auto-publish compatibility
 * for pass-through resources.
 *
 * @author Vaso Putica  2026.05.07
 * @since 26.04
 */
public class PassThroughRootResourceTest
    {
    /**
     * Save and override the REST config descriptor for each test.
     */
    @Before
    public void setup()
        {
        m_sOldRestConfig = System.getProperty(RestConfig.DESCRIPTOR_PROPERTY);
        System.setProperty(RestConfig.DESCRIPTOR_PROPERTY, "test-coherence-rest-config.xml");
        }

    /**
     * Restore the REST config descriptor and Coherence mode.
     */
    @After
    public void cleanup()
        {
        if (m_sOldRestConfig == null)
            {
            System.clearProperty(RestConfig.DESCRIPTOR_PROPERTY);
            }
        else
            {
            System.setProperty(RestConfig.DESCRIPTOR_PROPERTY, m_sOldRestConfig);
            }
        CoherenceModeHelper.clear();
        }

    /**
     * Should reject unknown pass-through resources in DEV mode.
     */
    @Test
    public void shouldRejectUnknownPassThroughResourceInDev()
        {
        shouldRejectUnknownPassThroughResource(CoherenceModeHelper.dev());
        }

    /**
     * Should reject unknown pass-through resources in PROD mode.
     */
    @Test
    public void shouldRejectUnknownPassThroughResourceInProd()
        {
        shouldRejectUnknownPassThroughResource(CoherenceModeHelper.prod());
        }

    /**
     * Should preserve pass-through auto-publish compatibility in LEGACY mode.
     */
    @Test
    public void shouldAutoPublishUnknownPassThroughResourceInLegacy()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            RestConfig config = RestConfig.create();
            TestPassThroughRootResource resource = new TestPassThroughRootResource(config);

            resource.getCacheResource("legacy-cache");

            ResourceConfig configResource = config.getResources().get("legacy-cache");
            assertNotNull(configResource);
            assertSame(configResource, resource.getCapturedConfig());
            assertEquals("legacy-cache", configResource.getCacheName());
            assertEquals(Integer.MAX_VALUE, configResource.getMaxResults());

            DirectQuery directQuery = configResource.getQueryConfig().getDirectQuery();
            assertNotNull(directQuery);
            assertTrue(configResource.getQueryConfig().isDirectQueryEnabled());
            assertEquals(Integer.MAX_VALUE, directQuery.getMaxResults());
            }
        }

    /**
     * Should use an explicit REST resource configuration in DEV mode.
     */
    @Test
    public void shouldUseExplicitResourceConfigInDev()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.dev())
            {
            RestConfig config = RestConfig.create();
            TestPassThroughRootResource resource = new TestPassThroughRootResource(config);

            resource.getCacheResource("test-cache-direct-query2");

            ResourceConfig configResource = resource.getCapturedConfig();
            assertNotNull(configResource);
            assertEquals("test-cache-direct-query2", configResource.getCacheName());
            assertEquals(-1, configResource.getMaxResults());
            assertTrue(configResource.getQueryConfig().isDirectQueryEnabled());
            assertEquals(-1, configResource.getQueryConfig().getDirectQuery().getMaxResults());
            }
        }

    /**
     * Should not infer direct-query enablement from explicit pass-through
     * allowlisting in hardened modes.
     */
    @Test
    public void shouldNotAutoEnableDirectQueryForExplicitResourceInDev()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.dev())
            {
            RestConfig config = RestConfig.create();
            TestPassThroughRootResource resource = new TestPassThroughRootResource(config);

            resource.getCacheResource("dist-test-basic");

            ResourceConfig configResource = resource.getCapturedConfig();
            assertNotNull(configResource);
            assertFalse(configResource.getQueryConfig().isDirectQueryEnabled());
            }
        }

    /**
     * Assert that an unknown pass-through resource is rejected in the supplied
     * mode scope.
     *
     * @param scope  the mode scope to test
     */
    private static void shouldRejectUnknownPassThroughResource(CoherenceModeHelper.ModeScope scope)
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            RestConfig config = RestConfig.create();
            TestPassThroughRootResource resource = new TestPassThroughRootResource(config);

            try
                {
                resource.getCacheResource("unknown-cache");
                fail("Expected NotFoundException");
                }
            catch (NotFoundException expected)
                {
                assertFalse(config.getResources().containsKey("unknown-cache"));
                }
            }
        }


    private static class TestPassThroughRootResource
            extends PassThroughRootResource
        {
        /**
         * Construct a test pass-through root resource.
         *
         * @param config  the REST configuration
         */
        TestPassThroughRootResource(RestConfig config)
            {
            m_config = config;
            }

        /**
         * Capture the resource config instead of creating a live cache
         * resource.
         *
         * @param configResource  the resource configuration
         *
         * @return {@code null}
         */
        @Override
        protected CacheResource instantiateCacheResourceInternal(ResourceConfig configResource)
            {
            m_configResource = configResource;
            return null;
            }

        /**
         * Return the captured resource configuration.
         *
         * @return the captured resource configuration
         */
        ResourceConfig getCapturedConfig()
            {
            return m_configResource;
            }

        private ResourceConfig m_configResource;
        }

    private String m_sOldRestConfig;
    }
