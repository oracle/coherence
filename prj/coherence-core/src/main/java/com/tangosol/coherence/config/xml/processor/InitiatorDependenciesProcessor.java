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
import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;

import com.tangosol.run.xml.XmlElement;

import java.util.Iterator;
import java.util.List;

/**
 * An {@link ElementProcessor} that will parse an &lt;initator-config&gt; and
 * produce an {@link InitiatorDependencies}.
 *
 * @author bo  2013.07.16
 * @since Coherence 12.1.3
 */
@XmlSimpleName("initiator-config")
public class InitiatorDependenciesProcessor
        implements ElementProcessor<InitiatorDependencies>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public InitiatorDependencies process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // locate the definition of an '-initiator' element and attempt to parse it
        XmlElement xmlInitiator = null;

        for (Iterator<XmlElement> iter = ((List<XmlElement>) xmlElement.getElementList()).iterator();
            iter.hasNext() && xmlInitiator == null; )
            {
            XmlElement xmlCurrent = iter.next();

            if (xmlCurrent.getName().endsWith("-initiator"))
                {
                xmlInitiator = xmlCurrent;
                }
            }

        // process the initiator to produce the dependencies
        Object oInitiatorDependencies = xmlInitiator == null
                ? new DefaultTcpInitiatorDependencies()
                : context.processElement(xmlInitiator);

        if (oInitiatorDependencies instanceof InitiatorDependencies)
            {
            InitiatorDependencies dependencies = (InitiatorDependencies) oInitiatorDependencies;

            // now inject properties from this (parent) element into the
            // initiator dependencies.  this allows the dependencies to
            // "inherit" common properties from this element
            context.inject(dependencies, xmlElement);

            return dependencies;
            }
        else
            {
            throw new ConfigurationException("The specified <initiator-config> element [" + xmlElement
                + "] does not produce an InitiatorDependencies", "Please ensure that the initiator produces an InitiatorDependencies implementation.");
            }
        }
    }
