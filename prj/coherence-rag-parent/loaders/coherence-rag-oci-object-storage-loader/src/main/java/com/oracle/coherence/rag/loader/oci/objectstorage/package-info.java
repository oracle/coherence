/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * OCI Object Storage document loader implementation for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with Oracle Cloud Infrastructure (OCI) Object Storage
 * for loading documents into the RAG framework. The loader supports OCI authentication
 * methods and provides efficient access to stored documents across OCI regions.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.loader.oci.objectstorage.OciObjectStorageDocumentLoader} - Main OCI Object Storage document loader</li>
 * </ul>
 * <p/>
 * The OCI Object Storage loader provides:
 * <ul>
 * <li>Integration with OCI SDK for Object Storage operations</li>
 * <li>Support for OCI authentication methods (config file, instance principals, delegation tokens)</li>
 * <li>Automatic content type detection</li>
 * <li>Metadata preservation from object metadata</li>
 * <li>Error handling for network and authentication issues</li>
 * <li>Support for OCI regions and compartments</li>
 * </ul>
 * <p/>
 * Authentication methods supported:
 * <ul>
 * <li>OCI config file (~/.oci/config)</li>
 * <li>Instance principals (recommended for compute instances)</li>
 * <li>Resource principals (for Functions and other services)</li>
 * <li>Delegation tokens</li>
 * <li>Manual authentication configuration</li>
 * </ul>
 * <p/>
 * URI format for OCI Object Storage documents:
 * <pre>{@code
 * oci.os://namespace/bucket-name/path/to/document.pdf
 * }</pre>
 * <p/>
 * Example usage:
 * <pre>{@code
 * DocumentLoader loader = new OciObjectStorageDocumentLoader();
 * Collection<Document> documents = loader.load("oci.os://mycompany/documents/technical-specs.pdf");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>OCI configuration file (~/.oci/config)</li>
 * <li>Environment variables (OCI_CONFIG_FILE, OCI_CONFIG_PROFILE)</li>
 * <li>System properties for authentication parameters</li>
 * <li>CDI configuration beans</li>
 * </ul>
 * <p/>
 * The loader automatically handles OCI region detection, tenancy configuration,
 * and compartment access. It supports both standard and archive storage tiers
 * and can work with pre-authenticated requests for secure access.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.loader.oci.objectstorage;
