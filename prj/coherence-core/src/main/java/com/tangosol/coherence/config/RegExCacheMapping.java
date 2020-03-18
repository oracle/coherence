/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import java.util.regex.Pattern;

/**
 * An extension of {@link CacheMapping} that can use a regular expression to
 * match with a given cache name.
 *
 * @author jk 2015.05.29
 * @since Coherence 14.1.1
 */
public class RegExCacheMapping
        extends CacheMapping
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link RegExCacheMapping} for caches that will use raw types by default.
     *
     * @param sCacheNamePattern   the RegEx pattern that maps cache names to caching schemes
     * @param sCachingSchemeName  the name of the caching scheme to which caches matching this
     *                            {@link RegExCacheMapping} will be associated
     */
    public RegExCacheMapping(String sCacheNamePattern, String sCachingSchemeName)
        {
        super(sCacheNamePattern, sCachingSchemeName);

        f_pattern = Pattern.compile(sCacheNamePattern);
        }

    // ----- CacheMapping methods -------------------------------------------

    @Override
    public boolean isForName(String sName)
        {
        return f_pattern.matcher(sName).matches();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The RegEx {@link Pattern} to use to match cache names
     */
    private final Pattern f_pattern;
    }
