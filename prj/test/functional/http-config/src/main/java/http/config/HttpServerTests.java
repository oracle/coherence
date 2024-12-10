/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http.config;

import com.tangosol.coherence.http.HttpServer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class HttpServerTests
    {
    @Test
    public void shouldDiscoverHttpServer()
        {
        HttpServer server = HttpServer.create();
        assertThat(server, is(notNullValue()));
        assertThat(server, is(instanceOf(HttpServerStub.class)));
        }
    }
