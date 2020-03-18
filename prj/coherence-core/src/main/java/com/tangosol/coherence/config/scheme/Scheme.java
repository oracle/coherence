/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

/**
 * A {@link Scheme} defines the configuration information and necessary 
 * builders for the construction of various well-known and identifiable 
 * structures used by Coherence at runtime.  
 * <p>
 * Coherence {@link Scheme}s are best thought of as "templates" or "plans" that 
 * are used for constructing runtime infrastructure.  Common examples include:
 * services, caches, backing maps and cache stores.
 * <p>
 * Some Coherence {@link Scheme}s, such as backing-map-schemes, are only used to 
 * create maps, whereas as others, such as distributed-scheme are used to create 
 * services, caches, and backing maps (as required by an inner scheme).  In 
 * addition, Coherence also provides {@link Scheme}s that are unrelated to 
 * caches and maps, such as invocation-scheme and cache-store scheme.
 *
 * @author pfm  2011.12.30
 * @since Coherence 12.1.2
 */
public interface Scheme
    {
    /**
     * Obtains the name of the {@link Scheme}.
     *
     * @return the scheme name
     */
    public String getSchemeName();
    
    /**
     * Determines if the {@link Scheme} is a defined and thus useful name.
     * 
     * @return if the {@link Scheme} has a name.
     */
    public boolean isAnonymous();
    }
