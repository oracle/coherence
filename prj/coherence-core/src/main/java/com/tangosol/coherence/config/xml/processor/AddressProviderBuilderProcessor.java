/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.CustomAddressProviderBuilder;
import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ListBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.LocalAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.List;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} that will parse and produce a
 * ParameterizedBuilder&lt;AddressProvider&gt; based on an address-provider configuration element,
 * that of which is defined as such (with support for foreign-namespaces)
 * <pre>
 *   &lt;!ELEMENT ... (socket-address+ | address-provider)&gt;
 *   &lt;!ELEMENT address-provider
 *     (class-name | (class-factory-name, method-name), init-params?&gt;
 *   &lt;!ELEMENT socket-address (address, port)&gt;
 * </pre>
 *
 * @author bo  2013.03.07
 * @since Coherence 12.1.3
 */
public class AddressProviderBuilderProcessor
        implements ElementProcessor<ParameterizedBuilder<AddressProvider>>
    {
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddressProviderBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        AddressProviderBuilder bldrAddressProvider = null;

        if (XmlHelper.hasElement(xmlElement, "address-provider"))
            {
            xmlElement = xmlElement.getElement("address-provider");
            }

        // assume a custom builder has been provided
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            List<XmlElement> listElements = xmlElement.getElementList();

            if (listElements.size() == 0)
                {
                // when the element is empty assume it's a string value
                // representing a named and registered address provider
                // (e.g. <address-provider>pof</address-provider>).
                String sName = xmlElement.getString().trim();

                // grab the operational context from which we can look it up
                OperationalContext ctxOperational = context.getCookie(OperationalContext.class);

                if (ctxOperational == null)
                    {
                    ConfigurationException e =
                        new ConfigurationException("Attempted to resolve the OperationalContext in [" + xmlElement
                            + "] but it was not defined", "The registered ElementHandler for the <"
                                + xmlElement.getName() + "> element is not operating in an OperationalContext");

                    bldrAddressProvider = new FactoryBasedAddressProviderBuilder(null).setDeferredException(e);
                    }
                else
                    {
                    // resolve the named provider factory
                    AddressProviderFactory factory = ctxOperational.getAddressProviderMap().get(sName);

                    if (factory == null)
                        {
                        Logger.warn("The address provider named '" + sName + "', specified in [" + xmlElement
                                    + "], cannot be found. Please ensure that the address provider is correctly defined "
                                    + "in the operational configuration override file");

                        // return null.  All 3 @Injectable("address-provider") methods can handle null as an AddressProviderBuilder.
                        // DefaultTcpAcceptorDependencies.getLoaderAddressProviderBuilder() defaults to NS local address.
                        }
                    else
                        {
                        bldrAddressProvider = new FactoryBasedAddressProviderBuilder(factory);
                        }
                    }
                }
            else if (listElements.size() == 1 && XmlHelper.hasElement(xmlElement, "local-address"))
                {
                bldrAddressProvider = newLocalAddressProviderBuilder(listElements.get(0));
                }
            else
                {
                ListBasedAddressProviderBuilder bldrAddressListProvider = new ListBasedAddressProviderBuilder();

                for (XmlElement xmlAddr : (List<XmlElement>) xmlElement.getElementList())
                    {
                    String sAddr;
                    int    nPort;

                    switch (xmlAddr.getName())
                        {
                        case "socket-address" :
                            if (xmlElement.getName().equalsIgnoreCase("well-known-addresses"))
                                {
                                Logger.warn("The use of <socket-address> for the <well-known-addresses> element is deprecated and the <port> value is ignored. Use <address> instead.");
                                }

                            sAddr = xmlAddr.getSafeElement("address").getString().trim();
                            nPort = xmlAddr.getSafeElement("port").getInt();
                            break;

                        case "host-address" :
                        case "address" :
                            sAddr = xmlAddr.getString().trim();
                            nPort = 0;
                            if (xmlElement.getName().equals("name-service-addresses"))
                                {
                                nPort = CacheFactory.getCluster().getDependencies().getGroupPort();
                                }
                            break;

                        default :
                            continue;
                        }

                    if (sAddr.isEmpty())
                        {
                        // ignore empty elements
                        continue;
                        }

                    bldrAddressListProvider.add(sAddr, nPort);
                    }

                if (!bldrAddressListProvider.isEmpty())
                    {
                    bldrAddressProvider = bldrAddressListProvider;
                    }
                }
            }
        else
            {
            bldrAddressProvider = new CustomAddressProviderBuilder((ParameterizedBuilder<AddressProvider>) bldr,
                context.getDefaultParameterResolver(), xmlElement);
            }

        return bldrAddressProvider;
        }

    /**
     * Build a new AddressProviderBuilder for the local-address.
     *
     * @param xmlLocalAddress  the {@link XmlElement} to process
     *
     * @return the newly constructed AddressProviderBuilder
     */
    public static AddressProviderBuilder newLocalAddressProviderBuilder(XmlElement xmlLocalAddress)
        {
        XmlElement xmlAddress    = xmlLocalAddress.getSafeElement("address");
        XmlElement xmlPort       = xmlLocalAddress.getSafeElement("port");
        XmlElement xmlPortAdjust = xmlLocalAddress.getSafeElement("port-auto-adjust");
        String     sAddress      = xmlAddress.getString().trim();
        String     sPort         = xmlPort.getString("").trim();
                                   // -1: use ephemeral subport
        int        nPort         = sPort.isEmpty() ? -1 : xmlPort.getInt();
        String     sPortAdjust   = xmlPortAdjust.getString("true").trim();
        // if nPort is ephemeral (-1 or 0), don't adjust
        int nPortAdjust = nPort <= 0
            ? nPort
            : sPortAdjust.compareToIgnoreCase("true") == 0
                  ? LocalAddressProviderBuilder.MAX_PORT
                  : sPortAdjust.compareToIgnoreCase("false") == 0 ? nPort : Integer.parseInt(sPortAdjust);

        return new LocalAddressProviderBuilder(sAddress, nPort, nPortAdjust, xmlLocalAddress);
        }
    }
