/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import io.helidon.microprofile.testing.junit5.HelidonTest;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@SuppressWarnings({"NewClassNamingConvention", "CdiInjectionPointsInspection"})
@HelidonTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledOnOs(OS.WINDOWS)
class KbIT
    {

    @Inject
    WebTarget target;

    private final String store = "it-store";

    private String docUri;

    @Inject
    Kb kb;

    @BeforeAll
    static void setUp()
        {
        System.setProperty("coherence.cacheconfig", "coherence-rag-cache-config.xml");
        }

    @Test
    @Order(1)
    void setUpStoreAndIngest() throws Exception
        {
        // configure store with local ONNX embedding model
        String cfg = "{\"embeddingModel\":\"-/all-MiniLM-L6-v2\",\"normalizeEmbeddings\":true,\"chunkSize\":512,\"chunkOverlap\":64}";
        try (Response putCfg = target.path("api/kb/config/" + store)
                .request()
                .put(Entity.entity(cfg, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(putCfg.getStatus(), is(204));
            }

        // compute absolute file URI to test PDF
        File pdf = new File("src/test/resources/database-release-notes.pdf");
        assertThat("Test PDF must exist", pdf.exists(), is(true));
        docUri = pdf.toURI().toString();

        // import document via docs endpoint
        String importBody = "[\"" + docUri + "\"]";
        try (Response imp = target.path("api/kb/" + store + "/docs")
                .request()
                .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(imp.getStatus(), is(204));
            }

        // wait until chunking and embedding complete
        Eventually.assertDeferred(() -> isDocIngested(docUri, 70), is(true), Timeout.after(5, TimeUnit.MINUTES));
        }

    @Test
    @Order(2)
    void testStoreList()
        {
        Response list = target.path("api/kb").request().get();
        assertThat(list.getStatus(), is(200));
        assertThat(list.readEntity(String.class), containsString(store));
        }

    @Test
    @Order(3)
    void testGetStoreConfig()
        {
        Response getCfg = target.path("api/kb/config/" + store).request(MediaType.APPLICATION_JSON_TYPE).get();
        assertThat(getCfg.getStatus(), is(200));
        assertThat(getCfg.readEntity(String.class), containsString("all-MiniLM-L6-v2"));
        }

    @Test
    @Order(4)
    void testGetChunks()
        {
        Response chunks = target.path("api/kb/" + store + "/chunks")
                .queryParam("docId", docUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(chunks.getStatus(), is(200));
        assertThat(chunks.readEntity(String.class), containsString("chunks"));
        }

    @Test
    @Order(5)
    void testCreateAndRemoveIndex()
        {
        String hnsw = "{\"type\":\"HNSW\",\"parameters\":{\"m\":16,\"efConstruction\":200,\"efSearch\":64}}";
        try (Response createIndex = target.path("api/kb/" + store + "/index")
                .request()
                .post(Entity.entity(hnsw, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(createIndex.getStatus(), is(202));
            }

        try (Response delIdx = target.path("api/kb/" + store + "/index").request().delete())
            {
            assertThat(delIdx.getStatus(), anyOf(is(202), is(304)));
            }
        }

    @Test
    @Order(6)
    void testVectorOnlySearch()
        {
        String body = "{\"query\":\"release notes\",\"maxResults\":5,\"minScore\":0.0}";
        try (Response res = target.path("api/kb/search")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(String.class), containsString("results"));
            }
        }

    @Test
    @Order(7)
    void testHybridSearch()
        {
        String body = "{\"query\":\"release notes\",\"maxResults\":5,\"minScore\":0.0,\"fullTextWeight\":0.5}";
        try (Response res = target.path("api/kb/search")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(String.class), containsString("results"));
            }
        }

    // ---- helpers ---------------------------------------------------------

    private boolean isDocIngested(String docId, int cExpectedChunks)
        {
        var mapChunks = kb.store(store).getChunks(docId);
        return mapChunks != null && mapChunks.size() >= cExpectedChunks;
        }
    }

