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

import com.tangosol.net.partition.PartitionListener;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * An ElementProcessor to process a &lt;partition-listener&gt; to produce a
 * List containing a single PartitionListener.
 *
 * @author bo  2013.03.12
 * @since Coherence 12.1.3
 */
@XmlSimpleName("partition-listener")
public class PartitionListenerProcessor
        extends AbstractEmptyElementProcessor<List<ParameterizedBuilder<PartitionListener>>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PartitionListenerProcessor}.
     */
    public PartitionListenerProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterizedBuilder<PartitionListener>> onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr instanceof ParameterizedBuilder)
            {
            ArrayList<ParameterizedBuilder<PartitionListener>> listBuildersListener =
                new ArrayList<ParameterizedBuilder<PartitionListener>>();

            listBuildersListener.add((ParameterizedBuilder<PartitionListener>) bldr);

            return listBuildersListener;
            }
        else
            {
            throw new ConfigurationException("Invalid <" + xmlElement.getName() + "> declaration in [" + xmlElement
                                             + "]", "Please specify a <" + xmlElement.getName()
                                                 + "> that will produce a PartitionListener");
            }
        }
    }
