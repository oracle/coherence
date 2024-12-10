/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.scheme.BackingMapScheme;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import com.tangosol.coherence.config.scheme.PagedTopicStorageScheme;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.service.grid.DefaultPagedTopicServiceDependencies;

import com.tangosol.internal.net.service.grid.PagedTopicServiceDependencies;
import com.tangosol.internal.net.topic.impl.paged.SubscriberCleanupListener;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.SafeConfigurablePofContext;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ExternalizableHelper;

import java.util.Collections;

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

    public PagedTopicSchemeProcessor(Class<PagedTopicScheme> clzToRealize)
        {
        super(clzToRealize);
        }

    @Override
    public PagedTopicScheme process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        PagedTopicScheme                    scheme     = super.process(context, element);
        PagedTopicServiceDependencies depService = (PagedTopicServiceDependencies) scheme.getServiceDependencies();
        DefaultPagedTopicServiceDependencies deps       = new DefaultPagedTopicServiceDependencies(depService);
        final SerializerFactory             factory    = deps.getSerializerFactory();
        SerializerFactory                   factoryPof = new SerializerFactory()
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

            @Override
            public String getName()
                {
                return "topics-pof";
                }
            };


        deps.setMemberListenerBuilders(Collections.singletonList(new InstanceBuilder<>(SubscriberCleanupListener.class)));
        deps.setPartitionListenerBuilders(Collections.singletonList(new InstanceBuilder<>(SubscriberCleanupListener.class)));

        // Ensure POF serializer since topic data model and processors are EvolvablePortableObject.
        // Application payload published to topic is serialized using serializer specified in cache configuration.
        deps.setSerializerFactory(factoryPof);
        scheme.setServiceDependencies(deps);

        CachingScheme    schemeStorage    = scheme.getStorageScheme(context.getDefaultParameterResolver());
        BackingMapScheme backingMapScheme = new BackingMapScheme();

        backingMapScheme.setStorageAccessAuthorizer(scheme.getStorageAccessAuthorizer());
        backingMapScheme.setTransient(scheme.getTransientExpression());
        backingMapScheme.setPartitioned(new LiteralExpression<>("true"));

        backingMapScheme.setInnerScheme(new PagedTopicStorageScheme(schemeStorage, scheme));

        scheme.setBackingMapScheme(backingMapScheme);

        return scheme;
        }
    }
