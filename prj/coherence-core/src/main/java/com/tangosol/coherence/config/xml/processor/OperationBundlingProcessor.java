/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.scheme.BundleManager;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;


import com.tangosol.run.xml.XmlElement;

import java.util.List;

/**
 * A {@link OperationBundlingProcessor} is responsible for processing an
 * operation-bundling {@link XmlElement} to produce an {@link BundleManager}.
 *
 * @author pfm  2011.12.02
 * @since Coherence 12.1.2
 */
@XmlSimpleName("operation-bundling")
public class OperationBundlingProcessor
        implements ElementProcessor<BundleManager>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public BundleManager process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        BundleManager manager = new BundleManager();

        // for each bundle-config child, create and populate a BundleConfig
        // and add it to the manager
        List<XmlElement> xmlChildren = element.getElementList();
        for (XmlElement xmlChild : xmlChildren)
            {
            if (xmlChild.getName().equals("bundle-config"))
                {
                BundleManager.BundleConfig config = new BundleManager.BundleConfig();
                context.inject(config, xmlChild);
                manager.addConfig(config);
                }
            }

        return manager;
        }
    }
