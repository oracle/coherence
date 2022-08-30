/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.net.InetSocketAddress32;

import com.oracle.coherence.grpc.CredentialsHelper;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService;

import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.internal.net.grpc.RemoteGrpcCacheServiceDependencies;

import com.tangosol.internal.net.service.extend.remote.DefaultRemoteNameServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteNameServiceHelper;

import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;

import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NameService;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.net.grpc.GrpcChannelDependencies;
import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.net.messaging.ConnectionException;

import io.grpc.Attributes;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.EquivalentAddressGroup;
import io.grpc.Grpc;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.NameResolverRegistry;
import io.grpc.ProxyDetector;
import io.grpc.Status;

import io.grpc.internal.GrpcUtil;

import io.grpc.internal.SharedResourceHolder;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * A default implementation of {@link GrpcChannelFactory}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class GrpcChannelFactory
        extends NameResolverProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link GrpcChannelFactory}
     */
    private GrpcChannelFactory()
        {
        NameResolverRegistry.getDefaultRegistry().register(this);
        }

    // ----- GrpcChannelFactory methods -------------------------------------

    /**
     * Returns the singleton instance of {@link GrpcChannelFactory}.
     *
     * @return the singleton instance of {@link GrpcChannelFactory}
     */
    public static GrpcChannelFactory singleton()
        {
        return Instance.Singleton.getFactory();
        }

    public Channel getChannel(GrpcRemoteCacheService service)
        {
        RemoteGrpcCacheServiceDependencies depsService = service.getDependencies();
        GrpcChannelDependencies            depsChannel = depsService.getChannelDependencies();
        ManagedChannelBuilder<?>           builder     = (ManagedChannelBuilder<?>) depsChannel.getChannelProvider()
                                                                .orElse(createManagedChannelBuilder(service));

        return builder.build();
        }

    // ----- helper methods -------------------------------------------------

    private ManagedChannelBuilder<?> createManagedChannelBuilder(GrpcRemoteCacheService service)
        {
        RemoteGrpcCacheServiceDependencies depsService    = service.getDependencies();
        OperationalContext                 ctx            = (OperationalContext) service.getCluster();
        String                             sService       = service.getServiceName();
        String                             sKey           = GrpcServiceInfo.createKey(service);
        String                             sRemoteService = depsService.getRemoteServiceName();
        String                             sRemoteCluster = depsService.getRemoteClusterName();
        GrpcChannelDependencies            depsChannel    = depsService.getChannelDependencies();

        m_mapServiceInfo.put(sKey, new GrpcServiceInfo(ctx, sService, sRemoteService, sRemoteCluster, depsChannel));

        String sTarget = depsChannel.getTarget();
        if (sTarget == null)
            {
            sTarget = GrpcServiceInfo.createTargetURI(service);
            }

        SocketProviderBuilder    builder        = depsChannel.getSocketProviderBuilder();
        ChannelCredentials       credentials    = CredentialsHelper.createChannelCredentials(sService, builder);
        ManagedChannelBuilder<?> channelBuilder = Grpc.newChannelBuilder(sTarget, credentials);

        depsChannel.getAuthorityOverride().ifPresent(channelBuilder::overrideAuthority);

        depsChannel.getConfigurer()
                .filter(GrpcChannelConfigurer.class::isInstance)
                .map(GrpcChannelConfigurer.class::cast)
                .ifPresent(c -> c.apply(channelBuilder));

        channelBuilder.defaultLoadBalancingPolicy(depsChannel.getDefaultLoadBalancingPolicy());
        channelBuilder.userAgent("Coherence Java Client");

        return channelBuilder;
        }

    // ----- NameResolverProvider methods -----------------------------------

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args)
        {
        String                  sKey         = GrpcServiceInfo.parseServiceInfoKey(targetUri);
        GrpcServiceInfo         serviceInfo  = m_mapServiceInfo.get(sKey);
        GrpcChannelDependencies dependencies = serviceInfo.getDependencies();
        return new AddressProviderNameResolver(dependencies, serviceInfo, args);
        }

    @Override
    public String getDefaultScheme()
        {
        return RESOLVER_SCHEME;
        }

    @Override
    protected boolean isAvailable()
        {
        return true;
        }

    @Override
    protected int priority()
        {
        return 0;
        }

    // ----- inner class: AddressProviderNameResolver -----------------------

    public static class AddressProviderNameResolver
            extends NameResolver
        {
        @SuppressWarnings("rawtypes")
        public AddressProviderNameResolver(GrpcChannelDependencies deps,
                GrpcServiceInfo serviceInfo, NameResolver.Args args)
            {
            m_fNameServiceAddressProvider = deps.isNameServiceAddressProvider();
            m_executorResource            = GrpcUtil.SHARED_CHANNEL_EXECUTOR;
            m_serviceInfo                 = serviceInfo;
            m_nameResolverArgs            = args;

            ParameterizedBuilder bldr = deps.getRemoteAddressProviderBuilder();
            if (bldr == null)
                {
                // default to the "cluster-discovery" address provider which consists of MC or WKAs
                AddressProviderFactory factory = serviceInfo.getOperationalContext()
                        .getAddressProviderMap().get("cluster-discovery");

                if (factory != null)
                    {
                    if (factory instanceof ParameterizedBuilder)
                        {
                        bldr = (ParameterizedBuilder) factory;
                        }
                    else
                        {
                        bldr = new FactoryBasedAddressProviderBuilder(factory);
                        }
                    }
                }

            ClassLoader loader = Classes.getContextClassLoader();
            m_addressProvider = (SocketAddressProvider) bldr.realize(new NullParameterResolver(), loader, null);

            // run an initial resolve to set the authority
            new Resolve(this, serviceInfo).run();
            }

        @Override
        public String getServiceAuthority()
            {
            return m_sAuthority;
            }

        @Override
        public void start(Listener2 listener)
            {
            m_listener = Objects.requireNonNull(listener);
            m_executor = SharedResourceHolder.get(m_executorResource);
            resolve();
            }

        @Override
        public void refresh()
            {
            if (m_listener != null)
                {
                resolve();
                }
            }

        @Override
        public void shutdown()
            {
            if (m_fShutdown)
                {
                return;
                }
            m_fShutdown = true;
            if (m_executor != null)
                {
                m_executor = SharedResourceHolder.release(m_executorResource, m_executor);
                }
            }

        // ----- helper methods ---------------------------------------------

        protected Args getNameResolverArgs()
            {
            return m_nameResolverArgs;
            }

        protected SocketAddressProvider getSocketAddressProvider()
            {
            return m_addressProvider;
            }

        protected boolean isNameServiceAddressProvider()
            {
            return m_fNameServiceAddressProvider;
            }

        protected void setAuthority(String sAuthority)
            {
            m_sAuthority = sAuthority;
            }

        private void resolve()
            {
            if (m_fResolving || m_fShutdown)
            {
            return;
            }
            m_fResolving = true;
            m_executor.execute(new Resolve(this, m_serviceInfo, m_listener));
            }

        // ----- data members -----------------------------------------------

        private final GrpcServiceInfo m_serviceInfo;

        private final NameResolver.Args m_nameResolverArgs;

        private String m_sAuthority;

        private final SocketAddressProvider m_addressProvider;

        private final boolean m_fNameServiceAddressProvider;

        private volatile boolean m_fResolving;

        private volatile boolean m_fShutdown;

        private Executor m_executor;

        private final SharedResourceHolder.Resource<Executor> m_executorResource;

        private Listener2 m_listener;
        }

    // ----- inner class: Resolve -------------------------------------------

    protected static class Resolve
            implements Runnable
        {
        protected Resolve(AddressProviderNameResolver addressProviderNameResolver, GrpcServiceInfo serviceInfo)
            {
            this(addressProviderNameResolver, serviceInfo, null);
            }

        protected Resolve(AddressProviderNameResolver addressProviderNameResolver,
                GrpcServiceInfo serviceInfo, NameResolver.Listener2 listener)
            {
            m_addressProviderNameResolver = addressProviderNameResolver;
            m_serviceInfo                 = serviceInfo;
            m_listener                    = listener;
            }

        @Override
        public void run()
            {
            List<SocketAddress> list = m_addressProviderNameResolver.isNameServiceAddressProvider()
                    ? lookupAddresses()
                    : resolveAddresses();

            NameResolver.ResolutionResult result;
            if (list.isEmpty())
                {
                NameResolver.ConfigOrError error = NameResolver.ConfigOrError
                        .fromError(Status.FAILED_PRECONDITION.withDescription("Failed to resolve any gRPC proxy addresses"));

                result = NameResolver.ResolutionResult.newBuilder()
                        .setServiceConfig(error)
                        .setAttributes(Attributes.EMPTY)
                        .build();
                }
            else
                {
                try
                    {
                    NameResolver.Args args          = m_addressProviderNameResolver.getNameResolverArgs();
                    ProxyDetector     proxyDetector = args == null ? null : args.getProxyDetector();

                    if (proxyDetector != null)
                        {
                        List<SocketAddress> proxiedAddresses = new ArrayList<>();
                        for (SocketAddress socketAddress : list)
                            {
                            proxiedAddresses.add(Objects.requireNonNullElse(proxyDetector.proxyFor(socketAddress), socketAddress));
                            }
                        list = proxiedAddresses;
                        }

                    result = NameResolver.ResolutionResult.newBuilder()
                            .setAddresses(Collections.singletonList(new EquivalentAddressGroup(list)))
                            .setAttributes(Attributes.EMPTY)
                            .build();
                    }
                catch (IOException e)
                    {
                    Logger.err(e);

                    NameResolver.ConfigOrError error = NameResolver.ConfigOrError
                            .fromError(Status.INTERNAL.withDescription(e.getMessage()));

                    result = NameResolver.ResolutionResult.newBuilder()
                            .setServiceConfig(error)
                            .setAttributes(Attributes.EMPTY)
                            .build();
                    }
                }

            if (m_listener != null)
                {
                m_listener.onResult(result);
                }
            }

        protected List<SocketAddress> resolveAddresses()
            {
            SocketAddressProvider addressProvider = m_addressProviderNameResolver.getSocketAddressProvider();
            List<SocketAddress>   list            = new ArrayList<>();
            SocketAddress         address         = addressProvider.getNextAddress();
            boolean               fFirst          = true;

            while (address != null)
                {
                if (address instanceof InetSocketAddress32)
                    {
                    // gRPC Java only allows plain InetSocketAddress
                    address = new InetSocketAddress(((InetSocketAddress32) address).getAddress(),
                                                    ((InetSocketAddress32) address).getPort());
                    }
                if (address instanceof InetSocketAddress)
                    {
                    if (fFirst)
                        {
                        updateAuthority((InetSocketAddress) address);
                        fFirst = false;
                        }
                    list.add(address);
                    }
                address = addressProvider.getNextAddress();
                }

            return list;
            }

        private void updateAuthority(InetSocketAddress address)
            {
            String sAuthority = GrpcUtil.authorityFromHostAndPort(address.getHostString(), address.getPort());
            m_addressProviderNameResolver.setAuthority(sAuthority);
            }

        @SuppressWarnings("resource")
        protected List<SocketAddress> lookupAddresses()
            {
            SocketAddressProvider addressProvider = m_addressProviderNameResolver.getSocketAddressProvider();
            RemoteNameService     serviceNS       = new RemoteNameService();
            OperationalContext    context         = m_serviceInfo.getOperationalContext();

            serviceNS.setOperationalContext(context);
            serviceNS.setContextClassLoader(Classes.getContextClassLoader());
            serviceNS.setServiceName(m_serviceInfo.getService() + ':' + NameService.TYPE_REMOTE);

            DefaultRemoteNameServiceDependencies nameServiceDeps =
                    LegacyXmlRemoteNameServiceHelper.fromXml(
                            CacheFactory.getServiceConfig(NameService.TYPE_REMOTE),
                            new DefaultRemoteNameServiceDependencies(),
                            context,
                            Classes.getContextClassLoader());

            // clone and inject the RemoteAddressProvider from this service's dependencies
            // into the RemoteNameService
            DefaultTcpInitiatorDependencies depsNsTcp = new DefaultTcpInitiatorDependencies();

            depsNsTcp.setRemoteSocketAddressProviderBuilder((resolver, loader, listParameters) -> addressProvider);


            // use the default socket provider, as we don't want to inherit SSL settings, NS is always in the clear
            depsNsTcp.setSocketProviderBuilder(new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, false));

            String sServiceRemote = m_serviceInfo.getRemoteService();
            String sCluster       = m_serviceInfo.getRemoteCluster();

            if (sCluster == null || sCluster.isEmpty())
                {
                // NS lookups and corresponding redirects are always done with a cluster name since multiple
                // clusters may effectively share the cluster port we don't know what cluster we'd land in.
                // remote-address based lookups on the other hand use the cluster name configured in the remote
                // scheme, which is allowed to be null.  This is because a remote-address based lookup is pointing
                // at an explicit unsharable port, and it is presumed the configuration is correct.
                sCluster = context.getLocalMember().getClusterName();
                }

            nameServiceDeps.setInitiatorDependencies(depsNsTcp);
            nameServiceDeps.setRemoteClusterName(sCluster);
            nameServiceDeps.setRemoteServiceName("NameService");
            serviceNS.setDependencies(nameServiceDeps);

            try
                {
                serviceNS.start();

                Object[] aoResult = (Object[]) serviceNS.lookup(sServiceRemote);

                if (aoResult == null)
                    {
                    // we got an answer, which means we found the cluster, but not the service
                    throw new ConnectionException("Unable to locate ProxyService '" + sServiceRemote
                                                        + "' within cluster '" + m_serviceInfo.getRemoteCluster() + "'");
                    }

                List<SocketAddress> list = new ArrayList<>();
                for (int i = 0; i < aoResult.length; i += 2)
                    {
                    list.add(new InetSocketAddress((String) aoResult[i], (Integer) aoResult[i+1]));
                    }

                list.stream()
                        .findAny()
                        .ifPresent(address -> updateAuthority((InetSocketAddress) address));

                if (list.isEmpty())
                    {
                    throw new ConnectionException("Unable to locate any addresses in cluster '" + sCluster
                            + "' while looking for its ProxyService '" + sServiceRemote + "'");
                    }

                return list;
                }
            catch (Exception ex)
                {
                // we failed to connect, thus the cluster was not reachable
                throw new ConnectionException("Unable to locate cluster '" + sCluster
                        + "' while looking for its ProxyService '" + sServiceRemote + "'", ex);
                }
            finally
                {
                serviceNS.stop();
                }
            }

        private final AddressProviderNameResolver m_addressProviderNameResolver;

        private final NameResolver.Listener2 m_listener;

        private final GrpcServiceInfo m_serviceInfo;
        }

    // ----- inner class: GrpcServiceInfo -----------------------------------

    public static class GrpcServiceInfo
        {
        public GrpcServiceInfo(OperationalContext ctx, String sService, String sRemoteService,
                String sRemoteCluster, GrpcChannelDependencies dependencies)
            {
            m_operationalContext = ctx;
            m_sService           = sService;
            m_sRemoteService     = sRemoteService;
            m_sRemoteCluster     = sRemoteCluster;
            m_dependencies       = dependencies;
            }

        public static String createKey(GrpcRemoteCacheService service)
            {
            String sService = service.getServiceName();
            String sScope   = service.getScopeName();
            if (sScope == null)
                {
                return sService + KEY_SEPARATOR;
                }
            return sService + KEY_SEPARATOR + sScope;
            }

        public static String parseServiceInfoKey(URI uri)
            {
            String sService = uri.getAuthority();
            String sScope   = uri.getQuery();
            if (sScope != null && !sScope.isEmpty() && sScope.charAt(0) == '/')
                {
                sScope = sService.substring(1);
                }
            if (sScope == null)
                {
                return sService + KEY_SEPARATOR;
                }
            return sService + KEY_SEPARATOR + sScope;
            }

        public static String createTargetURI(GrpcRemoteCacheService service)
            {
            String sService = service.getServiceName();
            String sScope   = service.getScopeName();
            if (sScope == null)
                {
                return RESOLVER_SCHEME + "://" + sService;
                }
            return RESOLVER_SCHEME + "://" + sService + "?" + sScope;
            }

        public OperationalContext getOperationalContext()
            {
            return m_operationalContext;
            }

        public String getService()
            {
            return m_sService;
            }

        public String getRemoteService()
            {
            if (m_sRemoteService == null || m_sRemoteService.isBlank())
                {
                return GrpcDependencies.SCOPED_PROXY_SERVICE_NAME;
                }
            return m_sRemoteService;
            }

        public String getRemoteCluster()
            {
            return m_sRemoteCluster;
            }

        public GrpcChannelDependencies getDependencies()
            {
            return m_dependencies;
            }

        // ----- constants ------------------------------------------------------

        public static final String KEY_SEPARATOR = "$";

        // ----- data members ---------------------------------------------------

        private final OperationalContext m_operationalContext;

        private final String m_sService;

        private final String m_sRemoteService;

        private final String m_sRemoteCluster;

        private final GrpcChannelDependencies m_dependencies;
        }

    // ----- singleton enum -------------------------------------------------

    private enum Instance
        {
        Singleton(new GrpcChannelFactory());

        Instance(GrpcChannelFactory factory)
            {
            m_factory = factory;
            }

        public GrpcChannelFactory getFactory()
            {
            return m_factory;
            }

        private final GrpcChannelFactory m_factory;
        }

    // ----- constants ------------------------------------------------------

    public static final String RESOLVER_SCHEME = "coherence";

    // ----- data members ---------------------------------------------------

    private final Map<String, GrpcServiceInfo> m_mapServiceInfo = new ConcurrentHashMap<>();
    }
