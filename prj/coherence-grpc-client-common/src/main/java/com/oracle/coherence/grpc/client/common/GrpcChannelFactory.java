/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.tangosol.coherence.config.scheme.ServiceScheme;
import io.grpc.Channel;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A factory to create a {@link Channel}.
 * <p/>
 * The instance of {@link GrpcChannelFactory} used will be discovered using
 * the Java {@link ServiceLoader}. If multiple instances are on the class path
 * the class with the highest {@link #getPriority()} method will be used.
 */
public interface GrpcChannelFactory
    {
    // ----- GrpcChannelFactory methods -------------------------------------

    /**
     * Returns the singleton instance of {@link GrpcChannelFactory}.
     *
     * @return the singleton instance of {@link GrpcChannelFactory}
     */
    static GrpcChannelFactory singleton()
        {
        return FactoryHolder.singleton();
        }

    /**
     * Return the priority of this controller if multiple controllers
     * are discovered. The controller with the highest priority will
     * be used. If multiple controllers have the highest priority the
     * actual controller used cannot be determined.
     *
     * @return the priority of this controller if multiple controllers
     *         are discovered
     */
    default int getPriority()
        {
        return PRIORITY_NORMAL - 1;
        }

    /**
     * Create a {@link Channel}.
     *
     * @param service  the {@link GrpcRemoteService} to create the channel for
     *
     * @return a {@link Channel}
     */
    Channel getChannel(GrpcRemoteService<?> service);

    /**
     * Create a target URI to connect to a Coherence gRPC proxy.
     *
     * @param service  the {@link GrpcRemoteService} that will connect to the proxy
     *
     * @return  the connection URI
     */
    static String createTargetURI(GrpcRemoteService<?> service)
        {
        String sService = service.getServiceName();
        String sScope   = service.getScopeName();
        int    nIdx     = sService.indexOf(ServiceScheme.DELIM_APPLICATION_SCOPE);

        if (sScope == null && nIdx > 0)
            {
            sScope   = sService.substring(0, nIdx);
            sService = sService.substring(nIdx + 1);
            }
        return createTargetURI(sService, sScope);
        }

    /**
     * Create a target URI to connect to a Coherence gRPC proxy.
     *
     * @param sService  the name of the remote service
     * @param sScope    the scope of the remote service
     *
     * @return the connection URI
     */
    static String createTargetURI(String sService, String sScope)
        {
        if (sScope == null)
            {
            return RESOLVER_SCHEME + "://" + sService;
            }
        return RESOLVER_SCHEME + "://" + sService + "?" + sScope;
        }

    // ----- inner class FactoryHolder --------------------------------------

    /**
     * A holder class to create the singleton {@link GrpcChannelFactory}.
     */
    class FactoryHolder
        {
        private FactoryHolder()
            {
            }

        /**
         * Returns the singleton instance of {@link GrpcChannelFactory}.
         *
         * @return the singleton instance of {@link GrpcChannelFactory}
         */
        public static GrpcChannelFactory singleton()
            {
            GrpcChannelFactory factory = s_instance;
            if (factory == null)
                {
                s_lock.lock();
                try
                    {
                    factory = s_instance;
                    if (factory == null)
                        {
                        factory = s_instance = ServiceLoader.load(GrpcChannelFactory.class).stream()
                                .map(ServiceLoader.Provider::get)
                                    .max(Comparator.comparingInt(GrpcChannelFactory::getPriority))
                                    .orElseThrow(() -> new IllegalStateException("No GrpcChannelFactory found on the class path"));
                        }
                    }
                finally
                    {
                    s_lock.unlock();
                    }
                }
            return factory;
            }

        /**
         * A lock to control creation of the singleton factory.
         */
        private static final ReentrantLock s_lock = new ReentrantLock();

        /**
         * The singleton {@link AbstractGrpcChannelFactory}.
         */
        private static volatile GrpcChannelFactory s_instance;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the custom Coherence gRPC name resolver.
     */
    String RESOLVER_SCHEME = "coherence";

    /**
     * The default priority for a controller.
     */
    int PRIORITY_NORMAL = 0;
    }
