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

import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.Response;

import com.tangosol.io.WriteBuffer;

import com.tangosol.net.NamedMap;

import java.io.IOException;

import java.util.Map;

/**
 * An admin CRUD controller to manage tenants.
 */
public class TenantController {

    private final NamedMap<String, TenantMetaData> tenants;

    private final JsonSerializer serializer;

    public TenantController(NamedMap<String, TenantMetaData> tenants, JsonSerializer serializer) {
        this.tenants = tenants;
        this.serializer = serializer;
    }

    public Response ready(HttpRequest request) {
        return Response.ok().build();
    }

    public Response get(HttpRequest request) {
        String         id       = request.getFirstPathParameter("tenant");
        TenantMetaData metaData = tenants.get(id);
        if (metaData == null) {
            return Response.notFound().entity(Map.of("Error", "Unknown tenant " + id)).build();
        }
        return Response.ok(serialize(metaData)).build();
    }

    public Response create(HttpRequest request) {
        TenantMetaData metaData = getMetadataFromBody(request);
        tenants.put(metaData.getTenant(), metaData);
        Logger.info("Created Tenant: " + metaData);
        return Response.ok(serialize(metaData)).build();
    }

    public Response update(HttpRequest request) {
        String id = request.getFirstPathParameter("tenant");

        TenantMetaData metaData = tenants.get(id);
        if (metaData == null) {
            return Response.notFound().entity(Map.of("Error", "Unknown tenant " + id)).build();
        }

        TenantMetaData updatedMetaData = getMetadataFromBody(request);

        String type = updatedMetaData.getType();
        if (type != null && !type.isBlank()) {
            metaData.setType(type);
        }

        int port = updatedMetaData.getPort();
        if (port > 0) {
            metaData.setPort(port);
        }

        tenants.put(id, metaData);

        return Response.ok(serialize(metaData)).build();
    }

    public Response delete(HttpRequest request) {
        String         id       = request.getFirstPathParameter("tenant");
        TenantMetaData metaData = tenants.remove(id);
        if (metaData == null) {
            return Response.notFound().entity(Map.of("Error", "Unknown tenant " + id)).build();
        }

        return Response.ok(serialize(metaData)).build();
    }

    private TenantMetaData getMetadataFromBody(HttpRequest request) {
        try {
            byte[] bytes = request.getBody().readAllBytes();
            return serializer.deserialize(bytes, TenantMetaData.class);
        }
        catch (IOException e) {
            throw Exceptions.ensureRuntimeException(e);
        }
    }

    private WriteBuffer serialize(TenantMetaData metaData) {
        try {
            return serializer.serialize(metaData);
        }
        catch (IOException e) {
            throw Exceptions.ensureRuntimeException(e);
        }
    }
}
