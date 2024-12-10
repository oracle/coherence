/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.ScopeResolver;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * A {@link ScopeResolver} that resolves a scope from the config URI.
 * <p>
 * If the scheme part of the URI is {@code scoped} it is assumed that the
 * host part of the URI is the scope name and the query part of the URI
 * is the actual configuration URI.
 * For example:
 * <ul>
 *     <li>{@code coherence-cache-config.xml} has no scope</li>
 *     <li>{@code http://foo.com/coherence-cache-config.xml} has no scope</li>
 *     <li>{@code scoped://FOO?coherence-cache-config.xml} has a scope of FOO and an
 *     actual configuration URI of coherence-cache-config.xml</li>
 *     <li>{@code scoped://FOO?http://foo.com/coherence-cache-config.xml} has a scope
 *     of FOO and an actual configuration URI of http://foo.com/coherence-cache-config.xml</li>
 * </ul>
 * In this way a configuration URI can be scope encoded and decoded without worrying about
 * corrupting the original URI.
 *
 * @author Jonathan Knight  2020.11.03
 * @since 20.12
 */
public class ScopedUriScopeResolver
        implements ScopeResolver
    {
    /**
     * Create a {@link ScopedUriScopeResolver} that will not override
     * any scope defined in the cache configuration XML.
     */
    public ScopedUriScopeResolver()
        {
        this(true);
        }

    /**
     * Create a {@link ScopedUriScopeResolver} that may override
     * any scope defined in the cache configuration XML.
     *
     * @param fUseScopeInConfig {@code true} to use any scope defined in the cache
     *                          configuration XML or {@code false} to use the scope
     *                          decoded from the URI
     */
    public ScopedUriScopeResolver(boolean fUseScopeInConfig)
        {
        f_fUseScopeInConfig = fUseScopeInConfig;
        }

    @Override
    public String resolveScopeName(String sConfigURI, ClassLoader loader, String sScopeName)
        {
        URI uri = URI.create(sConfigURI);
        if (SCOPED_SCHEME.equals(uri.getScheme()))
            {
            String sScope = uri.getAuthority();
            String sPath  = uri.getPath();
            if (sPath != null && !sPath.isBlank())
                {
                sScope = URLDecoder.decode(sScope, StandardCharsets.UTF_8)
                        + URLDecoder.decode(sPath, StandardCharsets.UTF_8);
                }
            return URLDecoder.decode(sScope, StandardCharsets.UTF_8);
            }
        return sScopeName;
        }

    @Override
    public String resolveURI(String sConfigURI)
        {
        URI uri = URI.create(sConfigURI);
        if (SCOPED_SCHEME.equals(uri.getScheme()))
            {
            String sQuery    = uri.getQuery();
            String sFragment = uri.getFragment();
            if (sFragment != null)
                {
                // the original config URI had a fragment part that we shouldn't lose
                return URLDecoder.decode(sQuery, StandardCharsets.UTF_8)
                        + "#" + URLDecoder.decode(sFragment, StandardCharsets.UTF_8);
                }
            return URLDecoder.decode(sQuery, StandardCharsets.UTF_8);
            }
        return sConfigURI;
        }

    @Override
    public boolean useScopeInConfig()
        {
        return f_fUseScopeInConfig;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Format a configuration URI to include a scope.
     * <p>
     * If the scope parameter is {@code null} or blank the configuration
     * URI will be returned unchanged.
     *
     * @param sConfigURI  the configuration URI to format
     * @param sScope      the scope to add
     *
     * @return the scoped configuration URI
     *
     * @throws IllegalArgumentException if the configuration is {@code null}
     *         or a blank String.
     */    
    public static String encodeScope(String sConfigURI, String sScope)
        {
        if (sConfigURI == null || sConfigURI.trim().isEmpty())
            {
            throw new IllegalArgumentException("Config URI cannot be null or blank");
            }

        if (sScope == null || sScope.trim().isEmpty())
            {
            return sConfigURI;
            }
        return String.format(SCOPED_PATTERN, SCOPED_SCHEME,
                URLEncoder.encode(sScope, StandardCharsets.UTF_8), URLEncoder.encode(sConfigURI));
        }
    
    // ----- constants ------------------------------------------------------

    /**
     * The value used for the scheme of a scoped URI.
     */
    public static final String SCOPED_SCHEME = "scoped";

    /**
     * The pattern used to create a scoped URI.
     * <p>
     * The format is "scoped://scope-name?configURI"
     */
    public static final String SCOPED_PATTERN = "%s://%s?%s";

    // ----- data members ---------------------------------------------------

    /**
     * {@code true} to use any scope defined in the cache configuration XML
     * or {@code false} to use the scope decoded from the URI.
     */
    private final boolean f_fUseScopeInConfig;
    }
