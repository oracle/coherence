/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.grpc;

import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.internal.util.DaemonPool;

import java.util.ServiceLoader;

/**
 * A class responsible for controlling a gRPC server.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface GrpcAcceptorController
    {
    /**
     * Set the {@link GrpcAcceptorDependencies}.
     *
     * @param deps  the {@link GrpcAcceptorDependencies}
     */
    void setDependencies(GrpcAcceptorDependencies deps);

    /**
     * Returns the {@link GrpcAcceptorDependencies gRPC acceptor dependencies}.
     *
     * @return the {@link GrpcAcceptorDependencies gRPC acceptor dependencies}
     */
    GrpcAcceptorDependencies getDependencies();

    /**
     * Set the {@link DaemonPool} to be used.
     *
     * @param pool  the {@link DaemonPool} to be used
     */
    void setDaemonPool(DaemonPool pool);

    /**
     * Start the gRPC server.
     */
    void start();

    /**
     * Stop the gRPC server.
     */
    void stop();

    /**
     * Returns {@code true} if the gRPC server is running.
     *
     * @return {@code true} if the gRPC server is running
     */
    boolean isRunning();

    /**
     * Return the address that the gRPC server is listening on.
     *
     * @return the address that the gRPC server is listening on
     */
    String getLocalAddress();

    /**
     * Return the name of the in-process gRPC server.
     *
     * @return the name of the in-process gRPC server
     */
    String getInProcessName();

    /**
     * Return the port that the gRPC server is listening on.
     *
     * @return the port that the gRPC server is listening on
     */
    int getLocalPort();

    /**
     * Discover any {@link GrpcAcceptorController} instances.
     *
     * @return the first {@link GrpcAcceptorController} instance discovered
     */
    static GrpcAcceptorController discoverController()
        {
        return ServiceLoader.load(GrpcAcceptorController.class)
                .findFirst()
                .orElse(NULL_CONTROLLER);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A no-op implementation of a {@link GrpcAcceptorController}.
     */
    GrpcAcceptorController NULL_CONTROLLER = new GrpcAcceptorController()
        {
        @Override
        public void setDependencies(GrpcAcceptorDependencies deps)
            {
            m_deps = deps;
            }

        @Override
        public GrpcAcceptorDependencies getDependencies()
            {
            return m_deps;
            }

        @Override
        public void setDaemonPool(DaemonPool pool)
            {
            }

        @Override
        public void start()
            {
            }

        @Override
        public void stop()
            {
            }

        @Override
        public boolean isRunning()
            {
            return false;
            }

        @Override
        public String getLocalAddress()
            {
            return "0.0.0.0";
            }

        @Override
        public int getLocalPort()
            {
            return -1;
            }

        @Override
        public String getInProcessName()
            {
            return null;
            }

        /**
         * The gRPC acceptor dependencies.
         */
        private GrpcAcceptorDependencies m_deps;
        };
    }
