/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.grpc;

import com.tangosol.config.annotation.Injectable;

/**
 * A default implementation of {@link RemoteGrpcCacheServiceDependencies}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class DefaultRemoteGrpcCacheServiceDependencies
        extends DefaultRemoteGrpcServiceDependencies
        implements RemoteGrpcCacheServiceDependencies
    {
    /**
     * Create a {@link DefaultRemoteGrpcCacheServiceDependencies}.
     */
    public DefaultRemoteGrpcCacheServiceDependencies()
        {
        this(null);
        }

    /**
     * Create a {@link DefaultRemoteGrpcCacheServiceDependencies} by copying
     * the specified {@link RemoteGrpcCacheServiceDependencies}.
     *
     * @param deps  the {@link RemoteGrpcCacheServiceDependencies} to copy
     */
    public DefaultRemoteGrpcCacheServiceDependencies(RemoteGrpcCacheServiceDependencies deps)
        {
        super(deps);
        }

    @Override
    public boolean isDeferKeyAssociationCheck()
        {
        return m_fDeferKeyAssociationCheck;
        }

    /**
     * Set the flag to defer the KeyAssociation check.
     *
     * @param fDefer  the KeyAssociation check defer flag
     */
    @Injectable("defer-key-association-check")
    public void setDeferKeyAssociationCheck(boolean fDefer)
        {
        m_fDeferKeyAssociationCheck = fDefer;
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The flag to indicate if the KeyAssociation check is deferred.
     */
    private boolean m_fDeferKeyAssociationCheck;
    }
