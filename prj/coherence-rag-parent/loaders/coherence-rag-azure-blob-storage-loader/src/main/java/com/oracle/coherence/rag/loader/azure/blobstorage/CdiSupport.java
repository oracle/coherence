/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.azure.blobstorage;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import com.tangosol.coherence.config.Config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI configuration support for Azure Blob Storage document loader integration.
 * <p/>
 * This class provides CDI producers for Azure Blob Storage client configuration,
 * enabling dependency injection of properly configured blob service clients
 * throughout the application.
 * <p/>
 * The configuration requires an Azure Storage connection string to be specified
 * via the {@code azure.blob.client} system property. This connection string
 * contains all necessary authentication and endpoint information for connecting
 * to Azure Blob Storage.
 * <p/>
 * Required configuration:
 * <ul>
 * <li>{@code azure.blob.client} - Azure Storage connection string</li>
 * </ul>
 * <p/>
 * The connection string typically follows this format:
 * {@code DefaultEndpointsProtocol=https;AccountName=<account>;AccountKey=<key>;EndpointSuffix=core.windows.net}
 * <p/>
 * Alternative authentication methods (Managed Identity, Service Principal)
 * can be configured by modifying the builder configuration in the producer method.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
class CdiSupport
    {
    /**
     * Produces a configured Azure Blob Storage service client for dependency injection.
     * <p/>
     * The client is configured using the connection string specified by the
     * {@code azure.blob.client} system property.
     * 
     * @return a configured BlobServiceClient instance
     */
    @Produces
    static BlobServiceClient blobServiceClient()
        {
        return new BlobServiceClientBuilder()
            .connectionString(Config.getProperty("azure.blob.client"))
            .buildClient();
        }
    }
