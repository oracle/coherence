/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.http.HttpApplication;

import com.tangosol.coherence.rest.DefaultRootResource;

import com.tangosol.coherence.rest.providers.EntryWriter;
import com.tangosol.coherence.rest.providers.JacksonMapperProvider;
import com.tangosol.coherence.rest.providers.JsonCollectionWriter;
import com.tangosol.coherence.rest.providers.ObjectWriter;
import com.tangosol.coherence.rest.providers.SecurityExceptionMapper;
import com.tangosol.coherence.rest.providers.SecurityFilter;
import com.tangosol.coherence.rest.providers.XmlCollectionWriter;
import com.tangosol.coherence.rest.providers.XmlKeysWriter;
import com.tangosol.coherence.rest.providers.XmlMapWriter;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;

import org.glassfish.jersey.logging.LoggingFeature;

import org.glassfish.jersey.media.sse.SseFeature;

import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.jersey.server.filter.UriConnegFilter;

/**
 * An extension of <tt>org.glassfish.jersey.server.ResourceConfig</tt>
 * that registers the Coherence REST root resource and provider classes, in
 * addition to user defined package names.
 *
 * @author ic  2011.07.05
 */
@ApplicationPath("/api")
public class DefaultResourceConfig
        extends ResourceConfig
        implements HttpApplication
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public DefaultResourceConfig()
        {
        this((String[]) null);
        }

    /**
     * Construct an instance of <tt>ResourceConfig</tt> that initialized with
     * a given set of resource/provider classes and Coherence predefined root
     * resource and providers.
     *
     * @param classes  a set of resource/provider classes to initialize the
     *                 resource configuration with. If null or an empty set is
     *                 provided, the Coherence predefined root resource and
     *                 providers will be used.
     */
    public DefaultResourceConfig(Class<?> classes)
        {
        super(classes);
        registerProviders();
        registerRootResource();
        registerContainerRequestFilters();
        registerContainerResponseFilters();
        registerResourceFilterFactories();
        registerLoggingFeature();
        }

    /**
     * Construct an instance of <tt>ResourceConfig</tt> that initialize
     * Coherence predefined properties and searches for root resource classes
     * and providers in the specified packages.
     *
     * @param asPackages  an array of package names to be scanned for root
     *                    resource classes and providers. If null or an empty
     *                    array is provided, the Coherence predefined root
     *                    resource and providers will be used.
     */
    public DefaultResourceConfig(String... asPackages)
        {
        if (asPackages != null && asPackages.length > 0)
            {
            packages(asPackages);
            }
        registerProviders();
        registerRootResource();
        registerContainerRequestFilters();
        registerContainerResponseFilters();
        registerResourceFilterFactories();
        registerLoggingFeature();
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return "DefaultResourceConfig";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Register the default Coherence REST root resource class.
     */
    protected void registerRootResource()
        {
        register(DefaultRootResource.class);
        }

    /**
     * Register the default Coherence REST providers.
     */
    protected void registerProviders()
        {
        register(new InjectionBinder(isRunningInContainer()));
        register(SecurityExceptionMapper.class);
        register(XmlKeysWriter.class);
        register(JsonCollectionWriter.class);
        register(XmlCollectionWriter.class);
        register(XmlMapWriter.class);
        register(EntryWriter.class);
        register(ObjectWriter.class);
        register(SecurityFilter.class);
        register(JacksonMapperProvider.class);
        register(JacksonFeature.class);
        register(SseFeature.class);
        }

    /**
     * Register the predefined Coherence REST container request filters.
     */
    protected void registerContainerRequestFilters()
        {
        register(new UriConnegFilter(getExtensionsMap(), new LinkedHashMap<>()));
        }

    /**
     * Register the predefined Coherence REST container response filters.
     */
    protected void registerContainerResponseFilters()
        {
        }

    /**
     * Register the predefined Coherence REST resource filter factories.
     */
    protected void registerResourceFilterFactories()
        {
        }

    /**
     * Register the Jersey LoggingFeature iff the {@code LOGGING_FEATURE_ENABLED_PROP} is {@code true}.
     *
     * @since 22.06
     */
    private void registerLoggingFeature()
        {
        if (LOGGING_FEATURE_ENABLED)
            {
            com.oracle.coherence.common.base.Logger.finest("Enabling Jersey LoggingFeature");

            register(new LoggingFeature(Logger.getLogger("coherence.rest.diagnostic"),
                                        Level.INFO,
                                        LoggingFeature.Verbosity.PAYLOAD_ANY, 4098));
            }
        }

    /**
     * Return whether REST API is run inside the container (true)
     * or standalone (false).
     *
     * @return whether REST API is run inside the container (true) or
     *         standalone (false)
     */
    protected boolean isRunningInContainer()
        {
        return false;
        }

    /**
     * Construct a map with URL suffix to media type mappings.
     * <p>
     * Supported values are ".txt", ".bin", ".xml", ".json".
     *
     * @return URL suffix to media type mappings
     */
    protected static Map<String, MediaType> getExtensionsMap()
        {
        Map<String, MediaType> map = new LinkedHashMap<>(4);
        map.put("txt",  MediaType.TEXT_PLAIN_TYPE);
        map.put("bin",  MediaType.APPLICATION_OCTET_STREAM_TYPE);
        map.put("xml",  MediaType.APPLICATION_XML_TYPE);
        map.put("json", MediaType.APPLICATION_JSON_TYPE);
        return map;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The system property name to enable Jersey's LoggingFeature extension which
     * will enable logging of HTTP payloads as received and sent by Jersey.
     *
     * @since 22.06
     */
    private static final String LOGGING_FEATURE_ENABLED_PROP = DefaultResourceConfig.class.getName() + ".logging.enabled";

    /**
     * Flag indicating whether Jersey's LoggingFeature extension is enabled or not.
     *
     * @since 22.06
     */
    private static final boolean LOGGING_FEATURE_ENABLED = Config.getBoolean(LOGGING_FEATURE_ENABLED_PROP);
    }
