/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import java.util.Iterator;
import java.util.List;

import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.TcpDatagramSocketProvider;

import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlSocketProviderDependencies parses XML to populate a SocketProviderDependencies
 * object.
 *
 * @author bb 2011.11.21
 * @since Coherence 12.1.2
 */
public class LegacyXmlSocketProviderFactoryDependencies
        extends SocketProviderFactory.DefaultDependencies
    {
    /**
     * Construct SocketProviderDependencies based on xml config
     * This constructor is to parse <socket-providers> element from operational-config
     * 
     * @param xml Config XmlElement
     */
    public LegacyXmlSocketProviderFactoryDependencies(XmlElement xml)
        {
        if (xml == null)
            {
            throw new IllegalArgumentException("Missing configuration");
            }
        else if (xml.getName().equals(XML_NAME))
            {
            for (Iterator iter = xml.getElements(XML_PROVIDER_NAME); iter.hasNext(); )
                {
                XmlElement xmlSub = (XmlElement) iter.next();
                String     sId    = xmlSub.getSafeAttribute("id").getString();
                if (sId.length() == 0)
                    {
                    throw new IllegalArgumentException("id attribute required for: " + xmlSub);
                    }
                else if (!xmlSub.getName().equals(XML_PROVIDER_NAME))
                    {
                    throw new IllegalArgumentException("unknown element: " + xmlSub);
                    }
                parseConfig(sId, xmlSub);
                }
            }
        else
            {
            throw new IllegalArgumentException("Invalid configuration: " + xml);
            }
        }
    
    /**
     * Construct SocketProviderDependencies based on xml config
     * This constructor is to parse unnamed <socket-provider> element 
     * 
     * @param providerId ProviderId for the socket-provider definition
     * @param xml        SocketProvider xml config 
     */
    public LegacyXmlSocketProviderFactoryDependencies(String providerId, XmlElement xml)
        {
        if (xml == null)
            {
            throw new IllegalArgumentException("Missing configuration");
            }
        else if (xml.getName().equals(XML_PROVIDER_NAME))
            {
            parseConfig(providerId, xml);
            }
        else
            {
            throw new IllegalArgumentException("Invalid configuration: " + xml);
            }
        }
    
    /**
     * Returns the Socket provider name from the given XmlElement snippet
     * 
     * @param  xml XmlElement snippet
     * @return Socket provider name      
     */
    @Deprecated
    public static String getProviderId(XmlElement xml)
        {
        String sRef;
        if (xml == null)
            {
           return null;
            }
        else if (xml.getElementList().isEmpty())
            {
            // socket-provider reference
            sRef = xml.getString();
            if (sRef.length() == 0)
                {
                // empty configuration
                return null;
                }
            }
        else
            {
            return SocketProviderFactory.UNNAMED_PROVIDER_ID;
            }
        return sRef;
        }
    
    /**
     * Parse socket-provider xml config
     * 
     * @param id  SocketProviderId
     * @param xml XmlElement config
     */
    protected void parseConfig(String id, XmlElement xml)
        {
        List listProviders = xml.getElementList();
        for (Iterator itr = xml.getElementList().iterator(); itr.hasNext();)
            {
            XmlElement xmlProvider = (XmlElement) itr.next();
            String     sType       = xmlProvider.getName();
            if (sType.equals(ProviderType.SYSTEM.getName()))
                {
                m_mapProvider.put(id, ProviderType.SYSTEM);
                }
            else if (sType.equals(ProviderType.TCP.getName()))
                {
                //parse tcp datagram config
                m_mapTCPDatagramDependencies.put(id, new LegacyXmlTcpDatagramSocketDependencies(xmlProvider));
                m_mapProvider.put(id, ProviderType.TCP);
                }
            else if (sType.equals(ProviderType.SSL.getName()))
                {
                m_mapSSLDependencies.put(id, new LegacyXmlSSLSocketProviderDependencies(xmlProvider, this));
                m_mapProvider.put(id, ProviderType.SSL);
                // check if datagram-socket element is present
                XmlElement xmlCat = xmlProvider.getElement("datagram-socket");
                if (xmlCat != null)
                    {
                    TcpDatagramSocketProvider.DefaultDependencies dependencies = 
                            new TcpDatagramSocketProvider.DefaultDependencies();
                    
                    XmlElement xmlSub;
                    if ((xmlSub = xmlCat.getElement("blocking")) != null)
                        {
                        // the use of "blocking" datagram sockets is not meant for production
                        // use and this setting should remain undocumented
                        dependencies.setBlocking(xmlSub.getBoolean(true));
                        }
                    if ((xmlSub = xmlCat.getElement("advance-frequency")) != null)
                        {
                        dependencies.setAdvanceFrequency(xmlSub.getInt());
                        }
                    m_mapTCPDatagramDependencies.put(id, dependencies);
                    }
                }
            else if (sType.equals(ProviderType.SDP.getName()))
                {
                m_mapProvider.put(id, ProviderType.SDP);
                }
            else
                {
                throw new IllegalArgumentException("Unsupported socket provider: "+sType);
                }
            }
        }
    
    
    // ----- data members ---------------------------------------------------

    /**
    * The Xml configuration.
    */
    protected XmlElement m_xml;
   
    // ----- constants ------------------------------------------------------
    
    /**
    * The name of the XmlElement in which SocketProviders are specified.
    */
    public static final String XML_NAME = "socket-providers";

    /**
    * The name of the XmlElement in which each SocketProvider is specified.
    */
    public static final String XML_PROVIDER_NAME = "socket-provider";
    }
