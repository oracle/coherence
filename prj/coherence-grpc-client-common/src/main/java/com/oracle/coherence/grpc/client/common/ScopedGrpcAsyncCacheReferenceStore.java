/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.tangosol.net.internal.AbstractScopedReferenceStore;
import com.tangosol.net.internal.ScopedReferenceStore;

/**
 * {@link ScopedGrpcAsyncCacheReferenceStore} holds scoped {@link AsyncNamedCacheClient} references.
 * <p>
 * {@link AsyncNamedCacheClient} references are scoped by ClassLoader and, optionally, Subject.
 * ScopedGrpcAsyncCacheReferenceStore requires no explicit input about
 * Subjects from its clients. Subject scoping is configured in the operational
 * configuration and applies only to remote cache.
 * <p>
 * Thread safety documented in {@link AbstractScopedReferenceStore}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
@SuppressWarnings({"rawtypes"})
public class ScopedGrpcAsyncCacheReferenceStore
        extends ScopedReferenceStore<AsyncNamedCacheClient>
    {
    public ScopedGrpcAsyncCacheReferenceStore()
        {
        super(AsyncNamedCacheClient.class,
              AsyncNamedCacheClient::isActiveInternal,
              AsyncNamedCacheClient::getCacheName,
              AsyncNamedCacheClient::getCacheService);
        }
    }
