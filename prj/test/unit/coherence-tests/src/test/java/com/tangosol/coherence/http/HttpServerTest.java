/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.oracle.coherence.testing.http.HttpServerStub;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class HttpServerTest
    {
    @Test
    public void shouldCreateDefaultHttpServer()
        {
        HttpServer server = HttpServer.create();
        assertThat(server, is(notNullValue()));
        assertThat(server, is(instanceOf(DefaultHttpServer.class)));
        }
    }
