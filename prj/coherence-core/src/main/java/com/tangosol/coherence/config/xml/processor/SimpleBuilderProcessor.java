/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} that returns a
 * simple {@link ParameterizedBuilder}.
 *
 * @param <T>  the type of the builder
 *
 * @author Jonathan Knight  2022.03.01
 * @since 22.06
 */
public class SimpleBuilderProcessor<T>
        extends AbstractEmptyElementProcessor<ParameterizedBuilder<T>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SimpleBuilderProcessor}.
     */
    public SimpleBuilderProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ParameterizedBuilder<T> onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        ParameterizedBuilder bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            String sName = xmlElement.getName();
            throw new ConfigurationException("<" + sName + "> fails to correctly define a ParameterizedBuilder implementation: "
                                             + xmlElement, "Please define a <" + sName + ">");
            }

        return (ParameterizedBuilder<T>) bldr;
        }
    }
