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

import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} for the &lt;buffer-type&gt; Cache Configuration
 * element.
 *
 * @author bo  2013.07.10
 */
@XmlSimpleName("buffer-type")
public class BufferTypeProcessor
        implements ElementProcessor<Integer>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public Integer process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        String sType = xmlElement.getString().trim();

        return sType.equalsIgnoreCase("heap")
               ? TcpAcceptorDependencies.BufferPoolConfig.TYPE_HEAP
               : TcpAcceptorDependencies.BufferPoolConfig.TYPE_DIRECT;
        }
    }
