/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.tangosol.io.Serializer;

import com.tangosol.net.grpc.GrpcDependencies;

/**
 * The legacy version zero implementation of a {@link NamedCacheClientChannel}.
 */
@SuppressWarnings({"DuplicatedCode"})
public abstract class BaseNamedCacheClientChannel
        extends BaseClientChannel<AsyncNamedCacheClient.Dependencies>
        implements NamedCacheClientChannel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates an {@link BaseNamedCacheClientChannel} from the specified
     * {@link AsyncNamedCacheClient.Dependencies}.
     *
     * @param dependencies the {@link AsyncNamedCacheClient.Dependencies} to configure this
     *                     {@link BaseNamedCacheClientChannel}.
     * @param connection   the {@link GrpcConnection}
     */
    public BaseNamedCacheClientChannel(AsyncNamedCacheClient.Dependencies dependencies, GrpcConnection connection)
        {
        super(dependencies, connection);
        f_sName      = dependencies.getName();
        f_sScopeName = dependencies.getScopeName().orElse(GrpcDependencies.DEFAULT_SCOPE);
        f_sFormat    = dependencies.getSerializerFormat()
                                     .orElseGet(() -> dependencies.getSerializer()
                                         .map(Serializer::getName)
                                         .orElseGet(BaseGrpcClient::getDefaultSerializerFormat));
        }

    @Override
    public AsyncNamedCacheClient.Dependencies getDependencies()
        {
        return super.getDependencies();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the scope to use to process requests on the proxy.
     */
    protected final String f_sScopeName;

    /**
     * The name of the serializer to use to process requests on the proxy.
     */
    protected final String f_sFormat;

    /**
     * The name of this cache.
     */
    protected final String f_sName;
    }
