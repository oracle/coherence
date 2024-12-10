/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.config;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.coherence.config.xml.processor.InstanceProcessor;

/**
 * A {@link CacheConfigNamespaceExtension} that add gRPC extensions to
 * the cache configuration file.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class CacheConfigNamespaceExtension
        implements CacheConfigNamespaceHandler.Extension
    {
    @Override
    public void extend(CacheConfigNamespaceHandler handler)
        {
        handler.registerProcessor("grpc-channel", new GrpcChannelProcessor());
        handler.registerProcessor("remote-grpc-cache-scheme", new RemoteGrpcCacheServiceBuilderProcessor());
//        handler.registerProcessor("remote-grpc-topic-scheme", new RemoteGrpcTopicServiceBuilderProcessor());
        handler.registerProcessor("configurer", new InstanceProcessor());
        }
    }
