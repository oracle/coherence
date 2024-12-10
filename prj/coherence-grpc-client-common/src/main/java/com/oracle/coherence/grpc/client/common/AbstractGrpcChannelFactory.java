/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Timeout;

import com.oracle.coherence.common.net.InetSocketAddress32;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService;

import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;

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
import io.grpc.LoadBalancer;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.ProxyDetector;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;

import io.grpc.internal.SharedResourceHolder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;

/**
 * A base implementation of {@link AbstractGrpcChannelFactory}.
 */
public abstract class AbstractGrpcChannelFactory
        extends NameResolverProvider
        implements GrpcChannelFactory
    {
    // ----- GrpcChannelFactory methods -------------------------------------

    /**
     * Create a {@link Channel}.
     *
     * @param service  the {@link GrpcRemoteService} to create the channel for
     *
     * @return a {@link Channel}
     */
    @Override
    public abstract Channel getChannel(GrpcRemoteService<?> service);

    // ----- helper methods -------------------------------------------------

    /**
     * Create a {@link ManagedChannelBuilder} to build a channel.
     *
     * @param service  the {@link GrpcRemoteCacheService} to create the channel for
     *
     * @return a {@link ManagedChannelBuilder} to build a channel
     */
    protected ManagedChannelBuilder<?> createManagedChannelBuilder(GrpcRemoteService<?> service)
        {
        RemoteGrpcServiceDependencies depsService    = service.getDependencies();
        OperationalContext            ctx            = (OperationalContext) service.getCluster();
        String                        sService       = service.getServiceName();
        String                        sKey           = GrpcServiceInfo.createKey(service);
        String                        sRemoteService = depsService.getRemoteServiceName();
        String                        sRemoteCluster = depsService.getRemoteClusterName();
        GrpcChannelDependencies       depsChannel    = depsService.getChannelDependencies();

        m_mapServiceInfo.put(sKey, new GrpcServiceInfo(ctx, sService, sRemoteService, sRemoteCluster, depsChannel));

        String sTarget = depsChannel.getTarget();
        if (sTarget == null)
            {
            sTarget = GrpcChannelFactory.createTargetURI(service);
            }

        SocketProviderBuilder    builder        = depsChannel.getSocketProviderBuilder();
        ChannelCredentials       credentials    = createChannelCredentials(builder);
        ManagedChannelBuilder<?> channelBuilder = Grpc.newChannelBuilder(sTarget, credentials);

        depsChannel.getAuthorityOverride().ifPresent(channelBuilder::overrideAuthority);

        depsChannel.getConfigurer()
                .filter(GrpcChannelConfigurer.class::isInstance)
                .map(GrpcChannelConfigurer.class::cast)
                .ifPresent(c -> c.apply(channelBuilder));

        Map<String, Object> mapServiceConfig = new HashMap<>();
        mapServiceConfig.put("healthCheckConfig", Collections.singletonMap("serviceName", GrpcDependencies.SCOPED_PROXY_SERVICE_NAME));

        channelBuilder.defaultServiceConfig(mapServiceConfig);
        channelBuilder.defaultLoadBalancingPolicy(depsChannel.getDefaultLoadBalancingPolicy());
        channelBuilder.userAgent("Coherence Java Client");

        return channelBuilder;
        }

    protected abstract ChannelCredentials createChannelCredentials(SocketProviderBuilder builder);

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

    /**
     * A custom gRPC {@link NameResolver}.
     */
    public static abstract class AbstractAddressProviderNameResolver
            extends NameResolver
        {
        @SuppressWarnings({"rawtypes", "unchecked"})
        public AbstractAddressProviderNameResolver(GrpcChannelDependencies deps,
                GrpcServiceInfo serviceInfo, Args args)
            {
            m_fNameServiceAddressProvider = deps.isNameServiceAddressProvider();
            m_executorResource            = GrpcUtil.SHARED_CHANNEL_EXECUTOR;
            m_serviceInfo                 = serviceInfo;
            m_nameResolverArgs            = args;

            ParameterizedBuilder<? extends SocketAddressProvider> bldr = deps.getRemoteAddressProviderBuilder();
            if (bldr == null)
                {
                // default to the "cluster-discovery" address provider which consists of MC or WKAs
                AddressProviderFactory factory = serviceInfo.getOperationalContext()
                        .getAddressProviderMap()
                        .get("cluster-discovery");

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
                else
                    {
                    throw new IllegalStateException("Cannot locate the cluster-discovery address provider factory");
                    }
                }

            m_addressProviderBuilder = bldr;
            }

        // ----- NameResolver methods ---------------------------------------

        @Override
        public String getServiceAuthority()
            {
            // should not return null
            return m_sAuthority == null ? "" : m_sAuthority;
            }

        @Override
        public void start(Listener2 listener)
            {
            m_listener = Objects.requireNonNull(listener);
            m_executor = SharedResourceHolder.get(m_executorResource);
            m_resolveTask = new Resolve(this, m_serviceInfo, m_listener);
            m_resolveTask.run();
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

        protected SocketAddressProvider buildSocketAddressProvider()
            {
            ClassLoader loader = Classes.getContextClassLoader();
            return m_addressProviderBuilder.realize(new NullParameterResolver(), loader, null);
            }

        protected boolean isNameServiceAddressProvider()
            {
            return m_fNameServiceAddressProvider;
            }

        protected void setAuthority(String sAuthority)
            {
            m_sAuthority = sAuthority;
            }

        protected String getAuthorityInternal()
            {
            return m_sAuthority;
            }

        protected GrpcServiceInfo getServiceInfo()
            {
            return m_serviceInfo;
            }

        private void resolve()
            {
            if (m_fResolving || m_fShutdown)
            {
            return;
            }
            m_fResolving = true;
            m_executor.execute(m_resolveTask);
            }

        // ----- data members -----------------------------------------------

        private final GrpcServiceInfo m_serviceInfo;

        private final Args m_nameResolverArgs;

        private String m_sAuthority;

        private final ParameterizedBuilder<? extends SocketAddressProvider> m_addressProviderBuilder;

        private final boolean m_fNameServiceAddressProvider;

        private volatile boolean m_fResolving;

        private volatile boolean m_fShutdown;

        private Executor m_executor;

        private final SharedResourceHolder.Resource<Executor> m_executorResource;

        private Listener2 m_listener;

        private Resolve m_resolveTask;
        }

    // ----- inner class: AddressProviderNameResolver -----------------------

    /**
     * A custom gRPC {@link NameResolver}.
     */
    public static class AddressProviderNameResolver
            extends AbstractAddressProviderNameResolver
        {
        public AddressProviderNameResolver(GrpcChannelDependencies deps, GrpcServiceInfo serviceInfo, Args args)
            {
            super(deps, serviceInfo, args);
            }

        @Override
        public String getServiceAuthority()
            {
            if (getAuthorityInternal() == null)
                {
                // run an initial resolve to set the authority
                new Resolve(this, getServiceInfo()).run();
                }
            return super.getServiceAuthority();
            }
        }

    // ----- inner class: Resolve -------------------------------------------

    /**
     * A {@link Runnable} that will actually resolve the gRPC endpoints.
     */
    protected static class Resolve
            implements Runnable
        {
        /**
         * Create a new resolve runnable.
         *
         * @param parent       the parent {@link AbstractAddressProviderNameResolver}
         * @param serviceInfo  the gRPC service info
         */
        protected Resolve(AbstractAddressProviderNameResolver parent, GrpcServiceInfo serviceInfo)
            {
            this(parent, serviceInfo, NullNameResolverListener.INSTANCE);
            }

        /**
         * Create a new resolve runnable.
         *
         * @param parent       the parent {@link AbstractAddressProviderNameResolver}
         * @param serviceInfo  the gRPC service info
         * @param listener     the listener to receive the resolve result
         */
        protected Resolve(AbstractAddressProviderNameResolver parent, GrpcServiceInfo serviceInfo, NameResolver.Listener2 listener)
            {
            f_parent      = Objects.requireNonNull(parent);
            f_serviceInfo = Objects.requireNonNull(serviceInfo);
            f_listener    = Objects.requireNonNull(listener);
            }

        // ----- Runnable implementation ------------------------------------

        @Override
        public void run()
            {
            List<Throwable>               listError       = new ArrayList<>();
            NameResolver.ResolutionResult result          = null;
            int                           cAttempt        = 1;
            GrpcChannelDependencies       dependencies    = f_serviceInfo.getDependencies();
            long                          cTimeoutSeconds = dependencies.getLoadBalancerTimeout();

            if (cTimeoutSeconds < 1)
                {
                cTimeoutSeconds = GrpcChannelDependencies.DEFAULT_LOAD_BALANCER_TIMEOUT
                        .evaluate(new NullParameterResolver()).get();
                }

            try (Timeout ignored = Timeout.after(cTimeoutSeconds, TimeUnit.SECONDS))
                {
                while(result == null)
                    {
                    try
                        {
                        List<SocketAddress> list = f_parent.isNameServiceAddressProvider()
                                ? lookupAddresses()
                                : resolveAddresses();

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
                            NameResolver.Args args          = f_parent.getNameResolverArgs();
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

                            Map<String, Object> config = Collections.singletonMap("serviceName", GrpcDependencies.SCOPED_PROXY_SERVICE_NAME);
                            Attributes          attrs  = Attributes.newBuilder()
                                                                   .set(LoadBalancer.ATTR_HEALTH_CHECKING_CONFIG, config)
                                                                   .build();

                            // ToDo: This is a work around for a bug in gRPC Java 1.60.0
                            // We should be able to create a single EquivalentAddressGroup with all the addresses in
                            // but if we do that gRPC throws an NPE
                            // This code can be changed when gRPC fixes the issues
                            List<EquivalentAddressGroup> listGroup = list.stream()
                                    .map(addr -> new EquivalentAddressGroup(addr, attrs))
                                    .collect(Collectors.toList());

                            result = NameResolver.ResolutionResult.newBuilder()
                                    .setAddresses(listGroup)
                                    .setAttributes(Attributes.EMPTY)
                                    .build();
                            }

                        Logger.config("Refreshed gRPC endpoints: " + result.getAddresses());
                        }
                    catch(Throwable t)
                        {
                        listError.add(t);
                         Logger.finest("Failed to lookup gRPC endpoints, attempts=" + cAttempt
                                + " : " + t.getMessage());

                        Blocking.sleep(1000);
                        cAttempt++;
                        }
                    }
                }
            catch (InterruptedException e)
                {
                // We timed-out trying to look up the endpoints
                // Add any previously thrown exceptions to the InterruptedException
                listError.forEach(e::addSuppressed);

                NameResolver.ConfigOrError error = NameResolver.ConfigOrError
                        .fromError(Status.DEADLINE_EXCEEDED.withDescription(e.getMessage()));

                result = NameResolver.ResolutionResult.newBuilder()
                        .setServiceConfig(error)
                        .setAttributes(Attributes.EMPTY)
                        .build();
                }
            finally
                {
                f_parent.m_fResolving = false;
                }

            f_listener.onResult(result);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Resolve the endpoints.
         *
         * @return  the gRPC endpoint addresses
         */
        protected List<SocketAddress> resolveAddresses()
            {
            SocketAddressProvider addressProvider = f_parent.buildSocketAddressProvider();
            List<SocketAddress>   list            = new ArrayList<>();
            SocketAddress         address         = addressProvider.getNextAddress();
            boolean               fFirst          = true;
            int                   nAddress        = 0;

            Logger.config("Resolving configured remote gRPC endpoints for service " + f_serviceInfo.getService());

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
                    nAddress++;
                    }
                address = addressProvider.getNextAddress();
                }
            if (nAddress > 1)
                {
                Collections.shuffle(list);
                }
            return list;
            }

        /**
         * Update the authority form an address.
         *
         * @param address  the address to use to obtain the authority
         */
        private void updateAuthority(InetSocketAddress address)
            {
            String sAuthority = GrpcUtil.authorityFromHostAndPort(address.getHostString(), address.getPort());
            f_parent.setAuthority(sAuthority);
            }

        /**
         * Lookup the gRPC endpoints using the name service.
         *
         * @return  the gRPC endpoint addresses
         */
        @SuppressWarnings("resource")
        protected List<SocketAddress> lookupAddresses()
            {
            SocketAddressProvider addressProvider = f_parent.buildSocketAddressProvider();
            RemoteNameService     serviceNS       = new RemoteNameService();
            OperationalContext    context         = f_serviceInfo.getOperationalContext();

            Logger.config("Using NameService to lookup remote gRPC endpoints for service " + f_serviceInfo.getService());

            serviceNS.setOperationalContext(context);
            serviceNS.setContextClassLoader(Classes.getContextClassLoader());
            serviceNS.setServiceName(f_serviceInfo.getService() + ':' + NameService.TYPE_REMOTE);

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

            String sServiceRemote = f_serviceInfo.getRemoteService();
            String sCluster       = f_serviceInfo.getRemoteCluster();

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
                    throw new ConnectionException("Unable to locate gRPC proxy service '" + sServiceRemote
                                                        + "' within cluster '" + sCluster + "'");
                    }

                List<SocketAddress> list     = new ArrayList<>();
                int                 nAddress = 0;

                for (int i = 0; i < aoResult.length; i += 2)
                    {
                    list.add(new InetSocketAddress((String) aoResult[i], (Integer) aoResult[i+1]));
                    nAddress++;
                    }

                if (list.isEmpty())
                    {
                    throw new ConnectionException("Unable to locate any addresses in cluster '" + sCluster
                            + "' while looking for its gRPC proxy service '" + sServiceRemote + "'");
                    }

                list.stream()
                        .findAny()
                        .ifPresent(address -> updateAuthority((InetSocketAddress) address));

                if (nAddress > 1)
                    {
                    Collections.shuffle(list);
                    }
                return list;
                }
            catch (Exception ex)
                {
                // we failed to connect, thus the cluster was not reachable
                throw new ConnectionException("Unable to lookup gRPC proxy '" + sServiceRemote + "' in cluster '" + sCluster
                        + "' cause: " + ex.getMessage(), ex);
                }
            finally
                {
                serviceNS.stop();
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The parent {@link AbstractAddressProviderNameResolver}.
         */
        private final AbstractAddressProviderNameResolver f_parent;

        /**
         * The listener to receive the resolver result.
         */
        private final NameResolver.Listener2 f_listener;

        /**
         * The gRPC service info.
         */
        private final GrpcServiceInfo f_serviceInfo;
        }

    // ----- inner class: NullNameResolverListener --------------------------

    /**
     * A no-op implementation of a {@link NameResolver.Listener2}.
     */
    protected static class NullNameResolverListener
            extends NameResolver.Listener2
        {
        @Override
        public void onResult(NameResolver.ResolutionResult resolutionResult)
            {
            }

        @Override
        public void onError(Status error)
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * A singleton instance of {@link NullNameResolverListener}.
         */
        protected static final NullNameResolverListener INSTANCE = new NullNameResolverListener();
        }

    // ----- inner class: GrpcServiceInfo -----------------------------------

    /**
     * A holder for gRPC service information.
     */
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

        public static String createKey(GrpcRemoteService<?> service)
            {
            String sService = service.getServiceName();
            String sScope   = service.getScopeName();
            int    nIdx     = sService.indexOf(ServiceScheme.DELIM_APPLICATION_SCOPE);

            if (sScope == null && nIdx > 0)
                {
                sScope   = sService.substring(0, nIdx);
                sService = sService.substring(nIdx + 1);
                }

            if (sScope == null)
                {
                return sService + KEY_SEPARATOR;
                }
            return sService + KEY_SEPARATOR + sScope;
            }

        public static String parseServiceInfoKey(URI uri)
            {
            String sService = uri.getHost();
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
            if (m_sRemoteService.endsWith(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME))
                {
                return GrpcDependencies.SCOPED_PROXY_SERVICE_NAME;
                }
            return m_sRemoteService + GrpcDependencies.SCOPED_PROXY_SERVICE_NAME;
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

    // ----- data members ---------------------------------------------------

    protected final Map<String, GrpcServiceInfo> m_mapServiceInfo = new ConcurrentHashMap<>();
    }
