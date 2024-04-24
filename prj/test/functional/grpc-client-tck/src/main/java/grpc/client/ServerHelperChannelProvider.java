/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;

import java.util.ServiceLoader;

public interface ServerHelperChannelProvider
    {
    Channel get(String sHost, int nPort);

    static Channel getChannel(String sHost, int nPort)
        {
        return ServiceLoader.load(ServerHelperChannelProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .map(p -> p.get(sHost, nPort))
                .findFirst()
                .orElseGet(() ->
                    {
                    ChannelCredentials credentials = InsecureChannelCredentials.create();
                    return Grpc.newChannelBuilderForAddress("127.0.0.1", nPort, credentials).build();
                    });
        }
    }
