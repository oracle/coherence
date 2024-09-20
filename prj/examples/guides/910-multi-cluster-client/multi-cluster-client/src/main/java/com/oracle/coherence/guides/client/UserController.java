/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client;


import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.guides.client.model.TenantMetaData;
import com.oracle.coherence.guides.client.model.User;
import com.oracle.coherence.io.json.JsonSerializer;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.Response;
import com.tangosol.io.WriteBuffer;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;


import java.io.IOException;
import java.util.Map;

/**
 * A basic set if REST endpoints to perform CRUD operations on
 * the {@link User} entity.
 */
public class UserController {

    /**
     * The name of the request header holding the tenant identifier.
     */
    public static final String TENANT_HEADER = "tenant";

    /**
     * The {@link NamedMap} holding tenant meta-data.
     */
    private final NamedMap<String, TenantMetaData> tenants;

    /**
     * The JSON serializer used to deserialize request bodies and
     * serialize response bodies.
     */
    private final JsonSerializer serializer;

    /**
     * Create an instance of {@link UserController}.
     *
     * @param tenants     the tenants {@link NamedMap}
     * @param serializer  a {@link JsonSerializer} to use to serialize request and response bodies
     */
    public UserController(NamedMap<String, TenantMetaData> tenants, JsonSerializer serializer) {
        this.tenants = tenants;
        this.serializer = serializer;
    }

    /**
     * Handle a User get request.
     *
     * @param request  the request
     *
     * @return the response
     */
    // # tag::get[]
    public Response get(HttpRequest request) {
        String tenant = request.getHeaderString(TENANT_HEADER);   // <1>
        if (tenant == null || tenant.isBlank()) {
            // <2>
            return Response.status(400).entity(Map.of("Error", "Missing tenant identifier")).build();
        }
        Session session = ensureSession(tenant);  // <3>
        if (session == null) {
            // <4>
            return Response.status(400).entity(Map.of("Error", "Unknown tenant " + tenant)).build();
        }
        // # end::get[]

        String                   id    = request.getFirstPathParameter("user");
        NamedCache<String, User> users = session.getCache("users");
        User                     user  = users.get(id);
        if (user == null) {
            return Response.notFound().entity(Map.of("Error", "Unknown user " + id)).build();
        }

        return Response.ok(serialize(user)).build();
    }

    /**
     * Handle a User create request.
     *
     * @param request  the request
     *
     * @return the response
     */
    public Response create(HttpRequest request) {
        String tenant = request.getHeaderString(TENANT_HEADER);
        if (tenant == null || tenant.isBlank()) {
            return Response.status(400).entity(Map.of("Error", "Missing tenant identifier")).build();
        }
        Session session = ensureSession(tenant);
        if (session == null) {
            return Response.status(400).entity(Map.of("Error", "Unknown tenant " + tenant)).build();
        }

        User user = getUserFromBody(request);
        user.setId(user.getFirstName() + "." + user.getLastName());

        NamedCache<String, User> users = session.getCache("users");

        Logger.info("Creating " + user);
        users.put(user.getId(), user);

        return Response.ok(serialize(user)).build();
    }

    /**
     * Handle a User update request.
     *
     * @param request  the request
     *
     * @return the response
     */
    public Response update(HttpRequest request) {
        String tenant = request.getHeaderString(TENANT_HEADER);
        if (tenant == null || tenant.isBlank()) {
            return Response.status(400).entity(Map.of("Error", "Missing tenant identifier")).build();
        }
        Session session = ensureSession(tenant);
        if (session == null) {
            return Response.status(400).entity(Map.of("Error", "Unknown tenant " + tenant)).build();
        }

        String                   id    = request.getFirstPathParameter("user");
        NamedCache<String, User> users = session.getCache("users");
        User                     user  = users.get(id);
        if (user == null) {
            return Response.notFound().entity(Map.of("Error", "Unknown user " + id)).build();
        }

        User update = getUserFromBody(request);

        String firstName = update.getFirstName();
        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName);
        }

        String lastName = update.getLastName();
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName);
        }

        String email = update.getEmail();
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }

        users.put(id, user);

        return Response.ok(serialize(user)).build();
    }

    /**
     * Handle a User delete request.
     *
     * @param request  the request
     *
     * @return the response
     */
    public Response delete(HttpRequest request) {
        String tenant = request.getHeaderString(TENANT_HEADER);
        if (tenant == null || tenant.isBlank()) {
            return Response.status(400).entity(Map.of("Error", "Missing tenant identifier")).build();
        }
        Session session = ensureSession(tenant);
        if (session == null) {
            return Response.status(400).entity(Map.of("Error", "Unknown tenant " + tenant)).build();
        }

        String                   id    = request.getFirstPathParameter("user");
        NamedCache<String, User> users = session.getCache("users");
        User                     user  = users.remove(id);
        if (user == null) {
            return Response.notFound().entity(Map.of("Error", "Unknown user " + id)).build();
        }

        return Response.ok(serialize(user)).build();
    }

    /**
     * Obtain a {@link Session} for a tenant.
     *
     * @param tenant  the name of the tenant
     *
     * @return the {@link Session} for the tenant, or {@code null} if no
     *         {@link Session} is available for the tenant
     */
    // # tag::ensure[]
    private Session ensureSession(String tenant) {
        TenantMetaData metaData = tenants.get(tenant);  // <1>
        if (metaData == null) {
            return null;  // <2>
        }
        Coherence coherence = Coherence.getInstance();  // <3>
        return coherence.getSessionIfPresent(tenant)  // <4>
                        .orElseGet(()->createSession(coherence, metaData));
    }
    // # end::ensure[]

    /**
     * Create a {@link Session} for a tenant
     *
     * @param coherence  the {@link Coherence} instance that will own the session
     * @param metaData   the {@link TenantMetaData tenant meta-data}
     *
     * @return the {@link Session} for the tenant
     */
    // # tag::create[]
    private Session createSession(Coherence coherence, TenantMetaData metaData) {
        String tenant = metaData.getTenant();
        if (metaData.isExtend()) {
            coherence.addSessionIfAbsent(tenant, ()->createExtendConfiguration(metaData));
        }
        else {
            coherence.addSessionIfAbsent(tenant, ()->createGrpcConfiguration(metaData));
        }
        return coherence.getSession(tenant);
    }
    // # end::create[]

    /**
     * Create a {@link Session} that will connect as an Extend client.
     *
     * @param metaData   the {@link TenantMetaData tenant meta-data}
     *
     * @return the {@link Session} for the tenant
     */
    // # tag::extend[]
    private SessionConfiguration createExtendConfiguration(TenantMetaData metaData) {
        String tenant = metaData.getTenant();
        return SessionConfiguration.builder()
                                   .named(tenant)             // <1>
                                   .withScopeName(tenant)     // <2>
                                   .withMode(Coherence.Mode.ClientFixed)  // <3>
                                   .withParameter("coherence.serializer", metaData.getSerializer())   // <4>
                                   .withParameter("coherence.extend.address", metaData.getHostName()) // <5>
                                   .withParameter("coherence.extend.port", metaData.getPort())        // <6>
                                   .build();  // <7>
    }
    // # end::extend[]

    /**
     * Create a {@link Session} that will connect as a gRPC client.
     *
     * @param metaData   the {@link TenantMetaData tenant meta-data}
     *
     * @return the {@link Session} for the tenant
     */
    // # tag::grpc[]
    private SessionConfiguration createGrpcConfiguration(TenantMetaData metaData) {
        String tenant = metaData.getTenant();
        return SessionConfiguration.builder()
                                   .named(tenant)             // <1>
                                   .withScopeName(tenant)     // <2>
                                   .withMode(Coherence.Mode.GrpcFixed)  // <3>
                                   .withParameter("coherence.serializer", metaData.getSerializer()) // <4>
                                   .withParameter("coherence.grpc.address", metaData.getHostName()) // <5>
                                   .withParameter("coherence.grpc.port", metaData.getPort())        // <6>
                                   .build();  // <7>
    }
    // # end::grpc[]

    private User getUserFromBody(HttpRequest request) {
        try {
            byte[] bytes = request.getBody().readAllBytes();
            return serializer.deserialize(bytes, User.class);
        }
        catch (IOException e) {
            throw Exceptions.ensureRuntimeException(e);
        }
    }

    private WriteBuffer serialize(User user) {
        try {
            return serializer.serialize(user);
        }
        catch (IOException e) {
            throw Exceptions.ensureRuntimeException(e);
        }
    }
}
