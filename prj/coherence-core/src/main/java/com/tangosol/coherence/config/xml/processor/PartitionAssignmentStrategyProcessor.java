/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.PartitionAssignmentStrategyBuilder;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;

/**
 * An ElementProcessor to process a &lt;partition-assignment-strategy&gt; to
 * produce a PartitionAssignmentStrategy.
 *
 * @author bo  2013.03.12
 * @since Coherence 12.1.3
 */
@XmlSimpleName("partition-assignment-strategy")
public class PartitionAssignmentStrategyProcessor
        implements ElementProcessor<PartitionAssignmentStrategyBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionAssignmentStrategyBuilder process(ProcessingContext context, XmlElement xmlElement)
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        return bldr instanceof ParameterizedBuilder ?
                new PartitionAssignmentStrategyBuilder(bldr, xmlElement) :
                new PartitionAssignmentStrategyBuilder(xmlElement.getString(), xmlElement);
        }
    }