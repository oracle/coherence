/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.net.management.MBeanServerFinder;
import com.tangosol.net.management.Registry;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * LegacyXmlGatewayHelper parses the <management-config> XML to populate the DefaultGatewayDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der  2011.07.08
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlGatewayHelper
    {
    /**
     * Populate the DefaultGatewayDependencies object from the XML configuration.
     *
     * @param xml   the <{@code <management-config>} XML element
     * @param deps  the DefaultGatewayDependencies to be populated
     *
     * @return the DefaultGatewayDependencies object that was passed in.
     */
    public static DefaultGatewayDependencies fromXml(XmlElement xml, DefaultGatewayDependencies deps)
        {
        Base.azzert(xml.getName().equals("management-config"));

        deps.setManagedNodes(xml.getSafeElement("managed-nodes").getString(deps.getManagedNodes()));
        deps.setHttpManagedNodes(xml.getSafeElement("http-managed-nodes").getString(deps.getHttpManagedNodes()));
        deps.setAllowRemoteManagement(xml.getSafeElement("allow-remote-management").getBoolean(
                deps.isAllowRemoteManagement()));
        deps.setReadOnly(xml.getSafeElement("read-only").getBoolean(deps.isReadOnly()));
        if (!Registry.SERVICE_NAME.equals(xml.getSafeElement("service-name").getString()))
            {
            Logger.info("The \"management-config/service-name\" element value is ignored");
            }
        deps.setDefaultDomain(xml.getSafeElement("default-domain-name").getString(deps.getDefaultDomain()));
        deps.setDomainNameSuffix(xml.getSafeElement("domain-name-suffix").getString(deps.getDomainNameSuffix()));
        deps.setExtendedMBeanName(xml.getSafeElement("extended-mbean-name").getBoolean(deps.isExtendedMBeanName()));

        XmlElement xmlFactory = xml.getSafeElement("server-factory");
        MBeanServerFinder mbsf = XmlHelper.isInstanceConfigEmpty(xmlFactory) ? null : (MBeanServerFinder) XmlHelper
                .createInstance(xmlFactory, null, null);
        deps.setMBeanServerFinder(mbsf);

        DefaultConnectorDependencies connectorDeps = LegacyXmlConnectorHelper.fromXml(xml,
                new DefaultConnectorDependencies());
        deps.setConnectorDependencies(connectorDeps);

        ReportBatch.DefaultDependencies reporterDeps = LegacyXmlReporterHelper.fromXml(xml.getSafeElement("reporter"),
                new ReportBatch.DefaultDependencies());
        deps.setReporterDependencies(reporterDeps);

        configureFilter(xml, deps);
        configureCustomBeans(xml, deps);

        return deps;
        }

    // ----- internal methods  ----------------------------------------------

    /**
     * Configure filters.
     *
     * @param xml  the <mbean-filter> element
     * @param deps  the DefaultConnectorDependencies to be populated
     */
    private static void configureFilter(XmlElement xml, DefaultGatewayDependencies deps)
        {
        XmlElement xmlFilter = xml.getSafeElement("mbean-filter");
        if (!XmlHelper.isEmpty(xmlFilter))
            {
            try
                {
                deps.setFilter((Filter) XmlHelper.createInstance(xmlFilter, null, null));
                }
            catch (Throwable e)
                {
                throw new IllegalArgumentException("Error instantiating mbean-filter.", e);
                }
            }
        }

    /**
     * Configure the customMBeans.
     *
     * @param xml   the <mbeans> element
     * @param deps  the DefaultGatewayDependencies to be populated
      */
    private static void configureCustomBeans(XmlElement xml, DefaultGatewayDependencies deps)
        {
        List<CustomMBeanDependencies> listCustomMBeanDeps = new ArrayList <CustomMBeanDependencies>();

        XmlElement xmlConfig = xml.getSafeElement("mbeans");

        for (Iterator iterBeans = xmlConfig.getElements("mbean"); iterBeans.hasNext();)
            {
            listCustomMBeanDeps.add(LegacyXmlCustomMBeanHelper.fromXml(
                    (XmlElement) iterBeans.next(),
                    new DefaultCustomMBeanDependencies()));
            }

        if (!listCustomMBeanDeps.isEmpty())
            {
            deps.setCustomMBeanDependencies(listCustomMBeanDeps);
            }
        }
    }
