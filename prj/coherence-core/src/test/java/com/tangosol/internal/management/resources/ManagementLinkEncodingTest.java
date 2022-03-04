/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test to ensure Management over REST encodes links properly.
 */
public class ManagementLinkEncodingTest
    {
    @Test
    public void shouldEncodeLink()
        {
        URI uriParent = URI.create(URL_ROOT);
        URI uri       = AbstractManagementResource.getSubUri(uriParent, "Foo");

        assertThat(uri, is(URI.create(URL_ROOT + "/Foo")));
        }

    @Test
    public void shouldEncodeLinkWithMultipleSegments()
        {
        URI uriParent = URI.create(URL_ROOT);
        URI uri       = AbstractManagementResource.getSubUri(uriParent, "Foo", "Bar");

        assertThat(uri, is(URI.create(URL_ROOT + "/Foo/Bar")));
        }

    @Test
    public void shouldEncodeLinkWithSpecialCharacters()
        {
        URI uriParent = URI.create(URL_ROOT);
        URI uri       = AbstractManagementResource.getSubUri(uriParent, "Foo#");

        assertThat(uri, is(URI.create(URL_ROOT + "/Foo%23")));
        }

    @Test
    public void shouldEncodeLinkWithSpaces()
        {
        URI uriParent = URI.create(URL_ROOT);
        URI uri       = AbstractManagementResource.getSubUri(uriParent, "Foo Bar 123");

        assertThat(uri, is(URI.create(URL_ROOT + "/Foo%20Bar%20123")));
        }

    @Test
    public void shouldEncodeLinkWithMultipleSegmentsWithSpaces()
        {
        URI uriParent = URI.create(URL_ROOT);
        URI uri       = AbstractManagementResource.getSubUri(uriParent, "Foo Bar", "123 456");

        assertThat(uri, is(URI.create(URL_ROOT + "/Foo%20Bar/123%20456")));
        }

    // ----- data members ---------------------------------------------------

    public static final String URL_ROOT = "http://localhost:30000/management/coherence/cluster/services";
    }
