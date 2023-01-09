/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.mp.health.testing;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.mp.health.CoherenceHealthChecks;
import com.tangosol.net.Coherence;

import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import java.io.StringReader;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CoherenceHealthIT
    {
    @BeforeAll
    static void setupBaseTest()
        {
        System.setProperty("coherence.ttl",       "0");
        System.setProperty("coherence.wka",       "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.cluster",   "CoherenceHealthIT");
        System.setProperty("coherence.log.level", "8");

        s_server  = Server.create().start();
        }


    @AfterAll
    static void cleanupBaseTest()
        {
        if (s_server != null)
            {
            s_server.stop();
            }
        }

    @Test
    public void shouldBeLive() throws Exception
        {
        Eventually.assertDeferred(this::coherenceLive, is(true));
        URI uri = URI.create("http://127.0.0.1:7001/health/live");
        assertResponse(uri, CoherenceHealthChecks.HEALTH_CHECK_LIVENESS);
        }

    @Test
    public void shouldBeReady() throws Exception
        {
        Eventually.assertDeferred(this::coherenceReady, is(true));
        URI uri = URI.create("http://127.0.0.1:7001/health/ready");
        assertResponse(uri, CoherenceHealthChecks.HEALTH_CHECK_READINESS);
        }

    void assertResponse(URI uri, String sName) throws Exception
        {
        HttpClient           client   = HttpClient.newBuilder().build();
        HttpRequest          request  = HttpRequest.newBuilder().GET().uri(uri).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        JsonReader reader     = Json.createReader(new StringReader(response.body()));
        JsonObject jsonObject = reader.readObject();
        assertThat(jsonObject.getString("status"), is("UP"));

        JsonArray checks = jsonObject.getJsonArray("checks");
        assertThat(checks, is(notNullValue()));
        assertThat(checks.size(), is(1));

        JsonValue jsonValue = checks.get(0);
        assertThat(jsonValue, is(notNullValue()));

        JsonObject check = jsonValue.asJsonObject();
        assertThat(check.getString("name"), is(sName));
        assertThat(check.getString("status"), is("UP"));
        }

    // ----- helper methods -------------------------------------------------

    protected boolean coherenceStarted()
        {
        Coherence coherence = Coherence.getInstance();
        return coherence.getManagement().allHealthChecksStarted();
        }

    protected boolean coherenceLive()
        {
        Coherence coherence = Coherence.getInstance();
        return coherence.getManagement().allHealthChecksStarted();
        }

    protected boolean coherenceReady()
        {
        Coherence coherence = Coherence.getInstance();
        return coherence.getManagement().allHealthChecksStarted();
        }

    // ----- data members ---------------------------------------------------

    private static Server s_server;
    }
