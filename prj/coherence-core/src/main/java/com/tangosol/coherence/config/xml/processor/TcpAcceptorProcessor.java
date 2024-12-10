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

import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that will parse an &lt;tcp-acceptor&gt; and
 * produce a TcpAcceptorDependencies object.
 *
 * @author pfm  2013.08.28
 * @since Coherence 12.1.3
 */
@XmlSimpleName("tcp-acceptor")
public class TcpAcceptorProcessor
        implements ElementProcessor<TcpAcceptorDependencies>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public TcpAcceptorDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        DefaultTcpAcceptorDependencies deps = new DefaultTcpAcceptorDependencies();
        context.inject(deps, xmlElement);

        SocketOptionsProcessor procSocketOptions = new SocketOptionsProcessor();
        deps.setSocketOptions(procSocketOptions.process(context, xmlElement));
        return deps;
        }
    }
