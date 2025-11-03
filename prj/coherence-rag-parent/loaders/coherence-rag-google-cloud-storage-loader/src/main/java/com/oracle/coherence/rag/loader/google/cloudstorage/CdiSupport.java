/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.google.cloudstorage;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI configuration support for Google Cloud Storage document loader integration.
 * <p/>
 * This class provides CDI producers for Google Cloud Storage client configuration,
 * enabling dependency injection of properly configured storage clients
 * throughout the application.
 * <p/>
 * The configuration uses Google Cloud's default credential resolution mechanism,
 * which automatically detects and uses available credentials from various sources
 * including environment variables, service account files, and compute engine
 * metadata services.
 * <p/>
 * Authentication is resolved using the standard Google Cloud credential chain:
 * <ol>
 * <li>Environment variable GOOGLE_APPLICATION_CREDENTIALS pointing to service account file</li>
 * <li>User credentials from gcloud auth application-default login</li>
 * <li>Service account attached to the compute resource (Google Cloud, App Engine, etc.)</li>
 * <li>Google Cloud SDK default credentials</li>
 * </ol>
 * <p/>
 * No additional configuration properties are required as the client uses
 * Google Cloud's standard default configuration and credential resolution.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
class CdiSupport
    {
    /**
     * Produces a configured Google Cloud Storage client for dependency injection.
     * <p/>
     * The client is configured using Google Cloud's default instance configuration,
     * which automatically resolves project ID and credentials using the standard
     * Google Cloud authentication mechanisms.
     * 
     * @return a configured Storage client instance
     */
    @Produces
    static Storage storageClient()
        {
        return StorageOptions.getDefaultInstance().getService();
        }
    }
