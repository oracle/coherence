/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml;

import com.tangosol.coherence.config.xml.preprocessor.SystemPropertyPreprocessor;
import com.tangosol.coherence.config.xml.processor.AddressProviderBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.ExecutorProcessor;
import com.tangosol.coherence.config.xml.processor.InitParamProcessor;
import com.tangosol.coherence.config.xml.processor.InitParamsProcessor;
import com.tangosol.coherence.config.xml.processor.InstanceProcessor;
import com.tangosol.coherence.config.xml.processor.InterceptorProcessor;
import com.tangosol.coherence.config.xml.processor.InterceptorsProcessor;
import com.tangosol.coherence.config.xml.processor.KeystoreProcessor;
import com.tangosol.coherence.config.xml.processor.ParamTypeProcessor;
import com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.PasswordProvidersProcessor;
import com.tangosol.coherence.config.xml.processor.PasswordURLProcessor;
import com.tangosol.coherence.config.xml.processor.PersistenceEnvironmentsProcessor;
import com.tangosol.coherence.config.xml.processor.ProviderProcessor;
import com.tangosol.coherence.config.xml.processor.ResourceBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.ResourcesProcessor;
import com.tangosol.coherence.config.xml.processor.SSLHostnameVerifierProcessor;
import com.tangosol.coherence.config.xml.processor.SSLManagerProcessor;
import com.tangosol.coherence.config.xml.processor.SSLNameListProcessor;
import com.tangosol.coherence.config.xml.processor.SSLProcessor;
import com.tangosol.coherence.config.xml.processor.SerializerBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.SerializersProcessor;
import com.tangosol.coherence.config.xml.processor.SocketProviderProcessor;
import com.tangosol.coherence.config.xml.processor.SocketProvidersProcessor;
import com.tangosol.coherence.config.xml.processor.StorageAccessAuthorizerBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.StorageAccessAuthorizersProcessor;
import com.tangosol.coherence.config.xml.processor.UnsupportedFeatureProcessor;

import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.config.xml.SimpleElementProcessor;

import com.tangosol.net.ssl.URLCertificateLoader;
import com.tangosol.net.ssl.URLKeyStoreLoader;
import com.tangosol.net.ssl.URLPrivateKeyLoader;
import com.tangosol.run.xml.XmlElement;

import java.net.URI;

/**
 * The {@link OperationalConfigNamespaceHandler} is responsible for capturing and
 * creating the Coherence operational configuration when processing a Coherence
 * operational configuration file.
 *
 * @author pfm  2013.03.21
 * @since Coherence 12.2.1
 */
public class OperationalConfigNamespaceHandler
        extends AbstractNamespaceHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Standard Constructor.
     */
    public OperationalConfigNamespaceHandler()
        {
        // define the DocumentPreprocessor for the OperationalConfig namespace
        DocumentElementPreprocessor dep = new DocumentElementPreprocessor();

        // add the system property pre-processor
        dep.addElementPreprocessor(SystemPropertyPreprocessor.INSTANCE);

        setDocumentPreprocessor(dep);

        // register the type-based ElementProcessors
        registerProcessor(AddressProviderBuilderProcessor.class);
        registerProcessor(ExecutorProcessor.class);
        registerProcessor(InitParamProcessor.class);
        registerProcessor(InitParamsProcessor.class);
        registerProcessor(InstanceProcessor.class);
        registerProcessor(InterceptorProcessor.class);
        registerProcessor(InterceptorsProcessor.class);
        registerProcessor(KeystoreProcessor.class);
        registerProcessor(ParamTypeProcessor.class);
        registerProcessor(PasswordProviderBuilderProcessor.class);
        registerProcessor(PasswordProvidersProcessor.class);
        registerProcessor(PasswordURLProcessor.class);
        registerProcessor(PersistenceEnvironmentsProcessor.class);
        registerProcessor(PersistenceEnvironmentsProcessor.PersistenceEnvironmentProcessor.class);
        registerProcessor(ProviderProcessor.class);
        registerProcessor(ResourceBuilderProcessor.class);
        registerProcessor(ResourcesProcessor.class);
        registerProcessor(SerializerBuilderProcessor.class);
        registerProcessor(SerializersProcessor.class);
        registerProcessor(SocketProviderProcessor.class);
        registerProcessor(SSLProcessor.class);
        registerProcessor(SSLHostnameVerifierProcessor.class);
        registerProcessor(StorageAccessAuthorizerBuilderProcessor.class);
        registerProcessor(StorageAccessAuthorizersProcessor.class);
        registerProcessor(SocketProvidersProcessor.class);

        // register customized ElementProcessors
        registerProcessor("address-provider", new AddressProviderBuilderProcessor());
        registerProcessor("cert", new SimpleElementProcessor<>(URLCertificateLoader.class));
        registerProcessor("cert-loader", new InstanceProcessor());
        registerProcessor("cipher-suites", new SSLNameListProcessor("cipher-suites"));
        registerProcessor("identity-manager", new SSLManagerProcessor());
        registerProcessor("key", new SimpleElementProcessor<>(URLPrivateKeyLoader.class));
        registerProcessor("key-loader", new InstanceProcessor());
        registerProcessor("key-store-loader", new InstanceProcessor());
        registerProcessor("name-service-addresses", new AddressProviderBuilderProcessor());
        registerProcessor("protocol-versions", new SSLNameListProcessor("protocol-versions"));
        registerProcessor("remote-addresses", new AddressProviderBuilderProcessor());
        registerProcessor("socket-provider", new SocketProviderProcessor());
        registerProcessor("trust-manager", new SSLManagerProcessor());
        registerProcessor("url", new SimpleElementProcessor<>(URLKeyStoreLoader.class));

        // register injectable types (in alphabetical order)
        registerProcessor("federation-config", new UnsupportedFeatureProcessor("Federated Caching"));
        }

    // ----- NamespaceHandler interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartNamespace(ProcessingContext context, XmlElement element, String prefix, URI uri)
        {
        super.onStartNamespace(context, element, prefix, uri);
        }
    }
