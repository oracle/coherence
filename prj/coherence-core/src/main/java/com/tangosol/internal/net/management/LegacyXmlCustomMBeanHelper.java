/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * LegacyXmlCustomMBeanHelper parses the MBean portion of the <management-config> XML to
 * populate the DefaultCustomMBeanDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der 2011.07.08
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlCustomMBeanHelper
    {
    /**
     * Populate the DefaultCustomMBeanHelper object from the given xml configuration
     *
     * @param xml   the {@code <management-config>} XML element
     * @param deps  the DefaultCustomMBeanDependencies that was passed in
     *
     * @return the DefaultCustomMBeanDependencies
     */
    public static DefaultCustomMBeanDependencies fromXml(XmlElement xml, DefaultCustomMBeanDependencies deps)
        {
        Base.azzert(xml.getName().equals("mbean"));

        deps.setMBeanFactory(xml.getSafeElement("mbean-factory").getString(deps.getMBeanFactory()));
        deps.setMBeanServerDomain(xml.getSafeElement("mbean-server-domain").getString(deps.getMBeanServerDomain()));
        deps.setMBeanQuery(xml.getSafeElement("mbean-query").getString(deps.getMBeanQuery()));
        deps.setMBeanClass(xml.getSafeElement("mbean-class").getString(deps.getMBeanClass()));
        deps.setMBeanAccessor(xml.getSafeElement("mbean-accessor").getString(deps.getMBeanAccessor()));
        deps.setMBeanName(xml.getSafeElement("mbean-name").getString(deps.getMBeanName()));
        deps.setLocalOnly(xml.getSafeElement("local-only").getBoolean(deps.isLocalOnly()));
        deps.setEnabled(xml.getSafeElement("enabled").getBoolean(deps.isEnabled()));
        deps.setExtendLifecycle(xml.getSafeElement("extend-lifecycle").getBoolean(deps.isExtendLifecycle()));

        return deps;
        }
    }
