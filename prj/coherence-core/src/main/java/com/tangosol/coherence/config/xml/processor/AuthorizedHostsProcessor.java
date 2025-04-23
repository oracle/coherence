/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

import java.util.Arrays;
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
     * An {@link ElementProcessor} for &lt;authorized-hosts&gt; Configuration Elements.
     * <p>
     * Since 25.03.1, the {@code authorized-hosts.host-address} value can be either an IP host address or
     * a comma separated list of IP host addresses.
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
                String hostString = ((XmlElement) iter.next()).getString();

                if (hostString.contains(","))
                    {
                    Arrays.stream(hostString.split(","))
                            .map(String::trim)
                            .forEach(s -> builder.addAuthorizedHostsToFilter(s, null));
                    }
                else
                    {
                    builder.addAuthorizedHostsToFilter(hostString, null);
                    }
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
