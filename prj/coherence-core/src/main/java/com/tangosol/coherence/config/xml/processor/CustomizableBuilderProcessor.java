/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.BuilderCustomization;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;


import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * A {@link CustomizableBuilderProcessor} is a multi-purpose {@link ElementProcessor}
 * responsible for processing xml elements that produce objects supporting {@link BuilderCustomization}.
 *
 * @author pfm  2011.11.30
 * @author bo   2012.02.09
 *
 * @since Coherence 12.1.2
 */
public class CustomizableBuilderProcessor<T>
        implements ElementProcessor<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CustomizableBuilderProcessor} for the specified {@link Class}.
     *
     * @param clzToRealize  the class that will be instantiated, injected and returned during processing
     */
    public CustomizableBuilderProcessor(Class<T> clzToRealize)
        {
        m_clzToRealize = clzToRealize;
        }

    // ----- ElementProcessor methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public T process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // create the target object to be injected
        T target = instantiate();

        // should it support builder customization, configure the custom builder from the element
        if (target instanceof BuilderCustomization)
            {
            // get the parameterized builder from the element
            ParameterizedBuilder<?> bldrCustom = ElementProcessorHelper.processParameterizedBuilder(context, element);

            // inject the custom builder into the target
            ((BuilderCustomization) target).setCustomBuilder(bldrCustom);
            }

        // now inject the target with the values in the element
        return context.inject(target, element);
        }

    // ----- CustomizableBuilderProcessor methods ---------------------------

    /**
     * Instantiate the required class to inject and return.
     *
     * @return object to be injected
     */
    protected T instantiate()
        {
        try
            {
            return m_clzToRealize.newInstance();
            }
        catch (InstantiationException e)
            {
            throw Base.ensureRuntimeException(e, "Failed to instantiate " + m_clzToRealize
                                              + ".  Please ensure it has a public no args constructor.");
            }
        catch (IllegalAccessException e)
            {
            throw Base.ensureRuntimeException(e, "Failed to instantiate " + m_clzToRealize
                                              + ". Please ensure it has a public no args constructor.");
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Class} to create, inject (from the {@link XmlElement} being processed) and return.
     */
    private Class<T> m_clzToRealize;
    }
