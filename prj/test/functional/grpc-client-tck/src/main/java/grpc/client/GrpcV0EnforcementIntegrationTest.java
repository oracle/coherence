/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.messages.cache.v0.ValuesRequest;
import com.oracle.coherence.grpc.services.cache.v0.NamedCacheServiceGrpc;
import com.oracle.coherence.grpc.v0.Requests;

import com.tangosol.net.NamedCache;
import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.Filter;

import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * gRPC v0 cache data-plane executable policy integration coverage.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.04
 */
class GrpcV0EnforcementIntegrationTest
        extends AbstractGrpcEnforcementIntegrationTest
    {
    @Override
    protected ServerHelper serverHelper()
        {
        return s_serverHelper;
        }

    @Override
    protected Collection<Integer> values(NamedCache<String, Integer> cache, Filter<Integer> filter,
            Comparator<Object> comparator)
        {
        ByteString filterBytes     = BinaryHelper.toByteString(filter, SERIALIZER);
        ByteString comparatorBytes = BinaryHelper.toByteString(comparator, SERIALIZER);
        ValuesRequest request      = Requests.values(GrpcDependencies.DEFAULT_SCOPE, cache.getCacheName(), "",
                filterBytes, comparatorBytes);
        Iterator<BytesValue> iterator = NamedCacheServiceGrpc.newBlockingStub(serverChannel()).values(request);
        Collection<Integer>  values   = new ArrayList<>();

        iterator.forEachRemaining(value -> values.add(fromBytesValue(value)));
        return values;
        }

    @RegisterExtension
    static ServerHelper s_serverHelper = new ServerHelper()
            .setProtocolVersion(0)
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.wka", "127.0.0.1")
            .setProperty("coherence.localhost", "127.0.0.1")
            .setProperty("coherence.cluster", "GrpcV0EnforcementIntegrationTest-" + System.nanoTime())
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.mode", "prod")
            .setProperty("coherence.grpc.error-disclosure", "diagnostic")
            .setProperty("coherence.cacheconfig", "coherence-config.xml");
    }
