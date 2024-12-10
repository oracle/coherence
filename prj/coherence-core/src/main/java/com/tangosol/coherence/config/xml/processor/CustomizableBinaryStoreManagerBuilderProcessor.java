/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilderCustomization;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;


import com.tangosol.run.xml.XmlElement;

/**
 * A {@link CustomizableBinaryStoreManagerBuilderProcessor} is a {@link CustomizableBuilderProcessor} that additionally
 * processes the required definition of a {@link BinaryStoreManagerBuilder} for those classes supporting
 * {@link BinaryStoreManagerBuilderCustomization}.
 *
 * @author bo  2012.02.09
 * @since Coherence 12.1.2
 */
public class CustomizableBinaryStoreManagerBuilderProcessor<T extends BinaryStoreManagerBuilderCustomization>
        extends CustomizableBuilderProcessor<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CustomizableBinaryStoreManagerBuilderProcessor} for the specified
     * {@link BinaryStoreManagerBuilderCustomization} {@link Class}.
     *
     * @param clzToRealize  the class that will be instantiated, injected and returned during processing
     */
    public CustomizableBinaryStoreManagerBuilderProcessor(Class<T> clzToRealize)
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
        T      bldr       = super.process(context, element);
        Object oProcessed = context.processRemainingElementOf(element);

        if (oProcessed instanceof BinaryStoreManagerBuilder)
            {
            bldr.setBinaryStoreManagerBuilder((BinaryStoreManagerBuilder) oProcessed);
            }
        else
            {
            throw new ConfigurationException(String.format(
                "%s is missing the definition of an appropriate <*-store-manager>.",
                element), "Please ensure that a store manager is correctly defined");
            }

        return bldr;
        }
    }
