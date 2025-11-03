/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal;

import com.oracle.coherence.rag.DocumentLoader;
import com.oracle.coherence.rag.util.Timer;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.LocalSemaphore;
import com.oracle.coherence.concurrent.Semaphore;
import com.oracle.coherence.concurrent.Semaphores;
import com.tangosol.coherence.config.Config;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.cache.CacheLoader;

import dev.langchain4j.data.document.Document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import jakarta.inject.Named;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Coherence cache loader for on-demand document loading and caching.
 * <p/>
 * This CDI-managed cache loader integrates with Coherence's caching framework
 * to provide on-demand loading of documents from various sources. It implements
 * the {@link CacheLoader} interface to automatically load documents when they
 * are requested but not present in the cache.
 * <p/>
 * The loader provides the following key features:
 * <ul>
 *   <li>Protocol-agnostic document loading via pluggable {@link DocumentLoader} implementations</li>
 *   <li>Configurable concurrency control per URI scheme to prevent resource exhaustion</li>
 *   <li>Performance monitoring with load timing and logging</li>
 *   <li>Robust error handling with detailed logging</li>
 *   <li>CDI integration for dependency injection and lifecycle management</li>
 * </ul>
 * <p/>
 * Concurrency is controlled through configurable semaphores that limit the number
 * of concurrent document loading operations per URI scheme. This prevents overwhelming
 * external document sources and provides backpressure control.
 * <p/>
 * Configuration properties:
 * <ul>
 *   <li>{@code coherence.rag.loader.maxConcurrency} - Global maximum concurrency</li>
 *   <li>{@code coherence.rag.loader.{scheme}.maxConcurrency} - Per-scheme maximum concurrency</li>
 * </ul>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@Named("DocumentCacheLoader")
@ApplicationScoped
public class DocumentCacheLoader
        implements CacheLoader<String, Document>
    {
    /**
     * CDI bean manager for dynamic lookup of DocumentLoader implementations.
     * <p/>
     * This is used to find the appropriate DocumentLoader based on the URI scheme
     * of the document being loaded, enabling protocol-specific handling.
     */
    @Inject
    private BeanManager beanManager;

    /**
     * Loads a document from the specified URI with concurrency control.
     * <p/>
     * This method implements the core document loading logic with the following steps:
     * <ol>
     *   <li>Parse the document ID as a URI to determine the scheme</li>
     *   <li>Find the appropriate DocumentLoader implementation for the scheme</li>
     *   <li>Acquire a semaphore permit to control concurrency</li>
     *   <li>Load the document using the selected loader</li>
     *   <li>Log performance metrics and release the semaphore</li>
     * </ol>
     * <p/>
     * The method includes comprehensive error handling and logging to aid in
     * troubleshooting document loading issues. All exceptions are caught and
     * logged, returning null to indicate load failure.
     * 
     * @param docId the document identifier, must be a valid URI string
     * 
     * @return the loaded Document object, or null if loading fails
     */
    public Document load(String docId)
        {
        URI    uri    = URI.create(docId);
        String sScheme = uri.getScheme();

        DocumentLoader loader = findDocumentLoader(sScheme);
        if (loader == null)
            {
            return null;
            }

        String    sName          = "coherence.rag.loader.%s.maxConcurrency".formatted(sScheme);
        int       maxConcurrency = Config.getInteger(sName, Config.getInteger("coherence.rag.loader.maxConcurrency", 0));
        Semaphore semaphore      = maxConcurrency > 0 ? Semaphores.localSemaphore(sName, maxConcurrency) : new LocalSemaphore(1);

        try
            {
            while (!semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS))
                {
                GuardSupport.heartbeat(1000);
                }

            Timer timer = new Timer().start();

            Document doc = loader.load(uri);
            if (doc == null)
                {
                Logger.info("Unable to load document %s".formatted(docId));
                }

            long time = timer.stop().duration().toMillis();
            Logger.fine("Loaded %s in %,d ms".formatted(docId, time));

            return doc;
            }
        catch (Throwable e)
            {
            Throwable cause = Exceptions.getRootCause(e);
            Logger.info("Failed to load document %s: %s".formatted(cause.getClass().getName(), cause.getMessage()));
            }
        finally
            {
            semaphore.release();
            }

        return null;
        }

    /**
     * Bulk loading method (not implemented).
     * <p/>
     * This method is part of the CacheLoader interface but is not currently
     * implemented. Individual document loading through {@link #load(String)}
     * is the supported approach.
     * 
     * @param colKeys collection of document IDs to load
     * 
     * @return map of loaded documents
     * 
     * @throws NotImplementedException always, as bulk loading is not supported
     */
    public Map<String, Document> loadAll(Collection<? extends String> colKeys)
        {
        throw new NotImplementedException("loadAll is not implemented");
        }

    /**
     * Finds the appropriate DocumentLoader implementation for a given URI scheme.
     * <p/>
     * This method uses CDI's BeanManager to dynamically locate DocumentLoader
     * implementations that are registered with a name matching the URI scheme.
     * This enables protocol-specific document loading (file, http, https, etc.).
     * 
     * @param scheme the URI scheme (e.g., "file", "http", "https")
     * 
     * @return the DocumentLoader for the scheme, or null if none found
     */
    private DocumentLoader findDocumentLoader(String scheme)
        {
        Set<Bean<?>> beans = beanManager.getBeans(DocumentLoader.class);
        Bean<?>      bean  = beans.stream()
                                   .filter(b -> b.getName() != null && b.getName().equals(scheme))
                                   .findFirst()
                                   .orElse(null);

        if (bean == null)
            {
            Logger.warn("Unable to find document loader for URI scheme '%s'".formatted(scheme));
            return null;
            }

        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        return (DocumentLoader) beanManager.getReference(bean, DocumentLoader.class, ctx);
        }
    }
