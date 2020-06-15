/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.util.List;

import static com.tangosol.util.BuilderHelper.using;

/**
 * The {@link DefaultsProcessor} is responsible for processing the &lt;defaults&gt; {@link XmlElement}
 * in a Coherence Cache Configuration file, registering each of the processed elements with
 * the {@link com.tangosol.util.ResourceRegistry}.
 *
 * @author bo  2013.12.01
 * @since Coherence 12.1.3
 */
@XmlSimpleName("defaults")
public class DefaultsProcessor
        implements ElementProcessor<Void>
    {
    @Override
    public Void process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // acquire the resource registry into which we'll register default resources
        ResourceRegistry registry = context.getResourceRegistry();

        // iterate over the child xml elements, each of which produces a resource
        List<XmlElement> listChildren = element.getElementList();

        for (XmlElement xmlChild : listChildren)
            {
            // process the child to produce a resource
            Object oResource = context.processElement(xmlChild);

            // only register non-null resources
            if (oResource != null)
                {
                // the name of the xml element will be the default name of the resource
                String sResourceName = xmlChild.getQualifiedName().getLocalName();
                Class  clzResource   = oResource.getClass();

                // register the resource (as the class) using replace to ensure values set here override
                registry.registerResource(clzResource, sResourceName, using(oResource), RegistrationBehavior.REPLACE, null);

                // register the resource (for each non-java interface that it implements)
                for (Class clzInterface : clzResource.getInterfaces())
                    {
                    if (isRegisterable(clzInterface))
                        {
                        registry.registerResource(clzInterface, sResourceName, using(oResource), RegistrationBehavior.REPLACE, null);
                        }
                    }
                }
            }

        // nothing to return
        return null;
        }

    /**
     * Determines if the specified {@link Class} of resource should be registered
     * with a {@link ResourceRegistry}.
     *
     * @param clzResource  the {@link Class} of resource
     *
     * @return  <code>true</code> if the resource should be registered
     */
    protected boolean isRegisterable(Class<?> clzResource)
        {
        // 1. We don't want to register resources using java platform interfaces
        // (ie: resources that implement java interface)

        // 2. We don't want to register resources that are deprecated (like XmlConfigurable)
        return clzResource != null && !clzResource.getName().startsWith("java")
               && !clzResource.equals(XmlConfigurable.class);
        }
    }
