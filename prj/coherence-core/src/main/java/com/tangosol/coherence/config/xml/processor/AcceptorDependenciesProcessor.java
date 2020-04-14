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

import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;

import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
import com.tangosol.run.xml.XmlElement;

import java.util.Iterator;
import java.util.List;

/**
 * An {@link ElementProcessor} that will parse an &lt;acceptor-config&gt; and
 * produce an {@link AcceptorDependencies}.
 *
 * @author bo  2013.07.02
 * @since Coherence 12.1.3
 */
@XmlSimpleName("acceptor-config")
public class AcceptorDependenciesProcessor
        implements ElementProcessor<AcceptorDependencies>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public AcceptorDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // locate the definition of an '-acceptor' element and attempt to parse it
        XmlElement xmlAcceptor = null;

        for (Iterator<XmlElement> iter = ((List<XmlElement>) xmlElement.getElementList()).iterator();
            iter.hasNext() && xmlAcceptor == null; )
            {
            XmlElement xmlCurrent = iter.next();

            if (xmlCurrent.getName().endsWith("-acceptor"))
                {
                xmlAcceptor = xmlCurrent;
                }
            }

        if (xmlAcceptor == null)
            {
            // Use the default TCP acceptor if the config is missing.
            AcceptorDependencies dependencies = new DefaultTcpAcceptorDependencies();
            context.inject(dependencies, xmlElement);
            return dependencies;
            }
        else
            {
            // process the acceptor to produce the dependencies
            Object oAcceptorDependencies = context.processElement(xmlAcceptor);

            if (oAcceptorDependencies instanceof AcceptorDependencies)
                {
                AcceptorDependencies dependencies = (AcceptorDependencies) oAcceptorDependencies;

                // now inject properties from this (parent) element into the
                // acceptor dependencies.  this allows the dependencies to
                // "inherit" common properties from this element
                context.inject(dependencies, xmlElement);

                return dependencies;
                }
            else
                {
                throw new ConfigurationException("The specified acceptor configuration [" + xmlAcceptor
                    + "] does not produce an AcceptorDependencies", "Please ensure that the acceptor produces an AcceptorDependencies implementation.");
                }
            }
        }
    }
