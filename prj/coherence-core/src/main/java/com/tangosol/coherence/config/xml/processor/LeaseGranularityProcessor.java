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

import com.tangosol.internal.net.service.grid.LeaseConfig;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} for Coherence &lt;lease-granularity&gt;
 * configuration
 *
 * @author bo  2013.04.01
 * @since Coherence 12.1.3
 */
@XmlSimpleName("lease-granularity")
public class LeaseGranularityProcessor
        implements ElementProcessor<Integer>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public Integer process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        String sLeaseGranularity = xmlElement.getString("").trim().toLowerCase();

        if (sLeaseGranularity.equals("member"))
            {
            return LeaseConfig.LEASE_BY_MEMBER;
            }
        else if (sLeaseGranularity.equals("thread"))
            {
            return LeaseConfig.LEASE_BY_THREAD;
            }
        else
            {
            throw new ConfigurationException("Invalid <" + xmlElement.getName() + "> value of [" + sLeaseGranularity
                                             + "]", "Please specify either 'member' or 'thread'.");
            }
        }
    }
