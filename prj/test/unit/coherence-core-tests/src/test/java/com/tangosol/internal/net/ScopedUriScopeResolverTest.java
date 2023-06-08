/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScopedUriScopeResolverTest
    {
    @Test
    public void shouldResolveScope()
        {
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sURI     = ScopedUriScopeResolver.encodeScope("coherence-cache-config.xml", "foo");
        String                 sScope   = resolver.resolveScopeName(sURI, null, "test");
        assertThat(sScope, is("foo"));
        }

    @Test
    public void shouldResolveScopeWithSpaces()
        {
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sURI     = ScopedUriScopeResolver.encodeScope("coherence-cache-config.xml", "foo bar");
        String                 sScope   = resolver.resolveScopeName(sURI, null, "test");
        assertThat(sScope, is("foo bar"));
        }

    @Test
    public void shouldResolveConfig()
        {
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sURI     = ScopedUriScopeResolver.encodeScope("coherence-cache-config.xml", "foo bar");
        String                 sConfig  = resolver.resolveURI(sURI);
        assertThat(sConfig, is("coherence-cache-config.xml"));
        }

    @Test
    public void shouldResolveConfigWithSpaces()
        {
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sURI     = ScopedUriScopeResolver.encodeScope("coherence cache config.xml", "foo bar");
        String                 sConfig  = resolver.resolveURI(sURI);
        assertThat(sConfig, is("coherence cache config.xml"));
        }
    }
