/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.scheme.AbstractCompositeScheme;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;


import com.tangosol.run.xml.XmlElement;

/**
 * A {@link CompositeSchemeProcessor} is a {@link CustomizableBuilderProcessor} for schemes
 * that consist of a front and back scheme.
 *
 * @author bo  2012.02.09
 * @since Coherence 12.1.2
 */
public class CompositeSchemeProcessor<T extends AbstractCompositeScheme>
        extends CustomizableBuilderProcessor<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CompositeSchemeProcessor} for the specified {@link Class}.
     *
     * @param clzToRealize  the class that will be instantiated, injected and returned during processing
     */
    public CompositeSchemeProcessor(Class<T> clzToRealize)
        {
        super(clzToRealize);
        }

    // ----- ElementProcessor methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public T process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // create the composite cache and inject and InstanceBuilder if present
        T scheme = super.process(context, element);

        // find the front-scheme and inject it into the front CachingScheme
        XmlElement xmlFront = element.findElement("front-scheme");

        if (xmlFront != null)
            {
            Object oFront = context.processOnlyElementOf(xmlFront);

            if (oFront instanceof CachingScheme)
                {
                scheme.setFrontScheme((CachingScheme) oFront);
                }
            else
                {
                throw new ConfigurationException(String.format("<front-scheme> of %s is not a CachingScheme.", element),
                                                 "Please ensure the <front-scheme> is of the appropriate type");
                }
            }

        // find the back-scheme and inject it into the back CachingScheme
        XmlElement xmlBack = element.findElement("back-scheme");

        if (xmlBack != null)
            {
            Object oBack = context.processOnlyElementOf(xmlBack);

            if (oBack instanceof CachingScheme)
                {
                scheme.setBackScheme((CachingScheme) oBack);
                }
            else
                {
                throw new ConfigurationException(String.format("<back-scheme> of %s is not a CachingScheme.", element),
                                                 "Please ensure the <back-scheme> is of the appropriate type");
                }
            }

        return scheme;
        }
    }
