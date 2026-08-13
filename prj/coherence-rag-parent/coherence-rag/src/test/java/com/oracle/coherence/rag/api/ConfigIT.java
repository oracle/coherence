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
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("CdiInjectionPointsInspection")
@HelidonTest
@AddBean(TestSecurityContextFilter.class)
class ConfigIT
    {

    @Inject
    WebTarget target;

    @Test
    void shouldRejectUnauthenticatedConfigGet()
        {
        try (Response get = target.path("api/_config/model.embedding")
                .request()
                .get())
            {
            assertThat(get.getStatus(), is(401));
            }
        }

    @Test
    void shouldRejectUnauthenticatedConfigWrite()
        {
        try (Response put = target.path("api/_config/model.embedding")
                .request()
                .put(Entity.text("-/all-MiniLM-L6-v2")))
            {
            assertThat(put.getStatus(), is(401));
            }
        }

    @Test
    void shouldRejectNonAdminConfigWrite()
        {
        try (Response put = target.path("api/_config/model.embedding")
                .request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader")
                .put(Entity.text("-/all-MiniLM-L6-v2")))
            {
            assertThat(put.getStatus(), is(403));
            }
        }

    @Test
    void shouldRejectSensitiveConfigWrite()
        {
        try (Response put = admin(target.path("api/_config/openai.api.key"))
                .put(Entity.text("secret")))
            {
            assertThat(put.getStatus(), is(400));
            }
        }

    @Test
    void shouldRejectPolicyNamespaceConfigWrite()
        {
        try (Response put = admin(target.path("api/_config/" + RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES))
                .put(Entity.text("file")))
            {
            assertThat(put.getStatus(), is(400));
            }
        }

    @Test
    void shouldRejectUnallowlistedModelDownloadConfigWriteInProd()
        {
        System.setProperty("coherence.mode", "prod");
        try
            {
            try (Response put = admin(target.path("api/_config/model.embedding"))
                    .put(Entity.text("sentence-transformers/all-MiniLM-L6-v2")))
                {
                assertThat(put.getStatus(), is(400));
                }
            }
        finally
            {
            System.clearProperty("coherence.mode");
            }
        }

    @Test
    void shouldReturn404ForSensitiveReadback()
        {
        try (Response get = target.path("api/_config/openai.api.key")
                .request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader")
                .get())
            {
            assertThat(get.getStatus(), is(404));
            }
        }

    @Test
    void shouldAllowAdminWriteToAllowedProperty()
        {
        try (Response put = admin(target.path("api/_config/model.embedding"))
                .put(Entity.text("-/all-MiniLM-L6-v2")))
            {
            assertThat(put.getStatus(), is(200));
            }

        try (Response get = target.path("api/_config/model.embedding")
                .request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader")
                .get())
            {
            assertThat(get.getStatus(), is(200));
            assertThat(get.readEntity(String.class), is("-/all-MiniLM-L6-v2"));
            }
        }

    private static jakarta.ws.rs.client.Invocation.Builder admin(WebTarget target)
        {
        return target.request()
                .header(TestSecurityContextFilter.HEADER_USER, "admin-user")
                .header(TestSecurityContextFilter.HEADER_ROLES, "admin");
        }
    }
