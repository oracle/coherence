/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.internal.net.ScopedUriScopeResolver;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScopedUriScopeResolverTest
    {
    @Test
    public void shouldResolveNoScope()
        {
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sScope   = resolver.resolveScopeName("http://oracle.com?abc#def", null, "bar");

        assertThat(sScope, is("bar"));
        }

    @Test
    public void shouldResolveScope()
        {
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sScope   = resolver.resolveScopeName("scoped://FOO?http://oracle.com?abc#def", null, "bar");

        assertThat(sScope, is("FOO"));
        }

    @Test
    public void shouldEncodeScope()
        {
        String                 sURI     = ScopedUriScopeResolver.encodeScope("http://oracle.com?abc#def", "foo");
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sScope   = resolver.resolveScopeName(sURI, null, "bar");

        assertThat(sScope, is("foo"));
        }

    @Test
    public void shouldResolveURI()
        {
        String                 sURI     = "http://oracle.com?abc#def";
        String                 sEncoded = ScopedUriScopeResolver.encodeScope(sURI, "foo");
        ScopedUriScopeResolver resolver = new ScopedUriScopeResolver();
        String                 sDecoded = resolver.resolveURI(sEncoded);

        assertThat(sDecoded, is(sURI));
        }
    }
