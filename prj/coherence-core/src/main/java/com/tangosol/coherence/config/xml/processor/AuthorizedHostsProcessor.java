/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.InetAddressRangeFilterBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Filter;

import java.util.Iterator;

/**
 * An {@link ElementProcessor} for &lt;authorized-hosts&gt; Configuration
 * Elements.
 *
 * @author bo  2013.07.10
 */
@XmlSimpleName("authorized-hosts")
public class AuthorizedHostsProcessor
        implements ElementProcessor<ParameterizedBuilder<Filter>>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<Filter> process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        XmlElement xmlHostsFilter = xmlElement.getElement("host-filter");

        if (xmlHostsFilter != null && !XmlHelper.isEmpty(xmlHostsFilter))
            {
            // create a ParameterizedBuilder for the host-filter
            ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlHostsFilter);

            return (ParameterizedBuilder<Filter>) bldr;
            }
        else
            {
            InetAddressRangeFilterBuilder builder = new InetAddressRangeFilterBuilder();

            // <host-address>
            for (Iterator iter = xmlElement.getElements("host-address"); iter.hasNext(); )
                {
                XmlElement xmlHost = (XmlElement) iter.next();

                builder.addAuthorizedHostsToFilter(xmlHost.getString(), null);
                }

            // <host-range>
            for (Iterator iter = xmlElement.getElements("host-range"); iter.hasNext(); )
                {
                XmlElement xmlHost = (XmlElement) iter.next();

                builder.addAuthorizedHostsToFilter(xmlHost.getSafeElement("from-address").getString(),
                                                   xmlHost.getSafeElement("to-address").getString());
                }

            return builder;
            }
        }
    }
