/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.cdi.Name;
import com.tangosol.net.NamedMap;
import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

@SuppressWarnings("CdiInjectionPointsInspection")
@HelidonTest
@AddBean(TestSecurityContextFilter.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledOnOs(OS.WINDOWS)
class StoreIT
    {

    @Inject
    WebTarget target;

    private final String store = "store-it";

    private String docUri;

    @Inject
    Kb kb;

    @BeforeAll
    static void beforeAll()
        {
        System.setProperty("coherence.cacheconfig", "coherence-rag-cache-config.xml");
        System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file");
        System.setProperty(RagSecurity.PROP_IMPORT_FILE_ALLOWED_ROOTS, Path.of("src/test/resources").toAbsolutePath().toString());
        }

    @Test
    @Order(1)
    void shouldRejectUnauthenticatedStoreConfig()
        {
        String cfg = "{\"embeddingModel\":\"-/all-MiniLM-L6-v2\",\"normalizeEmbeddings\":true,\"chunkSize\":384,\"chunkOverlap\":64}";
        try (Response putCfg = target.path("api/kb/config/" + store)
                .request()
                .put(Entity.entity(cfg, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(putCfg.getStatus(), is(401));
            }
        }

    @Test
    @Order(2)
    void shouldRejectNonAdminStoreConfig()
        {
        String cfg = "{\"embeddingModel\":\"-/all-MiniLM-L6-v2\",\"normalizeEmbeddings\":true,\"chunkSize\":384,\"chunkOverlap\":64}";
        try (Response putCfg = target.path("api/kb/config/" + store)
                .request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader")
                .put(Entity.entity(cfg, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(putCfg.getStatus(), is(403));
            }
        }

    @Test
    @Order(3)
    void shouldRejectStoreConfigWithUnallowlistedModelDownloadInProd()
        {
        System.setProperty("coherence.mode", "prod");
        String cfg = "{\"embeddingModel\":\"sentence-transformers/all-MiniLM-L6-v2\",\"normalizeEmbeddings\":true,\"chunkSize\":384,\"chunkOverlap\":64}";
        try
            {
            try (Response putCfg = admin(target.path("api/kb/config/" + store))
                    .put(Entity.entity(cfg, MediaType.APPLICATION_JSON_TYPE)))
                {
                assertThat(putCfg.getStatus(), is(400));
                }
            }
        finally
            {
            System.clearProperty("coherence.mode");
            }
        }

    @Test
    @Order(4)
    void configureStore()
        {
        String cfg = "{\"embeddingModel\":\"-/all-MiniLM-L6-v2\",\"normalizeEmbeddings\":true,\"chunkSize\":384,\"chunkOverlap\":64}";
        try (Response putCfg = admin(target.path("api/kb/config/" + store))
                .put(Entity.entity(cfg, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(putCfg.getStatus(), is(204));
            }
        }

    @Test
    @Order(5)
    void shouldRejectUnauthenticatedImport()
        {
        String importBody = "[\"file:///tmp/rejected.txt\"]";
        try (Response imp = target.path("api/kb/" + store + "/docs")
                .request()
                .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(imp.getStatus(), is(401));
            }
        }

    @Test
    @Order(6)
    void shouldRejectNonAdminImport()
        {
        String importBody = "[\"file:///tmp/rejected.txt\"]";
        try (Response imp = target.path("api/kb/" + store + "/docs")
                .request()
                .header(TestSecurityContextFilter.HEADER_USER, "reader")
                .header(TestSecurityContextFilter.HEADER_ROLES, "reader")
                .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(imp.getStatus(), is(403));
            }
        }

    @Test
    @Order(7)
    void shouldRejectFileImportOutsideAllowedRoot()
        {
        String importBody = "[\"file:///tmp/rejected.txt\"]";
        try (Response imp = admin(target.path("api/kb/" + store + "/docs"))
                .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(imp.getStatus(), is(400));
            }
        }

    @Test
    @Order(8)
    void shouldRejectHttpImportResolvingToPrivateAddress() throws Exception
        {
        System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file,http");
        System.setProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS, "docs.example.com");
        RagSecurity.setAddressResolverForTesting(host -> new InetAddress[]{InetAddress.getByName("10.0.0.5")});
        try
            {
            String importBody = "[\"http://docs.example.com/document.txt\"]";
            try (Response imp = admin(target.path("api/kb/" + store + "/docs"))
                    .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
                {
                assertThat(imp.getStatus(), is(400));
                }
            }
        finally
            {
            System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file");
            System.clearProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS);
            RagSecurity.setAddressResolverForTesting(null);
            }
        }

    @Test
    @Order(9)
    void shouldRejectCloudImportWithoutLocationAllowlist()
        {
        System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file,s3");
        try
            {
            String importBody = "[\"s3://blocked/document.txt\"]";
            try (Response imp = admin(target.path("api/kb/" + store + "/docs"))
                    .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
                {
                assertThat(imp.getStatus(), is(400));
                }
            }
        finally
            {
            System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file");
            }
        }

    @Test
    @Order(10)
    void shouldRejectMixedImportListAtomically()
        {
        File pdf = new File("src/test/resources/database-release-notes.pdf");
        String importBody = "[\"" + pdf.toURI() + "\",\"file:///tmp/rejected.txt\"]";
        try (Response imp = admin(target.path("api/kb/" + store + "/docs"))
                .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(imp.getStatus(), is(400));
            }
        }

    @Test
    @Order(11)
    void importPdfDocument() throws Exception
        {
        File pdf = new File("src/test/resources/database-release-notes.pdf");
        assertThat("Test PDF must exist", pdf.exists(), is(true));
        docUri = pdf.toURI().toString();

        String importBody = "[\"" + docUri + "\"]";
        try (Response imp = admin(target.path("api/kb/" + store + "/docs"))
                .post(Entity.entity(importBody, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(imp.getStatus(), is(204));
            }

        Eventually.assertDeferred(() -> isDocIngested(docUri, 100), is(true), Timeout.after(5, TimeUnit.MINUTES));
        }

    @Test
    @Order(12)
    void getStoreConfig()
        {
        Response getCfg = target.path("api/kb/" + store + "/config")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(getCfg.getStatus(), is(200));
        assertThat(getCfg.readEntity(String.class), containsString("all-MiniLM-L6-v2"));
        }

    @Test
    @Order(13)
    void getDocumentsListAndSpecific()
        {
        // list doc ids
        Response list = target.path("api/kb/" + store + "/docs").request().get();
        assertThat(list.getStatus(), is(200));
        String ids = list.readEntity(String.class);
        assertThat(ids, containsString(docUri));

        // get specific
        Response get = target.path("api/kb/" + store + "/docs")
                .queryParam("docId", docUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(get.getStatus(), is(200));
        assertThat(get.readEntity(String.class), containsString("text"));
        }

    @Test
    @Order(14)
    void getDocumentChunks()
        {
        Response chunks = target.path("api/kb/" + store + "/chunks")
                .queryParam("docId", docUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(chunks.getStatus(), is(200));
        assertThat(chunks.readEntity(String.class), containsString("chunks"));
        }

    @Test
    @Order(15)
    void addPreprocessedChunks()
        {
        String payload = "[ {\"id\":\"doc-extra\",\"chunks\":[{\"text\":\"Extra chunk text\",\"metadata\":{}}]} ]";
        try (Response add = target.path("api/kb/" + store + "/chunks")
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(add.getStatus(), is(204));
            }
        }

    @Test
    @Order(16)
    void createAndRemoveIndex()
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
    @Order(17)
    void vectorOnlySearch()
        {
        String body = "{\"query\":\"release notes\",\"maxResults\":5}";
        try (Response res = target.path("api/kb/" + store + "/search")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(res.getStatus(), is(200));
            assertThat(res.readEntity(String.class), containsString("results"));
            }
        }

    @Test
    @Order(18)
    void hybridSearch()
        {
        String body = "{\"query\":\"release notes\",\"maxResults\":5,\"minScore\":0.3,\"fullTextWeight\":0.5}";
        try (Response res = target.path("api/kb/" + store + "/search")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE)))
            {
            assertThat(res.getStatus(), is(200));
            }
        }

    // ---- helpers ---------------------------------------------------------

    private boolean isDocIngested(String docId, int cExpectedChunks)
        {
        var mapChunks = kb.store(store).getChunks(docId);
        return mapChunks != null && mapChunks.size() >= cExpectedChunks;
        }

    private static jakarta.ws.rs.client.Invocation.Builder admin(WebTarget target)
        {
        return target.request()
                .header(TestSecurityContextFilter.HEADER_USER, "admin-user")
                .header(TestSecurityContextFilter.HEADER_ROLES, "admin");
        }
    }
