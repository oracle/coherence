/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.run.xml.XmlElement;

import java.net.InetSocketAddress;

/**
 * An {@link ElementProcessor} that parses a &lt;local-address&gt; to produce
 * an {@link InetSocketAddress}.
 *
 * @author bo  2013.07.16
 * @since Coherence 12.1.3
 */
@XmlSimpleName("local-address")
public class LocalAddressProcessor
        implements ElementProcessor<InetSocketAddress>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // TODO: refactor this to remove the need to use the legacy helper clas
        return LegacyXmlConfigHelper.parseLocalSocketAddress(xmlElement);
        }
    }
