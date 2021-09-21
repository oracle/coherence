/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management;

import com.tangosol.coherence.management.internal.ClusterNameSupplier;
import com.tangosol.coherence.management.internal.MapProvider;

import com.tangosol.coherence.management.internal.filters.DenySniffResponseFilter;

import com.tangosol.coherence.management.internal.resources.ManagementRootResource;
import com.tangosol.coherence.management.internal.resources.VersionsResource;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.WrapperMBeanServerProxy;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import org.glassfish.jersey.message.GZipEncoder;

import org.glassfish.jersey.process.internal.RequestScoped;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

import javax.management.MBeanServer;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The entry point into Coherence Management over ReST that
 * can be used by external applications that want to publish
 * the Coherence Management resources on a different endpoint.
 *
 * @author jk  2019.05.30
 */
// --------------------------------------------------------------------------
// IMPLEMENTATION NOTE:
//
// This class is the integration point between Coherence Management over ReST
// and WebLogic's ReST management API. Changes to this class may impact WLS
// and should always be made in a backwards compatible way.
// --------------------------------------------------------------------------
public final class RestManagement
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Cannot be constructed.
     */
    private RestManagement()
        {
        }

    // ----- RestManagement methods -----------------------------------------

    /**
     * Initialize the {@link ResourceConfig} to provide the management over ReST endpoints.
     * <p>
     * This method will configure standard, single cluster, non-versioned endpoints.
     *
     * @param resourceConfig  the {@link ResourceConfig} to configure
     *
     * @throws NullPointerException if the any of the parameters are null
     */
    public static void configure(ResourceConfig resourceConfig)
        {
        configure(resourceConfig, MBeanServerProxyFactory.DEFAULT, null);
        }

    /**
     * Initialize the {@link ResourceConfig} to provide the management over ReST endpoints.
     * <p>
     * This method will configure standard, single cluster, non-versioned endpoints.
     *
     * @param resourceConfig  the {@link ResourceConfig} to configure
     * @param factory         the {@link MBeanServerProxyFactory} that will supply
     *                        instances of {@link MBeanServerProxy}
     *
     * @throws NullPointerException if the any of the parameters are null
     */
    public static void configure(ResourceConfig resourceConfig, MBeanServerProxyFactory factory)
        {
        configure(resourceConfig, factory, null);
        }

    /**
     * Initialize the {@link ResourceConfig} to provide the management over ReST endpoints.
     * <p>
     * This method will configure standard, single cluster, non-versioned endpoints.
     *
     * @param resourceConfig  the {@link ResourceConfig} to configure
     * @param factory         the {@link Supplier} that will supply
     *                        instances of {@link MBeanServer}
     *
     * @throws NullPointerException if the any of the parameters are null
     */
    public static void configure(ResourceConfig resourceConfig, Supplier<MBeanServer> factory)
        {
        configure(resourceConfig, createMBeanServerProxyFactory(factory), null);
        }

    /**
     * Initialize the {@link ResourceConfig} to provide the management over ReST endpoints
     * for a multi-cluster environment.
     * <p>
     * This method will configure the {@link ResourceConfig} to use versioned multi-cluster
     * endpoints.
     * <p>
     * The {@code supplierClusters} parameter is expected to supply the valid cluster names
     * that may be used in the request URLs.
     *
     * @param resourceConfig    the {@link ResourceConfig} to configure
     * @param factory           the {@link Supplier} that will supply
     *                          instances of {@link MBeanServer}
     * @param supplierClusters  a supplier of available cluster names
     *
     * @throws NullPointerException if the any of the parameters are null
     */
    // --------------------------------------------------------------------------
    // IMPLEMENTATION NOTE:
    //
    // This method the integration point between Coherence Management over ReST
    // and WebLogic's ReST management API. Changes to this method may impact WLS
    // and should always be made in a backwards compatible way.
    // --------------------------------------------------------------------------
    public static void configure(ResourceConfig        resourceConfig,
                                 Supplier<MBeanServer> factory,
                                 Supplier<Set<String>> supplierClusters)
        {
        configure(resourceConfig, createMBeanServerProxyFactory(factory), supplierClusters);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Initialize the {@link ResourceConfig} to provide the management over ReST endpoints.
     *
     * @param resourceConfig    the {@link ResourceConfig} to configure
     * @param factory           the {@link MBeanServerProxyFactory} that will supply
     *                          instances of {@link MBeanServerProxy}
     * @param supplierClusters  an optional supplier of available cluster names
     *
     * @throws NullPointerException if the either of {@code resourceConfig} or
     *         {@code factory} parameters are null
     */
    private static void configure(ResourceConfig          resourceConfig,
                                  MBeanServerProxyFactory factory,
                                  Supplier<Set<String>>   supplierClusters)
        {
        Objects.requireNonNull(resourceConfig);

        // --------------------------------------------------------------------------
        // IMPLEMENTATION NOTE:
        //
        // This method an integration point between Coherence Management over ReST
        // and WebLogic's ReST management API. Changes to this method may impact WLS
        // (for example registering providers or features that are incompatible with
        // the resource configuration used by WLS management framework).
        // --------------------------------------------------------------------------

        // common configurations
        resourceConfig.register(MapProvider.class);
        resourceConfig.register(new MBeanServerProxyBinder(Objects.requireNonNull(factory)));
        resourceConfig.register(DenySniffResponseFilter.class);
        EncodingFilter.enableFor(resourceConfig, GZipEncoder.class);

        if (supplierClusters == null)
            {
            // we're running in a single cluster environment so use the default resource
            resourceConfig.register(ManagementRootResource.class);
            }
        else
            {
            // we're running in a multi-cluster environment so use the versions resource
            resourceConfig.register(new ClusterNameSupplier.Binder(supplierClusters::get));
            resourceConfig.register(VersionsResource.class);
            }
        }

    private static MBeanServerProxyFactory createMBeanServerProxyFactory(Supplier<MBeanServer> supplier)
        {
        return () -> new WrapperMBeanServerProxy(Objects.requireNonNull(supplier));
        }

    // ----- inner interface MBeanServerProxyFactory ------------------------

    /**
     * A provider of instances of {@link MBeanServerProxy}.
     */
    @FunctionalInterface
    public interface MBeanServerProxyFactory
        {
        MBeanServerProxy get();

        /**
         * The default {@link MBeanServerProxyFactory}.
         */
        MBeanServerProxyFactory DEFAULT
                = () -> CacheFactory.getCluster().getManagement().getMBeanServerProxy();
        }

    // ----- inner class: MBeanServerProxyBinder ----------------------------

    /**
     * An {@link AbstractBinder} to bind an MBeanServerProxy
     * to a resource.
     */
    private static class MBeanServerProxyBinder
            extends AbstractBinder
        {
        private MBeanServerProxyBinder(MBeanServerProxyFactory factory)
            {
            f_factory = factory;
            }

        @Override
        protected void configure()
            {
            bindFactory(new Supplier<MBeanServerProxy>()
                    {
                    @RequestScoped
                    public MBeanServerProxy get()
                        {
                        return f_factory.get();
                        }
                    })
                .to(MBeanServerProxy.class);
            }

        private final MBeanServerProxyFactory f_factory;
        }
    }
