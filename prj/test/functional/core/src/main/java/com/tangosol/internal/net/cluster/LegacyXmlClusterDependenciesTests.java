/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Resources;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.fail;

public class LegacyXmlClusterDependenciesTests
    {
    @Test
    public void shouldResolveWKA()
        {
        URL                          url          = Resources.findFileOrResource("tangosol-coherence.xml", null);
        XmlElement                   xmlElement   = XmlHelper.loadXml(url);
        LegacyXmlClusterDependencies dependencies = new LegacyXmlClusterDependencies();

        // should not throw, should resolve loop-back WKA
        System.setProperty("coherence.wka", "127.0.0.1");
        XmlHelper.replaceSystemProperties(xmlElement, "system-property");
        dependencies.fromXml(xmlElement.getSafeElement("cluster-config"));
        }

    @Test
    public void shouldFailToResolveWKA()
        {
        URL                          url          = Resources.findFileOrResource("tangosol-coherence.xml", null);
        XmlElement                   xmlElement   = XmlHelper.loadXml(url);
        LegacyXmlClusterDependencies dependencies = new LegacyXmlClusterDependencies();

        System.setProperty("coherence.wka", "bad.host");
        System.setProperty(LegacyXmlClusterDependencies.PROP_WKA_RESOLVE_RETRY, "false");
        XmlHelper.replaceSystemProperties(xmlElement, "system-property");

        try
            {
            dependencies.fromXml(xmlElement.getSafeElement("cluster-config"));
            fail("Expected IllegalArgumentException");
            }
        catch (IllegalArgumentException ignored)
            {
            // expected
            }
        }

    @Test
    public void shouldEventuallyTimeoutResolvingWKA()
        {
        URL                          url          = Resources.findFileOrResource("tangosol-coherence.xml", null);
        XmlElement                   xmlElement   = XmlHelper.loadXml(url);
        LegacyXmlClusterDependencies dependencies = new LegacyXmlClusterDependencies();

        // should initially block with unresolved WKA
        System.setProperty("coherence.wka", "bad.host");
        System.setProperty(LegacyXmlClusterDependencies.PROP_WKA_RESOLVE_RETRY, "true");
        System.setProperty(LegacyXmlClusterDependencies.PROP_WKA_TIMEOUT, "2000");
        XmlHelper.replaceSystemProperties(xmlElement, "system-property");

        try
            {
            dependencies.fromXml(xmlElement.getSafeElement("cluster-config"));
            fail("Expected IllegalArgumentException");
            }
        catch (IllegalArgumentException ignored)
            {
            // expected
            }
        }
    }
