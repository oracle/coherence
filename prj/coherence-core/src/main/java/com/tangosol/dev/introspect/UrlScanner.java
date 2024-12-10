/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.util.Filter;

import java.net.URL;

import java.util.Enumeration;
import java.util.Set;

/**
 * A UrlScanner is responsible for scanning the provided resource(s) returning
 * the result to the caller. Implementations of this class will use
 * their own mechanisms to scan the resource and derive objects that
 * represent the underlying resource. This interface is relatively
 * unspecific but defines an intent and high-level flow for the implementer
 * to follow. The flow being:
 * <ol>
 *     <li>scan the provided resource</li>
 *     <li>populate some analysis result object</li>
 *     <li>pass the resultant object to the caller for post processing</li>
 * </ol>
 * <p>
 * A scanner should keep minimal state as it may be reused across many
 * resources as provided to {@link #scan(URL)}.
 * <p>
 * A UrlScanner allows consumers to restrict the remit of the UrlScanner.
 * The language used to describe the scope / remit of the scanner is based on
 * {@link Filter} implementations. It is the responsibility of the
 * implementation to determine the value the Filter will be applied against.
 * Implementations may allow filtering against the resource, annotation or
 * both.
 *
 * @author hr  2011.10.17
 *
 * @since Coherence 12.1.2
 *
 * @see ClassAnnotationScanner
 */
public interface UrlScanner<T>
        extends ClassLoaderAware
    {
    /**
     * Given the provided {@link URL} of the resource, scan
     * it's content returning the result.
     *
     * @param urlResource   the resource to scan
     *
     * @return an object representing the result of analyzing the URL
     */
    public T scan(URL urlResource);

    /**
     * Given the provided {@link URL} of the resources, scan their
     * content returning the results.
     *
     * @param enumResources  the resources to scan
     *
     * @return a {@link Set} of objects representing the result of analyzing
     *         the Enumeration of URLs
     */
    public Set<T> scan(Enumeration<URL> enumResources);

    /**
     * Use the provided {@literal filter} to limit the remit of this
     * RestrictiveScanner.
     *
     * @param filter  limits the remit of this scanner
     */
    public void setFilter(Filter filter);
    }