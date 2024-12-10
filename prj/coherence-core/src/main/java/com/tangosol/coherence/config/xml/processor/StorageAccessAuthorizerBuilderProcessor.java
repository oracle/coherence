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
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} for &lt;storage-authorizer&gt; elements defined by
 * a Coherence Operational Configuration file.
 *
 * @author bo  2014.10.27
 * @since Coherence 12.1.3
 */
@XmlSimpleName("storage-authorizer")
public class StorageAccessAuthorizerBuilderProcessor
        extends AbstractEmptyElementProcessor<ParameterizedBuilder<StorageAccessAuthorizer>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link StorageAccessAuthorizerBuilderProcessor}.
     */
    public StorageAccessAuthorizerBuilderProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    @Override
    public ParameterizedBuilder<StorageAccessAuthorizer> onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // assume the <storage-authorizer> contains a builder definition
        ParameterizedBuilder bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            throw new ConfigurationException("<storage-authorizer> fails to correctly define a StorageAccessAuthorizer implementation: "
                                             + xmlElement, "Please define a <storage-authorizer>");
            }

        return (ParameterizedBuilder<StorageAccessAuthorizer>) bldr;
        }
    }
