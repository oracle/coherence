/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("CdiInjectionPointsInspection")
@HelidonTest
class ConfigIT
    {

    @Inject
    WebTarget target;

    @Test
    void shouldSetAndGetProperty()
        {
        String property = "it.example.property";

        try (Response put = target.path("api/_config/" + property)
                .request()
                .put(jakarta.ws.rs.client.Entity.text("value-1")))
            {
            assertThat(put.getStatus(), is(200));
            }

        Response get = target.path("api/_config/" + property)
                .request()
                .get();
        assertThat(get.getStatus(), is(200));
        String value = get.readEntity(String.class);
        assertThat(value, is("value-1"));
        }
    }


