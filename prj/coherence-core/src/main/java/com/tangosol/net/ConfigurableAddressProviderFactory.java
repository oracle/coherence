/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * A {@link AddressProviderFactory} implementation that creates instances of an
 * address provider class configured using an XmlElement of the following structure:
 * <pre>
 *   &lt;!ELEMENT ... (socket-address+ | address-provider)&gt;
 *   &lt;!ELEMENT address-provider
 *     (class-name | (class-factory-name, method-name), init-params?&gt;
 *   &lt;!ELEMENT socket-address (address, port)&gt;
 * </pre>
 *
 * @author wl  2012.04.04
 *
 * @since Coherence 12.1.2
 */
@Deprecated
public class ConfigurableAddressProviderFactory
        implements AddressProviderFactory, XmlConfigurable
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ConfigurableAddressProviderFactory()
        {
        }

    // ----- AddressProviderFactory interface -------------------------------

    /**
     * Instantiate an AddressProvider configured according to the specified XML.
     * The passed XML has to conform to the following format:
     * <pre>
     *   &lt;!ELEMENT ... (socket-address+ | address-provider)&gt;
     *   &lt;!ELEMENT address-provider
     *     (class-name | (class-factory-name, method-name), init-params?&gt;
     *   &lt;!ELEMENT socket-address (address, port)&gt;
     * </pre>
     *
     * Note: "the "remote-addresses" element declaration for the coherence-cache-config.xsd in
     *       the Coherence library"
     *
     * @param loader  (optional) the ClassLoader that should be used to load
     *                necessary classes
     *
     * @return an instance of the corresponding AddressProvider implementation
     */
    public AddressProvider createAddressProvider(ClassLoader loader)
        {
        XmlElement xmlConfig = getConfig();

        return (xmlConfig.getName().equals("address-provider") &&
                !XmlHelper.isInstanceConfigEmpty(xmlConfig))
            ? (AddressProvider) XmlHelper.createInstance(xmlConfig, loader, null)
            : ConfigurableAddressProvider.makeProvider(xmlConfig);
        }

    // ----- XmlConfigurable ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void setConfig(XmlElement xml)
        {
        m_xmlConfig = xml;
        }

    /**
     * {@inheritDoc}
     */
    public XmlElement getConfig()
        {
        return m_xmlConfig;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return "ConfigurableAddressProviderFactory{Xml=" + getConfig() + "}";
        }

    // ---- data members ----------------------------------------------------

    /**
     * XML configuration for this ConfigurableAddressProviderFactory.
     */
    private XmlElement m_xmlConfig;
    }
