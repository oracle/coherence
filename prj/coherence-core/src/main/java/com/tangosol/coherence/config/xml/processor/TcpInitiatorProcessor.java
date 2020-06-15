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

import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that will parse an &lt;tcp-initiator&gt; and
 * produce a TcpInitiatorDependencies object.
 *
 * @author pfm  2013.08.28
 * @since Coherence 12.1.3
 */
@XmlSimpleName("tcp-initiator")
public class TcpInitiatorProcessor
        implements ElementProcessor<TcpInitiatorDependencies>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public TcpInitiatorDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        DefaultTcpInitiatorDependencies deps = new DefaultTcpInitiatorDependencies();
        context.inject(deps, xmlElement);

        SocketOptionsProcessor procSocketOptions = new SocketOptionsProcessor();
        deps.setSocketOptions(procSocketOptions.process(context, xmlElement));
        return deps;
        }
    }
