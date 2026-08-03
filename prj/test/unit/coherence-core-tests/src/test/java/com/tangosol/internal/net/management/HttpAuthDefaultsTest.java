/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link HttpAuthDefaults}.
 *
 * @author jk  2026.05.14
 * @since 26.07
 */
public class HttpAuthDefaultsTest
    {
    @After
    public void cleanup()
        {
        System.clearProperty(HttpAuthDefaults.PROP_MANAGEMENT_AUTH);
        System.clearProperty(HttpAuthDefaults.PROP_METRICS_AUTH);
        HttpAuthDefaults.setLoggersForTesting(null, null);
        }

    @Test
    public void shouldResolveManagementModeDefaults()
        {
        assertDefault(HttpAuthDefaults.SERVICE_MANAGEMENT, HttpAuthDefaults.PROP_MANAGEMENT_AUTH,
                CoherenceMode.PROD, "basic");
        assertDefault(HttpAuthDefaults.SERVICE_MANAGEMENT, HttpAuthDefaults.PROP_MANAGEMENT_AUTH,
                CoherenceMode.DEV, "basic");
        assertDefault(HttpAuthDefaults.SERVICE_MANAGEMENT, HttpAuthDefaults.PROP_MANAGEMENT_AUTH,
                CoherenceMode.LEGACY, "none");
        }

    @Test
    public void shouldResolveMetricsModeDefaults()
        {
        assertDefault(HttpAuthDefaults.SERVICE_METRICS, HttpAuthDefaults.PROP_METRICS_AUTH,
                CoherenceMode.PROD, "basic");
        assertDefault(HttpAuthDefaults.SERVICE_METRICS, HttpAuthDefaults.PROP_METRICS_AUTH,
                CoherenceMode.DEV, "basic");
        assertDefault(HttpAuthDefaults.SERVICE_METRICS, HttpAuthDefaults.PROP_METRICS_AUTH,
                CoherenceMode.LEGACY, "none");
        }

    @Test
    public void shouldLetExplicitPropertyNoneWinWithWarning()
        {
        List<String> listWarnings = new ArrayList<>();
        HttpAuthDefaults.setLoggersForTesting(s -> { }, listWarnings::add);
        System.setProperty(HttpAuthDefaults.PROP_MANAGEMENT_AUTH, "none");

        XmlElement xml = xmlWithMarker(HttpAuthDefaults.PROP_MANAGEMENT_AUTH);
        HttpAuthDefaults.Resolution resolution = HttpAuthDefaults.resolve(xml,
                HttpAuthDefaults.SERVICE_MANAGEMENT, HttpAuthDefaults.PROP_MANAGEMENT_AUTH, CoherenceMode.PROD);

        assertThat(resolution.getSource(), is(HttpAuthDefaults.Source.PROPERTY));
        assertThat(auth(xml), is("none"));
        assertThat(listWarnings.size(), is(1));
        assertThat(listWarnings.get(0).contains(HttpAuthDefaults.PROP_MANAGEMENT_AUTH), is(true));
        }

    @Test
    public void shouldLetExplicitXmlNoneWinWithoutMarker()
        {
        XmlElement xml = xmlWithAuth("none");
        HttpAuthDefaults.Resolution resolution = HttpAuthDefaults.resolve(xml,
                HttpAuthDefaults.SERVICE_MANAGEMENT, HttpAuthDefaults.PROP_MANAGEMENT_AUTH, CoherenceMode.PROD);

        assertThat(resolution.getSource(), is(HttpAuthDefaults.Source.XML));
        assertThat(auth(xml), is("none"));
        }

    @Test
    public void shouldPreserveExplicitAuthenticatedMethods()
        {
        assertExplicitXml("basic");
        assertExplicitXml("cert");
        assertExplicitXml("cert+basic");
        }

    private void assertDefault(String sService, String sProperty, CoherenceMode mode, String sExpected)
        {
        XmlElement xml = xmlWithMarker(sProperty);
        HttpAuthDefaults.Resolution resolution = HttpAuthDefaults.resolve(xml, sService, sProperty, mode);

        assertThat(resolution.getSource(), is(HttpAuthDefaults.Source.MODE_DEFAULT));
        assertThat(auth(xml), is(sExpected));
        assertThat(authElement(xml).getAttribute("system-property") == null,
                is(true));
        }

    private void assertExplicitXml(String sAuth)
        {
        XmlElement xml = xmlWithAuth(sAuth);
        HttpAuthDefaults.resolve(xml, HttpAuthDefaults.SERVICE_MANAGEMENT,
                HttpAuthDefaults.PROP_MANAGEMENT_AUTH, CoherenceMode.PROD);
        assertThat(auth(xml), is(sAuth));
        }

    private String auth(XmlElement xml)
        {
        return authElement(xml).getString();
        }

    private XmlElement authElement(XmlElement xml)
        {
        return xml.getElement("acceptor-config").getElement("http-acceptor").getElement("auth-method");
        }

    private XmlElement xmlWithMarker(String sProperty)
        {
        return XmlHelper.loadXml("<proxy-scheme><acceptor-config><http-acceptor>"
                + "<auth-method system-property=\"" + sProperty + "\"/>"
                + "</http-acceptor></acceptor-config></proxy-scheme>");
        }

    private XmlElement xmlWithAuth(String sAuth)
        {
        return XmlHelper.loadXml("<proxy-scheme><acceptor-config><http-acceptor>"
                + "<auth-method>" + sAuth + "</auth-method>"
                + "</http-acceptor></acceptor-config></proxy-scheme>");
        }
    }
