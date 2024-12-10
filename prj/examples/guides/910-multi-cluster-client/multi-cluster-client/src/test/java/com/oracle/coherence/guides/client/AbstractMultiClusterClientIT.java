/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client;


import com.oracle.coherence.guides.client.model.TenantMetaData;
import com.oracle.coherence.guides.client.model.User;
import com.tangosol.io.WriteBuffer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractMultiClusterClientIT {
    /**
     * The http client to use to access the client application REST endpoints.
     */
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Return the port the web-server is listening on.
     *
     * @return the port the web-server is listening on
     */
    protected abstract int getHttpPort();

    /**
     * Configure the tenants using the admin endpoints.
     * This will create the two tenants, "marvel" and "star-wars".
     *
     * @throws Exception if tenant creation fails
     */
    protected static void configureTenants(int httpPort, int marvelPort, int starWarsPort) throws Exception {
        URI createUri = URI.create("http://127.0.0.1:" + httpPort + "/tenants");

        TenantMetaData marvel = new TenantMetaData("marvel", "extend", "127.0.0.1", marvelPort, "java");
        HttpRequest createMarvel = HttpRequest.newBuilder(createUri)
                                              .POST(getBodyPublisher(marvel))
                                              .build();

        HttpResponse<Void> response = httpClient.send(createMarvel, HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode(), is(200));

        TenantMetaData starWars = new TenantMetaData("star-wars", "grpc", "127.0.0.1", starWarsPort, "java");
        HttpRequest createStarWars = HttpRequest.newBuilder(createUri)
                                                .POST(getBodyPublisher(starWars))
                                                .build();

        response = httpClient.send(createStarWars, HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode(), is(200));
    }

    @Test
    public void shouldReturnNotFoundWithNoTenantHeader() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + getHttpPort() + "/users"))
                                         .GET()
                                         .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(404));
    }

    @Test
    public void shouldReturnNotFoundWithUnknownTenantHeader() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + getHttpPort() + "/users"))
                                         .header(UserController.TENANT_HEADER, "foo")
                                         .GET()
                                         .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(404));
    }

    @Test
    public void shouldUseExtendTenant() throws Exception {
        int  httpPort  = getHttpPort();
        User ironMan   = new User("", "Iron", "Man", "iron.man@marvel.com");
        URI  createUri = URI.create("http://127.0.0.1:" + httpPort + "/users");
        HttpRequest create = HttpRequest.newBuilder(createUri)
                                        .header(UserController.TENANT_HEADER, "marvel")
                                        .POST(getBodyPublisher(ironMan))
                                        .build();

        HttpResponse<String> response = httpClient.send(create, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        User createdUser = Application.SERIALIZER.deserialize(response.body(), User.class);
        assertThat(createdUser, is(notNullValue()));
        assertThat(createdUser.getId(), is(notNullValue()));
        assertThat(createdUser.getId().isBlank(), is(false));
        assertThat(createdUser.getFirstName(), is(ironMan.getFirstName()));
        assertThat(createdUser.getLastName(), is(ironMan.getLastName()));
        assertThat(createdUser.getEmail(), is(ironMan.getEmail()));

        URI getUri = URI.create("http://127.0.0.1:" + httpPort + "/users/" + createdUser.getId());
        HttpRequest get = HttpRequest.newBuilder(getUri)
                                     .header(UserController.TENANT_HEADER, "marvel")
                                     .GET()
                                     .build();

        response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        User user = Application.SERIALIZER.deserialize(response.body(), User.class);
        assertThat(user, is(notNullValue()));
        assertThat(user.getFirstName(), is(ironMan.getFirstName()));
        assertThat(user.getLastName(), is(ironMan.getLastName()));
        assertThat(user.getEmail(), is(ironMan.getEmail()));

        User ironMan2  = new User(null, null, null, "iron.man2@marvel.com");
        URI  updateUri = URI.create(
                "http://127.0.0.1:" + httpPort + "/users/" + createdUser.getId() + "?email=iron.man2@marvel.com");
        HttpRequest update = HttpRequest.newBuilder(updateUri)
                                        .header(UserController.TENANT_HEADER, "marvel")
                                        .PUT(getBodyPublisher(ironMan2))
                                        .build();

        response = httpClient.send(update, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        user = Application.SERIALIZER.deserialize(response.body(), User.class);
        assertThat(user, is(notNullValue()));
        assertThat(user.getFirstName(), is(ironMan.getFirstName()));
        assertThat(user.getLastName(), is(ironMan.getLastName()));
        assertThat(user.getEmail(), is(ironMan2.getEmail()));

        URI deleteUri = URI.create("http://127.0.0.1:" + httpPort + "/users/" + createdUser.getId());
        HttpRequest delete = HttpRequest.newBuilder(deleteUri)
                                        .header(UserController.TENANT_HEADER, "marvel")
                                        .DELETE()
                                        .build();

        response = httpClient.send(delete, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(404));
    }

    @Test
    public void shouldUseGrpcTenant() throws Exception {
        int  httpPort  = getHttpPort();
        User luke      = new User("", "Like", "Skywalker", "luke.skywalker@star-wars.com");
        URI  createUri = URI.create("http://127.0.0.1:" + httpPort + "/users");
        HttpRequest create = HttpRequest.newBuilder(createUri)
                                        .header(UserController.TENANT_HEADER, "star-wars")
                                        .POST(getBodyPublisher(luke))
                                        .build();

        HttpResponse<String> response = httpClient.send(create, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        User createdUser = Application.SERIALIZER.deserialize(response.body(), User.class);
        assertThat(createdUser, is(notNullValue()));
        assertThat(createdUser.getId(), is(notNullValue()));
        assertThat(createdUser.getId().isBlank(), is(false));
        assertThat(createdUser.getFirstName(), is(luke.getFirstName()));
        assertThat(createdUser.getLastName(), is(luke.getLastName()));
        assertThat(createdUser.getEmail(), is(luke.getEmail()));

        URI getUri = URI.create("http://127.0.0.1:" + httpPort + "/users/" + createdUser.getId());
        HttpRequest get = HttpRequest.newBuilder(getUri)
                                     .header(UserController.TENANT_HEADER, "star-wars")
                                     .GET()
                                     .build();

        response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        User user = Application.SERIALIZER.deserialize(response.body(), User.class);
        assertThat(user, is(notNullValue()));
        assertThat(user.getFirstName(), is(luke.getFirstName()));
        assertThat(user.getLastName(), is(luke.getLastName()));
        assertThat(user.getEmail(), is(luke.getEmail()));

        User luke2     = new User(null, null, null, "luke.skywalker2@star-wars.com");
        URI  updateUri = URI.create("http://127.0.0.1:" + httpPort + "/users/" + createdUser.getId());
        HttpRequest update = HttpRequest.newBuilder(updateUri)
                                        .header(UserController.TENANT_HEADER, "star-wars")
                                        .PUT(getBodyPublisher(luke2))
                                        .build();

        response = httpClient.send(update, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        user = Application.SERIALIZER.deserialize(response.body(), User.class);
        assertThat(user, is(notNullValue()));
        assertThat(user.getFirstName(), is(luke.getFirstName()));
        assertThat(user.getLastName(), is(luke.getLastName()));
        assertThat(user.getEmail(), is(luke2.getEmail()));

        URI deleteUri = URI.create("http://127.0.0.1:" + httpPort + "/users/" + createdUser.getId());
        HttpRequest delete = HttpRequest.newBuilder(deleteUri)
                                        .header(UserController.TENANT_HEADER, "star-wars")
                                        .DELETE()
                                        .build();

        response = httpClient.send(delete, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(200));

        response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(404));
    }

    protected static HttpRequest.BodyPublisher getBodyPublisher(Object value) throws IOException {
        WriteBuffer buffer = Application.SERIALIZER.serialize(value);
        return HttpRequest.BodyPublishers.ofByteArray(buffer.toByteArray());
    }
}
