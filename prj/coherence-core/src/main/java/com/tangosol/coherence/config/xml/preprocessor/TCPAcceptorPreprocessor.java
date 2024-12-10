/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.List;


/**
 * A {@link TCPAcceptorPreprocessor} is an {@link ElementPreprocessor} that
 * introduces re-writes &lt;tcp-acceptor&gt; configuration elements, in
 * particular &lt;local-address&gt; declarations into an &lt;address-provider&gt; containing
 * a &lt;local-address&gt;.
 *
 * @author bko  2013.07.03
 * @since Coherence 12.1.3
 */
public class TCPAcceptorPreprocessor
        implements ElementPreprocessor
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        if (xmlElement.getName().equals("tcp-acceptor"))
            {
            XmlElement xmlLocal    = xmlElement.getSafeElement("local-address");
            XmlElement xmlProvider = xmlElement.getSafeElement("address-provider");

            boolean    fLocal      = !XmlHelper.isEmpty(xmlLocal);
            boolean    fProvider   = !XmlHelper.isEmpty(xmlProvider);

            if (fLocal && fProvider)
                {
                throw new ConfigurationException("<address-provider> and "
                    + "<local-address> elements are mutually exclusive", "Please define one or the other.");
                }
            else if (fLocal)
                {
                // copy the <local-address> into an <address-provider>
                xmlProvider                = new SimpleElement("address-provider");
                XmlElement xmlLocalAddress = xmlProvider.addElement("local-address");

                for (XmlElement xmlChild : (List<XmlElement>) xmlLocal.getElementList())
                    {
                    xmlLocalAddress.ensureElement(xmlChild.getName()).setString(xmlChild.getString());
                    }

                // now remove the outer <local-address>
                xmlElement.getElementList().remove(xmlLocal);

                // now add the provider, and port-auto-adjust
                xmlElement.getElementList().add(xmlProvider);

                return true;
                }
            else
                {
                return false;
                }
            }
        else
            {
            return false;
            }
        }
    }
