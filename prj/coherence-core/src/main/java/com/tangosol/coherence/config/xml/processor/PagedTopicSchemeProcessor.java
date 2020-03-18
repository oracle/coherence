/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.scheme.BackingMapScheme;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import com.tangosol.coherence.config.scheme.PagedTopicStorageScheme;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.net.service.grid.DefaultPartitionedCacheDependencies;
import com.tangosol.internal.net.service.grid.PartitionedCacheDependencies;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.SafeConfigurablePofContext;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ExternalizableHelper;

/**
 * An {@link ElementProcessor} that parses a &lt;paged-topic-scheme&gt; element;
 * produces a {@link PagedTopicScheme}.
 *
 * @author jk  2015.05.28
 * @since Coherence 14.1.1
 */
@XmlSimpleName("paged-topic-scheme")
public class PagedTopicSchemeProcessor
        extends ServiceBuilderProcessor<PagedTopicScheme>
    {
    // ----- constructors ---------------------------------------------------

    public PagedTopicSchemeProcessor()
        {
        super(PagedTopicScheme.class);
        }

    // ----- ServiceBuilderProcessor methods --------------------------------

    @Override
    public PagedTopicScheme process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        PagedTopicScheme             scheme     = super.process(context, element);
        PartitionedCacheDependencies deps       = new DefaultPartitionedCacheDependencies(scheme.getServiceDependencies());
        final SerializerFactory      factory    = deps.getSerializerFactory();
        SerializerFactory            factoryPof = new SerializerFactory()
            {
            @Override
            public Serializer createSerializer(ClassLoader loader)
                {
                Serializer serializer = factory == null
                    ? ExternalizableHelper.ensureSerializer(loader)
                    : factory.createSerializer(loader);

                return serializer instanceof PofContext
                    ? serializer
                    : new SafeConfigurablePofContext(serializer, loader);
                }
            };

        // Ensure POF serializer since topic data model and processors are EvolvablePortableObject.
        // Application payload published to topic is serialized using serializer specified in cache configuration.
        ((DefaultServiceDependencies) deps).setSerializerFactory(factoryPof);
        scheme.setServiceDependencies(deps);

        CachingScheme    schemeStorage    = scheme.getStorageScheme(context.getDefaultParameterResolver());
        BackingMapScheme backingMapScheme = new BackingMapScheme();

        backingMapScheme.setStorageAccessAuthorizer(scheme.getStorageAccessAuthorizer());
        backingMapScheme.setTransient(scheme.getTransientExpression());

        backingMapScheme.setInnerScheme(new PagedTopicStorageScheme(schemeStorage, scheme));

        scheme.setBackingMapScheme(backingMapScheme);

        return scheme;
        }
    }
