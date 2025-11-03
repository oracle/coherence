/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal.json;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import java.util.function.Supplier;

@Provider
@ApplicationScoped
public class JsonbProvider
        implements ContextResolver<Jsonb>, Supplier<Jsonb>
    {
    private final Jsonb jsonb;

    public JsonbProvider()
        {
        JsonbConfig config = new JsonbConfig();
                //.withDeserializers(new StoreConfigDeserializer());

        this.jsonb = JsonbBuilder.create(config);
        }

    @Override
    public Jsonb getContext(Class<?> type)
        {
        return jsonb;
        }

    @Override
    public Jsonb get()
        {
        return jsonb;
        }
    }
