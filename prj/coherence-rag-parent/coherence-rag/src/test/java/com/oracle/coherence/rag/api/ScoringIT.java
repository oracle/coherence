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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@SuppressWarnings("CdiInjectionPointsInspection")
@HelidonTest
class ScoringIT
    {

    @Inject
    WebTarget target;

    @Test
    void shouldScoreAnswersWithDefaultModel()
        {
        String body = "{\"modelName\":null,\"query\":\"What is machine learning?\",\"answers\":[" +
                      "\"Machine learning is a subset of AI that learns from data.\"," +
                      "\"Cats are animals.\"]}";

        try (Response res = target.path("api/score")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE)))
            {

            assertThat(res.getStatus(), is(200));
            String json = res.readEntity(String.class);
            assertThat(json, startsWith("["));
            assertThat(json, containsString("0."));
            }
        }

    @Test
    void shouldScoreAnswersWithSpecificLocalModel()
        {
        String body = "{\"modelName\":\"-/ms-marco-TinyBERT-L-2-v2\",\"query\":\"What is machine learning?\",\"answers\":[" +
                      "\"Machine learning is a subset of AI that learns from data.\"," +
                      "\"Cats are animals.\"]}";

        try (Response res = target.path("api/score")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(res.getStatus(), is(200));

            String json = res.readEntity(String.class);
            assertThat(json, startsWith("["));
            assertThat(json, containsString(","));
            }
        }
    }


