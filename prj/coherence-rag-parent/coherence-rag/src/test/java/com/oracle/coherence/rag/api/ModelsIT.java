/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import io.helidon.microprofile.testing.junit5.AddBean;
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
@AddBean(TestSecurityContextFilter.class)
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
        try (Response ignored = admin(target.path(path)).delete())
            {
            }

        // list (ok)
        Response list0 = authenticated(target.path("api/models")).get();
        assertThat(list0.getStatus(), is(200));

        // upsert
        try (Response put = admin(target.path(path))
                .put(Entity.entity("{\"temperature\":0.5,\"maxTokens\":256}", MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(put.getStatus(), is(204));
            }

        // get
        Response get = authenticated(target.path(path), MediaType.APPLICATION_JSON_TYPE).get();
        assertThat(get.getStatus(), is(200));
        String json = get.readEntity(String.class);
        assertThat(json, allOf(containsString("temperature"), containsString("0.5")));

        // list shows key
        Response list = authenticated(target.path("api/models")).get();
        assertThat(list.getStatus(), is(200));
        String listJson = list.readEntity(String.class);
        assertThat(listJson, containsString("chat:OpenAI/gpt-4o-mini"));

        // delete
        try (Response del = admin(target.path(path)).delete())
            {
            assertThat(del.getStatus(), is(204));
            }

        // get returns 404
        Response missing = authenticated(target.path(path)).get();
        assertThat(missing.getStatus(), is(404));
        }

    @Test
    void shouldRejectUnauthenticatedModelReads()
        {
        try (Response list = target.path("api/models").request().get())
            {
            assertThat(list.getStatus(), is(401));
            }

        try (Response get = target.path("api/models/chat/OpenAI/gpt-4o-mini").request().get())
            {
            assertThat(get.getStatus(), is(401));
            }
        }

    @Test
    void shouldRejectBasicModelReads()
        {
        try (Response list = target.path("api/models").request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader")
                .header(TestSecurityContextFilter.HEADER_SCHEME, jakarta.ws.rs.core.SecurityContext.BASIC_AUTH)
                .get())
            {
            assertThat(list.getStatus(), is(401));
            }
        }

    @Test
    void shouldAllowAuthenticatedModelReads()
        {
        try (Response list = authenticated(target.path("api/models")).get())
            {
            assertThat(list.getStatus(), is(200));
            }
        }

    @Test
    void shouldRejectUnauthenticatedModelMutation()
        {
        String path = "api/models/chat/OpenAI/gpt-4o-mini";

        try (Response put = target.path(path).request()
                .put(Entity.entity("{\"temperature\":0.5}", MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(put.getStatus(), is(401));
            }

        try (Response del = target.path(path).request().delete())
            {
            assertThat(del.getStatus(), is(401));
            }
        }

    @Test
    void shouldRejectNonAdminModelMutation()
        {
        String path = "api/models/chat/OpenAI/gpt-4o-mini";

        try (Response put = target.path(path).request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader")
                .put(Entity.entity("{\"temperature\":0.5}", MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(put.getStatus(), is(403));
            }
        }

    @Test
    void shouldRejectModelConfigWithSensitiveFields()
        {
        String path = "api/models/chat/OpenAI/gpt-4o-mini";

        try (Response put = admin(target.path(path))
                .put(Entity.entity("{\"baseUrl\":\"https://example.com\"}", MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(put.getStatus(), is(400));
            }
        }

    private static jakarta.ws.rs.client.Invocation.Builder admin(WebTarget target)
        {
        return target.request()
                .header(TestSecurityContextFilter.HEADER_USER, "admin-user")
                .header(TestSecurityContextFilter.HEADER_ROLES, "admin");
        }

    private static jakarta.ws.rs.client.Invocation.Builder authenticated(WebTarget target)
        {
        return target.request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader");
        }

    private static jakarta.ws.rs.client.Invocation.Builder authenticated(WebTarget target, MediaType mediaType)
        {
        return target.request(mediaType)
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader");
        }
    }
