/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.InetAddressRangeFilterBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.ConfigurableAddressProviderFactory;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.ServiceFailurePolicy;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class contains helper methods for processing configuration related XML DOMs.
 *
 * @author pfm 2011.05.12
 * @since Coherence 12.1.2
 */
public class LegacyXmlConfigHelper
    {
    /**
     * Parse the XML and return a filter list specified by use-filters.
     *
     * @param xml  the parent of the use-filters element
     *
     * @return the list of filter names
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Deprecated
    public static List<String> parseFilterList(XmlElement xml)
        {
        List<String> list = new ArrayList<String>();

        for (Iterator iter = xml.getSafeElement("use-filters").getElements("filter-name"); iter.hasNext(); )
            {
            String sName = ((XmlElement) iter.next()).getString();
            if (sName.length() > 0)
                {
                list.add(sName);
                }
            }

        return list.size() == 0 ? Collections.EMPTY_LIST : list;
        }

    /**
     * Parse the service failure policy and return service failure policy object.
     *
     * @param xml  the service-failure-policy element
     *
     * @return the service failure policy configuration
     */
    public static ServiceFailurePolicyBuilder parseServiceFailurePolicyBuilder(XmlElement xml)
        {
        ServiceFailurePolicyBuilder builder = null;
        String                      sPolicy = xml.getString().trim();

        if (sPolicy.length() != 0)
            {
            builder = new ServiceFailurePolicyBuilder(sPolicy, xml);
            }
        else if (!xml.getElementList().isEmpty())
            {
            ParameterizedBuilder<ServiceFailurePolicy> bldr =
                    LegacyXmlConfigHelper.createBuilder(xml, ServiceFailurePolicy.class);
            builder = new ServiceFailurePolicyBuilder(bldr, xml);
            }

        return builder;
        }

    /**
     * Parse the XML and return the authorized hosts filter.
     *
     * @param xml  the parent of the authorized-hosts XML element
     *
     * @return the authorized hosts filter or null
     */
    @SuppressWarnings({"rawtypes"})
    @Deprecated
    public static ParameterizedBuilder<Filter> parseAuthorizedHosts(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("authorized-hosts");

        // Use a custom filter if is specified.
        // <host-filter>
        XmlElement xmlVal = xmlCat.getElement("host-filter");
        if (xmlVal != null && !XmlHelper.isEmpty(xmlVal))
            {
            // don't process any host-addresses since there is a custom filter.
            return createBuilder(xmlVal, Filter.class);
            }
        else
            {
            InetAddressRangeFilterBuilder builder       = new InetAddressRangeFilterBuilder();

            // <host-address>
            for (Iterator iter = xmlCat.getElements("host-address"); iter.hasNext(); )
                {
                xmlVal = (XmlElement) iter.next();

                builder.addAuthorizedHostsToFilter(xmlVal.getString(), null);
                }

            // <host-range>
            for (Iterator iter = xmlCat.getElements("host-range"); iter.hasNext(); )
                {
                xmlVal = (XmlElement) iter.next();

                builder.addAuthorizedHostsToFilter(xmlVal.getSafeElement("from-address").getString(),
                                               xmlVal.getSafeElement("to-address").getString());
                }

            return builder;
            }
        }

    /**
     * Parse the XML and return a populated local InetSocketAddress.
     *
     * @param xml  the parent of the sXML elements containing the local address
     *
     * @return a local InetSocketAddress
     */
    public static InetSocketAddress parseLocalSocketAddress(XmlElement xml)
        {
        XmlElement xmlAddr = xml.getElement("address");
        XmlElement xmlPort = xml.getElement("port");

        if (xmlAddr == null && xmlPort == null)
            {
            return null;
            }

        String      sAddr = xmlAddr == null ? "localhost" : xmlAddr.getString();
        int         nPort = xmlPort == null ? 0 : xmlPort.getInt();

        InetAddress addr  = null;
        try
            {
            addr = InetAddressHelper.getLocalAddress(sAddr);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "the \"" + xml.getName()
                                              + "\" configuration element contains an invalid \"address\" element");
            }

        try
            {
            return new InetSocketAddress(addr, nPort);
            }
        catch (RuntimeException e)
            {
            throw Base.ensureRuntimeException(e, "the \"" + xml.getName()
                                              + "\" configuration element contains an invalid \"port\" element");
            }
        }

    /**
     *  Create an instance of the class configured using an XmlElement.
     *
     *  @param xml       the XML element that contains the instantiation info
     *  @param clzName   the class name of the instance to be created
     *
     *  @return an object instantiated or obtained based on the class configuration
     */
    public static Object createInstance(XmlElement xml, String clzName)
        {
        try
            {
            return XmlHelper.createInstance(xml, Base.getContextClassLoader(), null, null);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error creating " + clzName + " object." + xml);
            }
        }

    /**
     * Create a builder for the class configured using an XmlElement.
     * @param xml the XML element that contains the instantiation info
     * @param clz  the class of the instance created by the builder
     * @return the builder for xml element
     */
    public static <T> ParameterizedBuilder<T> createBuilder(XmlElement xml, Class<T> clz)
        {
        final XmlElement f_xml      = xml;
        final String     f_sClzName = clz.getName();

        return new ParameterizedBuilder<T>()
            {
            public T realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                try
                    {
                    return (T) XmlHelper.createInstance(f_xml, loader, null, clz);
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e, "Error creating " + f_sClzName + " object." + f_xml);
                    }
                }

            };
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Parse the XML and return an AddressProviderFactory.
     *
     * @param xml  the parent of the address-provider element
     * @param map  the map from name to AddressProviderFactory
     *
     * @return the AddressProviderFactory
     */
    public static AddressProviderFactory parseAddressProvider(XmlElement xml, Map<String, AddressProviderFactory> map)
        {
        return parseAddressProvider("address-provider", xml, map);
        }

    /**
     * Parse the XML for the specified element and return an AddressProviderFactory.
     *
     * @param sElement  the name of the element containing the AddressProvider
     *                  configuration
     * @param xml       the parent of the address-provider element
     * @param map       the map from name to AddressProviderFactory
     *
     * @return the AddressProviderFactory
     */
    public static AddressProviderFactory parseAddressProvider(String sElement, XmlElement xml,
        Map<String, AddressProviderFactory> map)
        {
        XmlElement xmlProvider = xml.getElement(sElement);
        boolean    fMissing    = xmlProvider == null;
        boolean    fEmpty      = !fMissing && xmlProvider.isEmpty();
        if (fEmpty || fMissing)
            {
            ConfigurableAddressProviderFactory factory = new ConfigurableAddressProviderFactory();
            factory.setConfig(fMissing ? xml : xmlProvider);

            return factory;
            }
        else
            {
            String sName = xmlProvider.getString();

            // The <address-provider> element is _not_ empty; it contains
            // a string value (e.g. <address-provider>name</address-provider>).
            // The address provider map should contain a corresponding address provider.
            AddressProviderFactory factory = map.get(sName);
            if (factory == null)
                {
                throw new IllegalArgumentException("Address-provider name \"" + sName + "\" is undefined:\n"
                                                   + xmlProvider);
                }

            return factory;
            }
        }
    }
