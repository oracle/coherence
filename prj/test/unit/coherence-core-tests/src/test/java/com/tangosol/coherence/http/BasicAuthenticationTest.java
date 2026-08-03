/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link BasicAuthentication}.
 *
 * @author jk  2026.05.14
 * @since 26.07
 */
public class BasicAuthenticationTest
    {
    @Test
    public void shouldParseBasicCredentials()
        {
        assertCredentials("user:password", "user", "password");
        assertCredentials("user:pass:word", "user", "pass:word");
        assertCredentials("user:a:b:c", "user", "a:b:c");
        }

    @Test
    public void shouldReturnNullForUnsupportedScheme()
        {
        assertThat(BasicAuthentication.parse("Bearer abc"), is(nullValue()));
        }

    @Test
    public void shouldRejectMalformedBase64()
        {
        assertInvalid("Basic ###");
        }

    @Test
    public void shouldRejectMissingColon()
        {
        assertInvalid("Basic " + AbstractHttpServer.toBase64("user"));
        }

    @Test
    public void shouldRejectEmptyUsername()
        {
        assertInvalid("Basic " + AbstractHttpServer.toBase64(":password"));
        }

    private void assertCredentials(String sCredentials, String sUsername, String sPassword)
        {
        BasicAuthentication.Credentials credentials =
                BasicAuthentication.parse("Basic " + AbstractHttpServer.toBase64(sCredentials));

        assertThat(credentials.getUsername(), is(sUsername));
        assertThat(credentials.getPassword(), is(sPassword));
        }

    private void assertInvalid(String sHeader)
        {
        try
            {
            BasicAuthentication.parse(sHeader);
            fail("Expected invalid Basic credentials");
            }
        catch (IllegalArgumentException expected)
            {
            // expected
            }
        }
    }
