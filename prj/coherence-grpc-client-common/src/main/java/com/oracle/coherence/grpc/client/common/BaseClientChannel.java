/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

/**
 * A base implementation of a {@link ClientProtocol}.
 *
 * @param <DepsType>  the type of the dependencies
 */
public abstract class BaseClientChannel<DepsType extends BaseGrpcClient.Dependencies>
        implements ClientProtocol
    {
    /**
     * Create a {@link BaseClientChannel}.
     *
     * @param dependencies  the dependencies to use
     * @param connection    the gRPC connection to use
     */
    protected BaseClientChannel(DepsType dependencies, GrpcConnection connection)
        {
        f_dependencies = dependencies;
        f_connection   = connection;
        }

    @Override
    public void close()
        {
        f_connection.close();
        }

    @Override
    public int getVersion()
        {
        return f_connection.getProtocolVersion();
        }

    @Override
    public boolean isActive()
        {
        return f_connection.isConnected();
        }

    @Override
    public GrpcConnection getConnection()
        {
        return f_connection;
        }

    public DepsType getDependencies()
        {
        return f_dependencies;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A constant void type.
     */
    protected static final Void VOID = null;

    /**
     * The {@link GrpcConnection} to use to send messages.
     */
    protected final GrpcConnection f_connection;

    /**
     * The client dependencies.
     */
    protected final DepsType f_dependencies;
    }
