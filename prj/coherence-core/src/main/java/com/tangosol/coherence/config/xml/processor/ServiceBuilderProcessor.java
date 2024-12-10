/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ServiceBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * A {@link ServiceBuilderProcessor} is an {@link ElementProcessor} responsible for producing various kinds of
 * {@link ServiceBuilder}s.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class ServiceBuilderProcessor<T extends ServiceBuilder>
        implements ElementProcessor<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ServiceBuilderProcessor} for the specified {@link Class} of {@link ServiceBuilder}.
     *
     * @param clzToRealize  the class that will be instantiated, injected and returned during processing
     */
    public ServiceBuilderProcessor(Class<T> clzToRealize)
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
        // Create the ServiceBuilder and set the XML which contains the service
        // configuration.  The ServiceBuilder.realizeService method will pass that
        // XML to SafeCluster.ensureService.  In some future release, the ServiceBuilder
        // will have its properties injected by CODI, like the scheme builders.  At
        // that time, the XML will no longer be needed.
        T bldr = instantiate();

        //TODO: remove the following line once dependencies are injectable (remove in 12.2.1)
        bldr.setXml(element);

        // Inject all annotated properties into the ServiceBuilder.  Only a few of
        // the service properties are annotated, the rest are in the form of XML as
        // previously stated.
        context.inject(bldr, element);

        return bldr;
        }

    // ----- ServiceBuilderProcessor methods --------------------------------

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
