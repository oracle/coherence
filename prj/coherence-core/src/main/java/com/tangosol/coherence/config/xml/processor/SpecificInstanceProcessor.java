/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that will process an {@link XmlElement} defining
 * a {@link ParameterizedBuilder}, after which it will eagerly realized to produce
 * an instance of the required type.
 *
 * @author bo  2013.03.11
 * @since Coherence 12.1.3
 */
public class SpecificInstanceProcessor<T>
        extends AbstractEmptyElementProcessor<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SpecificInstanceProcessor} for the specified {@link Class}.
     *
     * @param clzToRealize  the class that will be instantiated, injected and
     *                      returned during processing
     */
    public SpecificInstanceProcessor(Class<T> clzToRealize)
        {
        m_clzToRealize = clzToRealize;
        }

    /**
     * Constructs a {@link SpecificInstanceProcessor} for the specified {@link Class}.
     *
     * @param clzToRealize   the class that will be instantiated, injected and
     *                       returned during processing
     * @param behavior       the {@link AbstractEmptyElementProcessor.EmptyElementBehavior} when an empty
     *                       {@link XmlElement} is encountered
     */
    public SpecificInstanceProcessor(Class<T> clzToRealize, EmptyElementBehavior behavior)
        {
        super(behavior);
        m_clzToRealize = clzToRealize;
        }

    /**
     * Constructs a {@link SpecificInstanceProcessor} for the specified {@link Class}.
     *
     * @param clzToRealize   the class that will be instantiated, injected and
     *                       returned during processing
     * @param oDefaultValue  the value to return when an empty {@link XmlElement}
     *                       is encountered
     */
    public SpecificInstanceProcessor(Class<T> clzToRealize, T oDefaultValue)
        {
        super(oDefaultValue);
        m_clzToRealize = clzToRealize;
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public T onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            throw new ConfigurationException("Invalid <" + xmlElement.getName() + "> declaration in [" + xmlElement
                                             + "]", "Please specify a <" + xmlElement.getName()
                                                 + "> that will produce a " + m_clzToRealize.getName());
            }
        else
            {
            Object instance = bldr.realize(context.getDefaultParameterResolver(), context.getContextClassLoader(),
                                            null);
            return m_clzToRealize.isAssignableFrom(instance.getClass()) ? (T) instance : null;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Class} to create, inject (from the {@link com.tangosol.run.xml.XmlElement} being processed) and return.
     */
    private Class<T> m_clzToRealize;
    }
