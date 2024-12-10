/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.proxy;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ProxyService;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.grpc.GrpcAcceptorController;

public class FindGrpcProxyPort
        implements RemoteCallable<Integer>
    {
    @Override
    public Integer call()
        {
        Cluster cluster = CacheFactory.getCluster();
        ProxyService             service      = (ProxyService) cluster.getService(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME);
        ProxyServiceDependencies depsService  = (ProxyServiceDependencies) service.getDependencies();
        GrpcAcceptorDependencies depsAcceptor = (GrpcAcceptorDependencies) depsService.getAcceptorDependencies();
        GrpcAcceptorController controller   = depsAcceptor.getController();
        return controller.getLocalPort();
        }

    public static int local()
        {
        return INSTANCE.call();
        }

    public static final FindGrpcProxyPort INSTANCE = new FindGrpcProxyPort();
    }
