/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.EventObserverSupport;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;

import com.tangosol.net.events.NamedEventInterceptor;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import javax.enterprise.inject.literal.NamedLiteral;

import javax.enterprise.inject.spi.BeanManager;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A producer that creates the default {@link Coherence} bean.
 *
 * @author Jonathan Knight  2020.12.10
 * @since 20.12
 */
@ApplicationScoped
public class CoherenceProducer
    {
    /**
     * Create the default {@link Coherence} bean.
     * <p>
     * By default the {@link Coherence} bean will be run in {@link Coherence.Mode#Client} mode.
     * <p>
     * The configuration can be changed by supplying a {@link CoherenceConfiguration.Builder}
     * bean annotated with {@literal @}{@link Named} with a name {@link Coherence#DEFAULT_NAME}.
     * This configuration bean will then be used to provide the configuration for the
     * {@link Coherence} bean.
     *
     * @param beanManager  the CDI bean manager
     *
     * @return the default {@link Coherence} bean
     */
    @Produces
    @Singleton
    @Name(Coherence.DEFAULT_NAME)
    public Coherence createCoherence(BeanManager beanManager)
        {
        // build and start the Coherence instance
        Coherence coherence = Coherence.client(createConfiguration(beanManager));

        // wait for start-up to complete
        coherence.start().join();

        return coherence;
        }

    protected CoherenceConfiguration createConfiguration(BeanManager beanManager)
        {
        CoherenceExtension                            extension        = beanManager.getExtension(CoherenceExtension.class);
        List<EventObserverSupport.EventHandler<?, ?>> listInterceptors = extension.getInterceptors();

        Instance<SessionConfiguration> configurations = beanManager.createInstance()
                .select(SessionConfiguration.class, Any.Literal.INSTANCE);

        Instance<CoherenceExtension.InterceptorProvider> interceptorProviders = beanManager.createInstance()
                .select(CoherenceExtension.InterceptorProvider.class, Any.Literal.INSTANCE);

        List<NamedEventInterceptor<?>> listInterceptor = listInterceptors.stream()
                .map(handler -> new NamedEventInterceptor<>(handler.getId(), handler))
                .collect(Collectors.toList());

        interceptorProviders.stream()
                .flatMap(provider -> StreamSupport.stream(provider.getInterceptors().spliterator(), false))
                .forEach(listInterceptor::add);

        // See whether there is a CoherenceConfiguration.Builder bean available
        Instance<CoherenceConfiguration.Builder> instance = beanManager.createInstance()
                .select(CoherenceConfiguration.Builder.class, NamedLiteral.of(Coherence.DEFAULT_NAME));

        CoherenceConfiguration.Builder builder;
        if (instance.isResolvable())
            {
            // There is a CoherenceConfiguration.Builder bean so use that as the builder.
            builder = instance.get();
            }
        else
            {
            List<SessionConfiguration> sessionConfiguration = configurations.stream().collect(Collectors.toList());
            
            boolean fHasDefault = sessionConfiguration.stream().anyMatch(cfg -> Coherence.DEFAULT_NAME.equals(cfg.getName()));

            // else there is no CoherenceConfiguration.Builder bean so we create one
            builder = CoherenceConfiguration.builder()
                        .withSessions(sessionConfiguration)
                        .withEventInterceptors(listInterceptor)
                        .discoverSessions();

            if (!fHasDefault)
                {
                builder.withSession(SessionConfiguration.defaultSession());
                }
            }

        // build the configuration - ensuring the correct name is set
        return builder.named(Coherence.DEFAULT_NAME).build();
        }
    }
