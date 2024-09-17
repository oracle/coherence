/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.SchemeMappingRegistry;

import com.tangosol.coherence.config.builder.storemanager.AsyncStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.BdbStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.CustomStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.NioFileManagerBuilder;

import com.tangosol.coherence.config.scheme.CacheStoreScheme;

import com.tangosol.coherence.config.scheme.CaffeineScheme;
import com.tangosol.coherence.config.scheme.ClassScheme;
import com.tangosol.coherence.config.scheme.ContinuousQueryCacheScheme;
import com.tangosol.coherence.config.scheme.DistributedScheme;
import com.tangosol.coherence.config.scheme.ExternalScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.InvocationScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.NamedTopicScheme;
import com.tangosol.coherence.config.scheme.NearScheme;
import com.tangosol.coherence.config.scheme.OptimisticScheme;
import com.tangosol.coherence.config.scheme.OverflowScheme;
import com.tangosol.coherence.config.scheme.PagedExternalScheme;
import com.tangosol.coherence.config.scheme.ProxyScheme;
import com.tangosol.coherence.config.scheme.RamJournalScheme;
import com.tangosol.coherence.config.scheme.ReadWriteBackingMapScheme;
import com.tangosol.coherence.config.scheme.RemoteCacheScheme;
import com.tangosol.coherence.config.scheme.BaseGrpcCacheScheme;
import com.tangosol.coherence.config.scheme.RemoteInvocationScheme;
import com.tangosol.coherence.config.scheme.ReplicatedScheme;
import com.tangosol.coherence.config.scheme.TransactionalScheme;
import com.tangosol.coherence.config.scheme.ViewScheme;

import com.tangosol.coherence.config.xml.preprocessor.DefaultsCreationPreprocessor;
import com.tangosol.coherence.config.xml.preprocessor.ExtendPreprocessor;
import com.tangosol.coherence.config.xml.preprocessor.OperationalDefaultsPreprocessor;
import com.tangosol.coherence.config.xml.preprocessor.PofSerializerPreprocessor;
import com.tangosol.coherence.config.xml.preprocessor.SchemeRefPreprocessor;
import com.tangosol.coherence.config.xml.preprocessor.SystemPropertyPreprocessor;
import com.tangosol.coherence.config.xml.preprocessor.TCPAcceptorPreprocessor;

import com.tangosol.coherence.config.xml.processor.AcceptorDependenciesProcessor;
import com.tangosol.coherence.config.xml.processor.AddressProviderBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.AsyncBackupProcessor;
import com.tangosol.coherence.config.xml.processor.AuthorizedHostsProcessor;
import com.tangosol.coherence.config.xml.processor.BackingMapSchemeProcessor;
import com.tangosol.coherence.config.xml.processor.BufferTypeProcessor;
import com.tangosol.coherence.config.xml.processor.CacheConfigOverrideProcessor;
import com.tangosol.coherence.config.xml.processor.CacheMappingProcessor;
import com.tangosol.coherence.config.xml.processor.CacheServiceProxyProcessor;
import com.tangosol.coherence.config.xml.processor.CachingSchemeMappingProcessor;
import com.tangosol.coherence.config.xml.processor.CachingSchemesProcessor;
import com.tangosol.coherence.config.xml.processor.CompositeSchemeProcessor;
import com.tangosol.coherence.config.xml.processor.ConfigurationProcessor;
import com.tangosol.coherence.config.xml.processor.CustomizableBinaryStoreManagerBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.CustomizableBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.DefaultsProcessor;
import com.tangosol.coherence.config.xml.processor.DeltaCompressorProcessor;
import com.tangosol.coherence.config.xml.processor.ElementCalculatorProcessor;
import com.tangosol.coherence.config.xml.processor.EvictionPolicyProcessor;
import com.tangosol.coherence.config.xml.processor.ExecutorProcessor;
import com.tangosol.coherence.config.xml.processor.HealthProcessor;
import com.tangosol.coherence.config.xml.processor.HttpAcceptorDependenciesProcessor;
import com.tangosol.coherence.config.xml.processor.InitParamProcessor;
import com.tangosol.coherence.config.xml.processor.InitParamsProcessor;
import com.tangosol.coherence.config.xml.processor.InitiatorDependenciesProcessor;
import com.tangosol.coherence.config.xml.processor.InstanceProcessor;
import com.tangosol.coherence.config.xml.processor.InterceptorProcessor;
import com.tangosol.coherence.config.xml.processor.InterceptorsProcessor;
import com.tangosol.coherence.config.xml.processor.InternalCacheSchemeProcessor;
import com.tangosol.coherence.config.xml.processor.InvocationServiceProxyProcessor;
import com.tangosol.coherence.config.xml.processor.KeystoreProcessor;
import com.tangosol.coherence.config.xml.processor.LeaseGranularityProcessor;
import com.tangosol.coherence.config.xml.processor.LocalAddressProcessor;
import com.tangosol.coherence.config.xml.processor.MapListenerProcessor;
import com.tangosol.coherence.config.xml.processor.MemberListenerProcessor;
import com.tangosol.coherence.config.xml.processor.MemorySizeProcessor;
import com.tangosol.coherence.config.xml.processor.MillisProcessor;
import com.tangosol.coherence.config.xml.processor.NoOpElementProcessor;
import com.tangosol.coherence.config.xml.processor.OperationBundlingProcessor;
import com.tangosol.coherence.config.xml.processor.PagedTopicSchemeProcessor;
import com.tangosol.coherence.config.xml.processor.ParamTypeProcessor;
import com.tangosol.coherence.config.xml.processor.PartitionAssignmentStrategyProcessor;
import com.tangosol.coherence.config.xml.processor.PartitionListenerProcessor;
import com.tangosol.coherence.config.xml.processor.PartitionedQuorumPolicyProcessor;
import com.tangosol.coherence.config.xml.processor.PasswordURLProcessor;
import com.tangosol.coherence.config.xml.processor.PersistenceProcessor;
import com.tangosol.coherence.config.xml.processor.ProviderProcessor;
import com.tangosol.coherence.config.xml.processor.ProxyQuorumPolicyProcessor;
import com.tangosol.coherence.config.xml.processor.ReadLocatorProcessor;
import com.tangosol.coherence.config.xml.processor.ResourceBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.SSLHostnameVerifierProcessor;
import com.tangosol.coherence.config.xml.processor.SSLManagerProcessor;
import com.tangosol.coherence.config.xml.processor.SSLNameListProcessor;
import com.tangosol.coherence.config.xml.processor.SSLProcessor;
import com.tangosol.coherence.config.xml.processor.SchemesProcessor;
import com.tangosol.coherence.config.xml.processor.ScopeNameProcessor;
import com.tangosol.coherence.config.xml.processor.SerializerFactoryProcessor;
import com.tangosol.coherence.config.xml.processor.ServiceBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.ServiceFailurePolicyProcessor;
import com.tangosol.coherence.config.xml.processor.ServiceLoadBalancerProcessor;
import com.tangosol.coherence.config.xml.processor.SocketOptionsProcessor;
import com.tangosol.coherence.config.xml.processor.SocketProviderProcessor;
import com.tangosol.coherence.config.xml.processor.SpecificInstanceProcessor;
import com.tangosol.coherence.config.xml.processor.StorageProcessor;
import com.tangosol.coherence.config.xml.processor.SubscriberGroupProcessor;
import com.tangosol.coherence.config.xml.processor.SubscriberGroupsProcessor;
import com.tangosol.coherence.config.xml.processor.TcpAcceptorProcessor;
import com.tangosol.coherence.config.xml.processor.TcpInitiatorProcessor;
import com.tangosol.coherence.config.xml.processor.TopicMappingProcessor;
import com.tangosol.coherence.config.xml.processor.TopicSchemeMappingProcessor;
import com.tangosol.coherence.config.xml.processor.TransformerProcessor;
import com.tangosol.coherence.config.xml.processor.UnitCalculatorProcessor;
import com.tangosol.coherence.config.xml.processor.UnsupportedFeatureProcessor;
import com.tangosol.coherence.config.xml.processor.ValueStorageSchemeProcessor;
import com.tangosol.coherence.config.xml.processor.WrapperStreamFactoryListProcessor;

import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.OverrideProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.config.xml.SimpleElementProcessor;
import com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.DefaultMemcachedAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketOptions;

import com.tangosol.net.messaging.Codec;

import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.net.ssl.URLCertificateLoader;
import com.tangosol.net.ssl.URLKeyStoreLoader;
import com.tangosol.net.ssl.URLPrivateKeyLoader;
import com.tangosol.run.xml.XmlElement;

import static com.tangosol.coherence.config.xml.processor.AbstractEmptyElementProcessor.EmptyElementBehavior;

import java.net.URI;
import java.util.ServiceLoader;

/**
 * The {@link CacheConfigNamespaceHandler} is responsible for capturing and creating Coherence
 * Cache Configurations when processing a Coherence Configuration file.
 *
 * @author bo  2011.05.26
 * @since Coherence 12.1.2
 */
public class CacheConfigNamespaceHandler
        extends AbstractNamespaceHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Standard Constructor.
     */
    public CacheConfigNamespaceHandler()
        {
        // define the DocumentElementPreprocessor for the Cache Config Namespace
        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        // add the DefaultsCreationPreprocessor to automatically create
        // appropriate <defaults> elements (like for pof)
        dep.addElementPreprocessor(new DefaultsCreationPreprocessor());

        // add the pre-processor for scheme-ref
        dep.addElementPreprocessor(SchemeRefPreprocessor.INSTANCE);

        // add the pre-processor for Coherence*Extend
        dep.addElementPreprocessor(ExtendPreprocessor.INSTANCE);

        // add the pre-processor to merge operational service configuration
        OperationalDefaultsPreprocessor odp = new OperationalDefaultsPreprocessor();

        odp.addDefaultsDefinition("distributed-scheme", CacheFactory.getServiceConfig(CacheService.TYPE_DISTRIBUTED));
        odp.addDefaultsDefinition("invocation-scheme", CacheFactory.getServiceConfig(InvocationService.TYPE_DEFAULT));
        odp.addDefaultsDefinition("local-scheme", CacheFactory.getServiceConfig(CacheService.TYPE_LOCAL));
        odp.addDefaultsDefinition("optimistic-scheme", CacheFactory.getServiceConfig(CacheService.TYPE_OPTIMISTIC));
        odp.addDefaultsDefinition("proxy-scheme", CacheFactory.getServiceConfig("Proxy"));
        odp.addDefaultsDefinition("replicated-scheme", CacheFactory.getServiceConfig(CacheService.TYPE_REPLICATED));
        odp.addDefaultsDefinition("remote-cache-scheme", CacheFactory.getServiceConfig(CacheService.TYPE_REMOTE));
        odp.addDefaultsDefinition("remote-grpc-cache-scheme",
                                  CacheFactory.getServiceConfig(CacheService.TYPE_REMOTE_GRPC));
        odp.addDefaultsDefinition("remote-invocation-scheme",
                                  CacheFactory.getServiceConfig(InvocationService.TYPE_REMOTE));

        odp.addDefaultsDefinition("paged-topic-scheme", CacheFactory.getServiceConfig(CacheService.TYPE_DISTRIBUTED));

        dep.addElementPreprocessor(odp);

        // add the pre-processor for POF serializer
        dep.addElementPreprocessor(new PofSerializerPreprocessor());

        // add the system property pre-processor
        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        // add the acceptor-config pre-processor
        dep.addElementPreprocessor(new TCPAcceptorPreprocessor());

        setDocumentPreprocessor(dep);

        // register the type-based ElementProcessors (in alphabetical order)
        registerProcessor(AcceptorDependenciesProcessor.class);
        registerProcessor(AsyncBackupProcessor.class);
        registerProcessor(AuthorizedHostsProcessor.class);
        registerProcessor(BackingMapSchemeProcessor.class);
        registerProcessor(BufferTypeProcessor.class);
        registerProcessor(CacheServiceProxyProcessor.class);
        registerProcessor(CacheMappingProcessor.class);
        registerProcessor(CachingSchemeMappingProcessor.class);
        registerProcessor(CachingSchemesProcessor.class);
        registerProcessor(ConfigurationProcessor.class);
        registerProcessor(DefaultsProcessor.class);
        registerProcessor(DeltaCompressorProcessor.class);
        registerProcessor(ElementCalculatorProcessor.class);
        registerProcessor(ExecutorProcessor.class);
        registerProcessor(EvictionPolicyProcessor.class);
        registerProcessor(HealthProcessor.class);
        registerProcessor(HttpAcceptorDependenciesProcessor.class);
        registerProcessor(InitiatorDependenciesProcessor.class);
        registerProcessor(InitParamProcessor.class);
        registerProcessor(InitParamsProcessor.class);
        registerProcessor(InstanceProcessor.class);
        registerProcessor(InterceptorProcessor.class);
        registerProcessor(InterceptorsProcessor.class);
        registerProcessor(InternalCacheSchemeProcessor.class);
        registerProcessor(InvocationServiceProxyProcessor.class);
        registerProcessor(KeystoreProcessor.class);
        registerProcessor(LeaseGranularityProcessor.class);
        registerProcessor(LocalAddressProcessor.class);
        registerProcessor(MapListenerProcessor.class);
        registerProcessor(MemberListenerProcessor.class);
        registerProcessor(OperationBundlingProcessor.class);
        registerProcessor(PagedTopicSchemeProcessor.class);
        registerProcessor(ParamTypeProcessor.class);
        registerProcessor(PartitionAssignmentStrategyProcessor.class);
        registerProcessor(PartitionedQuorumPolicyProcessor.class);
        registerProcessor(PartitionListenerProcessor.class);
        registerProcessor(PasswordURLProcessor.class);
        registerProcessor(PersistenceProcessor.class);
        registerProcessor(ProviderProcessor.class);
        registerProcessor(ProxyQuorumPolicyProcessor.class);
        registerProcessor(ReadLocatorProcessor.class);
        registerProcessor(ResourceBuilderProcessor.class);
        registerProcessor(SchemesProcessor.class);
        registerProcessor(ScopeNameProcessor.class);
        registerProcessor(SerializerFactoryProcessor.class);
        registerProcessor(ServiceFailurePolicyProcessor.class);
        registerProcessor(ServiceLoadBalancerProcessor.class);
        registerProcessor(SocketProviderProcessor.class);
        registerProcessor(SSLProcessor.class);
        registerProcessor(SSLHostnameVerifierProcessor.class);
        registerProcessor(StorageProcessor.class);
        registerProcessor(SubscriberGroupProcessor.class);
        registerProcessor(SubscriberGroupsProcessor.class);
        registerProcessor(TcpAcceptorProcessor.class);
        registerProcessor(TcpInitiatorProcessor.class);
        registerProcessor(TopicSchemeMappingProcessor.class);
        registerProcessor(TransformerProcessor.class);
        registerProcessor(UnitCalculatorProcessor.class);
        registerProcessor(ValueStorageSchemeProcessor.class);
        registerProcessor(WrapperStreamFactoryListProcessor.class);

        // register customized ElementProcessors (in alphabetical order)
        registerProcessor("address-provider", new AddressProviderBuilderProcessor());
        registerProcessor("async-store-manager",
                          new CustomizableBinaryStoreManagerBuilderProcessor<>(AsyncStoreManagerBuilder.class));
        registerProcessor("backup-storage",
                          new CustomizableBuilderProcessor<>(DistributedScheme.BackupConfig.class));
        registerProcessor("bdb-store-manager",
                          new CustomizableBuilderProcessor<>(BdbStoreManagerBuilder.class));
        registerProcessor("buffer-size", new MemorySizeProcessor());
        registerProcessor("cachestore-scheme",
                          new CustomizableBuilderProcessor<>(CacheStoreScheme.class));
        registerProcessor("cert", new SimpleElementProcessor<>(URLCertificateLoader.class));
        registerProcessor("cert-loader", new InstanceProcessor());
        registerProcessor("class-scheme", new CustomizableBuilderProcessor<>(ClassScheme.class));
        registerProcessor("continuous-query-cache-scheme",
                          new ServiceBuilderProcessor<>(ContinuousQueryCacheScheme.class));
        registerProcessor("custom-store-manager",
                          new CustomizableBuilderProcessor<>(CustomStoreManagerBuilder.class));
        registerProcessor("connect-timeout", new MillisProcessor());
        registerProcessor("cipher-suites", new SSLNameListProcessor("cipher-suites"));
        registerProcessor("distributed-scheme",
                          new ServiceBuilderProcessor<>(DistributedScheme.class));
        registerProcessor("ensure-cache-timeout", new MillisProcessor());
        registerProcessor("external-scheme",
                          new CustomizableBinaryStoreManagerBuilderProcessor<>(ExternalScheme.class));
        registerProcessor("federated-scheme", new UnsupportedFeatureProcessor("Federated Caching"));
        registerProcessor("flashjournal-scheme",
                          new CustomizableBuilderProcessor<>(FlashJournalScheme.class));
        registerProcessor("guardian-timeout", new MillisProcessor());
        registerProcessor("grpc-channel", NoOpElementProcessor.INSTANCE);
        registerProcessor("heartbeat-interval", new MillisProcessor());
        registerProcessor("heartbeat-timeout", new MillisProcessor());
        registerProcessor("identity-manager", new SSLManagerProcessor());
        registerProcessor("key", new SimpleElementProcessor<>(URLPrivateKeyLoader.class));
        registerProcessor("key-loader", new InstanceProcessor());
        registerProcessor("key-store-loader", new InstanceProcessor());
        registerProcessor("key-associator",
                          new SpecificInstanceProcessor<>(KeyAssociator.class,
                                                          EmptyElementBehavior.IGNORE));
        registerProcessor("key-partitioning",
                          new SpecificInstanceProcessor<>(KeyPartitioningStrategy.class,
                                                          EmptyElementBehavior.IGNORE));
        registerProcessor("invocation-scheme", new ServiceBuilderProcessor<>(InvocationScheme.class));
        registerProcessor("limit-buffer-size", new MemorySizeProcessor());
        registerProcessor("local-scheme", new CustomizableBuilderProcessor<>(LocalScheme.class));
        registerProcessor("caffeine-scheme", new CustomizableBuilderProcessor<>(CaffeineScheme.class));

        registerProcessor("max-message-size", new MemorySizeProcessor());
        registerProcessor("message-codec", new SpecificInstanceProcessor<>(Codec.class));
        registerProcessor("message-expiration", new MillisProcessor());
        registerProcessor("name-service-addresses", new AddressProviderBuilderProcessor());
        registerProcessor("near-scheme", new CompositeSchemeProcessor<>(NearScheme.class));
        registerProcessor("nio-file-manager",
                          new CustomizableBuilderProcessor<>(NioFileManagerBuilder.class));
        registerProcessor("nominal-buffer-size", new MemorySizeProcessor());
        registerProcessor("optimistic-scheme", new ServiceBuilderProcessor<>(OptimisticScheme.class));
        registerProcessor("overflow-scheme", new CompositeSchemeProcessor<>(OverflowScheme.class));
        registerProcessor("paged-external-scheme",
                          new CustomizableBinaryStoreManagerBuilderProcessor<>(PagedExternalScheme.class));
        registerProcessor("protocol-versions", new SSLNameListProcessor("protocol-versions"));
        registerProcessor("proxy-scheme",
                          new ServiceBuilderProcessor<>(ProxyScheme.class));
        registerProcessor("ramjournal-scheme",
                          new CustomizableBuilderProcessor<>(RamJournalScheme.class));
        registerProcessor("read-write-backing-map-scheme",
                          new CustomizableBuilderProcessor<>(ReadWriteBackingMapScheme.class));
        registerProcessor("remote-addresses", new AddressProviderBuilderProcessor());

        registerProcessor("remote-cache-scheme",
                          new ServiceBuilderProcessor<>(RemoteCacheScheme.class));
        registerProcessor("remote-grpc-cache-scheme",
                          new ServiceBuilderProcessor<>(BaseGrpcCacheScheme.class));
        registerProcessor("remote-invocation-scheme",
                          new ServiceBuilderProcessor<>(RemoteInvocationScheme.class));
        registerProcessor("replicated-scheme", new ServiceBuilderProcessor<>(ReplicatedScheme.class));
        registerProcessor("request-timeout", new MillisProcessor());
        registerProcessor("standard-lease-milliseconds", new MillisProcessor());
        registerProcessor("suspect-buffer-size", new MemorySizeProcessor());
        registerProcessor("task-hung-threshold", new MillisProcessor());
        registerProcessor("task-timeout", new MillisProcessor());
        registerProcessor("cache", new CacheMappingProcessor());
        registerProcessor("topic-mapping", new TopicMappingProcessor("topic-name", NamedTopicScheme.class));
        registerProcessor("topic-scheme", new PagedTopicSchemeProcessor());
        registerProcessor("transactional-scheme",
                          new ServiceBuilderProcessor<>(TransactionalScheme.class));
        registerProcessor("transfer-threshold", new MemorySizeProcessor());
        registerProcessor("trust-manager", new SSLManagerProcessor());
        registerProcessor("url", new SimpleElementProcessor<>(URLKeyStoreLoader.class));
        registerProcessor("view-scheme", new CompositeSchemeProcessor<>(ViewScheme.class));

        // register injectable types (in alphabetical order)
        registerElementType("grpc-acceptor", DefaultGrpcAcceptorDependencies.class);
        registerElementType("memcached-acceptor", DefaultMemcachedAcceptorDependencies.class);

        registerElementType("incoming-buffer-pool", DefaultTcpAcceptorDependencies.PoolConfig.class);
        registerElementType("outgoing-buffer-pool", DefaultTcpAcceptorDependencies.PoolConfig.class);

        // load any namespace extensions
        ServiceLoader.load(Extension.class).forEach(e -> e.extend(this));
        }

    // ----- NamespaceHandler interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartNamespace(ProcessingContext context, XmlElement element, String prefix, URI uri)
        {
        super.onStartNamespace(context, element, prefix, uri);

        // register the element processors for the namespace based on types
        // (in alphabetical order)
        context.registerProcessor(SocketOptions.class, new SocketOptionsProcessor());

        // ensure we have a CacheConfig available in the context of this namespace
        if (context.getCookie(CacheConfig.class) == null)
            {
            CacheConfig cacheConfig = new CacheConfig(context.getDefaultParameterResolver());

            cacheConfig.addCacheMappingRegistry(new SchemeMappingRegistry());
            context.addCookie(CacheConfig.class, cacheConfig);
            }

        // ensure we have an OperationalContext in the context of this namespace
        if (context.getCookie(OperationalContext.class) == null)
            {
            context.addCookie(OperationalContext.class, (OperationalContext) CacheFactory.getCluster());
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public OverrideProcessor getOverrideProcessor()
        {
        if (m_overrideProcessor == null)
            {
            m_overrideProcessor = new CacheConfigOverrideProcessor();
            }

        return m_overrideProcessor;
        }

    // ----- inner interface Extension --------------------------------------

    /**
     * A class that can add extensions to the {@link CacheConfigNamespaceHandler}.
     * <p>
     * Extensions will be discovered at runtime using the {@link ServiceLoader}.
     */
    public interface Extension
        {
        /**
         * Add any extensions to the specified {@link CacheConfigNamespaceHandler}.
         *
         * @param handler  the {@link CacheConfigNamespaceHandler} to extend
         */
        void extend(CacheConfigNamespaceHandler handler);
        }

    // ----- Data members ---------------------------------------------------

    /**
     * {@link OverrideProcessor} for processing cache config override xml file.
     */
    private OverrideProcessor m_overrideProcessor;
    }
