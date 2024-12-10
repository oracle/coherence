/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.MemberListener;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * An ElementProcessor to process a &lt;member-listener&gt; to produce a
 * List containing a single MemberListener.
 *
 * @author bo  2013.03.08
 * @since Coherence 12.1.3
 */
@XmlSimpleName("member-listener")
public class MemberListenerProcessor
        extends AbstractEmptyElementProcessor<List<ParameterizedBuilder<MemberListener>>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link MemberListenerProcessor}.
     */
    public MemberListenerProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterizedBuilder<MemberListener>> onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr instanceof ParameterizedBuilder)
            {
            try
                {
                ArrayList<ParameterizedBuilder<MemberListener>> listBuilders = new ArrayList<ParameterizedBuilder<MemberListener>>();
                listBuilders.add((ParameterizedBuilder<MemberListener>)bldr);
                return listBuilders;
                }
            catch (Exception e)

                {
                throw new ConfigurationException("Invalid <" + xmlElement.getName()
                    + "> declaration.  The specified builder doesn't produce a MemberListener in [" + xmlElement
                    + "]", "Please specify a <" + xmlElement.getName() + ">", e);
                }
            }
        else
            {
            throw new ConfigurationException("Invalid <" + xmlElement.getName() + "> declaration in [" + xmlElement
                                             + "]", "Please specify a <" + xmlElement.getName()
                                                 + "> that will produce a MemberListener");
            }
        }
    }
