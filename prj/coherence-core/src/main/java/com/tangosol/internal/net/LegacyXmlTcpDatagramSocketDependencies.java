/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.TcpDatagramSocketProvider;
import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlTcpDatagramSocketDependencies parses XML to populate a 
 * TcpDatagramSocketDependencies object.
 *
 * @author bb 2011.11.21
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlTcpDatagramSocketDependencies 
        extends TcpDatagramSocketProvider.DefaultDependencies
    {
    /**
     * Construct TcpDatagramSocketProvider Dependencies based on xml config
     * 
     * @param xml Config XmlElement
     */
     public LegacyXmlTcpDatagramSocketDependencies(XmlElement xml)
         {
         parseConfig(xml);
         }
    
    // ----- helpers ---------------------------------------------------
    
    /**
     * Parse datagram-socket xml config
     * 
     * @param xml config
     */
    @Deprecated
    protected void parseConfig(XmlElement xml)
        {
       if (xml == null)
            {
            return;
            }
        else if (!xml.getName().equals(XML_NAME))
            {
            throw new IllegalArgumentException("Invalid configuration: " + xml);
            }

        XmlElement xmlCat = xml.getSafeElement("datagram-socket");

        XmlElement xmlSub;
        if ((xmlSub = xmlCat.getElement("blocking")) != null)
            {
            // the use of "blocking" datagram sockets is not meant for production
            // use and this setting should remain undocumented
            m_fBlocking = xmlSub.getBoolean(true);
            }
        if ((xmlSub = xmlCat.getElement("advance-frequency")) != null)
            {
            m_nAdvanceFrequency = xmlSub.getInt();
            }
        }
    
    // ----- constants ------------------------------------------------------

    /**
    * The name of the XmlElement in which the provider configuration is
    * specified.
    */
    public static final String XML_NAME = "tcp";
    }
