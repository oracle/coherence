/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.run.xml.XmlHelper;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for serializer container limit policies.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
public class SerializerLimitPolicyTest
    {
    @After
    public void cleanup()
        {
        restore(SerializationLimitPolicy.PROP_MAX_CONTAINER_BYTES, m_sContainerBytes);
        restore(SerializationLimitPolicy.PROP_MAX_ELEMENTS, m_sElements);
        restore(SerializationLimitPolicy.PROP_MAX_MAP_ENTRIES, m_sMapEntries);
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldDefaultLegacyModeToUnlimited()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            SerializationLimitPolicy policy = SerializationLimitPolicy.getDefault();

            assertNull(policy.getMaxContainerBytes());
            assertNull(policy.getMaxElements());
            assertNull(policy.getMaxMapEntries());
            }
        }

    @Test
    public void shouldDefaultProdModeToFiniteCaps()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            SerializationLimitPolicy policy = SerializationLimitPolicy.getDefault();

            assertEquals(Long.valueOf(SerializationLimitPolicy.DEFAULT_MAX_CONTAINER_BYTES),
                    policy.getMaxContainerBytes());
            assertEquals(Integer.valueOf(SerializationLimitPolicy.DEFAULT_MAX_ELEMENTS),
                    policy.getMaxElements());
            assertEquals(Integer.valueOf(SerializationLimitPolicy.DEFAULT_MAX_MAP_ENTRIES),
                    policy.getMaxMapEntries());
            }
        }

    @Test
    public void shouldRejectUnlimitedPropertyOutsideLegacyMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            System.setProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS, "unlimited");
            assertThrows(IllegalArgumentException.class, SerializationLimitPolicy::getDefault);
            }
        }

    @Test
    public void shouldMergeXmlFiniteValuesWithInheritedDefaults()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            SerializationLimitPolicy override = SerializationLimitPolicy.fromXml(XmlHelper.loadXml(
                    "<limits><max-elements>7</max-elements></limits>"));
            SerializationLimitPolicy policy = SerializationLimitPolicy.getDefault().merge(override);

            assertEquals(Long.valueOf(SerializationLimitPolicy.DEFAULT_MAX_CONTAINER_BYTES),
                    policy.getMaxContainerBytes());
            assertEquals(Integer.valueOf(7), policy.getMaxElements());
            assertEquals(Integer.valueOf(SerializationLimitPolicy.DEFAULT_MAX_MAP_ENTRIES),
                    policy.getMaxMapEntries());
            }
        }

    @Test
    public void shouldAllowXmlUnlimitedOnlyInLegacyMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            SerializationLimitPolicy override = SerializationLimitPolicy.fromXml(XmlHelper.loadXml(
                    "<limits><max-elements>unlimited</max-elements></limits>"));
            SerializationLimitPolicy policy = new SerializationLimitPolicy(64L, 8, 4).merge(override);

            assertEquals(Long.valueOf(64), policy.getMaxContainerBytes());
            assertNull(policy.getMaxElements());
            assertEquals(Integer.valueOf(4), policy.getMaxMapEntries());
            }

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            assertThrows(IllegalArgumentException.class, () -> SerializationLimitPolicy.fromXml(XmlHelper.loadXml(
                    "<limits><max-elements>unlimited</max-elements></limits>")));
            }
        }

    @Test
    public void shouldParseResolvedLimitElementTextOnly()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            System.setProperty("test.serializer.max.elements", "9");
            SerializationLimitPolicy override = SerializationLimitPolicy.fromXml(XmlHelper.loadXml(
                    "<limits><max-elements system-property=\"test.serializer.max.elements\">7</max-elements></limits>"));

            assertEquals(Integer.valueOf(7), SerializationLimitPolicy.getDefault().merge(override).getMaxElements());
            }
        finally
            {
            System.clearProperty("test.serializer.max.elements");
            }
        }

    private static void restore(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private final String m_sContainerBytes = System.getProperty(SerializationLimitPolicy.PROP_MAX_CONTAINER_BYTES);
    private final String m_sElements       = System.getProperty(SerializationLimitPolicy.PROP_MAX_ELEMENTS);
    private final String m_sMapEntries     = System.getProperty(SerializationLimitPolicy.PROP_MAX_MAP_ENTRIES);
    }
