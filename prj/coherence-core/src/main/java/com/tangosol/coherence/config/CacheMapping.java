/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.Scheme;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;

import com.tangosol.util.ClassHelper;

/**
 * A {@link CacheMapping} captures configuration information for a pattern-match-based mapping from a proposed
 * {@link NamedCache} name to a caching scheme.
 * <p>
 * In addition to the mapping between a cache name and a caching scheme, each {@link CacheMapping} retains a
 * {@link ParameterResolver} (representing user-provided parameters) to be during the realization of the said cache
 * and scheme.  (This allows individual mappings to be parameterized)
 * <p>
 * Lastly {@link CacheMapping}s also provide a mechanism to associate specific strongly typed resources
 * with each mapping at runtime. This provides a flexible and dynamic mechanism to associate further configuration
 * information with caches.
 * <p>
 * <strong>Pattern Matching Semantics:</strong>
 * The only wildcard permitted for pattern matching with cache names is the "*" and it <strong>may only</strong>
 * be used at the end of a cache name.
 * <p>
 * For example, the following cache name patterns are valid:
 * <code>"*"</code> and <code>something-*</code>, but <code>*-something</code> is invalid.
 *
 * @author bo  2011.06.25
 * @since Coherence 12.1.2
 */
@SuppressWarnings("rawtypes")
public class CacheMapping
        extends TypedResourceMapping<NamedCache>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link CacheMapping} for caches that will use raw types by default.
     *
     * @param sCacheNamePattern             the pattern that maps cache names to caching schemes
     * @param sCachingSchemeName            the name of the caching scheme to which caches matching this
     *                                      {@link CacheMapping} will be associated
     */
    public CacheMapping(String sCacheNamePattern, String sCachingSchemeName)
        {
        super(sCacheNamePattern, sCachingSchemeName);

        m_sKeyClassName = null;
        }

    // ----- ResourceMapping methods --------------------------------------------

    @Override
    public String getConfigElementName()
        {
        return "cache-name";
        }

    @Override
    public void validateScheme(Scheme scheme)
        {
        if (scheme instanceof CachingScheme)
            {
            return;
            }

        String sElement = getConfigElementName();
        String sPattern = getNamePattern();
        String sScheme  = scheme.getSchemeName();
        String sMsg     = String.format("Mapping <%s>%s</%s> maps to %s which is not a valid caching scheme", sScheme,
                                        sElement, sPattern, sElement);

        throw new IllegalStateException(sMsg);
        }

    // ----- CacheMapping methods -------------------------------------------
    /**
     * Set true if this cache mapping is for federated caches.
     * This has no effect for non-federated caches.
     *
     * @param  fIsFederated  true if this cache is to be federated
     * @return  this CacheMapping object
     */
    @Injectable("federated")
    public CacheMapping setFederated(boolean fIsFederated)
        {
        m_fFederated = fIsFederated;
        return this;
        }

    /**
     * Check if this CacheMapping is federated.
     *
     * @return  true if this CacheMapping is federated
     */
    public boolean isFederated()
        {
        return m_fFederated;
        }

    @Override
    public CacheMapping setInternal(boolean fIsInternal)
        {
        super.setInternal(fIsInternal);
        return this;
        }

    /**
     * Obtains the pattern used to match cache names to this {@link CacheMapping}.
     *
     * @return the pattern
     *
     * @deprecated As of Coherence 14.1.1, use {@link #getNamePattern()}.
     */
    public String getCacheNamePattern()
        {
        return getNamePattern();
        }

    /**
     * Obtains the name of the caching scheme to be used for {@link NamedCache}s that match this {@link CacheMapping}.
     *
     * @return the name of the associated caching scheme
     *
     * @deprecated As of Coherence 14.1.1, use {@link #getSchemeName()}.
     */
    public String getCachingSchemeName()
        {
        return getSchemeName();
        }

    /**
     * Obtains the name of the key class for {@link NamedCache}s using this {@link CacheMapping}.
     *
     * @return the name of the key class or <code>null</code> if rawtypes are being used
     */
    public String getKeyClassName()
        {
        return m_sKeyClassName;
        }

    /**
     * Sets the name of the key class for {@link NamedCache}s using this {@link CacheMapping}.
     *
     * @param sKeyClassName the name of the key class or <code>null</code> if rawtypes are being used
     */
    @Injectable("key-type")
    public void setKeyClassName(String sKeyClassName)
        {
        m_sKeyClassName = ClassHelper.getFullyQualifiedClassNameOf(sKeyClassName);
        }

    /**
     * Sets the name of the value class for {@link NamedCache}s using this {@link CacheMapping}.
     *
     * @param sValueClassName the name of the value class or <code>null</code> if rawtypes are being used
     */
    @Override
    @Injectable("value-type")
    public void setValueClassName(String sValueClassName)
        {
        super.setValueClassName(sValueClassName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The flag to indicate if this {@link CacheMapping} is federated.
     */
    public boolean m_fFederated = true;

    /**
     * The name of the key class or <code>null</code> if rawtypes are being used (the default).
     */
    private String m_sKeyClassName;
    }
