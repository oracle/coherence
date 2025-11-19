/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.mp.config.CoherenceConfigSource;
import com.oracle.coherence.mp.config.ConfigPropertyChanged;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;

/**
 * Abstract base class for AI model suppliers with configuration and caching support.
 * <p/>
 * This abstract class provides a framework for implementing model suppliers that
 * can create and cache AI model instances (such as chat models or embedding models)
 * based on configuration properties. It includes the following key features:
 * <ul>
 *   <li>Configurable default model selection via MicroProfile Config</li>
 *   <li>Thread-safe model instance caching to avoid repeated creation</li>
 *   <li>Dynamic configuration updates with automatic model switching</li>
 *   <li>Error handling and fallback to default models</li>
 *   <li>CDI integration for dependency injection and event handling</li>
 * </ul>
 * <p/>
 * Model instances are created lazily and cached for reuse, ensuring efficient
 * resource utilization. The supplier supports both default model access and
 * explicit model selection by name.
 * <p/>
 * Configuration changes are monitored through CDI events, allowing the supplier
 * to automatically update the default model when configuration properties change
 * at runtime.
 * 
 * @param <T> the type of model instances this supplier creates
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@SuppressWarnings("CdiInjectionPointsInspection")
public abstract class AbstractModelSupplier<T>
        implements Supplier<T>
    {
    /**
     * MicroProfile Config instance for accessing configuration properties.
     * <p/>
     * Used to read the default model name from configuration and provides
     * a standard way to access application configuration values.
     */
    @Inject
    protected Config config;

    /**
     * Coherence configuration source for dynamic configuration management.
     * <p/>
     * Provides access to Coherence's distributed configuration system,
     * enabling runtime configuration updates and cluster-wide consistency.
     */
    @Inject
    protected CoherenceConfigSource coherenceConfig;

    /**
     * The currently configured default model name.
     * <p/>
     * This field is volatile to ensure visibility across threads when
     * configuration changes occur. It represents the model that will be
     * returned when no specific model is requested.
     */
    private volatile ModelName defaultModelName;

    /**
     * Thread-safe cache of created model instances keyed by model name.
     * <p/>
     * This map ensures that model instances are created only once per name
     * and reused for subsequent requests, improving performance and resource
     * utilization.
     */
    private final ConcurrentMap<ModelName, T> mapModel = new ConcurrentHashMap<>();

    /**
     * Initializes the supplier after CDI injection is complete.
     * <p/>
     * This method sets up the default model name based on the current
     * configuration. It is called automatically by the CDI container
     * after all dependencies have been injected.
     */
    @PostConstruct
    private void init()
        {
        defaultModelName = defaultModelName();
        }

    /**
     * Returns the default model instance.
     * <p/>
     * This method provides access to the currently configured default model.
     * The model instance is created lazily on first access and cached for
     * subsequent requests.
     * 
     * @return the default model instance
     */
    public T get()
        {
        return get(defaultModelName);
        }

    /**
     * Returns a model instance for the specified model name.
     * <p/>
     * This convenience method wraps the model name in a ModelName object
     * and delegates to the primary get method.
     * 
     * @param modelName the name of the model to retrieve
     * 
     * @return the model instance for the specified name
     */
    public T get(String modelName)
        {
        return get(ModelName.of(modelName));
        }

    /**
     * Returns a model instance for the specified ModelName.
     * <p/>
     * This method implements the core model retrieval logic. It checks the
     * cache first and creates a new model instance if one doesn't exist.
     * The creation is thread-safe and ensures only one instance per model name.
     * 
     * @param modelName the ModelName object identifying the model
     * 
     * @return the model instance for the specified name
     */
    public T get(ModelName modelName)
        {
        return mapModel.computeIfAbsent(modelName, this::create);
        }

    /**
     * Determines the default model name from configuration.
     * <p/>
     * This method reads the configuration property specified by {@link #configProperty()}
     * and falls back to {@link #defaultModel()} if no configuration is found.
     * 
     * @return the ModelName representing the default model
     */
    public ModelName defaultModelName()
        {
        return new ModelName(config.getOptionalValue(configProperty(), String.class).orElse(defaultModel()));
        }

    /**
     * Returns a human-readable description of the model type.
     * <p/>
     * This description is used in logging messages to identify the type
     * of models this supplier manages (e.g., "chat", "embedding").
     * 
     * @return a descriptive string for the model type
     */
    protected abstract String description();

    /**
     * Returns the default model name to use when no configuration is provided.
     * <p/>
     * This method must be implemented by subclasses to specify the fallback
     * model name when no explicit configuration is available.
     * 
     * @return the default model name string
     */
    protected abstract String defaultModel();

    /**
     * Returns the configuration property key for this model supplier.
     * <p/>
     * This method must be implemented by subclasses to specify which
     * configuration property contains the default model name.
     * 
     * @return the configuration property key
     */
    protected abstract String configProperty();

    /**
     * Creates a new model instance for the specified model name.
     * <p/>
     * This abstract method must be implemented by subclasses to provide
     * the actual model creation logic. The implementation should handle
     * any model-specific initialization and configuration.
     * 
     * @param modelName the name of the model to create
     * 
     * @return a new model instance
     */
    public abstract T create(ModelName modelName);

    /**
     * Handles configuration property change events.
     * <p/>
     * This method observes CDI configuration change events and updates the
     * default model when the relevant configuration property changes. It
     * includes error handling and logging to track configuration updates.
     * 
     * @param evt the configuration property change event
     */
    private void onConfigChange(@Observes ConfigPropertyChanged evt)
        {
        if (evt.getKey().equals(configProperty()))
            {
            String name = evt.getValue();
            if (name != null && !name.isBlank())
                {
                try
                    {
                    ModelName modelName = new ModelName(name);
                    if (!defaultModelName.equals(modelName))
                        {
                        defaultModelName = modelName;
                        Logger.config("Changed default %s model to '%s'".formatted(description(), name));
                        }
                    }
                catch (Exception e)
                    {
                    Logger.err("Failed to change default %s model to '%s'".formatted(description(), name), e);
                    setDefault();
                    }
                }
            else
                {
                Logger.warn("Model name cannot be empty");
                setDefault();
                }
            }
        }

    /**
     * Sets the configuration back to the default model value.
     * <p/>
     * This method is called when configuration validation fails or when
     * an invalid model name is provided. It ensures the system falls back
     * to a known good configuration state.
     */
    private void setDefault()
        {
        Logger.config("Setting default %s model to %s".formatted(description(), defaultModel()));
        coherenceConfig.setValue(configProperty(), defaultModel());
        }
    }
