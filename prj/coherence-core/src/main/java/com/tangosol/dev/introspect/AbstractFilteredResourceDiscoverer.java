/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.util.Base;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * AbstractFilteredResourceDiscoverer implements {@link Filter} methods
 * leaving a single method {@link #getPermittedProtocols()} to be
 * implemented. The {@link Filter#evaluate(Object)} implementation will
 * use the permitted protocols to provide an answer on this
 * {@link ResourceDiscoverer}'s willingness to work with a resource.
 *
 * @author hr  2011.10.18
 *
 * @since Coherence 12.1.2
 *
 * @param <T> the root element to operate against
 */
public abstract class AbstractFilteredResourceDiscoverer<T>
        implements ResourceDiscoverer<T>, Filter, ClassLoaderAware
    {

    // ----- Filter interface -----------------------------------------------

    /**
     * The expected context in which this method is called is either with
     * a {@link URL} or a string representing the protocol
     * ({@link URL#getProtocol()}). This method will validate that the
     * protocol is one of the acceptable protocols as defined by
     * {@link #getPermittedProtocols()}.
     *
     * @param o  either a URL or the string protocol of the URL
     *
     * @return whether this {@link ResourceDiscoverer} can work with the
     *         specified resource
     */
    public boolean evaluate(Object o)
        {
        String sValue = o instanceof String ? (String) o :
            o instanceof URL ? ((URL) o).getProtocol() : null;

        return sValue != null && getPermittedProtocols().contains(sValue);
        }

    // ----- ResourceDiscoverer interface -----------------------------------

    /**
     * This implementation delegates to
     * {@link #discoverResource(String, Object)} converting the {@link Set}
     * to an {@link Enumeration}.
     *
     * @param sExpression  the expression that each leaf-resource must match
     * @param root         the root element to start traversing from
     *
     * @return a number of resources that complied with the
     *         {@code sExpression} and belong to {@code root}
     */
    public Enumeration<URL> discover(String sExpression, T root)
        {
        return toURLs(discoverResource(sExpression, root));
        }

    /**
     * This implementation simply iterates the {@code setRoot} delegating to
     * {@link #discoverResource(String, Object)} converting the combined
     * {@link Set} to an {@link Enumeration}.
     *
     * @param sExpression  the expression that each leaf resource must match
     * @param setRoot      the set of root elements to start traversing from
     *
     * @return a number of resources that complied with the
     *         {@code sExpression} and belong to {@code setRoot}
     */
    public Enumeration<URL> discover(String sExpression, Set<T> setRoot)
        {
        Collection<URI> colResults;
        if (setRoot == null || setRoot.isEmpty())
            {
            colResults = discoverResource(sExpression, null);
            }
        else
            {
            colResults = new HashSet<>();
            for (T root : setRoot)
                {
                Collection<URI> colResult = discoverResource(sExpression, root);
                if (colResult != null && colResult.size() > 0)
                    {
                    colResults.addAll(colResult);
                    }
                }
            }
        return toURLs(colResults);
        }

    // ----- ClassLoaderAware methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getContextClassLoader()
        {
        return m_loader == null ? m_loader = Base.getContextClassLoader() : m_loader;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        }

    // ----- AbstractFilteredResourceDiscoverer methods ---------------------

    /**
     * Returns a set of protocols this {@link ResourceDiscoverer} is
     * willing to work with, i.e. "file", "jar", "zip".
     *
     * @return a set of protocols this ResourceDiscoverer is willing to work
     *         with
     */
    protected abstract Set<String> getPermittedProtocols();

    /**
     * Starting from the {@code root} traverse the target evaluating all leaf
     * resources against {@code sExpression} returning complying resources.
     *
     * @param sExpression  the expression that each leaf resource must match
     * @param root         the root element to start traversing from
     *
     * @return a number of resources that complied with the
     *         {@code sExpression} and belong to {@code root}
     */
    public abstract Collection<URI> discoverResource(String sExpression, T root);

    // ----- constant helpers -----------------------------------------------

    /**
     * Return an Enumeration of URLs based on the provided Collection of URIs.
     *
     * @param colURI  a collection of URIs
     *
     * @return an Enumeration of URLs
     */
    protected static Enumeration<URL> toURLs(Collection<URI> colURI)
        {
        return colURI == null || colURI.isEmpty()
                ? Collections.emptyEnumeration()
                : Collections.enumeration(ConverterCollections.getCollection(
                    colURI,
                    AbstractFilteredResourceDiscoverer::toURL,
                    AbstractFilteredResourceDiscoverer::toURI));
        }

    /**
     * Return a URI based on the provided URL.
     *
     * @param url  the URL to convert to a URI
     *
     * @return a URI based on the provided URL
     */
    protected static URI toURI(URL url)
        {
        try
            {
            return url.toURI();
            }
        catch (URISyntaxException e) {}

        return null;
        }

    /**
     * Return a URL based on the provided URI.
     *
     * @param uri  the URI to convert to a URL
     *
     * @return a URL based on the provided URI
     */
    protected static URL toURL(URI uri)
        {
        try
            {
            return uri.toURL();
            }
        catch (MalformedURLException e) {}

        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Class loader to be used for loading classes.
     */
    protected ClassLoader m_loader;
    }