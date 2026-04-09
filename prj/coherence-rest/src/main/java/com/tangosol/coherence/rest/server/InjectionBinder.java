/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

import com.tangosol.coherence.rest.config.RestConfig;

import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.coherence.rest.query.QueryEngineRegistry;

import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.Session;

import com.tangosol.net.options.WithClassLoader;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

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
   private static class RestConfigFactory implements Factory<RestConfig>
       {
       @Override
       public RestConfig provide()
           {
           return RestConfig.create();
           }

       @Override
       public void dispose(RestConfig config)
           {
           }
       }

   /**
    * HK2 Factory that is used to create MarshallerRegistry instance.
    */
   private static class MarshallerRegistryFactory implements Factory<MarshallerRegistry>
       {
       @Override
       public MarshallerRegistry provide()
           {
           return m_config != null ? m_config.getMarshallerRegistry()
                : new MarshallerRegistry();
           }

       @Override
       public void dispose(MarshallerRegistry registry)
           {
           }

       @Inject
       private RestConfig m_config;
       }

   /**
    * HK2 Factory that is used to create ProcessorRegistry instance.
    */
   private static class ProcessorRegistryFactory implements Factory<ProcessorRegistry>
       {
       @Override
       public ProcessorRegistry provide()
           {
           return m_config != null ? m_config.getProcessorRegistry()
               : new ProcessorRegistry();
           }

       @Override
       public void dispose(ProcessorRegistry registry)
           {
           }

       @Inject
       private RestConfig m_config;
       }

   /**
    * HK2 Factory that is used to create AggregatorRegistry instance.
    */
   private static class AggregatorRegistryFactory implements Factory<AggregatorRegistry>
       {
       @Override
       public AggregatorRegistry provide()
           {
           return m_config != null ? m_config.getAggregatorRegistry()
               : new AggregatorRegistry();
           }

       @Override
       public void dispose(AggregatorRegistry registry)
           {
           }

       @Inject
       private RestConfig m_config;
       }

   /**
    * HK2 Factory that is used to create QueryEngineRegistry instance.
    */
   private static class QueryEngineRegistryFactory implements Factory<QueryEngineRegistry>
       {
       @Override
       public QueryEngineRegistry provide()
           {
           return m_config != null ? m_config.getQueryEngineRegistry()
               : new QueryEngineRegistry();
           }

       @Override
       public void dispose(QueryEngineRegistry registry)
           {
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
