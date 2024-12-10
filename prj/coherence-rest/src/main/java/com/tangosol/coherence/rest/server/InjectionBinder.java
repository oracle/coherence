/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

import com.tangosol.coherence.rest.config.RestConfig;

import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.coherence.rest.query.QueryEngineRegistry;

import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.Session;

import com.tangosol.net.options.WithClassLoader;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.function.Supplier;

import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.jersey.internal.inject.AbstractBinder;

/**
 * A binder for REST API dependencies.
 *
 * @author as 2015.07.29
 *
 * @since Coherence 12.2.1
 */
public class InjectionBinder
        extends AbstractBinder
   {
   // ---- constructors -----------------------------------------------------

   /**
    * Construct InjectionBinder instance.
    *
    * @param fContainer  the flag specifying whether REST API is run inside
    *                    the container (true) or standalone (false).
    */
   public InjectionBinder(boolean fContainer)
       {
       m_fContainer = fContainer;
       }

   // ---- AbstractBinder methods -------------------------------------------

   @Override
   protected void configure()
       {
       if (m_fContainer)
           {
           bind(Session.create(WithClassLoader.autoDetect())).to(Session.class);
           }

       bindFactory(RestConfigFactory.class).to(RestConfig.class).in(Singleton.class);
       bindFactory(MarshallerRegistryFactory.class).to(MarshallerRegistry.class).in(Singleton.class);
       bindFactory(ProcessorRegistryFactory.class).to(ProcessorRegistry.class).in(Singleton.class);
       bindFactory(AggregatorRegistryFactory.class).to(AggregatorRegistry.class).in(Singleton.class);
       bindFactory(QueryEngineRegistryFactory.class).to(QueryEngineRegistry.class).in(Singleton.class);
       }

   /**
    * Inject its fields and methods of a given resource and return the resource.
    *
    * @param <T>       the resource type
    * @param resource  the resource
    * @param locator   the service locator to use
    *
    * @return the resource with its fields and methods injected.
    */
   public static <T> T inject(T resource, ServiceLocator locator)
       {
       locator.inject(resource);
       locator.postConstruct(resource);
       return resource;
       }

   /**
    * HK2 Factory that is used to create RestConfig instance.
    */
   private static class RestConfigFactory implements Supplier<RestConfig>
       {
       @Override
       public RestConfig get()
           {
           return RestConfig.create();
           }

       }

   /**
    * HK2 Factory that is used to create MarshallerRegistry instance.
    */
   private static class MarshallerRegistryFactory implements Supplier<MarshallerRegistry>
       {
       @Override
       public MarshallerRegistry get()
           {
           return m_config != null ? m_config.getMarshallerRegistry()
                : new MarshallerRegistry();
           }

       @Inject
       private RestConfig m_config;
       }

   /**
    * HK2 Factory that is used to create ProcessorRegistry instance.
    */
   private static class ProcessorRegistryFactory implements Supplier<ProcessorRegistry>
       {
       @Override
       public ProcessorRegistry get()
           {
           return m_config != null ? m_config.getProcessorRegistry()
               : new ProcessorRegistry();
           }

       @Inject
       private RestConfig m_config;
       }

   /**
    * HK2 Factory that is used to create AggregatorRegistry instance.
    */
   private static class AggregatorRegistryFactory implements Supplier<AggregatorRegistry>
       {
       @Override
       public AggregatorRegistry get()
           {
           return m_config != null ? m_config.getAggregatorRegistry()
               : new AggregatorRegistry();
           }

       @Inject
       private RestConfig m_config;
       }

   /**
    * HK2 Factory that is used to create QueryEngineRegistry instance.
    */
   private static class QueryEngineRegistryFactory implements Supplier<QueryEngineRegistry>
       {
       @Override
       public QueryEngineRegistry get()
           {
           return m_config != null ? m_config.getQueryEngineRegistry()
               : new QueryEngineRegistry();
           }

       @Inject
       private RestConfig m_config;
       }

   // ---- data members -----------------------------------------------------

   /**
    * The flag specifying whether REST API is run inside the container (true)
    * or standalone (false).
    */
   protected final boolean m_fContainer;
   }
