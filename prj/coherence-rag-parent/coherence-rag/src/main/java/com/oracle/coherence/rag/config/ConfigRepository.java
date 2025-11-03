/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.rag.internal.json.JsonbProvider;

import com.tangosol.net.NamedMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.json.bind.JsonbException;

import java.util.Objects;

/**
 * Repository for storing and retrieving JSON configuration documents in a
 * distributed Coherence cache.
 * <p/>
 * This repository provides a simple, provider-agnostic mechanism to persist
 * arbitrary configuration payloads as JSON strings, keyed by a composite
 * {@link ConfigKey}. It is primarily used for AI model configuration, but can
 * be reused for other configuration domains (for example, external vector store
 * configuration).
 * <p/>
 * Features:
 * <ul>
 *   <li>Global and store-scoped entries using {@link ConfigKey#storeName()}</li>
 *   <li>Serialization and deserialization via an injected {@link JsonbProvider}
 *   allowing a centralized JSON-B configuration</li>
 *   <li>Typed accessors that map JSON to strongly-typed config classes in
 *   provider modules</li>
 * </ul>
 *
 * @author Aleks Seovic 2025.08.06
 * @since 25.09
 */
@SuppressWarnings("unchecked")
@ApplicationScoped
public class ConfigRepository
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link ConfigRepository} instance.
     *
     * @param mapConfig      Coherence NamedMap to use as configuration store
     * @param jsonbProvider  JSONB supplier
     */
    @Inject
    public ConfigRepository(@Name("jsonConfig") NamedMap<ConfigKey, String> mapConfig, JsonbProvider jsonbProvider)
        {
        f_mapConfig = mapConfig;
        f_jsonb = jsonbProvider;
        }

    // ---- public API ------------------------------------------------------

    /**
     * Returns the raw JSON configuration for the specified key.
     *
     * @param key  the configuration key
     *
     * @return the JSON payload, or {@code null} if no configuration exists
     */
    public String get(ConfigKey key)
        {
        Objects.requireNonNull(key, "Configuration key cannot be null");

        return f_mapConfig.get(key);
        }

    /**
     * Returns a typed configuration object for the specified key by
     * deserializing the stored JSON based on the class of the provided default
     * configuration. If there is no configuration stored under the given key,
     * or if JSON cannot be parsed, a {@code defaultConfig} is returned.
     * <p/>
     * This behavior allows provider code to apply defaults transparently when
     * no explicit configuration has been persisted.
     *
     * @param key            the configuration key
     * @param defaultConfig  the default configuration to return if not present,
     *                       or in case of JSON parsing error
     * @param <T>            the configuration type
     *
     * @return an instance of configuration class populated from JSON, or a
     *         default configuration if missing
     *
     * @throws NullPointerException if either {@code key} or {@code defaultConfig}
     *                              argument is {@code null}
     */
    public <T> T get(ConfigKey key, T defaultConfig)
        {
        Objects.requireNonNull(key, "Configuration key cannot be null");
        Objects.requireNonNull(defaultConfig, "Default configuration cannot be null");

        String jsonConfig = get(key);
        if (jsonConfig != null && !jsonConfig.isBlank())
            {
            try
                {
                return (T) f_jsonb.get().fromJson(jsonConfig, defaultConfig.getClass());
                }
            catch (JsonbException e)
                {
                Logger.err("Failed to parse JSON config for " + key, e);
                }
            }

        return defaultConfig;
        }

    /**
     * Stores the raw JSON configuration under the specified key, replacing any
     * existing value.
     *
     * @param key         the configuration key
     * @param jsonConfig  the JSON payload to store
     */
    public void put(ConfigKey key, String jsonConfig)
        {
        Objects.requireNonNull(key, "Configuration key cannot be null");

        f_mapConfig.put(key, jsonConfig);
        }

    /**
     * Stores the given configuration object under the specified key by
     * serializing it to JSON using the injected {@link JsonbProvider}.
     *
     * @param key     the configuration key
     * @param config  the configuration object to serialize and store
     * @param <T>     the configuration type
     */
    public <T> void put(ConfigKey key, T config)
        {
        Objects.requireNonNull(key, "Configuration key cannot be null");

        put(key, f_jsonb.get().toJson(config));
        }

    /**
     * Removes the configuration entry under the specified key, if present.
     *
     * @param key  the configuration key
     */
    public void remove(ConfigKey key)
        {
        Objects.requireNonNull(key, "Configuration key cannot be null");
        
        f_mapConfig.remove(key);
        }

    /**
     * Returns a snapshot view of all configuration keys stored in the repository.
     *
     * @return the set of keys present in the underlying map
     */
    public java.util.Set<ConfigKey> keys()
        {
        return java.util.Set.copyOf(f_mapConfig.keySet());
        }

    // ---- data members ----------------------------------------------------

    private final NamedMap<ConfigKey, String> f_mapConfig;
    private final JsonbProvider f_jsonb;
    }
