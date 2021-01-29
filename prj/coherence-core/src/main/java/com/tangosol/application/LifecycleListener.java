/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.application;

/**
 * A Coherence application typically includes a cache configuration file with
 * cache and clustered service definitions.  This interface defines lifecycle
 * callbacks that are invoked before and after the creation and destruction
 * of these services.
 * <p>
 * Implementations of this interface are typically declared in a Coherence
 * application deployment descriptor in a GAR (Grid Archive) file.  The
 * instantiation occurs in {@link ContainerAdapter}, which is the application
 * server integration layer.
 *
 * @author cf 2011.05.24
 *
 * @since Coherence 12.1.2
 */
public interface LifecycleListener
    {
    /**
     * Called before the application is activated.  In general this occurs
     * before services in a cache configuration file are started thus allowing
     * for pre service setup.
     * <p>
     * The {@link Context} contains the context of the application allowing for a custom
     * application to create custom services and/or a custom
     * {@link com.tangosol.net.CacheFactoryBuilder} implementation.
     *
     * @param ctx  the {@link Context} for the application
     */
    void preStart(Context ctx);

    /**
     * Called after the application is started.  At this point, services
     * marked with {@code <autostart>} will have been started.  These
     * services (and caches) can be accessed via
     * {@link Context#getConfigurableCacheFactory()}.
     *
     * @param ctx  the {@link Context} for the application
     */
    void postStart(Context ctx);

    /**
     * Called before the application stops its services and disposes of its resources.
     *
     * @param ctx  the {@link Context} for the application
     */
    void preStop(Context ctx);

    /**
     * Called after the application is stopped.  At this point any services created
     * by this application will have been stopped.
     *
     * @param ctx  the {@link Context} for the application
     */
    void postStop(Context ctx);
   }
