/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("CdiInjectionPointsInspection")
@HelidonTest
class ModelsIT
    {

    @Inject
    WebTarget target;

    @BeforeAll
    static void setUp()
        {
        System.setProperty("coherence.cacheconfig", "coherence-rag-cache-config.xml");
        }

    @Test
    void shouldListAndCRUDModelConfig()
        {
        String path = "api/models/chat/OpenAI/gpt-4o-mini";

        // ensure clean state
        //noinspection resource
        target.path(path).request().delete();

        // list (ok)
        Response list0 = target.path("api/models").request().get();
        assertThat(list0.getStatus(), is(200));

        // upsert
        try (Response put = target.path(path)
                .request()
                .put(Entity.entity("{\"temperature\":0.5,\"maxTokens\":256}", MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(put.getStatus(), is(204));
            }

        // get
        Response get = target.path(path).request(MediaType.APPLICATION_JSON_TYPE).get();
        assertThat(get.getStatus(), is(200));
        String json = get.readEntity(String.class);
        assertThat(json, allOf(containsString("temperature"), containsString("0.5")));

        // list shows key
        Response list = target.path("api/models").request().get();
        assertThat(list.getStatus(), is(200));
        String listJson = list.readEntity(String.class);
        assertThat(listJson, containsString("chat:OpenAI/gpt-4o-mini"));

        // delete
        try (Response del = target.path(path).request().delete())
            {
            assertThat(del.getStatus(), is(204));
            }

        // get returns 404
        Response missing = target.path(path).request().get();
        assertThat(missing.getStatus(), is(404));
        }
    }


