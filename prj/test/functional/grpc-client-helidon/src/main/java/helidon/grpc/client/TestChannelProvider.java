/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package helidon.grpc.client;

import grpc.client.ServerHelperChannelProvider;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.helidon.common.tls.TlsConfig;

public class TestChannelProvider
        implements ServerHelperChannelProvider
    {
    @Override
    public Channel get(String sHost, int nPort)
        {
//        WebClient webClient = WebClient.builder()
//                .tls(TlsConfig.builder().enabled(false).buildPrototype())
//                .baseUri("https://" + sHost + ":" + nPort)
//                .build();
//
//        GrpcClient client = webClient.client(GrpcClient.PROTOCOL);
//
//        return client.channel();
ChannelCredentials credentials = InsecureChannelCredentials.create();
return Grpc.newChannelBuilderForAddress("127.0.0.1", nPort, credentials).build();
        }
    }
